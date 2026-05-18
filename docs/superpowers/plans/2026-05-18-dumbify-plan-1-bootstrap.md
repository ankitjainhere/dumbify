# Dumbify Plan 1 — Bootstrap, Device Owner, Data Layer

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a Kotlin/Compose Android project that can be set as Device Owner, with a working Room data layer, SecurePrefs, and tested PinManager. No UI beyond a placeholder Activity. Deliverable: an installable debug APK that survives `adb dpm set-device-owner` and unit tests pass in CI.

**Architecture:** Single Gradle module `:app` at repo root. Two build types: `debug` (no tamper restrictions, ships `DevNukeReceiver`) and `release` (applies all restrictions). Hilt for DI. Room for DB. EncryptedSharedPreferences + Argon2id for PIN storage. JUnit5 + MockK for unit tests.

**Tech Stack:** Kotlin 1.9.x, Android Gradle Plugin 8.x, Compose BOM, Hilt 2.x, Room 2.x, WorkManager 2.x, EncryptedSharedPreferences (`androidx.security:security-crypto`), `de.mkammerer:argon2-jvm`, JUnit5, MockK.

**Spec references:** [00-overview.md](../specs/2026-05-18-dumbify-design/00-overview.md), [01-architecture.md](../specs/2026-05-18-dumbify-design/01-architecture.md), [02-data-model.md](../specs/2026-05-18-dumbify-design/02-data-model.md), [05-module-structure.md](../specs/2026-05-18-dumbify-design/05-module-structure.md), [09-development-safety.md](../specs/2026-05-18-dumbify-design/09-development-safety.md).

**Tracker:** Updates milestones M0, M1, M2 in [TRACKER.md](../specs/2026-05-18-dumbify-design/TRACKER.md).

---

## File map

Files this plan creates (relative to repo root `/Users/batman/Documents/limit-phone/`):

```
.gitignore
LICENSE                                         (GPLv3)
README.md
settings.gradle.kts
build.gradle.kts                                (root)
gradle.properties
gradle/wrapper/gradle-wrapper.properties
gradle/libs.versions.toml                       (version catalog)
.github/workflows/ci.yml

app/build.gradle.kts
app/proguard-rules.pro

app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
app/src/main/res/xml/device_admin.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml          (placeholder)

app/src/main/java/com/dumbify/app/DumbifyApp.kt
app/src/main/java/com/dumbify/app/MainActivity.kt           (placeholder)

app/src/main/java/com/dumbify/app/admin/DumbifyDeviceAdminReceiver.kt
app/src/main/java/com/dumbify/app/admin/PolicyEnforcer.kt

app/src/main/java/com/dumbify/app/data/DumbifyDb.kt
app/src/main/java/com/dumbify/app/data/SecurePrefs.kt
app/src/main/java/com/dumbify/app/data/entities/Config.kt
app/src/main/java/com/dumbify/app/data/entities/AppRule.kt
app/src/main/java/com/dumbify/app/data/entities/Event.kt
app/src/main/java/com/dumbify/app/data/dao/ConfigDao.kt
app/src/main/java/com/dumbify/app/data/dao/AppRuleDao.kt
app/src/main/java/com/dumbify/app/data/dao/EventDao.kt

app/src/main/java/com/dumbify/app/policy/PinManager.kt
app/src/main/java/com/dumbify/app/policy/RuleStore.kt

app/src/main/java/com/dumbify/app/di/DataModule.kt
app/src/main/java/com/dumbify/app/di/PolicyModule.kt

app/src/main/java/com/dumbify/app/util/Clock.kt

app/src/debug/AndroidManifest.xml
app/src/debug/java/com/dumbify/app/debug/DevNukeReceiver.kt

app/src/test/java/com/dumbify/app/policy/PinManagerTest.kt
app/src/test/java/com/dumbify/app/policy/RuleStoreTest.kt
app/src/test/java/com/dumbify/app/policy/FakeClock.kt
app/src/test/java/com/dumbify/app/data/FakeSecurePrefs.kt

app/schemas/                                    (Room schema export dir; gitignored content but dir tracked)
```

Plan 1 does NOT create: any Compose UI screens, AppMonitorService, BlockScreenActivity, NotificationListener, BootReceiver — those land in Plans 2/3.

---

## Task 1: Initialize repo, license, .gitignore, README

**Files:**
- Create: `LICENSE`
- Create: `.gitignore`
- Create: `README.md`

- [ ] **Step 1: Initialize git repo (if not already)**

Run:
```bash
cd /Users/batman/Documents/limit-phone
git init -b main
```
Expected: `Initialized empty Git repository` (or no-op if already a repo).

- [ ] **Step 2: Write LICENSE (GPLv3)**

Fetch the canonical GPLv3 text and save it. Run:
```bash
curl -sSfL https://www.gnu.org/licenses/gpl-3.0.txt -o LICENSE
head -3 LICENSE
```
Expected: header lines beginning with `                    GNU GENERAL PUBLIC LICENSE`.

- [ ] **Step 3: Write `.gitignore`**

Create `/Users/batman/Documents/limit-phone/.gitignore`:

```gitignore
# Android Studio / Gradle
.gradle/
build/
local.properties
*.iml
.idea/
captures/
.cxx/
.kotlin/

# Generated APKs
app/release/
*.apk
*.aab
*.keystore
keystore.properties

# OS
.DS_Store
Thumbs.db

# Room schema export
app/schemas/*.json
!app/schemas/.gitkeep

# Logs
*.log
```

- [ ] **Step 4: Write `README.md`**

Create `/Users/batman/Documents/limit-phone/README.md`:

```markdown
# Dumbify

Free, open-source Android app that turns a smartphone into a focused / "dumb" phone
using Android's Device Owner (MDM) APIs.

Inspired by limitphone.com — but free, open source, and self-hostable.

## Status

Pre-alpha. See [docs/superpowers/specs/2026-05-18-dumbify-design](docs/superpowers/specs/2026-05-18-dumbify-design)
for the design and [docs/superpowers/plans](docs/superpowers/plans) for the implementation plans.

## License

GPLv3. See [LICENSE](LICENSE).
```

- [ ] **Step 5: Commit**

```bash
git add LICENSE .gitignore README.md
git commit -m "chore: initial repo scaffolding with GPLv3 license"
```

---

## Task 2: Gradle scaffolding — root files

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: Write `settings.gradle.kts`**

Create `/Users/batman/Documents/limit-phone/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "dumbify"
include(":app")
```

- [ ] **Step 2: Write `build.gradle.kts` (root)**

Create `/Users/batman/Documents/limit-phone/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 3: Write `gradle.properties`**

Create `/Users/batman/Documents/limit-phone/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

- [ ] **Step 4: Write `gradle/libs.versions.toml` (version catalog)**

