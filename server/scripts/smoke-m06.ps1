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
    $path=Join-Path $env:TEMP "petlink-m06-$Suffix.png"
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
function Post-FollowUp([string]$Token,[string]$RecordId,[hashtable]$Payload){
    $h=@{Authorization="Bearer $Token"};$body=$Payload|ConvertTo-Json -Depth 6
    $w=Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BaseUrl/api/adoption-records/$RecordId/follow-ups" -Headers $h -ContentType "application/json" -Body $body
    return [pscustomobject]@{Status=[int]$w.StatusCode;Data=($w.Content|ConvertFrom-Json).data}
}
function Post-FollowUpConcurrentPair([string]$Token,[string]$RecordId,[hashtable]$Payload){
    $json=$Payload|ConvertTo-Json -Depth 6 -Compress
    $startTicks=[DateTime]::UtcNow.AddSeconds(3).Ticks
    $jobScript={
        param($JobBaseUrl,$JobToken,$JobRecordId,$JobJson,$JobStartTicks)
        while([DateTime]::UtcNow.Ticks -lt [Int64]$JobStartTicks){Start-Sleep -Milliseconds 10}
        try{
            $headers=@{Authorization="Bearer $JobToken"}
            $w=Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$JobBaseUrl/api/adoption-records/$JobRecordId/follow-ups" -Headers $headers -ContentType "application/json" -Body $JobJson
            [pscustomobject]@{Status=[int]$w.StatusCode;Body=$w.Content}
        }catch{
            $status=-1
            if($_.Exception.Response){$status=[int]$_.Exception.Response.StatusCode}
            [pscustomobject]@{Status=$status;Body=$_.Exception.Message}
        }
    }
    $jobs=@(
        Start-Job -ScriptBlock $jobScript -ArgumentList $BaseUrl,$Token,$RecordId,$json,$startTicks
        Start-Job -ScriptBlock $jobScript -ArgumentList $BaseUrl,$Token,$RecordId,$json,$startTicks
    )
    try{
        $jobs|Wait-Job|Out-Null
        $raw=@($jobs|Receive-Job)
        $out=@()
        foreach($r in $raw){
            $data=$null
            if($r.Status -ge 200 -and $r.Status -lt 300){$data=($r.Body|ConvertFrom-Json).data}
            $out += [pscustomobject]@{Status=[int]$r.Status;Data=$data;Raw=$r.Body}
        }
        return $out
    }finally{
        $jobs|Remove-Job -ErrorAction SilentlyContinue
    }
}

