# 🧪 Testing Strategy

## Overview

BankApp is built around the standard testing pyramid — and is honest about where it currently sits on it:

```
        ╱╲
       ╱  ╲
      ╱ UI ╲        (Few, slow, expensive — 0 real tests today)
     ╱──────╲
    ╱ Integ- ╲       (Room/Hilt integration — 0 real tests today,
   ╱  ration  ╲       and core:database has no consumer to test yet)
  ╱────────────╲
 ╱    Unit      ╲    (Many, fast, cheap — 1 real test file today:
╱────────────────╲    core:common's ResultTest.kt)
```

Every testing *dependency* this doc describes (JUnit, Truth, Turbine, MockWebServer, Mockito, Espresso, Hilt testing, `kotlinx-coroutines-test`) is already wired into every module's `build.gradle.kts` — see [modules.md](modules.md) for the real dependency blocks. What's missing is the tests themselves: every module except `core:common` still carries the Android Studio default `ExampleUnitTest`/`ExampleInstrumentedTest` template files, unmodified. This doc describes both — the one real example that exists, and the recommended shape for the tests that don't exist yet.

## Test Types

### Unit Tests — ✅ one real example exists

**Location:** `module/src/test/`

The one genuinely real test file in this repo, in full:

```kotlin
// core/common/src/test/java/com/mirza/common/ResultTest.kt — real, current code
class ResultTest {

    @Test
    fun `Success result contains data`() {
        val data = "test data"
        val result: Result<String> = Result.Success(data)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isError).isFalse()
        assertThat(result.isLoading).isFalse()
        assertThat(result.getOrNull()).isEqualTo(data)
    }

    @Test(expected = IllegalStateException::class)
    fun `getOrThrow throws for Error`() {
        val result: Result<String> = Result.Error(IllegalStateException("Test"))
        result.getOrThrow()
    }

    // + 9 more cases covering map(), getOrDefault(), onSuccess(), onError(), Loading
}
```

11 test cases, no mocking framework needed — because `Result<T>` is pure data, this is exactly the kind of test the domain layer's Android-free design is supposed to make trivial. It's the template every `domain`-layer test in this repo should follow.

### Use Case Tests — recommended, not yet written

Every use case in this repo extends one of four base classes (`UseCase`, `FlowUseCase`, `NoParamUseCase`, `NoParamFlowUseCase`) — testing one means faking the repository interface it depends on, exactly the way [interview_questions.md](interview_questions.md#2-how-would-you-test-accountviewmodel) already demonstrates:

```kotlin
class GetAccountsUseCaseTest {

    @Test
    fun `returns accounts from the repository`() = runTest {
        val fakeRepository = FakeAccountRepository(accounts = listOf(previewAccount))
        val useCase = GetAccountsUseCase(fakeRepository, UnconfinedTestDispatcher())

        useCase(Unit).test {
            assertThat(awaitItem()).isEqualTo(Result.Loading)
            val success = awaitItem() as Result.Success
            assertThat(success.data).containsExactly(previewAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

No mocking library needed here either — a hand-written `FakeAccountRepository implements AccountRepository` is simpler than mocking the interface, and it's what [interview_questions.md](interview_questions.md#1-solid-principles) already points to as the concrete payoff of Liskov substitution in this codebase.

### ViewModel Tests — recommended, not yet written

Every `ViewModel` in this repo needs `MainDispatcherRule` (below) to make `viewModelScope.launch { }` run synchronously inside a test:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loading accounts updates state to success`() = runTest {
        val fakeRepository = FakeAccountRepository(accounts = listOf(previewAccount))
        val viewModel = AccountViewModel(
            getAccountsUseCase = GetAccountsUseCase(fakeRepository, UnconfinedTestDispatcher()),
            getAccountByIdUseCase = GetAccountByIdUseCase(fakeRepository, UnconfinedTestDispatcher())
        )

        viewModel.onEvent(AccountEvent.LoadAccounts)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.accounts).containsExactly(previewAccount)
    }
}
```

### Repository Tests — recommended, not yet written

For the two features with a real remote data source (`auth`, `profile`), a repository test is where `MockWebServer` earns its place in the dependency list:

```kotlin
class AccountRepositoryTest {

    private val fakeLocalDataSource = FakeAccountLocalDataSource()
    private val fakeRemoteDataSource = FakeAccountRemoteDataSource()
    private val repository = AccountRepositoryImpl(fakeRemoteDataSource, fakeLocalDataSource)

