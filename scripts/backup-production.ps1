param(
    [string]$BackupDir = "backups",
    [int]$RetentionDays = 14
)

$ErrorActionPreference = "Stop"

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupRoot = Join-Path $BackupDir $timestamp

New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$dbBackupFile = "springecom-db-$timestamp.backup"
$imageBackupFile = "product-images-$timestamp.tar.gz"

Write-Host "Creating backup folder: $backupRoot"

Write-Host "Creating PostgreSQL backup..."
docker exec springecom-postgres pg_dump -U springecom -d springecom -Fc -f "/tmp/$dbBackupFile"
docker cp "springecom-postgres:/tmp/$dbBackupFile" (Join-Path $backupRoot $dbBackupFile)
docker exec springecom-postgres rm -f "/tmp/$dbBackupFile"

Write-Host "Creating product image volume backup..."
docker run --rm `
    -v springecom_springecom-product-images:/data `
    -v "${PWD}/${backupRoot}:/backup" `
    alpine `
    tar czf "/backup/$imageBackupFile" -C /data .

Write-Host "Removing backups older than $RetentionDays days..."
if (Test-Path $BackupDir) {
    Get-ChildItem -Path $BackupDir -Directory |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$RetentionDays) } |
        Remove-Item -Recurse -Force
}

Write-Host "Backup completed successfully."
Write-Host "Backup location: $backupRoot"