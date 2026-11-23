plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // KSP генерирует реализацию Room по аннотациям базы данных и DAO.
    alias(libs.plugins.ksp)
    // Преобразует google-services.json в Android-ресурсы конфигурации Firebase.
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "ru.plumsoftware.wordline"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.plumsoftware.wordline"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Создаёт отдельные варианты приложения для RuStore, Google Play и Huawei AppGallery.
    flavorDimensions += "store"
    productFlavors {
        create("rustore") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"rustore\"")
            // Контакт поддержки.
            buildConfigField("String", "SUPPORT_EMAIL", "\"Plumsoftware@yandex.ru\"")
            // Ссылка на политику конфиденциальности.
            buildConfigField("String", "PRIVACY_URL", "\"https://raw.githubusercontent.com/plumsoftware/privacy-policy-pool/refs/heads/main/wordline/privacy-policy.md\"")
            // Аудитория: general или children.
            buildConfigField("String", "AUDIENCE", "\"general\"")
            // Видео с наградой.
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"R-M-20460787-2\"")
            // Видео с наградой после уровня.
            buildConfigField("String", "LEVEL_COMPLETE_REWARDED_AD_UNIT_ID", "\"R-M-20460787-3\"")
            // Межстраничная реклама.
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"R-M-20460787-1\"")
        }
        create("googlePlay") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"googlePlay\"")
            // Контакт поддержки.
            buildConfigField("String", "SUPPORT_EMAIL", "\"\"")
            // Ссылка на политику конфиденциальности.
            buildConfigField("String", "PRIVACY_URL", "\"\"")
            // Аудитория: general или children.
            buildConfigField("String", "AUDIENCE", "\"\"")
            // Видео с наградой.
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"\"")
            // Видео с наградой после уровня.
            buildConfigField("String", "LEVEL_COMPLETE_REWARDED_AD_UNIT_ID", "\"\"")
            // Межстраничная реклама.
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"\"")
        }
        create("huaweiAppGallery") {
            dimension = "store"
            buildConfigField("String", "STORE", "\"huaweiAppGallery\"")
            // Контакт поддержки.
            buildConfigField("String", "SUPPORT_EMAIL", "\"\"")
            // Ссылка на политику конфиденциальности.
            buildConfigField("String", "PRIVACY_URL", "\"\"")
            // Аудитория: general или children.
            buildConfigField("String", "AUDIENCE", "\"\"")
            // Видео с наградой.
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"\"")
            // Видео с наградой после уровня.
            buildConfigField("String", "LEVEL_COMPLETE_REWARDED_AD_UNIT_ID", "\"\"")
            // Межстраничная реклама.
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"\"")
        }
    }
    buildTypes {
        debug {
            buildConfigField("boolean", "ADS_ENABLED", "true")
        }
        // Вариант для ручной проверки интерфейса: настройки debug, но реклама отключена.
        create("qa") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
            buildConfigField("boolean", "ADS_ENABLED", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "ADS_ENABLED", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Передаёт JVM-тестам путь к игровому контенту без Android Context и устройства.
    testOptions {
        unitTests.all {
            it.systemProperty("gameContentDir", file("src/main/assets/content").absolutePath)
        }
    }
}

dependencies {
    // WorkManager запускает DailyTasksWorker: фоновая проверка новых ежедневных заданий.
    implementation(libs.androidx.work.runtime)
    // Firebase Cloud Messaging принимает push-сообщения; обработчик — WordLineMessagingService.
    implementation(libs.firebase.messaging)
    implementation(libs.yandex.ads)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.json.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Сохраняет JSON-схемы Room в app/schemas для истории изменений и проверки миграций.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Перед сборкой запускает tools/validate_content.py через Python 3 из корня проекта.
// Скрипт проверяет комплектность и корректность встроенных кроссвордов; ошибка останавливает сборку.
val validateContent by tasks.registering(Exec::class) {
    group = "verification"
    description = "Validate the bundled crossword package before Android compilation. Requires Python 3."
    workingDir(rootProject.projectDir)
    commandLine(if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3", "tools/validate_content.py")
    inputs.dir("src/main/assets/content")
    inputs.file(rootProject.file("tools/validate_content.py"))
}
// Подключает проверку контента ко всем вариантам сборки, включая debug, qa и release.
tasks.named("preBuild") { dependsOn(validateContent) }

// Общая задача для ручной проверки всех релизных вариантов.
val validateReleaseInputs by tasks.registering {
    group = "verification"
    description = "Validate BuildConfig values for all release variants."
}

// В AGP 9 поля читаются через Variant API, после объединения flavor и build type.
// Каждый релиз проверяет только собственные значения, заданные напрямую выше.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val variantName = variant.name
        val capitalizedName = variantName.replaceFirstChar { it.uppercaseChar() }
        val fields = requireNotNull(variant.buildConfigFields).map { values ->
            values.mapValues { (_, field) -> field.value.toString() }
        }
        val validateVariantInputs = tasks.register("validate${capitalizedName}Inputs") {
            group = "verification"
            description = "Validate BuildConfig values for $variantName."
            inputs.property("buildConfigFields", fields)
            doLast {
                val values = fields.get()
                val adsEnabled = values.getValue("ADS_ENABLED").toBooleanStrict()
                val requiredFields = listOf("SUPPORT_EMAIL", "PRIVACY_URL", "AUDIENCE") +
                    if (adsEnabled) listOf("REWARDED_AD_UNIT_ID", "INTERSTITIAL_AD_UNIT_ID") else emptyList()
                requiredFields.forEach { name ->
                    val value = values.getValue(name).removeSurrounding("\"")
                    require(value.isNotBlank() && !value.startsWith("demo-")) {
                        "Configure $name for $variantName in buildConfigField in app/build.gradle.kts"
                    }
                }
                require(values.getValue("PRIVACY_URL").removeSurrounding("\"").startsWith("https://")) {
                    "Production privacy URL must use HTTPS"
                }
                require(values.getValue("AUDIENCE").removeSurrounding("\"") in setOf("general", "children")) {
                    "Configure the production audience: general or children"
                }
                require('@' in values.getValue("SUPPORT_EMAIL").removeSurrounding("\"")) {
                    "Configure a valid support contact"
                }
            }
        }
        validateReleaseInputs.configure { dependsOn(validateVariantInputs) }
        tasks.configureEach {
            if (name == "pre${capitalizedName}Build") dependsOn(validateVariantInputs)
        }
    }
}
// Включает фиксацию разрешённых версий зависимостей; lock-файлы создаются через --write-locks.
dependencyLocking { lockAllConfigurations() }
