# 🏗️ Architecture Overview

## Introduction

BankApp follows **Clean Architecture**, applied identically across every feature module. Each feature is split into `presentation`, `domain`, and `data` packages, plus a `di` package that wires them together with Hilt. This gives the codebase:

- **Separation of concerns** — a `Screen` never imports Retrofit; a `Repository` never imports Compose
- **Testability** — `domain` is plain Kotlin with no Android dependency, so business rules test in plain JVM unit tests
- **Maintainability** — every feature has the same shape, so there's one pattern to learn, not nine
- **Scalability** — features are independent Gradle modules, so the codebase grows by adding modules, not by growing existing ones

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │   Screens   │  │  ViewModels  │  │      UI State       │ │
│  │  (Compose)  │  │  (StateFlow) │  │   (Immutable)       │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                           │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │  Use Cases  │  │    Models    │  │  Repository         │ │
│  │             │  │              │  │  Interfaces         │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                            │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────┐ │
│  │ Repository  │  │ Data Sources │  │     Mappers         │ │
│  │   Impl      │  │(Remote/Local)│  │  (DTO ↔ Domain)     │ │
│  └─────────────┘  └──────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

The dependency arrow only ever points **downward**. `domain` never imports from `data` or `presentation` — it defines interfaces that `data` implements and `presentation` consumes through use cases.

## Layer Details

The examples below are the real `features:accounts` module, unedited.

### Presentation Layer

Built with **Jetpack Compose**, driven by a `StateFlow<UiState>` and a sealed `Event` type:

```kotlin
// AccountUiState.kt — immutable state the screen renders
data class AccountUiState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val errorMessage: String? = null
)

// AccountEvent.kt — the only way the screen talks back to the ViewModel
sealed interface AccountEvent {
    data object LoadAccounts : AccountEvent
    data class SelectAccount(val accountId: String) : AccountEvent
    data object DismissError : AccountEvent
}

// AccountViewModel.kt — state holder, orchestrates use cases
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
    // ...
}

// AccountScreen.kt — Composable, reads state, dispatches events
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    onAccountClick: (Account) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.accounts.isEmpty() ->
            FullScreenLoading(message = "Loading accounts...")

        uiState.errorMessage != null && uiState.accounts.isEmpty() ->
            ErrorView(
                message = uiState.errorMessage.orEmpty(),
                onRetry = { viewModel.onEvent(AccountEvent.LoadAccounts) }
            )

        else -> LazyColumn {
            items(uiState.accounts, key = { it.id }) { account ->
                AccountListItem(account = account, onClick = { onAccountClick(account) })
            }
        }
    }
}
```

### Domain Layer

Pure Kotlin — no `android.*` import anywhere in this package:

```kotlin
// Account.kt — domain model
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

// AccountRepository.kt — the contract; data implements this, domain/presentation depend on it
interface AccountRepository {
    fun getAccounts(): Flow<Result<List<Account>>>
    suspend fun getAccountById(accountId: String): Result<Account>
    suspend fun refreshAccounts(): Result<Unit>
}

// GetAccountsUseCase.kt — one business action, one class
class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Account>>(dispatcher) {

    override fun execute(parameters: Unit): Flow<Result<List<Account>>> =
        accountRepository.getAccounts()
}
```

Every use case extends one of four base classes in `core:common` — `UseCase`, `FlowUseCase`, `NoParamUseCase`, or `NoParamFlowUseCase` — so error handling and dispatcher switching are written once, not once per feature.

### Data Layer

Implements the `domain` repository interface, combining a remote and a local data source:

```kotlin
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val remoteDataSource: AccountRemoteDataSource,
    private val localDataSource: AccountLocalDataSource
) : AccountRepository {

    override fun getAccounts(): Flow<Result<List<Account>>> =
        localDataSource.observeAccounts()
            .map { it.toDomain() }
            .asResult()

    override suspend fun getAccountById(accountId: String): Result<Account> {
        return try {
            val dto = localDataSource.getAccountById(accountId)
                ?: remoteDataSource.getAccountById(accountId)
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshAccounts(): Result<Unit> {
        return try {
            val accounts = remoteDataSource.getAccounts()
            localDataSource.cacheAccounts(accounts)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

`getAccounts()` reads from the local cache and wraps it in `Result` via the shared `asResult()` extension (`core:common`), so the UI always sees `Loading → Success/Error` regardless of which feature it's watching. `getAccountById` prefers the local cache and falls back to remote — the same offline-first shape used across every feature, whether the remote data source is a real endpoint (`auth`, `profile`) or a stub returning mock data pending a backend (everything else — see the main [README](../README.md#-project-status)).

## Data Flow

```
User Action → Screen → ViewModel → UseCase → Repository → DataSource
     │           │          │          │           │            │
   Event      onEvent   Updates    Business      Data       Local cache /
              (intent)   State      Logic     Operations     Remote API
```

Every step in that chain only knows about its immediate neighbor. Nothing in `presentation` or `domain` ever imports `Retrofit`, `Room`, or a DTO type directly — those live entirely inside `data`.

## Dependency Injection

Each feature module has its own `di/<Feature>Module.kt`, a Hilt `@Module` installed in `SingletonComponent` that binds interfaces to implementations:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindAccountRemoteDataSource(impl: AccountRemoteDataSourceImpl): AccountRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAccountLocalDataSource(impl: AccountLocalDataSourceImpl): AccountLocalDataSource
}
```

Hilt discovers these modules automatically at compile time — nothing needs to be registered by hand outside the feature module itself.

## SOLID Principles Applied

### Single Responsibility
- Each class has one reason to change
- ViewModels handle only UI state and event routing
- UseCases contain exactly one business action

### Open/Closed
- `Repository` interfaces let the data source swap (mock → real API) without touching `domain` or `presentation`
- New features are added as new modules, not by growing an existing one

### Liskov Substitution
- Any `AccountRemoteDataSource` implementation can replace another — the stub used today and a real Retrofit-backed one tomorrow are interchangeable

### Interface Segregation
- Small, single-purpose interfaces — `AccountRemoteDataSource` and `AccountLocalDataSource` are separate contracts, not one combined repository interface

### Dependency Inversion
- `domain` defines the abstraction (`AccountRepository`); `data` depends on and implements it
- `presentation` depends only on `domain`, never on `data` directly

---

See the main [README](../README.md) for the full module structure, tech stack, and current project status.
