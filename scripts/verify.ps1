# One-command verification for agents (Windows PowerShell 5.1+ / PowerShell 7). Does not use git.
#   .\scripts\verify.ps1                                 -> all modules, all enabled steps
#   .\scripts\verify.ps1 -Changed <changed-files.txt>    -> only modules that contain the listed files
#   .\scripts\verify.ps1 -Quick [-Changed <list>]        -> compile + unit tests only
# Blocked by execution policy? powershell -ExecutionPolicy Bypass -File scripts\verify.ps1 ...
# Keep this file ASCII-only: Windows PowerShell 5.1 misreads UTF-8 without BOM.
param(
    [string]$Changed = "",
    [switch]$Quick
)

# ===== Project config (1 = run, 0 = skip). Enable only tasks the project really has. =====
$RunCompile    = 1   # compileDebugKotlin / compileKotlin
$RunUnitTest   = 1   # testDebugUnitTest / test
$RunKtlint     = 0   # ktlintCheck
$RunDetekt     = 0   # detekt
$RunLint       = 1   # Android lint: lintDebug
$RunScreenshot = 0   # Roborazzi: verifyRoborazziDebug
$RunAssemble   = 1   # <AppModule>:assembleDebug
$AppModule     = ":app"
# ==========================================================================================

Set-Location (Split-Path $PSScriptRoot -Parent)
$root = (Get-Location).Path
New-Item -ItemType Directory -Force build | Out-Null
$report = Join-Path $root "build\verify-report.txt"
[IO.File]::WriteAllText($report, "verify started $(Get-Date -Format s)`r`n")
$start = Get-Date
function Write-Report([string]$text) { [IO.File]::AppendAllText($report, $text + "`r`n") }

# ---- Environment: find JDK and Android SDK when missing ----
if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
    $jbr = @("$env:LOCALAPPDATA\Programs\Android Studio\jbr", "$env:ProgramFiles\Android\Android Studio\jbr") |
        Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
    if ($jbr) { $env:JAVA_HOME = $jbr; Write-Report "JAVA_HOME=$jbr (auto)" }
}
$hasSdkDir = (Test-Path local.properties) -and (Select-String -Path local.properties -Pattern '^\s*sdk\.dir' -Quiet)
if (-not $hasSdkDir -and -not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    $sdk = "$env:LOCALAPPDATA\Android\Sdk"
    if (Test-Path $sdk) { $env:ANDROID_HOME = $sdk; Write-Report "ANDROID_HOME=$sdk (auto)" }
}

# ---- Modules ----
$settings = (Get-ChildItem -File settings.gradle* | ForEach-Object { Get-Content $_.FullName -Raw }) -join "`n"

function Get-BuildFile([string]$dir) {
    foreach ($f in "build.gradle.kts", "build.gradle") {
        $p = Join-Path $dir $f
        if (Test-Path $p) { return $p }
    }
    return $null
}

function Get-ModuleOf([string]$file) {
    $p = $file.Trim() -replace '/', '\'
    if ($p.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { $p = $p.Substring($root.Length) }
    $p = $p.TrimStart('.', '\')
    $d = Split-Path $p -Parent
    while ($d) {
        if (Get-BuildFile $d) { return ":" + ($d -replace '\\', ':') }
        $d = Split-Path $d -Parent
    }
    return $null
}

$modules = @()
if ($Changed) {
    if (-not (Test-Path $Changed)) { Write-Host "Change list not found: $Changed"; exit 2 }
    $modules = Get-Content $Changed |
        Where-Object { $_ -match '\.(kt|kts|java|xml|toml|gradle|pro)$' } |
        ForEach-Object { Get-ModuleOf $_ } | Where-Object { $_ } | Sort-Object -Unique
    if (-not $modules) { $modules = @($AppModule) }
} else {
    $modules = Get-ChildItem -Recurse -Depth 4 -File -Include build.gradle.kts, build.gradle |
        Where-Object { $_.DirectoryName -ne $root -and $_.FullName -notmatch '\\(build|\.gradle|buildSrc|build-logic)\\' } |
        ForEach-Object { ":" + ($_.DirectoryName.Substring($root.Length + 1) -replace '\\', ':') } |
        Where-Object { $settings -match ('["'']' + [regex]::Escape($_) + '["'']') } |
        Sort-Object -Unique
}
Write-Report "modules: $($modules -join ' ')"

function Test-Android([string]$module) {
    $bf = Get-BuildFile ($module.TrimStart(':') -replace ':', '\')
    return [bool]($bf -and (Select-String -Path $bf -Pattern 'android' -Quiet))
}

function Get-Tasks([string]$androidTask, [string]$jvmTask) {
    $tasks = @()
    foreach ($m in $modules) {
        if (Test-Android $m) { if ($androidTask) { $tasks += "${m}:$androidTask" } }
        elseif ($jvmTask) { $tasks += "${m}:$jvmTask" }
    }
    return ,$tasks
}

# ---- Steps ----
$script:failed = @()
function Invoke-Step([string]$name, [string[]]$tasks) {
    if (-not $tasks -or $tasks.Count -eq 0) { return }
    Write-Host "> $name"
    Write-Report "===== $name : $($tasks -join ' ')"
    $cmd = ".\gradlew.bat --console=plain --continue " + ($tasks -join ' ') + " >> `"$report`" 2>&1"
    cmd /c $cmd
    if ($LASTEXITCODE -eq 0) { Write-Host "  OK   $name"; Write-Report "  OK $name" }
    else { Write-Host "  FAIL $name (see build\verify-report.txt)"; Write-Report "  FAIL $name"; $script:failed += $name }
}

if ($RunCompile)  { Invoke-Step "Compile"    (Get-Tasks "compileDebugKotlin" "compileKotlin") }
if ($RunUnitTest) { Invoke-Step "Unit tests" (Get-Tasks "testDebugUnitTest" "test") }
if (-not $Quick) {
    if ($RunKtlint)     { Invoke-Step "ktlint"       (Get-Tasks "ktlintCheck" "ktlintCheck") }
    if ($RunDetekt)     { Invoke-Step "detekt"       (Get-Tasks "detekt" "detekt") }
    if ($RunLint)       { Invoke-Step "Android lint" (Get-Tasks "lintDebug" "") }
    if ($RunScreenshot) { Invoke-Step "Screenshot"   (Get-Tasks "verifyRoborazziDebug" "") }
    if ($RunAssemble)   { Invoke-Step "Assemble"     @("${AppModule}:assembleDebug") }
}

# ---- Failed test result files from this run ----
Write-Report "===== Test failures"
Get-ChildItem -Recurse -File -Filter *.xml -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match '\\build\\test-results\\' -and $_.LastWriteTime -gt $start } |
    Where-Object { Select-String -Path $_.FullName -Pattern '<failure' -Quiet } |
    ForEach-Object { Write-Report $_.FullName; Write-Host "  failed tests: $($_.FullName)" }

if ($script:failed.Count -gt 0) { Write-Host "VERIFY FAILED: $($script:failed -join ', ')"; exit 1 }
Write-Host "VERIFY PASSED"
exit 0
