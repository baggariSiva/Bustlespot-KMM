import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.buildConfig)
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("app.cash.sqldelight") version "2.0.2"

}

kotlin {
    androidTarget {
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_11)
//        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            //   export("io.github.mirzemehdi:kmpnotifier:1.4.0")
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-lsqlite3", "_sqlite3")
            project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
                targets
                    .filterIsInstance<KotlinNativeTarget>()
                    .flatMap { it.binaries }
                    .forEach { compilationUnit ->
                        compilationUnit.linkerOpts(
                            "-lsqlite3",
                            "_sqlite3"
                        )
                    }
            }
        }
    }

    jvm("desktop")

    //build config app version
    version = "1.0.2"

    sourceSets {
        val desktopMain by getting
        commonMain.dependencies {
            implementation(libs.slf4j.nop)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kermit)
            implementation(libs.kotlinx.coroutines.core)
//            implementation(libs.ktor.client.core)
//            implementation(libs.ktor.client.content.negotiation)
//            implementation(libs.ktor.client.serialization)
//            implementation(libs.ktor.client.logging)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.composee)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.composeIcons.featherIcons)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.cio)
            implementation(libs.bundles.ktor)

            implementation(libs.lifecycle.viewmodel.compose)
            //           api(libs.kmpnotifier)
            implementation(libs.kermit)
//            implementation(libs.library.base)
            implementation(libs.coroutines.extensions)
            implementation(libs.stately.common) // Needed by SQLDelight
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.test)

        }

        androidMain.dependencies {
            // Kotlin + coroutines
            implementation(libs.androidx.work.runtime.ktx)
            implementation(compose.uiTooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.android.driver)

            // AndroidX
            implementation(libs.androidx.startup.runtime)
            api(libs.kmpnotifier)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.jnativehook)
            implementation(libs.sqlite.driver)

            api(libs.kmpnotifier)
        }

        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.native.driver)
        }

    }
}
sqldelight {
    databases {
        create("Database") {
            packageName.set("com.example")
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")
            verifyMigrations = true
        }
    }
}

buildConfig{
    packageName = "org.softsuave.bustlespot"
    useKotlinOutput {
        topLevelConstants = true
    }
    forClass("BuildConfigKt"){
        buildConfigField("String", "APP_NAME", "\"${project.name}\"")
        buildConfigField("String", "APP_VERSION", "\"${project.version}\"")
    }
}
android {
    namespace = "org.softsuave.bustlespot"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35

        applicationId = "org.softsuave.bustlespot.androidApp"
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {

    //    implementation(libs.androidx.startup.runtime)
//    implementation(libs.androidx.room.common)
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.softsuave.bustlespot.MainKt"
        buildTypes.release.proguard {
            optimize.set(false)
            isEnabled = true
            obfuscate.set(false)
//            version.set("7.5.0")
            configurationFiles.from(project.file("compose-desktop.pro"))
        }
        jvmArgs(
            "-Djava.util.logging.config.file=logging.properties" // Custom logging config
        )

        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            includeAllModules = true
            packageName = "Bustlespot"
            packageVersion = "1.0.0"
            vendor = "Soft Suave Technologies"
            modules("java.sql")
            modules("jdk.accessibility")

            windows {
//                targetFormats(TargetFormat.Exe) Exe if needed
                menu = true
                menuGroup = "KMMDev"
                upgradeUuid = "123e4567-e89b-12d3-a456-426614174010" // Unique ID for the installer
                iconFile.set(project.file("src/commonMain/composeResources/files/app_icon_windows.ico"))
                perUserInstall = false
                shortcut = true
            }

            // macOS configuration
            macOS {
                bundleID = "org.softsuave.bustlespot.app"
                iconFile.set(project.file("src/commonMain/composeResources/files/app_icon_macos.icns"))
                infoPlist(
                    fn = {
                        extraKeysRawXml = """
            <key>NSCameraUsageDescription</key>
            <string>This app requires access to the camera for capturing media.</string>
            
            <key>NSMicrophoneUsageDescription</key>
            <string>This app requires access to the microphone for recording audio.</string>
            
            <key>NSPhotoLibraryUsageDescription</key>
            <string>This app requires access to the photo library to save and select images.</string>
            
            <key>NSPhotoLibraryAddUsageDescription</key>
            <string>This app requires permission to add photos to your library.</string>
            
            <key>NSFileAccessUsageDescription</key>
            <string>This app requires access to files for reading and writing.</string>
            
            <key>NSDocumentsFolderUsageDescription</key>
            <true/>
            
            <key>NSDownloadsFolderUsageDescription</key>
            <true/>
            
            <key>NSDesktopFolderUsageDescription</key>
            <true/>
            
            <key>NSAccessibilityAccess</key>
            <true/>
            
            <key>NSScreenCaptureDescription</key>
            <true/>
            
            <key>NSFileProtectionComplete</key>
            <true/>
            
            <key>NSFileProtectionCompleteUnlessOpen</key>
            <true/>
            
            <key>NSFileProtectionCompleteUntilFirstUserAuthentication</key>
            <true/>
        """.trimIndent()
                    }
                )
            }
            // Linux configuration
            linux {
                appCategory = "Productivity"
                debMaintainer = "Softsuave"
                rpmLicenseType = "MIT"
                shortcut = true
            }
        }
    }
}
