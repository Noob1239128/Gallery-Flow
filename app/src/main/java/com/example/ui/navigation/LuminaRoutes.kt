package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface LuminaRoute {
    @Serializable
    data object Photos : LuminaRoute

    @Serializable
    data object Albums : LuminaRoute

    @Serializable
    data object Explore : LuminaRoute

    @Serializable
    data object Studio : LuminaRoute

    @Serializable
    data object Vault : LuminaRoute

    @Serializable
    data class AlbumDetail(val albumId: Long) : LuminaRoute

    @Serializable
    data class SmartAlbumDetail(val smartKey: String) : LuminaRoute

    @Serializable
    data class Viewer(val photoId: Long, val sourceMode: String = "ACTIVE") : LuminaRoute

    @Serializable
    data class Editor(val photoId: Long) : LuminaRoute

    @Serializable
    data class Slideshow(val photoIdsCsv: String, val startIndex: Int = 0) : LuminaRoute

    @Serializable
    data object Trash : LuminaRoute

    @Serializable
    data object Settings : LuminaRoute
}
