param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "ASSERT FAILED: $Message" }
}

Write-Host "[1/6] health"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
Assert-True ($health.status -eq "UP") "health must be UP"

$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 12)
$account = "petlink_$suffix"
$password = "Example123!"

Write-Host "[2/6] register $account"
$registerBody = @{
    account = $account
    password = $password
    nickname = "M01 Smoke User"
    phone = "13800138000"
} | ConvertTo-Json
$registered = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $registerBody
Assert-True ($registered.code -eq 0) "register response code"

Write-Host "[3/6] login"
$loginBody = @{ account = $account; password = $password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $loginBody
$token = $login.data.accessToken
Assert-True ([bool]$token) "login token"
$headers = @{ Authorization = "Bearer $token" }

Write-Host "[4/6] GET /api/users/me"
$me = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/users/me" -Headers $headers
Assert-True ($me.data.account -eq $account) "current account"

Write-Host "[5/6] PATCH /api/users/me"
$patchBody = @{ nickname = "M01 Connected"; phone = $null } | ConvertTo-Json
$patched = Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/users/me" -Headers $headers -ContentType "application/json" -Body $patchBody
Assert-True ($patched.data.nickname -eq "M01 Connected") "profile nickname"
Assert-True ($null -eq $patched.data.phone) "profile phone cleared"

Write-Host "[6/6] POST /api/files/temporary"
if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
    throw "curl.exe is required for the multipart upload smoke step."
}
$tempPng = Join-Path $env:TEMP "petlink-smoke-$suffix.png"
$pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
[IO.File]::WriteAllBytes($tempPng, [Convert]::FromBase64String($pngBase64))
try {
    $uploadJson = & curl.exe -sS -f -X POST `
        -H "Authorization: Bearer $token" `
        -F "file=@$tempPng;type=image/png" `
        "$BaseUrl/api/files/temporary"
    if ($LASTEXITCODE -ne 0) { throw "temporary upload failed with exit code $LASTEXITCODE" }
    $upload = $uploadJson | ConvertFrom-Json
    Assert-True ($upload.code -eq 0) "temporary upload code"
    Assert-True ([bool]$upload.data.token) "temporary upload token"
} finally {
    Remove-Item $tempPng -ErrorAction SilentlyContinue
}

Write-Host "M01 smoke flow PASSED. account=$account"
