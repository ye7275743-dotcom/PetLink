param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = $env:PETLINK_SMOKE_ADMIN_PASSWORD,
    [string]$RescuerAccount = "rescuer",
    [string]$RescuerPassword = $env:PETLINK_SMOKE_RESCUER_PASSWORD
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

function Login([string]$Account, [string]$Password) {
    $body = @{ account = $Account; password = $Password } | ConvertTo-Json
    $response = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $body
    Assert-True ([bool]$response.data.accessToken) "login token for $Account"
    return $response.data.accessToken
}

function Upload-TempPng([string]$Token, [string]$Suffix) {
    if (-not (Get-Command curl.exe -ErrorAction SilentlyContinue)) { throw "curl.exe is required." }
    $path = Join-Path $env:TEMP "petlink-m03-$Suffix.png"
    $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    [IO.File]::WriteAllBytes($path, [Convert]::FromBase64String($pngBase64))
    try {
        $json = & curl.exe -sS -f -X POST -H "Authorization: Bearer $Token" -F "file=@$path;type=image/png" "$BaseUrl/api/files/temporary"
        if ($LASTEXITCODE -ne 0) { throw "temporary upload failed: exit=$LASTEXITCODE" }
        $response = $json | ConvertFrom-Json
        Assert-True ($response.code -eq 0) "temporary upload code"
        return $response.data.token
    } finally { Remove-Item $path -ErrorAction SilentlyContinue }
}

