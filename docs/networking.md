# 🌐 Networking Guide

## Overview

BankApp's networking layer lives entirely in `core:network` and is built on:

- **Retrofit** — type-safe API interface definitions
- **OkHttp** — the HTTP client, interceptor chain, timeouts
- **Moshi** — JSON (de)serialization
- **`NetworkResponse<T>`** — a sealed class that turns every network outcome into data instead of a thrown exception

⚠️ **`BASE_URL` is currently a placeholder** — `"https://api.bank.com/"`, set in `core/network/build.gradle.kts`'s `buildConfigField`. Only `features:auth` (and `features:profile`, indirectly, via the session it reads) actually calls through this stack today; every other feature's remote data source is a local stub. See the [README's Project Status](../README.md#-project-status).

## Network Stack

```
┌─────────────────────────────────────────────────┐
│                  RETROFIT                        │
│         AuthApi — login / refresh / logout       │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────┐
│                   OKHTTP                         │
│  ┌──────────────────────────────────────────┐   │
│  │           INTERCEPTOR CHAIN               │   │
│  │  ┌─────────────┐ ┌─────────────────────┐  │   │
│  │  │    Auth     │ │      Logging        │  │   │
│  │  │ Interceptor │ │  (debug-only body)   │  │   │
│  │  └─────────────┘ └─────────────────────┘  │   │
│  │  ┌─────────────────────────────────────┐  │   │
│  │  │   NetworkDelay (debug-only, opt-in)  │  │   │
│  │  └─────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────┘
                     │
┌────────────────────┴────────────────────────────┐
│                    MOSHI                         │
│   LoginRequest / LoginResponse / UserDto adapters │
└─────────────────────────────────────────────────┘
```

## API Definition

`AuthApi` is the one real, wired-up API interface in the project:

```kotlin
interface AuthApi {

    @POST("auth/login")
    fun login(
        @Body request: LoginRequest,
        @Header("No-Auth") noAuth: String = "true"
    ): Call<NetworkResponse<LoginResponse>>

    @POST("auth/refresh")
    fun refreshToken(
        @Body refreshToken: String,
        @Header("No-Auth") noAuth: String = "true"
    ): Call<NetworkResponse<LoginResponse>>

    @POST("auth/logout")
    fun logout(): Call<NetworkResponse<Unit>>
}
```

The `No-Auth` header is a signal to `AuthInterceptor` (below), not the server — it never leaves the device.

## `NetworkResponse` Sealed Class

```kotlin
sealed class NetworkResponse<out T> {
    data class Success<T>(val data: T) : NetworkResponse<T>()
    data class ApiError(
        val code: Int,
        val message: String?,
        val body: String? = null
    ) : NetworkResponse<Nothing>()
    data class NetworkError(val error: Throwable) : NetworkResponse<Nothing>()
    data class UnknownError(val error: Throwable) : NetworkResponse<Nothing>()
}
```

A custom `NetworkResponseAdapterFactory` (`adapter/NetworkResponseAdapter.kt`) is what makes `Call<NetworkResponse<T>>` work as a Retrofit return type — it intercepts the raw `Response<T>`, and converts a *successful HTTP response* into `Success`, a *non-2xx response* into `ApiError`, and a *thrown `IOException`* into `NetworkError`, all before your code ever sees them. This is deliberately a second, separate type from `core:common`'s `Result<T>` — `NetworkResponse` carries HTTP-specific detail (`ApiError.code`, raw error body) that has no business leaking past `core:network`'s boundary; every feature's `data` layer converts one into the other at the edge (see [Error Handling](#error-handling)).

## Interceptors

### AuthInterceptor

Unlike a naive "always attach the token" interceptor, this one has to *not* attach a token to the login/refresh calls themselves — otherwise a request to get a token would require already having one:

```kotlin
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Login/refresh are tagged "No-Auth" in the API definition — skip token injection
        if (originalRequest.header("No-Auth") != null) {
            val newRequest = originalRequest.newBuilder()
                .removeHeader("No-Auth")   // never actually sent to the server
                .build()
            return chain.proceed(newRequest)
        }

        val token = tokenProvider.getAccessToken()
        return if (token.isNullOrEmpty()) {
            chain.proceed(originalRequest)
        } else {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        }
    }
}
```

`TokenProvider` is the narrow, three-method interface (`getAccessToken`/`getRefreshToken`/`clearTokens`) that `core:security`'s `SecureTokenManager` implements — `AuthInterceptor` only ever sees the interface, never the concrete manager (see [interview_questions.md](interview_questions.md#1-solid-principles) for why that's an Interface Segregation example).

