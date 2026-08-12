package com.harshshah6.shizukueasy

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShizukuStatusTest {

    @Test
    fun `INITIAL status has correct default values`() {
        val status = ShizukuStatus.INITIAL
        assertThat(status.connection).isEqualTo(ConnectionState.NOT_INITIALIZED)
        assertThat(status.permission).isEqualTo(PermissionState.UNKNOWN)
        assertThat(status.backend).isEqualTo(ShizukuBackend.UNKNOWN)
        assertThat(status.isAvailable).isFalse()
        assertThat(status.isAuthorized).isFalse()
        assertThat(status.isReady).isFalse()
    }

    @Test
    fun `isReady requires both connection and permission`() {
        val connected = ShizukuStatus(
            ConnectionState.CONNECTED,
            PermissionState.GRANTED,
            ShizukuBackend.ADB
        )
        assertThat(connected.isReady).isTrue()
    }

    @Test
    fun `isReady is false when connected but not authorized`() {
        val unauthorized = ShizukuStatus(
            ConnectionState.CONNECTED,
            PermissionState.DENIED,
            ShizukuBackend.ADB
        )
        assertThat(unauthorized.isAvailable).isTrue()
        assertThat(unauthorized.isAuthorized).isFalse()
        assertThat(unauthorized.isReady).isFalse()
    }

    @Test
    fun `isReady is false when authorized but disconnected`() {
        val disconnected = ShizukuStatus(
            ConnectionState.DISCONNECTED,
            PermissionState.GRANTED,
            ShizukuBackend.UNKNOWN
        )
        assertThat(disconnected.isAvailable).isFalse()
        assertThat(disconnected.isAuthorized).isTrue()
        assertThat(disconnected.isReady).isFalse()
    }

    @Test
    fun `isReady is false when binder is dead`() {
        val dead = ShizukuStatus(
            ConnectionState.DEAD,
            PermissionState.GRANTED,
            ShizukuBackend.ADB
        )
        assertThat(dead.isAvailable).isFalse()
        assertThat(dead.isReady).isFalse()
    }

    @Test
    fun `isAvailable only when CONNECTED`() {
        assertThat(ShizukuStatus(ConnectionState.CONNECTED, PermissionState.UNKNOWN, ShizukuBackend.UNKNOWN).isAvailable).isTrue()
        assertThat(ShizukuStatus(ConnectionState.DISCONNECTED, PermissionState.UNKNOWN, ShizukuBackend.UNKNOWN).isAvailable).isFalse()
        assertThat(ShizukuStatus(ConnectionState.DEAD, PermissionState.UNKNOWN, ShizukuBackend.UNKNOWN).isAvailable).isFalse()
        assertThat(ShizukuStatus(ConnectionState.NOT_INITIALIZED, PermissionState.UNKNOWN, ShizukuBackend.UNKNOWN).isAvailable).isFalse()
    }

    @Test
    fun `isAuthorized only when GRANTED`() {
        assertThat(ShizukuStatus(ConnectionState.CONNECTED, PermissionState.GRANTED, ShizukuBackend.ADB).isAuthorized).isTrue()
        assertThat(ShizukuStatus(ConnectionState.CONNECTED, PermissionState.DENIED, ShizukuBackend.ADB).isAuthorized).isFalse()
        assertThat(ShizukuStatus(ConnectionState.CONNECTED, PermissionState.DENIED_FOREVER, ShizukuBackend.ADB).isAuthorized).isFalse()
        assertThat(ShizukuStatus(ConnectionState.CONNECTED, PermissionState.UNKNOWN, ShizukuBackend.ADB).isAuthorized).isFalse()
    }

    @Test
    fun `data class equality works`() {
        val a = ShizukuStatus(ConnectionState.CONNECTED, PermissionState.GRANTED, ShizukuBackend.ROOT)
        val b = ShizukuStatus(ConnectionState.CONNECTED, PermissionState.GRANTED, ShizukuBackend.ROOT)
        assertThat(a).isEqualTo(b)
    }

    @Test
    fun `data class inequality works`() {
        val a = ShizukuStatus(ConnectionState.CONNECTED, PermissionState.GRANTED, ShizukuBackend.ROOT)
        val b = ShizukuStatus(ConnectionState.CONNECTED, PermissionState.GRANTED, ShizukuBackend.ADB)
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `copy modifies individual fields`() {
        val original = ShizukuStatus.INITIAL
        val connected = original.copy(connection = ConnectionState.CONNECTED)
        assertThat(connected.connection).isEqualTo(ConnectionState.CONNECTED)
        assertThat(connected.permission).isEqualTo(PermissionState.UNKNOWN)
        assertThat(connected.backend).isEqualTo(ShizukuBackend.UNKNOWN)
    }
}