Create `/Users/batman/Documents/limit-phone/gradle/libs.versions.toml`:

```toml
[versions]
agp = "8.5.2"
kotlin = "1.9.24"
ksp = "1.9.24-1.0.20"
coreKtx = "1.13.1"
lifecycle = "2.8.4"
activityCompose = "1.9.1"
composeBom = "2024.08.00"
hilt = "2.51.1"
hiltNavigationCompose = "1.2.0"
room = "2.6.1"
workManager = "2.9.0"
securityCrypto = "1.1.0-alpha06"
argon2Jvm = "2.11"
coroutines = "1.8.1"
junit5 = "5.10.2"
mockk = "1.13.12"
turbine = "1.1.0"
truth = "1.4.4"
androidJunit5Plugin = "1.10.2.0"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }

androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "workManager" }
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "securityCrypto" }
argon2-jvm = { module = "de.mkammerer:argon2-jvm", version.ref = "argon2Jvm" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

junit-jupiter-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit-jupiter-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit-jupiter-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-agent = { module = "io.mockk:mockk-agent-jvm", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
android-junit5 = { id = "de.mannodermaus.android-junit5", version.ref = "androidJunit5Plugin" }
```

- [ ] **Step 5: Generate Gradle wrapper**

Run:
```bash
cd /Users/batman/Documents/limit-phone
gradle wrapper --gradle-version 8.9 --distribution-type bin
```
Expected: creates `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`.

If `gradle` is not on PATH: install via `brew install gradle` first.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ gradlew gradlew.bat
git commit -m "build: add Gradle scaffolding and version catalog"
```

---

## Task 3: `:app` module Gradle config

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/schemas/.gitkeep`

- [ ] **Step 1: Write `app/build.gradle.kts`**

Create `/Users/batman/Documents/limit-phone/app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.dumbify.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dumbify.app"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // TODO: replace with real keystore in Plan 6
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.argon2.jvm)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.room.testing)
}
```

- [ ] **Step 2: Write `app/proguard-rules.pro`**

Create `/Users/batman/Documents/limit-phone/app/proguard-rules.pro`:

```proguard
# Keep Room entities and DAOs
-keep class com.dumbify.app.data.entities.** { *; }
-keep class com.dumbify.app.data.dao.** { *; }

# Keep DeviceAdminReceiver public API
-keep class com.dumbify.app.admin.DumbifyDeviceAdminReceiver { *; }

# Argon2-jvm uses JNI
-keep class de.mkammerer.argon2.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
```

- [ ] **Step 3: Create schemas dir placeholder**

```bash
mkdir -p /Users/batman/Documents/limit-phone/app/schemas
touch /Users/batman/Documents/limit-phone/app/schemas/.gitkeep
```

- [ ] **Step 4: Verify Gradle sync**

```bash
cd /Users/batman/Documents/limit-phone
./gradlew :app:tasks --no-daemon
```
Expected: lists `assembleDebug`, `testDebugUnitTest`, etc. No "Could not resolve" errors.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro app/schemas/.gitkeep
git commit -m "build: configure :app module with Compose, Hilt, Room, Argon2 deps"
```

---

## Task 4: Main manifest + strings + theme + placeholder MainActivity

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/xml/device_admin.xml`
- Create: `app/src/main/java/com/dumbify/app/DumbifyApp.kt`
- Create: `app/src/main/java/com/dumbify/app/MainActivity.kt`

- [ ] **Step 1: Write `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Dumbify</string>
    <string name="device_admin_description">Dumbify protects your focus by restricting which apps can run on this device. It must be set as Device Owner to function.</string>
    <string name="notif_foreground_service">Dumbify is protecting your focus</string>
</resources>
```

- [ ] **Step 2: Write `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Theme.Dumbify" parent="android:Theme.Material.Light.NoActionBar"/>
</resources>
```

(Compose handles its own theming; this is just a manifest placeholder to satisfy `android:theme` attribute.)

- [ ] **Step 3: Write `app/src/main/res/xml/device_admin.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<device-admin xmlns:android="http://schemas.android.com/apk/res/android"
    android:visible="false">
    <uses-policies>
        <limit-password />
        <watch-login />
        <reset-password />
        <force-lock />
        <wipe-data />
        <expire-password />
        <encrypted-storage />
        <disable-camera />
        <disable-keyguard-features />
    </uses-policies>
</device-admin>
```

- [ ] **Step 4: Write `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
    <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions"/>

    <application
        android:name=".DumbifyApp"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="false"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Dumbify"
        tools:ignore="MissingApplicationIcon"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <receiver
            android:name=".admin.DumbifyDeviceAdminReceiver"
            android:exported="true"
            android:permission="android.permission.BIND_DEVICE_ADMIN">
            <meta-data
                android:name="android.app.device_admin"
                android:resource="@xml/device_admin"/>
            <intent-filter>
                <action android:name="android.app.action.DEVICE_ADMIN_ENABLED"/>
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

Also create `app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="."/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="."/>
    </device-transfer>
</data-extraction-rules>
```

- [ ] **Step 5: Write `DumbifyApp.kt`**

```kotlin
package com.dumbify.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DumbifyApp : Application()
```

- [ ] **Step 6: Write placeholder `MainActivity.kt`**

```kotlin
package com.dumbify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Placeholder()
                }
            }
        }
    }
}

@Composable
private fun Placeholder() {
    Text(
        text = "Dumbify — pre-alpha",
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
        style = MaterialTheme.typography.headlineSmall
    )
}
```

- [ ] **Step 7: Build the APK**

```bash
cd /Users/batman/Documents/limit-phone
./gradlew :app:assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/
git commit -m "feat: scaffold manifest, DumbifyApp, placeholder MainActivity, device_admin xml"
```

---

## Task 5: `DumbifyDeviceAdminReceiver` + verify DO setup on emulator

**Files:**
- Create: `app/src/main/java/com/dumbify/app/admin/DumbifyDeviceAdminReceiver.kt`

- [ ] **Step 1: Write `DumbifyDeviceAdminReceiver.kt`**

```kotlin
package com.dumbify.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DumbifyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i(TAG, "Device admin disabled")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i(TAG, "Provisioning complete")
    }

    companion object {
        const val TAG = "DumbifyAdmin"
    }
}
```

- [ ] **Step 2: Reinstall APK and set DO on a clean emulator**

Start a fresh AVD (no accounts):
```bash
$ANDROID_HOME/emulator/emulator -avd Pixel_API_34 -no-snapshot-load &
adb wait-for-device
adb shell settings put global package_verifier_user_consent -1
./gradlew :app:installDebug --no-daemon
adb shell dpm set-device-owner com.dumbify.app.debug/com.dumbify.app.admin.DumbifyDeviceAdminReceiver
```
Expected: `Success: Device owner set to package ComponentInfo{...DumbifyDeviceAdminReceiver}`.

If you see `java.lang.IllegalStateException: Trying to set the device owner, but device owner is already set`: emulator already has a DO; either wipe data on the AVD or pick a fresh one.

If you see `Not allowed to set the device owner because there are already several users on the device`: factory-reset the AVD before retrying (the emulator must be in pristine state).

- [ ] **Step 3: Confirm in logcat**

```bash
adb logcat -d -s DumbifyAdmin:I | tail -5
```
Expected: `Device admin enabled` line.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dumbify/app/admin/DumbifyDeviceAdminReceiver.kt
git commit -m "feat(admin): add DumbifyDeviceAdminReceiver with onEnabled logging"
```

