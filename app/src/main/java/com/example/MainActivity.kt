package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.model.GridLayoutMode
import com.example.ui.navigation.LuminaRoute
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.PhotosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SlideshowScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.LuminaTheme
import com.example.ui.viewmodel.GalleryViewModel

private data class TopLevelTab(
    val route: LuminaRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GalleryViewModel = viewModel()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            LuminaTheme(themeMode = preferences.themeMode) {
                LuminaAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun LuminaAppRoot(viewModel: GalleryViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val activePhotos by viewModel.activePhotos.collectAsStateWithLifecycle()
    val vaultPhotos by viewModel.vaultPhotos.collectAsStateWithLifecycle()
    val trashedPhotos by viewModel.trashedPhotos.collectAsStateWithLifecycle()
    val albums by viewModel.albumsWithPhotos.collectAsStateWithLifecycle()
    val smartAlbums by viewModel.smartAlbums.collectAsStateWithLifecycle()
    val stories by viewModel.storyHighlights.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedPhotoIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedColor by viewModel.selectedColorFilter.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val filteredExplorePhotos by viewModel.filteredExplorePhotos.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Zero-permission Android Photo Picker (Play Policy Compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotos(uris)
        }
    }

    // System Camera Capture via FileProvider
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onCameraCaptureResult(success)
    }

    val topTabs = remember {
        listOf(
            TopLevelTab(LuminaRoute.Photos, "Fotos", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary),
            TopLevelTab(LuminaRoute.Albums, "Alben", Icons.Filled.Collections, Icons.Outlined.Collections),
            TopLevelTab(LuminaRoute.Explore, "Entdecken", Icons.Filled.Explore, Icons.Outlined.Explore),
            TopLevelTab(LuminaRoute.Studio, "Studio", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
            TopLevelTab(LuminaRoute.Vault, "Tresor", Icons.Filled.Lock, Icons.Outlined.Lock)
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val isTopLevel = currentDest?.let { dest ->
        dest.hasRoute<LuminaRoute.Photos>() ||
            dest.hasRoute<LuminaRoute.Albums>() ||
            dest.hasRoute<LuminaRoute.Explore>() ||
            dest.hasRoute<LuminaRoute.Studio>() ||
            dest.hasRoute<LuminaRoute.Vault>()
    } ?: true

    val navigateToTab: (LuminaRoute) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail {
                        topTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = LuminaRoute.Photos,
                    enterTransition = { fadeIn(animationSpec = tween(160)) },
                    exitTransition = { fadeOut(animationSpec = tween(160)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(160)) },
                    popExitTransition = { fadeOut(animationSpec = tween(160)) },
                    modifier = Modifier.weight(1f)
                ) {
                    composable<LuminaRoute.Photos> {
                        PhotosScreen(
                            photos = activePhotos,
                            stories = stories,
                            albums = albums,
                            preferences = preferences,
                            selectedIds = selectedIds,
                            isBusy = isBusy,
                            imageEngine = viewModel.repository.imageEngine,
                            onOpenPhoto = { photoId ->
                                navController.navigate(LuminaRoute.Viewer(photoId, "ACTIVE"))
                            },
                            onToggleSelect = viewModel::toggleSelection,
                            onSelectAll = viewModel::selectAll,
                            onClearSelection = viewModel::clearSelection,
                            onImportPhotos = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onTakePhoto = {
                                val uri = viewModel.prepareCameraUri()
                                cameraLauncher.launch(uri)
                            },
                            onStartSlideshow = { ids ->
                                if (ids.isNotEmpty()) {
                                    navController.navigate(
                                        LuminaRoute.Slideshow(ids.joinToString(","), 0)
                                    )
                                }
                            },
                            onToggleLayoutMode = {
                                val next = if (preferences.gridLayoutMode == GridLayoutMode.MASONRY) {
                                    GridLayoutMode.SQUARE
                                } else {
                                    GridLayoutMode.MASONRY
                                }
                                viewModel.setGridLayoutMode(next)
                            },
                            onCycleColumns = {
                                val nextCols = when (preferences.gridColumns) {
                                    2 -> 3
                                    3 -> 4
                                    else -> 2
                                }
                                viewModel.setGridColumns(nextCols)
                            },
                            onFavoriteSelected = { viewModel.favoriteSelected(true) },
                            onAddSelectedToAlbum = { albumId ->
                                viewModel.addPhotosToAlbum(albumId, selectedIds.toList())
                            },
                            onCreateAlbumWithSelected = { name, desc ->
                                viewModel.createAlbum(name, desc, selectedIds.toList())
                            },
                            onMoveSelectedToVault = { viewModel.moveSelectedToVault(true) },
                            onMoveSelectedToTrash = viewModel::moveSelectedToTrash,
                            onOpenSettings = { navController.navigate(LuminaRoute.Settings) }
                        )
                    }

                    composable<LuminaRoute.Albums> {
                        AlbumsScreen(
                            albums = albums,
                            smartAlbums = smartAlbums,
                            trashCount = trashedPhotos.size,
                            onOpenAlbum = { albumId ->
                                navController.navigate(LuminaRoute.AlbumDetail(albumId))
                            },
                            onOpenSmartAlbum = { smartKey ->
                                navController.navigate(LuminaRoute.SmartAlbumDetail(smartKey))
                            },
                            onCreateAlbum = { name, desc ->
                                viewModel.createAlbum(name, desc)
                            },
                            onOpenTrash = { navController.navigate(LuminaRoute.Trash) }
                        )
                    }

                    composable<LuminaRoute.Explore> {
                        ExploreScreen(
                            allPhotos = activePhotos,
                            filteredPhotos = filteredExplorePhotos,
                            searchQuery = searchQuery,
                            selectedColor = selectedColor,
                            selectedTag = selectedTag,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onColorSelected = viewModel::setColorFilter,
                            onTagSelected = viewModel::setTagFilter,
                            onOpenPhoto = { photoId ->
                                navController.navigate(LuminaRoute.Viewer(photoId, "EXPLORE"))
                            }
                        )
                    }

                    composable<LuminaRoute.Studio> {
                        StudioScreen(
                            photos = activePhotos,
                            isBusy = isBusy,
                            onOpenEditor = { photoId ->
                                navController.navigate(LuminaRoute.Editor(photoId))
                            },
                            onCreateCollage = { chosen, layout, spacing, corners, bg ->
                                viewModel.createCollage(chosen, layout, spacing, corners, bg) { newId ->
                                    navController.navigate(LuminaRoute.Viewer(newId, "ACTIVE"))
                                }
                            }
                        )
                    }

                    composable<LuminaRoute.Vault> {
                        VaultScreen(
                            isPinSet = preferences.isVaultPinSet,
                            isUnlocked = isVaultUnlocked,
                            vaultPhotos = vaultPhotos,
                            onSetupPin = viewModel::setupVaultPin,
                            onTryUnlock = viewModel::tryUnlockVault,
                            onLockVault = viewModel::lockVault,
                            onOpenPhoto = { photoId ->
                                navController.navigate(LuminaRoute.Viewer(photoId, "VAULT"))
                            },
                            onRestoreFromVault = { photoId ->
                                viewModel.movePhotoToVault(photoId, false)
                            }
                        )
                    }

                    composable<LuminaRoute.AlbumDetail> { entry ->
                        val route = entry.toRoute<LuminaRoute.AlbumDetail>()
                        val albumData by viewModel.observeAlbum(route.albumId)
                            .collectAsStateWithLifecycle(initialValue = null)
                        val currentAlbum = albumData
                        if (currentAlbum != null) {
                            AlbumDetailScreen(
                                title = currentAlbum.album.name,
                                subtitle = currentAlbum.album.description,
                                photos = currentAlbum.activePhotos,
                                allGalleryPhotos = activePhotos,
                                isEditableAlbum = true,
                                albumEntity = currentAlbum,
                                onBack = { navController.popBackStack() },
                                onOpenPhoto = { photoId ->
                                    navController.navigate(LuminaRoute.Viewer(photoId, "ACTIVE"))
                                },
                                onStartSlideshow = { ids ->
                                    navController.navigate(LuminaRoute.Slideshow(ids.joinToString(","), 0))
                                },
                                onRenameAlbum = { name, desc ->
                                    viewModel.renameAlbum(currentAlbum.album, name, desc)
                                },
                                onDeleteAlbum = {
                                    viewModel.deleteAlbum(currentAlbum.album.id)
                                    navController.popBackStack()
                                },
                                onAddPhotosToAlbum = { ids ->
                                    viewModel.addPhotosToAlbum(currentAlbum.album.id, ids)
                                },
                                onRemoveFromAlbum = { photoId ->
                                    viewModel.removePhotoFromAlbum(currentAlbum.album.id, photoId)
                                }
                            )
                        }
                    }

                    composable<LuminaRoute.SmartAlbumDetail> { entry ->
                        val route = entry.toRoute<LuminaRoute.SmartAlbumDetail>()
                        val smartPhotos = remember(activePhotos, route.smartKey) {
                            viewModel.getPhotosForSmartAlbum(route.smartKey, activePhotos)
                        }
                        AlbumDetailScreen(
                            title = viewModel.getSmartAlbumTitle(route.smartKey),
                            subtitle = "Automatische Sammlung",
                            photos = smartPhotos,
                            allGalleryPhotos = activePhotos,
                            isEditableAlbum = false,
                            onBack = { navController.popBackStack() },
                            onOpenPhoto = { photoId ->
                                navController.navigate(LuminaRoute.Viewer(photoId, "ACTIVE"))
                            },
                            onStartSlideshow = { ids ->
                                navController.navigate(LuminaRoute.Slideshow(ids.joinToString(","), 0))
                            }
                        )
                    }

                    composable<LuminaRoute.Viewer> { entry ->
                        val route = entry.toRoute<LuminaRoute.Viewer>()
                        val list = when (route.sourceMode) {
                            "VAULT" -> vaultPhotos
                            "EXPLORE" -> filteredExplorePhotos
                            else -> activePhotos
                        }
                        PhotoViewerScreen(
                            photos = list,
                            initialPhotoId = route.photoId,
                            imageEngine = viewModel.repository.imageEngine,
                            onBack = { navController.popBackStack() },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onOpenEditor = { photoId ->
                                navController.navigate(LuminaRoute.Editor(photoId))
                            },
                            onStartSlideshow = { ids, idx ->
                                navController.navigate(
                                    LuminaRoute.Slideshow(ids.joinToString(","), idx)
                                )
                            },
                            onToggleVault = { photo ->
                                viewModel.movePhotoToVault(photo.id, !photo.isInVault)
                            },
                            onMoveToTrash = viewModel::movePhotoToTrash,
                            onUpdateMetadata = viewModel::updatePhotoMetadata
                        )
                    }

                    composable<LuminaRoute.Editor> { entry ->
                        val route = entry.toRoute<LuminaRoute.Editor>()
                        val photo by viewModel.observePhoto(route.photoId)
                            .collectAsStateWithLifecycle(initialValue = null)
                        val target = photo
                        if (target != null) {
                            PhotoEditorScreen(
                                photo = target,
                                isBusy = isBusy,
                                onBack = { navController.popBackStack() },
                                onSave = { adjustments, saveAsCopy ->
                                    viewModel.saveEditedPhoto(target, adjustments, saveAsCopy) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }

                    composable<LuminaRoute.Slideshow> { entry ->
                        val route = entry.toRoute<LuminaRoute.Slideshow>()
                        val ids = remember(route.photoIdsCsv) {
                            route.photoIdsCsv.split(",").mapNotNull { it.toLongOrNull() }
                        }
                        val slideshowPhotos = remember(ids, activePhotos) {
                            val map = activePhotos.associateBy { it.id }
                            ids.mapNotNull { map[it] }.ifEmpty { activePhotos }
                        }
                        SlideshowScreen(
                            photos = slideshowPhotos,
                            startIndex = route.startIndex,
                            initialIntervalSec = preferences.slideshowIntervalSec,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<LuminaRoute.Trash> {
                        TrashScreen(
                            trashedPhotos = trashedPhotos,
                            onBack = { navController.popBackStack() },
                            onRestore = { id -> viewModel.restoreFromTrash(listOf(id)) },
                            onDeletePermanently = viewModel::deletePermanently,
                            onEmptyTrash = viewModel::emptyTrash
                        )
                    }

                    composable<LuminaRoute.Settings> {
                        SettingsScreen(
                            preferences = preferences,
                            onBack = { navController.popBackStack() },
                            onSetTheme = viewModel::setThemeMode,
                            onSetGridLayout = viewModel::setGridLayoutMode,
                            onSetColumns = viewModel::setGridColumns,
                            onSetSlideshowInterval = viewModel::setSlideshowInterval,
                            onResetVaultPin = viewModel::resetVaultPin,
                            onRestoreDemoPhotos = viewModel::restoreDemoPhotos
                        )
                    }
                }
            }
        }
    }
}
