package com.harshshah6.shizukueasy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConnectionStateTest {

    @Test
    fun `all states are present`() {
        assertThat(ConnectionState.entries).containsExactly(
            ConnectionState.NOT_INITIALIZED,
            ConnectionState.DISCONNECTED,
            ConnectionState.CONNECTED,
            ConnectionState.DEAD
        )
    }

    @Test
    fun `states have correct names`() {
        assertThat(ConnectionState.NOT_INITIALIZED.name).isEqualTo("NOT_INITIALIZED")
        assertThat(ConnectionState.DISCONNECTED.name).isEqualTo("DISCONNECTED")
        assertThat(ConnectionState.CONNECTED.name).isEqualTo("CONNECTED")
        assertThat(ConnectionState.DEAD.name).isEqualTo("DEAD")
    }

    @Test
    fun `valueOf round-trips`() {
        ConnectionState.entries.forEach { state ->
            assertThat(ConnectionState.valueOf(state.name)).isEqualTo(state)
        }
    }
}
