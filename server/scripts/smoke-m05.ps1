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
function Register-User([string]$Account,[string]$Password,[string]$Nickname){
    $body=@{account=$Account;password=$Password;nickname=$Nickname;phone="13800138000"}|ConvertTo-Json
    $r=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body $body
    Assert-True ($r.data.roleCode -eq "USER") "registered role USER"
}
function Upload-TempPng([string]$Token,[string]$Suffix){
    if(-not (Get-Command curl.exe -ErrorAction SilentlyContinue)){throw "curl.exe is required."}
    $path=Join-Path $env:TEMP "petlink-m05-$Suffix.png"
    $b64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    [IO.File]::WriteAllBytes($path,[Convert]::FromBase64String($b64))
    try{$json=& curl.exe -sS -f -X POST -H "Authorization: Bearer $Token" -F "file=@$path;type=image/png" "$BaseUrl/api/files/temporary";if($LASTEXITCODE-ne 0){throw "upload failed"};return ($json|ConvertFrom-Json).data.token}
    finally{Remove-Item $path -ErrorAction SilentlyContinue}
}
function Submit-App([string]$Token,[string]$AnimalId,[string]$Tag){
    $h=@{Authorization="Bearer $Token"}
    $body=[ordered]@{adoptionReason="reason $Tag";housingCondition="home $Tag";familyMembers="family $Tag";petExperience="experience $Tag";contact="13800138000"}|ConvertTo-Json
    return (Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$AnimalId/adoption-applications" -Headers $h -ContentType "application/json" -Body $body).data
}
function Make-Available([string]$AnimalId,[hashtable]$Headers){
    $d=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$AnimalId" -Headers $Headers).data
    $o=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$AnimalId/status-actions" -Headers $Headers -ContentType "application/json" -Body (@{action="TO_OBSERVING";version=$d.version}|ConvertTo-Json)).data
    $a=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$AnimalId/status-actions" -Headers $Headers -ContentType "application/json" -Body (@{action="OPEN_ADOPTION";version=$o.version}|ConvertTo-Json)).data
    Assert-True ($a.status -eq "AVAILABLE") "animal $AnimalId available"
}

$suffix=([guid]::NewGuid().ToString("N")).Substring(0,10)
$userPassword="PetLinkUser123!";$userA="m05a$suffix";$userB="m05b$suffix"
Write-Host "[1/13] health and actors"
Assert-True ((Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health").status -eq "UP") "health"
Register-User $userA $userPassword "M05UserA"; Register-User $userB $userPassword "M05UserB"
$adminToken=Login $AdminAccount $AdminPassword;$rescuerToken=Login $RescuerAccount $RescuerPassword;$userAToken=Login $userA $userPassword;$userBToken=Login $userB $userPassword
$ah=@{Authorization="Bearer $adminToken"};$rh=@{Authorization="Bearer $rescuerToken"};$uah=@{Authorization="Bearer $userAToken"};$ubh=@{Authorization="Bearer $userBToken"}

Write-Host "[2/13] create approved clue and rescue task"
$clueImage=Upload-TempPng $rescuerToken "$suffix-clue";$animalImage=Upload-TempPng $rescuerToken "$suffix-animal"
$key=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/idempotency-keys" -Headers $rh).data.idempotencyKey
$clueBody=[ordered]@{location="Nanjing M05 Smoke";foundTime=(Get-Date).AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:sszzz");animalDescription="M05 smoke animals";sceneDescription="M05 smoke scene";contact="13800138000";imageTokens=@($clueImage)}|ConvertTo-Json -Depth 6
$ch=@{Authorization="Bearer $rescuerToken";"Idempotency-Key"=$key};$clue=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers $ch -ContentType "application/json" -Body $clueBody).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$($clue.clueId)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)|Out-Null
$task=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$($clue.clueId)/accept" -Headers $rh).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/start" -Headers $rh|Out-Null

Write-Host "[3/13] rescue success creates three animals"
$animals=@(
    [ordered]@{name="M05Withdraw";species="CAT";sex="UNKNOWN";estimatedAgeMonths=10;color="orange";healthCondition="stable";initialHealthRecord=$null;imageTokens=@()},
    [ordered]@{name="M05Reject";species="DOG";sex="FEMALE";estimatedAgeMonths=18;color="white";healthCondition="stable";initialHealthRecord=$null;imageTokens=@()},
    [ordered]@{name="M05Approve";species="CAT";sex="MALE";estimatedAgeMonths=14;color="black";healthCondition="stable";initialHealthRecord="initial check";imageTokens=@($animalImage)}
)
$successBody=[ordered]@{result="SUCCESS";animals=$animals}|ConvertTo-Json -Depth 10
$success=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/result" -Headers $rh -ContentType "application/json" -Body $successBody).data
Assert-True (@($success.animalIds).Count -eq 3) "three animals created"
$animalA=$success.animalIds[0];$animalB=$success.animalIds[1];$animalC=$success.animalIds[2]
Make-Available $animalA $rh;Make-Available $animalB $rh;Make-Available $animalC $rh
$cBefore=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalC" -Headers $rh).data
Assert-True (@($cBefore.images).Count -eq 1) "approval animal has one image";$mediaId=$cBefore.images[0].id

Write-Host "[4/13] withdraw branch and lifetime uniqueness"
$appA=Submit-App $userAToken $animalA "withdraw";Assert-True ($appA.status -eq "PENDING") "withdraw app pending"
$withdraw=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/adoption-applications/$($appA.id)/withdraw" -Headers $uah).data
Assert-True ($withdraw.status -eq "WITHDRAWN") "application withdrawn"
Expect-HttpStatus {Submit-App $userAToken $animalA "duplicate"|Out-Null} 409 "withdrawn application cannot be submitted again"

Write-Host "[5/13] reject branch"
$appB=Submit-App $userAToken $animalB "reject"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/adoption-applications/$($appB.id)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="REJECT"}|ConvertTo-Json)|Out-Null} 400 "reject requires reason"
$reject=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/adoption-applications/$($appB.id)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="REJECT";rejectReason="not suitable"}|ConvertTo-Json)).data
Assert-True ($reject.application.status -eq "REJECTED") "application rejected";Assert-True (-not $reject.adoptionRecord) "reject has no adoption record"

