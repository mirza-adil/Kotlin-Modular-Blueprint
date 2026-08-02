# 📦 Module Structure

## Overview

BankApp uses a **multi-module** Gradle setup — 5 `core` modules, 10 `features` modules, and one `app` shell — to get:

- **Faster, parallel builds** — compiling `:features:accounts:compileDebugKotlin` doesn't touch `:features:cards`
- **Enforced separation of concerns** — a feature literally cannot import another feature's internals; there's no `implementation(project(":features:..."))` between them to accidentally add
- **Reusable core** — `core:common`'s `Result`/`UseCase` and `core:ui`'s theme/components are written once, used by all 10 feature modules
- **Clear, checkable dependency boundaries** — the rules below aren't just convention, they're literally what each `build.gradle.kts` does and doesn't declare

## Module Diagram

```
                                    ┌─────────┐
                                    │   app   │   ⚠️ not yet wired to anything below
                                    └─────────┘      (no project(...) deps — see Dependency Rules)

  ┌──────┬──────────┬──────────┬───────────┬─────────────┬────────┬──────────┬─────────┬──────────────┬──────────────┐
  │ auth │dashboard │ accounts │transactions│  transfers  │beneficiaries│ cards │payments │  profile   │notifications │
  │      │          │          │  🚧 stub  │             │             │       │         │            │              │
  └──┬───┴────┬─────┴────┬─────┴─────┬─────┴──────┬──────┴──────┬──────┴───┬───┴────┬────┴─────┬──────┴──────┬───────┘
     │        │          │           │            │             │          │        │          │             │
     └────────┴──────────┴───────────┴────────────┴──────┬──────┴──────────┴────────┴──────────┴─────────────┘
                                                           │  every feature depends on these two
                                              ┌────────────┴────────────┐
                                              ▼                         ▼
                                        ┌───────────┐            ┌───────────┐
                                        │core:common│            │ core:ui   │
                                        └───────────┘            └───────────┘
                                              ▲                         
                                              │ implementation           
                                    ┌─────────┴─────────┐
                                    │                   │
                              ┌───────────┐       ┌───────────┐
                              │core:network│◄──────│core:security│   only auth & profile
                              └───────────┘       └───────────┘   reach this far down
```

Two things this diagram is deliberately explicit about, because they're easy to assume incorrectly:

