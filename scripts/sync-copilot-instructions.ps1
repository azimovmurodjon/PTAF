<#
Generates or verifies IntelliJ/GitHub Copilot repository instructions from the FNB-ETAF embedded profile.
Usage: .\scripts\sync-copilot-instructions.ps1 [-Operation sync|verify] [-ConsumerRoot .]
This command is explicit by design and is never called by Maven test or normal framework execution.
#>
param(
    [ValidateSet('sync', 'verify')]
    [string]$Operation = 'sync',
    [string]$ConsumerRoot = '.'
)

$ErrorActionPreference = 'Stop'

mvn -q -DskipTests test-compile dependency:build-classpath '-Dmdep.outputFile=target/copilot-runtime-classpath.txt'
$classPath = "target/classes;$(Get-Content -Raw 'target/copilot-runtime-classpath.txt')"
java -cp $classPath com.ptaf.copilot.CopilotInstructionTool $Operation --root $ConsumerRoot
