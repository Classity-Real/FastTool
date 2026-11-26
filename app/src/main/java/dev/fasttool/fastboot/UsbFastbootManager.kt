package dev.fasttool.fastboot

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FastbootDevice(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?
)

sealed class HostState {
    data object NoDevice : HostState()
    data object PermissionNeeded : HostState()
    data class Ready(val device: FastbootDevice) : HostState()
    data class Busy(val message: String) : HostState()
    data class Error(val message: String) : HostState()
}

class UsbFastbootManager(private val context: Context) {

    private val tag = "UsbFastboot"
    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val runner = FastbootRunner(context)

    private val _state = MutableStateFlow<HostState>(HostState.NoDevice)
    val state: StateFlow<HostState> = _state.asStateFlow()

    private val permissionAction = "${context.packageName}.USB_PERMISSION"
    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            try {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        val device = intent.getParcelableExtraCompat<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        device?.let { onDeviceAttached(it) }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        _state.value = HostState.NoDevice
                    }
                    permissionAction -> {
                        val device = intent.getParcelableExtraCompat<UsbDevice>(UsbManager.EXTRA_DEVICE)
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (device != null && granted) {
                            _state.value = HostState.Ready(device.toFastbootDevice())
                        } else {
                            _state.value = HostState.Error("USB permission denied")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "receiver error", e)
                _state.value = HostState.Error(e.message ?: "USB error")
            }
        }
    }

    fun start() {
        if (usbManager == null) {
            _state.value = HostState.Error("USB host not available")
            return
        }
        try {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(permissionAction)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(usbReceiver, filter)
            }
            receiverRegistered = true
            scanConnectedDevices()
        } catch (e: Exception) {
            Log.e(tag, "start failed", e)
            _state.value = HostState.Error(e.message ?: "USB init failed")
        }
    }

    fun stop() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {
        }
        receiverRegistered = false
    }

    fun scanConnectedDevices() {
        val mgr = usbManager ?: run {
            _state.value = HostState.Error("USB host not available")
            return
        }
        try {
            val devices = mgr.deviceList?.values.orEmpty()
            val candidate = devices.firstOrNull { it.looksLikeFastboot() }
            if (candidate == null) {
                _state.value = HostState.NoDevice
                return
            }
            if (mgr.hasPermission(candidate)) {
                _state.value = HostState.Ready(candidate.toFastbootDevice())
            } else {
                requestPermission(candidate)
            }
        } catch (e: Exception) {
            Log.e(tag, "scan failed", e)
            _state.value = HostState.NoDevice
        }
    }

    private fun onDeviceAttached(device: UsbDevice) {
        val mgr = usbManager ?: return
        if (!device.looksLikeFastboot()) return
        if (mgr.hasPermission(device)) {
            _state.value = HostState.Ready(device.toFastbootDevice())
        } else {
            requestPermission(device)
        }
    }

    private fun requestPermission(device: UsbDevice) {
        val mgr = usbManager ?: return
        _state.value = HostState.PermissionNeeded
        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pi = PendingIntent.getBroadcast(context, 0, Intent(permissionAction), flags)
            mgr.requestPermission(device, pi)
        } catch (e: Exception) {
            Log.e(tag, "requestPermission failed", e)
            _state.value = HostState.Error(e.message ?: "Permission request failed")
        }
    }

    fun setBusy(message: String) {
        _state.value = HostState.Busy(message)
    }

    fun setReadyFromCurrent() {
        scanConnectedDevices()
    }

    /** Run arbitrary fastboot args, e.g. devices / getvar all / reboot */
    suspend fun runFastboot(vararg args: String): Result<String> {
        return try {
            setBusy(args.joinToString(" ").ifBlank { "fastboot" })
            val result = runner.run(*args)
            val out = listOf(result.stdout, result.stderr)
                .filter { it.isNotBlank() }
                .joinToString("\n")
            if (result.exitCode == 0) {
                Result.success(out.ifBlank { "OK" })
            } else {
                Result.failure(RuntimeException(out.ifBlank { "exit ${result.exitCode}" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            setReadyFromCurrent()
        }
    }

    suspend fun flash(partition: String, imageUri: Uri): Result<String> {
        return try {
            setBusy("Flashing $partition…")
            val imageFile = runner.materializeUri(imageUri, "$partition.img")
            val result = runner.flash(partition, imageFile)
            if (result.exitCode == 0) {
                Result.success(result.stdout.ifBlank { "OK: flashed $partition" })
            } else {
                val msg = listOf(result.stderr, result.stdout)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                Result.failure(RuntimeException(msg.ifBlank { "fastboot exited ${result.exitCode}" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun devices(): Result<String> = runFastboot("devices")

    suspend fun reboot(): Result<String> = runFastboot("reboot")
}

private fun UsbDevice.looksLikeFastboot(): Boolean {
    if (vendorId == 0x18D1) return true
    try {
        for (i in 0 until interfaceCount) {
            val intf = getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) return true
        }
    } catch (_: Exception) {
    }
    return true
}

private fun UsbDevice.toFastbootDevice() = FastbootDevice(
    deviceName = deviceName ?: "unknown",
    vendorId = vendorId,
    productId = productId,
    manufacturer = try { manufacturerName } catch (_: Exception) { null },
    product = try { productName } catch (_: Exception) { null }
)

@Suppress("DEPRECATION")
private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key) as? T
    }
}
