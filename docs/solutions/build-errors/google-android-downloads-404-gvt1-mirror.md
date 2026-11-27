---
title: Google отвечает 404 на Android SDK и Google Maven — загрузка через redirector.gvt1.com
date: 2026-11-24
category: build-errors
module: build
problem_type: build_error
component: tooling
symptoms:
  - "Мастер первого запуска Android Studio падает с «SDK emulator directory is missing», папка SDK не создаётся"
  - "В idea.log ответы 404 на https://dl.google.com/android/repository/addons_list-*.xml и repository2-*.xml"
  - "Проверка обновлений Studio (dl.google.com/android/studio/patches/updates.xml) тоже получает 404"
  - "Gradle не находит AGP и библиотеки AndroidX: maven.google.com и dl.google.com/dl/android/maven2 отвечают 404"
root_cause: blocked_download_host
resolution_type: environment_setup
severity: high
framework_version: android studio 2026.2.2, agp 9.5.2, gradle 9.9.2
tags: [android-sdk, google-maven, dl-google-com, gvt1, mirror, sdk-test-base-url, local-properties, russia]
---

# Google отвечает 404 на Android SDK и Google Maven — загрузка через redirector.gvt1.com

## Problem

С компьютера владельца (Россия, 2026-11) Google отдаёт 404 на все файлы Android SDK и на Google Maven. Сайт developer.android.com при этом открывается, поэтому это похоже на ограничение по региону, а не на сбой сети. Без обхода нельзя ни поставить SDK, ни собрать проект: AGP и AndroidX лежат в Google Maven.

## Symptoms

- Мастер первого запуска Android Studio выбирает Standard, ничего не скачивает и падает с «SDK emulator directory is missing». Папки `%LOCALAPPDATA%\Android\Sdk` нет.
- В `%LOCALAPPDATA%\Google\AndroidStudio<версия>\log\idea.log` подряд идут `Request failed with status code 404` для `addons_list-7.xml` … `addons_list-1.xml`, `repository2-3.xml`, `repository2-4.xml`, затем `Failed to download any source lists!`.
- `curl -s -o /dev/null -w '%{http_code}' https://dl.google.com/android/repository/addons_list-5.xml` печатает `404`; так же отвечают `https://maven.google.com/com/android/tools/build/gradle/maven-metadata.xml` и `https://dl.google.com/dl/android/maven2/...`.

## What Didn't Work

- Мастер Android Studio без настроек: все списки пакетов получают 404.
- `winget` для установки инструментов: на этом компьютере он не смог обновить свой источник (`InternetOpenUrl() failed`, 0x80072ee7).
- Init-скрипт Gradle в `~/.gradle/init.d`, который подменяет адрес google() для всех проектов: запись в домашнюю папку пользователя отклонила проверка безопасности агента. Настройка в проекте к тому же нагляднее: её видно в `settings.gradle.kts`.

## Solution

Те же файлы отдаёт CDN Google `redirector.gvt1.com` с префиксом `edgedl`. Проверка: `https://redirector.gvt1.com/edgedl/android/repository/addons_list-5.xml` и `https://redirector.gvt1.com/edgedl/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml` отвечают 200 и отдают настоящие списки пакетов и версий.

**1. Android SDK (Studio, sdkmanager и автодокачка в AGP).** Задать пользовательскую переменную окружения и полностью перезапустить Android Studio:

```powershell
[Environment]::SetEnvironmentVariable('SDK_TEST_BASE_URL', 'https://redirector.gvt1.com/edgedl/android/repository/', 'User')
```

Адрес заканчивается на `/`, как и адрес по умолчанию `https://dl.google.com/android/repository/` внутри `sdklib.jar`. Убрать переменную можно той же командой со значением `$null`. После перезапуска мастер или SDK Manager (More Actions → SDK Manager) ставят SDK, platform-tools, эмулятор и образы систем. Когда сборке не хватило платформы, Gradle с этой переменной в окружении команды сам поставил `Android SDK Platform 37.2` (в логе сборки `Installing Android SDK Platform 37.2`). Без переменной этот сценарий не проверялся.

**2. Google Maven (плагины и библиотеки Gradle).** В `local.properties`, который не попадает в git, задать адрес зеркала:

```properties
googleMavenMirror=https://redirector.gvt1.com/edgedl/dl/android/maven2/
```

`settings.gradle.kts` читает этот ключ и подставляет `maven(mirror)` вместо `google()` в обоих списках репозиториев:

```kotlin
pluginManagement {
    val googleMavenMirror = java.util.Properties()
        .apply { settingsDir.resolve("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) } }
        .getProperty("googleMavenMirror")
    repositories {
        if (googleMavenMirror != null) {
            maven(googleMavenMirror) {
                content {
                    includeGroupByRegex("androidx\\..*")
                    includeGroupByRegex("com\\.android(\\..*)?")
                    includeGroupByRegex("com\\.google\\..*")
                }
            }
        } else {
            google()
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Блок `pluginManagement` вычисляется отдельно от остального файла и не видит его переменных, поэтому для `dependencyResolutionManagement` свойство читается второй раз. В CI `local.properties` нет, и там остаётся обычный `google()`.

Фильтр `content` отдаёт зеркалу только группы Google. Без него Gradle сначала спрашивает зеркало о любой библиотеке. 2026-11-26 запрос за `kotlinx-coroutines-bom` к зеркалу завис по тайм-ауту, и сборка упала, хотя Maven Central отвечал. Группы `com.google.*` из Maven Central (KSP, Guava, Gson) по-прежнему идут сначала на зеркало.

## Why This Works

По наблюдению из этой сети, ограничение срабатывает на адресах `dl.google.com` и `maven.google.com`, а `redirector.gvt1.com` перенаправляет запрос на узел CDN Google (`rr*---sn-*.gvt1.com`) и отдаёт те же списки и артефакты по тем же путям с префиксом `edgedl`. В `sdklib.jar` Android Studio есть строка `SDK_TEST_BASE_URL`: этой переменной окружения SDK-менеджер заменяет базовый адрес репозитория. В манифестах SDK архивы указаны относительными путями, поэтому вслед за манифестами и сами архивы качаются с того же зеркала.

## Prevention

- При любой ошибке загрузки SDK или артефакта Google сначала проверь оба адреса через `curl`: `dl.google.com` отвечает 404, а тот же файл по адресу `https://redirector.gvt1.com/edgedl/<путь>` отвечает 200 — значит, это та же блокировка.
- Новые компоненты SDK ставь из Studio после перезапуска с переменной или из командной строки, где `SDK_TEST_BASE_URL` есть в окружении.
- На новом компьютере разработчика из той же сети: задать переменную и добавить `googleMavenMirror` в `local.properties` до первой синхронизации проекта.

## Related Issues

- `settings.gradle.kts` — подстановка зеркала.
- `.gitignore` — `local.properties` не попадает в git.
