# 🔐 Security Best Practices

## Overview

BankApp's security layer, entirely in `core:security` (plus `AuthInterceptor` in `core:network`), covers:

- **Native (NDK/C++) API key storage** with XOR obfuscation and package-name verification
- **Encrypted session storage** via `EncryptedSharedPreferences`
- **Bearer token injection** that skips login/refresh requests

✅ **Writing this doc surfaced two real, currently-fixed bugs that had made the native key provider non-functional — see [The JNI Package Mismatch](#-the-jni-package-mismatch-fixed) for what was wrong and how it was verified fixed.**

---

## 🔑 Native API Key Storage (NDK/C++)

### The Problem

- **`BuildConfig`/`strings.xml`** — trivially extracted from a decompiled APK
- **Plain `SharedPreferences`** — unencrypted, readable with root or a backup extraction
- **Even `EncryptedSharedPreferences`** — fine for a token issued at runtime, but a *build-time secret* still has to originate somewhere before it can be encrypted

### The Solution Hierarchy

| Level | Approach | Security | This repo |
|:------|:---------|:---------|:-----------|
| 1 | `local.properties` + `BuildConfig` | 🔴 Low | Not used |
| 2 | **NDK + C++** | 🟡 Medium-High | **Implemented** (with a real bug — see below) |
| 3 | Backend proxy / short-lived token exchange | 🟢 High | Not implemented |
| 4 | Provider-side restrictions (package name, SHA-1, quota) | 🟢 Mandatory regardless of the above | Not configured — there's no third-party API key in use yet to restrict |

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
├─────────────────────────────────────────────────────────┤
│  @Inject lateinit var apiKeyProvider: ApiKeyProvider     │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              Hilt Dependency Injection                   │
├─────────────────────────────────────────────────────────┤
│  SecurityModule.provideApiKeyProvider()                  │
│  → DefaultApiKeyProvider(nativeKeyProvider)               │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│                  ApiKeyProvider (interface)               │
│  isAvailable() · getApiKey() · getSecretKey() ·           │
│  getAppIdentifier() · validateKeyFormat() ·                │
│  getApiKeyOrNull() · getSecretKeyOrNull()                  │
└────────────────────────┬────────────────────────────────┘
                         │ implemented by
┌────────────────────────▼────────────────────────────────┐
│               NativeKeyProvider (Kotlin)                  │
│  package com.mirza.security.di                            │
│  System.loadLibrary("native-keys")                        │
│  external fun getApiKeyNative(context): String             │
└────────────────────────┬────────────────────────────────┘
                         │ JNI  ✅ verified matching — see below
┌────────────────────────▼────────────────────────────────┐
│               native-keys.cpp (C++)                       │
│  XOR-encoded keys · package-name verification ·           │
│  runtime decoding                                          │
└─────────────────────────────────────────────────────────┘
```

### ✅ The JNI Package Mismatch — Fixed

`NativeKeyProvider.kt` lives in `package com.mirza.security.di`:

```kotlin
// NativeKeyProvider.kt — line 1
package com.mirza.security.di

// ...
private external fun getApiKeyNative(context: Context): String
```

JNI resolves a native method by mangling the **fully-qualified Kotlin/Java package + class name** into the exported C symbol. For this class, that has to be `Java_com_mirza_security_di_NativeKeyProvider_getApiKeyNative`. Until this was fixed, `native-keys.cpp` instead exported:

```cpp
// native-keys.cpp — as originally found, before the fix
JNIEXPORT jstring JNICALL
Java_com_eslam_bakingapp_core_security_NativeKeyProvider_getApiKeyNative(
        JNIEnv* env, jobject /* thiz */, jobject context
)
```

`com_eslam_bakingapp_core_security` was the *original template's* package — a leftover, exactly like the `getRecipesPagingSource()` naming in `core:database` (see [modules.md](modules.md#-core-database)), except this one wasn't cosmetic. **It was a build that looked fine and failed at runtime**, in a way that was easy to miss:

- `System.loadLibrary("native-keys")` **succeeds** — it only loads the `.so` file, which has no idea what symbols Kotlin will ask for. `NativeKeyProvider.isAvailable()` returns `true`.
- Any call to `getApiKey()`, `getSecretKey()`, `getAppIdentifier()`, or `validateKeyFormat()` would then hit `getApiKeyNative()` — a native method the JVM couldn't find a matching exported symbol for — and throw `UnsatisfiedLinkError`.
- **`UnsatisfiedLinkError` extends `java.lang.Error`, not `Exception`.** Every one of `NativeKeyProvider`'s methods wraps its native call in `catch (e: Exception)` — which does **not** catch an `Error`. The exception would not be swallowed; it would propagate and **crash the app** the first time any code actually called `apiKeyProvider.getApiKey()`.

Interestingly, the *package-name verification string inside the C++* was already correctly updated before this fix:

```cpp
// native-keys.cpp — this part was already correct
const std::string EXPECTED_PACKAGE = "com.mirza.kotlinmodularblueprint";
const std::string EXPECTED_PACKAGE_DEBUG = "com.mirza.kotlinmodularblueprint.debug";
```

That matches `app/build.gradle.kts`'s real `applicationId` — so whoever adapted this template renamed the *runtime package check* but missed the *four JNI export symbol names* above it, which was a very easy thing to miss since nothing about it failed at compile time.

**The fix**, applied: all four `Java_com_eslam_bakingapp_core_security_NativeKeyProvider_*` symbols in `native-keys.cpp` were renamed to `Java_com_mirza_security_di_NativeKeyProvider_*`.

#### A second bug found while verifying the fix

Building `core:security` to confirm the rename actually took effect surfaced a second, more fundamental problem: **the native library wasn't being compiled at all.** `core/security/build.gradle.kts` configured `defaultConfig.externalNativeBuild.cmake { cppFlags, arguments }` and `buildTypes { debug/release { externalNativeBuild { cmake { ... } } } }`, but never declared the one thing that actually registers a CMake build with Gradle — `android.externalNativeBuild.cmake.path`. Without it, `./gradlew :core:security:assembleDebug` produced `mergeDebugNativeLibs NO-SOURCE` — `native-keys.cpp` had never been compiled into this project's build at all, so the JNI rename above would have had nothing to attach to.

**The fix**, applied:

```kotlin
// core/security/build.gradle.kts — added inside the android { } block
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}
```

**Both fixes were verified, not just applied:** `./gradlew :core:security:assembleDebug` now runs real `configureCMakeDebug[<abi>]`/`buildCMakeDebug[<abi>]` tasks for all four ABIs and produces `libnative-keys.so` for each. Running `nm` against the compiled `arm64-v8a` binary confirms the exported symbols now match exactly what `NativeKeyProvider.kt` declares:

```
$ nm -D libnative-keys.so | grep Java_
Java_com_mirza_security_di_NativeKeyProvider_getApiKeyNative
Java_com_mirza_security_di_NativeKeyProvider_getAppIdentifierNative
Java_com_mirza_security_di_NativeKeyProvider_getSecretKeyNative
Java_com_mirza_security_di_NativeKeyProvider_validateKeyFormatNative
```

### Security Layers

1. **Native code storage** — keys exist only as ARM/x86 assembly after compilation, not as a string constant in a `.dex` file
2. **XOR obfuscation** — `API_KEY_ENCODED`/`SECRET_KEY_ENCODED` are `std::vector<char>`, not a plaintext string, so they don't show up in a `strings` dump of the `.so`
3. **String splitting** — `getAppIdentifier()` concatenates `KEY_PREFIX_PART_1` + `KEY_PREFIX_PART_2` + `"v1"` at runtime rather than storing one complete literal
4. **Package verification** — `verifyPackageName()` calls back into the JVM via JNI to read the calling app's real package name and refuses to decode anything if it doesn't match

### Usage

```kotlin
@Inject
lateinit var apiKeyProvider: ApiKeyProvider