---

## Task 6: `PolicyEnforcer` with build-variant-gated restrictions

**Files:**
- Create: `app/src/main/java/com/dumbify/app/admin/PolicyEnforcer.kt`
- Create: `app/src/main/java/com/dumbify/app/di/PolicyModule.kt`

- [ ] **Step 1: Write `PolicyEnforcer.kt`**

```kotlin
package com.dumbify.app.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager
import com.dumbify.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolicyEnforcer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin: ComponentName =
        ComponentName(context, DumbifyDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    fun applyRestrictions() {
        check(isDeviceOwner()) { "Cannot apply restrictions: not Device Owner" }
        RELEASE_ONLY_RESTRICTIONS.forEach { restriction ->
            if (!BuildConfig.DEBUG) {
                dpm.addUserRestriction(admin, restriction)
            }
        }
        ALWAYS_APPLIED_RESTRICTIONS.forEach { restriction ->
            dpm.addUserRestriction(admin, restriction)
        }
        if (!BuildConfig.DEBUG) {
            dpm.setUninstallBlocked(admin, context.packageName, true)
        }
    }

    fun clearAllRestrictionsForRemoval() {
        check(isDeviceOwner()) { "Cannot clear restrictions: not Device Owner" }
        (RELEASE_ONLY_RESTRICTIONS + ALWAYS_APPLIED_RESTRICTIONS).forEach { restriction ->
            runCatching { dpm.clearUserRestriction(admin, restriction) }
        }
        runCatching { dpm.setUninstallBlocked(admin, context.packageName, false) }
    }

    fun clearDeviceOwner() {
        runCatching { dpm.clearDeviceOwnerApp(context.packageName) }
    }

    fun setPackagesSuspended(packages: List<String>, suspended: Boolean): List<String> {
        check(isDeviceOwner()) { "Cannot suspend packages: not Device Owner" }
        return dpm.setPackagesSuspended(admin, packages.toTypedArray(), suspended).toList()
    }

    companion object {
        private val RELEASE_ONLY_RESTRICTIONS = listOf(
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        )
        private val ALWAYS_APPLIED_RESTRICTIONS = listOf(
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_APPS_CONTROL,
        )
    }
}
```

- [ ] **Step 2: Write `app/src/main/java/com/dumbify/app/di/PolicyModule.kt`**

```kotlin
package com.dumbify.app.di

import com.dumbify.app.admin.PolicyEnforcer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PolicyModule {
    // PolicyEnforcer is @Inject constructor — no provides needed.
    // Module exists for future bindings.
}
```

(Module is intentionally minimal — Hilt will auto-construct `PolicyEnforcer` via its `@Inject` constructor. We add the file now so subsequent tasks have a place to add bindings.)

- [ ] **Step 3: Build to confirm compiles**

```bash
./gradlew :app:assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dumbify/app/admin/PolicyEnforcer.kt app/src/main/java/com/dumbify/app/di/PolicyModule.kt
git commit -m "feat(admin): add PolicyEnforcer with build-variant-gated restrictions"
```

---

## Task 7: `DevNukeReceiver` (debug variant only)

**Files:**
- Create: `app/src/debug/AndroidManifest.xml`
- Create: `app/src/debug/java/com/dumbify/app/debug/DevNukeReceiver.kt`

- [ ] **Step 1: Write `app/src/debug/java/com/dumbify/app/debug/DevNukeReceiver.kt`**

```kotlin
package com.dumbify.app.debug

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.dumbify.app.admin.DumbifyDeviceAdminReceiver

class DevNukeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.w(TAG, "DEV_NUKE invoked — clearing all DO restrictions and stepping down")
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, DumbifyDeviceAdminReceiver::class.java)

        val allRestrictions = listOf(
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_APPS_CONTROL,
        )
        allRestrictions.forEach {
            runCatching { dpm.clearUserRestriction(admin, it) }
                .onFailure { e -> Log.w(TAG, "clearUserRestriction($it) failed", e) }
        }
        runCatching { dpm.setUninstallBlocked(admin, context.packageName, false) }
            .onFailure { e -> Log.w(TAG, "setUninstallBlocked(false) failed", e) }
        runCatching { dpm.clearDeviceOwnerApp(context.packageName) }
            .onFailure { e -> Log.w(TAG, "clearDeviceOwnerApp failed", e) }

        Log.w(TAG, "DEV_NUKE complete — app should now be uninstallable normally")
    }

    companion object {
        const val TAG = "DumbifyDevNuke"
        const val ACTION = "com.dumbify.app.debug.DEV_NUKE"
    }
}
```

- [ ] **Step 2: Write `app/src/debug/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <receiver
            android:name="com.dumbify.app.debug.DevNukeReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="com.dumbify.app.debug.DEV_NUKE"/>
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 3: Smoke-test the nuke on emulator**

```bash
./gradlew :app:installDebug --no-daemon
adb shell dpm set-device-owner com.dumbify.app.debug/com.dumbify.app.admin.DumbifyDeviceAdminReceiver
adb shell dumpsys device_policy | grep -A1 "Device Owner"
```
Expected: shows `com.dumbify.app.debug` as device owner.

Now nuke:
```bash
adb shell am broadcast -a com.dumbify.app.debug.DEV_NUKE -n com.dumbify.app.debug/com.dumbify.app.debug.DevNukeReceiver
adb logcat -d -s DumbifyDevNuke:W | tail -5
adb shell dumpsys device_policy | grep -A1 "Device Owner"
```
Expected: log line `DEV_NUKE complete`; dumpsys shows no device owner.

Now uninstall normally:
```bash
adb uninstall com.dumbify.app.debug
```
Expected: `Success`.

- [ ] **Step 4: Verify release build excludes DevNukeReceiver**

Build the release variant (using debug signing for now):
```bash
./gradlew :app:assembleRelease --no-daemon
```
Inspect manifest:
```bash
$ANDROID_HOME/build-tools/34.0.0/aapt dump xmltree app/build/outputs/apk/release/app-release-unsigned.apk AndroidManifest.xml | grep -c "DevNukeReceiver"
```
Expected: `0`.

(If `aapt` not on PATH, use the full path under `$ANDROID_HOME/build-tools/<version>/aapt`.)

- [ ] **Step 5: Commit**

```bash
git add app/src/debug/
git commit -m "feat(debug): add DevNukeReceiver as debug-variant escape hatch"
```

---

## Task 8: `Clock` abstraction + `FakeClock` for tests

**Files:**
- Create: `app/src/main/java/com/dumbify/app/util/Clock.kt`
- Create: `app/src/test/java/com/dumbify/app/policy/FakeClock.kt`

- [ ] **Step 1: Write `Clock.kt`**

```kotlin
package com.dumbify.app.util

