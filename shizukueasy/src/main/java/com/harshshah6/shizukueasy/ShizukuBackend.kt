package com.harshshah6.shizukueasy

/**
 * Identifies the privilege level of the running Shizuku server.
 *
 * The backend determines what operations are available. [ROOT] (UID 0) has
 * full system access, while [ADB] (UID 2000, shell) is subject to shell-level
 * SELinux and permission restrictions.
 */
enum class ShizukuBackend {
    /** Backend has not been determined (Shizuku not connected). */
    UNKNOWN,

    /** Shizuku is running via ADB/wireless debugging (UID 2000). */
    ADB,

    /** Shizuku is running via root (UID 0). */
    ROOT;

    companion object {
        /** UID for the Android shell user. */
        internal const val UID_SHELL = 2000

        /** UID for the root user. */
        internal const val UID_ROOT = 0

        /**
         * Determines the backend from the Shizuku server UID.
         * @param uid The UID returned by `Shizuku.getUid()`.
         */
        @JvmStatic
        fun fromUid(uid: Int): ShizukuBackend = when (uid) {
            UID_ROOT -> ROOT
            UID_SHELL -> ADB
            else -> UNKNOWN
        }
    }
}
