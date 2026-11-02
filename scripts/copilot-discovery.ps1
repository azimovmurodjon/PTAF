<#
Explicit local FNB-ETAF MCP discovery setup; never starts MCP/browser/tests.
Run from the Maven consumer/framework root in IntelliJ Windows PowerShell:
  .\scripts\copilot-discovery.ps1 -Operation mcp-template -ConsumerRoot .
  .\scripts\copilot-discovery.ps1 -Operation mcp-setup -ConsumerRoot .
  .\scripts\copilot-discovery.ps1 -Operation mcp-setup -ConsumerRoot . -RegisterCopilot
  .\scripts\copilot-discovery.ps1 -Operation validate -InputFile <candidate.yml>
  .\scripts\copilot-discovery.ps1 -Operation draft -InputFile <candidate.yml> -ConsumerRoot .
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('mcp-template', 'mcp-setup', 'validate', 'draft')]
    [string]$Operation,
    [string]$InputFile,
    [string]$ConsumerRoot = '.',
    [switch]$RegisterCopilot,
    [string]$CopilotConfigPath
)
$ErrorActionPreference = 'Stop'
if (($Operation -eq 'validate' -or $Operation -eq 'draft') -and [string]::IsNullOrWhiteSpace($InputFile)) {
    throw '-InputFile is required for validate and draft operations.'
}
if ($RegisterCopilot -and $Operation -ne 'mcp-setup') { throw '-RegisterCopilot is supported only with mcp-setup.' }
. (Join-Path $PSScriptRoot 'copilot-common.ps1')
$ConsumerRoot = (Resolve-Path -LiteralPath $ConsumerRoot).Path
$arguments = @($Operation)
switch ($Operation) {
    'mcp-template' { $arguments += @('--root', $ConsumerRoot) }
    'mcp-setup' { $arguments += @('--root', $ConsumerRoot) }
    'validate' { $arguments += @('--input', $InputFile) }
    'draft' { $arguments += @('--input', $InputFile, '--root', $ConsumerRoot) }
}
Invoke-FnbCopilotJava -ProjectRoot $ConsumerRoot -ToolClass 'com.ptaf.copilot.CopilotDiscoveryTool' -ToolArguments $arguments
if ($RegisterCopilot) {
    $parameters = @{ ConsumerRoot = $ConsumerRoot }
    if (-not [string]::IsNullOrWhiteSpace($CopilotConfigPath)) { $parameters['CopilotConfigPath'] = $CopilotConfigPath }
    & (Join-Path $PSScriptRoot 'register-copilot-mcp.ps1') @parameters
}
