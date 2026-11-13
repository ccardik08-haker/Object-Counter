plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.objectcounter"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.objectcounter"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

}

dependencies {
    implementation("androidx.compose.ui:ui-graphics")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    tasks.register<Copy>("copyApkToRoot") {
        group = "build"
        description = "Копирует собранный APK в корень проекта"

        dependsOn("assembleDebug")

        from(layout.buildDirectory.dir("outputs/apk/debug")) {
            include("*.apk")
            rename { "ObjectCounter.apk" } // Переименовываем файл на лету
        }

        into(rootProject.projectDir)

        doLast {
            println("✅ Проверьте корень проекта! APK должен быть там: ${rootProject.projectDir.absolutePath}")
        }
    }

    tasks.named("build") {
        dependsOn("copyApkToRoot")
    }
    implementation("androidx.compose.material:material-icons-extended")
}