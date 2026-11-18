package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AlbumWithPhotos
import com.example.data.local.PhotoEntity
import com.example.data.local.UserPreferences
import com.example.data.media.ImageProcessingEngine
import com.example.data.model.GridLayoutMode
import com.example.data.model.StoryHighlight
import com.example.ui.components.AddToAlbumSheetDialog
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.LuminaEmptyState
import com.example.ui.components.PhotoThumbnailCard
import com.example.ui.components.rememberThumbnailRequest
import com.example.ui.components.sharePhotosWithSystem
import java.util.Calendar

private data class PhotoSection(
    val title: String,
    val photos: List<PhotoEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    photos: List<PhotoEntity>,
    stories: List<StoryHighlight>,
    albums: List<AlbumWithPhotos>,
    preferences: UserPreferences,
    selectedIds: Set<Long>,
    isBusy: Boolean,
    imageEngine: ImageProcessingEngine,
    onOpenPhoto: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onClearSelection: () -> Unit,
    onImportPhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onStartSlideshow: (List<Long>) -> Unit,
    onToggleLayoutMode: () -> Unit,
    onCycleColumns: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onAddSelectedToAlbum: (Long) -> Unit,
    onCreateAlbumWithSelected: (String, String) -> Unit,
    onMoveSelectedToVault: () -> Unit,
    onMoveSelectedToTrash: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val isSelectionMode = selectedIds.isNotEmpty()
    var showAlbumPicker by remember { mutableStateOf(false) }
    var showCreateAlbum by remember { mutableStateOf(false) }

    if (isSelectionMode) {
        BackHandler { onClearSelection() }
    }

    val sections = remember(photos) { groupPhotosByDate(photos) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            TopAppBar(
                title = {
                    Text(
                        text = "${selectedIds.size} ausgewählt",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Filled.Close, contentDescription = "Auswahl aufheben")
                    }
                },
                actions = {
                    IconButton(onClick = { onSelectAll(photos.map { it.id }) }) {
                        Icon(Icons.Filled.SelectAll, contentDescription = "Alle auswählen")
                    }
                    IconButton(onClick = onFavoriteSelected) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = "Als Favorit markieren")
                    }
                    IconButton(onClick = { showAlbumPicker = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "Zu Album hinzufügen")
                    }
                    IconButton(
                        onClick = {
                            val selected = photos.filter { it.id in selectedIds }
                            sharePhotosWithSystem(context, imageEngine, selected)
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Teilen")
                    }
                    IconButton(onClick = onMoveSelectedToVault) {
                        Icon(Icons.Filled.Lock, contentDescription = "In Tresor verschieben")
                    }
                    IconButton(onClick = onMoveSelectedToTrash) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "In Papierkorb verschieben",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        } else {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lumina",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${photos.size} Aufnahmen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onTakePhoto,
                        modifier = Modifier.testTag("camera_capture_button")
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Foto mit Kamera aufnehmen")
                    }
                    IconButton(
                        onClick = onImportPhotos,
                        modifier = Modifier.testTag("import_photos_button")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Fotos importieren")
                    }
                    if (photos.isNotEmpty()) {
                        IconButton(
                            onClick = { onStartSlideshow(photos.map { it.id }) },
                            modifier = Modifier.testTag("slideshow_all_button")
                        ) {
                            Icon(Icons.Filled.Slideshow, contentDescription = "Diashow starten")
                        }
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }

        AnimatedVisibility(visible = isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (photos.isEmpty()) {
            LuminaEmptyState(
                icon = Icons.Filled.PhotoLibrary,
                title = "Deine Galerie ist noch leer",
                subtitle = "Importiere Fotos vom Gerät oder nimm direkt ein neues Bild mit der Kamera auf.",
                actionLabel = "Fotos importieren",
                onAction = onImportPhotos,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Layout controls strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = preferences.gridLayoutMode == GridLayoutMode.MASONRY,
                    onClick = onToggleLayoutMode,
                    label = {
                        Text(
                            if (preferences.gridLayoutMode == GridLayoutMode.MASONRY) "Wasserfall"
                            else "Quadrat-Raster"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (preferences.gridLayoutMode == GridLayoutMode.MASONRY)
                                Icons.Filled.Dashboard else Icons.Filled.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("layout_toggle_chip")
                )
                FilterChip(
                    selected = false,
                    onClick = onCycleColumns,
                    label = { Text("${preferences.gridColumns} Spalten") },
                    modifier = Modifier.testTag("columns_cycle_chip")
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Gedrückt halten = Auswahl",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (preferences.gridLayoutMode == GridLayoutMode.MASONRY) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(preferences.gridColumns),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalItemSpacing = 10.dp,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (stories.isNotEmpty() && !isSelectionMode) {
                        item(
                            span = StaggeredGridItemSpan.FullLine,
                            key = "stories_header",
                            contentType = "stories"
                        ) {
                            StoriesStrip(
                                stories = stories,
                                onStoryClick = { onStartSlideshow(it.photoIds) }
                            )
                        }
                    }

                    sections.forEach { section ->
                        item(
                            span = StaggeredGridItemSpan.FullLine,
                            key = "hdr_${section.title}",
                            contentType = "date_header"
                        ) {
                            SectionDateHeader(title = section.title, count = section.photos.size)
                        }
                        items(
                            items = section.photos,
                            key = { it.id },
                            contentType = { "photo" }
                        ) { photo ->
                            PhotoThumbnailCard(
                                photo = photo,
                                isSelectionMode = isSelectionMode,
                                isSelected = photo.id in selectedIds,
                                useNaturalAspect = true,
                                onClick = {
                                    if (isSelectionMode) onToggleSelect(photo.id)
                                    else onOpenPhoto(photo.id)
                                },
                                onLongClick = { onToggleSelect(photo.id) }
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(preferences.gridColumns),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (stories.isNotEmpty() && !isSelectionMode) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "stories_header",
                            contentType = "stories"
                        ) {
                            StoriesStrip(
                                stories = stories,
                                onStoryClick = { onStartSlideshow(it.photoIds) }
                            )
                        }
                    }

                    sections.forEach { section ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "hdr_${section.title}",
                            contentType = "date_header"
                        ) {
                            SectionDateHeader(title = section.title, count = section.photos.size)
                        }
                        items(
                            items = section.photos,
                            key = { it.id },
                            contentType = { "photo" }
                        ) { photo ->
                            PhotoThumbnailCard(
                                photo = photo,
                                isSelectionMode = isSelectionMode,
                                isSelected = photo.id in selectedIds,
                                useNaturalAspect = false,
                                onClick = {
                                    if (isSelectionMode) onToggleSelect(photo.id)
                                    else onOpenPhoto(photo.id)
                                },
                                onLongClick = { onToggleSelect(photo.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAlbumPicker) {
        AddToAlbumSheetDialog(
            albums = albums,
            onDismiss = { showAlbumPicker = false },
            onSelectAlbum = { albumId ->
                showAlbumPicker = false
                onAddSelectedToAlbum(albumId)
            },
            onCreateNewAlbum = {
                showAlbumPicker = false
                showCreateAlbum = true
            }
        )
    }

    if (showCreateAlbum) {
        CreateAlbumDialog(
            onDismiss = { showCreateAlbum = false },
            onConfirm = { name, desc ->
                showCreateAlbum = false
                onCreateAlbumWithSelected(name, desc)
            }
        )
    }
}

@Composable
private fun StoriesStrip(
    stories: List<StoryHighlight>,
    onStoryClick: (StoryHighlight) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "Rückblicke & Momente",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stories, key = { it.id }, contentType = { "story" }) { story ->
                val storyCoverRequest = rememberThumbnailRequest(story.coverPhotoPath, maxSizePx = 420)
                Box(
                    modifier = Modifier
                        .width(164.dp)
                        .height(106.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onStoryClick(story) }
                        .testTag("story_card_${story.id}")
                ) {
                    AsyncImage(
                        model = storyCoverRequest,
                        contentDescription = story.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.05f),
                                        Color.Black.copy(alpha = 0.78f)
                                    )
                                )
                            )
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Diashow abspielen",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = story.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionDateHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun groupPhotosByDate(photos: List<PhotoEntity>): List<PhotoSection> {
    if (photos.isEmpty()) return emptyList()
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = cal.timeInMillis
    val weekStart = todayStart - 7L * 24L * 60L * 60L * 1000L

    val today = photos.filter { it.takenAt >= todayStart }
    val thisWeek = photos.filter { it.takenAt in weekStart until todayStart }
    val older = photos.filter { it.takenAt < weekStart }

    return buildList {
        if (today.isNotEmpty()) add(PhotoSection("Heute", today))
        if (thisWeek.isNotEmpty()) add(PhotoSection("Diese Woche", thisWeek))
        if (older.isNotEmpty()) add(PhotoSection("Ältere Aufnahmen", older))
    }
}
