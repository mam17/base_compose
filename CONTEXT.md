# CONTEXT

## Tech

- Language/Build: Kotlin 2.2.10, AGP 9.4.1, KSP, Java 11, minSdk 24, targetSdk/compileSdk 37
- UI: Jetpack Compose (BOM 2025.12.00) + Material 3. View/XML (viewBinding, Fragment, RecyclerView) remains for the older base classes.
- Navigation: Navigation Compose 2.9.0 (declared, not used yet; only one screen today: `GameScreen`)
- DI: Hilt 2.59.2 (`App` has `@HiltAndroidApp`; `MainActivity` lacks `@AndroidEntryPoint`)
- Architecture: MVVM, flow UI → ViewModel → UseCase → Repository (the `domain/` and `data/` packages exist but are empty)
- Modules: `:app` (only one)
- Root package: `com.example.basecompose`
  - `base/`: shared base classes
  - `data/local`, `data/remote/api`, `data/remote/dto`, `data/repository`
  - `domain/model`, `domain/repository`, `domain/usecase`
  - `presentation/components`, `ui/theme`, `di/`, `utils/`
  - Package root: puzzle game code (`GameScreen`, `GameState`, `PuzzleBoard`, `PuzzleLogic`...)
- Shared base classes:
  - `BaseViewModel`: `isLoading`, `error` (`StateFlow`), `safeLaunch(dispatcher, showLoading, onError, block)`
  - `UiState<T>`: sealed `Idle` / `Loading` / `Success(data)` / `Error(message, throwable)`
  - `BaseComposeDialog`, `BaseAlertDialog` for Compose dialogs
  - `NetworkMonitor` (injected via `AppModule`)
- One-time events: TODO: `Channel` or `SharedFlow`?

## Commands

Windows PowerShell uses `.\gradlew.bat` and `.\scripts\verify.ps1`; macOS/Linux/Git Bash use `./gradlew` and `./scripts/verify.sh`.
Dev machines may lack `JAVA_HOME`/`ANDROID_HOME` and `local.properties`: the verify scripts detect them; when calling Gradle directly, set both variables for that command session.

- Fast compile: `./gradlew :app:compileDebugKotlin`
- Unit tests, one class: `./gradlew :app:testDebugUnitTest --tests "*<TestClass>"`
- Unit tests, whole module: `./gradlew :app:testDebugUnitTest`
- Quick verify (after each ticket): `.\scripts\verify.ps1 -Quick -Changed docs/features/<slug>/changed-files.txt` | `./scripts/verify.sh --quick --changed ...`
- Full verify (before handover): `.\scripts\verify.ps1` | `./scripts/verify.sh` → report in `build/verify-report.txt`
- Enabled verify steps: Compile, Unit tests, Android lint (`lintDebug`), Assemble (`:app:assembleDebug`). No ktlint/detekt/Roborazzi yet.
- Instrumented tests (only with a device, end of pipeline): `./gradlew :app:connectedDebugAndroidTest`

## Testing

- Available: JUnit4 (`src/test`), Compose UI test `ui-test-junit4` + Espresso (`src/androidTest`)
- Missing: `kotlinx-coroutines-test`, Turbine (needed before testing ViewModels/Flows), Robolectric, Truth, a coverage tool
- Compose UI tests live in: `src/androidTest` (no Robolectric yet)
- Shared helpers: none yet. Put them in `app/src/test/java/com/example/basecompose/testing/`
- Test names: unit tests use backticks starting with the test-case ID; androidTest must NOT use spaces (minSdk 24)

## Conventions

- Naming: `XxxViewModel`, `XxxUiState`, `XxxRepository` (interface, `domain/repository`), `XxxRepositoryImpl` (`data/repository`), `FakeXxxRepository` (tests), `XxxUseCase`
- Screens: `XxxScreen` (stateful, takes the ViewModel) and `XxxContent` (stateless, takes state + lambdas)
- ViewModels extend `BaseViewModel` and have `@HiltViewModel` + `@Inject constructor`
- Code comments/KDoc are written in Vietnamese (existing convention in the codebase)

## Known issues

- `BaseViewModel.safeLaunch` defaults to `Dispatchers.IO`. New code must pass an injected dispatcher: `safeLaunch(dispatcher = ioDispatcher) { ... }` to stay testable.
- `GameState` holds state with Compose `mutableStateOf` and is `remember`ed in `GameScreen` (not in a ViewModel), so it is lost on process death.
- `PuzzleLogic` is pure Kotlin logic, testable with JUnit on the JVM.
- Using `hiltViewModel()` in `MainActivity` requires adding `@AndroidEntryPoint`.
- Agent setup is project-scoped: rules in `RULES.md`, skills in `.agents/skills/`, junctioned into `.claude/skills/` (Claude Code) and `.agent/skills/` (Antigravity) by `.agents/link-skills.ps1`. Junctions are absolute paths: re-run the script after moving or cloning the repo.
- `.gitignore` has a `skills/` rule, which also ignores `.agents/skills/`: the skill source is not tracked by git.
- Pipeline artifacts live in `docs/features/<slug>/`; backups in `.agent-backup/` (gitignored).

## Domain glossary

- **Picture** (ảnh): an image cut into k×k pieces.
- **Cut** (kiểu cắt): the number k; each picture has k² pieces.
- **Piece** (mảnh): `pieceId = pic * k² + idx`. Avoid: tile, cell.
- **Cell** (ô): a position on the board; holds a piece or is empty (`EMPTY`).
- **Group** (nhóm): pieces of the same picture placed next to each other in the correct order; they "stick" together and move as one.
- **Completed** (hoàn thành): a picture whose full k×k block is assembled anywhere on the board.
- **Deck** (bộ bài): pieces not yet placed on the board.
- TODO: add other terms (level, hint, undo...) if needed.
