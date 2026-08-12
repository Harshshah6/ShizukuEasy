package com.harshshah6.shizukueasy.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShizukuErrorTest {

    @Test
    fun `Unavailable has correct default message`() {
        val error = ShizukuError.Unavailable()
        assertThat(error.message).isEqualTo("Shizuku is not available.")
        assertThat(error.cause).isNull()
    }

    @Test
    fun `PermissionDenied has correct default message`() {
        val error = ShizukuError.PermissionDenied()
        assertThat(error.message).isEqualTo("Shizuku permission is not granted.")
    }

    @Test
    fun `custom message is preserved`() {
        val error = ShizukuError.Unsupported("Requires API 30+")
        assertThat(error.message).isEqualTo("Requires API 30+")
    }

    @Test
    fun `cause is preserved`() {
        val cause = RuntimeException("inner")
        val error = ShizukuError.OperationFailed("outer", cause)
        assertThat(error.cause).isSameInstanceAs(cause)
    }

    @Test
    fun `InsufficientPrivilege carries message`() {
        val error = ShizukuError.InsufficientPrivilege("Requires root backend.")
        assertThat(error.message).isEqualTo("Requires root backend.")
    }

    @Test
    fun `toString includes class name and message`() {
        val error = ShizukuError.Unavailable("test message")
        assertThat(error.toString()).contains("Unavailable")
        assertThat(error.toString()).contains("test message")
    }

    @Test
    fun `error types are distinct sealed subtypes`() {
        val errors = listOf(
            ShizukuError.Unavailable(),
            ShizukuError.PermissionDenied(),
            ShizukuError.Unsupported("test"),
            ShizukuError.InsufficientPrivilege("test"),
            ShizukuError.OperationFailed("test")
        )

        assertThat(errors.map { it::class }).containsNoDuplicates()
    }

    @Test
    fun `when expression covers all error types`() {
        val error: ShizukuError = ShizukuError.Unavailable()
        val handled = when (error) {
            is ShizukuError.Unavailable -> "unavailable"
            is ShizukuError.PermissionDenied -> "denied"
            is ShizukuError.Unsupported -> "unsupported"
            is ShizukuError.InsufficientPrivilege -> "privilege"
            is ShizukuError.OperationFailed -> "failed"
        }
        assertThat(handled).isEqualTo("unavailable")
    }
}
