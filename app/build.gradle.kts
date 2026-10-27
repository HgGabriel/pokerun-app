plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.hotswan.compiler)
}

hotSwanCompiler {
    enabled = true
    debugOnly = true
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
        // Toda distribuição pede um versionCode maior (nº 86). 1 foi a de 21/09, 2 a de 15/10,
        // 3 a de 20/10, 4 a de 26/10.
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // O R8 é o maior ganho de quadro do Compose: junta lambdas, tira o
            // `sourceInformation` e desvirtualiza o runtime. Sem ele, o release roda quase
            // como o debug e a grade da Pokédex engasga no scroll.
            isMinifyEnabled = true
            isShrinkResources = true
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

/**
 * O plugin do Baseline Profile cria `nonMinifiedRelease` e `benchmarkRelease` a partir do
 * `release`, que não tem assinatura: sem uma, o APK não instala no aparelho que gera e
 * mede o perfil. Só esses dois tipos usam a chave de debug; o `release` fica como está.
 */
android.buildTypes.configureEach {
    if (name == "nonMinifiedRelease" || name == "benchmarkRelease") {
        signingConfig = android.signingConfigs.getByName("debug")
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

    // Mesma armadilha, outro diretório: o lint de microcopy de `F1-T20`
    // (`MicrocopiaTest`) lê `values/strings.xml` em tempo de execução. Uma linha
    // acrescentada lá não gera classe nenhuma, e sem esta declaração o travessão
    // novo entraria com a suíte `UP-TO-DATE` e verde.
    inputs.dir(layout.projectDirectory.dir("src/main/res"))
        .withPropertyName("recursosDeProducao")
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

    // Sprite da espécie, com cache em disco: sem ele o app sem sinal mostra o card vazio
    // (D-10). Entrou com F2-T14, que a autoriza na ficha.
    implementation(libs.coil.compose)
    // Os sprites animados do Showdown na arena do registro de corrida (26/10/2026).
    implementation(libs.coil.gif)

    // Splash do sistema: a mesma API do Android 12 no piso 26. Abaixo do 31 o ícone
    // fica estático (docs/02 §2.7).
    implementation(libs.androidx.core.splashscreen)

    // Instala o Baseline Profile que `:baselineprofile` gera: a partida a frio e a abertura
    // rodam pré-compiladas desde a primeira execução, sem esperar o JIT.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    // Google Sign-In via Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
