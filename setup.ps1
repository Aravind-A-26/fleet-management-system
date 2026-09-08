$ErrorActionPreference = 'Stop'
if (!(Get-Command node -ErrorAction SilentlyContinue)) { throw 'Install Node.js 22.13+ first.' }
if (!(Get-Command java -ErrorAction SilentlyContinue) -and !$env:JAVA_HOME) { throw 'Install JDK 17+ and set JAVA_HOME first.' }
& node (Join-Path $PSScriptRoot 'scripts/download-maven.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Maven download failed' }
Expand-Archive -LiteralPath (Join-Path $PSScriptRoot '.tools/maven.zip') -DestinationPath (Join-Path $PSScriptRoot '.tools') -Force
& (Join-Path $PSScriptRoot 'build.ps1')
