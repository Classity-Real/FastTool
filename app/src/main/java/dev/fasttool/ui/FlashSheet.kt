package dev.fasttool.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashSheet(
    onDismiss: () -> Unit,
    onFlash: (partition: String, imageUri: Uri) -> Unit
) {
    var partition by remember { mutableStateOf("boot") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) imageUri = uri
    }

    val common = listOf("boot", "recovery", "vbmeta", "dtbo", "system", "vendor", "userdata", "cache", "super")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Flash",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFECECEC)
            )
            Text(
                text = "Type the partition name and pick an image",
                fontSize = 13.sp,
                color = Color(0xFF767676)
            )

            OutlinedTextField(
                value = partition,
                onValueChange = { partition = it.trim() },
                label = { Text("Partition") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFECECEC),
                    unfocusedTextColor = Color(0xFFECECEC),
                    focusedBorderColor = Color(0xFF4ADE80),
                    unfocusedBorderColor = Color(0xFF444444),
                    focusedLabelColor = Color(0xFF4ADE80),
                    unfocusedLabelColor = Color(0xFF767676),
                    cursorColor = Color(0xFFECECEC)
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                common.take(5).forEach { name ->
                    FilterChip(
                        selected = partition == name,
                        onClick = { partition = name },
                        label = { Text(name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2A2A2A),
                            selectedLabelColor = Color(0xFF4ADE80),
                            containerColor = Color(0xFF222222),
                            labelColor = Color(0xFFAAAAAA)
                        )
                    )
                }
            }

            Surface(
                onClick = { picker.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF222222),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Image file", color = Color(0xFFECECEC), fontWeight = FontWeight.Medium)
                        Text(
                            text = imageUri?.lastPathSegment ?: "Tap to select .img",
                            color = if (imageUri != null) Color(0xFF4ADE80) else Color(0xFF767676),
                            fontSize = 12.sp
                        )
                    }
                    if (imageUri != null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val uri = imageUri
                    val part = partition.trim()
                    if (uri != null && part.isNotEmpty()) {
                        onFlash(part, uri)
                        onDismiss()
                    }
                },
                enabled = imageUri != null && partition.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE4E4E4),
                    contentColor = Color(0xFF161616),
                    disabledContainerColor = Color(0xFF333333),
                    disabledContentColor = Color(0xFF666666)
                )
            ) {
                Text("Flash $partition", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
