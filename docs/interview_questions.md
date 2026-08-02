# 🧠 Senior Android Developer Interview Questions

## Based on BankApp Architecture

Every answer below is grounded in this repository's actual code — file names, class names, and behavior are real, not illustrative. Where the codebase falls short of the "right" answer (a known gap, a simplification, an unimplemented control), that's called out explicitly rather than glossed over, consistent with [architecture.md](architecture.md) and [compose_guidelines.md](compose_guidelines.md).

---

## 🟦 A. OOP & Clean Architecture

### 1. SOLID Principles

**Q: Explain each SOLID principle and where it's applied in this project.**

**A:**
- **S – Single Responsibility:** Each use case does one thing — `LoginUseCase` only logs in, `GetAccountsUseCase` only reads accounts, `ToggleCardFreezeUseCase` only flips a card's frozen state
- **O – Open/Closed:** `AccountRepository` is an interface in `domain`; swapping `AccountRepositoryImpl`'s data source (mock → real API) never requires touching a `ViewModel` or `UseCase`
- **L – Liskov Substitution:** Any `AccountRemoteDataSource` implementation is interchangeable — the current stub (`AccountRemoteDataSourceImpl`, returns mock data) can be replaced by a real Retrofit-backed one without changing `AccountRepositoryImpl`'s logic
- **I – Interface Segregation:** `TokenProvider` (`core:network`) exposes exactly three methods — `getAccessToken()`, `getRefreshToken()`, `clearTokens()` — while `SecureTokenManager` (`core:security`) implements it *and* adds a dozen more (`saveTokens`, `isTokenExpired`, `saveUserInfo`, ...). Code that only needs to read a token (like `AuthInterceptor`) depends on the narrow `TokenProvider`, not the whole manager
- **D – Dependency Inversion:** `LoginUseCase` depends on the `AuthRepository` interface (defined in `domain`), never on `AuthRepositoryImpl` (defined in `data`). Hilt's `@Binds` in `AuthModule` is what actually wires the concrete class in at runtime

### 2. Layer Separation

**Q: Why separate `domain`, `data`, and `presentation` into different packages instead of one flat feature package?**