function New-ApprovedClue([string]$UserToken, [hashtable]$UserHeaders, [hashtable]$AdminHeaders, [string]$Suffix, [string]$Label) {
    $imageToken = Upload-TempPng $UserToken "$Suffix-$Label-clue"
    $key = (Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/idempotency-keys" -Headers $UserHeaders).data.idempotencyKey
    $body = [ordered]@{
        location = "Nanjing Smoke $Label"
        foundTime = (Get-Date).AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:sszzz")
        animalDescription = "M03 smoke animal $Label"
        sceneDescription = "M03 smoke scene $Label"
        contact = "13800138000"
        imageTokens = @($imageToken)
    } | ConvertTo-Json -Depth 6
    $headers = @{ Authorization = "Bearer $UserToken"; "Idempotency-Key" = $key }
    $created = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers $headers -ContentType "application/json" -Body $body
    $clueId = $created.data.clueId
    $approve = @{ decision = "APPROVE" } | ConvertTo-Json
    $approved = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$clueId/audit" -Headers $AdminHeaders -ContentType "application/json" -Body $approve
    Assert-True ($approved.data.status -eq "WAITING_ACCEPT") "$Label approved"
    return $clueId
}

function Accept-And-Start([string]$ClueId, [hashtable]$RescuerHeaders) {
    $accepted = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$ClueId/accept" -Headers $RescuerHeaders
    Assert-True ($accepted.data.status -eq "WAITING_START") "accepted task waiting start"
    $taskId = $accepted.data.taskId
    $started = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$taskId/start" -Headers $RescuerHeaders
    Assert-True ($started.data.status -eq "IN_PROGRESS") "task started"
    return $taskId
}

$suffix = ([guid]::NewGuid().ToString("N")).Substring(0, 12)
$userAccount = "m03_$suffix"
$userPassword = "Example123!"

Write-Host "[1/16] health and login dev actors"
$health = Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health"
Assert-True ($health.status -eq "UP") "health"
$adminToken = Login $AdminAccount $AdminPassword
$rescuerToken = Login $RescuerAccount $RescuerPassword
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
$rescuerHeaders = @{ Authorization = "Bearer $rescuerToken" }

Write-Host "[2/16] register publisher USER"
$register = @{ account=$userAccount; password=$userPassword; nickname="M03 Smoke User"; phone="13800138000" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $register | Out-Null
$userToken = Login $userAccount $userPassword
$userHeaders = @{ Authorization = "Bearer $userToken" }

Write-Host "[3/16] create approved clue A for FAILED -> REOPEN -> CANCEL"
$clueA = New-ApprovedClue $userToken $userHeaders $adminHeaders $suffix "A"
$waiting = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/waiting-acceptance?page=1&size=100" -Headers $rescuerHeaders
Assert-True (@($waiting.data.records | Where-Object { $_.id -eq $clueA }).Count -ge 1) "A in waiting acceptance"
$taskA1 = Accept-And-Start $clueA $rescuerHeaders

Write-Host "[4/16] add/get rescue record"
$recordBody = @{ content = "Reached scene and started search" } | ConvertTo-Json
$record = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$taskA1/records" -Headers $rescuerHeaders -ContentType "application/json" -Body $recordBody
Assert-True ([bool]$record.data.id) "record id"
$records = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-tasks/$taskA1/records" -Headers $rescuerHeaders
Assert-True (@($records.data).Count -ge 1) "record list"

Write-Host "[5/16] FAILED result keeps clue converted"
$failBody = @{ result="FAILED"; failureReason="Animal escaped during search" } | ConvertTo-Json
$failed = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$taskA1/result" -Headers $rescuerHeaders -ContentType "application/json" -Body $failBody
Assert-True ($failed.data.status -eq "FAILED") "A first task failed"
$clueADetail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueA" -Headers $adminHeaders
Assert-True ($clueADetail.data.status -eq "CONVERTED") "failed task leaves clue converted"

Write-Host "[6/16] ADMIN REOPEN failed clue"
$reopenBody = @{ action="REOPEN"; resolutionReason="New sighting available" } | ConvertTo-Json
$reopened = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-tasks/$taskA1/failure-resolution" -Headers $adminHeaders -ContentType "application/json" -Body $reopenBody
Assert-True ($reopened.data.status -eq "WAITING_ACCEPT") "A reopened"

Write-Host "[7/16] accept again creates a new task"
$acceptedAgain = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$clueA/accept" -Headers $rescuerHeaders
$taskA2 = $acceptedAgain.data.taskId
Assert-True ($taskA2 -ne $taskA1) "new task after reopen"

Write-Host "[8/16] old FAILED task may no longer resolve clue"
Expect-HttpStatus { Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-tasks/$taskA1/failure-resolution" -Headers $adminHeaders -ContentType "application/json" -Body $reopenBody | Out-Null } 409 "old failed task must conflict"

Write-Host "[9/16] ADMIN cancel WAITING_START task reopens clue"
$cancelBody = @{ cancelReason="Rescuer unavailable" } | ConvertTo-Json
$canceled = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-tasks/$taskA2/cancel" -Headers $adminHeaders -ContentType "application/json" -Body $cancelBody
Assert-True ($canceled.data.status -eq "CANCELED") "task A2 canceled"
$clueADetail2 = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueA" -Headers $adminHeaders
Assert-True ($clueADetail2.data.status -eq "WAITING_ACCEPT") "cancel reopens clue"

Write-Host "[10/16] create clue B for FAILED -> CLOSE"
$clueB = New-ApprovedClue $userToken $userHeaders $adminHeaders $suffix "B"
$taskB = Accept-And-Start $clueB $rescuerHeaders
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$taskB/result" -Headers $rescuerHeaders -ContentType "application/json" -Body $failBody | Out-Null
$closeBody = @{ action="CLOSE"; resolutionReason="Search exhausted" } | ConvertTo-Json
$closedB = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-tasks/$taskB/failure-resolution" -Headers $adminHeaders -ContentType "application/json" -Body $closeBody
Assert-True ($closedB.data.status -eq "CLOSED") "B closed after failure"

Write-Host "[11/16] create clue C for SUCCESS"
$clueC = New-ApprovedClue $userToken $userHeaders $adminHeaders $suffix "C"
$taskC = Accept-And-Start $clueC $rescuerHeaders

Write-Host "[12/16] upload animal image and submit SUCCESS with two animals"
$animalImageToken = Upload-TempPng $rescuerToken "$suffix-C-animal"
$successBody = [ordered]@{
    result="SUCCESS"
    animals=@(
        [ordered]@{
            name="Milo"; species="CAT"; sex="UNKNOWN"; estimatedAgeMonths=12; color="orange";
            healthCondition="leg injury treated"; initialHealthRecord="cleaned and bandaged"; imageTokens=@($animalImageToken)
        },
        [ordered]@{
            name="Luna"; species="CAT"; sex="FEMALE"; estimatedAgeMonths=8; color="white";
            healthCondition="stable"; imageTokens=@()
        }
    )
} | ConvertTo-Json -Depth 8
$success = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$taskC/result" -Headers $rescuerHeaders -ContentType "application/json" -Body $successBody
Assert-True ($success.data.status -eq "SUCCESS") "C success"
Assert-True (@($success.data.animalIds).Count -eq 2) "two animals created"

Write-Host "[13/16] task detail includes clue, record list and animals"
$taskDetail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-tasks/$taskC" -Headers $rescuerHeaders
Assert-True ($taskDetail.data.status -eq "SUCCESS") "task C detail success"
Assert-True ($taskDetail.data.clue.id -eq $clueC) "task detail clue"
Assert-True (@($taskDetail.data.animals).Count -eq 2) "task detail animals"

Write-Host "[14/16] clue C is CLOSED"
$clueCDetail = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-clues/$clueC" -Headers $adminHeaders
Assert-True ($clueCDetail.data.status -eq "CLOSED") "successful rescue closes clue"

Write-Host "[15/16] mine tasks includes created tasks"
$mine = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-tasks/me?page=1&size=100" -Headers $rescuerHeaders
Assert-True (@($mine.data.records | Where-Object { $_.id -eq $taskC }).Count -ge 1) "mine contains task C"

Write-Host "[16/16] duplicate accept of non-waiting clue conflicts"
Expect-HttpStatus { Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$clueC/accept" -Headers $rescuerHeaders | Out-Null } 409 "closed clue cannot be accepted"

Write-Host "M03 smoke flow PASSED. A: FAILED->REOPEN->CANCELED; B: FAILED->CLOSED; C: SUCCESS with 2 animals."