val apiKey = apiKeyProvider.getApiKey()
```

### Safer Access Pattern (still recommended)

Even with the JNI binding fixed, `isAvailable()` can still be `false` on a device where the `.so` genuinely can't load for that ABI (or in a unit test with no native environment at all) — prefer the `OrNull` variants at any call site that isn't prepared to crash over it:

```kotlin
val apiKey = apiKeyProvider.getApiKeyOrNull() ?: run {
    // handle unavailable key — don't crash the whole screen over it
    null
}
```

### Encoding New Keys

```bash
cd core/security/scripts
python encode_keys.py "your_real_api_key" 0x5A --verify
```

`encode_keys.py` exists at `core/security/scripts/encode_keys.py` — real, verified present in this repo — and outputs a C++ `std::vector<char>` initializer to paste into `native-keys.cpp`.

### File Structure

```
core/security/
├── src/main/
│   ├── cpp/
│   │   ├── CMakeLists.txt         # 16KB page alignment for Android 15+, symbol stripping in release
│   │   └── native-keys.cpp        # JNI export names verified matching (see above)
│   └── java/
│       ├── ApiKeyProvider.kt      # public interface
│       ├── NativeKeyProvider.kt   # JNI bridge — package com.mirza.security.di
│       ├── EncryptedPreferencesManager.kt
│       ├── SecureTokenManager.kt
│       └── di/SecurityModule.kt
├── scripts/
│   └── encode_keys.py
├── proguard-rules.pro
└── consumer-rules.pro
```

### Build Requirements

- **NDK** — version pinned via the version catalog: `ndkVersion = libs.versions.ndk.get()` in `core/security/build.gradle.kts` (see [pr_review_guide.md](pr_review_guide.md#gradle--version-catalog--already-followed-consistently) — this is the one place a version-catalog value feeds a build config field rather than a dependency)
- **CMake** 3.22.1+ (declared in `CMakeLists.txt`'s `cmake_minimum_required`, and now also registered via `android.externalNativeBuild.cmake.path` in `build.gradle.kts` — see [above](#a-second-bug-found-while-verifying-the-fix))
- Supported ABIs (from `core/security/build.gradle.kts`'s `abiFilters`): `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` — all four verified building successfully

---

## Encrypted Storage

```kotlin
// EncryptedPreferencesManager.kt — real code
@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
```

### What's Actually Stored Encrypted in This Repo

| Data | Stored encrypted? | Where |
|-----------|-----------------|---------|
| Access & refresh tokens | ✅ Yes | `SecureTokenManager` via `EncryptedPreferencesManager` |
| Token expiry timestamp | ✅ Yes | same |
| Cached user id/email/name | ✅ Yes | same — used by both `features:auth` and `features:profile` |
| Every other feature's local cache (accounts, cards, transfers, ...) | ❌ No, and correctly so | plain in-memory `MutableStateFlow` — not persisted at all, so encryption isn't the relevant control; see [modules.md](modules.md#-core-database) |
| App preferences (dark mode, biometric toggle) | ❌ No | `features:profile`'s `AppPreferences` is in-memory only today, no persistence layer at all yet |

---

## Token Management

```kotlin
// SecureTokenManager.kt — real code
@Singleton
class SecureTokenManager @Inject constructor(
    private val encryptedPrefsManager: EncryptedPreferencesManager
) : TokenProvider {

    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        encryptedPrefsManager.putString(KEY_ACCESS_TOKEN, accessToken)
        encryptedPrefsManager.putString(KEY_REFRESH_TOKEN, refreshToken)
        encryptedPrefsManager.putLong(KEY_TOKEN_EXPIRY, expiryTime)
    }

    fun isTokenExpired(): Boolean {
        val expiryTime = encryptedPrefsManager.getLong(KEY_TOKEN_EXPIRY, 0L)
        return System.currentTimeMillis() >= expiryTime
    }

    fun hasValidToken(): Boolean = getAccessToken() != null && !isTokenExpired()

    fun clearAll() {
        clearTokens()
        clearUserInfo()
    }
}
```

`SecureTokenManager` implements the narrow `TokenProvider` interface (`getAccessToken`/`getRefreshToken`/`clearTokens`) that `core:network`'s `AuthInterceptor` depends on — the interceptor never sees the broader token manager, only the three methods it actually needs (see [interview_questions.md](interview_questions.md#1-solid-principles) for why that's Interface Segregation, not accidental).

**As documented in [interview_questions.md](interview_questions.md#3-why-does-logout-clear-the-local-session-even-if-the-network-call-fails):** `AuthRepositoryImpl.logout()` calls `localDataSource.clearSession()` even when the remote logout call throws — a user who taps "Log out" is never left silently signed in locally just because the network happened to fail.

---

## Network Security

### Certificate Pinning — ❌ Not Implemented

`NetworkModule.provideOkHttpClient()` has the hook, commented out:

```kotlin
.retryOnConnectionFailure(true)
// Certificate pinning can be added here for production
// .certificatePinner(certificatePinner)
```

What it would look like once a real backend exists:

```kotlin
val certificatePinner = CertificatePinner.Builder()
    .add("api.bank.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .build()

OkHttpClient.Builder()
    .certificatePinner(certificatePinner)
    .build()
```

### Network Security Config — ❌ Not Configured

There's no `res/xml/network_security_config.xml` anywhere in this repo, and `app/src/main/AndroidManifest.xml` doesn't set `android:networkSecurityConfig` or `android:usesCleartextTraffic` — verified by grep, not assumed. On modern Android (API 28+) cleartext HTTP is blocked by default regardless, so this isn't an active hole today, but it also means there's no explicit pin-set backing the certificate pinning above once that's added:

```xml
<!-- Not present today — res/xml/network_security_config.xml, once BASE_URL is real -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.bank.com</domain>
        <pin-set expiration="2025-12-31">
            <pin digest="SHA-256">base64-encoded-pin</pin>
            <pin digest="SHA-256">backup-pin</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

## Logging Security

### Debug-Only Logging — ✅ Already Applied

```kotlin
// NetworkModule.kt — real code
level = if (BuildConfig.DEBUG) {
    HttpLoggingInterceptor.Level.BODY
} else {
    HttpLoggingInterceptor.Level.NONE
}
```

Full request/response bodies (including the `Authorization` header and login payload) only ever log in a debug build — release builds log nothing from this interceptor.

### Redacting Sensitive Headers — ❌ Not Applied

There's no `redactHeader(...)` call anywhere in `NetworkModule` — verified by grep. Since logging is already fully disabled (`Level.NONE`) in release, this is lower priority than it would be otherwise, but it's still worth doing for local debug-build hygiene (so a screen-recorded debug session or a shared Logcat dump doesn't leak a real token):

```kotlin
// Not present today — worth adding to provideLoggingInterceptor():
HttpLoggingInterceptor().apply {
    redactHeader("Authorization")
    redactHeader("Cookie")
}
```

---

## Security Checklist

### API Key Security
- [x] NDK/C++ storage implemented
- [x] XOR obfuscation applied
- [x] Package name verification implemented (and correctly targets this app's real `applicationId`)
- [x] **JNI export symbols match the Kotlin package** — fixed and verified via `nm` against the compiled `.so`; see [above](#-the-jni-package-mismatch-fixed)
- [x] **CMake build actually registered with Gradle** — fixed; `native-keys.cpp` was never being compiled at all before this
- [ ] Provider-side restrictions configured — N/A, no third-party API key in use yet

### Token Security
- [x] `EncryptedSharedPreferences` for tokens
- [x] Expiry checking (`isTokenExpired()`)
- [x] Cleared unconditionally on logout, even if the network call fails
- [ ] Token *refresh* — `AuthApi.refreshToken()` exists, but nothing in `AuthRepositoryImpl` currently calls it when `isTokenExpired()` is true; expiry is checked but not acted on yet

### Network Security
- [ ] Certificate pinning — commented out, not implemented
- [ ] Explicit network security config — not present (relying on API 28+ defaults only)
- [ ] Header redaction in logs — not applied (mitigated by `Level.NONE` in release)
- [x] No sensitive data logged in release builds

### Build Security
- [x] R8/ProGuard configured for release (`isMinifyEnabled = true` in `app/build.gradle.kts`)
- [x] Native symbols stripped in release (`CMakeLists.txt`'s `-Wl,--gc-sections -s` under `CMAKE_BUILD_TYPE STREQUAL "Release"`)
- [ ] Root/tamper detection — not implemented (optional)

---

## Anti-Patterns to Avoid

```kotlin
// ❌ DON'T: hardcode a key in Kotlin
const val API_KEY = "abc123secret"

// ✅ DO: go through the native provider
val apiKey = apiKeyProvider.getApiKey()

// ❌ DON'T: log a token
Log.d("Auth", "Token: $accessToken")

// ✅ DO: log shape, not content
Log.d("Auth", "Token received, length: ${accessToken.length}")

// ❌ DON'T: plain SharedPreferences for anything security-sensitive
context.getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("token", token).apply()

// ✅ DO: this repo's actual pattern
secureTokenManager.saveTokens(accessToken, refreshToken, expiresIn)
```

---

## Testing Security Code

```kotlin
// A fake for tests — the interface this repo already has makes this trivial
class FakeApiKeyProvider : ApiKeyProvider {
    override fun isAvailable() = true
    override fun getApiKey() = "test_api_key"
    override fun getSecretKey() = "test_secret_key"
    override fun getAppIdentifier() = "test_app_v1"
    override fun validateKeyFormat(key: String) = true
    override fun getApiKeyOrNull() = getApiKey()
    override fun getSecretKeyOrNull() = getSecretKey()
}

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SecurityModule::class]
)
@Module
class TestSecurityModule {
    @Provides
    @Singleton
    fun provideApiKeyProvider(): ApiKeyProvider = FakeApiKeyProvider()
}
```

No `@TestInstallIn` module currently exists in this repo (see [testing.md](testing.md)) — but this is exactly the shape it would take once one's added, and it's *why* `ApiKeyProvider` is an interface rather than every caller depending on `NativeKeyProvider` directly: a test never needs to load the native library at all, regardless of platform or ABI.

---

## Further Reading

- [Android NDK Documentation](https://developer.android.com/ndk)
- [JNI Tips](https://developer.android.com/training/articles/perf-jni) — specifically the section on native method name mangling, directly relevant to the JNI fix documented above
- [Network Security Config](https://developer.android.com/training/articles/security-config)
- [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [OWASP Mobile Security](https://owasp.org/www-project-mobile-top-10/)

---

See [interview_questions.md](interview_questions.md#-f-security) for the interview-style Q&A version of this material, and the main [README](../README.md#-project-status) for overall project status.
