# 🎨 Jetpack Compose Guidelines

## Overview

BankApp uses **Jetpack Compose** with **Material 3** for every screen, backed by one shared design system in `core:ui`. This doc covers how theming, components, screens, side effects, and navigation are meant to be written across all nine feature modules — with real code from the repo, not hypothetical snippets.

Where the codebase hasn't caught up to a guideline yet, that's called out explicitly rather than glossed over — see [Current Adoption Status](#-current-adoption-status) at the bottom.

## Project Structure

```
core/ui/
└── src/main/java/
    ├── theme/
    │   ├── Color.kt          # Named color tokens (Primary, Secondary, Tertiary, ...)
    │   ├── Type.kt            # BankTypography — full Material 3 type scale
    │   └── Theme.kt           # BankAppTheme composable
    └── components/
        ├── BankButton.kt      # PrimaryButton, SecondaryButton, TertiaryButton
        ├── BankTextField.kt   # BankTextField, PasswordTextField
        ├── LoadingIndicator.kt # LoadingIndicator, FullScreenLoading, BouncingDotsLoading
        ├── ErrorView.kt       # ErrorView, CompactErrorView
        └── RecipeCard.kt      # Leftover from the original template — unused, safe to remove
```

Every feature module depends on `core:ui` (which re-exports the Compose BOM, Material 3, and `lifecycle-runtime-compose` as `api`), so a feature never needs its own copy of a button or text field — it composes these directly.

## Theming

### Color Scheme

Colors are named tokens in `theme/Color.kt`, wired into light/dark `ColorScheme`s in `theme/Theme.kt`:

```kotlin
// Color.kt
val Primary = Color(0xFFD4654A)
val PrimaryLight = Color(0xFFE8917A)
val PrimaryDark = Color(0xFFB04A32)
// ... Secondary, Tertiary, Background, Surface, Error, Success/Warning/Info, Outline/Divider

// Theme.kt
@Composable
fun BankAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled to keep brand colors consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BankTypography,
        content = content
    )
}
```

**Rule:** never reach for a raw `Color(0xFF...)` literal inside a feature module. If the palette is missing a token you need, add it to `core/ui/theme/Color.kt` — don't hardcode it at the call site.

### Typography

`BankTypography` fills out the entire Material 3 type scale (`displayLarge` → `labelSmall`) in `theme/Type.kt`, currently on system serif/sans-serif fallback fonts:

```kotlin
val BankTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    // ...
)
```

**Rule:** reach for `MaterialTheme.typography.*` in every `Text`, never a one-off `fontSize =`. Every feature screen in this repo already follows this — see `AccountScreen`'s `MaterialTheme.typography.titleMedium` on the account holder name.

## Component Design

### Reusable Buttons

`core:ui` ships three button variants so features don't hand-roll `Button`/`OutlinedButton`/`TextButton` styling:

```kotlin
// BankButton.kt
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
```

Every button (and `BankTextField`/`PasswordTextField`) exposes its own `isLoading`/`isError` flag rather than making the caller wrap it in a loading overlay — see `AuthScreen`'s `PrimaryButton(isLoading = uiState.isLoading)`.

### State Hoisting

Every reusable component in `core:ui` takes its value and an `onValueChange`/`onClick` lambda — it never owns `remember { mutableStateOf(...) }` for data the caller needs to read.

```kotlin
// ❌ Don't: state trapped inside the component, caller can't read or drive it
@Composable
fun SearchField() {
    var query by remember { mutableStateOf("") }
    TextField(value = query, onValueChange = { query = it })
}

// ✅ Do: this is the real BankTextField signature — state lives in the caller (the ViewModel's UiState)
@Composable
fun BankTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    // ...
)
```

In every feature, the "caller" holding that state is the `ViewModel`'s `UiState` — e.g. `AuthScreen` passes `uiState.email` / `uiState.password` straight through, and the ViewModel is the only thing that ever updates them (via `onEvent(AuthEvent.EmailChanged(...))`).

## Screen Architecture

**Recommended pattern** — split a screen into a stateful entry point and a stateless, previewable content composable:

```kotlin
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = hiltViewModel(),
    onAccountClick: (Account) -> Unit = {}
) {
    // 1. Collect state
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 2. Delegate to stateless content — this is what you'd put behind a @Preview
    AccountContent(
        uiState = uiState,
        onAccountClick = onAccountClick,
        onRetry = { viewModel.onEvent(AccountEvent.LoadAccounts) }
    )
}

@Composable
private fun AccountContent(
    uiState: AccountUiState,
    onAccountClick: (Account) -> Unit,
    onRetry: () -> Unit
) {
    // Pure UI — no ViewModel, no Hilt, fully previewable with fake data
}
```

The stateless `*Content` split matters because it's the only way to `@Preview` a screen's loading/error/success states without a running ViewModel or a Hilt graph.

> **Where this repo stands today:** every `*Screen.kt` in `features/*` (e.g. `AccountScreen`, `AuthScreen`) currently inlines the `when { isLoading -> ...; error -> ...; else -> ... }` branching directly in the stateful composable rather than delegating to a private `*Content`. It works and is simple to read, but it means none of these screens have a `@Preview` yet. Splitting out `*Content` is the natural next refactor — see [Current Adoption Status](#-current-adoption-status).

## Side Effects

### LaunchedEffect

Used for one-shot work triggered by composition or a state change — this is real, shipping code from `AuthScreen`:

```kotlin
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Re-runs only when isLoggedIn flips — navigates out once, not on every recomposition
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    // ... rest of the screen
}
```

Keying `LaunchedEffect` on the specific piece of state that should trigger it (`uiState.isLoggedIn`, not `Unit`) is what keeps this from re-firing on every unrelated state update.

### rememberCoroutineScope

For UI-driven one-off work that isn't tied to composition lifecycle — most commonly, showing a `Snackbar` from a click handler:

```kotlin
@Composable
fun SomeScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        PrimaryButton(text = "Copy account number", onClick = {
            scope.launch { snackbarHostState.showSnackbar("Copied") }
        })
    }
}
```

Not used anywhere in this repo yet — every screen currently surfaces errors as inline `Text`/`ErrorView` rather than a `Snackbar`. Reach for this pattern once a screen needs a transient, non-blocking confirmation (e.g. "Transfer sent", "Copied to clipboard").

## Animations

`core:ui` already has one real animated component — `BouncingDotsLoading` in `LoadingIndicator.kt`:

```kotlin
@Composable
fun BouncingDotsLoading(
    dotColor: Color = MaterialTheme.colorScheme.primary,
    dotSize: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            Box(modifier = Modifier.size(dotSize.dp).scale(scale).background(dotColor, CircleShape))
        }
    }
}
```

**Recommended pattern** for switching between a screen's loading/content/error states without a hard cut:

```kotlin
AnimatedVisibility(
    visible = uiState.isLoading,
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically()
) {
    FullScreenLoading()
}
```

Every screen currently uses a plain `when { }` branch (an instant swap, no `AnimatedVisibility`) — fine for now, worth revisiting once the UI needs to feel less abrupt.

## Preview Best Practices

**Recommended pattern** — every composable that renders real data should ship light + dark previews with realistic (not `"test"` / `"foo"`) fake data:

```kotlin
@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun AccountListItemPreview() {
    BankAppTheme {
        AccountListItem(account = previewAccount, onClick = {})
    }
}

private val previewAccount = Account(
    id = "acc_1",
    accountNumber = "•••• 4821",
    accountHolderName = "Mirza Adil",
    accountType = AccountType.SAVINGS,
    balance = 12_450.32,
    currency = "USD",
    status = AccountStatus.ACTIVE
)
```

`core:ui`'s own components (`ErrorView`, `LoadingIndicator`, `BankTextField`) already do this — every one of them ships a light and dark `@Preview`. No feature screen does yet, since that depends on the `Screen`/`Content` split above landing first.

## Navigation

There's no `NavHost` in `app/` yet (see the main [README](../README.md#-project-status) and [architecture.md](architecture.md)). This is the target shape, built from the real screen composables that already exist in each feature:

```kotlin
@Composable
fun BankNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.AUTH
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen()
        }

        composable(Routes.ACCOUNTS) {
            AccountScreen(onAccountClick = { account ->
                navController.navigate("${Routes.ACCOUNT_DETAIL}/${account.id}")
            })
        }

        composable(
            route = "${Routes.ACCOUNT_DETAIL}/{accountId}",
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            // AccountDetailScreen(accountId = backStackEntry.arguments?.getString("accountId").orEmpty())
        }
    }
}
```

Every feature already exposes exactly the `onXClick`/`onXSuccess` lambda parameters this needs (`AuthScreen(onLoginSuccess = ...)`, `AccountScreen(onAccountClick = ...)`) — wiring the graph is a matter of writing this file, not changing any feature module.

---

## ✅ Current Adoption Status

| Guideline | Status in this repo |
|:----------|:---------------------|
| Shared theme (`BankAppTheme`, `BankTypography`, color tokens) | ✅ In place, used by every screen |
| Reusable components (`PrimaryButton`, `BankTextField`, `ErrorView`, `LoadingIndicator`) | ✅ In place, used by every screen |
| State hoisting in components | ✅ Every `core:ui` component is stateless |
| `Screen` / stateless `Content` split | ❌ Not yet — every screen inlines its `when` branching |
| `collectAsStateWithLifecycle()` | ❌ Screens currently use plain `collectAsState()` — `lifecycle-runtime-compose` is already on the classpath via `core:ui`, so this is a drop-in swap |
| `LaunchedEffect` for one-shot navigation/side effects | ✅ Used in `AuthScreen` |
| `rememberCoroutineScope` + `Snackbar` | ❌ Not yet used anywhere |
| Animated state transitions (`AnimatedVisibility`) | ⚠️ Partial — one component (`BouncingDotsLoading`) animates; screens swap states instantly |
| `@Preview` functions | ⚠️ Partial — `core:ui` components have previews; feature screens don't yet |
| Navigation graph (`NavHost`) | ❌ Not implemented — `app/` has no navigation wiring at all |

---

See [architecture.md](architecture.md) for the Clean Architecture layering these screens sit on top of, and the main [README](../README.md) for full project status.
