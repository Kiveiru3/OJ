param(
  [switch]$SkipTests,
  [switch]$SkipCompile
)

$ErrorActionPreference = "Stop"

function Resolve-MavenCommand {
  if (Test-Path ".\mvnw.cmd") {
    return ".\mvnw.cmd"
  }

  $mvn = Get-Command mvn -ErrorAction SilentlyContinue
  if ($null -ne $mvn) {
    return "mvn"
  }

  throw "Maven not found. Install Maven or add mvnw.cmd to project root."
}

$runCompile = -not $SkipCompile
$runTests = -not $SkipTests

if (-not $runCompile -and -not $runTests) {
  Write-Host "No checks selected. Nothing to run."
  exit 0
}

$maven = Resolve-MavenCommand
Write-Host "Using Maven command: $maven"

if ($runCompile) {
  Write-Host "Running compile check..."
  & $maven -DskipTests compile
}

if ($runTests) {
  Write-Host "Running tests..."
  & $maven test
}

Write-Host "Backend checks completed."
