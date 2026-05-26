param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupPath)) {
    throw "Backup path does not exist: $BackupPath"
}

$dbBackup = Get-ChildItem -Path $BackupPath -Filter "springecom-db-*.backup" | Select-Object -First 1
$imageBackup = Get-ChildItem -Path $BackupPath -Filter "product-images-*.tar.gz" | Select-Object -First 1

if (-not $dbBackup) {
    throw "Database backup file not found in: $BackupPath"
}

if (-not $imageBackup) {
    throw "Product image backup file not found in: $BackupPath"
}

Write-Host "Restoring PostgreSQL database from: $($dbBackup.FullName)"
docker cp $dbBackup.FullName "springecom-postgres:/tmp/$($dbBackup.Name)"
docker exec springecom-postgres pg_restore -U springecom -d springecom --clean --if-exists "/tmp/$($dbBackup.Name)"
docker exec springecom-postgres rm -f "/tmp/$($dbBackup.Name)"

Write-Host "Restoring product images from: $($imageBackup.FullName)"
docker run --rm `
    -v springecom_springecom-product-images:/data `
    -v "${PWD}/${BackupPath}:/backup" `
    alpine `
    sh -c "cd /data && tar xzf /backup/$($imageBackup.Name)"

Write-Host "Restore completed successfully."