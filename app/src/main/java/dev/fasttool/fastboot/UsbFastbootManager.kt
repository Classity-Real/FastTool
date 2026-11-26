package dev.fasttool.fastboot

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
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

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _state = MutableStateFlow<HostState>(HostState.NoDevice)
    val state: StateFlow<HostState> = _state.asStateFlow()

    private val permissionAction = "${context.packageName}.USB_PERMISSION"

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
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
        }
    }

    fun start() {
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

        // Check already connected devices
        scanConnectedDevices()
    }

    fun stop() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    fun scanConnectedDevices() {
        val devices = usbManager.deviceList.values
        val candidate = devices.firstOrNull { it.looksLikeFastboot() }
        if (candidate == null) {
            _state.value = HostState.NoDevice
            return
        }
        if (usbManager.hasPermission(candidate)) {
            _state.value = HostState.Ready(candidate.toFastbootDevice())
        } else {
            requestPermission(candidate)
        }
    }

    private fun onDeviceAttached(device: UsbDevice) {
        if (!device.looksLikeFastboot()) return
        if (usbManager.hasPermission(device)) {
            _state.value = HostState.Ready(device.toFastbootDevice())
        } else {
            requestPermission(device)
        }
    }

    private fun requestPermission(device: UsbDevice) {
        _state.value = HostState.PermissionNeeded
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(context, 0, Intent(permissionAction), flags)
        usbManager.requestPermission(device, pi)
    }

    fun setBusy(message: String) {
        _state.value = HostState.Busy(message)
    }

    fun setReadyFromCurrent() {
        scanConnectedDevices()
    }

    // --- Fastboot command stubs (real protocol / binary integration comes next) ---

    suspend fun flash(partition: String, imagePath: String): Result<String> {
        val current = _state.value
        if (current !is HostState.Ready) {
            return Result.failure(IllegalStateException("No fastboot device ready"))
        }
        setBusy("Flashing $partition…")
        // TODO: real fastboot flash implementation (USB bulk or bundled binary)
        return Result.success("Queued flash $partition ← $imagePath (protocol not yet wired)")
    }

    suspend fun erase(partition: String): Result<String> {
        val current = _state.value
        if (current !is HostState.Ready) {
            return Result.failure(IllegalStateException("No fastboot device ready"))
        }
        setBusy("Erasing $partition…")
        return Result.success("Queued erase $partition (protocol not yet wired)")
    }

    suspend fun reboot(): Result<String> {
        setBusy("Rebooting…")
        return Result.success("Queued reboot (protocol not yet wired)")
    }
}

private fun UsbDevice.looksLikeFastboot(): Boolean {
    // Heuristic: many devices in fastboot expose vendor-specific class or known VIDs
    if (vendorId == 0x18D1) return true // Google
    // Interface class 0xFF is common for fastboot
    for (i in 0 until interfaceCount) {
        val intf = getInterface(i)
        if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC) return true
    }
    return true // for now accept any attached device while developing
}

private fun UsbDevice.toFastbootDevice() = FastbootDevice(
    deviceName = deviceName,
    vendorId = vendorId,
    productId = productId,
    manufacturer = manufacturerName,
    product = productName
)

@Suppress("DEPRECATION")
private inline fun <reified T> Intent.getParcelableExtraCompat(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        getParcelableExtra(key) as? T
    }
}
