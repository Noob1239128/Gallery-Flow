package com.example.data.repository

import android.net.Uri
import com.example.R
import com.example.data.local.AlbumEntity
import com.example.data.local.AlbumPhotoCrossRef
import com.example.data.local.AlbumWithPhotos
import com.example.data.local.GalleryDao
import com.example.data.local.PhotoEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.media.ImageProcessingEngine
import com.example.data.model.CollageLayout
import com.example.data.model.GridLayoutMode
import com.example.data.model.PhotoAdjustments
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GalleryRepository(
    private val dao: GalleryDao,
    private val prefs: PreferencesRepository,
    val imageEngine: ImageProcessingEngine
) {
    val activePhotos: Flow<List<PhotoEntity>> = dao.observeActivePhotos()
    val vaultPhotos: Flow<List<PhotoEntity>> = dao.observeVaultPhotos()
    val trashedPhotos: Flow<List<PhotoEntity>> = dao.observeTrashedPhotos()
    val albumsWithPhotos: Flow<List<AlbumWithPhotos>> = dao.observeAlbumsWithPhotos()
    val preferences: Flow<UserPreferences> = prefs.preferencesFlow

    fun observePhoto(id: Long): Flow<PhotoEntity?> = dao.observePhotoById(id)
    fun observeAlbum(id: Long): Flow<AlbumWithPhotos?> = dao.observeAlbumWithPhotos(id)

    suspend fun ensureSeededIfFirstLaunch() {
        val currentPrefs = prefs.preferencesFlow.first()
        if (currentPrefs.hasSeededInitialData && dao.countActivePhotos() > 0) return

        seedCuratedShowcasePhotos()
        prefs.markSeeded(true)
    }

    suspend fun restoreCuratedDemoPhotos(): Int {
        return seedCuratedShowcasePhotos()
    }

    private suspend fun seedCuratedShowcasePhotos(): Int {
        val existingCuratedPaths = dao.getCuratedPhotos().map { it.originalFilePath }.toSet()
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        data class CuratedSpec(
            val drawableId: Int,
            val fileName: String,
            val title: String,
            val caption: String,
            val location: String,
            val camera: String,
            val lens: String,
            val aperture: String,
            val shutter: String,
            val iso: Int,
            val tags: String,
            val offsetMs: Long,
            val favorite: Boolean
        )

        val specs = listOf(
            CuratedSpec(
                drawableId = R.drawable.img_photo_alpine_1795011868756,
                fileName = "curated_dolomites_golden.jpg",
                title = "Dolomiten im Abendlicht",
                caption = "Spiegelglatter Bergsee kurz vor Sonnenuntergang.",
                location = "Südtirol, Italien",
                camera = "Hasselblad X2D 100C",
                lens = "38mm f/2.5V",
                aperture = "f/8.0",
                shutter = "1/160s",
                iso = 64,
                tags = "Berge, Goldene Stunde, Landschaft, Reise",
                offsetMs = 2L * 60L * 60L * 1000L,
                favorite = true
            ),
            CuratedSpec(
                drawableId = R.drawable.img_photo_tokyo_1795011869739,
                fileName = "curated_tokyo_rain.jpg",
                title = "Regennacht in Shinjuku",
                caption = "Neon-Spiegelungen auf nassem Kopfsteinpflaster.",
                location = "Tokio, Japan",
                camera = "Leica Q3",
                lens = "28mm Summilux",
                aperture = "f/1.7",
                shutter = "1/60s",
                iso = 1600,
                tags = "Nacht, Street, Neon, Reise",
                offsetMs = 2L * dayMs,
                favorite = true
            ),
            CuratedSpec(
                drawableId = R.drawable.img_photo_botanical_1795011870623,
                fileName = "curated_botanical_dew.jpg",
                title = "Morgentau auf Monstera",
                caption = "Makroaufnahme im warmen Gewächshaus-Morgenlicht.",
                location = "Botanischer Garten, München",
                camera = "Sony A7R V",
                lens = "90mm f/2.8 Macro",
                aperture = "f/4.0",
                shutter = "1/250s",
                iso = 200,
                tags = "Natur, Makro, Grün, Botanik",
                offsetMs = 5L * dayMs,
                favorite = false
            ),
            CuratedSpec(
                drawableId = R.drawable.img_photo_architecture_1795011871333,
                fileName = "curated_terracotta_lines.jpg",
                title = "Terrakotta & Schatten",
                caption = "Geometrische Linien im späten Nachmittagslicht.",
                location = "Alicante, Spanien",
                camera = "Fujifilm GFX 100S",
                lens = "45mm f/2.8",
                aperture = "f/11",
                shutter = "1/400s",
                iso = 100,
                tags = "Architektur, Minimalismus, Licht, Reise",
                offsetMs = 11L * dayMs,
                favorite = true
            )
        )

        val insertedIds = mutableListOf<Long>()
        val travelIds = mutableListOf<Long>()
        val nightIds = mutableListOf<Long>()

        for (spec in specs) {
            val targetPath = File(imageEngine.photosDir, spec.fileName).absolutePath
            if (targetPath in existingCuratedPaths) continue

            val meta = imageEngine.saveDrawableAsGalleryFile(spec.drawableId, spec.fileName)
            val entity = PhotoEntity(
                title = spec.title,
                caption = spec.caption,
                filePath = meta.filePath,
                originalFilePath = meta.filePath,
                width = meta.width,
                height = meta.height,
                fileSizeBytes = meta.fileSizeBytes,
                takenAt = now - spec.offsetMs,
                addedAt = now - spec.offsetMs,
                locationName = spec.location,
                cameraModel = spec.camera,
                lensSpec = spec.lens,
                aperture = spec.aperture,
                shutterSpeed = spec.shutter,
                iso = spec.iso,
                tagsCsv = spec.tags,
                paletteCsv = meta.paletteCsv,
                colorCategory = meta.colorCategory.name,
                isFavorite = spec.favorite,
                sourceType = "CURATED"
            )
            val id = dao.insertPhoto(entity)
            insertedIds.add(id)
            if (spec.tags.contains("Reise")) travelIds.add(id)
            if (spec.tags.contains("Nacht") || spec.tags.contains("Architektur")) nightIds.add(id)
        }

        if (insertedIds.isNotEmpty() && existingCuratedPaths.isEmpty()) {
            val album1 = dao.insertAlbum(
                AlbumEntity(
                    name = "Reisen & Fernweh",
                    description = "Landschaften, Städte und besondere Orte"
                )
            )
            val album2 = dao.insertAlbum(
                AlbumEntity(
                    name = "Licht & Architektur",
                    description = "Kontraste, Linien und Atmosphäre"
                )
            )
            dao.addPhotosToAlbum(travelIds.map { AlbumPhotoCrossRef(album1, it) })
            dao.addPhotosToAlbum(nightIds.map { AlbumPhotoCrossRef(album2, it) })
        }

        return insertedIds.size
    }

    suspend fun importPickedUris(uris: List<Uri>): Int {
        var count = 0
        for (uri in uris) {
            val meta = imageEngine.importUriToGallery(uri) ?: continue
            dao.insertPhoto(
                PhotoEntity(
                    title = "Importiertes Foto",
                    caption = "Aus der Geräte-Mediathek hinzugefügt",
                    filePath = meta.filePath,
                    originalFilePath = meta.filePath,
                    width = meta.width,
                    height = meta.height,
                    fileSizeBytes = meta.fileSizeBytes,
                    takenAt = meta.takenAt,
                    addedAt = System.currentTimeMillis(),
                    locationName = "Gerät",
                    cameraModel = meta.cameraModel,
                    lensSpec = meta.lensSpec,
                    aperture = meta.aperture,
                    shutterSpeed = meta.shutterSpeed,
                    iso = meta.iso,
                    tagsCsv = "Import, Galerie",
                    paletteCsv = meta.paletteCsv,
                    colorCategory = meta.colorCategory.name,
                    sourceType = "IMPORTED"
                )
            )
            count++
        }
        return count
    }

    suspend fun importCameraCapture(file: File): Boolean {
        val meta = imageEngine.importLocalFileToGallery(file, deleteSource = true) ?: return false
        dao.insertPhoto(
            PhotoEntity(
                title = "Kamera-Aufnahme",
                caption = "Direkt mit Lumina aufgenommen",
                filePath = meta.filePath,
                originalFilePath = meta.filePath,
                width = meta.width,
                height = meta.height,
                fileSizeBytes = meta.fileSizeBytes,
                takenAt = meta.takenAt,
                addedAt = System.currentTimeMillis(),
                locationName = "Kamera",
                cameraModel = meta.cameraModel,
                lensSpec = meta.lensSpec,
                aperture = meta.aperture,
                shutterSpeed = meta.shutterSpeed,
                iso = meta.iso,
                tagsCsv = "Kamera, Neu",
                paletteCsv = meta.paletteCsv,
                colorCategory = meta.colorCategory.name,
                sourceType = "CAMERA"
            )
        )
        return true
    }

    suspend fun saveEditedPhoto(
        original: PhotoEntity,
        adjustments: PhotoAdjustments,
        saveAsCopy: Boolean
    ): Long? {
        val overwritePath = if (saveAsCopy) null else {
            if (original.filePath != original.originalFilePath) original.filePath else null
        }
        val meta = imageEngine.renderAndSaveEditedPhoto(
            sourceFilePath = original.originalFilePath,
            adjustments = adjustments,
            takenAt = original.takenAt,
            overwriteTargetPath = overwritePath
        ) ?: return null

        val json = Json.encodeToString(adjustments)
        return if (saveAsCopy) {
            dao.insertPhoto(
                original.copy(
                    id = 0,
                    title = "${original.title} (${adjustments.filter.displayName})",
                    filePath = meta.filePath,
                    width = meta.width,
                    height = meta.height,
                    fileSizeBytes = meta.fileSizeBytes,
                    addedAt = System.currentTimeMillis(),
                    paletteCsv = meta.paletteCsv,
                    colorCategory = meta.colorCategory.name,
                    isEdited = true,
                    sourceType = "EDITED",
                    adjustmentsJson = json
                )
            )
        } else {
            dao.updatePhoto(
                original.copy(
                    filePath = meta.filePath,
                    width = meta.width,
                    height = meta.height,
                    fileSizeBytes = meta.fileSizeBytes,
                    paletteCsv = meta.paletteCsv,
                    colorCategory = meta.colorCategory.name,
                    isEdited = adjustments.isModified,
                    adjustmentsJson = json
                )
            )
            original.id
        }
    }

    suspend fun createCollage(
        photos: List<PhotoEntity>,
        layout: CollageLayout,
        spacingPx: Float,
        cornerRadiusPx: Float,
        backgroundColor: Int
    ): Long? {
        val meta = imageEngine.renderAndSaveCollage(
            filePaths = photos.map { it.filePath },
            layout = layout,
            spacingPx = spacingPx,
            cornerRadiusPx = cornerRadiusPx,
            backgroundColor = backgroundColor
        ) ?: return null

        return dao.insertPhoto(
            PhotoEntity(
                title = "Kollage (${layout.label})",
                caption = "Erstellt im Lumina Kollage-Studio",
                filePath = meta.filePath,
                originalFilePath = meta.filePath,
                width = meta.width,
                height = meta.height,
                fileSizeBytes = meta.fileSizeBytes,
                takenAt = meta.takenAt,
                addedAt = meta.takenAt,
                locationName = "Lumina Studio",
                cameraModel = meta.cameraModel,
                lensSpec = meta.lensSpec,
                aperture = meta.aperture,
                shutterSpeed = meta.shutterSpeed,
                iso = meta.iso,
                tagsCsv = "Kollage, Studio",
                paletteCsv = meta.paletteCsv,
                colorCategory = meta.colorCategory.name,
                isCollage = true,
                sourceType = "COLLAGE"
            )
        )
    }

    suspend fun toggleFavorite(photo: PhotoEntity) = dao.setFavorite(photo.id, !photo.isFavorite)
    suspend fun setFavorites(ids: List<Long>, favorite: Boolean) = dao.setFavorites(ids, favorite)
    suspend fun moveToVault(ids: List<Long>, inVault: Boolean) = dao.setVaultStatus(ids, inVault)
    suspend fun moveToTrash(ids: List<Long>) = dao.setTrashedStatus(ids, true, System.currentTimeMillis())
    suspend fun restoreFromTrash(ids: List<Long>) = dao.setTrashedStatus(ids, false, null)

    suspend fun deletePermanently(photo: PhotoEntity) {
        dao.deletePhotoPermanently(photo)
        runCatching {
            File(photo.filePath).delete()
            if (photo.originalFilePath != photo.filePath) {
                File(photo.originalFilePath).delete()
            }
        }
    }

    suspend fun emptyTrash(trashed: List<PhotoEntity>) {
        dao.emptyTrash()
        trashed.forEach { photo ->
            runCatching {
                File(photo.filePath).delete()
                if (photo.originalFilePath != photo.filePath) {
                    File(photo.originalFilePath).delete()
                }
            }
        }
    }

    suspend fun updatePhotoDetails(
        photo: PhotoEntity,
        title: String,
        caption: String,
        locationName: String,
        tagsCsv: String
    ) {
        dao.updatePhoto(
            photo.copy(
                title = title.ifBlank { photo.title },
                caption = caption,
                locationName = locationName,
                tagsCsv = tagsCsv
            )
        )
    }

    suspend fun createAlbum(name: String, description: String, initialPhotoIds: List<Long> = emptyList()): Long {
        val albumId = dao.insertAlbum(AlbumEntity(name = name.trim(), description = description.trim()))
        if (initialPhotoIds.isNotEmpty()) {
            dao.addPhotosToAlbum(initialPhotoIds.map { AlbumPhotoCrossRef(albumId, it) })
        }
        return albumId
    }

    suspend fun renameAlbum(album: AlbumEntity, newName: String, newDescription: String) {
        dao.updateAlbum(album.copy(name = newName.trim(), description = newDescription.trim()))
    }

    suspend fun deleteAlbum(albumId: Long) = dao.deleteAlbumById(albumId)

    suspend fun addPhotosToAlbum(albumId: Long, photoIds: List<Long>) {
        dao.addPhotosToAlbum(photoIds.map { AlbumPhotoCrossRef(albumId, it) })
    }

    suspend fun removePhotoFromAlbum(albumId: Long, photoId: Long) =
        dao.removePhotoFromAlbum(albumId, photoId)

    // Preferences
    suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    suspend fun setGridLayoutMode(mode: GridLayoutMode) = prefs.setGridLayoutMode(mode)
    suspend fun setGridColumns(cols: Int) = prefs.setGridColumns(cols)
    suspend fun setSlideshowInterval(sec: Int) = prefs.setSlideshowInterval(sec)
    suspend fun setVaultPin(pin: String) = prefs.setVaultPin(pin)
    suspend fun verifyVaultPin(pin: String): Boolean = prefs.verifyVaultPin(pin)
    suspend fun clearVaultPin() = prefs.clearVaultPin()
}
