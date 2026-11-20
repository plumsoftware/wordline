plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Independent production ad units per store. Fill these later or override with -Pwordline.<store>.*.
val storeAdUnits = mapOf(
    "rustore" to ("" to ""),            // rewarded, interstitial
    "googlePlay" to ("" to ""),
    "huaweiAppGallery" to ("" to "")
)
fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

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

    flavorDimensions += "store"
    productFlavors {
        create("rustore") { dimension = "store" }
        create("googlePlay") { dimension = "store" }
        create("huaweiAppGallery") { dimension = "store" }
        all {
            // Override each store independently in ~/.gradle/gradle.properties or CI -P arguments.
            val prefix = "wordline.${name}"
            buildConfigField("String", "STORE", "\"${name}\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", quoted(providers.gradleProperty("$prefix.rewardedAdUnitId").orElse(storeAdUnits.getValue(name).first).get()))
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", quoted(providers.gradleProperty("$prefix.interstitialAdUnitId").orElse(storeAdUnits.getValue(name).second).get()))
            buildConfigField("String", "SUPPORT_EMAIL", quoted(providers.gradleProperty("$prefix.supportEmail").orElse("").get()))
            buildConfigField("String", "PRIVACY_URL", quoted(providers.gradleProperty("$prefix.privacyUrl").orElse("").get()))
            buildConfigField("String", "AUDIENCE", quoted(providers.gradleProperty("$prefix.audience").orElse("").get()))
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "ADS_ENABLED", providers.gradleProperty("wordline.adsEnabled").orElse("true").get())
        }
        create("qa") {
            initWith(getByName("debug"))
            matchingFallbacks += "debug"
            applicationIdSuffix = ".qa"
            buildConfigField("boolean", "ADS_ENABLED", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "ADS_ENABLED", providers.gradleProperty("wordline.adsEnabled").orElse("true").get())
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
    testOptions {
        unitTests.all {
            it.systemProperty("gameContentDir", file("src/main/assets/content").absolutePath)
        }
    }
}

dependencies {
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val validateContent by tasks.registering(Exec::class) {
    group = "verification"
    description = "Validate the bundled crossword package before Android compilation. Requires Python 3."
    workingDir(rootProject.projectDir)
    commandLine("python3", "tools/validate_content.py")
    inputs.dir("src/main/assets/content")
    inputs.file(rootProject.file("tools/validate_content.py"))
}
tasks.named("preBuild") { dependsOn(validateContent) }

val requestedReleaseTasks = gradle.startParameter.taskNames.joinToString(" ").lowercase()
val releaseStores = listOf("rustore", "googlePlay", "huaweiAppGallery").filter {
    requestedReleaseTasks.contains(it.lowercase())
}.ifEmpty { listOf("rustore", "googlePlay", "huaweiAppGallery") }
val releaseValues = releaseStores.associateWith { store ->
    listOf("rewardedAdUnitId", "interstitialAdUnitId", "supportEmail", "privacyUrl", "audience").associateWith { key ->
        providers.gradleProperty("wordline.$store.$key").orElse(when (key) { "rewardedAdUnitId" -> storeAdUnits.getValue(store).first; "interstitialAdUnitId" -> storeAdUnits.getValue(store).second; else -> "" }).get()
    }
}
val releaseAdsEnabled = providers.gradleProperty("wordline.adsEnabled").orElse("true").get().toBooleanStrict()
val validateReleaseInputs by tasks.registering {
    group = "verification"
    inputs.properties(releaseValues.flatMap { (store, values) -> values.map { (key, value) -> "$store.$key" to value } }.toMap())
    doLast {
        releaseValues.forEach { (store, values) ->
            values.forEach { (key, value) ->
                if (releaseAdsEnabled || key !in setOf("rewardedAdUnitId", "interstitialAdUnitId")) {
                    require(value.isNotBlank() && !value.startsWith("demo-")) { "Provide wordline.$store.$key before a production release" }
                }
            }
            require(values.getValue("privacyUrl").startsWith("https://")) { "Production privacy URL must use HTTPS" }
            require(values.getValue("audience") in setOf("general", "children")) { "Configure the production audience: general or children" }
            require('@' in values.getValue("supportEmail")) { "Configure a valid support contact" }
        }
    }
}
tasks.configureEach {
    if (name.startsWith("pre") && name.endsWith("ReleaseBuild")) dependsOn(validateReleaseInputs)
}
dependencyLocking { lockAllConfigurations() }
