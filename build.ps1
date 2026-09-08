param([switch]$SkipInstall)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$mavenExe = Join-Path $projectRoot '.tools/apache-maven-3.9.9/bin/mvn.cmd'
if (!(Test-Path -LiteralPath $mavenExe)) {
  $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
  if (!$mavenCommand) { throw 'Maven 3.9+ is required. Run setup.ps1 or install Maven and add it to PATH.' }
  $mavenExe = $mavenCommand.Source
}
Push-Location (Join-Path $projectRoot 'frontend')
try {
  if (!$SkipInstall) { & npm.cmd ci --ignore-scripts; if($LASTEXITCODE -ne 0){throw 'Dependency installation failed'} }
  & node node_modules/typescript/bin/tsc --noEmit
  if($LASTEXITCODE -ne 0){throw 'Frontend type check failed'}
  & node node_modules/vitest/vitest.mjs run --configLoader native
  if($LASTEXITCODE -ne 0){throw 'Frontend tests failed'}
  & node node_modules/vite/bin/vite.js build --configLoader native
  if($LASTEXITCODE -ne 0){throw 'Frontend build failed'}
} finally { Pop-Location }
Push-Location (Join-Path $projectRoot 'backend')
try {
  & $mavenExe -B -ntp package
  if($LASTEXITCODE -ne 0){throw 'Backend build or tests failed'}
} finally { Pop-Location }
Write-Host 'Build complete. Run .\start.ps1 and open http://localhost:8080.'
