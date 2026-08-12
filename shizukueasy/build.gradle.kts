plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "com.harshshah6.shizukueasy"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.shizuku.api)
    api(libs.shizuku.provider)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}

val keyIdProp = providers.gradleProperty("signingInMemoryKeyId").orNull
    ?: providers.gradleProperty("signing.keyId").orNull

if (!keyIdProp.isNullOrBlank()) {
    val cleanKeyId = keyIdProp.removePrefix("0x").removePrefix("0X")
    if (cleanKeyId.length > 8) {
        val shortKeyId = cleanKeyId.takeLast(8)
        extra["signingInMemoryKeyId"] = shortKeyId
        extra["signing.keyId"] = shortKeyId
    }
}

mavenPublishing {
    coordinates(
        groupId = "com.harshbits.shizukueasy",
        artifactId = "core",
        version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")
    )

    pom {
        name.set("ShizukuEasy")
        description.set("Shizuku without the boilerplate. A high-level, developer-friendly wrapper around the Shizuku API for Android.")
        url.set("https://github.com/Harshshah6/ShizukuEasy")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("harshshah6")
                name.set("Harsh Shah")
                url.set("https://github.com/Harshshah6")
            }
        }

        scm {
            url.set("https://github.com/Harshshah6/ShizukuEasy")
            connection.set("scm:git:git://github.com/Harshshah6/ShizukuEasy.git")
            developerConnection.set("scm:git:ssh://git@github.com/Harshshah6/ShizukuEasy.git")
        }
    }

    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