**A:**
- **Testability:** `domain` has zero Android imports, so `GetAccountsUseCase` tests as a plain JVM unit test — no emulator, no Robolectric
- **Maintainability:** `AuthScreen` doesn't know `AuthApi` exists; a backend contract change only touches `data`
- **Consistency at scale:** every one of the 9 built-out feature modules (`accounts`, `auth`, `beneficiaries`, `cards`, `dashboard`, `notifications`, `payments`, `profile`, `transfers`) has the exact same four-package shape — a new engineer only has to learn it once
- **Independent modules:** no `features:*` module depends on another `features:*` module — see the [module dependency diagram](../README.md#module-dependencies) in the README

### 3. Repository Pattern

**Q: What's the actual difference between a `Repository` and a `DataSource` in this codebase?**

**A:**
- **Repository** (`AccountRepositoryImpl`) — coordinates between sources and owns the caching decision. It decides "check local first, fall back to remote" and maps DTOs to domain models
- **DataSource** (`AccountRemoteDataSource`, `AccountLocalDataSource`) — talks to exactly one source (a stub/API, or an in-memory cache), knows nothing about the other, and contains no business logic

```kotlin
override suspend fun getAccountById(accountId: String): Result<Account> {
    return try {
        val dto = localDataSource.getAccountById(accountId)
            ?: remoteDataSource.getAccountById(accountId)   // ← the coordination decision lives here
        Result.Success(dto.toDomain())
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

---

## 🟥 B. Kotlin + Coroutines + Flow

### 1. StateFlow vs. one-shot events

**Q: This project only uses `StateFlow` — no `SharedFlow`/`Channel` for events. Is that a problem?**

**A:** Every `ViewModel` exposes a single `StateFlow<UiState>`; navigation triggers (e.g. "login succeeded, go to dashboard") are modeled as a boolean *inside* that state, observed with `LaunchedEffect`:

```kotlin
// AuthUiState.kt
data class AuthUiState(/* ... */ val isLoggedIn: Boolean = false)

// AuthScreen.kt
LaunchedEffect(uiState.isLoggedIn) {
    if (uiState.isLoggedIn) onLoginSuccess()
}
```

This works because `isLoggedIn` is idempotent — re-observing "still logged in" is harmless. It would **not** be safe for a true one-shot event (e.g. "show this exact error snackbar once"), because `StateFlow` replays its latest value to every new collector. For that case, a `Channel`/`SharedFlow` with no replay is the correct tool — this codebase just hasn't needed one yet, since every screen currently surfaces errors as inline state (`uiState.errorMessage`), not a transient snackbar.

### 2. Why avoid `GlobalScope`?

**A:**
- No lifecycle awareness — a coroutine launched in `GlobalScope` from a `ViewModel` outlives the `ViewModel`
- Cannot be cancelled when the screen is left
- Every `ViewModel` in this codebase uses `viewModelScope.launch { }` instead — see any `onEvent()` handler, e.g. `AccountViewModel.loadAccounts()`

### 3. `launch`, `async`, and `withContext` — where each shows up here

**A:**
```kotlin
// UseCase.kt (core:common) — withContext: switch dispatcher, suspend until the block returns
suspend operator fun invoke(parameters: P): Result<R> = try {
    withContext(coroutineDispatcher) { Result.Success(execute(parameters)) }
} catch (e: Exception) {
    Result.Error(e)
}

// FlowUseCase.kt (core:common) — flowOn: the Flow equivalent of withContext
operator fun invoke(parameters: P): Flow<Result<R>> =
    execute(parameters).catch { e -> emit(Result.Error(e as Exception)) }.flowOn(coroutineDispatcher)

// AccountViewModel.kt — launch: fire-and-forget from the ViewModel's own scope
viewModelScope.launch { getAccountsUseCase(Unit).collect { /* update state */ } }
```
`async`/`await` doesn't appear anywhere in this codebase yet — nothing currently needs two independent results joined in parallel (e.g. fetching accounts and cards concurrently for a combined screen would be the natural place to introduce it).

---

## 🟩 C. Jetpack Compose

### 1. Recomposition

**Q: What causes unnecessary recomposition, and how does this codebase avoid it?**

**A:**
- **Causes:** unstable parameters, lambdas recreated every recomposition, reading fast-changing state too high in the tree
- **This codebase's mitigation:** every `UiState` is a `data class` of `val`s (see `AccountUiState`, `AuthUiState`, `CardUiState`, ...) — Compose's compiler treats these as **stable**, so a screen only recomposes when the state object it actually reads changes

### 2. `remember` vs. `derivedStateOf`

**A:**
```kotlin
// remember: cache a value across recompositions, recompute only if keys change
val viewModel: AccountViewModel = hiltViewModel()

// derivedStateOf: recompute only when the *inputs* change, not every recomposition
val hasFrozenCards by remember(uiState.cards) {
    derivedStateOf { uiState.cards.any { it.status == CardStatus.FROZEN } }
}
```
Not currently used in this codebase — every screen derives simple booleans inline (e.g. `uiState.isLoading && uiState.accounts.isEmpty()`) since none of the current lists are large or filtered enough to need memoized derivation yet.

### 3. What makes a class "stable" in Compose?

**A:**
- All properties are `val`, and their types are themselves stable (primitives, `String`, or other stable data classes)
- Every domain model in this repo (`Account`, `Card`, `Transfer`, `Payment`, ...) and every `UiState` qualifies automatically — they're all immutable `data class`es with no `var`, no mutable collections

---

## 🟧 D. Data Structures & Algorithms

### 1. Paging

**Q: `core:database`'s `BankDao` declares `getRecipesPagingSource(): PagingSource<Int, BankEntity>`, and `Paging 3` is on the classpath. Explain the complexity, and is it actually used?**

**A:** Loading one page is `O(k)` for page size `k`; total network/DB calls for `n` items is `O(n/k)`; memory stays bounded at `O(k × windowSize)` because Paging only keeps a limited window of pages loaded. **Honestly:** the `PagingSource` is declared but no feature screen in this repo currently consumes it through `Paging Compose`'s `LazyPagingItems` — every list screen (`AccountScreen`, `CardScreen`, ...) loads its full list via a plain `StateFlow<List<T>>`. It's wired up and ready, but not adopted yet — transaction history is the natural first candidate once `features:transactions` is built out.

### 2. Debounce (general implementation, not currently in this codebase)

**Q: Implement debounce for a search field — e.g. searching beneficiaries by name.**

**A:**
```kotlin
private var searchJob: Job? = null

fun onSearchQueryChange(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(300)
        searchBeneficiaries(query)
    }
}
```
No feature currently has a search field, but `features:beneficiaries` is the obvious place this would land.

### 3. LRU Cache

**Q: How is image caching handled, and how would you implement an LRU cache yourself?**

**A:** `core:ui` pulls in **Coil**, which already does LRU memory + disk caching for images out of the box — nothing custom is needed for the common case (e.g. a beneficiary's avatar, a card art image). To implement one by hand:
```kotlin
class LRUCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
    override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean = size > maxSize
}
// Or reach for android.util.LruCache directly.
```

---

## 🟪 E. Networking

### 1. The Retrofit stack

**Q: Walk through how Retrofit, OkHttp, and Moshi work together in `core:network`.**

**A:**
- **Retrofit** — defines `AuthApi` as an interface with `@POST("auth/login")` etc.; `NetworkModule.provideAuthApi()` builds the real instance via `retrofit.create(AuthApi::class.java)`
- **OkHttp** — the actual HTTP client; `NetworkModule.provideOkHttpClient()` chains `AuthInterceptor` (injects the bearer token), `NetworkDelayInterceptor`, and a logging interceptor
- **Moshi** — (de)serializes `LoginRequest`/`LoginResponse`/`UserDto` to/from JSON via `MoshiConverterFactory`

### 2. Why a `NetworkResponse` sealed class instead of throwing?

**A:** `core:network`'s `NetworkResponse<T>` (`Success` / `ApiError` / `NetworkError` / `UnknownError`) forces the caller into an exhaustive `when` — no forgetting to catch a `SocketTimeoutException` versus a 500. `AuthRemoteDataSourceImpl` unwraps it into a thrown exception at the edge of `data`, so everything above that layer just sees `Result.Error` — the app has **two** result-wrapping types on purpose: `NetworkResponse` at the transport boundary, `Result` everywhere else (`core:common`). Conflating them would leak HTTP-specific concepts (`ApiError.code`) into `domain`.

### 3. Offline-first — is this app actually offline-first?

**A: Partially, and it's honest about where.** `features:dashboard`'s repository is the clearest real example:
```kotlin
override fun getDashboardSummary(): Flow<Result<DashboardSummary>> =
    localDataSource.observeDashboardSummary()
        .onStart { refreshDashboardSummary() }   // kick off a refresh...
        .filterNotNull()                          // ...but emit from cache immediately if present
        .map { it.toDomain() }
        .asResult()
