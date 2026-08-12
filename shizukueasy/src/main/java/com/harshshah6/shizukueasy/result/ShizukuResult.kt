package com.harshshah6.shizukueasy.result

/**
 * Represents the outcome of a Shizuku operation.
 *
 * Operations in the [capabilities][com.harshshah6.shizukueasy.capabilities] layer
 * return `ShizukuResult` instead of throwing exceptions for expected failures
 * (Shizuku unavailable, permission denied, etc.).
 *
 * ```kotlin
 * when (val result = ShizukuEasy.packages.getInstalled()) {
 *     is ShizukuResult.Success -> handlePackages(result.value)
 *     is ShizukuResult.Failure -> handleError(result.error)
 * }
 * ```
 *
 * @param T The type of the success value.
 */
public sealed class ShizukuResult<out T> {

    /** The operation succeeded with [value]. */
    public data class Success<T>(val value: T) : ShizukuResult<T>()

    /** The operation failed with [error]. */
    public data class Failure(val error: ShizukuError) : ShizukuResult<Nothing>()

    /** `true` if this is a [Success]. */
    public val isSuccess: Boolean get() = this is Success

    /** `true` if this is a [Failure]. */
    public val isFailure: Boolean get() = this is Failure

    /** Returns the success value, or `null` if this is a [Failure]. */
    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    /** Returns the success value, or throws [IllegalStateException] if this is a [Failure]. */
    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw IllegalStateException(error.message, error.cause)
    }

    /** Returns the success value, or the result of [fallback] if this is a [Failure]. */
    public inline fun getOrElse(fallback: (ShizukuError) -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> fallback(error)
    }

    /** Transforms the success value. */
    public inline fun <R> map(transform: (T) -> R): ShizukuResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    /** Performs [action] if this is a [Success]. Returns `this` for chaining. */
    public inline fun onSuccess(action: (T) -> Unit): ShizukuResult<T> {
        if (this is Success) action(value)
        return this
    }

    /** Performs [action] if this is a [Failure]. Returns `this` for chaining. */
    public inline fun onFailure(action: (ShizukuError) -> Unit): ShizukuResult<T> {
        if (this is Failure) action(error)
        return this
    }

    public companion object {
        /** Creates a successful result. */
        @JvmStatic
        public fun <T> success(value: T): ShizukuResult<T> = Success(value)

        /** Creates a failure result. */
        @JvmStatic
        public fun failure(error: ShizukuError): ShizukuResult<Nothing> = Failure(error)
    }
}
