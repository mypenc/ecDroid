plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val repoRoot = rootDir.parentFile
val workspaceVersion: String =
    Regex("""(?m)^version\s*=\s*"([^"]+)"""").find(File(repoRoot, "Cargo.toml").readText())?.groupValues?.get(1) ?: "0.0.0"

// The launcher icon comes from the repository's own icon set, so it isn't duplicated here.
val generatedRes = layout.buildDirectory.dir("generated/res")
val copyIcon by tasks.registering(Copy::class) {
    from(File(repoRoot, "assets/app-icon/hicolor/512x512/apps/ai.storyteller.effectcraft.png"))
    rename { "ic_launcher.png" }
    into(generatedRes.map { it.dir("mipmap-xxxhdpi") })
}

android {
    namespace = "ai.storyteller.effectcraft"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.storyteller.effectcraft"
        minSdk = 29 // scoped storage; AAudio and Vulkan are solid from here
        targetSdk = 35
        versionCode = 1
        versionName = workspaceVersion
        // `android/scripts/build.sh` puts the Rust library here (cargo-ndk).
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    sourceSets["main"].res.srcDir(generatedRes)

    buildTypes {
        release {
            isMinifyEnabled = false // Kotlin side is tiny; JNI entry points must keep their names
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs.useLegacyPackaging = false }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Resources") }.configureEach { dependsOn(copyIcon) }

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    // GameActivity: the input-method (soft keyboard) support android-activity builds on.
    // Do NOT enable prefab for it: android-activity brings its own native glue.
    implementation("androidx.games:games-activity:4.4.0")
}
