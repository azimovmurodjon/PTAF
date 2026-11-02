# Explicit local script contracts; synthetic files only; no network/browser or user profile edits.
param([string]$ScriptsRoot = (Split-Path -Parent $PSScriptRoot))
$ErrorActionPreference = 'Stop'
function Assert-True([bool]$Condition, [string]$Message) { if (-not $Condition) { throw $Message } }
$cases = 0
Get-ChildItem -LiteralPath $ScriptsRoot -Filter '*.ps1' | ForEach-Object {
    $tokens = $null; $errors = $null
    [System.Management.Automation.Language.Parser]::ParseFile($_.FullName, [ref]$tokens, [ref]$errors) | Out-Null
    Assert-True ($errors.Count -eq 0) ('Parse failed: ' + $_.Name)
    $cases++
}
$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ('fnb copilot script tests ' + [Guid]::NewGuid().ToString('N'))
$repo = Join-Path $temporaryRoot 'consumer with spaces'
$personal = Join-Path $temporaryRoot 'personal/mcp.json'
$sourceDir = Join-Path $repo '.fnb-etaf/copilot'
New-Item -ItemType Directory -Path $sourceDir -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path -Parent $personal) -Force | Out-Null
$sourcePath = Join-Path $sourceDir 'mcp.json'
$registration = Join-Path $ScriptsRoot 'register-copilot-mcp.ps1'
try {
    [IO.File]::WriteAllText($sourcePath, '{"servers":{"fnb-etaf-playwright-local":{"command":"npx","args":["-y","@playwright/mcp@0.0.83","--allowed-origins=https://one.example.invalid","--headless","--isolated"]}}}')
    [IO.File]::WriteAllText($personal, '{"otherSetting":"keep","servers":{"other-server":{"command":"keep-other"}}}')
    & $registration -ConsumerRoot $repo -CopilotConfigPath $personal
    $after = Get-Content -LiteralPath $personal -Raw | ConvertFrom-Json
    Assert-True ($after.otherSetting -eq 'keep') 'Other top-level setting changed.'
    Assert-True ($after.servers.'other-server'.command -eq 'keep-other') 'Other server changed.'
    Assert-True ($after.servers.'fnb-etaf-playwright-local'.command -eq 'npx') 'Framework server was not merged.'
    Assert-True (@(Get-ChildItem -LiteralPath (Split-Path -Parent $personal) -Filter '*.fnb-etaf-backup-*').Count -eq 1) 'No backup created.'
    $cases += 4
    $text = [IO.File]::ReadAllText($sourcePath).Replace('one.example.invalid', 'two.example.invalid')
    [IO.File]::WriteAllText($sourcePath, $text)
    & $registration -ConsumerRoot $repo -CopilotConfigPath $personal
    $after = Get-Content -LiteralPath $personal -Raw | ConvertFrom-Json
    Assert-True ($after.servers.'fnb-etaf-playwright-local'.args -contains '--allowed-origins=https://two.example.invalid') 'Target update failed.'
    Assert-True ($after.servers.'other-server'.command -eq 'keep-other') 'Other server changed during update.'
    $cases += 2
    $insideRepo = Join-Path $repo '.github/mcp.json'
    $rejected = $false
    try { & $registration -ConsumerRoot $repo -CopilotConfigPath $insideRepo } catch { $rejected = $true }
    Assert-True $rejected 'Repository registration must be refused.'
    Assert-True (-not (Test-Path -LiteralPath $insideRepo)) 'A workspace MCP file was created.'
    $cases += 2
    [IO.File]::WriteAllText($personal, 'invalid existing json')
    $rejected = $false
    try { & $registration -ConsumerRoot $repo -CopilotConfigPath $personal } catch { $rejected = $true }
    Assert-True $rejected 'Invalid personal JSON must be refused.'
    Assert-True ([IO.File]::ReadAllText($personal) -eq 'invalid existing json') 'Invalid personal JSON was overwritten.'
    $cases += 2
    [IO.File]::WriteAllText($sourcePath, [IO.File]::ReadAllText($sourcePath).Replace('https://two.example.invalid', 'https://<APPROVED_TARGET_ORIGIN>'))
    $rejected = $false
    try { & $registration -ConsumerRoot $repo -CopilotConfigPath $personal } catch { $rejected = $true }
    Assert-True $rejected 'Template placeholder must be refused.'
    $cases++
    # Native Maven failure must stop Java execution instead of using a stale classpath.
    . (Join-Path $ScriptsRoot 'copilot-common.ps1')
    $failRepo = Join-Path $temporaryRoot 'failed-maven'
    New-Item -ItemType Directory -Path $failRepo -Force | Out-Null
    [IO.File]::WriteAllText((Join-Path $failRepo 'pom.xml'), '<project/>')
    if ([IO.Path]::DirectorySeparatorChar -eq '/') {
        $wrapper = Join-Path $failRepo 'mvnw'
        [IO.File]::WriteAllText($wrapper, "#!/bin/sh`nexit 19`n")
        & chmod '+x' $wrapper
    } else {
        $wrapper = Join-Path $failRepo 'mvnw.cmd'
        [IO.File]::WriteAllText($wrapper, "@exit /b 19`r`n")
    }
    $rejected = $false
    try { Invoke-FnbCopilotJava -ProjectRoot $failRepo -ToolClass 'not.a.RealClass' -ToolArguments @('never-run') } catch { $rejected = $_.Exception.Message -like 'Maven failed*' }
    Assert-True $rejected 'Maven failure did not stop the helper before Java.'
    $cases++
    Write-Host "PowerShell script contracts passed: $cases checks. Synthetic files only."
} finally { Remove-Item -LiteralPath $temporaryRoot -Recurse -Force }
