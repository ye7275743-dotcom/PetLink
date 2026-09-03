param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = $env:PETLINK_SMOKE_ADMIN_PASSWORD
)

$ErrorActionPreference = "Stop"

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "ASSERT FAILED: $Message" }
}

function Expect-HttpStatus([scriptblock]$Action, [int]$ExpectedStatus, [string]$Message) {
    try {
        & $Action
        throw "ASSERT FAILED: $Message"
    } catch {
        if ($_.Exception.Message -like "ASSERT FAILED:*") { throw }
        if (-not $_.Exception.Response) { throw }
        $actual = [int]$_.Exception.Response.StatusCode
        if ($actual -ne $ExpectedStatus) {
            throw "ASSERT FAILED: $Message (expected HTTP $ExpectedStatus, got $actual)"
        }
    }
}

function Upload-TempPng([string]$Token, [string]$Suffix) {
    if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) {
        throw "curl.exe is required for multipart upload smoke steps."
    }
    $path = Join-Path $env:TEMP "petlink-m02-$Suffix.png"
    $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    [IO.File]::WriteAllBytes($path, [Convert]::FromBase64String($pngBase64))
    try {
        $json = & curl.exe -sS -f -X POST `
            -H "Authorization: Bearer $Token" `
            -F "file=@$path;type=image/png" `
            "$BaseUrl/api/files/temporary"
        if ($LASTEXITCODE -ne 0) { throw "temporary upload failed: exit=$LASTEXITCODE" }
        $response = $json | ConvertFrom-Json
        Assert-True ($response.code -eq 0) "temporary upload code"
        Assert-True ([bool]$response.data.token) "temporary upload token"
        return $response.data.token
    } finally {
        Remove-Item $path -ErrorAction SilentlyContinue
    }
}

function New-SmokeClue([string]$UserToken, [hashtable]$UserHeaders, [string]$Suffix, [string]$Label) {
    $imageToken = Upload-TempPng $UserToken "$Suffix-$Label"
    $keyResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/idempotency-keys" -Headers $UserHeaders
    $key = $keyResponse.data.idempotencyKey
    Assert-True ([bool]$key) "$Label idempotency key"

    $bodyObject = [ordered]@{
        location = "Nanjing Xuanwu Smoke $Label"
        foundTime = (Get-Date).AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:sszzz")
        animalDescription = "M02 smoke animal $Label"
        sceneDescription = "Waiting for rescue $Label"
        contact = "13800138000"
        imageTokens = @($imageToken)
    }
    $body = $bodyObject | ConvertTo-Json -Depth 6
    $headers = @{ Authorization = "Bearer $UserToken"; "Idempotency-Key" = $key }
    $created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers $headers -ContentType "application/json" -Body $body
    Assert-True ($created.data.status -eq "PENDING_REVIEW") "$Label initial status"
    Assert-True ([bool]$created.data.clueId) "$Label clue id"
    return @{ Result = $created; Body = $body; Headers = $headers }
}

$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 12)
$account = "m02_$suffix"
$password = "Example123!"

Write-Host "[1/18] health"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
Assert-True ($health.status -eq "UP") "health must be UP"

Write-Host "[2/18] register/login USER"
$registerBody = @{
    account = $account
    password = $password
    nickname = "M02 Smoke User"
    phone = "13800138000"
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $registerBody | Out-Null
$loginBody = @{ account = $account; password = $password } | ConvertTo-Json
$userLogin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $loginBody
$userToken = $userLogin.data.accessToken
$userHeaders = @{ Authorization = "Bearer $userToken" }
Assert-True ([bool]$userToken) "USER login token"

Write-Host "[3/18] create clue C and replay same idempotent request"
$clueC = New-SmokeClue $userToken $userHeaders $suffix "C"
$clueIdC = $clueC.Result.data.clueId
$replayed = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers $clueC.Headers -ContentType "application/json" -Body $clueC.Body
Assert-True ($replayed.data.clueId -eq $clueIdC) "same idempotency request replays first clue"

Write-Host "[4/18] mine + detail"
$mine = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/me?page=1&size=20&status=PENDING_REVIEW" -Headers $userHeaders
Assert-True (@($mine.data.records | Where-Object { $_.id -eq $clueIdC }).Count -ge 1) "mine contains clue C"
$detail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueIdC" -Headers $userHeaders
Assert-True (@($detail.data.images).Count -eq 1) "clue C detail has first image"

Write-Host "[5/18] PATCH changed fields"
$patchBody = @{ sceneDescription = "  More scene details  "; contact = "13900139000" } | ConvertTo-Json
$patched = Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/rescue-clues/$clueIdC" -Headers $userHeaders -ContentType "application/json" -Body $patchBody
Assert-True ($patched.data.sceneDescription -eq "More scene details") "PATCH trims sceneDescription"
Assert-True ($patched.data.contact -eq "13900139000") "PATCH contact"

Write-Host "[6/18] PATCH same values as no-op"
$noOpBody = @{ sceneDescription = "More scene details"; contact = "13900139000" } | ConvertTo-Json
$noOp = Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/rescue-clues/$clueIdC" -Headers $userHeaders -ContentType "application/json" -Body $noOpBody
Assert-True ($noOp.data.sceneDescription -eq "More scene details") "same-value PATCH returns detail"

Write-Host "[7/18] append second image"
$imageToken2 = Upload-TempPng $userToken "$suffix-C2"
$appendBody = @{ imageTokens = @($imageToken2) } | ConvertTo-Json -Depth 5
$appended = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$clueIdC/images" -Headers $userHeaders -ContentType "application/json" -Body $appendBody
Assert-True (@($appended.data.images).Count -eq 2) "append image count"
Assert-True ($appended.data.images[0].sortOrder -eq 1 -and $appended.data.images[1].sortOrder -eq 2) "image sort order"

Write-Host "[8/18] delete image + media read"
$deleteImageId = $appended.data.images[0].id
$remaining = Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/rescue-clues/$clueIdC/images/$deleteImageId" -Headers $userHeaders
Assert-True (@($remaining.data.images).Count -eq 1) "delete retains one image"
Assert-True ($remaining.data.images[0].sortOrder -eq 1) "remaining image reindexed"
$mediaId = $remaining.data.images[0].id
$mediaPath = Join-Path $env:TEMP "petlink-m02-media-$suffix.bin"
try {
    Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/media/rescue-clue-images/$mediaId" -Headers $userHeaders -OutFile $mediaPath | Out-Null
    Assert-True ((Get-Item $mediaPath).Length -gt 0) "media returns bytes"
} finally {
    Remove-Item $mediaPath -ErrorAction SilentlyContinue
}

Write-Host "[9/18] create clue A for withdraw"
$clueA = New-SmokeClue $userToken $userHeaders $suffix "A"
$clueIdA = $clueA.Result.data.clueId

Write-Host "[10/18] withdraw clue A"
$withdrawn = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$clueIdA/withdraw" -Headers $userHeaders
Assert-True ($withdrawn.data.status -eq "WITHDRAWN") "clue A becomes WITHDRAWN"

Write-Host "[11/18] create clue B for reject"
$clueB = New-SmokeClue $userToken $userHeaders $suffix "B"
$clueIdB = $clueB.Result.data.clueId

Write-Host "[12/18] login ADMIN"
$adminLoginBody = @{ account = $AdminAccount; password = $AdminPassword } | ConvertTo-Json
$adminLogin = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $adminLoginBody
$adminToken = $adminLogin.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Assert-True ([bool]$adminToken) "ADMIN login token"

Write-Host "[13/18] ADMIN review queue contains B and C, excludes withdrawn A"
$queue = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/rescue-clues?page=1&size=100" -Headers $adminHeaders
Assert-True (@($queue.data.records | Where-Object { $_.id -eq $clueIdB }).Count -ge 1) "queue contains clue B"
Assert-True (@($queue.data.records | Where-Object { $_.id -eq $clueIdC }).Count -ge 1) "queue contains clue C"
Assert-True (@($queue.data.records | Where-Object { $_.id -eq $clueIdA }).Count -eq 0) "default queue excludes clue A"

Write-Host "[14/18] REJECT without reason must fail"
$badRejectBody = @{ decision = "REJECT" } | ConvertTo-Json
Expect-HttpStatus { Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$clueIdB/audit" -Headers $adminHeaders -ContentType "application/json" -Body $badRejectBody | Out-Null } 400 "REJECT requires rejectReason"

Write-Host "[15/18] ADMIN reject clue B"
$rejectBody = @{ decision = "REJECT"; rejectReason = "Insufficient scene information" } | ConvertTo-Json
$rejected = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$clueIdB/audit" -Headers $adminHeaders -ContentType "application/json" -Body $rejectBody
Assert-True ($rejected.data.status -eq "REJECTED") "clue B becomes REJECTED"
$rejectedDetail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueIdB" -Headers $userHeaders
Assert-True ($rejectedDetail.data.status -eq "REJECTED") "user sees clue B REJECTED"
Assert-True ($rejectedDetail.data.rejectReason -eq "Insufficient scene information") "reject reason persisted"

Write-Host "[16/18] ADMIN approve clue C"
$approveBody = @{ decision = "APPROVE" } | ConvertTo-Json
$approved = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$clueIdC/audit" -Headers $adminHeaders -ContentType "application/json" -Body $approveBody
Assert-True ($approved.data.status -eq "WAITING_ACCEPT") "clue C becomes WAITING_ACCEPT"

Write-Host "[17/18] approved clue C is no longer editable"
$approvedDetail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueIdC" -Headers $userHeaders
Assert-True ($approvedDetail.data.status -eq "WAITING_ACCEPT") "approved detail status"
Expect-HttpStatus { Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/rescue-clues/$clueIdC" -Headers $userHeaders -ContentType "application/json" -Body (@{ contact = "13700137000" } | ConvertTo-Json) | Out-Null } 409 "PATCH after audit must conflict"

Write-Host "[18/18] final state verification"
$withdrawnDetail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueIdA" -Headers $userHeaders
Assert-True ($withdrawnDetail.data.status -eq "WITHDRAWN") "clue A final status"
Assert-True ($rejectedDetail.data.status -eq "REJECTED") "clue B final status"
Assert-True ($approvedDetail.data.status -eq "WAITING_ACCEPT") "clue C final status"

Write-Host "M02 smoke flow PASSED. A=$clueIdA WITHDRAWN; B=$clueIdB REJECTED; C=$clueIdC WAITING_ACCEPT"
