package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "album_photo_cross_ref",
    primaryKeys = ["albumId", "photoId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("photoId")]
)
data class AlbumPhotoCrossRef(
    val albumId: Long,
    val photoId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

data class AlbumWithPhotos(
    @Embedded val album: AlbumEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AlbumPhotoCrossRef::class,
            parentColumn = "albumId",
            entityColumn = "photoId"
        )
    )
    val photos: List<PhotoEntity>
) {
    val activePhotos: List<PhotoEntity>
        get() = photos.filter { !it.isTrashed && !it.isInVault }.sortedByDescending { it.takenAt }
}
