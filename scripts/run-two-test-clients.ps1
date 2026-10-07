param(
    [Parameter(Mandatory = $true)]
    [Guid]$Session,
    [ValidateRange(1, 2147483647)]
    [int]$Requests = 200,
    [string]$HostName = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$Port = 5050,
    [ValidateSet("step", "snapshot", "queue-full")]
    [string]$Scenario = "step",
    [string]$Output = "test-results"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$clientA = $null
$clientB = $null
Push-Location $projectRoot
try {
    $javaPath = $null
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomePath = $env:JAVA_HOME.Trim().Trim('"')
        $candidate = Join-Path $javaHomePath "bin\java.exe"
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $javaPath = (Get-Item -LiteralPath $candidate).FullName
        }
    }
    if ([string]::IsNullOrWhiteSpace($javaPath)) {
        $javaCommand = Get-Command java.exe -CommandType Application -ErrorAction Stop |
            Select-Object -First 1
        $javaPath = $javaCommand.Path
    }
    if ([string]::IsNullOrWhiteSpace($javaPath) -or
        -not (Test-Path -LiteralPath $javaPath -PathType Leaf)) {
        throw "Cannot locate java.exe. Set JAVA_HOME to your Java 26 JDK folder."
    }
    Write-Host "Java executable: $javaPath"

    & .\mvnw.cmd -pl test-client -am package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed with exit code $LASTEXITCODE"
    }

    $outputPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $Output))
    New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

    # Clear only client outputs. The running server owns server.csv.
    foreach ($fileName in @(
        "client-A.csv", "client-B.csv", "summary-A.csv", "summary-B.csv", "summary.csv"
    )) {
        $filePath = Join-Path $outputPath $fileName
        if (Test-Path -LiteralPath $filePath) {
            Remove-Item -LiteralPath $filePath -Force
        }
    }

    $classPath = @(
        (Join-Path $projectRoot "test-client\target\classes"),
        (Join-Path $projectRoot "protocol\target\classes"),
        (Join-Path $projectRoot "test-client\target\dependency\*")
    ) -join ";"

    function Start-RapidClient([string]$Label) {
        $arguments = @(
            "-cp", "`"$classPath`"",
            "testclient.RapidTestClientMain",
            "--client=$Label",
            "--requests=$Requests",
            "--session=$Session",
            "--host=$HostName",
            "--port=$Port",
            "--scenario=$Scenario",
            "--output=`"$outputPath`""
        )

        # Keep the original process handle so its exit status remains available.
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo.FileName = $javaPath
        $process.StartInfo.Arguments = $arguments -join " "
        $process.StartInfo.WorkingDirectory = $projectRoot
        $process.StartInfo.UseShellExecute = $false
        try {
            if (-not $process.Start()) {
                throw "Unable to start rapid client $Label"
            }
        }
        catch {
            $process.Dispose()
            throw
        }
        return $process
    }

    $clientA = Start-RapidClient "A"
    $clientB = Start-RapidClient "B"
    $clientA.WaitForExit()
    $clientB.WaitForExit()
    $exitA = $clientA.ExitCode
    $exitB = $clientB.ExitCode
    if ($null -eq $exitA -or $null -eq $exitB -or $exitA -ne 0 -or $exitB -ne 0) {
        throw "Rapid clients failed: A=$exitA, B=$exitB"
    }

    $summaries = @()
    foreach ($label in @("A", "B")) {
        $summaryPath = Join-Path $outputPath "summary-$label.csv"
        if (-not (Test-Path -LiteralPath $summaryPath -PathType Leaf)) {
            throw "Rapid client $label did not create a summary"
        }
        $rows = @(Import-Csv -LiteralPath $summaryPath)
        if ($rows.Count -ne 1) {
            throw "Rapid client $label must produce exactly one summary row"
        }
        $summary = $rows[0]
        if ([long]$summary.total_sent -ne $Requests -or
            [long]$summary.total_responses -ne $Requests -or
            [long]$summary.total_lost -ne 0 -or
            [long]$summary.total_duplicate -ne 0 -or
            ([long]$summary.total_success + [long]$summary.total_rejected) -ne $Requests) {
            throw "Rapid client $label evidence did not reconcile"
        }
        $summaries += $summary
    }

    $combined = [PSCustomObject]@{
        total_sent = ($summaries | Measure-Object total_sent -Sum).Sum
        total_responses = ($summaries | Measure-Object total_responses -Sum).Sum
        total_success = ($summaries | Measure-Object total_success -Sum).Sum
        total_rejected = ($summaries | Measure-Object total_rejected -Sum).Sum
        total_lost = ($summaries | Measure-Object total_lost -Sum).Sum
        total_duplicate = ($summaries | Measure-Object total_duplicate -Sum).Sum
        max_queue_depth = ($summaries | Measure-Object max_queue_depth -Maximum).Maximum
        test_duration_ms = ($summaries | Measure-Object test_duration_ms -Maximum).Maximum
    }
    $combined | Export-Csv (Join-Path $outputPath "summary.csv") -NoTypeInformation
    $combined | Format-List
}
finally {
    # Both clients finish before their process handles are released.
    foreach ($client in @($clientA, $clientB)) {
        if ($null -ne $client) {
            try {
                $client.WaitForExit()
            }
            finally {
                $client.Dispose()
            }
        }
    }
    Pop-Location
}
