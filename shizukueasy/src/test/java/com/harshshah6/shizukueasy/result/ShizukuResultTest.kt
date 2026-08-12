package com.harshshah6.shizukueasy.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShizukuResultTest {

    @Test
    fun `success result has correct value`() {
        val result = ShizukuResult.success("hello")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()).isEqualTo("hello")
        assertThat(result.getOrThrow()).isEqualTo("hello")
    }

    @Test
    fun `failure result has correct error`() {
        val error = ShizukuError.Unavailable()
        val result: ShizukuResult<String> = ShizukuResult.failure(error)
        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws on failure`() {
        val result = ShizukuResult.failure(ShizukuError.Unavailable("test"))
        result.getOrThrow()
    }

    @Test
    fun `getOrElse returns fallback on failure`() {
        val result: ShizukuResult<String> = ShizukuResult.failure(ShizukuError.Unavailable())
        val value = result.getOrElse { "fallback" }
        assertThat(value).isEqualTo("fallback")
    }

    @Test
    fun `getOrElse returns value on success`() {
        val result = ShizukuResult.success("original")
        val value = result.getOrElse { "fallback" }
        assertThat(value).isEqualTo("original")
    }

    @Test
    fun `map transforms success value`() {
        val result = ShizukuResult.success(42)
        val mapped = result.map { it.toString() }
        assertThat(mapped.getOrNull()).isEqualTo("42")
    }

    @Test
    fun `map preserves failure`() {
        val error = ShizukuError.PermissionDenied()
        val result: ShizukuResult<Int> = ShizukuResult.failure(error)
        val mapped = result.map { it.toString() }
        assertThat(mapped.isFailure).isTrue()
        assertThat((mapped as ShizukuResult.Failure).error).isSameInstanceAs(error)
    }

    @Test
    fun `onSuccess is called for success`() {
        var called = false
        ShizukuResult.success("value").onSuccess { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun `onSuccess is not called for failure`() {
        var called = false
        ShizukuResult.failure(ShizukuError.Unavailable()).onSuccess { called = true }
        assertThat(called).isFalse()
    }

    @Test
    fun `onFailure is called for failure`() {
        var called = false
        ShizukuResult.failure(ShizukuError.Unavailable()).onFailure { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun `onFailure is not called for success`() {
        var called = false
        ShizukuResult.success("value").onFailure { called = true }
        assertThat(called).isFalse()
    }

    @Test
    fun `chaining onSuccess and onFailure`() {
        var successValue: String? = null
        var failureError: ShizukuError? = null

        ShizukuResult.success("hello")
            .onSuccess { successValue = it }
            .onFailure { failureError = it }

        assertThat(successValue).isEqualTo("hello")
        assertThat(failureError).isNull()
    }

    @Test
    fun `static factory methods work correctly`() {
        val success = ShizukuResult.success(42)
        assertThat(success).isInstanceOf(ShizukuResult.Success::class.java)

        val failure = ShizukuResult.failure(ShizukuError.Unavailable())
        assertThat(failure).isInstanceOf(ShizukuResult.Failure::class.java)
    }
}
