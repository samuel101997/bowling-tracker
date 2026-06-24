package com.bowlingtracker.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asFailure
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.core.domain.port.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Implements [MediaStore] for real recorded clips: extracts evenly-spaced frames
 * from the MP4 to JPEG files and reports their count + the effective sampling
 * fps so the engine can compute timing (ADR-0002, on-device record-then-process).
 *
 * The extracted frame file paths are encoded into the clipRef the engine reads
 * via FrameSequence; here we extract and return the metadata.
 */
class AndroidMediaStore(
    private val context: Context,
    private val maxFrames: Int = 60,
) : MediaStore {

    /** Directory holding extracted frames for the most recent clip. */
    fun framesDir(clipPath: String): File =
        File(context.cacheDir, "frames_${File(clipPath).nameWithoutExtension}").apply { mkdirs() }

    override suspend fun extractFrames(clipRef: String): Result<ClipFrames, DomainError> {
        val clipPath = clipRef.substringBefore("|")
        val file = File(clipPath)
        if (!file.exists()) return DomainError.Unexpected("Clip not found: $clipPath").asFailure()

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) {
                return DomainError.Unexpected("Clip has zero duration").asFailure()
            }
            val dir = framesDir(clipPath)
            dir.listFiles()?.forEach { it.delete() }

            // Sample up to maxFrames evenly across the clip.
            val frameCount = maxFrames
            val stepUs = (durationMs * 1000L) / frameCount
            var written = 0
            for (i in 0 until frameCount) {
                val tUs = i * stepUs
                val bmp: Bitmap = retriever.getFrameAtTime(
                    tUs, MediaMetadataRetriever.OPTION_CLOSEST,
                ) ?: continue
                // sequential names so the engine can index 0..written-1 with no gaps
                val out = File(dir, "f_%04d.jpg".format(written))
                FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                written++
            }
            if (written < 3) return DomainError.Unexpected("Too few frames extracted").asFailure()

            val effectiveFps = written.toDouble() / (durationMs / 1000.0)
            // clipRef now points at the frames directory so vision can list files.
            ClipFrames(
                clipRef = dir.absolutePath,
                frameCount = written,
                fps = effectiveFps,
            ).asSuccess()
        } catch (e: Exception) {
            DomainError.Unexpected("Frame extraction failed: ${e.message}").asFailure()
        } finally {
            retriever.release()
        }
    }

    override suspend fun deleteClip(clipRef: String): Result<Unit, DomainError> {
        val clipPath = clipRef.substringBefore("|")
        File(clipPath).delete()
        framesDir(clipPath).deleteRecursively()
        return Unit.asSuccess()
    }
}
