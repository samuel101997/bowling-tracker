package com.bowlingtracker.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Flat delivery row — domain Insights flattened into columns (no framework types leak to domain). */
@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recorded_at") val recordedAtEpochMs: Long,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Double,
    @ColumnInfo(name = "swing_dir") val swingDirection: String,
    @ColumnInfo(name = "swing_m") val swingMeters: Double,
    @ColumnInfo(name = "confidence") val confidence: String,
    @ColumnInfo(name = "engine_version") val engineVersion: String,
)

@Dao
interface DeliveryDao {
    @Insert
    suspend fun insert(delivery: DeliveryEntity): Long

    @Query("SELECT * FROM deliveries ORDER BY recorded_at DESC")
    fun observeAll(): Flow<List<DeliveryEntity>>

    @Query("SELECT COUNT(*) FROM deliveries")
    suspend fun count(): Int
}

@Database(entities = [DeliveryEntity::class], version = 1, exportSchema = false)
abstract class BowlingDatabase : RoomDatabase() {
    abstract fun deliveryDao(): DeliveryDao
}