1. **`app` doesn't depend on anything yet.** No `core:*` or `features:*` module is declared in `app/build.gradle.kts`, there's no `@HiltAndroidApp` Application class, and `MainActivity` renders a placeholder Compose screen. Every module below the dashed line compiles and works in isolation — nothing has been wired into the app shell. See [Dependency Rules](#dependency-rules) below and the [README's Project Status](../README.md#-project-status).
2. **Only `auth` and `profile` reach `core:network`/`core:security`.** The other 8 feature modules only depend on `core:common` + `core:ui` — their remote data sources are local stubs, not real network calls (see [architecture.md](architecture.md)).

## Module Responsibilities

### 🔹 app

**Entry point module — currently a bare shell**

- `MainActivity.kt` — single Activity, hosts Compose
- `ui/theme/` — an app-level Material theme (separate from `core:ui`'s `BankAppTheme`, not yet consolidated)
- No navigation graph, no DI bootstrap, no feature dependencies

```kotlin
// app/build.gradle.kts — actual current dependencies block
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)   // present, but no NavHost built yet
    implementation(libs.hilt.android)                  // present, but no @HiltAndroidApp class yet
    ksp(libs.hilt.compiler)
    // ⚠️ zero project(":core:...") or project(":features:...") entries
}
```

Wiring this up is 3 concrete steps: add a `@HiltAndroidApp` `Application` class + `android:name` in the manifest, add `@AndroidEntryPoint` to `MainActivity`, then add `implementation(project(":features:..."))` for each screen you want reachable and build a `NavHost` (see [compose_guidelines.md](compose_guidelines.md#navigation)).

### 🔹 core:common

**Shared utilities and base classes — zero Android dependencies**

- `result/Result.kt` — the `Result<T>` sealed interface every layer returns
- `base/BaseUseCase.kt` — `UseCase`, `FlowUseCase`, `NoParamUseCase`, `NoParamFlowUseCase`
- `dispatcher/DispatcherModule.kt` — `@IoDispatcher`/`@MainDispatcher`/`@DefaultDispatcher` Hilt qualifiers
- `extensions/FlowExtensions.kt` — `asResult()`, retry-with-backoff, throttle-first

```kotlin
dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing utilities exposed to every dependent module via `api`
    api(libs.junit)
    api(libs.coroutines.test)
}
```

Every other module in the project — `core` or `features` — depends on this one. It has no `project(...)` dependencies of its own.

### 🔹 core:network

**Retrofit/OkHttp/Moshi networking layer**

- `api/AuthApi.kt` — real, working endpoints (`login`, `refreshToken`, `logout`)
- `api/AccountApi.kt`, `api/KYCApi.kt` — ⚠️ leftover from the original template; still shaped around recipe endpoints (`getRecipes`, `RecipeListResponse`) rather than real account/KYC contracts. Not currently used by `features:accounts` (which stays local-only) or any other feature
- `model/NetworkResponse.kt` — `Success` / `ApiError` / `NetworkError` / `UnknownError` sealed class, the transport-boundary counterpart to `core:common`'s `Result`
- `interceptor/AuthInterceptor.kt` — injects the bearer token, skips requests tagged `No-Auth`
- `di/NetworkModule.kt` — provides `Moshi`, `OkHttpClient`, `Retrofit`, `AuthApi`

```kotlin
dependencies {
    implementation(project(":core:common"))
    api(libs.retrofit)
    api(libs.retrofit.moshi)
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

### 🔹 core:database

**Room persistence — currently unused by any feature module**

- `BankDatabase.kt` — the `RoomDatabase`, entities `BankEntity` / `AccountsEntity` / `CardEntity`
- `dao/BankDao.kt` — CRUD + a `PagingSource` (`getRecipesPagingSource()`, unrenamed leftover — see [interview_questions.md](interview_questions.md#1-paging))
- `entity/BankWithDetails.kt` — a `@Relation`-based join across the three entities

```kotlin
dependencies {
    implementation(project(":core:common"))
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.room.paging)
    ksp(libs.room.compiler)
    api(libs.paging.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

⚠️ **This module isn't depended on by any `features:*` module.** Every feature's `LocalDataSource` today is an in-memory `MutableStateFlow` cache (or, for `auth`/`profile`, `core:security`'s `EncryptedSharedPreferences`) — none of them use Room yet. `core:database` compiles and is ready, but nothing consumes it.

### 🔹 core:security

**Encrypted session storage + native key obfuscation**

- `EncryptedPreferencesManager.kt` — thin wrapper over `EncryptedSharedPreferences`
- `SecureTokenManager.kt` — session persistence (tokens, expiry, basic profile), implements `core:network`'s `TokenProvider`
- `ApiKeyProvider.kt` / `NativeKeyProvider.kt` + `cpp/native-keys.cpp` — NDK-backed, XOR-obfuscated API key storage
- `di/SecurityModule.kt` — binds `TokenProvider`, provides `ApiKeyProvider`

```kotlin
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))   // needed for the TokenProvider interface
    api(libs.androidx.security.crypto)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ndkVersion = libs.versions.ndk.get()
    // + externalNativeBuild { cmake { path("src/main/cpp/CMakeLists.txt") } }
}
```

Only `features:auth` and `features:profile` depend on this module — every other feature manages state entirely with an in-memory cache and never touches encrypted storage.

### 🔹 core:ui

**Shared theme + reusable Compose components — no dependency on other core modules**

- `theme/Color.kt`, `theme/Type.kt`, `theme/Theme.kt` — `BankAppTheme`, `BankTypography`, named color tokens
- `components/BankButton.kt` — `PrimaryButton`, `SecondaryButton`, `TertiaryButton`
- `components/BankTextField.kt` — `BankTextField`, `PasswordTextField`
- `components/LoadingIndicator.kt` — `LoadingIndicator`, `FullScreenLoading`, `BouncingDotsLoading`
- `components/ErrorView.kt` — `ErrorView`, `CompactErrorView`
- `components/RecipeCard.kt` — ⚠️ leftover from the original template, unused, safe to delete

```kotlin
dependencies {
    api(libs.androidx.core.ktx)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons)
    api(libs.androidx.activity.compose)
    api(libs.lifecycle.runtime.compose)
    api(libs.coil.compose)
    api(libs.paging.compose)
    debugApi(libs.androidx.compose.ui.tooling)
    // no project(":core:...") dependency — fully self-contained
}
```

Notably, `core:ui` doesn't depend on `core:common` — it's the one core module with zero project dependencies, which is why every feature module can safely depend on it without pulling in coroutines/dispatcher machinery it doesn't need for pure UI work.

## Feature Modules

Every module below shares the same internal shape — see [architecture.md](architecture.md) for the full `data`/`domain`/`presentation`/`di` layer breakdown. This table is what differs between them: what each one covers, and which `core:*` modules it actually reaches into.

| Module | Covers | Depends on (beyond `core:common` + `core:ui`) | Status |
|:-------|:-------|:------------------------------------------------|:------:|
| `features:auth` | Sign-in, session persistence, logout | `core:network` (real `AuthApi`), `core:security` (`SecureTokenManager`) | ✅ |
| `features:dashboard` | Total balance, account summaries, recent activity | — (local cache only) | ✅ |
| `features:accounts` | Account list & details | — (local cache only) | ✅ |
| `features:transfers` | Own-account, local & international transfers | — (local cache only) | ✅ |
| `features:beneficiaries` | Saved recipients — add, browse, remove | — (local cache only) | ✅ |
| `features:cards` | Card list, freeze/unfreeze | — (local cache only) | ✅ |
| `features:payments` | Bill payments, scheduled payments | — (local cache only) | ✅ |
| `features:profile` | Personal details, biometric/dark-mode preferences | `core:network`, `core:security` (reads the same cached session as `auth`) | ✅ |
| `features:notifications` | Inbox, push notification preferences | — (local cache only) | ✅ |
| `features:transactions` | Transaction history, filtering, detail | — | 🚧 placeholder |

A representative feature `build.gradle.kts` (every module in the "local cache only" row looks like this):

```kotlin
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
```

`auth` and `profile` add exactly two more lines — `implementation(project(":core:network"))` and `implementation(project(":core:security"))` — nothing else about their shape changes.

## Dependency Rules

1. **Feature modules depend only on `core:*` modules — never on each other.** There is no `implementation(project(":features:..."))` anywhere in this repo; verified by grepping every feature's `build.gradle.kts`
2. **Core modules may depend on other core modules**, but the graph stays shallow: `core:network` → `core:common`; `core:database` → `core:common`; `core:security` → `core:common` + `core:network`; `core:ui` depends on nothing else
3. **`domain` has zero Android dependencies**, in every feature, with no exceptions — it's what makes use cases testable as plain JVM unit tests
4. **`app` should depend on every `features:*` module plus `core:ui`** for the theme — today it depends on neither; that's the top item in the [roadmap](../README.md#-project-status)
5. **Nothing above `data` ever sees a DTO or entity.** `presentation` and `domain` only ever see the mapped domain model (`Account`, `Transfer`, `Card`, ...) — the mapping happens once, inside each feature's `data/mapper/`, and never leaks upward

---

See [architecture.md](architecture.md) for the layer-by-layer Clean Architecture walkthrough, [compose_guidelines.md](compose_guidelines.md) for UI-layer conventions, and the main [README](../README.md) for full project status.
