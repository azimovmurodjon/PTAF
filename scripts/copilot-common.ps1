# Shared explicit Copilot CLI support. Dot-source from the public helper scripts.
# Windows PowerShell 5.1 and PowerShell 7 compatible. Does not run any tests/browser.
function Invoke-FnbCopilotJava {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$ToolClass,
        [Parameter(Mandatory = $true)][string[]]$ToolArguments
    )
    $root = (Resolve-Path -LiteralPath $ProjectRoot -ErrorAction Stop).Path
    if (-not (Test-Path -LiteralPath (Join-Path $root 'pom.xml') -PathType Leaf)) {
        throw 'Run this command against the Maven project root containing pom.xml.'
    }
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($null -eq $javaCommand) { throw 'Java is not available in this terminal. Configure Java 21 and reopen IntelliJ.' }
    $maven = $null
    foreach ($wrapper in @('mvnw.cmd', 'mvnw')) {
        $candidate = Join-Path $root $wrapper
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { $maven = $candidate; break }
    }
    if ($null -eq $maven) {
        foreach ($name in @('mvn.cmd', 'mvn')) {
            $command = Get-Command $name -ErrorAction SilentlyContinue
            if ($null -ne $command) { $maven = $command.Source; break }
        }
    }
    if ([string]::IsNullOrWhiteSpace($maven)) {
        throw 'Maven was not found. Install approved Maven and add its bin directory to PATH, or supply the approved Maven Wrapper. IntelliJ bundled Maven is not automatically on the terminal PATH.'
    }
    Push-Location -LiteralPath $root
    try {
        # Runtime scope includes the framework JAR in an Artifactory consumer.
        # compile, not test-compile: testers need no framework contract-test sources.
        & $maven '-q' '-DskipTests' 'compile' 'dependency:build-classpath' '-Dmdep.includeScope=runtime' '-Dmdep.outputFile=target/copilot-runtime-classpath.txt' | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Maven failed. No Copilot Java tool was run; resolve the build/dependency error first.' }
        $classpathFile = Join-Path $root 'target/copilot-runtime-classpath.txt'
        if (-not (Test-Path -LiteralPath $classpathFile -PathType Leaf)) { throw 'Maven did not create the runtime classpath file.' }
        $dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
        $classPath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
        & $javaCommand.Source '-cp' $classPath $ToolClass @ToolArguments | Out-Host
        if ($LASTEXITCODE -ne 0) { throw 'Copilot tool failed. No browser or MCP server was started. For ClassNotFoundException in a consumer, update to the approved framework artifact containing this tool.' }
    }
    finally { Pop-Location }
}
