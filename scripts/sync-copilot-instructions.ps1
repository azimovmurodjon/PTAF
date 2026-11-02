<#
Explicit sync/verification of instructions from the embedded FNB-ETAF profile.
Not invoked by normal Maven/test/browser runtime. Supports Artifactory consumers.
Usage: .\scripts\sync-copilot-instructions.ps1 [-Operation sync|verify] [-ConsumerRoot .]
#>
param(
    [ValidateSet('sync', 'verify')][string]$Operation = 'sync',
    [string]$ConsumerRoot = '.'
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'copilot-common.ps1')
$ConsumerRoot = (Resolve-Path -LiteralPath $ConsumerRoot).Path
Invoke-FnbCopilotJava -ProjectRoot $ConsumerRoot -ToolClass 'com.ptaf.copilot.CopilotInstructionTool' -ToolArguments @($Operation, '--root', $ConsumerRoot)
