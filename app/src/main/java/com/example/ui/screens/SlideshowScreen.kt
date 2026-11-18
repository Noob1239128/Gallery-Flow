package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.ui.components.rememberThumbnailRequest
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SlideshowScreen(
    photos: List<PhotoEntity>,
    startIndex: Int,
    initialIntervalSec: Int,
    onBack: () -> Unit
) {
    if (photos.isEmpty()) {
        onBack()
        return
    }

    var currentIndex by remember { mutableIntStateOf(startIndex.coerceIn(0, photos.lastIndex)) }
    var isPlaying by remember { mutableStateOf(true) }
    var intervalSec by remember { mutableIntStateOf(initialIntervalSec.coerceIn(2, 8)) }

    LaunchedEffect(isPlaying, currentIndex, intervalSec, photos.size) {
        if (isPlaying && photos.size > 1) {
            delay(intervalSec * 1000L)
            currentIndex = (currentIndex + 1) % photos.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "kenBurns")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing)
        ),
        label = "scale"
    )

    val currentPhoto = photos[currentIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Crossfade(
            targetState = currentPhoto,
            animationSpec = tween(durationMillis = 800),
            label = "slideshowCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { photo ->
            val slideReq = rememberThumbnailRequest(photo.filePath, maxSizePx = 1600)
            AsyncImage(
                model = slideReq,
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isPlaying) {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                        }
                    }
            )
        }

        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / photos.size.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Diashow beenden",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${currentIndex + 1} / ${photos.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.16f)
                    ) {
                        TextButton(
                            onClick = {
                                intervalSec = when (intervalSec) {
                                    2 -> 4
                                    4 -> 6
                                    else -> 2
                                }
                            }
                        ) {
                            Text("${intervalSec}s", color = Color.White)
                        }
                    }
                }
            }
        }

        // Bottom Title & Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = currentPhoto.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                val dateStr = remember(currentPhoto.takenAt) {
                    SimpleDateFormat("dd. MMMM yyyy", Locale.GERMAN).format(Date(currentPhoto.takenAt))
                }
                Text(
                    text = listOf(currentPhoto.locationName, dateStr)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            currentIndex = if (currentIndex == 0) photos.lastIndex else currentIndex - 1
                        }
                    ) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Zurück", tint = Color.White)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Abspielen",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = { currentIndex = (currentIndex + 1) % photos.size }
                    ) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Weiter", tint = Color.White)
                    }
                }
            }
        }
    }
}
