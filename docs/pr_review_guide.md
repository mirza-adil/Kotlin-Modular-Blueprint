# 🔍 Pull Request Review Guide

## Overview

A good PR review catches bugs, keeps 16 modules internally consistent, and stops small inconsistencies (a `Double` where money should be a `BigDecimal`, a screen using `collectAsState()` instead of the lifecycle-aware version) from spreading across nine near-identical feature modules before anyone notices.

This guide is a checklist-driven framework for reviewing PRs against this repo's actual conventions — the same ones documented in [architecture.md](architecture.md), [compose_guidelines.md](compose_guidelines.md), and [networking.md](networking.md). Where this project's own code currently violates one of its own rules, that's called out explicitly below, not hidden — a PR reviewer should be able to spot the same things this doc does.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PR REVIEW WORKFLOW                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│   PR Created → Context Check → Code Review → Testing → Feedback → Merge │
│       │              │              │           │          │         │   │
│       ▼              ▼              ▼           ▼          ▼         ▼   │
│    Author       Understand      Review     Verify    Approve/    CI/CD  │
│    Submits      the "Why"       Changes    Works     Request     Runs*  │
│                                                      Changes            │
└─────────────────────────────────────────────────────────────────────────┘
```
\* *There's no CI pipeline configured in this repo yet — see [Automated Checks](#automated-checks) for what "Testing" and "Runs" actually mean here today.*

## The 7-Step PR Review Framework

| Step | Focus Area | Key Question |
|------|------------|---------------|
| **1. Context** | Purpose & Background | Why is this change needed? |
| **2. Correctness** | Technical Accuracy | Does it work correctly? |
| **3. Completeness** | Full Implementation | Are all affected layers/modules updated? |
| **4. Compatibility** | Breaking Changes | Does it break existing code or data? |
| **5. Consistency** | Code Style | Does it follow this repo's conventions? |
| **6. Clarity** | Readability | Is it understandable without the author explaining it? |
| **7. Considerations** | Edge Cases | Performance, security, accessibility? |

---

## Step 1: Context Analysis 📋

**Goal:** Understand the purpose before reading a single line of the diff.

### Checklist
- [ ] PR title and description explain the "why," not just the "what"
- [ ] Scope is appropriate for one PR — one feature module or one core module, not a sweep across several unless the change genuinely spans them (e.g. a `core:common` change)
- [ ] If it's a new feature module, it follows the existing module template (see [modules.md](modules.md#feature-modules))

### Red Flags 🚩
- PR with no description
- A PR touching more than one `features:*` module for a change that isn't in `core:*` — that's a sign it should be split
- "Add real endpoint for X" PRs that don't also update the feature's `data/mapper/` and remove the corresponding `// TODO: wire up to the real ... endpoint` comment

### Example: Good PR Description for This Repo

```markdown
## Summary
Wire features:accounts's remote data source to the real GET /accounts endpoint

## Problem
AccountRemoteDataSourceImpl currently returns emptyList() with a TODO —
the account list only ever shows what's already in the local cache.

## Solution
- Add `AccountApi` interface to core:network (the existing one is a
  leftover recipe-shaped stub — this replaces it, doesn't extend it)
- Provide it in NetworkModule.provideAccountApi(), same pattern as provideAuthApi
- Replace the stub body in AccountRemoteDataSourceImpl with the real call
- No changes needed in domain/ or presentation/ — this is the point of
  the repository interface living in domain

## Testing
- Ran :features:accounts:compileDebugKotlin — passes
- Manually verified against a local mock server

## Related
- See docs/networking.md#adding-a-real-endpoint for the pattern this follows
```

---

## Step 2: Correctness Analysis ✅

**Goal:** Verify the implementation is technically correct.

### Checklist
- [ ] Logic handles the expected cases *and* the empty/error case
- [ ] No unnecessary `!!` — this repo's `Result`/`NetworkResponse` sealed types exist specifically so nothing needs to force-unwrap
- [ ] Threading is correct — I/O work goes through a `UseCase` subclass (which already handles the dispatcher switch), never a raw `withContext` scattered in a `ViewModel`
- [ ] New coroutines are launched in `viewModelScope`, never `GlobalScope`

### This Repo's Idioms

