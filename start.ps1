param([switch]$Postgres, [switch]$MySql, [switch]$H2)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
if (($Postgres -and $MySql) -or ($H2 -and ($Postgres -or $MySql))) { throw 'Choose one database mode.' }
if (!$Postgres -and !$H2 -and (Test-Path -LiteralPath (Join-Path $projectRoot '.mysql-ready'))) { $MySql = $true }
if ($Postgres -or $MySql) {
  $envPath = Join-Path $projectRoot $(if ($MySql) {'.env.mysql'} else {'.env'})
  if (!(Test-Path -LiteralPath $envPath)) { throw "Configure the database connection file first: $envPath" }
  foreach ($line in Get-Content -LiteralPath $envPath) {
    if ($line -match '^\s*([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), 'Process') }
  }
}
if ($MySql) { $env:SPRING_PROFILES_ACTIVE='mysql' }
if ($H2) {
  foreach($key in @('DB_URL','DB_USER','DB_PASSWORD','SPRING_PROFILES_ACTIVE')) { [Environment]::SetEnvironmentVariable($key,$null,'Process') }
}
$javaExe = 'java'
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) { $javaExe = Join-Path $env:JAVA_HOME 'bin/java.exe' }
$jar = Join-Path $projectRoot 'backend/target/fleet-api-1.0.0.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'Build first: frontend npm ci + npm run build, then backend mvn package. See README.md.' }
Push-Location (Join-Path $projectRoot 'backend')
try { Write-Host 'Fleetline: http://localhost:8080 (Ctrl+C to stop)'; & $javaExe -jar $jar } finally { Pop-Location }

