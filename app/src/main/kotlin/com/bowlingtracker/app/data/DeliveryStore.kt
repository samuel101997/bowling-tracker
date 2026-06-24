package com.bowlingtracker.app.data

import android.content.Context
import androidx.room.Room
import com.bowlingtracker.core.domain.model.Insights
import com.bowlingtracker.core.domain.model.SwingDirection
import kotlinx.coroutines.flow.Flow

/** App-level persistence facade over Room (kept simple; would be data:persistence in full DI). */
class DeliveryStore(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext, BowlingDatabase::class.java, "bowling.db",
    ).build()
    private val dao = db.deliveryDao()

    fun observeAll(): Flow<List<DeliveryEntity>> = dao.observeAll()

    suspend fun save(insights: Insights) {
        dao.insert(
            DeliveryEntity(
                recordedAtEpochMs = System.currentTimeMillis(),
                speedKmh = insights.releaseSpeed.kmPerHour,
                swingDirection = insights.swing.direction.name,
                swingMeters = insights.swing.lateral.meters,
                confidence = insights.speedConfidence.level.name,
                engineVersion = insights.engineVersion,
            ),
        )
    }
}