import javax.inject.Inject
import javax.inject.Singleton

interface Clock {
    fun nowMillis(): Long
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
```

- [ ] **Step 2: Bind `SystemClock` in `PolicyModule`**

Replace `app/src/main/java/com/dumbify/app/di/PolicyModule.kt` with:

```kotlin
package com.dumbify.app.di

import com.dumbify.app.util.Clock
import com.dumbify.app.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PolicyModule {
    @Binds @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
```

- [ ] **Step 3: Write `FakeClock.kt` (test helper)**

```kotlin
package com.dumbify.app.policy

import com.dumbify.app.util.Clock

class FakeClock(var current: Long = 0L) : Clock {
    override fun nowMillis(): Long = current
    fun advance(millis: Long) { current += millis }
}
```

- [ ] **Step 4: Build to confirm Hilt graph compiles**

```bash
./gradlew :app:assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dumbify/app/util/Clock.kt app/src/main/java/com/dumbify/app/di/PolicyModule.kt app/src/test/java/com/dumbify/app/policy/FakeClock.kt
git commit -m "feat(util): add Clock abstraction with SystemClock binding and FakeClock test helper"
```

---

## Task 9: Room entities — `Config`, `AppRule`, `Event`

**Files:**
- Create: `app/src/main/java/com/dumbify/app/data/entities/Config.kt`
- Create: `app/src/main/java/com/dumbify/app/data/entities/AppRule.kt`
- Create: `app/src/main/java/com/dumbify/app/data/entities/Event.kt`

- [ ] **Step 1: Write `Config.kt`**

```kotlin
package com.dumbify.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlockMode { ALLOWLIST, DENYLIST }
enum class UserRole { SELF, MANAGED }

@Entity(tableName = "config")
data class Config(
    @PrimaryKey val id: Int = 0,
    val mode: BlockMode,
    val userRole: UserRole,
    val customMessage: String,
    val launcherEnabled: Boolean,
    val onboardingComplete: Boolean,
)
```

- [ ] **Step 2: Write `AppRule.kt`**

```kotlin
package com.dumbify.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BypassMode { DELAY, PIN, DELAY_AND_PIN }

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val isAllowed: Boolean,
    val bypassMode: BypassMode,
    val delaySeconds: Int,
    val grantedUntil: Long?,
)
```

- [ ] **Step 3: Write `Event.kt`**

```kotlin
package com.dumbify.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType {
    BLOCK_HIT,
    UNBLOCK_REQUEST,
    UNBLOCK_GRANTED,
    UNBLOCK_DENIED,
    RULE_EDIT,
    PIN_FAIL,
    MODE_CHANGE,
    REMOVAL_ATTEMPT,
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: EventType,
    val packageName: String?,
    val detail: String?,
)
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dumbify/app/data/entities/
git commit -m "feat(data): add Config, AppRule, Event entities"
```

---

## Task 10: DAOs

**Files:**
- Create: `app/src/main/java/com/dumbify/app/data/dao/ConfigDao.kt`
- Create: `app/src/main/java/com/dumbify/app/data/dao/AppRuleDao.kt`
- Create: `app/src/main/java/com/dumbify/app/data/dao/EventDao.kt`

- [ ] **Step 1: Write `ConfigDao.kt`**

```kotlin
package com.dumbify.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dumbify.app.data.entities.Config
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config WHERE id = 0 LIMIT 1")
    suspend fun get(): Config?

    @Query("SELECT * FROM config WHERE id = 0 LIMIT 1")
    fun observe(): Flow<Config?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: Config)
}
```

- [ ] **Step 2: Write `AppRuleDao.kt`**

```kotlin
package com.dumbify.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dumbify.app.data.entities.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules")
    fun observeAll(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules")
    suspend fun all(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE packageName = :pkg LIMIT 1")
    suspend fun byPkg(pkg: String): AppRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRule)

    @Query("DELETE FROM app_rules WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("UPDATE app_rules SET grantedUntil = :until WHERE packageName = :pkg")
    suspend fun setGrantedUntil(pkg: String, until: Long?)
}
```

- [ ] **Step 3: Write `EventDao.kt`**

```kotlin
package com.dumbify.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dumbify.app.data.entities.Event

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: Event): Long

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<Event>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun all(): List<Event>
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dumbify/app/data/dao/
git commit -m "feat(data): add ConfigDao, AppRuleDao, EventDao"
```

---

## Task 11: `DumbifyDb` + Hilt DataModule

**Files:**
- Create: `app/src/main/java/com/dumbify/app/data/DumbifyDb.kt`
- Create: `app/src/main/java/com/dumbify/app/di/DataModule.kt`

- [ ] **Step 1: Write `DumbifyDb.kt`**

```kotlin
package com.dumbify.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.dumbify.app.data.entities.UserRole

class EnumConverters {
    @TypeConverter fun blockModeToString(v: BlockMode): String = v.name
    @TypeConverter fun stringToBlockMode(v: String): BlockMode = BlockMode.valueOf(v)

    @TypeConverter fun userRoleToString(v: UserRole): String = v.name
    @TypeConverter fun stringToUserRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun bypassModeToString(v: BypassMode): String = v.name
    @TypeConverter fun stringToBypassMode(v: String): BypassMode = BypassMode.valueOf(v)

    @TypeConverter fun eventTypeToString(v: EventType): String = v.name
    @TypeConverter fun stringToEventType(v: String): EventType = EventType.valueOf(v)
}

@Database(
    entities = [Config::class, AppRule::class, Event::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class DumbifyDb : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun eventDao(): EventDao
}
```

- [ ] **Step 2: Write `DataModule.kt`**

```kotlin
package com.dumbify.app.di

import android.content.Context
import androidx.room.Room
import com.dumbify.app.data.DumbifyDb
import com.dumbify.app.data.SecurePrefs
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): DumbifyDb =
        Room.databaseBuilder(ctx, DumbifyDb::class.java, "dumbify.db")
            .fallbackToDestructiveMigration(dropAllTables = true) // pre-1.0; replace before release
            .build()

    @Provides fun provideConfigDao(db: DumbifyDb): ConfigDao = db.configDao()
    @Provides fun provideAppRuleDao(db: DumbifyDb): AppRuleDao = db.appRuleDao()
    @Provides fun provideEventDao(db: DumbifyDb): EventDao = db.eventDao()
}
```

(`SecurePrefs` is referenced but defined in Task 12 — we'll wire its `@Provides` then.)

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`. KSP generates `DumbifyDb_Impl`.

