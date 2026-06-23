package com.bowlingtracker.core.domain.port

import com.bowlingtracker.core.common.DomainError
import com.bowlingtracker.core.common.Result

/** Port: stores capture clips and extracts frames. Impl: data:media. */
interface MediaStore {
    suspend fun extractFrames(clipRef: String): Result<ClipFrames, DomainError>
    suspend fun deleteClip(clipRef: String): Result<Unit, DomainError>
}
