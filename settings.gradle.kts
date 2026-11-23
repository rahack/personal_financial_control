pluginManagement {
    // Из некоторых сетей Google Maven отвечает 404. Тогда в local.properties задаётся
    // googleMavenMirror: адрес того же репозитория на другом сервере Google.
    val googleMavenMirror = java.util.Properties()
        .apply { settingsDir.resolve("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) } }
        .getProperty("googleMavenMirror")
    repositories {
        if (googleMavenMirror != null) maven(googleMavenMirror) else google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Блок pluginManagement вычисляется отдельно от остального файла, поэтому свойство читается ещё раз.
val googleMavenMirror: String? = java.util.Properties()
    .apply { settingsDir.resolve("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) } }
    .getProperty("googleMavenMirror")

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        if (googleMavenMirror != null) maven(googleMavenMirror) else google()
        mavenCentral()
    }
}

rootProject.name = "personal-financial-control"

include(":shared", ":androidApp")