Also confirm schema export:
```bash
ls app/schemas/com.dumbify.app.data.DumbifyDb/
```
Expected: `1.json` present.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dumbify/app/data/DumbifyDb.kt app/src/main/java/com/dumbify/app/di/DataModule.kt app/schemas/
git commit -m "feat(data): add Room database with TypeConverters and Hilt module"
```

---

## Task 12: `SecurePrefs` (EncryptedSharedPreferences wrapper) + `FakeSecurePrefs`

**Files:**
- Create: `app/src/main/java/com/dumbify/app/data/SecurePrefs.kt`
- Create: `app/src/test/java/com/dumbify/app/data/FakeSecurePrefs.kt`
- Modify: `app/src/main/java/com/dumbify/app/di/DataModule.kt`

- [ ] **Step 1: Write `SecurePrefs.kt`**

```kotlin
package com.dumbify.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface SecurePrefs {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getBytes(key: String): ByteArray?
    fun putBytes(key: String, value: ByteArray?)
    fun getLong(key: String, default: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun getInt(key: String, default: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun contains(key: String): Boolean
    fun remove(key: String)
}

@Singleton
class EncryptedSecurePrefs @Inject constructor(
    @ApplicationContext context: Context,
) : SecurePrefs {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "dumbify_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun getBytes(key: String): ByteArray? =
        prefs.getString(key, null)?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }

    override fun putBytes(key: String, value: ByteArray?) {
        prefs.edit().apply {
            if (value == null) remove(key)
            else putString(key, android.util.Base64.encodeToString(value, android.util.Base64.NO_WRAP))
        }.apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)
    override fun putLong(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun contains(key: String): Boolean = prefs.contains(key)
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

object SecurePrefsKeys {
    const val REMOVAL_PIN_HASH = "removal_pin_hash"
    const val REMOVAL_PIN_SALT = "removal_pin_salt"
    const val BYPASS_PIN_HASH = "bypass_pin_hash"
    const val BYPASS_PIN_SALT = "bypass_pin_salt"
    const val PIN_FAIL_COUNT = "pin_fail_count"
    const val PIN_COOLDOWN_UNTIL = "pin_cooldown_until"
}
```

- [ ] **Step 2: Bind `EncryptedSecurePrefs` in `DataModule`**

Replace `app/src/main/java/com/dumbify/app/di/DataModule.kt` with:

```kotlin
package com.dumbify.app.di

import android.content.Context
import androidx.room.Room
import com.dumbify.app.data.DumbifyDb
import com.dumbify.app.data.EncryptedSecurePrefs
import com.dumbify.app.data.SecurePrefs
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds @Singleton
    abstract fun bindSecurePrefs(impl: EncryptedSecurePrefs): SecurePrefs

    companion object {
        @Provides @Singleton
        fun provideDb(@ApplicationContext ctx: Context): DumbifyDb =
            Room.databaseBuilder(ctx, DumbifyDb::class.java, "dumbify.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()

        @Provides fun provideConfigDao(db: DumbifyDb): ConfigDao = db.configDao()
        @Provides fun provideAppRuleDao(db: DumbifyDb): AppRuleDao = db.appRuleDao()
        @Provides fun provideEventDao(db: DumbifyDb): EventDao = db.eventDao()
    }
}
```

- [ ] **Step 3: Write `FakeSecurePrefs.kt` (test helper)**

```kotlin
package com.dumbify.app.data

class FakeSecurePrefs : SecurePrefs {
    private val store = mutableMapOf<String, Any?>()

    override fun getString(key: String): String? = store[key] as? String
    override fun putString(key: String, value: String?) {
        if (value == null) store.remove(key) else store[key] = value
    }
    override fun getBytes(key: String): ByteArray? = store[key] as? ByteArray
    override fun putBytes(key: String, value: ByteArray?) {
        if (value == null) store.remove(key) else store[key] = value
    }
    override fun getLong(key: String, default: Long): Long = (store[key] as? Long) ?: default
    override fun putLong(key: String, value: Long) { store[key] = value }
    override fun getInt(key: String, default: Int): Int = (store[key] as? Int) ?: default
    override fun putInt(key: String, value: Int) { store[key] = value }
    override fun contains(key: String): Boolean = store.containsKey(key)
    override fun remove(key: String) { store.remove(key) }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/dumbify/app/data/SecurePrefs.kt app/src/main/java/com/dumbify/app/di/DataModule.kt app/src/test/java/com/dumbify/app/data/FakeSecurePrefs.kt
git commit -m "feat(data): add SecurePrefs interface, EncryptedSharedPreferences impl, FakeSecurePrefs"
```

---

## Task 13: `PinManager` — TDD

**Files:**
- Create: `app/src/test/java/com/dumbify/app/policy/PinManagerTest.kt`
- Create: `app/src/main/java/com/dumbify/app/policy/PinManager.kt`

PinManager responsibilities:
1. Hash a PIN with Argon2id + random salt; store hash + salt in SecurePrefs under `removal_*` or `bypass_*` keys.
2. Verify a PIN attempt against stored hash.
3. Track consecutive failures; on 3rd fail, set cooldown for 5 minutes. Refuse `verify` while in cooldown.
4. On success, reset fail count.

Two scopes: `Scope.REMOVAL` and `Scope.BYPASS`. The fail counter is shared across scopes (a wrong removal PIN counts toward cooldown for any subsequent PIN entry — defense-in-depth).

- [ ] **Step 1: Write `PinManagerTest.kt` (failing tests)**

```kotlin
package com.dumbify.app.policy

import com.dumbify.app.data.FakeSecurePrefs
import com.dumbify.app.policy.PinManager.Scope
import com.dumbify.app.policy.PinManager.VerifyResult
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PinManagerTest {

    private lateinit var prefs: FakeSecurePrefs
    private lateinit var clock: FakeClock
    private lateinit var pinManager: PinManager

    @BeforeEach
    fun setup() {
        prefs = FakeSecurePrefs()
        clock = FakeClock(1_000_000L)
        pinManager = PinManager(prefs, clock)
    }

    @Test
    fun `setPin stores hash and salt for removal scope`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        assertThat(prefs.getString("removal_pin_hash")).isNotNull()
        assertThat(prefs.getBytes("removal_pin_salt")).isNotNull()
    }

    @Test
    fun `setPin stores hash and salt for bypass scope`() {
        pinManager.setPin(Scope.BYPASS, "5678")
        assertThat(prefs.getString("bypass_pin_hash")).isNotNull()
        assertThat(prefs.getBytes("bypass_pin_salt")).isNotNull()
    }

    @Test
    fun `hasPin returns false when unset`() {
        assertThat(pinManager.hasPin(Scope.REMOVAL)).isFalse()
        assertThat(pinManager.hasPin(Scope.BYPASS)).isFalse()
    }

    @Test
    fun `hasPin returns true after setPin`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        assertThat(pinManager.hasPin(Scope.REMOVAL)).isTrue()
    }

    @Test
    fun `verify returns SUCCESS for correct PIN`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        val result = pinManager.verify(Scope.REMOVAL, "1234")
        assertThat(result).isEqualTo(VerifyResult.SUCCESS)
    }

    @Test
    fun `verify returns WRONG for incorrect PIN`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        val result = pinManager.verify(Scope.REMOVAL, "9999")
        assertThat(result).isEqualTo(VerifyResult.WRONG)
    }

    @Test
    fun `verify returns NOT_SET when no PIN configured`() {
        val result = pinManager.verify(Scope.REMOVAL, "1234")
        assertThat(result).isEqualTo(VerifyResult.NOT_SET)
    }

    @Test
    fun `three consecutive wrong attempts trigger cooldown`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.REMOVAL, "0000")
        val third = pinManager.verify(Scope.REMOVAL, "0000")
        assertThat(third).isEqualTo(VerifyResult.WRONG)
        val fourth = pinManager.verify(Scope.REMOVAL, "1234") // even correct PIN refused while cooling down
        assertThat(fourth).isEqualTo(VerifyResult.COOLDOWN)
    }

    @Test
    fun `cooldown expires after 5 minutes`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        repeat(3) { pinManager.verify(Scope.REMOVAL, "0000") }
        clock.advance(5 * 60 * 1000L + 1)
        val result = pinManager.verify(Scope.REMOVAL, "1234")
        assertThat(result).isEqualTo(VerifyResult.SUCCESS)
    }

    @Test
    fun `successful verify resets fail counter`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.REMOVAL, "1234") // success
        // Now two more wrong attempts should NOT trigger cooldown
        pinManager.verify(Scope.REMOVAL, "0000")
        val result = pinManager.verify(Scope.REMOVAL, "0000")
        assertThat(result).isEqualTo(VerifyResult.WRONG)
    }

    @Test
    fun `fail counter is shared across scopes`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.setPin(Scope.BYPASS, "5678")
        pinManager.verify(Scope.REMOVAL, "0000")
        pinManager.verify(Scope.BYPASS, "0000")
        pinManager.verify(Scope.REMOVAL, "0000")
        // 3 total fails — cooldown active for either scope
        assertThat(pinManager.verify(Scope.BYPASS, "5678")).isEqualTo(VerifyResult.COOLDOWN)
    }

    @Test
    fun `clearPin removes hash and salt`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        pinManager.clearPin(Scope.REMOVAL)
        assertThat(pinManager.hasPin(Scope.REMOVAL)).isFalse()
        assertThat(prefs.getString("removal_pin_hash")).isNull()
        assertThat(prefs.getBytes("removal_pin_salt")).isNull()
    }

    @Test
    fun `different PINs produce different hashes`() {
        pinManager.setPin(Scope.REMOVAL, "1234")
        val hash1 = prefs.getString("removal_pin_hash")!!
        pinManager.clearPin(Scope.REMOVAL)
        pinManager.setPin(Scope.REMOVAL, "1234")
        val hash2 = prefs.getString("removal_pin_hash")!!
        // Different salts → different hashes even for same PIN
        assertThat(hash1).isNotEqualTo(hash2)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dumbify.app.policy.PinManagerTest" --no-daemon
```
Expected: FAILED with `Unresolved reference: PinManager` (file does not exist yet).

- [ ] **Step 3: Implement `PinManager.kt`**

```kotlin
package com.dumbify.app.policy

import com.dumbify.app.data.SecurePrefs
import com.dumbify.app.data.SecurePrefsKeys
import com.dumbify.app.util.Clock
import de.mkammerer.argon2.Argon2Factory
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    private val prefs: SecurePrefs,
    private val clock: Clock,
) {
    enum class Scope { REMOVAL, BYPASS }
    enum class VerifyResult { SUCCESS, WRONG, NOT_SET, COOLDOWN }

    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    fun setPin(scope: Scope, pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val saltedPin = saltedInput(pin, salt)
        val hash = argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY_KB, ARGON2_PARALLELISM, saltedPin.toCharArray())
        prefs.putString(hashKey(scope), hash)
        prefs.putBytes(saltKey(scope), salt)
    }

    fun hasPin(scope: Scope): Boolean = prefs.contains(hashKey(scope))

    fun clearPin(scope: Scope) {
        prefs.remove(hashKey(scope))
        prefs.remove(saltKey(scope))
    }

    fun verify(scope: Scope, pin: String): VerifyResult {
        val cooldownUntil = prefs.getLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL)
        if (clock.nowMillis() < cooldownUntil) return VerifyResult.COOLDOWN

        val hash = prefs.getString(hashKey(scope)) ?: return VerifyResult.NOT_SET
        val salt = prefs.getBytes(saltKey(scope)) ?: return VerifyResult.NOT_SET

        val saltedPin = saltedInput(pin, salt)
        val matches = argon2.verify(hash, saltedPin.toCharArray())

        return if (matches) {
            prefs.putInt(SecurePrefsKeys.PIN_FAIL_COUNT, 0)
            prefs.putLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL, 0)
            VerifyResult.SUCCESS
        } else {
            val fails = prefs.getInt(SecurePrefsKeys.PIN_FAIL_COUNT) + 1
            prefs.putInt(SecurePrefsKeys.PIN_FAIL_COUNT, fails)
            if (fails >= MAX_ATTEMPTS) {
                prefs.putLong(SecurePrefsKeys.PIN_COOLDOWN_UNTIL, clock.nowMillis() + COOLDOWN_MS)
                prefs.putInt(SecurePrefsKeys.PIN_FAIL_COUNT, 0)
            }
            VerifyResult.WRONG
        }
    }

    private fun saltedInput(pin: String, salt: ByteArray): String {
        // Argon2-jvm's verify(hash, chars) handles its own embedded salt;
        // we additionally pepper with our SecurePrefs salt by prefixing,
        // so changing the salt invalidates the hash even for the same PIN.
        return android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP) + ":" + pin
    }

    private fun hashKey(scope: Scope): String = when (scope) {
        Scope.REMOVAL -> SecurePrefsKeys.REMOVAL_PIN_HASH
        Scope.BYPASS -> SecurePrefsKeys.BYPASS_PIN_HASH
    }

    private fun saltKey(scope: Scope): String = when (scope) {
        Scope.REMOVAL -> SecurePrefsKeys.REMOVAL_PIN_SALT
        Scope.BYPASS -> SecurePrefsKeys.BYPASS_PIN_SALT
    }

    companion object {
        const val MAX_ATTEMPTS = 3
        const val COOLDOWN_MS = 5L * 60L * 1000L
        const val SALT_BYTES = 16
        // Argon2id parameters — tuned conservatively for low-end devices.
        const val ARGON2_ITERATIONS = 3
        const val ARGON2_MEMORY_KB = 65_536 // 64 MB
        const val ARGON2_PARALLELISM = 2
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dumbify.app.policy.PinManagerTest" --no-daemon
```
Expected: all 13 tests PASS.

If `argon2-jvm` native library fails to load on macOS JDK 17, run with explicit native lib loading (the library auto-loads via JNA from its JAR — should work out of the box). If it doesn't, add `testImplementation("net.java.dev.jna:jna:5.14.0")` to `app/build.gradle.kts` and re-run.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/dumbify/app/policy/PinManagerTest.kt app/src/main/java/com/dumbify/app/policy/PinManager.kt
git commit -m "feat(policy): add PinManager with Argon2id hashing and brute-force cooldown"
```

---

## Task 14: `RuleStore` — TDD

**Files:**
- Create: `app/src/test/java/com/dumbify/app/policy/RuleStoreTest.kt`
- Create: `app/src/main/java/com/dumbify/app/policy/RuleStore.kt`

RuleStore wraps `AppRuleDao` + `ConfigDao` and exposes `isBlocked(pkg)` evaluation per spec section "Dual mode" in `01-architecture.md`. Always-allowed packages are hardcoded.

- [ ] **Step 1: Write `RuleStoreTest.kt`**

```kotlin
package com.dumbify.app.policy

import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.UserRole
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RuleStoreTest {

    private val ownPackage = "com.dumbify.app"

    private fun config(mode: BlockMode) = Config(
        id = 0,
        mode = mode,
        userRole = UserRole.SELF,
        customMessage = "",
        launcherEnabled = false,
        onboardingComplete = true,
    )

    private fun rule(pkg: String, allowed: Boolean) = AppRule(
        packageName = pkg,
        isAllowed = allowed,
        bypassMode = BypassMode.DELAY,
        delaySeconds = 30,
        grantedUntil = null,
    )

    private fun makeStore(config: Config, rules: Map<String, AppRule>): RuleStore {
        val configDao = mockk<com.dumbify.app.data.dao.ConfigDao>()
        coEvery { configDao.get() } returns config
        val appRuleDao = mockk<com.dumbify.app.data.dao.AppRuleDao>()
        coEvery { appRuleDao.byPkg(any()) } answers { rules[firstArg<String>()] }
        return RuleStore(configDao, appRuleDao, ownPackage)
    }

    @Test
    fun `allowlist mode blocks unlisted package`() = runTest {
        val store = makeStore(config(BlockMode.ALLOWLIST), emptyMap())
        assertThat(store.isBlocked("com.instagram.android")).isTrue()
    }

    @Test
    fun `allowlist mode allows package marked allowed`() = runTest {
        val rules = mapOf("com.maps" to rule("com.maps", allowed = true))
        val store = makeStore(config(BlockMode.ALLOWLIST), rules)
        assertThat(store.isBlocked("com.maps")).isFalse()
    }

    @Test
    fun `allowlist mode blocks package marked not allowed`() = runTest {
        val rules = mapOf("com.tiktok" to rule("com.tiktok", allowed = false))
        val store = makeStore(config(BlockMode.ALLOWLIST), rules)
        assertThat(store.isBlocked("com.tiktok")).isTrue()
    }

    @Test
    fun `denylist mode allows unlisted package`() = runTest {
        val store = makeStore(config(BlockMode.DENYLIST), emptyMap())
        assertThat(store.isBlocked("com.random")).isFalse()
    }

    @Test
    fun `denylist mode blocks package marked not allowed`() = runTest {
        val rules = mapOf("com.tiktok" to rule("com.tiktok", allowed = false))
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.tiktok")).isTrue()
    }

    @Test
    fun `denylist mode allows package marked allowed`() = runTest {
        val rules = mapOf("com.maps" to rule("com.maps", allowed = true))
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.maps")).isFalse()
    }

    @Test
    fun `dumbify itself is never blocked in allowlist mode`() = runTest {
        val store = makeStore(config(BlockMode.ALLOWLIST), emptyMap())
        assertThat(store.isBlocked(ownPackage)).isFalse()
    }

    @Test
    fun `dumbify itself is never blocked in denylist mode even if rule says blocked`() = runTest {
        val rules = mapOf(ownPackage to rule(ownPackage, allowed = false))
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked(ownPackage)).isFalse()
    }

    @Test
    fun `system essentials are never blocked`() = runTest {
        val store = makeStore(config(BlockMode.ALLOWLIST), emptyMap())
        assertThat(store.isBlocked("com.android.dialer")).isFalse()
        assertThat(store.isBlocked("com.android.mms")).isFalse()
        assertThat(store.isBlocked("com.android.settings")).isFalse()
        assertThat(store.isBlocked("com.android.camera2")).isFalse()
    }

    @Test
    fun `package with active grant is not blocked`() = runTest {
        // grantedUntil in the future — treated as currently allowed regardless of mode
        val futureRule = rule("com.tiktok", allowed = false).copy(grantedUntil = Long.MAX_VALUE)
        val rules = mapOf("com.tiktok" to futureRule)
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.tiktok", now = 0L)).isFalse()
    }

    @Test
    fun `package with expired grant is blocked again`() = runTest {
        val expiredRule = rule("com.tiktok", allowed = false).copy(grantedUntil = 100L)
        val rules = mapOf("com.tiktok" to expiredRule)
        val store = makeStore(config(BlockMode.DENYLIST), rules)
        assertThat(store.isBlocked("com.tiktok", now = 200L)).isTrue()
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dumbify.app.policy.RuleStoreTest" --no-daemon
```
Expected: FAILED with `Unresolved reference: RuleStore`.

- [ ] **Step 3: Implement `RuleStore.kt`**

```kotlin
package com.dumbify.app.policy

import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RuleStore @Inject constructor(
    private val configDao: ConfigDao,
    private val appRuleDao: AppRuleDao,
    @Named("ownPackage") private val ownPackage: String,
) {
    suspend fun isBlocked(pkg: String, now: Long = System.currentTimeMillis()): Boolean {
        if (pkg in ALWAYS_ALLOWED || pkg == ownPackage) return false
        val rule = appRuleDao.byPkg(pkg)
        if (rule?.grantedUntil != null && rule.grantedUntil > now) return false
        val config = configDao.get() ?: return false // no config = not onboarded; allow everything
        return evaluate(config, rule)
    }

    private fun evaluate(config: Config, rule: AppRule?): Boolean = when (config.mode) {
        BlockMode.ALLOWLIST -> rule?.isAllowed != true
        BlockMode.DENYLIST -> rule?.isAllowed == false
    }

    suspend fun upsert(rule: AppRule) = appRuleDao.upsert(rule)
    suspend fun delete(pkg: String) = appRuleDao.delete(pkg)
    suspend fun byPkg(pkg: String): AppRule? = appRuleDao.byPkg(pkg)
    suspend fun all(): List<AppRule> = appRuleDao.all()
    suspend fun setGrantedUntil(pkg: String, until: Long?) = appRuleDao.setGrantedUntil(pkg, until)

    suspend fun getConfig(): Config? = configDao.get()
    suspend fun upsertConfig(config: Config) = configDao.upsert(config)

    companion object {
        // Always-allowed packages — prevents user from locking themselves out
        // of essentials. AOSP packages; OEMs may use different names — refine later.
        val ALWAYS_ALLOWED = setOf(
            "com.android.dialer",
            "com.android.contacts",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.camera",
            "com.android.camera2",
            "com.android.settings",
            "com.android.systemui",
            "android",
        )
    }
}
```

- [ ] **Step 4: Bind `ownPackage` in `PolicyModule`**

Replace `app/src/main/java/com/dumbify/app/di/PolicyModule.kt` with:

```kotlin
package com.dumbify.app.di

import android.content.Context
import com.dumbify.app.util.Clock
import com.dumbify.app.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PolicyModule {
    @Binds @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    companion object {
        @Provides @Singleton @Named("ownPackage")
        fun provideOwnPackage(@ApplicationContext context: Context): String = context.packageName
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dumbify.app.policy.RuleStoreTest" --no-daemon
```
Expected: all 11 tests PASS.

- [ ] **Step 6: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest --no-daemon
```
Expected: all tests pass (PinManagerTest + RuleStoreTest, 24 total).

- [ ] **Step 7: Build full debug APK**

```bash
./gradlew :app:assembleDebug --no-daemon
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/test/java/com/dumbify/app/policy/RuleStoreTest.kt app/src/main/java/com/dumbify/app/policy/RuleStore.kt app/src/main/java/com/dumbify/app/di/PolicyModule.kt
git commit -m "feat(policy): add RuleStore with allowlist/denylist evaluation and always-allowed set"
```

---

## Task 15: GitHub Actions CI

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Write `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    name: Build & Unit Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ runner.os }}-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties', '**/libs.versions.toml') }}
          restore-keys: gradle-${{ runner.os }}-

      - name: Grant execute permission to gradlew
        run: chmod +x ./gradlew

      - name: Build debug
        run: ./gradlew :app:assembleDebug --no-daemon --stacktrace

      - name: Run unit tests
        run: ./gradlew :app:testDebugUnitTest --no-daemon --stacktrace

      - name: Lint debug
        run: ./gradlew :app:lintDebug --no-daemon --stacktrace

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: dumbify-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: app/build/reports/tests/
```

- [ ] **Step 2: Commit and push to verify CI runs**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions workflow for build, unit tests, lint"
```

