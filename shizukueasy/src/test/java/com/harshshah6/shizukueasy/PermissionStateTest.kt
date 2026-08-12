package com.harshshah6.shizukueasy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionStateTest {

    @Test
    fun `all states are present`() {
        assertThat(PermissionState.entries).containsExactly(
            PermissionState.UNKNOWN,
            PermissionState.GRANTED,
            PermissionState.DENIED,
            PermissionState.DENIED_FOREVER
        )
    }

    @Test
    fun `states have correct names`() {
        assertThat(PermissionState.UNKNOWN.name).isEqualTo("UNKNOWN")
        assertThat(PermissionState.GRANTED.name).isEqualTo("GRANTED")
        assertThat(PermissionState.DENIED.name).isEqualTo("DENIED")
        assertThat(PermissionState.DENIED_FOREVER.name).isEqualTo("DENIED_FOREVER")
    }

    @Test
    fun `DENIED and DENIED_FOREVER are distinct`() {
        assertThat(PermissionState.DENIED).isNotEqualTo(PermissionState.DENIED_FOREVER)
    }

    @Test
    fun `valueOf round-trips`() {
        PermissionState.entries.forEach { state ->
            assertThat(PermissionState.valueOf(state.name)).isEqualTo(state)
        }
    }
}
