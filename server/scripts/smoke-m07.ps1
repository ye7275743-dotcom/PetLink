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
        if($actual-ne$ExpectedStatus){throw "ASSERT FAILED: $Message (expected HTTP $ExpectedStatus, got $actual)"}
    }
}
function Login([string]$Account,[string]$Password){
    $r=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body (@{account=$Account;password=$Password}|ConvertTo-Json)
    Assert-True ([bool]$r.data.accessToken) "login token for $Account";return $r.data.accessToken
}
function Register-User([string]$Account,[string]$Password,[string]$Nickname){
    $r=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body (@{account=$Account;password=$Password;nickname=$Nickname;phone="13800138000"}|ConvertTo-Json)
    Assert-True ($r.data.roleCode-eq"USER") "registered role USER"
}
function Upload-TempPng([string]$Token,[string]$Suffix){
    if(-not(Get-Command curl.exe -ErrorAction SilentlyContinue)){throw "curl.exe is required."}
    $path=Join-Path $env:TEMP "petlink-m07-$Suffix.png"
    $b64="iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    [IO.File]::WriteAllBytes($path,[Convert]::FromBase64String($b64))
    try{$json=& curl.exe -sS -f -X POST -H "Authorization: Bearer $Token" -F "file=@$path;type=image/png" "$BaseUrl/api/files/temporary";if($LASTEXITCODE-ne 0){throw "upload failed"};return($json|ConvertFrom-Json).data.token}
    finally{Remove-Item $path -ErrorAction SilentlyContinue}
}
function Favorite([string]$Token,[string]$AnimalId){
    $w=Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BaseUrl/api/animals/$AnimalId/favorite" -Headers @{Authorization="Bearer $Token"}
    return [pscustomobject]@{Status=[int]$w.StatusCode;Data=($w.Content|ConvertFrom-Json).data}
}
function Favorite-Concurrently([string]$Token,[string]$AnimalId){
    $startTicks=[DateTime]::UtcNow.AddSeconds(3).Ticks
    $jobScript={param($U,$T,$A,$Ticks);while([DateTime]::UtcNow.Ticks-lt[Int64]$Ticks){Start-Sleep -Milliseconds 10};try{$w=Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$U/api/animals/$A/favorite" -Headers @{Authorization="Bearer $T"};[pscustomobject]@{Status=[int]$w.StatusCode;Body=$w.Content}}catch{[pscustomobject]@{Status=if($_.Exception.Response){[int]$_.Exception.Response.StatusCode}else{-1};Body=$_.Exception.Message}}}
    $jobs=@(Start-Job -ScriptBlock $jobScript -ArgumentList $BaseUrl,$Token,$AnimalId,$startTicks;Start-Job -ScriptBlock $jobScript -ArgumentList $BaseUrl,$Token,$AnimalId,$startTicks)
    try{$jobs|Wait-Job|Out-Null;$out=@();foreach($r in @($jobs|Receive-Job)){$data=$null;if($r.Status-ge 200-and$r.Status-lt 300){$data=($r.Body|ConvertFrom-Json).data};$out+=[pscustomobject]@{Status=[int]$r.Status;Data=$data}};return $out}finally{$jobs|Remove-Job -ErrorAction SilentlyContinue}
}
function Make-Available([string]$AnimalId,[hashtable]$Headers){
    $d=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$AnimalId" -Headers $Headers).data
    $o=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$AnimalId/status-actions" -Headers $Headers -ContentType "application/json" -Body (@{action="TO_OBSERVING";version=$d.version}|ConvertTo-Json)).data
    $a=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$AnimalId/status-actions" -Headers $Headers -ContentType "application/json" -Body (@{action="OPEN_ADOPTION";version=$o.version}|ConvertTo-Json)).data
    Assert-True ($a.status-eq"AVAILABLE") "animal available"
}

$suffix=([guid]::NewGuid().ToString("N")).Substring(0,10);$password="PetLinkUser123!";$user="m07u$suffix";$other="m07o$suffix"
Write-Host "[1/12] health and actors"
Assert-True((Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health").status-eq"UP") "health"
Register-User $user $password "M07User";Register-User $other $password "M07Other"
$adminToken=Login $AdminAccount $AdminPassword;$rescuerToken=Login $RescuerAccount $RescuerPassword;$userToken=Login $user $password;$otherToken=Login $other $password
$ah=@{Authorization="Bearer $adminToken"};$rh=@{Authorization="Bearer $rescuerToken"};$uh=@{Authorization="Bearer $userToken"};$oh=@{Authorization="Bearer $otherToken"}

Write-Host "[2/12] create two AVAILABLE animals with a cover image"
$clueImage=Upload-TempPng $rescuerToken "$suffix-clue";$animalImage=Upload-TempPng $rescuerToken "$suffix-animal"
$key=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/idempotency-keys" -Headers $rh).data.idempotencyKey
$clueBody=[ordered]@{location="M07 Smoke";foundTime=(Get-Date).AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:sszzz");animalDescription="M07 animals";sceneDescription="M07 scene";contact="13800138000";imageTokens=@($clueImage)}|ConvertTo-Json -Depth 6
$clue=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues" -Headers @{Authorization="Bearer $rescuerToken";"Idempotency-Key"=$key} -ContentType "application/json" -Body $clueBody).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/rescue-clues/$($clue.clueId)/audit" -Headers $ah -ContentType "application/json" -Body (@{decision="APPROVE"}|ConvertTo-Json)|Out-Null
$task=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-clues/$($clue.clueId)/accept" -Headers $rh).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/start" -Headers $rh|Out-Null
$animals=@([ordered]@{name="M07Primary";species="CAT";sex="UNKNOWN";estimatedAgeMonths=12;color="orange";healthCondition="stable";initialHealthRecord=$null;imageTokens=@($animalImage)},[ordered]@{name="M07Race";species="DOG";sex="FEMALE";estimatedAgeMonths=18;color="white";healthCondition="stable";initialHealthRecord=$null;imageTokens=@()})
$success=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/rescue-tasks/$($task.taskId)/result" -Headers $rh -ContentType "application/json" -Body ([ordered]@{result="SUCCESS";animals=$animals}|ConvertTo-Json -Depth 10)).data
$animal1=$success.animalIds[0];$animal2=$success.animalIds[1];Make-Available $animal1 $rh;Make-Available $animal2 $rh

Write-Host "[3/12] favorite create and idempotent replay"
$first=Favorite $userToken $animal1;Assert-True($first.Status-eq201) "first favorite 201";Assert-True([bool]$first.Data.animal.coverImageUrl) "visible cover URL"
$again=Favorite $userToken $animal1;Assert-True($again.Status-eq200) "favorite replay 200";Assert-True($again.Data.favoriteId-eq$first.Data.favoriteId) "same favorite id"
$mine=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/favorites/me?page=1&size=20" -Headers $uh).data;Assert-True(@($mine.records|Where-Object{$_.favoriteId-eq$first.Data.favoriteId}).Count-eq1) "favorite list contains relation"

Write-Host "[4/12] concurrent named-unique recovery"
$race=@(Favorite-Concurrently $otherToken $animal2);Assert-True($race.Count-eq2) "two race responses";$statuses=@($race|ForEach-Object{$_.Status}|Sort-Object);Assert-True(($statuses-join",")-eq"200,201") "race returns 200 and 201";Assert-True($race[0].Data.favoriteId-eq$race[1].Data.favoriteId) "race returns same favorite"
$otherMine=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/favorites/me?page=1&size=100" -Headers $oh).data;Assert-True(@($otherMine.records|Where-Object{$_.animal.id-eq[string]$animal2}).Count-eq1) "race creates one row"

Write-Host "[5/12] historical favorite and media projection"
$before=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/animals/$animal1" -Headers $rh).data
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/animals/$animal1/status-actions" -Headers $rh -ContentType "application/json" -Body (@{action="SUSPEND_ADOPTION";version=$before.version;suspendReason="M07 smoke"}|ConvertTo-Json)|Out-Null
$historical=Favorite $userToken $animal1;Assert-True($historical.Status-eq200) "existing favorite ignores later animal status";Assert-True($null-eq$historical.Data.animal.coverImageUrl) "favorite does not grant hidden media"
Expect-HttpStatus {Favorite $otherToken $animal1|Out-Null} 409 "new favorite requires AVAILABLE"

Write-Host "[6/12] idempotent unfavorite"
Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/animals/$animal1/favorite" -Headers $uh|Out-Null
$deletedAgain=(Invoke-RestMethod -Method Delete -Uri "$BaseUrl/api/animals/$animal1/favorite" -Headers $uh).data;Assert-True(-not$deletedAgain.favorited) "repeat delete remains false"

Write-Host "[7/12] create draft and public hiding"
$cw=Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BaseUrl/api/admin/announcements" -Headers $ah -ContentType "application/json" -Body (@{title=" M07 Draft ";content=" draft body "}|ConvertTo-Json)
Assert-True([int]$cw.StatusCode-eq201) "draft create 201";$draft=($cw.Content|ConvertFrom-Json).data;$announcementId=$draft.id;Assert-True($draft.status-eq"DRAFT"-and$draft.version-eq0) "draft state"
$public=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/announcements?page=1&size=100").data;Assert-True(@($public.records|Where-Object{$_.id-eq$announcementId}).Count-eq0) "draft hidden from public list"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/announcements/$announcementId"|Out-Null} 404 "draft public detail hidden"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/announcements" -Headers $uh -ContentType "application/json" -Body (@{title="x";content="y"}|ConvertTo-Json)|Out-Null} 403 "USER cannot create announcement"

Write-Host "[8/12] patch draft with field presence"
$patched=(Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/admin/announcements/$announcementId" -Headers $ah -ContentType "application/json" -Body (@{title="M07 Published Title";version=$draft.version}|ConvertTo-Json)).data
Assert-True($patched.title-eq"M07 Published Title"-and$patched.content-eq"draft body"-and$patched.version-eq1) "title-only patch preserves content"
Expect-HttpStatus {Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/admin/announcements/$announcementId" -Headers $ah -ContentType "application/json" -Body (@{version=$patched.version}|ConvertTo-Json)|Out-Null} 400 "version-only patch rejected"

Write-Host "[9/12] publish and public projection"
$published=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/announcements/$announcementId/publish" -Headers $ah -ContentType "application/json" -Body (@{version=$patched.version}|ConvertTo-Json)).data
Assert-True($published.status-eq"PUBLISHED"-and$published.version-eq2-and[bool]$published.publishedAt) "published state"
$publicDetail=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/announcements/$announcementId").data;Assert-True($publicDetail.title-eq"M07 Published Title"-and$null-eq$publicDetail.version) "public fixed projection"

Write-Host "[10/12] edit published and optimistic conflict"
$edited=(Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/admin/announcements/$announcementId" -Headers $ah -ContentType "application/json" -Body (@{content="updated public body";version=$published.version}|ConvertTo-Json)).data
Assert-True($edited.status-eq"PUBLISHED"-and$edited.version-eq3-and$edited.content-eq"updated public body") "published editable"
Expect-HttpStatus {Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/admin/announcements/$announcementId" -Headers $ah -ContentType "application/json" -Body (@{title="stale";version=$published.version}|ConvertTo-Json)|Out-Null} 409 "stale version rejected"

Write-Host "[11/12] withdraw terminal state"
$withdrawn=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/announcements/$announcementId/withdraw" -Headers $ah -ContentType "application/json" -Body (@{version=$edited.version}|ConvertTo-Json)).data
Assert-True($withdrawn.status-eq"WITHDRAWN"-and$withdrawn.version-eq4-and$withdrawn.publishedAt-eq$published.publishedAt) "withdraw preserves first publish time"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/announcements/$announcementId"|Out-Null} 404 "withdrawn public detail hidden"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/announcements/$announcementId/publish" -Headers $ah -ContentType "application/json" -Body (@{version=$withdrawn.version}|ConvertTo-Json)|Out-Null} 409 "withdrawn cannot republish"
Expect-HttpStatus {Invoke-RestMethod -Method Patch -Uri "$BaseUrl/api/admin/announcements/$announcementId" -Headers $ah -ContentType "application/json" -Body (@{title="no";version=$withdrawn.version}|ConvertTo-Json)|Out-Null} 409 "withdrawn cannot edit"

Write-Host "[12/12] admin filters and role enforcement"
$adminPage=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/announcements?page=1&size=100&status=WITHDRAWN" -Headers $ah).data;Assert-True(@($adminPage.records|Where-Object{$_.id-eq$announcementId}).Count-eq1) "admin status filter"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/announcements" -Headers $uh|Out-Null} 403 "USER cannot use admin list"
Write-Host "M07 smoke flow PASSED. favorite=$($race[0].Data.favoriteId) announcement=$announcementId animal=$animal2"