### NetworkDelayInterceptor

A debug-only, opt-in artificial delay for exercising loading states without a slow real network:

```kotlin
@Singleton
class NetworkDelayInterceptor @Inject constructor() : Interceptor {

    var delayMs: Long = 1500L
    var isEnabled: Boolean = false   // off by default — flip on to test a slow-network UI

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isEnabled) Thread.sleep(delayMs)
        return chain.proceed(chain.request())
    }
}
```

It's always in the interceptor chain (see below), but `isEnabled` defaults to `false`, so it's a no-op unless something flips it — there's currently no debug menu wired up to do that toggling; it's there to be driven from one when needed.

## OkHttp Configuration

```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    authInterceptor: AuthInterceptor,
    networkDelayInterceptor: NetworkDelayInterceptor
): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(BuildConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)  // 30s
        .readTimeout(BuildConfig.READ_TIMEOUT, TimeUnit.SECONDS)        // 30s
        .writeTimeout(BuildConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)      // 30s
        .addInterceptor(authInterceptor)
        .addInterceptor(networkDelayInterceptor)
        .addInterceptor(loggingInterceptor)
        // Certificate pinning can be added here for production
        // .certificatePinner(certificatePinner)
        .retryOnConnectionFailure(true)
        .build()
}
```

`HttpLoggingInterceptor`'s level is itself gated:

```kotlin
level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
```

— full request/response bodies only ever log in a debug build.

## Calling the API from a Suspend Function

Retrofit's `Call<T>` isn't a suspend function on its own here (the API interface returns `Call<NetworkResponse<T>>`, not a `suspend fun`), so `features:auth`'s `AuthRemoteDataSourceImpl` wraps it with a small `suspendCancellableCoroutine` bridge instead of calling the blocking `.execute()`:

```kotlin
private suspend fun <T> Call<T>.await(): T = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val body = response.body()
            if (body != null) {
                continuation.resume(body)
            } else {
                continuation.resumeWithException(IllegalStateException("Empty response body"))
            }
        }

        override fun onFailure(call: Call<T>, throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    })
    continuation.invokeOnCancellation { cancel() }
}
```

This matters: `.execute()` blocks the calling thread until the response arrives; `enqueue()` + `suspendCancellableCoroutine` lets OkHttp's own dispatcher thread pool handle the request while the calling coroutine suspends — and `invokeOnCancellation { cancel() }` means if the coroutine is cancelled (screen left mid-request), the underlying HTTP call is actually cancelled too, not left running.

## Error Handling

`await()` gives you the raw `NetworkResponse<T>`; a second small extension collapses it to either a value or a thrown exception, right at the `data` layer boundary:

```kotlin
private fun <T> NetworkResponse<T>.unwrap(): T = when (this) {
    is NetworkResponse.Success -> data
    is NetworkResponse.ApiError -> throw IllegalStateException(message ?: "API error ($code)")
    is NetworkResponse.NetworkError -> throw error
    is NetworkResponse.UnknownError -> throw error
}

// AuthRemoteDataSourceImpl.kt
override suspend fun login(email: String, password: String): LoginResponse =
    authApi.login(LoginRequest(email, password)).await().unwrap()
```

The repository above that catches it and converts to `core:common`'s `Result`:

```kotlin
// AuthRepositoryImpl.kt
override suspend fun login(email: String, password: String): Result<User> {
    return try {
        val response = remoteDataSource.login(email, password)
        localDataSource.saveSession(response)
        Result.Success(response.user.toDomain())
    } catch (e: Exception) {
        Result.Error(e)
    }
}
```

By the time this reaches `AuthViewModel`, it's just `Result.Success`/`Result.Error` — the `ViewModel` never sees `NetworkResponse`, `Call`, or a raw HTTP status code.

## Offline-First Strategy

The clearest real example is `features:dashboard`, which emits from its local cache immediately and kicks off a refresh in the background rather than blocking the UI on the network:

