param(
  [Parameter(Mandatory = $true)]
  [string]$Token,

  [string]$BaseUrl = "http://localhost:8082/api",
  [string]$DataFile = "scripts/data/luogu_offline_problems_1000_hidden_20_dense.json",
  [int]$Start = 1,
  [int]$End = 1000,
  [int]$BatchSize = 20,
  [int]$Delay = 80,
  [switch]$ContinueOnError,
  [switch]$DryRun
)

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
  $nodeArgs = @(
    "scripts/import_in_order_batches.js",
    "--base-url", $BaseUrl,
    "--input", $DataFile,
    "--token", $Token,
    "--start", "$Start",
    "--end", "$End",
    "--batch-size", "$BatchSize",
    "--delay", "$Delay"
  )

  if ($ContinueOnError) {
    $nodeArgs += "--continue-on-error"
  }
  if ($DryRun) {
    $nodeArgs += "--dry-run"
  }

  node @nodeArgs
  exit $LASTEXITCODE
}
finally {
  Pop-Location
}
