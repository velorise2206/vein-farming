[CmdletBinding()]
param(
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$WorkspaceParent = Split-Path -Parent $Root.TrimEnd('\', '/')
$OutputDirectory = Join-Path (Join-Path $WorkspaceParent 'mods') 'vein_farming'

$OriginalJavaHome = $env:JAVA_HOME
$OriginalPath = $env:Path

function Use-JavaHome([string]$VariableName, [string]$Label) {
    $candidate = [Environment]::GetEnvironmentVariable($VariableName)

    if ([string]::IsNullOrWhiteSpace($candidate)) {
        if ($VariableName -eq 'JAVA21_HOME' -and (Test-Path 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe')) {
            $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
        } elseif ($VariableName -eq 'JAVA17_HOME' -and (Test-Path 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe')) {
            $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
        } elseif ($VariableName -eq 'JAVA25_HOME' -and (Test-Path 'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot\bin\java.exe')) {
            $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot'
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        if (-not (Test-Path (Join-Path $candidate 'bin\java.exe'))) {
            throw "$VariableName points to an invalid JDK directory: $candidate"
        }

        $env:JAVA_HOME = $candidate
        $env:Path = "$(Join-Path $candidate 'bin');$OriginalPath"
        Write-Host "Using $Label from ${VariableName}: $candidate"
        return
    }

    if (-not [string]::IsNullOrWhiteSpace($OriginalJavaHome)) {
        $env:JAVA_HOME = $OriginalJavaHome
        $env:Path = "$(Join-Path $OriginalJavaHome 'bin');$OriginalPath"
        Write-Host "${VariableName} is not set; using current JAVA_HOME for ${Label}: $OriginalJavaHome" -ForegroundColor Yellow
        return
    }

    $env:JAVA_HOME = $null
    $env:Path = $OriginalPath
    Write-Host "${VariableName} and JAVA_HOME are not set; Gradle will use java.exe from PATH for ${Label}." -ForegroundColor Yellow
}

function Invoke-Build(
    [string]$DisplayName,
    [string]$Folder,
    [string]$JavaVariable,
    [string]$JavaLabel
) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building $DisplayName" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan

    Use-JavaHome $JavaVariable $JavaLabel

    Push-Location (Join-Path $Root $Folder)
    try {
        if ($Clean) {
            cmd.exe /c "gradlew.bat clean build"
        } else {
            cmd.exe /c "gradlew.bat build"
        }
        if ($LASTEXITCODE -ne 0) {
            throw "$DisplayName build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

function Get-LatestReleaseJar(
    [string]$Folder,
    [string]$NamePattern,
    [string]$DisplayName
) {
    if (-not (Test-Path $Folder)) {
        throw "$DisplayName output directory does not exist: $Folder"
    }

    $jar = Get-ChildItem -Path $Folder -Filter $NamePattern -File |
        Where-Object {
            $_.Name -notmatch '(-sources|-dev|-javadoc|-slim|-plain)\.jar$'
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Could not find the release JAR for $DisplayName in: $Folder"
    }

    return $jar
}

function Copy-ReleaseJar(
    [System.IO.FileInfo]$Jar,
    [string]$DisplayName
) {
    $destination = Join-Path $OutputDirectory $Jar.Name
    Copy-Item -Path $Jar.FullName -Destination $destination -Force
    Write-Host "[COPIED] $DisplayName -> $destination" -ForegroundColor Green
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

try {
    Invoke-Build 'Forge 1.20.1'     '1.20.1\forge'     'JAVA17_HOME' 'JDK 17'
    Invoke-Build 'Fabric 1.20.1'    '1.20.1\fabric'    'JAVA17_HOME' 'JDK 17'
    Invoke-Build 'NeoForge 1.21.1'  '1.21.1\neoforge'  'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'Fabric 1.21.1'    '1.21.1\fabric'    'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'Fabric 1.21.11'   '1.21.11\fabric'   'JAVA21_HOME' 'JDK 21'
    Invoke-Build 'NeoForge 26.1.2'  '26.1\neoforge'    'JAVA25_HOME' 'JDK 25'
    Invoke-Build 'Fabric 26.1.2'    '26.1\fabric'      'JAVA25_HOME' 'JDK 25'
    Invoke-Build 'NeoForge 26.2'    '26.2\neoforge'    'JAVA25_HOME' 'JDK 25'
    Invoke-Build 'Fabric 26.2'      '26.2\fabric'      'JAVA25_HOME' 'JDK 25'
    Invoke-Build 'NeoForge 26.3'    '26.3\neoforge'    'JAVA25_HOME' 'JDK 25'
    Invoke-Build 'Fabric 26.3'      '26.3\fabric'      'JAVA25_HOME' 'JDK 25'

    $forge1201Jar = Get-LatestReleaseJar `
        (Join-Path $Root '1.20.1\forge\build\libs') `
        'vein_farming-1.20.1-forge-*.jar' `
        'Forge 1.20.1'

    $fabric1201Jar = Get-LatestReleaseJar `
        (Join-Path $Root '1.20.1\fabric\build\libs') `
        'vein_farming-1.20.1-fabric-*.jar' `
        'Fabric 1.20.1'

    $neoForge121Jar = Get-LatestReleaseJar `
        (Join-Path $Root '1.21.1\neoforge\build\libs') `
        'vein_farming-1.21.1-neoforge-*.jar' `
        'NeoForge 1.21.1'

    $fabric121Jar = Get-LatestReleaseJar `
        (Join-Path $Root '1.21.1\fabric\build\libs') `
        'vein_farming-fabric-1.21.1-*.jar' `
        'Fabric 1.21.1'

    $fabric12111Jar = Get-LatestReleaseJar `
        (Join-Path $Root '1.21.11\fabric\build\libs') `
        'vein_farming-fabric-1.21.11-*.jar' `
        'Fabric 1.21.11'

    $neoForge261Jar = Get-LatestReleaseJar `
        (Join-Path $Root '26.1\neoforge\build\libs') `
        'vein_farming-26.1.2-neoforge-*.jar' `
        'NeoForge 26.1.2'

    $fabric261Jar = Get-LatestReleaseJar `
        (Join-Path $Root '26.1\fabric\build\libs') `
        'vein_farming-26.1.2-fabric-*.jar' `
        'Fabric 26.1.2'

    $neoForge262Jar = Get-LatestReleaseJar `
        (Join-Path $Root '26.2\neoforge\build\libs') `
        'vein_farming-26.2-neoforge-*.jar' `
        'NeoForge 26.2'

    $fabric262Jar = Get-LatestReleaseJar `
        (Join-Path $Root '26.2\fabric\build\libs') `
        'vein_farming-26.2-fabric-*.jar' `
        'Fabric 26.2'

    $neoForge263Jar = Get-LatestReleaseJar `
        (Join-Path $Root '26.3\neoforge\build\libs') `
        'vein_farming-26.3-neoforge-*.jar' `
        'NeoForge 26.3'

    $fabric263Jar = Get-LatestReleaseJar `
        (Join-Path $Root '26.3\fabric\build\libs') `
        'vein_farming-26.3-fabric-*.jar' `
        'Fabric 26.3'

    Write-Host ""
    Write-Host "Copying artifacts to release directory..." -ForegroundColor Cyan
    Copy-ReleaseJar $forge1201Jar     'Forge 1.20.1'
    Copy-ReleaseJar $fabric1201Jar    'Fabric 1.20.1'
    Copy-ReleaseJar $neoForge121Jar   'NeoForge 1.21.1'
    Copy-ReleaseJar $fabric121Jar     'Fabric 1.21.1'
    Copy-ReleaseJar $fabric12111Jar   'Fabric 1.21.11'
    Copy-ReleaseJar $neoForge261Jar   'NeoForge 26.1.2'
    Copy-ReleaseJar $fabric261Jar     'Fabric 26.1.2'
    Copy-ReleaseJar $neoForge262Jar   'NeoForge 26.2'
    Copy-ReleaseJar $fabric262Jar     'Fabric 26.2'
    Copy-ReleaseJar $neoForge263Jar   'NeoForge 26.3'
    Copy-ReleaseJar $fabric263Jar     'Fabric 26.3'

    $gameModsDir = 'C:\Users\hunga\AppData\Roaming\.minecraft\versions\neoforge 1.21.1\mods'
    if (Test-Path $gameModsDir) {
        Get-ChildItem -Path $gameModsDir -Filter 'vein_farming*.jar' | Remove-Item -Force
        Copy-Item -Path $neoForge121Jar.FullName -Destination $gameModsDir -Force
        Write-Host "[DEPLOYED] NeoForge 1.21.1 -> $gameModsDir\$($neoForge121Jar.Name)" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "All 11 builds completed successfully!" -ForegroundColor Green
    Write-Host "Release directory: $OutputDirectory" -ForegroundColor Green
}
finally {
    $env:JAVA_HOME = $OriginalJavaHome
    $env:Path = $OriginalPath
}