```
Cached data emits immediately (fast perceived load), a refresh fires in the background, and the `StateFlow` naturally picks up the update. `features:accounts`, by contrast, only reads from `AccountLocalDataSource` and has to be told to `refreshAccounts()` explicitly — it doesn't auto-refresh on subscribe. Neither pattern is "wrong"; they're different tradeoffs, and this codebase currently has both, unintentionally inconsistent. Standardizing on the dashboard's `onStart { refresh() }` pattern across every feature is a reasonable interview answer for "what would you fix first."

---

## 🟫 F. Security

### 1. Why `EncryptedSharedPreferences` for session storage?

**A:** `SecureTokenManager` (`core:security`) persists the access token, refresh token, expiry timestamp, and basic profile info via `EncryptedPreferencesManager`, which wraps `androidx.security.crypto.EncryptedSharedPreferences`:
- Values are encrypted at rest with AES-256-GCM; keys are AES-256-SIV
- The `MasterKey` lives in the Android Keystore, not in the app's storage
- A rooted device reading the raw XML file still can't read the token without the Keystore-backed key

### 2. Certificate pinning — is it implemented?

**A: No, and the code says so.** `NetworkModule.provideOkHttpClient()` has it stubbed and commented out:
```kotlin
.retryOnConnectionFailure(true)
// Certificate pinning can be added here for production
// .certificatePinner(certificatePinner)
```
For a real banking app this is a real gap, not a nitpick — without it, a compromised CA (or a device with a malicious user-installed CA cert) can MITM every request, including login. The fix is exactly what the comment says: build a `CertificatePinner` with the backend's public key hash and attach it before this ships past a prototype.

### 3. Native (NDK) key storage — why go this far?

**A:** `core/security/src/main/cpp/native-keys.cpp` stores API keys in a compiled native library rather than a Kotlin `const val` or `BuildConfig` field, exposed via `NativeKeyProvider` → `ApiKeyProvider`:
- A string constant in a Kotlin/Java class is trivially visible in a decompiled APK (`jadx`, `apktool`)
- A key compiled into native ARM/x86 code, additionally XOR-obfuscated, requires disassembly rather than decompilation — a meaningfully higher bar, though never a substitute for the key not being embedded client-side at all in a real production system

---

## 🟨 G. Testing

### 1. Unit test vs. instrumented test

**A:**
- **Unit** (`src/test`) — pure JVM, fast, no device — this is where `GetAccountsUseCase`, `AccountViewModel`, and `AccountRepositoryImpl` belong
- **Instrumented** (`src/androidTest`) — runs on a device/emulator, needed for anything touching Room, Compose UI trees, or the Android framework directly

**Honestly:** every module here has both directories scaffolded (`build.gradle.kts` wires in JUnit, Truth, Turbine, MockWebServer, Mockito, and Espresso already), but only `core:common`'s `ResultTest.kt` has real coverage today — every feature module still carries the default template `ExampleUnitTest`/`ExampleInstrumentedTest`. That's the most honest answer to "how well is this tested": the scaffolding is production-grade, the coverage isn't there yet.

### 2. How would you test `AccountViewModel`?

**A:**
```kotlin
@Test
fun `loading accounts updates state to success`() = runTest {
    val fakeRepository = FakeAccountRepository(
        accounts = listOf(previewAccount)
    )
    val viewModel = AccountViewModel(
        getAccountsUseCase = GetAccountsUseCase(fakeRepository, testDispatcher),
        getAccountByIdUseCase = GetAccountByIdUseCase(fakeRepository, testDispatcher)
    )

    viewModel.onEvent(AccountEvent.LoadAccounts)
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.isLoading).isFalse()
    assertThat(viewModel.uiState.value.accounts).containsExactly(previewAccount)
}
```
A `FakeAccountRepository` implementing the `domain` interface is all that's needed — this is exactly what Liskov substitution (section A.1) buys you for testing.

---

## 🟧 H. System Design

### 1. Scaling this app to millions of users

**A (banking-specific, not generic advice):**
- **Paginate transaction history** — the one place unbounded lists are inevitable; wire up the already-declared `PagingSource` (see D.1)
- **Strong consistency on balance reads** — unlike a social feed, a stale account balance is a support ticket, not a UX nitpick; caching balances needs a short, explicit TTL, not "cache forever, refresh eventually"
- **Idempotent money movement** — every transfer/payment request needs a client-generated idempotency key, so a retried request (flaky network, user double-tapping "Send") can't double-charge. `CreateTransferUseCase.Params` and `PayBillUseCase.Params` don't carry one today — that's a real gap, not a stylistic choice
- **WorkManager for scheduled payments** — `features:payments`'s `ScheduledPayment` model already has the shape for this (`frequency`, `nextRunDate`); executing them reliably in the background is exactly what `WorkManager` (already a dependency) is for
- **Audit logging, not just analytics** — a banking app needs an immutable record of who did what, when, independent of crash/analytics tooling

### 2. Offline-first design — done right for money, not just recipes

**A:** The reference pattern for a content app ("last-write-wins or merge on conflict") is actively **wrong** for financial data. The correct shape:
1. **Single source of truth:** the *server* is authoritative for balances and transaction state — local Room/cache is a read-through view, never the source of truth for money
2. **Sync strategy:** `WorkManager` with network constraints for anything queued while offline (e.g. a payment submitted with no connectivity)
3. **Conflict resolution:** there is no client-side merge for money — a queued transfer either succeeds, fails, or is still pending server-side; the client never guesses
4. **Idempotency, not optimistic writes:** every mutating request carries a client-generated ID so a retry after a timeout doesn't become a duplicate transfer
5. **UI:** show the account's last-known balance immediately (from cache) clearly labeled as "as of [time]" if a refresh is in flight or failed — never silently show stale data as current

---

## 🟦 I. Domain-Specific: Banking Concerns

These are gaps and design decisions specific to *this being a banking app* — the kind of thing a generic "swap the model names" architecture sample wouldn't surface, and the reason this section exists separately from the general Android questions above.

### 1. Money as `Double` — a real problem in this codebase

**Q: `Account.balance`, `Transfer.amount`, and `Payment.amount` are all `Double`. Why is that a problem, and what's the fix?**

**A:** `Double` is binary floating point — it cannot represent most decimal fractions exactly (`0.1 + 0.2 != 0.3`). For UI display that's invisible; for **accumulated arithmetic on money**, it's a real correctness bug waiting to happen. The standard fixes:
- Use `BigDecimal` for any arithmetic, or
- Store money as an integer number of minor units (cents) and only format to major units at the UI boundary

This repo currently uses `Double` throughout the domain models — acceptable for a reference/blueprint project, but exactly the kind of thing that should be fixed before any of this touched real balances.

### 2. Session expiry vs. biometric login

**Q: How do session expiry and the biometric login preference interact?**

**A:** `SecureTokenManager.isTokenExpired()` compares the stored expiry timestamp against `System.currentTimeMillis()`; `hasValidToken()` combines that with "is there a token at all." `features:profile`'s `AppPreferences.biometricLoginEnabled` is modeled as a user preference today, but there's no code path yet that actually gates re-authentication behind a biometric prompt when the token expires — the flag exists, the enforcement doesn't. That's the honest state of it: the data model anticipated the feature, the feature itself isn't built.

### 3. Why does `logout()` clear the local session even if the network call fails?

**A:** From `AuthRepositoryImpl`:
```kotlin
override suspend fun logout(): Result<Unit> {
    return try {
        remoteDataSource.logout()
        localDataSource.clearSession()
        Result.Success(Unit)
    } catch (e: Exception) {
        localDataSource.clearSession()   // ← cleared even on failure
        Result.Error(e)
    }
}
```
For a banking app, a user who tapped "Log out" must never be left silently signed in on their device just because the logout network call happened to fail (e.g. no connectivity). Clearing the local session unconditionally — and surfacing the network failure separately — is the safer default, even though it means the server-side session might briefly outlive the client-side one.

---

See [architecture.md](architecture.md) for the full layer walkthrough these answers reference, [compose_guidelines.md](compose_guidelines.md) for the UI-layer conventions, and the main [README](../README.md#-project-status) for exactly what's implemented versus planned.
