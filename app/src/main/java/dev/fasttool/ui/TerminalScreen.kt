package dev.fasttool.ui

import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fasttool.fastboot.HostState
import dev.fasttool.fastboot.UsbFastbootManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TerminalLine(
    val text: String,
    val type: LineType = LineType.Output
)

enum class LineType {
    System, Command, Output, Error, Success
}

@Composable
fun TerminalScreen() {
    val context = LocalContext.current
    val usbManager = remember { UsbFastbootManager(context) }
    val hostState by usbManager.state.collectAsState()

    var lines by remember {
        mutableStateOf(
            listOf(
                TerminalLine("Session idle — waiting for a command", LineType.System)
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var showTemplates by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val isBusy = hostState is HostState.Busy

    fun append(text: String, type: LineType = LineType.Output) {
        lines = lines + TerminalLine(text, type)
    }

    val statusText = when (val s = hostState) {
        is HostState.NoDevice -> "OTG host · no device"
        is HostState.PermissionNeeded -> "OTG host · permission needed"
        is HostState.Ready -> "OTG host · ready"
        is HostState.Busy -> "OTG host · ${s.message}"
        is HostState.Error -> "OTG host · error"
    }

    val statusColor = when (hostState) {
        is HostState.Ready -> Color(0xFF4ADE80)
        is HostState.Busy -> Color(0xFFFACC15)
        is HostState.Error, is HostState.PermissionNeeded -> Color(0xFFF87171)
        else -> Color(0xFF767676)
    }

    DisposableEffect(Unit) {
        usbManager.start()
        onDispose { usbManager.stop() }
    }

    fun runCommand(raw: String) {
        val cmdLine = raw.trim()
        if (cmdLine.isEmpty()) return

        append("$ $cmdLine", LineType.Command)
        input = ""

        val parts = cmdLine.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        scope.launch {
            when {
                cmd == "help" -> {
                    append("FastTool commands:", LineType.System)
                    append("  help")
                    append("  status")
                    append("  devices")
                    append("  clear")
                    append("  version")
                    append("  fastboot <args>   run real fastboot")
                    append("")
                    append("Examples:", LineType.System)
                    append("  fastboot devices")
                    append("  fastboot getvar product")
                    append("  fastboot reboot")
                    append("  fastboot flash boot boot.img")
                    append("")
                    append("Or use Templates (download button).", LineType.System)
                }
                cmd == "status" -> {
                    append("Host status", LineType.System)
                    when (val s = hostState) {
                        is HostState.NoDevice -> {
                            append("  State  no device")
                            append("  Hint   connect device in fastboot over OTG")
                        }
                        is HostState.PermissionNeeded -> append("  State  waiting for USB permission")
                        is HostState.Ready -> {
                            append("  State  ready")
                            append("  Device ${s.device.product ?: s.device.deviceName}")
                            append("  ID     ${s.device.vendorId.toString(16)}:${s.device.productId.toString(16)}")
                        }
                        is HostState.Busy -> append("  State  busy — ${s.message}")
                        is HostState.Error -> append("  State  error — ${s.message}")
                    }
                }
                cmd == "devices" || (cmd == "fastboot" && args.firstOrNull() == "devices") -> {
                    val result = usbManager.runFastboot("devices")
                    result.onSuccess { out ->
                        out.lines().forEach { append(it) }
                        if (out.isBlank()) append("(no devices)")
                    }.onFailure {
                        append(it.message ?: "failed", LineType.Error)
                    }
                }
                cmd == "clear" -> {
                    lines = listOf(TerminalLine("Session cleared", LineType.System))
                }
                cmd == "version" -> {
                    append("FastTool 0.1.0")
                    append("https://github.com/Classity-Real/FastTool")
                }
                cmd == "fastboot" -> {
                    val result = usbManager.runFastboot(*args.toTypedArray())
                    result.onSuccess { out ->
                        out.lines().forEach { line ->
                            if (line.isNotBlank()) append(line)
                        }
                        if (out.isBlank()) append("OK", LineType.Success)
                    }.onFailure {
                        append(it.message ?: "fastboot failed", LineType.Error)
                    }
                }
                cmd in listOf("getvar", "reboot", "flash", "erase", "oem", "continue", "boot") -> {
                    val result = usbManager.runFastboot(cmd, *args.toTypedArray())
                    result.onSuccess { out ->
                        out.lines().forEach { line ->
                            if (line.isNotBlank()) append(line)
                        }
                        if (out.isBlank()) append("OK", LineType.Success)
                    }.onFailure {
                        append(it.message ?: "failed", LineType.Error)
                    }
                }
                else -> {
                    append("Unknown command: $cmd", LineType.Error)
                    append("Type help or: fastboot devices", LineType.System)
                }
            }
            listState.animateScrollToItem(lines.lastIndex.coerceAtLeast(0))
        }
    }

    fun onFlash(template: FlashTemplate, images: Map<String, Uri>) {
        scope.launch {
            append("Template: ${template.title}", LineType.System)
            template.partitions.forEach { part ->
                val uri = images[part]
                append("  $part ← ${uri?.lastPathSegment ?: "?"}")
            }
            append("")

            for (part in template.partitions) {
                val uri = images[part] ?: continue
                append("Flashing $part…")
                val result = usbManager.flash(part, uri)
                result.onSuccess {
                    append(it, LineType.Success)
                }.onFailure {
                    append("Failed: ${it.message}", LineType.Error)
                }
            }
            usbManager.setReadyFromCurrent()
            append("Template finished.", LineType.System)
            listState.animateScrollToItem(lines.lastIndex.coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "FastTool",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = Color(0xFFECECEC),
                    lineHeight = 25.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(color = statusColor, shape = CircleShape)
                    )
                    Text(
                        text = statusText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF767676),
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Surface(
                onClick = { showTemplates = true },
                shape = CircleShape,
                color = Color(0xFF1E1E1E),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Templates",
                        tint = Color(0xFFECECEC),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF161616)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(lines) { line ->
                    val color = when (line.type) {
                        LineType.System -> Color(0xFF5C5C5C)
                        LineType.Command -> Color(0xFFECECEC)
                        LineType.Output -> Color(0xFFB0B0B0)
                        LineType.Error -> Color(0xFFF87171)
                        LineType.Success -> Color(0xFF4ADE80)
                    }
                    val size = if (line.type == LineType.System) 12.sp else 13.sp

                    Text(
                        text = line.text,
                        color = color,
                        fontSize = size,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (size.value * 1.7f).sp
                    )
                }

                item {
                    if (!isBusy) {
                        Row {
                            Text(
                                text = "$ ",
                                color = Color(0xFFECECEC),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "_",
                                color = Color(0xFF4A4A4A),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF1A1A1A),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        enabled = !isBusy,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFFECECEC),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Color(0xFFECECEC)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = { runCommand(input) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (input.isEmpty()) {
                                Text(
                                    text = "Type a command…",
                                    color = Color(0xFF555555),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            Surface(
                onClick = { if (!isBusy) runCommand(input) },
                enabled = !isBusy,
                shape = CircleShape,
                color = Color(0xFFE4E4E4),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Run command",
                        tint = Color(0xFF161616),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showTemplates) {
        TemplatesSheet(
            onDismiss = { showTemplates = false },
            onFlash = { template, images -> onFlash(template, images) }
        )
    }

    LaunchedEffect(Unit) {
        delay(1200)
        if (lines.size == 1) {
            append("Try: fastboot devices", LineType.System)
        }
    }
}
