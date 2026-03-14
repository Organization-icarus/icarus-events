plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.icarus.events"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.icarus.events"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    /*implementation line from given from Claude AI, March 13, 2026 "How can I find the local
    * android.jar file across any operating system"
    * */
    //implementation(files("${android.sdkDirectory}/platforms/android-${android.compileSdk}/android.jar"))
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

tasks.register<Javadoc>("javadoc") {
    source = android.sourceSets["main"].java.getSourceFiles()
    classpath += files(android.bootClasspath.joinToString(File.pathSeparator))

    android.applicationVariants.configureEach {
        if (name == "debug") {
            classpath += javaCompileProvider.get().classpath
        }
    }

    destinationDir = file("${projectDir}/docs/javadoc")
    isFailOnError = false

    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
    }
}