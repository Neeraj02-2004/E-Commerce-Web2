$ErrorActionPreference = "Stop"

$patterns = @(
    @{
        Name = "Cloudinary API secret"
        Regex = "CLOUDINARY_API_SECRET\s*=\s*(?!change-me|your-|<|\$\(|\$\{|$)[A-Za-z0-9_\-]{16,}"
    },
    @{
        Name = "Cloudinary API key"
        Regex = "CLOUDINARY_API_KEY\s*=\s*(?!change-me|your-|<|\$\(|\$\{|$)[0-9]{8,}"
    },
    @{
        Name = "Razorpay key secret"
        Regex = "RAZORPAY_KEY_SECRET\s*=\s*(?!change-me|your-|<|\$\(|\$\{|$).{12,}"
    },
    @{
        Name = "Razorpay webhook secret"
        Regex = "RAZORPAY_WEBHOOK_SECRET\s*=\s*(?!change-me|your-|<|\$\(|\$\{|$).{12,}"
    },
    @{
        Name = "Google client secret"
        Regex = "GOOGLE_CLIENT_SECRET\s*=\s*(?!change-me|test-|your-|<|\$\(|\$\{|$).{12,}"
    },
    @{
        Name = "JWT secret"
        Regex = "JWT_SECRET\s*=\s*(?!change-me|test-|your-|<|\$\(|\$\{|$).{32,}"
    },
    @{
        Name = "Database password"
        Regex = "DB_PASSWORD\s*=\s*(?!change-me|test-password|your-|<|\$\(|\$\{|$).{8,}"
    },
    @{
        Name = "Redis password"
        Regex = "REDIS_PASSWORD\s*=\s*(?!change-me|your-|<|\$\(|\$\{|$).{8,}"
    }
)

$files = git ls-files --cached --others --exclude-standard

$ignoredExtensions = @(
    ".jar", ".class", ".png", ".jpg", ".jpeg", ".webp", ".gif",
    ".pdf", ".zip", ".gz", ".backup", ".dump", ".ico"
)

$findings = New-Object System.Collections.Generic.List[string]

foreach ($file in $files) {
    if (-not (Test-Path $file -PathType Leaf)) {
        continue
    }

    $extension = [System.IO.Path]::GetExtension($file).ToLowerInvariant()

    if ($ignoredExtensions -contains $extension) {
        continue
    }

    $content = Get-Content -Path $file -Raw -ErrorAction SilentlyContinue

    if ($null -eq $content) {
        continue
    }

    foreach ($pattern in $patterns) {
        if ($content -match $pattern.Regex) {
            $findings.Add("$($pattern.Name) may be present in $file")
        }
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Secret scan failed:" -ForegroundColor Red
    $findings | ForEach-Object { Write-Host "- $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Secret scan passed."