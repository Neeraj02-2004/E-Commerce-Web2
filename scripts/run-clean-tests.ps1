param(
    [string]$DriveLetter = "S"
)

$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$drive = "${DriveLetter}:"

Write-Host "Mapping $drive to $projectRoot"

cmd /c "subst $drive /D 2>nul" | Out-Null
cmd /c "subst $drive ""$projectRoot""" | Out-Null

try {
    Push-Location "$drive\"
    .\mvnw.cmd clean test
}
finally {
    Pop-Location
    cmd /c "subst $drive /D 2>nul" | Out-Null
}