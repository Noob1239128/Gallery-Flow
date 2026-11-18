package com.example.data.model

import kotlinx.serialization.Serializable

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dunkel"),
    LIGHT("Hell")
}

enum class GridLayoutMode(val label: String) {
    MASONRY("Wasserfall"),
    SQUARE("Raster")
}

enum class PhotoFilter(val displayName: String) {
    ORIGINAL("Original"),
    KODAK_GOLD("Kodak Gold"),
    POLAROID("Polaroid"),
    MONOCHROM("Monochrom"),
    FILM_NOIR("Film Noir"),
    BERNSTEIN("Bernstein"),
    NORDISCH("Nordisch"),
    NEON("Cyber Neon"),
    SEPIA("Vintage Sepia"),
    LEBENDIG("Lebendig")
}

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Original", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    THREE_FOUR("3:4", 3f / 4f),
    SIXTEEN_NINE("16:9", 16f / 9f)
}

enum class FrameStyle(val label: String) {
    NONE("Ohne Rahmen"),
    GALLERY_WHITE("Galerie"),
    POLAROID("Polaroid"),
    DARK_MATTE("Passepartout")
}

@Serializable
data class PhotoAdjustments(
    val brightness: Float = 0f,      // -0.5f .. 0.5f
    val contrast: Float = 1f,        // 0.5f .. 1.8f
    val saturation: Float = 1f,      // 0f .. 2f
    val warmth: Float = 0f,          // -0.5f .. 0.5f
    val vignette: Float = 0f,        // 0f .. 1f
    val rotationDegrees: Int = 0,    // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropAspect: CropAspect = CropAspect.FREE,
    val filter: PhotoFilter = PhotoFilter.ORIGINAL,
    val frameStyle: FrameStyle = FrameStyle.NONE
) {
    val isModified: Boolean
        get() = brightness != 0f ||
            contrast != 1f ||
            saturation != 1f ||
            warmth != 0f ||
            vignette != 0f ||
            rotationDegrees != 0 ||
            flipHorizontal ||
            flipVertical ||
            cropAspect != CropAspect.FREE ||
            filter != PhotoFilter.ORIGINAL ||
            frameStyle != FrameStyle.NONE
}

enum class ColorCategory(val label: String, val swatchHex: Long) {
    ALL("Alle Farben", 0xFFF59E0B),
    WARM_AMBER("Warm & Gold", 0xFFE67E22),
    COOL_BLUE("Blau & Himmel", 0xFF2980B9),
    EMERALD("Natur & Grün", 0xFF27AE60),
    NEON_CYAN("Nacht & Neon", 0xFF00D2D3),
    MONOCHROME("Neutral & Grau", 0xFF7F8C8D)
}

enum class CollageLayout(val label: String, val requiredCount: Int) {
    TWO_SPLIT("2er Split", 2),
    TWO_STACK("2er Stapel", 2),
    THREE_EDITORIAL("3er Editorial", 3),
    FOUR_GRID("4er Raster", 4)
}

data class StoryHighlight(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverPhotoPath: String,
    val photoIds: List<Long>
)

data class SmartAlbumSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val count: Int,
    val coverPath: String?
)
