package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.data.model.CollageLayout
import com.example.ui.components.rememberThumbnailRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    photos: List<PhotoEntity>,
    isBusy: Boolean,
    onOpenEditor: (Long) -> Unit,
    onCreateCollage: (
        selectedPhotos: List<PhotoEntity>,
        layout: CollageLayout,
        spacingPx: Float,
        cornerRadiusPx: Float,
        bgColor: Int
    ) -> Unit
) {
    var selectedLayout by remember { mutableStateOf(CollageLayout.TWO_SPLIT) }
    var selectedIds by remember(photos) {
        mutableStateOf(photos.take(2).map { it.id })
    }
    var spacingDp by remember { mutableFloatStateOf(24f) }
    var cornerDp by remember { mutableFloatStateOf(28f) }

    val canvasColors = remember {
        listOf(
            Color(0xFFFAF8F4),
            Color(0xFF14161D),
            Color(0xFF2B2118),
            Color(0xFF132625),
            Color(0xFFF59E0B)
        )
    }
    var selectedBgIndex by remember { mutableIntStateOf(0) }

    val requiredCount = selectedLayout.requiredCount
    val chosenPhotos = remember(selectedIds, photos, requiredCount) {
        selectedIds.mapNotNull { id -> photos.find { it.id == id } }.take(requiredCount)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Kreativ-Studio",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Kollagen bauen & Fotos veredeln",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        if (isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Section 1: Quick Launch Foto-Editor
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Foto-Editor öffnen",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Wähle ein Foto für Filter, Helligkeit, Kontrast, Vignette, Zuschnitt und Polaroid-Rahmen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(photos.take(12), key = { it.id }, contentType = { "studio_edit" }) { photo ->
                            val thumbReq = rememberThumbnailRequest(photo.filePath, maxSizePx = 240)
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenEditor(photo.id) }
                                    .testTag("studio_edit_${photo.id}")
                            ) {
                                AsyncImage(
                                    model = thumbReq,
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Collage Studio
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GridView,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kollage-Studio",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Layout selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CollageLayout.entries) { layout ->
                            FilterChip(
                                selected = layout == selectedLayout,
                                onClick = {
                                    selectedLayout = layout
                                    if (selectedIds.size < layout.requiredCount) {
                                        val extra = photos.map { it.id }.filter { it !in selectedIds }
                                        selectedIds = (selectedIds + extra).take(layout.requiredCount)
                                    } else {
                                        selectedIds = selectedIds.take(layout.requiredCount)
                                    }
                                },
                                label = { Text(layout.label) },
                                modifier = Modifier.testTag("collage_layout_${layout.name}")
                            )
                        }
                    }

                    // Live Collage Preview
                    CollageLivePreview(
                        photos = chosenPhotos,
                        layout = selectedLayout,
                        spacingDp = spacingDp / 3f,
                        cornerDp = cornerDp / 3f,
                        bgColor = canvasColors[selectedBgIndex]
                    )

                    // Photo picker strip for collage
                    Text(
                        text = "Fotos wählen (${chosenPhotos.size}/$requiredCount)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photos, key = { it.id }, contentType = { "collage_pick" }) { photo ->
                            val order = selectedIds.indexOf(photo.id)
                            val isSelected = order >= 0
                            val pickReq = rememberThumbnailRequest(photo.filePath, maxSizePx = 220)
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedIds = if (isSelected) {
                                            selectedIds - photo.id
                                        } else {
                                            if (selectedIds.size >= requiredCount) {
                                                selectedIds.drop(1) + photo.id
                                            } else {
                                                selectedIds + photo.id
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = pickReq,
                                    contentDescription = photo.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${order + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Controls: Spacing & Corners & Background
                    Column {
                        Text(
                            text = "Abstand: ${spacingDp.toInt()} px",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = spacingDp,
                            onValueChange = { spacingDp = it },
                            valueRange = 0f..60f
                        )
                        Text(
                            text = "Ecken-Radius: ${cornerDp.toInt()} px",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = cornerDp,
                            onValueChange = { cornerDp = it },
                            valueRange = 0f..64f
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Hintergrund:", style = MaterialTheme.typography.bodySmall)
                        canvasColors.forEachIndexed { idx, col ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (idx == selectedBgIndex) 2.5.dp else 1.dp,
                                        color = if (idx == selectedBgIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedBgIndex = idx }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onCreateCollage(
                                chosenPhotos,
                                selectedLayout,
                                spacingDp,
                                cornerDp,
                                canvasColors[selectedBgIndex].toArgb()
                            )
                        },
                        enabled = chosenPhotos.size >= requiredCount && !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_collage_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kollage erstellen & speichern")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CollageLivePreview(
    photos: List<PhotoEntity>,
    layout: CollageLayout,
    spacingDp: Float,
    cornerDp: Float,
    bgColor: Color
) {
    val gap = spacingDp.dp
    val radius = cornerDp.dp
    val aspect = when (layout) {
        CollageLayout.TWO_SPLIT -> 4f / 3f
        CollageLayout.TWO_STACK -> 8f / 9f
        CollageLayout.THREE_EDITORIAL -> 1f
        CollageLayout.FOUR_GRID -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect.coerceIn(0.8f, 1.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(gap)
    ) {
        when (layout) {
            CollageLayout.TWO_SPLIT -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    CollageCell(photos.getOrNull(0), radius, Modifier.weight(1f).fillMaxHeight())
                    CollageCell(photos.getOrNull(1), radius, Modifier.weight(1f).fillMaxHeight())
                }
            }
            CollageLayout.TWO_STACK -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    CollageCell(photos.getOrNull(0), radius, Modifier.weight(1f).fillMaxWidth())
                    CollageCell(photos.getOrNull(1), radius, Modifier.weight(1f).fillMaxWidth())
                }
            }
            CollageLayout.THREE_EDITORIAL -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    CollageCell(photos.getOrNull(0), radius, Modifier.weight(1.15f).fillMaxHeight())
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        CollageCell(photos.getOrNull(1), radius, Modifier.weight(1f).fillMaxWidth())
                        CollageCell(photos.getOrNull(2), radius, Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            CollageLayout.FOUR_GRID -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        CollageCell(photos.getOrNull(0), radius, Modifier.weight(1f).fillMaxHeight())
                        CollageCell(photos.getOrNull(1), radius, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        CollageCell(photos.getOrNull(2), radius, Modifier.weight(1f).fillMaxHeight())
                        CollageCell(photos.getOrNull(3), radius, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageCell(
    photo: PhotoEntity?,
    radius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(Color.Gray.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            val cellReq = rememberThumbnailRequest(photo.filePath, maxSizePx = 480)
            AsyncImage(
                model = cellReq,
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