$suffix=([guid]::NewGuid().ToString("N")).Substring(0,10)
$userPassword="PetLinkUser123!";$adopter="m06a$suffix";$outsider="m06o$suffix"
Write-Host "[1/11] health and actors"
Assert-True ((Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health").status -eq "UP") "health"
Register-User $adopter $userPassword "M06Adopter";Register-User $outsider $userPassword "M06Outsider"
$adminToken=Login $AdminAccount $AdminPassword;$rescuerToken=Login $RescuerAccount $RescuerPassword;$userToken=Login $adopter $userPassword;$outsiderToken=Login $outsider $userPassword
$ah=@{Authorization="Bearer $adminToken"};$rh=@{Authorization="Bearer $rescuerToken"};$uh=@{Authorization="Bearer $userToken"};$oh=@{Authorization="Bearer $outsiderToken"}

Write-Host "[2/11] create rescue prerequisite with two animals"
$clueImage=Upload-TempPng $rescuerToken "$suffix-clue"
$key=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/idempotency-keys" -Headers $rh).data.idempotencyKey
$clueBody=[ordered]@{location="Nanjing M06 Smoke";foundTime=(Get-Date).AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:sszzz");animalDescription="M06 smoke animals";sceneDescription="M06 smoke scene";contact="13800138000";imageTokens=@($clueImage)}|ConvertTo-Json -Depth 6
$ch=@{Authorization="Bearer $rescuerToken";"Idempotency-Key"=$key};$clue=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers $ch -ContentType "application/json" -Body $clueBody).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$($clue.clueId)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)|Out-Null
$task=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$($clue.clueId)/accept" -Headers $rh).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/start" -Headers $rh|Out-Null
$animals=@(
    [ordered]@{name="M06Primary";species="CAT";sex="UNKNOWN";estimatedAgeMonths=12;color="orange";healthCondition="stable";initialHealthRecord=$null;imageTokens=@()},
    [ordered]@{name="M06Conflict";species="DOG";sex="FEMALE";estimatedAgeMonths=18;color="white";healthCondition="stable";initialHealthRecord=$null;imageTokens=@()}
)
$success=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/result" -Headers $rh -ContentType "application/json" -Body ([ordered]@{result="SUCCESS";animals=$animals}|ConvertTo-Json -Depth 10)).data
Assert-True (@($success.animalIds).Count -eq 2) "two animals created";$animal1=$success.animalIds[0];$animal2=$success.animalIds[1]
Make-Available $animal1 $rh;Make-Available $animal2 $rh

Write-Host "[3/11] approve adoption records"
$app1=Submit-App $userToken $animal1 "primary";$app2=Submit-App $userToken $animal2 "conflict"
$approved1=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/adoption-applications/$($app1.id)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)).data
$approved2=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/adoption-applications/$($app2.id)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)).data
$record1=$approved1.adoptionRecord.id;$record2=$approved2.adoptionRecord.id
Assert-True ([bool]$record1 -and [bool]$record2) "two adoption records"

Write-Host "[4/11] reject empty follow-up"
$empty=[ordered]@{idempotencyKey=[guid]::NewGuid().ToString();content=" ";healthCondition=$null;imageTokens=@()}
Expect-HttpStatus {Post-FollowUp $userToken $record1 $empty|Out-Null} 400 "follow-up requires text health or image"

Write-Host "[5/11] first follow-up with two images"
$img1=Upload-TempPng $userToken "$suffix-fu1";$img2=Upload-TempPng $userToken "$suffix-fu2";$followKey=[guid]::NewGuid().ToString().ToLower()
$payload=[ordered]@{idempotencyKey=$followKey;content=" settled in well ";healthCondition=" appetite normal ";imageTokens=@($img1,$img2)}
$created=Post-FollowUp $userToken $record1 $payload
Assert-True ($created.Status -eq 201) "first follow-up HTTP 201";Assert-True (@($created.Data.images).Count -eq 2) "two follow-up images"
Assert-True ($created.Data.images[0].sortOrder -eq 1 -and $created.Data.images[1].sortOrder -eq 2) "image client order";$followId=$created.Data.id;$imageId=$created.Data.images[0].id

Write-Host "[6/11] idempotent replay before old imageTokens validation"
$replayPayload=[ordered]@{idempotencyKey=$followKey;content="changed but ignored";healthCondition=$null;imageTokens=@("not-a-valid-old-token")}
$replay=Post-FollowUp $userToken $record1 $replayPayload
Assert-True ($replay.Status -eq 200) "idempotent replay HTTP 200";Assert-True ($replay.Data.id -eq $followId) "idempotent replay returns first record"
# IDEMPOTENCY_KEY_CONFLICT: the same permanent key cannot name another adoption record.
Expect-HttpStatus {Post-FollowUp $userToken $record2 $replayPayload|Out-Null} 409 "IDEMPOTENCY_KEY_CONFLICT on different adoption record"

