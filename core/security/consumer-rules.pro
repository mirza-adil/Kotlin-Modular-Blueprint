# Consumer ProGuard rules for core:security module
# These rules are automatically included when this module is used as a dependency

# ============================================================================
# Native Key Provider - Essential Rules
# ============================================================================
# JNI methods must have exact names - cannot be obfuscated
-keep class com.mirza.security.di.NativeKeyProvider {
    native <methods>;
    public static ** Companion;
}

# Keep static initializer that loads the native library
-keepclassmembers class com.mirza.security.di.NativeKeyProvider {
    static <clinit>;
}

# ============================================================================
# Public API
# ============================================================================
# Keep interface for dependency injection
-keep interface com.mirza.security.di.ApiKeyProvider {
    *;
}

# Keep exception for proper error handling
-keep class com.mirza.security.di.NativeLibraryNotLoadedException {
    *;
}
