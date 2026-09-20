param(
    [Parameter(Mandatory = $true)]
    [Guid]$Session,
    [int]$Requests = 200,
    [string]$HostName = "127.0.0.1",
    [int]$Port = 5050,
    [ValidateSet("step", "snapshot", "queue-full")]
    [string]$Scenario = "step",
    [string]$Output = "test-results"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    & .\mvnw.cmd -pl test-client -am package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven package failed with exit code $LASTEXITCODE"
    }

    $outputPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $Output))
    New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
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
        Start-Process -FilePath "java" -ArgumentList $arguments -PassThru -NoNewWindow
    }

    $clientA = Start-RapidClient "A"
    $clientB = Start-RapidClient "B"
    $clientA.WaitForExit()
    $clientB.WaitForExit()
    $clientA.Refresh()
    $clientB.Refresh()
    $exitA = $clientA.ExitCode
    $exitB = $clientB.ExitCode
    if (($null -ne $exitA -and $exitA -ne 0) -or
        ($null -ne $exitB -and $exitB -ne 0)) {
        throw "Rapid clients failed: A=$exitA, B=$exitB"
    }

    $summaries = @()
    $summaries += Import-Csv (Join-Path $outputPath "summary-A.csv")
    $summaries += Import-Csv (Join-Path $outputPath "summary-B.csv")
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

    if ([int]$combined.total_sent -ne [int]$combined.total_responses -or
        [int]$combined.total_lost -ne 0 -or
        [int]$combined.total_duplicate -ne 0) {
        throw "Combined rapid-client evidence did not reconcile"
    }
}
finally {
    Pop-Location
}
