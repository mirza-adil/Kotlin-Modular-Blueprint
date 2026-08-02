# 🏦 BankApp — Kotlin Modular Blueprint

<div align="center">

**Enterprise-Grade, Large-Scale Clean Architecture Reference for Android Banking Apps**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.09-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Hilt-2.52-34A853.svg?logo=android)](https://dagger.dev/hilt/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean-FF6F00.svg)](https://developer.android.com/topic/architecture)
[![Scale](https://img.shields.io/badge/Scale-Enterprise-1A237E.svg)](#️-architecture)

<br/>

*A modular Android codebase that models a retail banking app — accounts, transfers, cards, payments, and more — with every feature isolated in its own Gradle module and built to the same Clean Architecture contract.*

[Features](#-features) • [Architecture](#️-architecture) • [Tech Stack](#️-tech-stack) • [Getting Started](#-getting-started) • [Project Status](#-project-status)

</div>

---

## ✨ Highlights

| 🏗️ **Clean Architecture** | 📱 **Modern UI** | 🔒 **Secure** | 🧩 **Modular** |
|:---:|:---:|:---:|:---:|
| `data` / `domain` / `presentation` / `di` in every feature | Jetpack Compose with Material 3 | Native (NDK) key storage + encrypted token store | 9 independent feature modules, zero cross-feature coupling |

---

## 📸 Screenshots

| Sign In | Dashboard | Transfers |
|:-------:|:---------:|:---------:|
| 🔐 Secure sign-in | 💳 Balance & account overview | 💸 Own-account, local & international |

> Screens aren't wired into a navigation graph yet — see [Project Status](#-project-status). Each feature's `Screen.kt` composable is complete and previewable in isolation today.

---

## 🎯 Features

| Module | Covers | Status |
|:-------|:-------|:------:|
| 🔐 `features:auth` | Email/password sign-in, session persistence, logout | ✅ |
| 🏠 `features:dashboard` | Total balance, per-account summaries, recent activity | ✅ |
| 💳 `features:accounts` | Account list & account details | ✅ |
| 💸 `features:transfers` | Own-account, local, and international transfers | ✅ |
| 👥 `features:beneficiaries` | Add, browse, and remove saved recipients | ✅ |
| 💳 `features:cards` | Card list, freeze/unfreeze, card controls | ✅ |
| 🧾 `features:payments` | Bill payments and recurring scheduled payments | ✅ |
| ⚙️ `features:profile` | Personal details, biometric login & dark mode preferences | ✅ |
| 🔔 `features:notifications` | Notification inbox, push preferences | ✅ |
| 📊 `features:transactions` | Transaction history, filtering, detail | 🚧 |

---

## 🏗️ Architecture

Every feature module follows **Clean Architecture**, split into four packages with a strict, one-directional dependency rule. The same shape scales from a single feature to a large, multi-team codebase without changing:

- ✅ **Separation of concerns** — a `Screen` never imports Retrofit; a `Repository` never imports Compose
- ✅ **Testability** — `domain` is plain Kotlin, so business rules test without Android, a device, or an emulator
- ✅ **Consistency** — learn one feature's shape, and you've learned all nine
- ✅ **Independent modules** — no `features:*` module depends on another; anything shared lives in `core:*`
- ✅ **Enterprise / large-scale ready** — isolated Gradle modules mean parallel builds, independent per-feature ownership, and no cross-feature merge conflicts as team and codebase size grow

### Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                                │
│  ┌────────────────┐    ┌────────────────┐    ┌─────────────────────────┐   │
│  │    Screens     │    │   ViewModels   │    │       UI State          │   │
│  │   (Compose)    │◄───│  (StateFlow)   │───►│    (Immutable)          │   │
│  │                │    │                │    │                         │   │
│  │ • AuthScreen   │    │ • AuthVM       │    │ • AuthUiState           │   │
│  │ • AccountScreen│    │ • AccountVM    │    │ • AccountUiState        │   │
│  │ • TransferScreen│   │ • TransferVM   │    │ • TransferUiState       │   │
│  └────────────────┘    └───────┬────────┘    └─────────────────────────┘   │
│                                │                                            │
└────────────────────────────────┼────────────────────────────────────────────┘
                                 │ invoke()
                                 ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                             DOMAIN LAYER                                    │
│  ┌────────────────┐    ┌────────────────┐    ┌─────────────────────────┐   │
│  │   Use Cases    │    │     Models     │    │  Repository Interfaces  │   │
│  │                │    │                │    │                         │   │
│  │ • LoginUseCase │    │ • Account      │    │ • AccountRepository     │   │
│  │ • GetAccounts  │    │ • Transfer     │    │ • TransferRepository    │   │
│  │ • CreateTransfer│   │ • Card         │    │ • AuthRepository        │   │
│  │ • PayBill      │    │ • Payment      │    │ • ...                   │   │
│  └────────────────┘    └────────────────┘    └────────────┬────────────┘   │
│                                                            │                │
└────────────────────────────────────────────────────────────┼────────────────┘
                                                             │ implements
                                                             ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                     │
│  ┌────────────────┐    ┌────────────────┐    ┌─────────────────────────┐   │
│  │  Repository    │    │  Data Sources  │    │        Mappers          │   │
│  │     Impl       │    │                │    │                         │   │
│  │                │◄───│ • RemoteDataSource│  │ • AccountDto → Account  │   │
│  │ • AccountRepo  │    │ • LocalDataSource │  │ • UserDto → User        │   │
│  │ • AuthRepo     │    │ • AuthApi (real)  │  │                         │   │
│  └────────────────┘    └────────────────┘    └─────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow

```
┌──────────┐     ┌──────────────┐     ┌───────────┐     ┌────────────┐     ┌────────────┐
│   User   │────►│   Composable │────►│ ViewModel │────►│  Use Case  │────►│ Repository │
│  Action  │     │    Screen    │     │           │     │            │     │            │
└──────────┘     └──────────────┘     └─────┬─────┘     └────────────┘     └──────┬─────┘
                                            │                                      │
                                            │ StateFlow                            │
                                            │                                      ▼
                                      ┌─────▼─────┐                         ┌──────────────┐
                                      │ UI State  │◄────────────────────────│ Data Source  │
                                      │ (Updated) │       Result<T>         │ (API/Local)  │
                                      └───────────┘                         └──────────────┘
```

---

## 📦 Module Structure

```
KotlinModularBlueprint/
│
├── 📱 app/                              # Application shell
│   ├── MainActivity.kt                  # Single Activity, Compose host
│   └── ui/theme/                        # App-level Material 3 theme
│
├── 🧱 core/                             # Shared modules — no feature ever depends on another feature
│   ├── common/                          # Base classes & utilities
│   │   ├── base/BaseUseCase.kt          # UseCase, FlowUseCase, NoParamUseCase, NoParamFlowUseCase
│   │   ├── result/Result.kt             # Result<T> sealed interface
│   │   ├── dispatcher/DispatcherModule.kt  # @IoDispatcher / @MainDispatcher qualifiers
│   │   └── extensions/FlowExtensions.kt # asResult(), retry, throttle
│   │
│   ├── network/                         # Retrofit + OkHttp + Moshi
│   │   ├── api/AuthApi.kt               # Real login/refresh/logout endpoints
│   │   ├── interceptor/AuthInterceptor.kt  # Bearer token injection
│   │   ├── model/NetworkResponse.kt     # Success / ApiError / NetworkError / UnknownError
│   │   └── di/NetworkModule.kt          # Retrofit, OkHttp, Moshi providers
│   │
│   ├── database/                        # Room
│   │   ├── BankDatabase.kt
│   │   ├── dao/BankDao.kt
│   │   └── entity/                      # BankEntity, AccountsEntity, CardEntity
│   │
│   ├── security/                        # Secure storage
│   │   ├── cpp/native-keys.cpp          # NDK-backed, XOR-obfuscated key storage
│   │   ├── SecureTokenManager.kt        # Session persistence via EncryptedSharedPreferences
│   │   └── di/SecurityModule.kt
│   │
│   └── ui/                              # Shared Compose components + theme
│       ├── components/                  # BankButton, BankTextField, ErrorView, LoadingIndicator
│       └── theme/                       # Color, Theme, Type
│
├── 🎨 features/                         # 9 fully modularized features
│   ├── auth/
│   │   ├── data/
│   │   │   ├── datasource/AuthRemoteDataSource.kt   # Wraps the real AuthApi
│   │   │   ├── datasource/AuthLocalDataSource.kt    # Wraps SecureTokenManager
│   │   │   ├── mapper/AuthMapper.kt
│   │   │   └── repository/AuthRepositoryImpl.kt
│   │   ├── domain/
│   │   │   ├── model/User.kt
│   │   │   ├── repository/AuthRepository.kt
│   │   │   └── usecase/{LoginUseCase, LogoutUseCase, ObserveAuthStateUseCase}.kt
│   │   ├── presentation/
│   │   │   ├── AuthScreen.kt
│   │   │   ├── AuthViewModel.kt
│   │   │   ├── AuthUiState.kt
│   │   │   └── AuthEvent.kt
│   │   └── di/AuthModule.kt
│   │
│   ├── accounts/  transfers/  beneficiaries/  cards/  payments/
│   ├── profile/  notifications/  dashboard/            # ← same four-package shape as auth
│   │
│   └── transactions/                    # 🚧 still a bare placeholder — next in line
│
└── gradle/libs.versions.toml            # Single source of truth for every dependency version
```

### Module Dependencies

```
                    ┌─────────┐
                    │   app   │   (not yet wired to any module below — see Project Status)
                    └─────────┘

          ┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
          │   auth   │dashboard │ accounts │transfers │beneficiar-│  cards   │ payments │ profile  │notificat-│
          │          │          │          │          │   ies     │          │          │          │  ions    │
          └────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┴────┬─────┘
               │          │          │          │          │          │          │          │          │
               └──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
                                                  │
                              ┌───────────────────┼───────────────────┐
                              ▼                   ▼                   ▼
                        ┌───────────┐      ┌───────────┐        ┌──────────┐
                        │core:common│      │ core:ui   │        │core:security│  ← auth & profile only
                        └───────────┘      └───────────┘        └──────┬───────┘
                                                                        │
                                                                        ▼
                                                                  ┌───────────┐
                                                                  │core:network│  ← auth & profile only
                                                                  └───────────┘
```

Every feature depends on `core:common` and `core:ui`. Only `auth` and `profile` reach into `core:network` and `core:security`, because those are the two features currently backed by a real session (the rest use local in-memory stubs — see [Project Status](#-project-status)).

---

## 🎭 State Management Pattern

### UI State

Every screen exposes one immutable state object:

```kotlin
// AccountUiState.kt
data class AccountUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val errorMessage: String? = null
)
```

### Events

User intents are a sealed interface — one entry point into the ViewModel:

```kotlin
// AccountEvent.kt
sealed interface AccountEvent {
    data object LoadAccounts : AccountEvent
    data class SelectAccount(val accountId: String) : AccountEvent
    data object DismissError : AccountEvent
}
```

### ViewModel

```kotlin
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAccountByIdUseCase: GetAccountByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun onEvent(event: AccountEvent) {
        when (event) {
            AccountEvent.LoadAccounts -> loadAccounts()
            is AccountEvent.SelectAccount -> selectAccount(event.accountId)
            AccountEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            getAccountsUseCase(Unit).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> _uiState.update {
                        it.copy(isLoading = false, accounts = result.data, errorMessage = null)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }
    }
    // ...
}
```

---

## 🔧 Result Type

A generic sealed interface used across every layer to carry success, failure, and loading state without throwing:

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(
        val exception: Throwable,
        val message: String? = exception.message
    ) : Result<Nothing>
    data object Loading : Result<Nothing>
}

// Extension functions
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R>
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T>
inline fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T>
fun <T> Result<T>.getOrNull(): T?
fun <T> Result<T>.getOrDefault(default: T): T
fun <T> Result<T>.getOrThrow(): T
```

## 📐 Base Use Cases

Every use case in every feature extends one of these four `core:common` base classes:

```kotlin
// Single-shot, single value
abstract class UseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {
    suspend operator fun invoke(parameters: P): Result<R> = try {
        withContext(coroutineDispatcher) { Result.Success(execute(parameters)) }
    } catch (e: Exception) {
        Result.Error(e)
    }
    protected abstract suspend fun execute(parameters: P): R
}

// Reactive stream
abstract class FlowUseCase<in P, R>(private val coroutineDispatcher: CoroutineDispatcher) {
    operator fun invoke(parameters: P): Flow<Result<R>> =
        execute(parameters).catch { e -> emit(Result.Error(e as Exception)) }.flowOn(coroutineDispatcher)
    protected abstract fun execute(parameters: P): Flow<Result<R>>
}

// + NoParamUseCase<R> and NoParamFlowUseCase<R> for use cases that take no input
```

---

## 🛠️ Tech Stack

### Core

| Technology | Version | Purpose |
|:-----------|:-------:|:--------|
| **Kotlin** | 2.0.20 | Programming language |
| **Jetpack Compose** | 2025.09.00 (BOM) | Declarative UI toolkit |
| **Material 3** | Latest | Design system |
| **Hilt** | 2.52 | Dependency injection |
| **Coroutines** | 1.9.0 | Asynchronous programming |
| **Navigation Compose** | 2.9.1 | Screen navigation |

### Networking

| Technology | Version | Purpose |
|:-----------|:-------:|:--------|
| **Retrofit** | 2.11.0 | HTTP client |
| **OkHttp** | 4.12.0 | HTTP engine + interceptors |
| **Moshi** | 1.15.0 | JSON serialization |

### Database & Storage

| Technology | Version | Purpose |
|:-----------|:-------:|:--------|
| **Room** | 2.6.1 | Local SQLite database |
| **Paging 3** | 3.3.6 | Efficient data pagination |
| **Security Crypto** | 1.1.0-alpha06 | `EncryptedSharedPreferences` for session storage |

### Background & Media

| Technology | Version | Purpose |
|:-----------|:-------:|:--------|
| **WorkManager** | 2.10.4 | Background task scheduling |
| **Coil** | 2.6.0 | Image loading for Compose |

### Testing

| Technology | Version | Purpose |
|:-----------|:-------:|:--------|
| **JUnit** | 4.13.2 | Unit test framework |
| **Turbine** | 1.2.1 | Flow testing |
| **MockWebServer** | 4.12.0 | API mocking |
| **Truth** | 1.4.5 | Fluent assertions |
| **Mockito** | 5.12.0 | Mocking framework |
| **Espresso** | 3.7.0 | UI testing |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** — a recent stable release with AGP 8.9+ support
- **JDK 17**
- **Android SDK 36** (minimum SDK 24 / Android 7.0+)

### Build & Run

```bash
# Clone the repository
git clone https://github.com/mirza-adil/Kotlin-Modular-Blueprint.git
cd Kotlin-Modular-Blueprint

# Compile a single feature module — fastest way to iterate
./gradlew :features:accounts:compileDebugKotlin

# Build the app shell
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug

# Or simply open in Android Studio and click Run ▶️
```

> **Heads up:** `MainActivity` currently renders a placeholder Compose screen — the app shell isn't wired to any feature module yet (no Hilt bootstrap, no nav graph). Every feature module builds, runs its own tests, and previews independently; see [Project Status](#-project-status) for exactly what's left to connect them.

---

## 🧪 Testing

```bash
# Run every unit test in the project
./gradlew testDebugUnitTest

# Run one module's tests
./gradlew :features:auth:testDebugUnitTest

# Run instrumented tests
./gradlew connectedAndroidTest
```

| Layer | How it's tested |
|:------|:-----------------|
| **Domain** | Plain Kotlin — use cases test with fake repositories, no Android dependency |
| **Data** | Repository/data source tests with `MockWebServer` + `Turbine` for Flows |
| **Presentation** | ViewModel tests with `Turbine` + `kotlinx-coroutines-test` |
| **UI** | Compose UI tests with Espresso |

Testing dependencies (`JUnit`, `Truth`, `Turbine`, `MockWebServer`, `Mockito`, `Espresso`) are wired into every module's `build.gradle.kts` and ready to use. Today, `core:common` has a real example (`ResultTest.kt`); every other module still carries the default template `ExampleUnitTest` / `ExampleInstrumentedTest` — writing real tests per feature is the natural next step once `transactions` is filled in.

---

## 🔒 Security Features

| Feature | Implementation |
|:--------|:----------------|
| **Native key storage** | NDK/C++ (`core/security/src/main/cpp`) with XOR-obfuscated keys, exposed via `NativeKeyProvider` |
| **Encrypted session storage** | `SecureTokenManager` persists access/refresh tokens and basic profile info via `EncryptedSharedPreferences` |
| **Bearer token injection** | `AuthInterceptor` attaches the stored token to every request, skipping endpoints tagged `No-Auth` (login, refresh) |
| **Clean logout** | `AuthRepositoryImpl.logout()` clears the local session even if the remote call fails |

```kotlin
@Inject
lateinit var apiKeyProvider: ApiKeyProvider

// Reads the obfuscated key compiled into the native library
val apiKey = apiKeyProvider.getApiKey()
```

---

## 📱 Planned Navigation

There's no `NavHost` in `app/` yet — this is the target graph once the app shell is wired up:

```
App Navigation Graph
│
├── /auth              →  Sign in (start destination)
├── /dashboard          →  Balance summary, recent activity
├── /accounts           →  Account list → /accounts/{id}
├── /transfers          →  Own-account · local · international
├── /beneficiaries      →  Saved recipients
├── /cards              →  Card list, freeze/unfreeze
├── /payments           →  Bill payments · scheduled payments
├── /profile            →  Personal details · preferences
└── /notifications      →  Inbox · push preferences
```

---

## 🎨 Design Principles

| Principle | Where you'll see it |
|:----------|:----------------------|
| **Single Responsibility** | ViewModels hold state and route events; UseCases hold one business rule each |
| **Open/Closed** | Every `Repository` is an interface in `domain`; swap the `data` implementation without touching callers |
| **Dependency Inversion** | `domain` defines the contract, `data` implements it, `di` binds them with Hilt `@Binds` |
| **Interface Segregation** | Small, focused data source interfaces (`AccountRemoteDataSource` vs. `AccountLocalDataSource`) rather than one god-repository |

### Domain Model Example

```kotlin
data class Account(
    val id: String,
    val accountNumber: String,
    val accountHolderName: String,
    val accountType: AccountType,
    val balance: Double,
    val currency: String,
    val status: AccountStatus
)

enum class AccountType { SAVINGS, CURRENT, FIXED_DEPOSIT }
enum class AccountStatus { ACTIVE, INACTIVE, FROZEN }
```

---

## 📊 Project Status

**What's real today:**
- 9 of 10 feature modules fully modularized (`data` / `domain` / `presentation` / `di`), each compiling and passing KSP/Hilt codegen independently
- `auth` and `profile` back onto a real `AuthApi` (Retrofit) and `SecureTokenManager` (EncryptedSharedPreferences) — a genuine, working session layer
- Native (NDK) key obfuscation in `core:security`
- Shared `core:common` (Result, UseCase, dispatchers) and `core:ui` (Compose theme + components) used consistently across every feature

**What's still ahead:**
- [ ] `features:transactions` — still a bare placeholder; every other feature is its template
- [ ] Wire the `app` module: add a `@HiltAndroidApp` Application class, `@AndroidEntryPoint` on `MainActivity`, and `implementation(project(":features:..."))` dependencies
- [ ] Build the `NavHost` connecting all nine screens
- [ ] Point `core:network`'s `BASE_URL` at a real backend — most feature remote data sources are stubs marked `TODO`, returning local/in-memory data until a backend exists
- [ ] Replace template `ExampleUnitTest`/`ExampleInstrumentedTest` files with real coverage per feature

If you're extending this project, `features:transactions` is the natural next module to build — every other feature is a working template to copy.

---

## 📚 Documentation

| Document | Description |
|:---------|:-------------|
| [architecture.md](docs/architecture.md) | Clean Architecture deep dive — layer-by-layer walkthrough with real code from `features:accounts` |
| [modules.md](docs/modules.md) | Module-by-module breakdown — responsibilities, real `build.gradle.kts` dependency blocks, and the actual dependency graph for all 16 modules |
| [networking.md](docs/networking.md) | Networking layer deep dive — Retrofit/OkHttp/Moshi stack, `NetworkResponse`, interceptors, error handling, offline-first, and a walkthrough for wiring up a real endpoint |
| [performance.md](docs/performance.md) | Performance optimization guide — Compose stability, state collection, memory, coroutines, database, networking, and background work, with an honest already-applied-vs-recommended breakdown |
| [pr_review_guide.md](docs/pr_review_guide.md) | 7-step PR review framework, feedback tone/prefixes, a review checklist template, and an honest look at what's actually enforced today (no ktlint/detekt/CI configured yet) |
| [security.md](docs/security.md) | Security deep dive — native NDK key storage, encrypted session storage, token management, and network security. Documents two real bugs found and fixed while verifying this doc: a JNI package mismatch and a missing CMake build registration |
| [testing.md](docs/testing.md) | Testing strategy — test types, tools, the one real test suite in the repo (`core:common`), and what full coverage would look like for every layer |
| [compose_guidelines.md](docs/compose_guidelines.md) | Jetpack Compose conventions — theming, components, state hoisting, screen structure, navigation, and an honest adoption-status table |
| [interview_questions.md](docs/interview_questions.md) | Senior Android interview Q&A grounded in this repo's real code — Clean Architecture, Coroutines/Flow, Compose, networking, security, testing, system design, and banking-specific domain concerns |

---

## 🧑‍💻 Contributing

1. **Fork** the repository
2. **Create** a feature branch
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Commit** your changes
   ```bash
   git commit -m 'Add amazing feature'
   ```
4. **Push** to the branch and **open a Pull Request**

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Match the existing feature module shape (`data`/`domain`/`presentation`/`di`) for any new feature
- Add tests alongside new business logic rather than leaving the template test in place

---

## 📄 License

No license file is currently included in this repository. Treat the code as reference material, and check with the repository owner before reusing it elsewhere.

---

## 🙏 Acknowledgments

- [Google Android Team](https://developer.android.com/) for Jetpack Compose, Hilt integration, and the official architecture guidance this project follows
- [Material Design](https://material.io/) for the design system
- The Android community for the Clean Architecture + multi-module patterns this blueprint is built on

---

## 👤 Author

**Mirza Adil**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-mirzaadil-0A66C2.svg?logo=linkedin&logoColor=white)](https://www.linkedin.com/in/mirzaadil/)

---

<div align="center">

**Built as a reference for modular, testable Android banking apps.**

Made with ❤️ by [Mirza Adil](https://www.linkedin.com/in/mirzaadil/)

</div>