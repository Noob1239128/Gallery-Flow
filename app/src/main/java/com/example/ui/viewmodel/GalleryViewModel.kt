package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlbumEntity
import com.example.data.local.AlbumWithPhotos
import com.example.data.local.GalleryDatabase
import com.example.data.local.PhotoEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.model.CollageLayout
import com.example.data.model.ColorCategory
import com.example.data.model.GridLayoutMode
import com.example.data.model.PhotoAdjustments
import com.example.data.model.SmartAlbumSummary
import com.example.data.model.StoryHighlight
import com.example.data.model.ThemeMode
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val repository: GalleryRepository

    init {
        val db = GalleryDatabase.getInstance(application)
        val prefs = PreferencesRepository(application)
        val engine = com.example.data.media.ImageProcessingEngine(application)
        repository = GalleryRepository(db.galleryDao(), prefs, engine)
        viewModelScope.launch {
            repository.ensureSeededIfFirstLaunch()
        }
    }

    val preferences: StateFlow<UserPreferences> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val activePhotos: StateFlow<List<PhotoEntity>> = repository.activePhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultPhotos: StateFlow<List<PhotoEntity>> = repository.vaultPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedPhotos: StateFlow<List<PhotoEntity>> = repository.trashedPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumsWithPhotos: StateFlow<List<AlbumWithPhotos>> = repository.albumsWithPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow(ColorCategory.ALL)
    val selectedColorFilter: StateFlow<ColorCategory> = _selectedColorFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var pendingCameraFile: File? = null

    val filteredExplorePhotos: StateFlow<List<PhotoEntity>> = combine(
        activePhotos,
        _searchQuery,
        _selectedColorFilter,
        _selectedTagFilter
    ) { photos, query, color, tag ->
        val q = query.trim().lowercase()
        photos.filter { p ->
            val matchesColor = color == ColorCategory.ALL || p.colorCategory == color.name
            val matchesTag = tag == null || p.tagsList.any { it.equals(tag, ignoreCase = true) }
            val matchesQuery = q.isEmpty() ||
                p.title.lowercase().contains(q) ||
                p.caption.lowercase().contains(q) ||
                p.locationName.lowercase().contains(q) ||
                p.cameraModel.lowercase().contains(q) ||
                p.tagsCsv.lowercase().contains(q)
            matchesColor && matchesTag && matchesQuery
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storyHighlights: StateFlow<List<StoryHighlight>> = activePhotos
        .map { photos ->
            if (photos.isEmpty()) emptyList()
            else {
                val stories = mutableListOf<StoryHighlight>()
                val favorites = photos.filter { it.isFavorite }
                if (favorites.isNotEmpty()) {
                    stories.add(
                        StoryHighlight(
                            id = "fav_story",
                            title = "Lieblingsmomente",
                            subtitle = "${favorites.size} Favoriten",
                            coverPhotoPath = favorites.first().filePath,
                            photoIds = favorites.map { it.id }
                        )
                    )
                }
                val warm = photos.filter {
                    it.colorCategory == ColorCategory.WARM_AMBER.name ||
                        it.tagsCsv.contains("Goldene", ignoreCase = true)
                }
                if (warm.isNotEmpty()) {
                    stories.add(
                        StoryHighlight(
                            id = "golden_story",
                            title = "Goldenes Licht",
                            subtitle = "${warm.size} Aufnahmen",
                            coverPhotoPath = warm.first().filePath,
                            photoIds = warm.map { it.id }
                        )
                    )
                }
                val recent = photos.take(6)
                stories.add(
                    StoryHighlight(
                        id = "recent_story",
                        title = "Neueste Galerie",
                        subtitle = "${recent.size} Highlights",
                        coverPhotoPath = recent.first().filePath,
                        photoIds = recent.map { it.id }
                    )
                )
                stories
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val smartAlbums: StateFlow<List<SmartAlbumSummary>> = activePhotos
        .map { photos ->
            val favs = photos.filter { it.isFavorite }
            val edited = photos.filter { it.isEdited }
            val collages = photos.filter { it.isCollage }
            val landscape = photos.filter { it.width > it.height }
            val portrait = photos.filter { it.height >= it.width }
            listOf(
                SmartAlbumSummary("favorites", "Favoriten", "Mit Herz markiert", favs.size, favs.firstOrNull()?.filePath),
                SmartAlbumSummary("edited", "Bearbeitet", "Filter & Studio-Edits", edited.size, edited.firstOrNull()?.filePath),
                SmartAlbumSummary("collages", "Kollagen", "Kollage-Studio", collages.size, collages.firstOrNull()?.filePath),
                SmartAlbumSummary("landscape", "Querformat", "Panorama & Weitwinkel", landscape.size, landscape.firstOrNull()?.filePath),
                SmartAlbumSummary("portrait", "Hochformat", "Porträts & Details", portrait.size, portrait.firstOrNull()?.filePath)
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observePhoto(id: Long) = repository.observePhoto(id)
    fun observeAlbum(id: Long) = repository.observeAlbum(id)

    fun getPhotosForSmartAlbum(key: String, all: List<PhotoEntity>): List<PhotoEntity> = when (key) {
        "favorites" -> all.filter { it.isFavorite }
        "edited" -> all.filter { it.isEdited }
        "collages" -> all.filter { it.isCollage }
        "landscape" -> all.filter { it.width > it.height }
        "portrait" -> all.filter { it.height >= it.width }
        else -> all
    }

    fun getSmartAlbumTitle(key: String): String = when (key) {
        "favorites" -> "Favoriten"
        "edited" -> "Bearbeitete Fotos"
        "collages" -> "Kollagen"
        "landscape" -> "Querformat"
        "portrait" -> "Hochformat"
        else -> "Sammlung"
    }

    // Selection
    fun toggleSelection(photoId: Long) {
        val current = _selectedPhotoIds.value
        _selectedPhotoIds.value = if (photoId in current) current - photoId else current + photoId
    }

    fun selectAll(ids: List<Long>) {
        _selectedPhotoIds.value = ids.toSet()
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    // Explore Filters
    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setColorFilter(category: ColorCategory) {
        _selectedColorFilter.value = category
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = if (_selectedTagFilter.value == tag) null else tag
    }

    // Imports & Camera
    fun importPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            val count = repository.importPickedUris(uris)
            _isBusy.value = false
            _messages.emit(
                if (count == 1) "1 Foto importiert"
                else "$count Fotos importiert"
            )
        }
    }

    fun prepareCameraUri(): Uri {
        val (file, uri) = repository.imageEngine.createCameraTempFile()
        pendingCameraFile = file
        return uri
    }

    fun onCameraCaptureResult(success: Boolean) {
        val file = pendingCameraFile
        pendingCameraFile = null
        if (!success || file == null) {
            file?.delete()
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val ok = repository.importCameraCapture(file)
            _isBusy.value = false
            if (ok) _messages.emit("Kamera-Foto gespeichert")
        }
    }

    // Photo Actions
    fun toggleFavorite(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(photo)
        }
    }

    fun favoriteSelected(favorite: Boolean) {
        val ids = _selectedPhotoIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setFavorites(ids, favorite)
            clearSelection()
            _messages.emit(if (favorite) "Als Favoriten markiert" else "Aus Favoriten entfernt")
        }
    }

    fun moveSelectedToTrash() {
        val ids = _selectedPhotoIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveToTrash(ids)
            clearSelection()
            _messages.emit("${ids.size} in den Papierkorb verschoben")
        }
    }

    fun movePhotoToTrash(photoId: Long) {
        viewModelScope.launch {
            repository.moveToTrash(listOf(photoId))
            _messages.emit("In den Papierkorb verschoben")
        }
    }

    fun restoreFromTrash(ids: List<Long>) {
        viewModelScope.launch {
            repository.restoreFromTrash(ids)
            _messages.emit("Wiederhergestellt")
        }
    }

    fun deletePermanently(photo: PhotoEntity) {
        viewModelScope.launch {
            repository.deletePermanently(photo)
            _messages.emit("Endgültig gelöscht")
        }
    }

    fun emptyTrash() {
        val items = trashedPhotos.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.emptyTrash(items)
            _messages.emit("Papierkorb geleert")
        }
    }

    fun moveSelectedToVault(inVault: Boolean) {
        val ids = _selectedPhotoIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveToVault(ids, inVault)
            clearSelection()
            _messages.emit(
                if (inVault) "In den privaten Tresor verschoben"
                else "Aus dem Tresor wiederhergestellt"
            )
        }
    }

    fun movePhotoToVault(photoId: Long, inVault: Boolean) {
        viewModelScope.launch {
            repository.moveToVault(listOf(photoId), inVault)
            _messages.emit(
                if (inVault) "In den privaten Tresor verschoben"
                else "Aus dem Tresor entfernt"
            )
        }
    }

    fun updatePhotoMetadata(
        photo: PhotoEntity,
        title: String,
        caption: String,
        location: String,
        tagsCsv: String
    ) {
        viewModelScope.launch {
            repository.updatePhotoDetails(photo, title, caption, location, tagsCsv)
            _messages.emit("Metadaten gespeichert")
        }
    }

    // Editor & Collage
    fun saveEditedPhoto(
        original: PhotoEntity,
        adjustments: PhotoAdjustments,
        saveAsCopy: Boolean,
        onSaved: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val savedId = repository.saveEditedPhoto(original, adjustments, saveAsCopy)
            _isBusy.value = false
            if (savedId != null) {
                _messages.emit(if (saveAsCopy) "Bearbeitete Kopie gespeichert" else "Änderungen gespeichert")
                onSaved(savedId)
            } else {
                _messages.emit("Fehler beim Speichern des Bildes")
            }
        }
    }

    fun createCollage(
        photos: List<PhotoEntity>,
        layout: CollageLayout,
        spacingPx: Float,
        cornerRadiusPx: Float,
        backgroundColor: Int,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val id = repository.createCollage(photos, layout, spacingPx, cornerRadiusPx, backgroundColor)
            _isBusy.value = false
            if (id != null) {
                _messages.emit("Kollage erstellt")
                onCreated(id)
            } else {
                _messages.emit("Kollage konnte nicht erstellt werden")
            }
        }
    }

    // Albums
    fun createAlbum(name: String, description: String, initialPhotoIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createAlbum(name, description, initialPhotoIds)
            clearSelection()
            _messages.emit("Album \"$name\" erstellt")
        }
    }

    fun renameAlbum(album: AlbumEntity, newName: String, newDesc: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameAlbum(album, newName, newDesc)
            _messages.emit("Album aktualisiert")
        }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch {
            repository.deleteAlbum(albumId)
            _messages.emit("Album gelöscht")
        }
    }

    fun addPhotosToAlbum(albumId: Long, photoIds: List<Long>) {
        viewModelScope.launch {
            repository.addPhotosToAlbum(albumId, photoIds)
            clearSelection()
            _messages.emit("Zum Album hinzugefügt")
        }
    }

    fun removePhotoFromAlbum(albumId: Long, photoId: Long) {
        viewModelScope.launch {
            repository.removePhotoFromAlbum(albumId, photoId)
            _messages.emit("Aus Album entfernt")
        }
    }

    // Vault PIN
    fun setupVaultPin(pin: String) {
        viewModelScope.launch {
            repository.setVaultPin(pin)
            _isVaultUnlocked.value = true
            _messages.emit("Tresor-PIN festgelegt")
        }
    }

    fun tryUnlockVault(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.verifyVaultPin(pin)
            if (ok) _isVaultUnlocked.value = true
            onResult(ok)
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun resetVaultPin() {
        viewModelScope.launch {
            repository.clearVaultPin()
            _isVaultUnlocked.value = false
            _messages.emit("Tresor-PIN zurückgesetzt")
        }
    }

    // Settings
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setGridLayoutMode(mode: GridLayoutMode) {
        viewModelScope.launch { repository.setGridLayoutMode(mode) }
    }

    fun setGridColumns(cols: Int) {
        viewModelScope.launch { repository.setGridColumns(cols) }
    }

    fun setSlideshowInterval(sec: Int) {
        viewModelScope.launch { repository.setSlideshowInterval(sec) }
    }

    fun restoreDemoPhotos() {
        viewModelScope.launch {
            val count = repository.restoreCuratedDemoPhotos()
            _messages.emit(
                if (count > 0) "$count Demo-Fotos wiederhergestellt"
                else "Alle Demo-Fotos sind bereits vorhanden"
            )
        }
    }

    fun notifyMessage(msg: String) {
        viewModelScope.launch { _messages.emit(msg) }
    }
}
