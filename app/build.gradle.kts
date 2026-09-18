import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room3)
}

// Esquemas exportados versionados no git: base pra escrever migrações e testá-las.
room3 {
    schemaDirectory("$projectDir/schemas")
}

// Permite sobrescrever a URL base da API por máquina, via local.properties (gitignored).
// Útil pra testar em dispositivo físico: emulador alcança o host em 10.0.2.2, mas um
// celular na mesma rede precisa do IP real da máquina (e do backend ouvindo em 0.0.0.0,
// não só em 127.0.0.1) — ou de um túnel `adb reverse tcp:3000 tcp:3000` + 127.0.0.1.
//
//   # local.properties
//   api.baseUrl=http://192.168.0.42:3000
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val devApiBaseUrl = localProperties.getProperty("api.baseUrl") ?: "http://10.0.2.2:3000"

// URL de produção, definida pelo time de backend (2026-09-17). O túnel ainda não está no ar — o
// build de release já aponta pra ela, mas só faz sentido instalar depois que o domínio responder.
val productionApiBaseUrl = "https://backbone.rl2.me"

// Keystore de release: caminho e senhas ficam fora do repo, em keystore.properties (gitignored).
// Sem esse arquivo, o build de release falha ao tentar assinar em vez de saír sem assinatura.
//
//   # keystore.properties
//   storeFile=C:/Users/raave/Chaves/carbon
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.raave.filament"
    compileSdk {
        // 37.1 é exigido pelo androidx.compose.ui 1.13.0-alpha03 (blur progressivo nativo).
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.raave.filament"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Padrão de dev; o build type release sobrescreve com a URL de produção (mais específico
        // vence sobre defaultConfig).
        buildConfigField("String", "API_BASE_URL", "\"$devApiBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "API_BASE_URL", "\"$productionApiBaseUrl\"")
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.framework)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.icons.lucide.android)
    implementation(libs.icons.font.awesome.brands.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
