# ShizukuEasy

A high-level, developer-friendly wrapper around the [Shizuku](https://github.com/RikkaApps/Shizuku) API for Android.

ShizukuEasy removes the boilerplate of Shizuku setup — binder connections, permission handling, lifecycle management, and backend detection — so you can focus on using Shizuku's capabilities.

## Quick Start

### Kotlin

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ShizukuEasy.init(this)

        if (ShizukuEasy.ready) {
            // Shizuku is connected and permitted — use it!
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuEasy.destroy()
    }
}
```

### Java

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShizukuEasy.init(this);

        if (ShizukuEasy.isReady()) {
            // Shizuku is connected and permitted — use it!
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShizukuEasy.destroy();
    }
}
```

## Installation

Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.harshshah6:shizukueasy:<version>")
}
```

> **Note:** ShizukuEasy transitively includes the official Shizuku API and provider dependencies. You do not need to add them separately.

### Manifest Setup

Add the Shizuku provider to your app's `AndroidManifest.xml` inside the `<application>` tag:

```xml
<provider
    android:name="rikka.shizuku.ShizukuProvider"
    android:authorities="${applicationId}.shizuku"
    android:multiprocess="false"
    android:enabled="true"
    android:exported="true"
    android:permission="android.permission.INTERACT_ACROSS_USERS_FULL" />
```

This is required by the Shizuku API and cannot be shipped inside the library because the authority must be unique per application.

## API Reference

### Initialization & Lifecycle

| Method | Description |
|---|---|
| `ShizukuEasy.init(context)` | Initializes ShizukuEasy. Call once in `onCreate()`. |
| `ShizukuEasy.destroy()` | Tears down listeners. Call in `onDestroy()`. |

### State Properties

| Property | Type | Description |
|---|---|---|
| `state` | `ShizukuState` | Current detailed state. |
| `available` | `Boolean` | Shizuku binder is alive and reachable. |
| `permissionGranted` | `Boolean` | Shizuku permission is granted. |
| `ready` | `Boolean` | `available && permissionGranted` — safe to use Shizuku. |
| `backend` | `ShizukuBackend` | `ADB`, `ROOT`, or `UNKNOWN`. |
| `isRoot` | `Boolean` | Server running as root (UID 0). |
| `isShell` | `Boolean` | Server running as shell/ADB (UID 2000). |
| `serverVersion` | `Int` | Shizuku server version, or -1 if unavailable. |

### Permission

| Method | Description |
|---|---|
| `requestPermission(callback)` | Requests permission with a result callback. |
| `requestPermission()` | Requests permission without a callback. |
| `permissionDeniedForever` | `true` if the user permanently denied permission. |

### State Observation

```kotlin
ShizukuEasy.addStateChangeListener { state ->
    when (state) {
        ShizukuState.READY -> { /* Connected and permitted */ }
        ShizukuState.UNAUTHORIZED -> { /* Connected but needs permission */ }
        ShizukuState.UNAVAILABLE -> { /* Shizuku not running */ }
        ShizukuState.DEAD -> { /* Binder died, waiting for reconnection */ }
        ShizukuState.NOT_INITIALIZED -> { /* init() not called or destroy() was called */ }
    }
}
```

### System Services

Access Android system services with elevated privileges:

```kotlin
val pm = ShizukuEasy.getSystemService("package") { binder ->
    IPackageManager.Stub.asInterface(binder)
}
```

> System service access requires hidden API interfaces (AIDL stubs). This is the same requirement as using the raw Shizuku API.

## States

```
NOT_INITIALIZED ──► init() ──► UNAVAILABLE (Shizuku not running)
                               UNAUTHORIZED (running, no permission)
                               READY (running + permitted)

READY / UNAUTHORIZED ──► binder dies ──► DEAD ──► binder reconnects ──► READY / UNAUTHORIZED

destroy() ──► NOT_INITIALIZED
```

## Backend Detection

ShizukuEasy detects whether the Shizuku server is running via **ADB** (wireless debugging / USB) or **root** (Magisk/Sui):

```kotlin
when (ShizukuEasy.backend) {
    ShizukuBackend.ADB -> { /* Shell-level access (UID 2000) */ }
    ShizukuBackend.ROOT -> { /* Root-level access (UID 0) */ }
    ShizukuBackend.UNKNOWN -> { /* Not connected */ }
}

// Convenience:
if (ShizukuEasy.isRoot) { /* ... */ }
if (ShizukuEasy.isShell) { /* ... */ }
```

## Requirements

- **Android 7.0+** (API 24)
- User must have the [Shizuku](https://shizuku.rikka.app/download/) app installed and running
- On non-rooted devices: Shizuku must be started via ADB or wireless debugging
- On rooted devices: [Sui](https://github.com/RikkaApps/Sui) (Magisk module) can start Shizuku automatically

## Relationship to the Shizuku API

ShizukuEasy is built on top of the official [Shizuku API](https://github.com/RikkaApps/Shizuku-API) (`dev.rikka.shizuku:api`). It does not replace, fork, or modify the Shizuku API — it wraps it with a simpler interface.

The official Shizuku API is included as a transitive dependency. If you need to access lower-level Shizuku functionality (e.g., `Shizuku.newProcess()`, `Shizuku.peekUserService()`), you can use the official API directly alongside ShizukuEasy.

## Limitations

- **Provider declaration**: Must be in your app's manifest (cannot be shipped in the library).
- **Hidden APIs**: Accessing system services via `getSystemService()` requires AIDL stubs for hidden Android APIs. ShizukuEasy provides the plumbing but not the stubs themselves.
- **ADB backend**: When running via ADB, Shizuku has shell-level permissions (UID 2000). Some operations require root.
- **Shizuku availability**: On non-rooted devices, Shizuku must be manually restarted after every reboot (or started via wireless debugging on Android 11+).

## Project Structure

```
ShizukuEasy/
├── app/              → Demo application
├── shizukueasy/      → Library module
│   └── src/main/java/com/harshshah6/shizukueasy/
│       ├── ShizukuEasy.kt              → Public facade
│       ├── ShizukuState.kt             → State enum
│       ├── ShizukuBackend.kt           → Backend enum
│       ├── ShizukuServiceFactory.kt    → System service access
│       ├── OnStateChangeListener.kt    → State callback
│       ├── OnPermissionResultListener.kt → Permission callback
│       └── internal/                   → Implementation details
├── README.md
└── LICENSE
```

## License

```
Copyright 2026 harshshah6

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
