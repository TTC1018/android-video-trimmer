plugins {
    alias(libs.plugins.android.application)
    // kotlin("android") // 필요하다면 주석 해제
}

android {
    compileSdk = 36
    buildToolsVersion = "30.0.3"

    namespace = "com.gowtham.videotrimmer"

    defaultConfig {
        applicationId = "com.gowtham.videotrimmer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        resConfigs += listOf("ko-rKR", "en", "ar")
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "src/main/assets/")
            res.srcDirs("src/main/res", "src/main/res/drawable")
        }
    }

    dexOptions {
        javaMaxHeapSize = "4g"
    }

    configurations.all {
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "xpp3", module = "xpp3")
    }

    packagingOptions {
        exclude("META-INF/license.txt")
        exclude("META-INF/notice.txt")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
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

    // Kotlin 프로젝트인 경우
    // kotlinOptions {
    //     jvmTarget = "1.8"
    // }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(project(":library"))
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.cocosw:bottomsheet:1.5.0@aar")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
}
