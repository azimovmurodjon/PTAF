<#
Registers ONLY fnb-etaf-playwright-local in the personal GitHub Copilot IntelliJ
mcp.json. Explicitly requested by -RegisterCopilot or direct invocation.
Does not start/restart any server, edit workspace .github, or replace other servers.
#>
param(
    [string]$ConsumerRoot = '.',
    [string]$CopilotConfigPath
)
$ErrorActionPreference = 'Stop'
$serverName = 'fnb-etaf-playwright-local'
$root = (Resolve-Path -LiteralPath $ConsumerRoot).Path
$source = Join-Path $root '.fnb-etaf/copilot/mcp.json'
if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw 'Generate the local selected-origin MCP configuration with mcp-setup first.' }
function Assert-NoReparsePoint([string]$Path) {
    $current = [IO.Path]::GetFullPath($Path)
    while (-not [string]::IsNullOrWhiteSpace($current)) {
        if (Test-Path -LiteralPath $current) {
            $item = Get-Item -LiteralPath $current -Force
            if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                throw 'Refusing an MCP configuration path with a symlink or junction.'
            }
        }
        $parent = [IO.Path]::GetDirectoryName($current)
        if ($parent -eq $current) { break }
        $current = $parent
    }
}
Assert-NoReparsePoint $source
try { $localConfig = Get-Content -LiteralPath $source -Raw | ConvertFrom-Json } catch { throw 'Local MCP JSON is invalid. Regenerate it; configuration content was not logged.' }
if ($null -eq $localConfig.servers) { throw 'Local configuration must contain a servers mapping.' }
$entryProperty = $localConfig.servers.PSObject.Properties[$serverName]
if ($null -eq $entryProperty) { throw 'The governed local Playwright server was not found.' }
$entry = $entryProperty.Value
$originArguments = @($entry.args | Where-Object { $_ -like '--allowed-origins=*' })
if ($originArguments.Count -ne 1 -or $originArguments[0] -match '[<>]' -or $originArguments[0] -notlike '--allowed-origins=https://*') {
    throw 'Run mcp-setup with an approved selection first; the template placeholder cannot be registered.'
}
if ([string]::IsNullOrWhiteSpace($CopilotConfigPath)) {
    if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
        throw 'Use the exact personal file opened by Copilot Add More Tools with -CopilotConfigPath. Automatic path detection is for Windows only.'
    }
    $CopilotConfigPath = Join-Path $env:LOCALAPPDATA 'github-copilot/intellij/mcp.json'
}
$destination = [IO.Path]::GetFullPath($CopilotConfigPath)
$repoPrefix = $root.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
if ($destination.StartsWith($repoPrefix, [StringComparison]::OrdinalIgnoreCase) -or $destination -eq $root) {
    throw 'Copilot registration must use a personal file outside the repository, not .github or a workspace path.'
}
Assert-NoReparsePoint $destination
if (Test-Path -LiteralPath $destination) {
    if (-not (Test-Path -LiteralPath $destination -PathType Leaf)) { throw 'Copilot configuration path is not a regular file.' }
    try { $personal = Get-Content -LiteralPath $destination -Raw | ConvertFrom-Json } catch { throw 'Existing Copilot JSON is invalid. Nothing was overwritten; fix it in Copilot first.' }
    if ($null -eq $personal -or $personal -is [Array] -or $personal -is [string]) { throw 'Existing Copilot JSON must be an object.' }
} else { $personal = [pscustomobject]@{} }
if ($null -eq $personal.PSObject.Properties['servers']) { $personal | Add-Member -NotePropertyName 'servers' -NotePropertyValue ([pscustomobject]@{}) }
if ($null -eq $personal.servers -or $personal.servers -is [Array] -or $personal.servers -is [string]) { throw 'Existing Copilot servers must be an object. Nothing was overwritten.' }
# Add-Member -Force updates this server only; all other entries/top-level fields remain.
$personal.servers | Add-Member -NotePropertyName $serverName -NotePropertyValue $entry -Force
$content = $personal | ConvertTo-Json -Depth 100
$directory = [IO.Path]::GetDirectoryName($destination)
[IO.Directory]::CreateDirectory($directory) | Out-Null
Assert-NoReparsePoint $destination
$temporary = Join-Path $directory ('.fnb-etaf-register-' + [Guid]::NewGuid().ToString('N') + '.tmp')
$backup = $destination + '.fnb-etaf-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N')
try {
    [IO.File]::WriteAllText($temporary, $content + [Environment]::NewLine, (New-Object System.Text.UTF8Encoding($false)))
    if (Test-Path -LiteralPath $destination) {
        # Replace is atomic on supported local filesystems and keeps a private backup.
        [IO.File]::Replace($temporary, $destination, $backup)
        Write-Host 'Existing personal configuration backed up locally; other servers preserved.'
    } else { [IO.File]::Move($temporary, $destination) }
} finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
Write-Host "Registered $serverName in the personal Copilot configuration. No URL was printed."
Write-Host 'Stop/start only this server in Copilot Tools to load the selected target; keep individual approvals. No browser was started by this script.'