```kotlin
// ❌ BAD — bypasses the Result type this whole codebase is built around
val account = accountRepository.getAccountById(id).getOrThrow()   // in a ViewModel, uncaught

// ✅ GOOD — every ViewModel in this repo handles all three Result states explicitly
when (val result = getAccountByIdUseCase(id)) {
    is Result.Success -> _uiState.update { it.copy(selectedAccount = result.data) }
    is Result.Error -> _uiState.update { it.copy(errorMessage = result.message) }
    Result.Loading -> Unit
}
```

```kotlin
// ❌ BAD — reimplements what UseCase already does for you
class GetAccountsUseCase @Inject constructor(private val repo: AccountRepository) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) { repo.getAccounts() }
}

// ✅ GOOD — extend the base class, dispatcher is handled once, centrally
class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<Unit, List<Account>>(dispatcher) {
    override fun execute(parameters: Unit) = accountRepository.getAccounts()
}
```

### Common Correctness Issues in This Codebase

| Issue | Where it'd show up | What to check |
|-------|---------------------|----------------|
| Wrong entity type on a Room query | `core:database`'s `BankDao` (see [modules.md](modules.md#-core-database)) — this file already has a documented history of entity/return-type mismatches | Return type actually matches the queried table's columns |
| Money as `Double` | Any new field on `Account`, `Transfer`, `Payment` | Flag it — see [interview_questions.md](interview_questions.md#1-money-as-double--a-real-problem-in-this-codebase) for why this matters and isn't just style |
| Missing idempotency on a mutating call | `CreateTransferUseCase`, `PayBillUseCase`, or any new one like them | Does a retried request risk a duplicate transfer/payment? |
| `GlobalScope` or a bare `Thread` | Anywhere | Should be `viewModelScope.launch` or inside a `UseCase` |

---

## Step 3: Completeness Analysis 🧩

**Goal:** Ensure a change is threaded through every layer it touches — this repo's four-package-per-feature shape makes it easy to update `domain` and forget `data`, or vice versa.

### Checklist
- [ ] A new domain field is mapped in both directions in `data/mapper/` (DTO → domain *and*, if applicable, domain → DTO)
- [ ] A new use case has a corresponding `di/<Feature>Module.kt` binding if it introduces a new interface
- [ ] A `UiState` change is actually consumed somewhere in the `Screen`, not just added and left unused
- [ ] If touching `core:common`, every module that depends on it still compiles — run `./gradlew compileDebugKotlin` for the whole project, not just one module

### Multi-Module Considerations

```
When changing a core module, check what depends on it:
├── core:common    → depended on by every module in the project
├── core:network   → depended on by core:security, features:auth, features:profile
├── core:security  → depended on by features:auth, features:profile
├── core:database  → currently depended on by nothing (see modules.md) — a change here can't break a feature today, but shouldn't be assumed safe forever
├── core:ui        → depended on by every features:* module
└── features:*     → never depended on by another features:* module (verified — grep for it)
```

### Example: An Incomplete PR, In This Repo's Shape

```kotlin
// PR adds a new field to the domain model...
data class Account(
    val id: String,
    val accountNumber: String,
    val pendingBalance: Double,   // NEW — but is it threaded through everywhere?
    // ...
)
```
Checklist for that one-line change:
- [ ] `AccountDto` (`data/datasource/AccountDto.kt`) — does it have a matching field, or does the mapper silently default it?
- [ ] `AccountMapper.toDomain()` — does it actually map the new field, or leave it hardcoded?
- [ ] `AccountRemoteDataSourceImpl` — if this is meant to come from a real API eventually, does the stub reflect the new shape?
- [ ] `AccountScreen`'s `AccountListItem` — does the UI do anything with it, or is the field dead on arrival?

---

## Step 4: Compatibility Analysis 🔄

**Goal:** Verify no breaking changes to public interfaces or persisted data.

### Checklist
- [ ] A `domain` interface change (e.g. adding a method to `AccountRepository`) is implemented in `AccountRepositoryImpl` *and* any fakes used in tests
- [ ] A Room entity/schema change ships with a real `Migration`, not `fallbackToDestructiveMigration()`
- [ ] Bumping `minSdk` (currently 24) or `compileSdk`/`targetSdk` (currently 36) in one module's `build.gradle.kts` is intentional and consistent with the others

### ⚠️ A real example already in this codebase

The reference pattern below is exactly what `core/database/di/DatabaseModule.kt` does **today**:

```kotlin
// core/database/di/DatabaseModule.kt — current, real code
Room.databaseBuilder(context, BankDatabase::class.java, BankDatabase.DATABASE_NAME)
    .fallbackToDestructiveMigration()   // ⚠️ wipes local data on any schema bump
    .build()
```
This is a fine, honest default *while `core:database` has zero real consumers* (see [modules.md](modules.md#-core-database)) — there's no user data to lose yet. The moment any feature module starts actually persisting through Room, a PR that bumps the schema version without adding a real `Migration` should be a **[Blocker]**, not a nitpick:

```kotlin
// What a real migration looks like, once this matters:
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE accounts ADD COLUMN pending_balance REAL NOT NULL DEFAULT 0")
    }
}
```

---

## Step 5: Consistency Analysis 📐

**Goal:** New code should be indistinguishable in style from the module next to it.

### Naming Conventions (already consistent across all 9 built feature modules — hold new code to the same bar)

```kotlin
// Files & classes
AccountViewModel.kt        // ViewModel suffix
AccountRepository.kt       // interface, no suffix
AccountRepositoryImpl.kt   // implementation, Impl suffix
GetAccountsUseCase.kt      // UseCase suffix, verb-first
AccountUiState.kt          // UiState suffix
AccountEvent.kt            // Event suffix, sealed interface

// Functions
suspend fun login(...)          // suspend for coroutine-based work
fun onEvent(event: AccountEvent)  // the one public entry point into a ViewModel
```

### Architecture Consistency

```kotlin
// ✅ GOOD — matches every ViewModel in this repo: depends on UseCases only
class AccountViewModel @Inject constructor(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getAccountByIdUseCase: GetAccountByIdUseCase
) : ViewModel()

// ❌ BAD — bypasses domain entirely; would be the first ViewModel in this
// codebase to do this, and should be treated as a [Blocker], not a style nit
class AccountViewModel @Inject constructor(
    private val accountRepository: AccountRepository,  // repository, not use case
    private val accountApi: AccountApi                 // API, straight into presentation!
) : ViewModel()
```

---

## Step 6: Clarity Analysis 📖

**Goal:** Code should be understandable without the author walking the reviewer through it live.

### Checklist
- [ ] No comment explaining *what* the code does when the code already says it — this repo's own conventions (see the top-level CLAUDE-style guidance) favor self-explanatory names over narration
- [ ] Magic numbers are named constants
- [ ] A non-obvious business rule (why does `logout()` clear the local session even on failure? see [interview_questions.md](interview_questions.md#3-why-does-logout-clear-the-local-session-even-if-the-network-call-fails)) has a comment explaining the *why*, not the *what*

### Self-Documenting Code, Banking-Flavored

```kotlin
// ❌ BAD — magic numbers, unclear intent
if (amount > 0 && amount <= 50000 && recipientAccountNumber.length == 10) { ... }

// ✅ GOOD — the rule reads as a rule
private const val MAX_TRANSFER_AMOUNT = 50_000.0
private const val ACCOUNT_NUMBER_LENGTH = 10

fun isTransferRequestValid(amount: Double, recipientAccountNumber: String): Boolean {
    val isAmountInRange = amount > 0 && amount <= MAX_TRANSFER_AMOUNT
    val isAccountNumberValid = recipientAccountNumber.length == ACCOUNT_NUMBER_LENGTH
    return isAmountInRange && isAccountNumberValid
}
```

---

## Step 7: Considerations Analysis 💭

**Goal:** Non-functional requirements a passing build won't catch.

### Performance Checklist
- [ ] New `LazyColumn`/`LazyRow` uses `key = { it.id }` (see [performance.md](performance.md#keyed-lists--already-applied-everywhere) — every existing screen already does this)
- [ ] New screen uses `collectAsStateWithLifecycle()`, not `collectAsState()` — ⚠️ every *existing* screen in this repo currently uses the latter, so don't let a reviewer wave this through just because "that's what's already there"; it's a known gap, not the standard to match
- [ ] A new `Room` query on `core:database` selects only needed columns and has an index on any `WHERE`-filtered column

### Security Checklist
- [ ] No token, password, or PII in a `Log.d`/`println` — check `HttpLoggingInterceptor`'s level stays gated on `BuildConfig.DEBUG` (see [networking.md](networking.md#okhttp-configuration))
- [ ] Any new hardcoded key belongs behind `core:security`'s native key provider, not a Kotlin `const val` (see [interview_questions.md](interview_questions.md#3-native-ndk-key-storage--why-go-this-far))
- [ ] A new endpoint added to `core:network` doesn't quietly rely on the still-commented-out certificate pinner being active — it isn't (see [networking.md](networking.md#best-practices))

---

## Providing Feedback

### The Right Tone

```
✅ DO:
- "Consider using X because..."
- "Have you thought about...?"
- "This works, but we could improve it by..."

❌ DON'T:
- "This is wrong."
- "Why didn't you do X?"
- "I would never do it this way."
```

### Feedback Categories

| Prefix | Meaning | Required before merge? |
|--------|---------|-----------|
| `[Blocker]` | Must fix | Yes |
| `[Major]` | Significant issue, should fix | Usually |
| `[Minor]` | Small improvement | No |
| `[Nit]` | Personal preference | No |
| `[Question]` | Seeking clarification | N/A |
| `[Praise]` | Calling out good work | N/A |

### Example Review Comments, In This Repo's Context

```markdown
[Blocker] This adds a new field to Payment.amount but it's still a Double —
please don't compound the existing money-as-Double issue on new code;
at minimum use BigDecimal for this one going forward.

[Major] This ViewModel injects AccountRepository directly instead of a
UseCase — every other ViewModel in this repo goes through domain. See
AccountViewModel for the pattern.

[Minor] New LazyColumn here is missing `key = { it.id }` — every existing
screen has this, worth staying consistent.

[Question] Is there a reason this uses collectAsState() instead of
collectAsStateWithLifecycle()? I know the rest of the repo does too, but
new code seems like the right place to start switching.

[Praise] Nice catch wrapping this in suspendCancellableCoroutine instead
of calling .execute() — matches AuthRemoteDataSourceImpl's pattern exactly.
```

---

## PR Review Checklist Template

```markdown
## PR Review: [PR Title]

### Context
- [ ] Purpose is clear, scope is one module (or a justified core change)

### Correctness
- [ ] Result/NetworkResponse handled exhaustively, no unnecessary !!
- [ ] Dispatcher handled via a UseCase base class, not ad hoc withContext
- [ ] viewModelScope used, not GlobalScope

### Completeness
- [ ] DTO → mapper → domain → UiState → Screen all updated together
- [ ] Hilt binding added to di/ if a new interface was introduced
- [ ] Whole-project `compileDebugKotlin` still passes if core:* changed

### Compatibility
- [ ] No Room schema change without a real Migration (once core:database has consumers)
- [ ] No breaking change to a domain interface without updating all implementations

### Consistency
- [ ] Naming matches this repo's convention (see Step 5)
- [ ] ViewModel depends on UseCases, not Repository/Api directly

### Clarity
- [ ] No magic numbers; comments explain why, not what

### Considerations
- [ ] LazyColumn has a stable key; collectAsStateWithLifecycle() used for new screens
- [ ] No secrets logged or hardcoded

### Verdict
- [ ] ✅ Approved
- [ ] 🔄 Request Changes
- [ ] 💬 Comment Only
```

---

## Android-Specific Review Points

### Gradle / Version Catalog — already followed consistently

```kotlin
// ✅ This repo already does this everywhere — hold new PRs to the same bar
implementation(libs.retrofit)

// ❌ Should never appear in a PR against this repo
implementation("com.squareup.retrofit2:retrofit:2.11.0")
```
Every dependency across all 16 modules resolves through `gradle/libs.versions.toml` — verified, there's no hardcoded version string anywhere in the project today. The one place an explicit version-like value legitimately appears outside the catalog is `core:security`'s `ndkVersion = libs.versions.ndk.get()` — which is still catalog-sourced, just assigned to a build config field rather than a dependency coordinate.

### Jetpack Compose

| Check | Current state in this repo |
|-------|------|
| `LazyColumn` keys | ✅ Already applied everywhere — hold new code to it |
| `collectAsStateWithLifecycle()` | ⚠️ Not adopted anywhere yet — see [Considerations](#step-7-considerations-analysis-) above |
| Stable/`@Immutable` types | ✅ Every domain model and `UiState` already qualifies (all-`val` data classes) — no explicit annotation needed, see [performance.md](performance.md#stable-types--already-applied-no-annotation-needed) |
| `derivedStateOf` for computed values | Not used yet — no screen filters a list client-side today |

### Hilt / Dependency Injection

```kotlin
// Every Hilt module in this repo is installed the same way — check new ones match:
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {
    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
}
```
This repo doesn't use `@ActivityScoped`/`@ViewModelScoped`/`@ActivityRetainedScoped` anywhere — every binding is `@Singleton` in `SingletonComponent`. A PR introducing a narrower scope isn't wrong on its face, but it'd be the first of its kind here — worth a `[Question]` about why the narrower scope is needed rather than a silent approve.

---

## Common PR Anti-Patterns

### 1. The Monster PR
A single PR building out an entire feature module (all four packages) at once is *expected* here — that's how every existing module landed. The anti-pattern is a PR that touches **multiple unrelated feature modules** in one diff; each feature module's independence (see [modules.md](modules.md#dependency-rules)) means there's rarely a reason for that.

### 2. The Sneaky Refactor
A PR titled "fix account balance display" that also renames unrelated files in `core:ui`. Split it.

### 3. The Copy-Paste
Duplicating a data source pattern instead of noticing it already exists — e.g. writing a new `suspendCancellableCoroutine` `Call.await()` bridge in a second feature instead of promoting `AuthRemoteDataSourceImpl`'s existing one to `core:network` once a second feature needs real networking.

### 4. The Missing Tests
**Honestly, this describes almost every module in this repo today** — every feature module still carries the default template `ExampleUnitTest`/`ExampleInstrumentedTest` rather than real coverage (`core:common`'s `ResultTest.kt` is the one exception). A reviewer holding *new* PRs to "no tests, no merge" while the existing codebase has none is inconsistent — but it's also exactly how that gap ever closes. See [interview_questions.md](interview_questions.md#1-unit-test-vs-instrumented-test) for the fuller honest take.

### 5. The Silent Dependency Update
Bumping a version in `gradle/libs.versions.toml` without checking the changelog — easy to do since it's one line in one file that every module reads from.

---

## Automated Checks

⚠️ **Honestly: there's no `ktlint`, `detekt`, or CI pipeline configured in this repo today** — no lint/static-analysis Gradle plugin, no `.github/workflows/`. What a reviewer can actually run before a manual review, right now:

```bash
# Compile the module(s) the PR touches — fastest signal
./gradlew :features:accounts:compileDebugKotlin

# Compile everything, if the PR touched core:*
./gradlew compileDebugKotlin

# Run unit tests (see the honest caveat above about how much coverage exists)
./gradlew testDebugUnitTest

# Full debug build
./gradlew :app:assembleDebug
```

| Check | Tool | Status |
|-------|------|--------|
| Compile | Gradle/Kotlin | ✅ Works today, run it |
| Unit tests | JUnit (via `testDebugUnitTest`) | ✅ Works today, coverage is thin |
| Lint / style | ktlint | ❌ Not configured |
| Static analysis | Detekt | ❌ Not configured |
| Coverage reporting | JaCoCo | ❌ Not configured |
| CI | GitHub Actions or similar | ❌ Not configured |

Adding `ktlint`/`detekt` and a CI workflow that runs the four commands above on every PR is a reasonable, low-effort next step for this repo — worth doing before the module count or contributor count grows much further.

## Review Turnaround Guidelines

| PR Size | Expected Review Time |
|---------|---------------------|
| XS (< 50 lines) | Same day |
| S (50–200 lines) | 1 business day |
| M (200–500 lines) — roughly one new feature module | 2 business days |
| L (500+ lines) | Consider splitting |

---

## Summary

1. **Be thorough but efficient** — this repo's consistency across 9 feature modules is the thing most worth protecting in review
2. **Be kind but honest** — critique code, not people
3. **Be curious** — ask why, especially when a PR deviates from an established pattern
4. **Hold new code to the target, not the current baseline** — several of this repo's own known gaps (`collectAsState()` vs. the lifecycle-aware version, thin test coverage, `Double` for money) are documented, not endorsed; don't let "that's what's already there" be a reason to wave through more of it

> "A good code review is a conversation, not an inspection."

---

## Related Documentation

- [Architecture Overview](architecture.md)
- [Compose Guidelines](compose_guidelines.md)
- [Networking Guide](networking.md)
- [Performance Optimization](performance.md)
- [Module Structure](modules.md)
- [Interview Questions](interview_questions.md) — several of the honest gaps referenced above are discussed in more depth here
