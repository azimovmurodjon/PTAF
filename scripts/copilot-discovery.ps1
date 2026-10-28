<#
Local FNB-ETAF Playwright MCP discovery helper.
Usage:
  .\scripts\copilot-discovery.ps1 -Operation mcp-template [-ConsumerRoot .]
  .\scripts\copilot-discovery.ps1 -Operation validate -InputFile <candidate.yml>
  .\scripts\copilot-discovery.ps1 -Operation draft -InputFile <candidate.yml> [-ConsumerRoot .]
This command never starts Playwright MCP or normal FNB-ETAF tests.
#>
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('mcp-template', 'validate', 'draft')]
    [string]$Operation,
    [string]$InputFile,
    [string]$ConsumerRoot = '.'
)
$ErrorActionPreference = 'Stop'

if (($Operation -eq 'validate' -or $Operation -eq 'draft') -and [string]::IsNullOrWhiteSpace($InputFile)) {
    throw '-InputFile is required for validate and draft operations.'
}

mvn -q -DskipTests test-compile dependency:build-classpath '-Dmdep.outputFile=target/copilot-runtime-classpath.txt'
$classPath = "target/classes;$(Get-Content -Raw 'target/copilot-runtime-classpath.txt')"

switch ($Operation) {
    'mcp-template' {
        java -cp $classPath com.ptaf.copilot.CopilotDiscoveryTool mcp-template --root $ConsumerRoot
    }
    'validate' {
        java -cp $classPath com.ptaf.copilot.CopilotDiscoveryTool validate --input $InputFile
    }
    'draft' {
        java -cp $classPath com.ptaf.copilot.CopilotDiscoveryTool draft --input $InputFile --root $ConsumerRoot
    }
}
