package dev.fasttool.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fasttool.RunMethod
import dev.fasttool.SetupPrefs
import dev.fasttool.fastboot.HostState
import dev.fasttool.fastboot.UsbFastbootManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class TerminalLine(val text: String, val type: LineType = LineType.Output)
enum class LineType { System, Command, Output, Error, Success }
enum class SetupPhase { Idle, Method, Directory, Warning, Done }

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val setupPrefs = remember { SetupPrefs(context) }
    val usbManager = remember { UsbFastbootManager(context) }
    val hostState by usbManager.state.collectAsState()

    var lines by remember { mutableStateOf(listOf<TerminalLine>()) }
    var input by remember { mutableStateOf("") }
    var showTemplates by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var setupPhase by remember { mutableStateOf(if (setupPrefs.completed) SetupPhase.Done else SetupPhase.Idle) }
    var setupVisible by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(setupPrefs.method) }
    var workDir by remember { mutableStateOf(setupPrefs.workDir) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val isBusy = hostState is HostState.Busy

    fun append(text: String, type: LineType = LineType.Output) { lines = lines + TerminalLine(text, type) }
    fun hasSu(): Boolean = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su").any { File(it).exists() }

    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            workDir = uri.toString()
            append("Directory: $workDir", LineType.Success)
            setupPhase = SetupPhase.Warning
            setupVisible = true
        }
    }

    fun startSetup() {
        lines = emptyList()
        append("Setup starting..", LineType.System)
        setupPhase = SetupPhase.Method
        setupVisible = true
        if (hasSu()) {
            selectedMethod = RunMethod.Root
            append("Root (su) detected — selected automatically", LineType.Success)
        }
    }

    fun finishSetup() {
        setupPrefs.method = selectedMethod
        setupPrefs.workDir = workDir
        setupPrefs.completed = true
        setupPhase = SetupPhase.Done
        setupVisible = false
        append("Setup complete.", LineType.Success)
        append("Method: ${selectedMethod.name}", LineType.System)
        if (workDir.isNotBlank()) append("Work dir: $workDir", LineType.System)
        append("Type help or fastboot devices", LineType.System)
    }

    DisposableEffect(Unit) {
        usbManager.start()
        onDispose { usbManager.stop() }
    }

    LaunchedEffect(Unit) {
        if (!setupPrefs.completed) { delay(400); startSetup() }
        else {
            append("Session idle — waiting for a command", LineType.System)
            append("Try: fastboot devices", LineType.System)
        }
    }

    fun runCommand(raw: String) {
        val cmdLine = raw.trim()
        if (cmdLine.isEmpty()) return
        append("~ $ $cmdLine", LineType.Command)
        input = ""
        val parts = cmdLine.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)
        scope.launch {
            when {
                cmd == "setup" -> startSetup()
                cmd == "help" -> {
                    append("FastTool commands:", LineType.System)
                    append("  help / status / devices / clear / version / setup")
                    append("  fastboot <args>")
                }
                cmd == "status" -> {
                    when (val s = hostState) {
                        is HostState.NoDevice -> append("OTG host · no device")
                        is HostState.PermissionNeeded -> append("OTG host · permission needed")
                        is HostState.AdbOnly -> append("Device Detected but not in Fastboot")
                        is HostState.Ready -> append("OTG host · ready (${s.device.product ?: s.device.deviceName})")
                        is HostState.Busy -> append("OTG host · ${s.message}")
                        is HostState.Error -> append("OTG host · error — ${s.message}")
                    }
                    append("Method: ${setupPrefs.method.name}")
                }
                cmd == "devices" || (cmd == "fastboot" && args.firstOrNull() == "devices") -> {
                    usbManager.runFastboot("devices").onSuccess { out ->
                        out.lines().forEach { if (it.isNotBlank()) append(it) }
                        if (out.isBlank()) append("(no devices)")
                    }.onFailure { append(it.message ?: "failed", LineType.Error) }
                }
                cmd == "clear" -> lines = listOf(TerminalLine("Session cleared", LineType.System))
                cmd == "version" -> { append("FastTool 0.2.0"); append("https://github.com/Classity-Real/FastTool") }
                cmd == "fastboot" -> {
                    usbManager.runFastboot(*args.toTypedArray()).onSuccess { out ->
                        out.lines().forEach { if (it.isNotBlank()) append(it) }
                        if (out.isBlank()) append("OK", LineType.Success)
                    }.onFailure { append(it.message ?: "fastboot failed", LineType.Error) }
                }
                cmd in listOf("getvar", "reboot", "flash", "erase", "oem", "continue", "boot") -> {
                    usbManager.runFastboot(cmd, *args.toTypedArray()).onSuccess { out ->
                        out.lines().forEach { if (it.isNotBlank()) append(it) }
                        if (out.isBlank()) append("OK", LineType.Success)
                    }.onFailure { append(it.message ?: "failed", LineType.Error) }
                }
                else -> {
                    append("Unknown command: $cmd", LineType.Error)
                    append("Type help or: fastboot devices", LineType.System)
                }
            }
            listState.animateScrollToItem(lines.lastIndex.coerceAtLeast(0))
        }
    }

    fun onTemplateFlash(template: FlashTemplate, images: Map<String, Uri>) {
        scope.launch {
            append("Template: ${template.title}", LineType.System)
            for (part in template.partitions) {
                val uri = images[part] ?: continue
                append("Flashing $part…")
                usbManager.flash(part, uri).onSuccess { append(it, LineType.Success) }.onFailure { append("Failed: ${it.message}", LineType.Error) }
            }
            usbManager.setReadyFromCurrent()
            append("Template finished.", LineType.System)
            listState.animateScrollToItem(lines.lastIndex.coerceAtLeast(0))
        }
    }

    fun onSingleFlash(partition: String, uri: Uri) {
        scope.launch {
            append("Flash $partition ← ${uri.lastPathSegment}", LineType.System)
            usbManager.flash(partition, uri).onSuccess { append(it, LineType.Success) }.onFailure { append("Failed: ${it.message}", LineType.Error) }
            usbManager.setReadyFromCurrent()
            listState.animateScrollToItem(lines.lastIndex.coerceAtLeast(0))
        }
    }

    var lastAdbNotice by remember { mutableStateOf(false) }
    LaunchedEffect(hostState) {
        when (hostState) {
            is HostState.AdbOnly -> {
                if (!lastAdbNotice) {
                    lastAdbNotice = true
                    append("Device Detected but not in Fastboot", LineType.System)
                }
            }
            else -> lastAdbNotice = false
        }
    }

    val otgLabel = when (val s = hostState) {
        is HostState.NoDevice -> "No device"
        is HostState.PermissionNeeded -> "Permission needed"
        is HostState.AdbOnly -> "Device Detected but not in Fastboot"
        is HostState.Ready -> s.device.product?.takeIf { it.isNotBlank() } ?: "Ready"
        is HostState.Busy -> s.message
        is HostState.Error -> "Error"
    }
    val otgColor = when (hostState) {
        is HostState.Ready -> Color(0xFF4ADE80)
        is HostState.AdbOnly, is HostState.Busy, is HostState.PermissionNeeded -> Color(0xFFFBBF24)
        is HostState.Error -> Color(0xFFF87171)
        is HostState.NoDevice -> Color(0xFF666666)
    }
    val otgDot = when (hostState) {
        is HostState.Ready -> Color(0xFF4ADE80)
        is HostState.AdbOnly, is HostState.Busy, is HostState.PermissionNeeded -> Color(0xFFFBBF24)
        is HostState.Error -> Color(0xFFF87171)
        is HostState.NoDevice -> Color(0xFF555555)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).statusBarsPadding().imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("FastTool", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF5F5F5), letterSpacing = (-0.8).sp, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(7.dp).background(otgDot, CircleShape))
                    Text(otgLabel, fontSize = 12.sp, color = otgColor, maxLines = 1)
                }
            }
            Text("\"Fastboot Tool made for Android :)\"", fontSize = 13.sp, color = Color(0xFF5A5A5A), modifier = Modifier.padding(top = 4.dp))
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp).background(Color(0xFF101010), RoundedCornerShape(24.dp))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(lines) { line ->
                    val color = when (line.type) {
                        LineType.System -> Color(0xFF6A6A6A)
                        LineType.Command -> Color(0xFFECECEC)
                        LineType.Output -> Color(0xFFB0B0B0)
                        LineType.Error -> Color(0xFFF87171)
                        LineType.Success -> Color(0xFF4ADE80)
                    }
                    Text(line.text, color = color, fontSize = 13.sp, fontFamily = FontFamily.Monospace, lineHeight = 22.sp)
                }
            }

            if (setupVisible && setupPhase != SetupPhase.Done && setupPhase != SetupPhase.Idle) {
                SetupOverlay(
                    phase = setupPhase,
                    selectedMethod = selectedMethod,
                    workDir = workDir,
                    onSelectMethod = { method ->
                        selectedMethod = method
                        append("Method: ${method.name}", LineType.Success)
                        setupPhase = SetupPhase.Directory
                    },
                    onPickDirectory = { dirPicker.launch(null) },
                    onWorkDirTyped = { workDir = it },
                    onContinueDirectory = {
                        if (workDir.isNotBlank()) {
                            append("Directory: $workDir", LineType.Success)
                            setupPhase = SetupPhase.Warning
                        }
                    },
                    onAcceptWarning = { finishSetup() },
                    onDeclineWarning = {
                        append("Setup cancelled.", LineType.System)
                        setupPhase = SetupPhase.Idle
                        setupVisible = false
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(onClick = { showTemplates = true }, shape = CircleShape, color = Color(0xFF1C1C1C), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Download, contentDescription = "Templates", tint = Color(0xFFE8E8E8), modifier = Modifier.size(22.dp))
                }
            }
            Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF1A1A1A), modifier = Modifier.weight(1f).height(48.dp)) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        enabled = !isBusy,
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFFECECEC), fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(Color(0xFFECECEC)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { runCommand(input) }),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) Text("~ $", color = Color(0xFF555555), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            inner()
                        }
                    )
                }
            }
            Surface(onClick = { showFlash = true }, shape = CircleShape, color = Color(0xFF1C1C1C), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Monitor, contentDescription = "Flash", tint = Color(0xFFE8E8E8), modifier = Modifier.size(22.dp))
                }
            }
        }
    }

    if (showFlash) FlashSheet(onDismiss = { showFlash = false }, onFlash = { part, uri -> onSingleFlash(part, uri) })
    if (showTemplates) TemplatesSheet(onDismiss = { showTemplates = false }, onFlash = { template, images -> onTemplateFlash(template, images) })
}
