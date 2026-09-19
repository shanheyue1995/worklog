import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

// 正式签名信息放在 keystore/release.properties（不入库，见 keystore/README.md）。
// 没有这个文件时 release 包是未签名的，别人克隆下来照样能构建、能跑测试。
val releaseSigning: Map<String, String> = rootProject.file("keystore/release.properties")
    .takeIf { it.exists() }
    ?.let { file ->
        Properties().apply { file.inputStream().use { load(it) } }
            .entries
            .associate { (key, value) -> key.toString() to value.toString() }
    }
    ?: emptyMap()

android {
    namespace = "com.worklog.quickrecord"
    compileSdk = 37

    signingConfigs {
        create("development") {
            // 固定使用工程内的调试签名。
            // 默认的调试签名会随构建环境变化（HOME / ANDROID_USER_HOME 不同就换一个），
            // 一旦漂移，新包就装不上已装的旧包，只能卸载重装、丢掉全部记录。
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        if (releaseSigning.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.worklog.quickrecord"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "0.2.1"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("development")
        }
        release {
            isMinifyEnabled = false
            if (releaseSigning.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    // 导出建表语句，便于后续做数据库迁移。
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.ui.tooling)
}