Push to remote (configure remote first if needed):
```bash
# If remote not configured:
# gh repo create dumbify --public --source=. --remote=origin --push
git push -u origin main
```

Then visit `https://github.com/<user>/dumbify/actions` to confirm green.

- [ ] **Step 3: Update TRACKER.md — mark M0, M1, M2 complete**

Edit `/Users/batman/Documents/limit-phone/docs/superpowers/specs/2026-05-18-dumbify-design/TRACKER.md`:

Replace all `⬜` with `✅` for tasks 0.1 through 2.5 in milestones M0, M1, M2. Add to Change log section:

```markdown
| 2026-05-18 | Plan 1 complete: M0, M1, M2 done. Bootstrap + DO + data layer landed. |
```

Commit:
```bash
git add docs/superpowers/specs/2026-05-18-dumbify-design/TRACKER.md
git commit -m "docs: mark M0/M1/M2 complete in tracker"
```

---

## Task 16: Final smoke test — install on emulator, confirm DO + DB + PIN end-to-end

**Files:** none — verification only.

- [ ] **Step 1: Install fresh on emulator**

```bash
adb shell pm uninstall com.dumbify.app.debug 2>/dev/null
./gradlew :app:installDebug --no-daemon
adb shell dpm set-device-owner com.dumbify.app.debug/com.dumbify.app.admin.DumbifyDeviceAdminReceiver
```
Expected: Success.

