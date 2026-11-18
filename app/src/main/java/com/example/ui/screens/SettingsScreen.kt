package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.UserPreferences
import com.example.data.model.GridLayoutMode
import com.example.data.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetGridLayout: (GridLayoutMode) -> Unit,
    onSetColumns: (Int) -> Unit,
    onSetSlideshowInterval: (Int) -> Unit,
    onResetVaultPin: () -> Unit,
    onRestoreDemoPhotos: () -> Unit
) {
    var showPinResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance
            SettingsSectionCard(title = "Erscheinungsbild") {
                Text(
                    text = "Farbschema",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { onSetTheme(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Standard-Ansicht",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GridLayoutMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.gridLayoutMode == mode,
                            onClick = { onSetGridLayout(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text(
                    text = "Spaltenanzahl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4).forEach { cols ->
                        FilterChip(
                            selected = preferences.gridColumns == cols,
                            onClick = { onSetColumns(cols) },
                            label = { Text("$cols Spalten") }
                        )
                    }
                }
            }

            // Slideshow
            SettingsSectionCard(title = "Diashow") {
                Text(
                    text = "Anzeigedauer pro Foto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 4, 6).forEach { sec ->
                        FilterChip(
                            selected = preferences.slideshowIntervalSec == sec,
                            onClick = { onSetSlideshowInterval(sec) },
                            label = { Text("$sec Sek.") }
                        )
                    }
                }
            }

            // Vault & Library
            SettingsSectionCard(title = "Sicherheit & Mediathek") {
                if (preferences.isVaultPinSet) {
                    OutlinedButton(
                        onClick = { showPinResetConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tresor-PIN zurücksetzen")
                    }
                } else {
                    Text(
                        text = "Noch kein Tresor-PIN festgelegt (im Tab 'Tresor' einrichten).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onRestoreDemoPhotos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kuratierte Demo-Fotos wiederherstellen")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lumina Galerie · Alle Fotos und Alben werden lokal und privat auf deinem Gerät gespeichert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPinResetConfirm) {
        AlertDialog(
            onDismissRequest = { showPinResetConfirm = false },
            title = { Text("Tresor-PIN zurücksetzen?") },
            text = { Text("Beim nächsten Öffnen des Tresors kannst du einen neuen 4-stelligen PIN festlegen.") },
            confirmButton = {
                Button(onClick = {
                    showPinResetConfirm = false
                    onResetVaultPin()
                }) { Text("Zurücksetzen") }
            },
            dismissButton = {
                TextButton(onClick = { showPinResetConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
