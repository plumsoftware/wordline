// Общие объявления плагинов: apply false оставляет подключение конкретному модулю приложения.
plugins {
    // Генерация кода Room; применяется в app/build.gradle.kts.
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Плагин конфигурации Firebase; google-services.json обрабатывается в модуле app.
    alias(libs.plugins.google.gms.google.services) apply false
}
