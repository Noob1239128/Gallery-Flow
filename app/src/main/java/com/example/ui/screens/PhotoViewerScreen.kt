package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.data.media.ImageProcessingEngine
import com.example.ui.components.rememberThumbnailRequest
import com.example.ui.components.sharePhotosWithSystem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photos: List<PhotoEntity>,
    initialPhotoId: Long,
    imageEngine: ImageProcessingEngine,
    onBack: () -> Unit,
    onToggleFavorite: (PhotoEntity) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onStartSlideshow: (List<Long>, Int) -> Unit,
    onToggleVault: (PhotoEntity) -> Unit,
    onMoveToTrash: (Long) -> Unit,
    onUpdateMetadata: (PhotoEntity, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    if (photos.isEmpty()) {
        onBack()
        return
    }

    val initialPage = remember(photos, initialPhotoId) {
        photos.indexOfFirst { it.id == initialPhotoId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { photos.size })
    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()

    var showChrome by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { photos[it].id },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = photos[page]
            ZoomablePhotoPage(
                photo = photo,
                onTap = { showChrome = !showChrome }
            )
        }

        // Top Chrome
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentPhoto.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val dateText = remember(currentPhoto.takenAt) {
                            SimpleDateFormat("dd. MMM yyyy · HH:mm", Locale.GERMAN)
                                .format(Date(currentPhoto.takenAt))
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                    IconButton(
                        onClick = { showInfoSheet = true },
                        modifier = Modifier.testTag("viewer_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "EXIF & Infos",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Action Chrome
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerActionItem(
                        icon = if (currentPhoto.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        label = "Favorit",
                        tint = if (currentPhoto.isFavorite) MaterialTheme.colorScheme.tertiary else Color.White,
                        onClick = { onToggleFavorite(currentPhoto) },
                        testTag = "viewer_favorite_button"
                    )
                    ViewerActionItem(
                        icon = Icons.Filled.Tune,
                        label = "Editor",
                        onClick = { onOpenEditor(currentPhoto.id) },
                        testTag = "viewer_edit_button"
                    )
                    ViewerActionItem(
                        icon = Icons.Filled.Slideshow,
                        label = "Diashow",
                        onClick = {
                            onStartSlideshow(photos.map { it.id }, pagerState.currentPage)
                        },
                        testTag = "viewer_slideshow_button"
                    )
                    ViewerActionItem(
                        icon = Icons.Filled.Share,
                        label = "Teilen",
                        onClick = { sharePhotosWithSystem(context, imageEngine, listOf(currentPhoto)) },
                        testTag = "viewer_share_button"
                    )
                    ViewerActionItem(
                        icon = if (currentPhoto.isInVault) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        label = if (currentPhoto.isInVault) "Freigeben" else "Tresor",
                        onClick = {
                            onToggleVault(currentPhoto)
                            if (photos.size <= 1) onBack()
                        },
                        testTag = "viewer_vault_button"
                    )
                    ViewerActionItem(
                        icon = Icons.Filled.DeleteOutline,
                        label = "Löschen",
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            onMoveToTrash(currentPhoto.id)
                            if (photos.size <= 1) onBack()
                        },
                        testTag = "viewer_trash_button"
                    )
                }
            }
        }
    }

    if (showInfoSheet) {
        PhotoExifBottomSheet(
            photo = currentPhoto,
            onDismiss = { showInfoSheet = false },
            onSaveMetadata = { title, caption, location, tags ->
                onUpdateMetadata(currentPhoto, title, caption, location, tags)
                showInfoSheet = false
            }
        )
    }
}

@Composable
private fun ZoomablePhotoPage(
    photo: PhotoEntity,
    onTap: () -> Unit
) {
    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        offset = if (newScale > 1.05f) {
            offset + panChange
        } else {
            Offset.Zero
        }
    }

    val fullImageReq = rememberThumbnailRequest(photo.filePath, maxSizePx = 1800)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.4f
                        }
                    }
                )
            }
            .then(
                if (scale > 1.02f) Modifier.transformable(state = transformState)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = fullImageReq,
            contentDescription = photo.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

@Composable
private fun ViewerActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp)
            .testTag(testTag)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = tint)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoExifBottomSheet(
    photo: PhotoEntity,
    onDismiss: () -> Unit,
    onSaveMetadata: (title: String, caption: String, location: String, tagsCsv: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isEditing by remember { mutableStateOf(false) }
    var title by remember(photo.id) { mutableStateOf(photo.title) }
    var caption by remember(photo.id) { mutableStateOf(photo.caption) }
    var location by remember(photo.id) { mutableStateOf(photo.locationName) }
    var tags by remember(photo.id) { mutableStateOf(photo.tagsCsv) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Details & EXIF", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { isEditing = !isEditing }) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEditing) "Abbrechen" else "Infos bearbeiten")
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Beschreibung") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ort") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (mit Komma trennen)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveMetadata(title, caption, location, tags) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Änderungen speichern")
                }
            } else {
                Text(photo.title, style = MaterialTheme.typography.titleMedium)
                if (photo.caption.isNotBlank()) {
                    Text(
                        text = photo.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (photo.locationName.isNotBlank()) {
                    Text(
                        text = "Ort: ${photo.locationName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (photo.tagsList.isNotEmpty()) {
                    Text(
                        text = photo.tagsList.joinToString("  ") { "#$it" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            HorizontalDivider()

            // Extracted Color Palette Swatches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dominante Farbpalette", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    photo.paletteHexes.forEach { hex ->
                        val swatch = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(hex, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            HorizontalDivider()

            // EXIF Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Camera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(photo.cameraModel, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = "${photo.lensSpec} · ${photo.aperture} · ${photo.shutterSpeed} · ISO ${photo.iso}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val sizeKb = (photo.fileSizeBytes / 1024f)
                    val sizeStr = if (sizeKb > 1024f) {
                        String.format(Locale.GERMAN, "%.2f MB", sizeKb / 1024f)
                    } else {
                        "${sizeKb.toInt()} KB"
                    }
                    Text(
                        text = "${photo.width} × ${photo.height} px · $sizeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
