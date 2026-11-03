import java.util.Properties
import java.security.KeyStore
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.android.gms.oss-licenses-plugin")
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { load(it) }
    }
}

val verifyReleaseSigning = tasks.register("verifyReleaseSigning") {
    group = "verification"
    description = "Checks the local release keystore and credentials without displaying secrets."
    doLast {
        val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        val missingKeys = requiredKeys.filter { releaseSigningProperties.getProperty(it).isNullOrBlank() }
        if (missingKeys.isNotEmpty()) {
            throw GradleException("Fill in keystore.properties before signing a release. Missing: ${missingKeys.joinToString()}")
        }
        val keystoreFile = rootProject.file(releaseSigningProperties.getProperty("storeFile"))
        if (!keystoreFile.isFile) {
            throw GradleException("Release keystore does not exist. Check storeFile in keystore.properties.")
        }
        try {
            val keystore = KeyStore.getInstance(keystoreFile, releaseSigningProperties.getProperty("storePassword").toCharArray())
            val alias = releaseSigningProperties.getProperty("keyAlias")
            check(keystore.isKeyEntry(alias))
            check(keystore.getKey(alias, releaseSigningProperties.getProperty("keyPassword").toCharArray()) is java.security.PrivateKey)
            check(keystore.getCertificate(alias) != null)
        } catch (_: Exception) {
            throw GradleException("Cannot access the release private key. Check the keystore, alias and passwords locally.")
        }
    }
}

tasks.matching { it.name == "validateSigningRelease" }.configureEach {
    dependsOn(verifyReleaseSigning)
}

android {
    namespace = "com.github.jimmy90109.geoalarm"
    compileSdk = 37

    val debugAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
    val debugHomeNativeAdUnitId = "ca-app-pub-3940256099942544/2247696110"

    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    defaultConfig {
        applicationId = "com.github.jimmy90109.geoalarm"
        minSdk = 31
        targetSdk = 37
        versionCode = 2611030
        versionName = "1.5.1"
        
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = localProperties.getProperty("maps.apiKey") ?: ""
        manifestPlaceholders["ADMOB_APP_ID"] = localProperties.getProperty("admob.appId") ?: ""
        manifestPlaceholders["appName"] = "@string/app_name"
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${localProperties.getProperty("maps.apiKey") ?: ""}\"")
        buildConfigField("String", "HOME_NATIVE_AD_UNIT_ID", "\"${localProperties.getProperty("admob.homeNativeAdUnitId") ?: ""}\"")
        buildConfigField("Boolean", "ADS_ENABLED", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            releaseSigningProperties.getProperty("storeFile")?.takeIf { it.isNotBlank() }?.let {
                storeFile = rootProject.file(it)
            }
            storePassword = releaseSigningProperties.getProperty("storePassword")
            keyAlias = releaseSigningProperties.getProperty("keyAlias")
            keyPassword = releaseSigningProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            manifestPlaceholders += mapOf()
            val releaseAdMobAppId = localProperties.getProperty("admob.appId") ?: ""
            val releaseHomeNativeAdUnitId = localProperties.getProperty("admob.homeNativeAdUnitId") ?: ""
            manifestPlaceholders["ADMOB_APP_ID"] = releaseAdMobAppId
            buildConfigField("String", "HOME_NATIVE_AD_UNIT_ID", "\"$releaseHomeNativeAdUnitId\"")
            buildConfigField("Boolean", "ADS_ENABLED", "${releaseAdMobAppId.isNotBlank() && releaseHomeNativeAdUnitId.isNotBlank()}")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "@string/app_name_debug"
            manifestPlaceholders["ADMOB_APP_ID"] = debugAdMobAppId
            buildConfigField("String", "HOME_NATIVE_AD_UNIT_ID", "\"$debugHomeNativeAdUnitId\"")
            buildConfigField("Boolean", "ADS_ENABLED", "true")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        suites {
            create("journeysTest") {
                assets {
                }
                targets {
                    create("default") {
                    }
                }
                useJunitEngine {
                    inputs += listOf(com.android.build.api.dsl.AgpTestSuiteInputParameters.TESTED_APKS)
                    includeEngines += listOf("journeys-test-engine")
                    enginesDependencies(libs.junit.platform.launcher)
                    enginesDependencies(libs.junit.platform.engine)
                    enginesDependencies(libs.journeys.junit.engine)
                }
                targetVariants += listOf("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

configurations.matching { it.name == "composeMappingProducerClasspath" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name == "compose-group-mapping") {
            useVersion(libs.versions.kotlin.get())
        }
    }
}

// Room Schema Export Location
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.graphics.path)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.play.services.location)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.oss.licenses)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
    implementation(libs.maps.compose)
    implementation(libs.places)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.appfunctions)
    implementation(libs.androidx.appfunctions.service)
    implementation(libs.user.messaging.platform)

    // Compose
    implementation(libs.androidx.compose.foundation.layout)

    // Glance Widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    ksp(libs.androidx.appfunctions.compiler)

    // HyperIsland ToolKit for Xiaomi Dynamic Island notifications
    implementation(libs.hyperisland.kit)

    // Image/Video Previews & Reorderable List
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.reorderable)
    implementation(libs.telephoto.zoomable.image.coil)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
