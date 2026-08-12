# ShizukuEasy

> **Shizuku without the boilerplate.**

A high-level, developer-friendly wrapper around the [Shizuku](https://github.com/RikkaApps/Shizuku) API for Android. ShizukuEasy handles binder connections, permission management, and state tracking so you can focus on building features.

[![Maven Central](https://img.shields.io/maven-central/v/com.harshshah6.shizukueasy/core)](https://central.sonatype.com/artifact/com.harshshah6.shizukueasy/core)
[![License](https://img.shields.io/github/license/Harshshah6/ShizukuEasy)](LICENSE)

## Installation

```kotlin
dependencies {
    implementation("com.harshshah6.shizukueasy:core:0.1.0")
}
```

> ShizukuEasy transitively includes the official Shizuku API and provider dependencies. The `ShizukuProvider` manifest entry is automatically merged — no manual XML needed.

## Quick Start

```kotlin
// Initialize once
ShizukuEasy.init(this)

// Check readiness
if (ShizukuEasy.isReady) {
    val packages = ShizukuEasy.packages.getInstalled()
}

// Or react to readiness
ShizukuEasy.onReady {
    val packages = ShizukuEasy.packages.getInstalled()
}
```

That's it. No binder listeners, no permission codes, no `SystemServiceHelper`.

## Permission Handling

```kotlin
// Request permission with a callback
ShizukuEasy.requestPermission { granted ->
    if (granted) {
        // Ready to use
    }
}

// Or without a callback — observe via status listener
ShizukuEasy.requestPermission()
```

ShizukuEasy internally manages request codes, listener registration/removal, duplicate requests, and lifecycle.

### Observing Status Changes

```kotlin
ShizukuEasy.addStatusListener { status ->
    // status.connection — ConnectionState (CONNECTED, DISCONNECTED, DEAD, ...)
    // status.permission — PermissionState (GRANTED, DENIED, DENIED_FOREVER, ...)
    // status.backend    — ShizukuBackend  (ADB, ROOT, UNKNOWN)
    // status.isReady    — Boolean (connected + permitted)
}
```

## High-Level APIs

ShizukuEasy provides convenient capability APIs for common Shizuku use cases:

### Packages

```kotlin
// Check if a package is installed
ShizukuEasy.packages.isInstalled("com.example.app")

// List all installed packages
ShizukuEasy.packages.getInstalled()

// Enable/disable a package
ShizukuEasy.packages.enable("com.example.app")
ShizukuEasy.packages.disable("com.example.app")

// Clear app data
ShizukuEasy.packages.clearData("com.example.app")
```

### Activities

```kotlin
// Force-stop an application
ShizukuEasy.activities.forceStop("com.example.app")
```

### Users

```kotlin
// Get current user ID
ShizukuEasy.users.getCurrentUserId()
```

### Power (requires root)

```kotlin
ShizukuEasy.power.reboot()
ShizukuEasy.power.shutdown()
```

### Shell

```kotlin
ShizukuEasy.shell.exec("pm list packages").onSuccess { output ->
    println(output.stdout)
    println("Exit code: ${output.exitCode}")
}
```

### Result Handling

All capability APIs return `ShizukuResult<T>` instead of throwing exceptions:

```kotlin
when (val result = ShizukuEasy.packages.getInstalled()) {
    is ShizukuResult.Success -> handlePackages(result.value)
    is ShizukuResult.Failure -> when (result.error) {
        is ShizukuError.Unavailable -> showShizukuRequired()
        is ShizukuError.PermissionDenied -> requestPermission()
        is ShizukuError.InsufficientPrivilege -> showRootRequired()
        else -> showError(result.error.message)
    }
}

// Or use convenience methods
ShizukuEasy.packages.getInstalled()
    .onSuccess { packages -> /* ... */ }
    .onFailure { error -> /* ... */ }

// Or get the value directly
val packages = ShizukuEasy.packages.getInstalled().getOrNull()
val count = ShizukuEasy.packages.getInstalled().getOrElse { emptyList() }.size
```

## Advanced API

For experienced developers who need raw access to Shizuku functionality:

```kotlin
// Raw system service access (requires AIDL stubs)
val pm = ShizukuEasy.advanced.getSystemService("package") { binder ->
    IPackageManager.Stub.asInterface(binder)
}

// Raw binder
val binder = ShizukuEasy.advanced.getBinder()

// UserService
ShizukuEasy.advanced.userService.bind(
    serviceClass = MyPrivilegedService::class.java,
    converter = { IMyService.Stub.asInterface(it) }
) { result ->
    result.onSuccess { service -> service.doWork() }
}

// Direct Shizuku queries
val uid = ShizukuEasy.advanced.getServerUid()
val version = ShizukuEasy.advanced.getServerVersion()
```

## UserService

ShizukuEasy provides a clean abstraction over Shizuku's UserService for running code with elevated privileges:

```kotlin
// Bind a UserService (hides UserServiceArgs, ServiceConnection, etc.)
ShizukuEasy.advanced.userService.bind(
    serviceClass = MyService::class.java,
    converter = { IMyService.Stub.asInterface(it) }
) { result ->
    result.onSuccess { service ->
        // service runs with shell/root identity
    }
}

// Cleanup
ShizukuEasy.advanced.userService.unbind(MyService::class.java)
```

The raw `Shizuku.bindUserService()` / `Shizuku.unbindUserService()` APIs are also available through `ShizukuEasy.advanced` for full control.

## Backend Limitations

ShizukuEasy detects whether the Shizuku server is running via **ADB** (shell, UID 2000) or **root** (UID 0):

```kotlin
when (ShizukuEasy.backend) {
    ShizukuBackend.ADB -> { /* Shell-level access */ }
    ShizukuBackend.ROOT -> { /* Full system access */ }
    ShizukuBackend.UNKNOWN -> { /* Not connected */ }
}
```

Some operations (e.g., `power.reboot()`) require root and will return `ShizukuError.InsufficientPrivilege` when running via ADB.

## Requirements

- **Android 7.0+** (API 24)
- [Shizuku](https://shizuku.rikka.app/download/) app installed and running
- Non-rooted devices: Shizuku started via ADB or wireless debugging
- Rooted devices: [Sui](https://github.com/RikkaApps/Sui) (Magisk module) starts Shizuku automatically

## Java Usage

All public APIs are Java-compatible:

```java
// Initialize
ShizukuEasy.init(this);

// Check readiness
if (ShizukuEasy.isReady()) {
    // Use capabilities
}

// Request permission
ShizukuEasy.requestPermission(granted -> {
    if (granted) { /* ... */ }
});

// Status observation
ShizukuEasy.addStatusListener(status -> {
    if (status.isReady()) { /* ... */ }
});
```

## Maven Central

ShizukuEasy is published to Maven Central:

```kotlin
// Gradle Kotlin DSL
implementation("com.harshshah6.shizukueasy:core:0.1.0")
```

```groovy
// Gradle Groovy DSL
implementation 'com.harshshah6.shizukueasy:core:0.1.0'
```

## Relationship to the Shizuku API

ShizukuEasy is built on top of the official [Shizuku API](https://github.com/RikkaApps/Shizuku-API) (`dev.rikka.shizuku:api`). It does **not** replace, fork, or modify the Shizuku API — it wraps it with a simpler interface.

ShizukuEasy does **not** bypass Shizuku's security or permission model. The user must still have Shizuku installed and grant your app permission.

## Project Structure

```
ShizukuEasy/
├── app/                    → Demo application
└── shizukueasy/            → Library module (published as :core)
    └── src/main/java/com/harshshah6/shizukueasy/
        ├── ShizukuEasy.kt              → Public facade
        ├── ShizukuStatus.kt            → Composite status
        ├── ConnectionState.kt          → Connection enum
        ├── PermissionState.kt          → Permission enum
        ├── ShizukuBackend.kt           → Backend enum
        ├── result/                     → Result/error types
        ├── capabilities/               → High-level APIs
        ├── advanced/                   → Advanced escape hatch
        ├── userservice/                → UserService abstraction
        └── internal/                   → Implementation details
```

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Submit a pull request

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
