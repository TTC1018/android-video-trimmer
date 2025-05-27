plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    compileSdk = 36
    buildToolsVersion = "31.0.0"

    namespace = "com.gowtham.library"

    packagingOptions {
        pickFirst("lib/x86/libc++_shared.so")
        pickFirst("lib/x86_64/libc++_shared.so")
        pickFirst("lib/armeabi-v7a/libc++_shared.so")
        pickFirst("lib/arm64-v8a/libc++_shared.so")
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        ndkVersion = "22.1.7171670"
        androidResources {
//            localeFilters += setOf("ko-rKR", "en", "ar")
        }
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("assets", "src\\main\\assets")
            jniLibs.srcDirs(listOf("../path/to/libs"))
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    // Kotlin 프로젝트라면 추가
    // kotlinOptions {
    //     jvmTarget = "1.8"
    // }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(files("libs/mobile-ffmpeg-min-gpl.aar"))
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.exoplayer:exoplayer:2.18.2")
    implementation("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")
    implementation("com.akexorcist:localization:1.2.9")
    implementation("com.google.code.gson:gson:2.10")
    implementation("com.airbnb.android:lottie:5.0.2")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.TTC1018"
                artifactId = "android-video-trimmer"
                version = "0.1.7-async"
            }
        }
    }
}

