param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = $env:PETLINK_SMOKE_ADMIN_PASSWORD,
    [string]$RescuerAccount = "rescuer",
    [string]$RescuerPassword = $env:PETLINK_SMOKE_RESCUER_PASSWORD
)
$ErrorActionPreference = "Stop"
function Assert-True([bool]$Condition,[string]$Message){if(-not $Condition){throw "ASSERT FAILED: $Message"}}
function Expect-HttpStatus([scriptblock]$Action,[int]$ExpectedStatus,[string]$Message){
    try { & $Action; throw "ASSERT FAILED: $Message" } catch {
        if ($_.Exception.Message -like "ASSERT FAILED:*") { throw }
        if (-not $_.Exception.Response) { throw }
        $actual=[int]$_.Exception.Response.StatusCode
        if($actual -ne $ExpectedStatus){throw "ASSERT FAILED: $Message (expected HTTP $ExpectedStatus, got $actual)"}
    }
}
function Login([string]$Account,[string]$Password){
    $body=@{account=$Account;password=$Password}|ConvertTo-Json
    $r=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $body
    Assert-True ([bool]$r.data.accessToken) "login token for $Account"; return $r.data.accessToken
}
function Upload-TempPng([string]$Token,[string]$Suffix){
    if(-not (Get-Command curl.exe -ErrorAction SilentlyContinue)){throw "curl.exe is required."}
    $path=Join-Path $env:TEMP "petlink-m04-$Suffix.png"
    $b64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    [IO.File]::WriteAllBytes($path,[Convert]::FromBase64String($b64))
    try{$json=& curl.exe -sS -f -X POST -H "Authorization: Bearer $Token" -F "file=@$path;type=image/png" "$BaseUrl/api/files/temporary";if($LASTEXITCODE-ne 0){throw "upload failed"};return ($json|ConvertFrom-Json).data.token}
    finally{Remove-Item $path -ErrorAction SilentlyContinue}
}
$suffix=([guid]::NewGuid().ToString("N")).Substring(0,12)
Write-Host "[1/15] health and actor login"
Assert-True ((Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health").status -eq "UP") "health"
$adminToken=Login $AdminAccount $AdminPassword;$rescuerToken=Login $RescuerAccount $RescuerPassword
$ah=@{Authorization="Bearer $adminToken"};$rh=@{Authorization="Bearer $rescuerToken"}

Write-Host "[2/15] create and approve clue as rescuer"
$clueImage=Upload-TempPng $rescuerToken "$suffix-clue";$key=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/idempotency-keys" -Headers $rh).data.idempotencyKey
$clueBody=[ordered]@{location="Nanjing M04 Smoke";foundTime=(Get-Date).AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:sszzz");animalDescription="M04 smoke cat";sceneDescription="M04 smoke scene";contact="13800138000";imageTokens=@($clueImage)}|ConvertTo-Json -Depth 6
$ch=@{Authorization="Bearer $rescuerToken";"Idempotency-Key"=$key};$clue=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers $ch -ContentType "application/json" -Body $clueBody).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$($clue.clueId)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)|Out-Null

Write-Host "[3/15] rescue success creates TREATING animal"
$task=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$($clue.clueId)/accept" -Headers $rh).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/start" -Headers $rh|Out-Null
$successBody=[ordered]@{result="SUCCESS";animals=@([ordered]@{name="M04Cat";species="CAT";sex="UNKNOWN";estimatedAgeMonths=10;color="orange";healthCondition="recovering";initialHealthRecord="initial treatment";imageTokens=@()})}|ConvertTo-Json -Depth 8
$success=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/result" -Headers $rh -ContentType "application/json" -Body $successBody).data
Assert-True (@($success.animalIds).Count -eq 1) "one animal created";$animalId=$success.animalIds[0]

Write-Host "[4/15] responsible list and privileged detail"
$responsible=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/responsible/me?page=1&size=100&status=TREATING" -Headers $rh
Assert-True (@($responsible.data.records | Where-Object {$_.id -eq $animalId}).Count -eq 1) "responsible list contains animal"
$detail=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalId" -Headers $rh).data
Assert-True ($detail.rescueTaskId -eq $task.taskId) "privileged rescueTaskId";Assert-True (@($detail.healthRecords).Count -eq 1) "initial health record";Assert-True ([bool]$detail.healthRecords[0].recorderId) "privileged recorderId"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalId"|Out-Null} 404 "TREATING animal is not public"

Write-Host "[5/15] optimistic PATCH"
$patch=@{name="M04 Cat Updated";healthCondition="stable";version=$detail.version}|ConvertTo-Json
$updated=(Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/animals/$animalId" -Headers $rh -ContentType "application/json" -Body $patch).data
Assert-True ($updated.name -eq "M04 Cat Updated") "patch name";Assert-True ($updated.version -eq ($detail.version+1)) "patch increments version"

Write-Host "[6/15] append health record"
$record=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animalId/health-records" -Headers $rh -ContentType "application/json" -Body (@{content="follow-up treatment"}|ConvertTo-Json)).data
Assert-True ([bool]$record.recorderId) "new health recorder"
$records=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalId/health-records" -Headers $rh
Assert-True (@($records.data).Count -eq 2) "two health records"

Write-Host "[7/15] TO_OBSERVING"
$state=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animalId/status-actions" -Headers $rh -ContentType "application/json" -Body (@{action="TO_OBSERVING";version=$updated.version}|ConvertTo-Json)).data
Assert-True ($state.status -eq "OBSERVING") "observing"

Write-Host "[8/15] OPEN_ADOPTION"
$state=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animalId/status-actions" -Headers $rh -ContentType "application/json" -Body (@{action="OPEN_ADOPTION";version=$state.version}|ConvertTo-Json)).data
Assert-True ($state.status -eq "AVAILABLE") "available"
$public=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals?page=1&size=100&species=CAT"
Assert-True (@($public.data.records | Where-Object {$_.id -eq $animalId}).Count -eq 1) "available animal in public list"
$publicDetail=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalId").data
Assert-True (-not ($publicDetail.PSObject.Properties.Name -contains "rescueTaskId")) "public detail hides rescueTaskId"
Assert-True (-not ($publicDetail.healthRecords[0].PSObject.Properties.Name -contains "recorderId")) "public health hides recorderId"

Write-Host "[9/15] append two animal images"
$t1=Upload-TempPng $rescuerToken "$suffix-a1";$t2=Upload-TempPng $rescuerToken "$suffix-a2"
$withImages=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animalId/images" -Headers $rh -ContentType "application/json" -Body (@{imageTokens=@($t1,$t2)}|ConvertTo-Json -Depth 5)).data
Assert-True (@($withImages.images).Count -eq 2) "two animal images"
Assert-True ($withImages.images[0].sortOrder -eq 1 -and $withImages.images[1].sortOrder -eq 2) "animal image order"

Write-Host "[10/15] visitor media read"
$mediaId=$withImages.images[0].id;$out=Join-Path $env:TEMP "petlink-m04-media-$suffix.bin"
try{Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/media/animal-images/$mediaId" -OutFile $out|Out-Null;Assert-True ((Get-Item $out).Length -gt 0) "public media bytes"}finally{Remove-Item $out -ErrorAction SilentlyContinue}

Write-Host "[11/15] delete image and reindex"
$remaining=(Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/animals/$animalId/images/$mediaId" -Headers $rh).data
Assert-True (@($remaining.images).Count -eq 1) "one image remains";Assert-True ($remaining.images[0].sortOrder -eq 1) "remaining image reindexed"

Write-Host "[12/15] SUSPEND_ADOPTION"
$suspended=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animalId/status-actions" -Headers $rh -ContentType "application/json" -Body (@{action="SUSPEND_ADOPTION";suspendReason="needs treatment";version=$remaining.version}|ConvertTo-Json)).data
Assert-True ($suspended.status -eq "SUSPENDED") "suspended";Assert-True ($suspended.suspendReason -eq "needs treatment") "suspend reason"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalId"|Out-Null} 404 "suspended animal hidden publicly"

Write-Host "[13/15] RESUME_ADOPTION"
$resumed=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animalId/status-actions" -Headers $rh -ContentType "application/json" -Body (@{action="RESUME_ADOPTION";version=$suspended.version}|ConvertTo-Json)).data
Assert-True ($resumed.status -eq "AVAILABLE") "resumed available";Assert-True (-not $resumed.suspendReason) "suspend reason cleared"

Write-Host "[14/15] stale version conflicts"
Expect-HttpStatus {Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/animals/$animalId" -Headers $rh -ContentType "application/json" -Body (@{name="stale";version=$detail.version}|ConvertTo-Json)|Out-Null} 409 "stale animal version"

Write-Host "[15/15] final public verification"
$final=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalId").data
Assert-True ($final.status -eq "AVAILABLE") "final available";Assert-True (@($final.images).Count -eq 1) "final image count"
Write-Host "M04 smoke flow PASSED. animal=$animalId AVAILABLE"