Write-Host "[7/11] append-only history order and hidden outsider"
$second=Post-FollowUp $userToken $record1 ([ordered]@{idempotencyKey=[guid]::NewGuid().ToString().ToLower();content="second visit";healthCondition=$null;imageTokens=@()})
Assert-True ($second.Status -eq 201) "second follow-up created"
$history=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-records/$record1/follow-ups" -Headers $uh).data
Assert-True (@($history).Count -eq 2) "owner sees two follow-ups";Assert-True ($history[0].id -eq $followId -and $history[1].id -eq $second.Data.id) "history ascending order"
$detail=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/follow-ups/$followId" -Headers $uh).data;Assert-True ($detail.id -eq $followId) "owner detail"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/follow-ups/$followId" -Headers $oh|Out-Null} 404 "outsider cannot read follow-up"

Write-Host "[8/11] real concurrent permanent-idempotency race"
$concurrentKey=[guid]::NewGuid().ToString().ToLower()
$concurrentPayload=[ordered]@{idempotencyKey=$concurrentKey;content="concurrent follow-up";healthCondition=$null;imageTokens=@()}
$concurrent=@(Post-FollowUpConcurrentPair $userToken $record1 $concurrentPayload)
Assert-True ($concurrent.Count -eq 2) "two concurrent responses returned"
$statuses=@($concurrent|ForEach-Object {$_.Status}|Sort-Object)
Assert-True (($statuses -join ",") -eq "200,201") "concurrent same-key requests return one 201 and one 200"
Assert-True ([bool]$concurrent[0].Data.id -and $concurrent[0].Data.id -eq $concurrent[1].Data.id) "concurrent same-key requests return the same followUpId"
$concurrentFollowId=$concurrent[0].Data.id
$historyAfterConcurrent=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/adoption-records/$record1/follow-ups" -Headers $uh).data
Assert-True (@($historyAfterConcurrent).Count -eq 3) "concurrent pair creates exactly one additional FollowUpRecord"
Assert-True (@($historyAfterConcurrent|Where-Object {$_.id -eq $concurrentFollowId}).Count -eq 1) "concurrent followUpId exists once in history"
Expect-HttpStatus {Post-FollowUp $userToken $record2 $concurrentPayload|Out-Null} 409 "IDEMPOTENCY_KEY_CONFLICT after concurrent create on different adoption record"

Write-Host "[9/11] responsible rescuer and admin query views"
$rpage=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescuer/follow-ups?page=1&size=100&animalId=$animal1" -Headers $rh
Assert-True (@($rpage.data.records|Where-Object {$_.id -eq $followId}).Count -eq 1) "responsible rescuer page contains follow-up"
$apage=Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/follow-ups?page=1&size=100&animalId=$animal1&userId=$($approved1.adoptionRecord.userId)&adoptionRecordId=$record1" -Headers $ah
Assert-True (@($apage.data.records|Where-Object {$_.id -eq $followId}).Count -eq 1) "admin filtered page contains follow-up"

Write-Host "[10/11] follow-up media visibility"
foreach($pair in @(@($uh,"owner"),@($rh,"rescuer"),@($ah,"admin"))){$out=Join-Path $env:TEMP "petlink-m06-media-$suffix-$($pair[1]).bin";try{Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/media/follow-up-images/$imageId" -Headers $pair[0] -OutFile $out|Out-Null;Assert-True ((Get-Item $out).Length -gt 0) "$($pair[1]) media bytes"}finally{Remove-Item $out -ErrorAction SilentlyContinue}}
Expect-HttpStatus {Invoke-WebRequest -UseBasicParsing -Method Get -Uri "$BaseUrl/api/media/follow-up-images/$imageId" -Headers $oh|Out-Null} 404 "outsider cannot read follow-up image"

Write-Host "[11/11] role and scope enforcement"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/follow-ups" -Headers $uh|Out-Null} 403 "USER cannot use admin follow-up list"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescuer/follow-ups" -Headers $uh|Out-Null} 403 "USER cannot use rescuer follow-up list"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescuer/follow-ups?animalId=999999999" -Headers $rh|Out-Null} 404 "rescuer animal filter must be in responsible scope"
Write-Host "M06 smoke flow PASSED. followUp=$followId adoptionRecord=$record1 second=$($second.Data.id)"
