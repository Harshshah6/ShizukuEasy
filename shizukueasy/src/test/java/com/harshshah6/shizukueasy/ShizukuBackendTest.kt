package com.harshshah6.shizukueasy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShizukuBackendTest {

    @Test
    fun `fromUid returns ROOT for UID 0`() {
        assertThat(ShizukuBackend.fromUid(0)).isEqualTo(ShizukuBackend.ROOT)
    }

    @Test
    fun `fromUid returns ADB for UID 2000`() {
        assertThat(ShizukuBackend.fromUid(2000)).isEqualTo(ShizukuBackend.ADB)
    }

    @Test
    fun `fromUid returns UNKNOWN for other UIDs`() {
        assertThat(ShizukuBackend.fromUid(1000)).isEqualTo(ShizukuBackend.UNKNOWN)
        assertThat(ShizukuBackend.fromUid(-1)).isEqualTo(ShizukuBackend.UNKNOWN)
        assertThat(ShizukuBackend.fromUid(9999)).isEqualTo(ShizukuBackend.UNKNOWN)
    }

    @Test
    fun `UID constants are correct`() {
        assertThat(ShizukuBackend.UID_ROOT).isEqualTo(0)
        assertThat(ShizukuBackend.UID_SHELL).isEqualTo(2000)
    }

    @Test
    fun `all enum values are present`() {
        assertThat(ShizukuBackend.entries).containsExactly(
            ShizukuBackend.UNKNOWN,
            ShizukuBackend.ADB,
            ShizukuBackend.ROOT
        )
    }
}
