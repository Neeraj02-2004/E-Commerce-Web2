param(
    [Parameter(Mandatory = $true)]
    [string]$BackupPath,

    [switch]$RestoreLocalImageVolume
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupPath -PathType Container)) {
    throw "Backup path does not exist or is not a folder: $BackupPath"
}

$dbBackup = Get-ChildItem -Path $BackupPath -Filter "springecom-db-*.backup" | Select-Object -First 1
$imageBackup = Get-ChildItem -Path $BackupPath -Filter "product-images-*.tar.gz" | Select-Object -First 1

if (-not $dbBackup) {
    throw "Database backup file not found in: $BackupPath"
}

Write-Host "Restoring PostgreSQL database from: $($dbBackup.FullName)"
docker cp $dbBackup.FullName "springecom-postgres:/tmp/$($dbBackup.Name)"
docker exec springecom-postgres pg_restore -U springecom -d springecom --clean --if-exists "/tmp/$($dbBackup.Name)"
docker exec springecom-postgres rm -f "/tmp/$($dbBackup.Name)"

if ($RestoreLocalImageVolume) {
    if (-not $imageBackup) {
        throw "Product image backup file not found in: $BackupPath"
    }

    Write-Host "Restoring local product images from: $($imageBackup.FullName)"
    $resolvedBackupPath = (Resolve-Path $BackupPath).Path

    docker run --rm `
        -v springecom_springecom-product-images:/data `
        -v "${resolvedBackupPath}:/backup" `
        alpine `
        sh -c "cd /data && tar xzf /backup/$($imageBackup.Name)"
} else {
    Write-Host "Skipping local product image volume restore."
    Write-Host "Production product images are served from Cloudinary and are not restored from this database backup."
}

Write-Host "Restore completed successfully."