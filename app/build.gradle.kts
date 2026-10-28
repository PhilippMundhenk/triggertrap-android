plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    jacoco
}

android {
    namespace = "com.triggertrap"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.triggertrap"
        minSdk = 26
        targetSdk = 35
        versionCode = 4000
        versionName = "4.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing: app/release.keystore + env (CI secrets). Without it the release build is
    // debug-signed so the artifact is still installable.
    val keystore = file("release.keystore")
    signingConfigs {
        if (keystore.isFile) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // instrumented (Espresso) coverage, see jacocoAndroidTestReport below
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (keystore.isFile) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        // the legacy code base; warnings are reported, only errors fail the build
        abortOnError = true
        checkReleaseBuilds = false
        disable += setOf("MissingTranslation", "ExtraTranslation")
    }
    packaging {
        resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment:1.8.5")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.viewpager:viewpager:1.1.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    // Wi-Fi master / slave discovery (Bonjour); the 2013 jar is replaced by the Maven artifact
    implementation("org.jmdns:jmdns:3.5.9")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

// ---------------------------------------------------------------------------------------------
// Coverage of the instrumented tests (the only tests of this module: everything here touches
// Android). AGP writes the JaCoCo .ec files when connectedDebugAndroidTest runs with
// enableAndroidTestCoverage; these tasks turn them into a report and a line gate.
// Excluded: R, BuildConfig.
jacoco { toolVersion = "0.8.12" }

val coverageClassDirs = files(
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")) {
        exclude("**/BuildConfig*", "**/R.class", "**/R\$*.class")
    },
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        exclude("**/BuildConfig*", "**/R.class", "**/R\$*.class")
    }
)
val coverageExecData = fileTree(layout.buildDirectory.dir("outputs/code_coverage/debugAndroidTest/connected")) { include("**/*.ec") }
val coverageSources = files("src/main/java", "src/main/kotlin")

tasks.register<JacocoReport>("jacocoAndroidTestReport") {
    group = "verification"
    description = "Coverage report of the instrumented tests (run connectedDebugAndroidTest first)"
    dependsOn("compileDebugJavaWithJavac"); mustRunAfter("connectedDebugAndroidTest")
    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(coverageSources)
    executionData.setFrom(coverageExecData)
    reports { xml.required.set(true); html.required.set(true) }
}

tasks.register<JacocoCoverageVerification>("jacocoAndroidTestCoverageVerification") {
    group = "verification"
    description = "Fails when the instrumented tests cover less than 60 % of the app lines (measured 65.2 % on 2026-10-28, target 95 %: issue #9)"
    dependsOn("compileDebugJavaWithJavac"); mustRunAfter("connectedDebugAndroidTest", "jacocoAndroidTestReport")
    classDirectories.setFrom(coverageClassDirs)
    sourceDirectories.setFrom(coverageSources)
    executionData.setFrom(coverageExecData)
    violationRules { rule { limit { counter = "LINE"; value = "COVEREDRATIO"; minimum = "0.60".toBigDecimal() } } }
}
