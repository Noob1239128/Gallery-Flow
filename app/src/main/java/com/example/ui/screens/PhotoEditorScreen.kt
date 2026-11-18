package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.data.media.ImageProcessingEngine
import com.example.data.model.CropAspect
import com.example.data.model.FrameStyle
import com.example.data.model.PhotoAdjustments
import com.example.data.model.PhotoFilter
import com.example.ui.components.rememberThumbnailRequest
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    photo: PhotoEntity,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSave: (PhotoAdjustments, saveAsCopy: Boolean) -> Unit
) {
    var adjustments by remember(photo.id) {
        val initial = if (photo.adjustmentsJson.isNotBlank()) {
            runCatching { Json.decodeFromString<PhotoAdjustments>(photo.adjustmentsJson) }
                .getOrDefault(PhotoAdjustments())
        } else {
            PhotoAdjustments()
        }
        mutableStateOf(initial)
    }

    var activeToolTab by remember { mutableIntStateOf(0) }
    var isComparingOriginal by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val activeAdjustments = if (isComparingOriginal) PhotoAdjustments() else adjustments
    val editorImageReq = rememberThumbnailRequest(photo.originalFilePath, maxSizePx = 1080)
    val composeColorFilter = remember(activeAdjustments) {
        val androidMatrix = ImageProcessingEngine.buildAndroidColorMatrix(activeAdjustments)
        ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foto-Editor", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Hold to compare original
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isComparingOriginal = true
                                        tryAwaitRelease()
                                        isComparingOriginal = false
                                    }
                                )
                            }
                            .testTag("editor_compare_button")
                    ) {
                        Icon(
                            Icons.Filled.Compare,
                            contentDescription = "Gedrückt halten für Vorher/Nachher",
                            tint = if (isComparingOriginal) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { adjustments = PhotoAdjustments() },
                        modifier = Modifier.testTag("editor_reset_button")
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "Zurücksetzen")
                    }
                    Button(
                        onClick = { showSaveDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_save_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sichern")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Live Canvas Preview
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0B0E))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val frameBg = when (activeAdjustments.frameStyle) {
                    FrameStyle.GALLERY_WHITE -> Color(0xFFFAF8F4)
                    FrameStyle.POLAROID -> Color(0xFFF5F2EB)
                    FrameStyle.DARK_MATTE -> Color(0xFF161820)
                    FrameStyle.NONE -> Color.Transparent
                }
                val framePad = if (activeAdjustments.frameStyle == FrameStyle.NONE) 0.dp else 14.dp
                val bottomPad = if (activeAdjustments.frameStyle == FrameStyle.POLAROID) 36.dp else framePad

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(frameBg)
                        .padding(start = framePad, end = framePad, top = framePad, bottom = 0.dp)
                ) {
                    val previewModifier = Modifier
                        .then(
                            activeAdjustments.cropAspect.ratio?.let { ratio ->
                                Modifier.aspectRatio(ratio)
                            } ?: Modifier
                        )
                        .clip(RoundedCornerShape(4.dp))

                    Box(
                        modifier = previewModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = editorImageReq,
                            contentDescription = photo.title,
                            contentScale = if (activeAdjustments.cropAspect == CropAspect.FREE)
                                ContentScale.Fit else ContentScale.Crop,
                            colorFilter = composeColorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = activeAdjustments.rotationDegrees.toFloat()
                                    scaleX = if (activeAdjustments.flipHorizontal) -1f else 1f
                                    scaleY = if (activeAdjustments.flipVertical) -1f else 1f
                                }
                        )

                        if (activeAdjustments.vignette > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = (activeAdjustments.vignette * 0.72f).coerceIn(0f, 0.85f))
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    if (activeAdjustments.frameStyle == FrameStyle.POLAROID) {
                        val dateLabel = remember(photo.takenAt) {
                            SimpleDateFormat("dd. MMM yyyy", Locale.GERMAN).format(Date(photo.takenAt))
                        }
                        Box(
                            modifier = Modifier.height(bottomPad),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF3B3A36)
                            )
                        }
                    } else if (bottomPad > 0.dp) {
                        Spacer(modifier = Modifier.height(bottomPad))
                    }
                }

                if (isComparingOriginal) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Original (Vorher)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tool Tabs
            TabRow(selectedTabIndex = activeToolTab) {
                Tab(
                    selected = activeToolTab == 0,
                    onClick = { activeToolTab = 0 },
                    text = { Text("Filter") },
                    icon = { Icon(Icons.Filled.FilterVintage, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeToolTab == 1,
                    onClick = { activeToolTab = 1 },
                    text = { Text("Licht") },
                    icon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeToolTab == 2,
                    onClick = { activeToolTab = 2 },
                    text = { Text("Zuschnitt") },
                    icon = { Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeToolTab == 3,
                    onClick = { activeToolTab = 3 },
                    text = { Text("Rahmen") },
                    icon = { Icon(Icons.Filled.FilterFrames, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tool Panels
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                when (activeToolTab) {
                    0 -> FilterStripPanel(
                        photoPath = photo.originalFilePath,
                        selected = adjustments.filter,
                        onSelect = { adjustments = adjustments.copy(filter = it) }
                    )
                    1 -> AdjustmentsSlidersPanel(
                        adjustments = adjustments,
                        onChange = { adjustments = it }
                    )
                    2 -> TransformPanel(
                        adjustments = adjustments,
                        onChange = { adjustments = it }
                    )
                    3 -> FrameStylePanel(
                        selected = adjustments.frameStyle,
                        onSelect = { adjustments = adjustments.copy(frameStyle = it) }
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Bearbeitung sichern") },
            text = { Text("Möchtest du eine neue Kopie anlegen oder das angezeigte Foto aktualisieren?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        onSave(adjustments, true)
                    },
                    modifier = Modifier.testTag("save_as_copy_button")
                ) { Text("Als Kopie speichern") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Abbrechen") }
                    OutlinedButton(onClick = {
                        showSaveDialog = false
                        onSave(adjustments, false)
                    }) { Text("Ersetzen") }
                }
            }
        )
    }
}

@Composable
private fun FilterStripPanel(
    photoPath: String,
    selected: PhotoFilter,
    onSelect: (PhotoFilter) -> Unit
) {
    val thumbReq = rememberThumbnailRequest(photoPath, maxSizePx = 220)
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(PhotoFilter.entries, key = { it.name }) { filter ->
            val isSel = filter == selected
            val thumbFilter = remember(filter) {
                val cm = ImageProcessingEngine.buildAndroidColorMatrix(PhotoAdjustments(filter = filter))
                ColorFilter.colorMatrix(ColorMatrix(cm.array))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(86.dp)
                    .clickable { onSelect(filter) }
                    .testTag("filter_item_${filter.name}")
            ) {
                AsyncImage(
                    model = thumbReq,
                    contentDescription = filter.displayName,
                    contentScale = ContentScale.Crop,
                    colorFilter = thumbFilter,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSel) 2.5.dp else 0.dp,
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = filter.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AdjustmentsSlidersPanel(
    adjustments: PhotoAdjustments,
    onChange: (PhotoAdjustments) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        EditorSliderRow("Helligkeit", adjustments.brightness, -0.4f..0.4f) {
            onChange(adjustments.copy(brightness = it))
        }
        EditorSliderRow("Kontrast", adjustments.contrast, 0.5f..1.8f) {
            onChange(adjustments.copy(contrast = it))
        }
        EditorSliderRow("Sättigung", adjustments.saturation, 0f..2f) {
            onChange(adjustments.copy(saturation = it))
        }
        EditorSliderRow("Wärme", adjustments.warmth, -0.5f..0.5f) {
            onChange(adjustments.copy(warmth = it))
        }
        EditorSliderRow("Vignette", adjustments.vignette, 0f..1f) {
            onChange(adjustments.copy(vignette = it))
        }
    }
}

@Composable
private fun EditorSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(86.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TransformPanel(
    adjustments: PhotoAdjustments,
    onChange: (PhotoAdjustments) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onChange(adjustments.copy(rotationDegrees = (adjustments.rotationDegrees + 270) % 360))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("-90°")
            }
            OutlinedButton(
                onClick = {
                    onChange(adjustments.copy(rotationDegrees = (adjustments.rotationDegrees + 90) % 360))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+90°")
            }
            OutlinedButton(
                onClick = {
                    onChange(adjustments.copy(flipHorizontal = !adjustments.flipHorizontal))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Spiegeln")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CropAspect.entries) { aspect ->
                FilterChip(
                    selected = adjustments.cropAspect == aspect,
                    onClick = { onChange(adjustments.copy(cropAspect = aspect)) },
                    label = { Text(aspect.label) }
                )
            }
        }
    }
}

@Composable
private fun FrameStylePanel(
    selected: FrameStyle,
    onSelect: (FrameStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrameStyle.entries.forEach { style ->
            val isSel = style == selected
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSel) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .weight(1f)
                    .height(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(style) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