    @Test
    fun `getAccounts emits mapped local cache`() = runTest {
        fakeLocalDataSource.cacheAccounts(listOf(testAccountDto))

        repository.getAccounts().test {
            assertThat(awaitItem()).isEqualTo(Result.Loading)
            val success = awaitItem() as Result.Success
            assertThat(success.data.first().id).isEqualTo(testAccountDto.id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

For `AuthRepositoryImpl` specifically, `MockWebServer` would sit *underneath* `AuthApi` (a real Retrofit client pointed at the test server), rather than faking `AuthRemoteDataSource` directly — that's the version of this test that actually exercises the `suspendCancellableCoroutine` `await()` bridge described in [networking.md](networking.md#calling-the-api-from-a-suspend-function).

### Integration Tests — recommended, blocked on a real consumer

**Location:** `module/src/androidTest/`

`core:database` already has an `androidTest` source set and a Hilt testing dependency, but — as [modules.md](modules.md#-core-database) documents — no feature module actually queries it yet, so there's nothing feature-shaped to integration-test against Room today. The shape this would take, once something does:

```kotlin
@HiltAndroidTest
class BankDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: BankDatabase

    private lateinit var bankDao: BankDao

    @Before
    fun setup() {
        hiltRule.inject()
        bankDao = database.bankDao()
    }

    @Test
    fun insertAndRetrieveRecipe() = runTest {
        val entity = createTestBankEntity()
        bankDao.insertRecipe(entity)

        val retrieved = bankDao.getRecipeById(entity.id).first()
        assertThat(retrieved?.name).isEqualTo(entity.name)
    }
}
```

(Note `getRecipeById`/`insertRecipe` are the real, if unrenamed, method names on `BankDao` today — see [modules.md](modules.md#-core-database).)

### UI Tests — recommended, not yet written

```kotlin
class AccountScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun accountList_showsLoadingIndicator_whenStateIsLoading() {
        composeTestRule.setContent {
            BankAppTheme {
                // A real AccountScreen test needs a way to inject a fake ViewModel —
                // this repo doesn't yet have a @Composable that takes UiState directly
                // rather than a hiltViewModel() default; see compose_guidelines.md's
                // recommended Screen/Content split, which is exactly what would unblock this.
            }
        }
    }
}
```

**Honestly:** this is the one test category actively blocked by an architectural gap, not just missing effort — [compose_guidelines.md](compose_guidelines.md#screen-architecture) already documents that no screen in this repo splits into a stateful `Screen` + stateless `Content`. Without that split, a Compose UI test either needs a full Hilt test graph (`@HiltAndroidTest`, slow) or can't easily drive the screen with fake state at all. Writing the `Screen`/`Content` split is a prerequisite for meaningful UI tests here, not a parallel task.

## Testing Tools

All already declared as dependencies — see the real `testImplementation`/`androidTestImplementation` blocks in [modules.md](modules.md#feature-modules):

| Tool | Purpose | Version |
|------|---------|---------|
| JUnit | Test framework | 4.13.2 |
| Truth | Fluent assertions (used throughout `ResultTest`) | 1.4.5 |
| Turbine | `Flow` testing (`.test { awaitItem() }`) | 1.2.1 |
| MockWebServer | HTTP-level API mocking | 4.12.0 |
| Mockito / Mockito-Kotlin | Mocking, for the cases a hand-written fake isn't simpler | 5.12.0 / 5.4.0 |
| `kotlinx-coroutines-test` | `runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle()` | 1.9.0 |
| Room Testing | In-memory database for DAO tests | 2.6.1 |
| Hilt Android Testing | `@HiltAndroidTest`, `@TestInstallIn` | 2.52 |
| Espresso / Compose UI Testing | Instrumented UI tests | 3.7.0 |

## Test Utilities

### `MainDispatcherRule` — ✅ already exists, ready to use

```kotlin
// core/common/src/main/java/com/mirza/common/testing/MainDispatcherRule.kt — real code
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

This already ships in `core:common`'s main source set (not `test/`) specifically so every module that depends on `core:common` can use `@get:Rule val mainDispatcherRule = MainDispatcherRule()` in a `ViewModel` test without redeclaring it — but as of today, no test in the repo actually uses it yet.

### Test Doubles — pattern, not yet written per-feature

```kotlin
class FakeAccountRepository(
    private var accounts: List<Account> = emptyList()
) : AccountRepository {

    override fun getAccounts(): Flow<Result<List<Account>>> = flowOf(Result.Success(accounts))

    override suspend fun getAccountById(accountId: String): Result<Account> =
        accounts.find { it.id == accountId }
            ?.let { Result.Success(it) }
            ?: Result.Error(NoSuchElementException("Account $accountId not found"))

    override suspend fun refreshAccounts(): Result<Unit> = Result.Success(Unit)
}
```

Because every feature's `domain/repository/*Repository.kt` is an interface with no Android dependency, a fake like this is a same-file amount of code, not a mocking-framework incantation — the same point made in [interview_questions.md](interview_questions.md#3-repository-pattern).

## Coverage Goals

Targets to work toward — not a claim about current state, which is close to 0% outside `core:common`:

| Layer | Target Coverage | Current |
|-------|-----------------|---------|
| Domain (UseCases) | 90%+ | 0% (`ResultTest.kt` covers `core:common`'s `Result`, not any feature's use cases) |
| ViewModels | 80%+ | 0% |
| Repositories | 80%+ | 0% |
| UI Components | 60%+ | 0% |

`core:common` itself — `Result`, `BaseUseCase`, `FlowExtensions` — is the closest thing to fully covered, and even that's only `Result` today; `BaseUseCase` and `FlowExtensions` have no dedicated tests yet either.

## Running Tests

```bash
# Unit tests — everything under src/test across all modules
./gradlew testDebugUnitTest

# Unit tests for one module — much faster while iterating
./gradlew :core:common:testDebugUnitTest
./gradlew :features:accounts:testDebugUnitTest

# Instrumented tests — requires a connected device/emulator
./gradlew connectedAndroidTest
```

`jacocoTestReport` isn't wired into this project (see [pr_review_guide.md](pr_review_guide.md#automated-checks) — no coverage tooling configured yet), so there's no single command that reports a coverage percentage today; the table above is a target to design tests against, not a number `./gradlew` will currently print.

---

See [pr_review_guide.md](pr_review_guide.md) for what a reviewer should expect (and not expect) around test coverage on a new PR today, and the main [README](../README.md#-project-status) for overall project status.