Write-Host "[6/13] two pending applications on approval animal"
$appC1=Submit-App $userAToken $animalC "approve-a";$appC2=Submit-App $userBToken $animalC "approve-b"
$queue=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/adoption-applications?page=1&size=100&status=PENDING" -Headers $ah
Assert-True (@($queue.data.records | Where-Object {$_.id -eq $appC1.id}).Count -eq 1) "admin queue has target"
Assert-True (@($queue.data.records | Where-Object {$_.id -eq $appC2.id}).Count -eq 1) "admin queue has competing app"

Write-Host "[7/13] approve forbids rejectReason field"
$approveWithReason=[ordered]@{decision="APPROVE";rejectReason=$null}|ConvertTo-Json
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/adoption-applications/$($appC1.id)/audit" -Headers $ah -ContentType "application/json" -Body $approveWithReason|Out-Null} 400 "approve rejects rejectReason field"

Write-Host "[8/13] approve creates record, adopts animal, invalidates other"
$approved=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/adoption-applications/$($appC1.id)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)).data
Assert-True ($approved.application.status -eq "APPROVED") "target approved";Assert-True ([bool]$approved.adoptionRecord.id) "adoption record created";$recordId=$approved.adoptionRecord.id
$appC2After=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-applications/$($appC2.id)" -Headers $ubh).data
Assert-True ($appC2After.status -eq "INVALIDATED") "other pending app invalidated"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalC"|Out-Null} 404 "adopted animal not public"

Write-Host "[9/13] owner record list and detail"
$myRecords=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-records/me?page=1&size=100" -Headers $uah
Assert-True (@($myRecords.data.records | Where-Object {$_.id -eq $recordId}).Count -eq 1) "owner record list contains adoption"
$record=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-records/$recordId" -Headers $uah).data
Assert-True ($record.animalId -eq $animalC) "record animal id";Assert-True ([bool]$record.animal.coverImageUrl) "final adopter record has cover url"

Write-Host "[10/13] final adopter media authorization"
$out=Join-Path $env:TEMP "petlink-m05-media-$suffix.bin"
try{Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/media/animal-images/$mediaId" -Headers $uah -OutFile $out|Out-Null;Assert-True ((Get-Item $out).Length -gt 0) "final adopter media bytes"}finally{Remove-Item $out -ErrorAction SilentlyContinue}
Expect-HttpStatus {Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/media/animal-images/$mediaId" -Headers $ubh|Out-Null} 404 "invalidated applicant cannot read adopted media"
Assert-True (-not $appC2After.animal.coverImageUrl) "invalidated historical applicant gets null cover url"

Write-Host "[11/13] rescuer read-only adoption overview"
$overview=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalC/adoption-overview" -Headers $rh).data
Assert-True ($overview.animalStatus -eq "ADOPTED") "overview adopted";Assert-True ($overview.pendingApplicationCount -eq 0) "overview pending zero";Assert-True ($overview.approvedApplicationId -eq $appC1.id) "overview approved app";Assert-True ($overview.adoptionRecordId -eq $recordId) "overview record"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animalC/adoption-overview" -Headers $uah|Out-Null} 403 "USER cannot use rescuer overview"

Write-Host "[12/13] approved applicant cannot withdraw and competing applicant cannot reapply"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/adoption-applications/$($appC1.id)/withdraw" -Headers $uah|Out-Null} 409 "approved application cannot withdraw"
Expect-HttpStatus {Submit-App $userBToken $animalC "again"|Out-Null} 409 "invalidated application remains lifetime unique"

Write-Host "[13/13] final application filters"
$approvedMine=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-applications/me?page=1&size=100&status=APPROVED" -Headers $uah
Assert-True (@($approvedMine.data.records | Where-Object {$_.id -eq $appC1.id}).Count -eq 1) "approved filter finds target"
$invalidMine=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-applications/me?page=1&size=100&status=INVALIDATED" -Headers $ubh
Assert-True (@($invalidMine.data.records | Where-Object {$_.id -eq $appC2.id}).Count -eq 1) "invalidated filter finds competing app"
Write-Host "M05 smoke flow PASSED. withdrawn=$($appA.id) rejected=$($appB.id) approved=$($appC1.id) record=$recordId"
