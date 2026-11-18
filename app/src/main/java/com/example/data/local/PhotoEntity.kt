package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ColorCategory

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val caption: String = "",
    val filePath: String,
    val originalFilePath: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val takenAt: Long,
    val addedAt: Long,
    val locationName: String = "",
    val cameraModel: String = "Lumina Kamera",
    val lensSpec: String = "28mm f/1.8",
    val aperture: String = "f/2.0",
    val shutterSpeed: String = "1/250s",
    val iso: Int = 100,
    val tagsCsv: String = "",
    val paletteCsv: String = "#F59E0B,#1E293B,#64748B",
    val colorCategory: String = ColorCategory.ALL.name,
    val isFavorite: Boolean = false,
    val isInVault: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val isEdited: Boolean = false,
    val isCollage: Boolean = false,
    val sourceType: String = "CURATED",
    val adjustmentsJson: String = ""
) {
    val aspectRatio: Float
        get() = if (height > 0) (width.toFloat() / height.toFloat()).coerceIn(0.45f, 2.2f) else 1f

    val tagsList: List<String>
        get() = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val paletteHexes: List<String>
        get() = paletteCsv.split(",").map { it.trim() }.filter { it.startsWith("#") }
}
