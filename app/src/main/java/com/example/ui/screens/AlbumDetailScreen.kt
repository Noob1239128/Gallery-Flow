package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AlbumWithPhotos
import com.example.data.local.PhotoEntity
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.LuminaEmptyState
import com.example.ui.components.PhotoThumbnailCard
import com.example.ui.components.rememberThumbnailRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    title: String,
    subtitle: String,
    photos: List<PhotoEntity>,
    allGalleryPhotos: List<PhotoEntity>,
    isEditableAlbum: Boolean,
    albumEntity: AlbumWithPhotos? = null,
    onBack: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onStartSlideshow: (List<Long>) -> Unit,
    onRenameAlbum: (String, String) -> Unit = { _, _ -> },
    onDeleteAlbum: () -> Unit = {},
    onAddPhotosToAlbum: (List<Long>) -> Unit = {},
    onRemoveFromAlbum: (Long) -> Unit = {}
) {
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddPhotos by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${photos.size} Fotos" + if (subtitle.isNotBlank()) " · $subtitle" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { onStartSlideshow(photos.map { it.id }) }) {
                            Icon(Icons.Filled.Slideshow, contentDescription = "Diashow starten")
                        }
                    }
                    if (isEditableAlbum) {
                        IconButton(onClick = { showAddPhotos = true }) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Fotos hinzufügen")
                        }
                        IconButton(onClick = { showRename = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Album umbenennen")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Album löschen",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (photos.isEmpty()) {
            LuminaEmptyState(
                icon = Icons.Filled.PhotoLibrary,
                title = "Keine Fotos in diesem Album",
                subtitle = if (isEditableAlbum) "Füge bestehende Fotos aus deiner Galerie hinzu."
                else "Hier erscheinen automatisch passende Aufnahmen.",
                actionLabel = if (isEditableAlbum) "Fotos hinzufügen" else null,
                onAction = if (isEditableAlbum) {
                    { showAddPhotos = true }
                } else null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoThumbnailCard(
                        photo = photo,
                        isSelectionMode = false,
                        isSelected = false,
                        useNaturalAspect = false,
                        onClick = { onOpenPhoto(photo.id) },
                        onLongClick = {
                            if (isEditableAlbum) onRemoveFromAlbum(photo.id)
                        }
                    )
                }
            }
        }
    }

    if (showRename && albumEntity != null) {
        CreateAlbumDialog(
            initialName = albumEntity.album.name,
            initialDescription = albumEntity.album.description,
            dialogTitle = "Album bearbeiten",
            confirmLabel = "Speichern",
            onDismiss = { showRename = false },
            onConfirm = { newName, newDesc ->
                showRename = false
                onRenameAlbum(newName, newDesc)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Album löschen?") },
            text = { Text("Das Album wird entfernt. Die Fotos bleiben in deiner Galerie erhalten.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDeleteAlbum()
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }

    if (showAddPhotos) {
        val existingIds = photos.map { it.id }.toSet()
        val candidates = allGalleryPhotos.filter { it.id !in existingIds }
        var picked by remember { mutableStateOf(setOf<Long>()) }

        AlertDialog(
            onDismissRequest = { showAddPhotos = false },
            title = { Text("Fotos auswählen (${picked.size})") },
            text = {
                if (candidates.isEmpty()) {
                    Text("Alle verfügbaren Fotos befinden sich bereits in diesem Album.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        items(candidates, key = { it.id }) { candidate ->
                            val isSel = candidate.id in picked
                            val candidateReq = rememberThumbnailRequest(candidate.filePath, maxSizePx = 280)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        picked = if (isSel) picked - candidate.id else picked + candidate.id
                                    }
                            ) {
                                AsyncImage(
                                    model = candidateReq,
                                    contentDescription = candidate.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isSel) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
                                )
                                Icon(
                                    imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSel) MaterialTheme.colorScheme.primary else Color.White,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddPhotos = false
                        if (picked.isNotEmpty()) onAddPhotosToAlbum(picked.toList())
                    },
                    enabled = picked.isNotEmpty()
                ) { Text("Hinzufügen") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhotos = false }) { Text("Abbrechen") }
            }
        )
    }
}
