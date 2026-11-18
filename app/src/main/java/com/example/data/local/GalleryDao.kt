package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {

    @Query("SELECT * FROM photos WHERE isTrashed = 0 AND isInVault = 0 ORDER BY takenAt DESC")
    fun observeActivePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isTrashed = 0 AND isInVault = 1 ORDER BY takenAt DESC")
    fun observeVaultPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrashedPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    fun observePhotoById(id: Long): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Long): PhotoEntity?

    @Query("SELECT COUNT(*) FROM photos WHERE isTrashed = 0 AND isInVault = 0")
    suspend fun countActivePhotos(): Int

    @Query("SELECT * FROM photos WHERE sourceType = 'CURATED'")
    suspend fun getCuratedPhotos(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>): List<Long>

    @Update
    suspend fun updatePhoto(photo: PhotoEntity)

    @Delete
    suspend fun deletePhotoPermanently(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deletePhotosByIds(ids: List<Long>)

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE photos SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavorites(ids: List<Long>, isFavorite: Boolean)

    @Query("UPDATE photos SET isInVault = :inVault WHERE id IN (:ids)")
    suspend fun setVaultStatus(ids: List<Long>, inVault: Boolean)

    @Query("UPDATE photos SET isTrashed = :trashed, trashedAt = :trashedAt WHERE id IN (:ids)")
    suspend fun setTrashedStatus(ids: List<Long>, trashed: Boolean, trashedAt: Long?)

    @Query("DELETE FROM photos WHERE isTrashed = 1")
    suspend fun emptyTrash()

    // Albums
    @Transaction
    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun observeAlbumsWithPhotos(): Flow<List<AlbumWithPhotos>>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    fun observeAlbumWithPhotos(albumId: Long): Flow<AlbumWithPhotos?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("DELETE FROM albums WHERE id = :albumId")
    suspend fun deleteAlbumById(albumId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPhotosToAlbum(refs: List<AlbumPhotoCrossRef>)

    @Query("DELETE FROM album_photo_cross_ref WHERE albumId = :albumId AND photoId = :photoId")
    suspend fun removePhotoFromAlbum(albumId: Long, photoId: Long)
}
