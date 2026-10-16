plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.baselineprofile)
}

/**
 * Gera o Baseline Profile do app e mede a partida a frio (docs/02 §6).
 *
 * Gerar: `./gradlew :app:generateReleaseBaselineProfile`, com um aparelho de API 33 ou
 * mais conectado e desbloqueado. O perfil vai para `app/src/release/generated/baselineProfiles`.
 * Medir: `./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest`.
 */
android {
    namespace = "com.hggabriel.pokerun.baselineprofile"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

// Gera no aparelho conectado, não num emulador gerenciado pelo Gradle.
baselineProfile {
    useConnectedDevices = true
}

/**
 * O plugin pendura a geração no `assemble` deste módulo: um `./gradlew assemble` ou
 * `build` instalava o app no aparelho conectado, rodava a jornada e o desinstalava, levando
 * o login e os caches locais (15/10/2026). Os testes de aparelho só rodam quando a tarefa
 * de geração ou de medição é pedida pelo nome.
 */
val pedidoNoAparelho = gradle.startParameter.taskNames.any { tarefa ->
    "BaselineProfile" in tarefa || "connected" in tarefa
}
tasks.matching { it.name.startsWith("connected") || it.name.startsWith("collect") }.configureEach {
    onlyIf("só roda quando pedida pelo nome") { pedidoNoAparelho }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
