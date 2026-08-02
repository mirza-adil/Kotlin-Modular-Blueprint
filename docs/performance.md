# ⚡ Performance Optimization

## Overview

BankApp targets:

- **Smooth UI (60fps)** — stable Compose state, keyed lists
- **Correct dispatcher usage** — never block the main thread on I/O
- **Minimal memory footprint** — no leaked `Context`, bounded caches
- **Fast, resumable background work** — for the things a banking app can't do synchronously (scheduled payments)

Like the other docs in `/docs`, this one is explicit about which patterns are **already applied** across the codebase versus **recommended but not yet adopted** — see the practices marked ⚠️ below, and cross-reference [compose_guidelines.md](compose_guidelines.md#-current-adoption-status) for the UI-layer adoption table this extends.

## Compose Performance

### Keyed Lists — ✅ already applied everywhere

Every `LazyColumn` in every feature screen already uses a stable `key`, not just a raw `items(list) { }`:

```kotlin
// AccountScreen.kt — real code
LazyColumn {
    items(uiState.accounts, key = { it.id }) { account ->
        AccountListItem(account = account, onClick = { onAccountClick(account) })
    }
}
```

Without `key = { it.id }`, Compose diffs list items by position — reordering, inserting, or removing an account would cause every item after the change point to be treated as "changed" and recompose. With a stable key, only the items that actually changed do.

### Stable Types — ✅ already applied, no annotation needed

```kotlin
// Every domain model and every UiState in this repo is shaped like this:
data class Account(
    val id: String,
    val accountNumber: String,
    val balance: Double,
    // every property is `val`, every type is itself stable (String, Double, enum, List<stable>)
)
```

Because every property is `val` and every type is itself stable, the Compose compiler infers these as **stable** automatically — none of them need an explicit `@Immutable` or `@Stable` annotation. Reach for `@Stable` only if a class holds a `mutableStateOf` var directly (none currently do) or a type the compiler can't see into (e.g. a Java class from a third-party library).

### `derivedStateOf` — ⚠️ not used anywhere yet

```kotlin
// Recommended pattern — none of the current screens filter/derive from a list yet,
// but this is exactly what features:beneficiaries would need once it grows a search field:
val filteredBeneficiaries by remember(query, uiState.beneficiaries) {
    derivedStateOf {
        uiState.beneficiaries.filter { it.name.contains(query, ignoreCase = true) }
    }
}
```

Every current screen renders its full list with no client-side filtering, so there's been nothing to derive yet — this becomes relevant the moment any feature adds a search/filter field.

## State Management

### `StateFlow.update {}` — ✅ already applied everywhere

Every `ViewModel` in this codebase already uses the atomic `update {}` form, never a manual `.value = _uiState.value.copy(...)` (which is a read-modify-write race under concurrent updates):

```kotlin
// AccountViewModel.kt — real code
is Result.Success -> _uiState.update {
    it.copy(isLoading = false, accounts = result.data, errorMessage = null)
}
```

### Lifecycle-aware collection — ⚠️ not applied yet

```kotlin
// What every screen currently does:
val uiState by viewModel.uiState.collectAsState()

// Recommended: stops collecting when the screen is STOPPED, resumes on RESUMED —
// avoids updating state (and doing the work behind it) for a screen the user can't see
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

`lifecycle-runtime-compose` (which provides `collectAsStateWithLifecycle`) is already on the classpath via `core:ui`'s `api(libs.lifecycle.runtime.compose)` — every screen using plain `collectAsState()` today is a drop-in swap, not a dependency change. This is the single highest-value, lowest-effort item in this doc.

## Memory Management

### Avoiding leaked `Context` — ✅ already applied everywhere it matters

Every class in this codebase that needs a `Context` takes `@ApplicationContext`, never the injected Activity/Fragment context, so nothing outlives an Activity by accident:

```kotlin
// EncryptedPreferencesManager.kt, NativeKeyProvider.kt, DatabaseModule.provideDatabase() — all real
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
)
```

### Image Loading — ⚠️ Coil is on the classpath, but nothing actually renders an image yet

`core:ui` exposes `coil-compose` as an `api` dependency, and several domain models already carry an image URL field (`User.avatarUrl`, `UserProfile.avatarUrl`) — but no feature screen currently renders `AsyncImage`. The only reference to Coil/`AsyncImage` anywhere in the codebase is inside `core/ui/components/RecipeCard.kt`, the unused leftover component flagged in [modules.md](modules.md#-core-ui). When a screen does need one (a profile avatar, a card art image), the pattern to reach for:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(userProfile.avatarUrl)
        .crossfade(true)
        .memoryCacheKey(userProfile.userId)
        .diskCacheKey(userProfile.userId)
        .build(),
    contentDescription = userProfile.fullName
)
```

## Coroutine Optimization

### Dispatcher usage — ✅ already applied everywhere, enforced by the base classes

Every use case in every feature extends `core:common`'s `UseCase`/`FlowUseCase`, which take a `CoroutineDispatcher` in their constructor and switch onto it internally — there's no way to write a use case in this codebase that *forgets* to get off the main thread for I/O, because the base class does it for you:

