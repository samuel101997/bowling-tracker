package com.bowlingtracker.data.media

import com.bowlingtracker.core.common.Result
import com.bowlingtracker.core.common.asSuccess
import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.domain.port.ClipFrames
import com.bowlingtracker.core.domain.port.MediaStore

/**
 * Implements the [MediaStore] port. Extracts frames from a recorded clip and
 * manages clip lifecycle (ARCHITECTURE.md §5, ADR-0002).
 *
 * STATUS: minimal implementation. Frame extraction via MediaMetadataRetriever /
 * MediaCodec is wired in a follow-up; for now it reports the frame count the
 * capture layer recorded so the pipeline can run end-to-end on device.
 */
class AndroidMediaStore : MediaStore {

    override suspend fun extractFrames(clipRef: String): Result<ClipFrames, DomainError> {
        // TODO(capture): decode real frames with MediaCodec. For the first
        // runnable build we pass through the metadata recorded at capture time,
        // encoded in clipRef as "path|frameCount|fps".
        val parts = clipRef.split("|")
        val frameCount = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val fps = parts.getOrNull(2)?.toDoubleOrNull() ?: 30.0
        return ClipFrames(clipRef = parts.first(), frameCount = frameCount, fps = fps).asSuccess()
    }

    override suspend fun deleteClip(clipRef: String): Result<Unit, DomainError> =
        Unit.asSuccess()
}
