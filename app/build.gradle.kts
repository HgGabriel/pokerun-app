plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.hggabriel.pokerun"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.hggabriel.pokerun"
        minSdk = 26
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
    buildFeatures {
        compose = true
    }
}

// Kotlin 2.3 removeu o DSL `kotlinOptions`.
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

/**
 * A varredura de cor de `TemaTest` (F0-T13) percorre os fontes de produção em
 * tempo de execução, e fonte não é entrada declarada de uma tarefa de teste — o
 * Gradle só olha o classpath. Um arquivo que não declara nada não gera classe, o
 * classpath não muda, a tarefa fica `UP-TO-DATE` e **o teste não roda**: foi assim
 * que duas violações plantadas passaram por um build verde na Sessão 15.
 *
 * Declarar o diretório devolve a decisão ao Gradle. Sem isto, provar a varredura
 * exige lembrar de `--rerun` — e uma defesa que depende de lembrança não é defesa.
 */
tasks.withType<Test>().configureEach {
    inputs.dir(layout.projectDirectory.dir("src/main/java"))
        .withPropertyName("fontesDeProducao")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    // Navegação type-safe (docs/03 §1)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Firebase — Spark tier, sem Cloud Functions (docs/05 §2.4)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.config) // constantes de calibração (F4-T03)

    // Google Sign-In via Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Ingestão de treinos (docs/05 §4)
    implementation(libs.androidx.health.connect.client)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