```kotlin
// GetAccountsUseCase.kt — real code; @IoDispatcher is resolved by Hilt via DispatcherModule
class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Account>>(dispatcher) {
    override fun execute(parameters: Unit): Flow<Result<List<Account>>> =
        accountRepository.getAccounts()
}

// FlowUseCase itself does the dispatching — every subclass gets this for free:
operator fun invoke(parameters: P): Flow<Result<R>> =
    execute(parameters).catch { e -> emit(Result.Error(e as Exception)) }.flowOn(coroutineDispatcher)
```

### Cancellation / debounce — ⚠️ not needed yet, no search field exists

```kotlin
// Recommended pattern for the day a feature adds search (see D.2 in interview_questions.md):
private var searchJob: Job? = null

fun onSearchQueryChange(query: String) {
    searchJob?.cancel()   // cancel the in-flight search before starting a new one
    searchJob = viewModelScope.launch {
        delay(300)
        searchBeneficiaries(query)
    }
}
```

Worth calling out separately from `derivedStateOf` above: that pattern is for filtering data *already in memory*; this one is for cancelling an in-flight *use case call* (e.g. a real network search) — don't reach for `Job.cancel()` on a purely local `List.filter`.

## Database Optimization

### Efficient queries & indices — ⚠️ recommended, not yet needed

`core:database`'s entities (`BankEntity`, `AccountsEntity`, `CardEntity`) currently declare no `@Entity(indices = [...])` — reasonable today, since **no feature module actually queries `core:database` yet** (every feature's local cache is an in-memory `MutableStateFlow`, see [modules.md](modules.md#-core-database)). This becomes relevant the moment a feature switches from in-memory caching to Room:

```kotlin
// Recommended once a feature's LocalDataSource moves to Room:
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["account_id"]), Index(value = ["created_at"])]
)
data class TransactionEntity(/* ... */)

@Dao
interface TransactionDao {
    // Select only the columns a list screen actually renders, not SELECT *
    @Query("SELECT id, amount, merchant_name, created_at FROM transactions WHERE account_id = :accountId")
    fun getTransactionPreviews(accountId: String): Flow<List<TransactionPreview>>
}
```

`features:transactions` — the one feature module still unbuilt — is the most likely first real consumer of `core:database`, given it's the one screen genuinely expected to hold more rows than fit comfortably in memory.

## Network Optimization

### HTTP response caching — ⚠️ not configured

`NetworkModule.provideOkHttpClient()` sets timeouts and an interceptor chain (see [networking.md](networking.md)) but doesn't currently attach an OkHttp response `Cache`:

```kotlin
// Not present in NetworkModule today — worth adding once real GET endpoints exist:
.cache(Cache(context.cacheDir, 10L * 1024 * 1024)) // 10MB
```
Low priority right now since only `AuthApi`'s `POST` endpoints are real, and `POST` responses aren't cacheable by HTTP semantics regardless — this matters once a real `GET /accounts`-style endpoint replaces `AccountRemoteDataSourceImpl`'s stub.

## Battery Optimization

### WorkManager for background work — ⚠️ dependency present, no `Worker` implemented yet

`WorkManager` and `hilt-work` are already dependencies of `app/build.gradle.kts`, and `features:payments`'s `ScheduledPayment` domain model already has exactly the shape background execution needs (`frequency`, `nextRunDate`, `isActive`) — but no `CoroutineWorker` exists in the codebase yet to actually run them:

```kotlin
// Recommended shape, using the real ScheduledPayment/PaymentRepository this repo already has:
class ScheduledPaymentWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val paymentRepository: PaymentRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (paymentRepository.refreshPayments()) {
            is com.mirza.common.result.Result.Success -> Result.success()
            else -> Result.retry()
        }
    }
}

val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "scheduled_payments_sync",
        ExistingPeriodicWorkPolicy.KEEP,
        PeriodicWorkRequestBuilder<ScheduledPaymentWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
    )
```

Note the `NetworkType.CONNECTED` constraint rather than `UNMETERED` — unlike a media-sync job, a scheduled bill payment shouldn't silently wait for Wi-Fi if it's due today.

## Performance Monitoring

### Compose compiler metrics — ⚠️ not configured

None of this project's `build.gradle.kts` files currently emit Compose compiler stability/recomposition reports. Adding it to `core:ui` (and any feature module as needed) is a one-block addition:

```kotlin
// Not present today — add to a module's build.gradle.kts to start generating reports:
composeCompiler {
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_reports")
}
```

The generated report is what would actually confirm the "stable by default" claim earlier in this doc — worth turning on before `features:transactions` (the module most likely to render a genuinely long list) is built out.

---

See [architecture.md](architecture.md) for the layering these optimizations sit inside, [compose_guidelines.md](compose_guidelines.md) for the broader UI conventions, and the main [README](../README.md#-project-status) for what's implemented versus planned across the whole project.
