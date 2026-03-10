param(
  [switch]$SkipBackendChecks,
  [switch]$SkipFrontendLint,
  [switch]$SkipFrontendBuild,
  [switch]$SkipFrontendSmoke,
  [string]$SmokeBaseUrl = "http://localhost:8082",
  [string]$SmokeApiPrefix = "",
  [string]$SmokeUsername = "admin2",
  [string]$SmokePassword = "admin123"
)

$ErrorActionPreference = "Stop"

function Run-Step {
  param(
    [string]$Name,
    [scriptblock]$Action
  )

  Write-Host ""
  Write-Host "=== $Name ==="
  & $Action
  Write-Host "=== $Name done ==="
}

function Probe-Backend {
  param([string]$BaseUrl)

  $base = $BaseUrl.TrimEnd("/")
  $probeUrls = @(
    "$base/system/public-configs",
    "$base/api/system/public-configs"
  )

  foreach ($url in $probeUrls) {
    try {
      $resp = Invoke-RestMethod -Method Get -Uri $url -TimeoutSec 5
      if ($null -ne $resp -and [int]$resp.code -eq 200) {
        Write-Host "Backend probe OK: $url"
        return $true
      }
    } catch {
      continue
    }
  }
  return $false
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $repoRoot "onlinejudge-backend"
$frontendDir = Join-Path $repoRoot "onlinejudge-frontend"

if (-not (Test-Path $backendDir)) {
  throw "Backend directory not found: $backendDir"
}
if (-not (Test-Path $frontendDir)) {
  throw "Frontend directory not found: $frontendDir"
}

if (-not $SkipBackendChecks) {
  Run-Step "Backend checks (compile + test)" {
    Push-Location $backendDir
    try {
      & ".\scripts\run_backend_checks.ps1"
    } finally {
      Pop-Location
    }
  }
} else {
  Write-Host "Skip backend checks."
}

if (-not $SkipFrontendLint) {
  Run-Step "Frontend lint" {
    Push-Location $frontendDir
    try {
      & npm run lint
    } finally {
      Pop-Location
    }
  }
} else {
  Write-Host "Skip frontend lint."
}

if (-not $SkipFrontendBuild) {
  Run-Step "Frontend build" {
    Push-Location $frontendDir
    try {
      & npm run build
    } finally {
      Pop-Location
    }
  }
} else {
  Write-Host "Skip frontend build."
}

if (-not $SkipFrontendSmoke) {
  if (-not (Probe-Backend -BaseUrl $SmokeBaseUrl)) {
    throw "Backend is not reachable for smoke test. Start backend service first. BaseUrl=$SmokeBaseUrl"
  }

  Run-Step "Frontend smoke test" {
    Push-Location $frontendDir
    $oldBaseUrl = $env:SMOKE_BASE_URL
    $oldPrefix = $env:SMOKE_API_PREFIX
    $oldUser = $env:SMOKE_USERNAME
    $oldPass = $env:SMOKE_PASSWORD
    try {
      $env:SMOKE_BASE_URL = $SmokeBaseUrl
      if ([string]::IsNullOrWhiteSpace($SmokeApiPrefix)) {
        Remove-Item Env:SMOKE_API_PREFIX -ErrorAction SilentlyContinue
      } else {
        $env:SMOKE_API_PREFIX = $SmokeApiPrefix
      }
      $env:SMOKE_USERNAME = $SmokeUsername
      $env:SMOKE_PASSWORD = $SmokePassword

      & npm run smoke:test
    } finally {
      if ($null -eq $oldBaseUrl) { Remove-Item Env:SMOKE_BASE_URL -ErrorAction SilentlyContinue } else { $env:SMOKE_BASE_URL = $oldBaseUrl }
      if ($null -eq $oldPrefix) { Remove-Item Env:SMOKE_API_PREFIX -ErrorAction SilentlyContinue } else { $env:SMOKE_API_PREFIX = $oldPrefix }
      if ($null -eq $oldUser) { Remove-Item Env:SMOKE_USERNAME -ErrorAction SilentlyContinue } else { $env:SMOKE_USERNAME = $oldUser }
      if ($null -eq $oldPass) { Remove-Item Env:SMOKE_PASSWORD -ErrorAction SilentlyContinue } else { $env:SMOKE_PASSWORD = $oldPass }
      Pop-Location
    }
  }
} else {
  Write-Host "Skip frontend smoke."
}

Write-Host ""
Write-Host "All selected acceptance checks passed."