- [ ] **Step 2: Launch the placeholder UI**

```bash
adb shell am start -n com.dumbify.app.debug/com.dumbify.app.MainActivity
```
Expected: "Dumbify — pre-alpha" screen renders, no crash.

- [ ] **Step 3: Confirm Room DB file is created**

```bash
adb root 2>/dev/null
adb shell ls /data/data/com.dumbify.app.debug/databases/
```
Expected: shows `dumbify.db` after the first DB access. (May be empty until something queries; this is OK for Plan 1.)

- [ ] **Step 4: Confirm DO is active**

```bash
adb shell dumpsys device_policy | grep -A2 "Device Owner"
```
Expected: lists `com.dumbify.app.debug` as device owner.

- [ ] **Step 5: Nuke and verify clean uninstall**

```bash
adb shell am broadcast -a com.dumbify.app.debug.DEV_NUKE -n com.dumbify.app.debug/com.dumbify.app.debug.DevNukeReceiver
sleep 2
adb uninstall com.dumbify.app.debug
```
Expected: `Success`.

- [ ] **Step 6: Tag the milestone in git**

```bash
git tag -a v0.1.0-bootstrap -m "Plan 1 complete: bootstrap + DO + data layer"
git push origin v0.1.0-bootstrap  # if remote configured
```

---

## Done

Plan 1 deliverable:
- ✅ Repo scaffolded with GPLv3, .gitignore, README
- ✅ Gradle + version catalog + wrapper
- ✅ `:app` module builds debug + release APKs
- ✅ `DumbifyDeviceAdminReceiver` can be set as Device Owner
- ✅ `PolicyEnforcer` applies build-variant-gated restrictions
- ✅ `DevNukeReceiver` (debug only) cleanly steps down DO
- ✅ Room DB with `Config`, `AppRule`, `Event` entities
- ✅ `SecurePrefs` with EncryptedSharedPreferences
- ✅ `PinManager` with Argon2id + brute-force cooldown — 13 unit tests pass
- ✅ `RuleStore` with allowlist/denylist eval + always-allowed set — 11 unit tests pass
- ✅ GitHub Actions CI green on push

**Next:** Plan 2 — app blocking + notification suppression (M3 + M4).
