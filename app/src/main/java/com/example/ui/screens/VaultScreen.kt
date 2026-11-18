package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.PhotoEntity
import com.example.ui.components.LuminaEmptyState
import com.example.ui.components.PhotoThumbnailCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    isPinSet: Boolean,
    isUnlocked: Boolean,
    vaultPhotos: List<PhotoEntity>,
    onSetupPin: (String) -> Unit,
    onTryUnlock: (String, (Boolean) -> Unit) -> Unit,
    onLockVault: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onRestoreFromVault: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Privater Tresor",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isUnlocked) "${vaultPhotos.size} geschützte Fotos" else "PIN-geschützter Bereich",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = {
                if (isUnlocked) {
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier.testTag("lock_vault_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Tresor sperren")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        if (!isUnlocked) {
            VaultPinGate(
                isPinSet = isPinSet,
                onSetupPin = onSetupPin,
                onTryUnlock = onTryUnlock,
                modifier = Modifier.weight(1f)
            )
        } else {
            if (vaultPhotos.isEmpty()) {
                LuminaEmptyState(
                    icon = Icons.Filled.Shield,
                    title = "Tresor ist leer",
                    subtitle = "Wähle Fotos in der Galerie aus und tippe auf das Schloss-Symbol, um sie hier sicher zu verbergen.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = "Tippen zum Ansehen · Gedrückt halten zum Entfernen aus dem Tresor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vaultPhotos, key = { it.id }) { photo ->
                        PhotoThumbnailCard(
                            photo = photo,
                            isSelectionMode = false,
                            isSelected = false,
                            useNaturalAspect = false,
                            onClick = { onOpenPhoto(photo.id) },
                            onLongClick = { onRestoreFromVault(photo.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultPinGate(
    isPinSet: Boolean,
    onSetupPin: (String) -> Unit,
    onTryUnlock: (String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var firstSetupPin by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val headline = when {
        !isPinSet && firstSetupPin == null -> "4-stelligen PIN festlegen"
        !isPinSet && firstSetupPin != null -> "PIN zur Bestätigung wiederholen"
        else -> "Tresor entsperren"
    }

    fun handleDigit(d: String) {
        if (enteredPin.length >= 4) return
        errorMsg = null
        val updated = enteredPin + d
        enteredPin = updated
        if (updated.length == 4) {
            if (!isPinSet) {
                val first = firstSetupPin
                if (first == null) {
                    firstSetupPin = updated
                    enteredPin = ""
                } else if (first == updated) {
                    onSetupPin(updated)
                    enteredPin = ""
                    firstSetupPin = null
                } else {
                    errorMsg = "PINs stimmen nicht überein. Erneut versuchen."
                    firstSetupPin = null
                    enteredPin = ""
                }
            } else {
                onTryUnlock(updated) { ok ->
                    if (!ok) {
                        errorMsg = "Falscher PIN"
                        enteredPin = ""
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPinSet) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMsg ?: if (!isPinSet) "Schützt private Bilder vor fremden Blicken"
            else "Gib deinen 4-stelligen PIN ein",
            style = MaterialTheme.typography.bodySmall,
            color = if (errorMsg != null) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        // 4 PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { idx ->
                val filled = idx < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Keypad
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "" -> Spacer(modifier = Modifier.size(68.dp))
                            "DEL" -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Löschen"
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable { handleDigit(key) }
                                        .testTag("pin_key_$key")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
