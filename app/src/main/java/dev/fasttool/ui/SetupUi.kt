package dev.fasttool.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.modifier.modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.fasttool.RunMethod

@Composable
fun SetupFadeOverlay(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

@Composable
fun SetupOverlay(
    phase: SetupPhase,
    selectedMethod: RunMethod,
    workDir: String,
    onSelectMethod: (RunMethod) -> Unit,
    onPickDirectory: () -> Unit,
    onWorkDirTyped: (String) -> Unit,
    onContinueDirectory: () -> Unit,
    onAcceptWarning: () -> Unit,
    onDeclineWarning: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0A0A0A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (phase) {
                SetupPhase.Method -> {
                    Text(
                        "Which method are you using?",
                        color = Color(0xFFECECEC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    SetupChoice(
                        title = "Root",
                        subtitle = "Uses su when available",
                        selected = selectedMethod == RunMethod.Root,
                        onClick = { onSelectMethod(RunMethod.Root) }
                    )
                    SetupChoice(
                        title = "Normal",
                        subtitle = "No root — OTG host only",
                        selected = selectedMethod == RunMethod.Normal,
                        onClick = { onSelectMethod(RunMethod.Normal) }
                    )
                }
                SetupPhase.Directory -> {
                    Text(
                        "Select a directory for backup / restore",
                        color = Color(0xFFECECEC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = workDir,
                        onValueChange = onWorkDirTyped,
                        placeholder = { Text("Path or pick a folder", color = Color(0xFF666666)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFECECEC),
                            unfocusedTextColor = Color(0xFFECECEC),
                            focusedBorderColor = Color(0xFF4ADE80),
                            unfocusedBorderColor = Color(0xFF333333),
                            cursorColor = Color(0xFFECECEC)
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = onPickDirectory) {
                            Text("Browse", color = Color(0xFF4ADE80))
                        }
                        Button(
                            onClick = onContinueDirectory,
                            enabled = workDir.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE4E4E4),
                                contentColor = Color(0xFF121212)
                            )
                        ) {
                            Text("Continue")
                        }
                    }
                }
                SetupPhase.Warning -> {
                    Text(
                        "Warning",
                        color = Color(0xFFF87171),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "By continuing you agree that FastTool can potentially brick your device " +
                            "through your actions. The owner has nothing to do with any damage caused.",
                        color = Color(0xFFB0B0B0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = onDeclineWarning) {
                            Text("Cancel", color = Color(0xFF888888))
                        }
                        Button(
                            onClick = onAcceptWarning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE4E4E4),
                                contentColor = Color(0xFF121212)
                            )
                        ) {
                            Text("I understand")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SetupChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFF1E2A1E) else Color(0xFF1A1A1A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = Color(0xFFECECEC), fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF767676), fontSize = 12.sp)
        }
    }
}
