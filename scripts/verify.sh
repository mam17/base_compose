#!/usr/bin/env bash
# One-command verification for agents (macOS / Linux / Git Bash). Does not use git.
#   ./scripts/verify.sh                                -> all modules, all enabled steps
#   ./scripts/verify.sh --changed <changed-files.txt>  -> only modules that contain the listed files
#   ./scripts/verify.sh --quick [--changed <list>]     -> compile + unit tests only
# Needs LF line endings: add "*.sh text eol=lf" to .gitattributes.
set -uo pipefail

# ===== Project config (1 = run, 0 = skip). Enable only tasks the project really has. =====
RUN_COMPILE=1      # compileDebugKotlin / compileKotlin
RUN_UNIT_TEST=1    # testDebugUnitTest / test
RUN_KTLINT=0       # ktlintCheck
RUN_DETEKT=0       # detekt
RUN_LINT=1         # Android lint: lintDebug
RUN_SCREENSHOT=0   # Roborazzi: verifyRoborazziDebug
RUN_ASSEMBLE=1     # $APP_MODULE:assembleDebug
APP_MODULE=":app"
# ==========================================================================================

CHANGED=""; QUICK=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --changed) CHANGED="${2:-}"; shift 2 ;;
    --quick) QUICK=1; shift ;;
    *) echo "Unknown argument: $1"; exit 2 ;;
  esac
done

cd "$(dirname "$0")/.."
mkdir -p build
REPORT="build/verify-report.txt"
MARKER="build/.verify-start"
echo "verify started $(date)" > "$REPORT"
touch "$MARKER"

# ---- Environment: find JDK and Android SDK when missing ----
to_unix() { if command -v cygpath >/dev/null 2>&1; then cygpath -u "$1"; else echo "$1"; fi; }
if [[ -z "${JAVA_HOME:-}" ]] && ! command -v java >/dev/null 2>&1; then
  for c in "/Applications/Android Studio.app/Contents/jbr/Contents/Home" "$HOME/android-studio/jbr" \
           "/opt/android-studio/jbr" "$(to_unix "${LOCALAPPDATA:-/none}")/Programs/Android Studio/jbr" \
           "/c/Program Files/Android/Android Studio/jbr"; do
    if [[ -x "$c/bin/java" || -x "$c/bin/java.exe" ]]; then export JAVA_HOME="$c"; echo "JAVA_HOME=$c (auto)" >> "$REPORT"; break; fi
  done
fi
if ! grep -qs '^[[:space:]]*sdk\.dir' local.properties && [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  for c in "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" "$(to_unix "${LOCALAPPDATA:-/none}")/Android/Sdk"; do
    if [[ -d "$c" ]]; then export ANDROID_HOME="$c"; echo "ANDROID_HOME=$c (auto)" >> "$REPORT"; break; fi
  done
fi

# ---- Modules ----
SETTINGS=$(cat settings.gradle* 2>/dev/null)
build_file() { for f in "$1/build.gradle.kts" "$1/build.gradle"; do [[ -f "$f" ]] && { echo "$f"; return; }; done; }

module_of() { # relative file path -> :gradle:path (empty if none)
  local d; d=$(dirname "$1")
  while [[ "$d" != "." && "$d" != "/" && -n "$d" ]]; do
    if [[ -n "$(build_file "$d")" ]]; then echo ":${d//\//:}"; return; fi
    d=$(dirname "$d")
  done
}

MODULES=()
if [[ -n "$CHANGED" ]]; then
  [[ -f "$CHANGED" ]] || { echo "Change list not found: $CHANGED"; exit 2; }
  while IFS= read -r m; do [[ -n "$m" ]] && MODULES+=("$m"); done < <(
    tr -d '\r' < "$CHANGED" | tr '\\' '/' | sed 's#^\./##' \
      | grep -E '\.(kt|kts|java|xml|toml|gradle|pro)$' \
      | while IFS= read -r f; do module_of "$f"; done | sort -u)
  [[ ${#MODULES[@]} -eq 0 ]] && MODULES=("$APP_MODULE")
else
  while IFS= read -r m; do
    if grep -qE "[\"']${m}[\"']" <<<"$SETTINGS"; then MODULES+=("$m"); fi
  done < <(find . -maxdepth 5 \( -name build -o -name .gradle -o -name buildSrc -o -name build-logic -o -name node_modules \) -prune \
             -o \( -name build.gradle.kts -o -name build.gradle \) -print \
           | sed 's#^\./##' | xargs -I{} dirname {} | grep -v '^\.$' | sed 's#/#:#g; s#^#:#' | sort -u)
fi
echo "modules: ${MODULES[*]:-}" >> "$REPORT"

is_android() { local bf; bf=$(build_file "$(echo "${1#:}" | tr ':' '/')"); [[ -n "$bf" ]] && grep -qi 'android' "$bf"; }

tasks_for() { # $1 = android task, $2 = jvm task ("" = skip)
  local m
  for m in "${MODULES[@]}"; do
    if is_android "$m"; then [[ -n "$1" ]] && printf '%s:%s ' "$m" "$1"
    elif [[ -n "$2" ]]; then printf '%s:%s ' "$m" "$2"; fi
  done
}

# ---- Steps ----
FAILED=()
run_step() { # $1 = name, $2 = tasks
  local name="$1" tasks="$2"
  [[ -z "${tasks// /}" ]] && return
  echo "> $name"
  echo "===== $name : $tasks" >> "$REPORT"
  # shellcheck disable=SC2086
  if ./gradlew --console=plain --continue $tasks >> "$REPORT" 2>&1; then
    echo "  OK   $name"; echo "  OK $name" >> "$REPORT"
  else
    echo "  FAIL $name (see $REPORT)"; echo "  FAIL $name" >> "$REPORT"; FAILED+=("$name")
  fi
}

(( RUN_COMPILE ))   && run_step "Compile"    "$(tasks_for compileDebugKotlin compileKotlin)"
(( RUN_UNIT_TEST )) && run_step "Unit tests" "$(tasks_for testDebugUnitTest test)"
if (( ! QUICK )); then
  (( RUN_KTLINT ))     && run_step "ktlint"       "$(tasks_for ktlintCheck ktlintCheck)"
  (( RUN_DETEKT ))     && run_step "detekt"       "$(tasks_for detekt detekt)"
  (( RUN_LINT ))       && run_step "Android lint" "$(tasks_for lintDebug "")"
  (( RUN_SCREENSHOT )) && run_step "Screenshot"   "$(tasks_for verifyRoborazziDebug "")"
  (( RUN_ASSEMBLE ))   && run_step "Assemble"     "${APP_MODULE}:assembleDebug"
fi

# ---- Failed test result files from this run ----
echo "===== Test failures" >> "$REPORT"
find . -path '*/build/test-results/*' -name '*.xml' -newer "$MARKER" 2>/dev/null \
  | xargs grep -l '<failure' 2>/dev/null | tee -a "$REPORT" | sed 's/^/  failed tests: /' || true

if (( ${#FAILED[@]} )); then echo "VERIFY FAILED: ${FAILED[*]}"; exit 1; fi
echo "VERIFY PASSED"
