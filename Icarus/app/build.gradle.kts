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
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/INDEX.LIST"
        }
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
    implementation("com.cloudinary:cloudinary-android:2.2.0") // For Storing Images
    implementation("com.squareup.picasso:picasso:2.71828") // For Displaying Images via URL
    implementation("com.google.android.gms:play-services-location:21.3.0")
    //implementation(libs.play.services.maps) // For location services
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    androidTestImplementation(libs.espresso.intents)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.work:work-runtime:2.9.0")
    implementation("com.google.guava:guava:31.1-android")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.1")

    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.android.volley:volley:1.2.1")
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