```kotlin
override fun getDashboardSummary(): Flow<Result<DashboardSummary>> =
    localDataSource.observeDashboardSummary()
        .onStart { refreshDashboardSummary() }   // fire the refresh...
        .filterNotNull()                          // ...but emit cached data immediately if present
        .map { it.toDomain() }
        .asResult()

override suspend fun refreshDashboardSummary(): Result<Unit> {
    return try {
        val summary = remoteDataSource.getDashboardSummary()
        localDataSource.cacheDashboardSummary(summary)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e)   // refresh failure doesn't clear what's already cached
    }
}
```

**Honestly:** this pattern isn't applied consistently. `features:accounts`'s repository only reads from its local cache and has to be told to `refreshAccounts()` explicitly — it won't auto-refresh on subscribe the way `dashboard` does. Standardizing every feature on the `onStart { refresh() }` shape is a reasonable next cleanup pass (see [interview_questions.md](interview_questions.md#3-offline-first--is-this-app-actually-offline-first) for the fuller discussion, including why "cache-then-refresh" needs to be *more* conservative than usual for money data specifically).

## Adding a Real Endpoint

Every non-`auth`/`profile` feature's remote data source today is a stub like this (`features:accounts/data/datasource/AccountRemoteDataSource.kt`):

```kotlin
class AccountRemoteDataSourceImpl @Inject constructor() : AccountRemoteDataSource {
    override suspend fun getAccounts(): List<AccountDto> {
        // TODO: wire up to the real accounts endpoint once it is available
        return emptyList()
    }
    // ...
}
```

To turn one into a real network call once a backend exists, following the exact shape `AuthApi` already uses:

1. **Define the endpoint** in `core:network/api/` — a Retrofit interface method returning `Call<NetworkResponse<YourDto>>` (note: `core:network` already has an `AccountApi.kt` and `KYCApi.kt`, but both are unrenamed leftovers from the original template shaped around recipe endpoints — don't reuse them as-is; write the real contract fresh, the way `AuthApi` was)
2. **Provide it** in `NetworkModule.kt` — one `@Provides fun provideYourApi(retrofit: Retrofit): YourApi = retrofit.create(YourApi::class.java)`, same as `provideAuthApi`
3. **Inject it** into the feature's `RemoteDataSourceImpl` constructor, replacing the stub body with `yourApi.someCall().await().unwrap()`
4. **Point `BASE_URL`** at a real backend in `core/network/build.gradle.kts`

Nothing in `domain` or `presentation` needs to change — that's the entire point of the repository interface living in `domain` (see [architecture.md](architecture.md)).

## Best Practices

1. **Always model responses as a sealed type** — `NetworkResponse` here — never a nullable + exception combo
2. **Prefer `enqueue()` + `suspendCancellableCoroutine` over blocking `.execute()`** inside a suspend function, so cancellation actually cancels the in-flight call
3. **Cache what you can, and emit it before the network responds** — but for money-shaped data, treat the server as authoritative and never resolve conflicts client-side (see [interview_questions.md](interview_questions.md#2-offline-first-design--done-right-for-money-not-just-recipes))
4. **Log request/response bodies in debug builds only** — `HttpLoggingInterceptor`'s level is already gated on `BuildConfig.DEBUG` here; don't loosen that
5. **Set explicit timeouts** — connect/read/write all default to 30s via `BuildConfig`, not OkHttp's defaults
6. **Retry with backoff exists, but isn't wired to networking yet** — `core:common/extensions/FlowExtensions.kt` has `retryWithExponentialBackoff()`, written for `Flow<T>` generally; no `RemoteDataSource` in this repo currently calls it. Applying it to `AuthRemoteDataSourceImpl` (or wherever the first real non-auth endpoint lands) is a natural next step, not a rewrite
7. **Certificate pinning is not implemented** — the `.certificatePinner(...)` call is commented out in `NetworkModule`. Fine for a reference project; a real blocker before this touches production traffic (see [interview_questions.md](interview_questions.md#2-certificate-pinning--is-it-implemented))

---

See [architecture.md](architecture.md) for how the `data` layer this doc describes fits into the full Clean Architecture picture, and the main [README](../README.md#-project-status) for what's real versus planned across the whole project.
