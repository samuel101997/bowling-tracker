package com.bowlingtracker.core.common

/**
 * A typed success/failure result used across module boundaries.
 *
 * Per ARCHITECTURE.md P9 ("fail loud"), modules return [Result] for expected
 * failure modes instead of throwing. Never return a silently-wrong value.
 */
sealed interface Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>
    data class Failure<E>(val error: E) : Result<Nothing, E>

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.value
    fun errorOrNull(): E? = (this as? Failure)?.error
}

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Failure -> this
    }

inline fun <T, E, R> Result<T, E>.flatMap(transform: (T) -> Result<R, E>): Result<R, E> =
    when (this) {
        is Result.Success -> transform(value)
        is Result.Failure -> this
    }

inline fun <T, E> Result<T, E>.getOrElse(fallback: (E) -> @UnsafeVariance T): T =
    when (this) {
        is Result.Success -> value
        is Result.Failure -> fallback(error)
    }

fun <T> T.asSuccess(): Result<T, Nothing> = Result.Success(this)
fun <E> E.asFailure(): Result<Nothing, E> = Result.Failure(this)
