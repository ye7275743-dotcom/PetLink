param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminAccount = "admin",
    [string]$AdminPassword = $env:PETLINK_SMOKE_ADMIN_PASSWORD
)
$ErrorActionPreference = "Stop"
function Assert-True([bool]$Condition,[string]$Message){if(-not $Condition){throw "ASSERT FAILED: $Message"}}
function Expect-HttpStatus([scriptblock]$Action,[int]$ExpectedStatus,[string]$Message){
    try{& $Action;throw "ASSERT FAILED: $Message"}catch{
        if($_.Exception.Message -like "ASSERT FAILED:*"){throw}
        if(-not $_.Exception.Response){throw}
        $actual=[int]$_.Exception.Response.StatusCode
        if($actual-ne$ExpectedStatus){throw "ASSERT FAILED: $Message (expected HTTP $ExpectedStatus, got $actual)"}
    }
}
function Login([string]$Account,[string]$Password){
    $r=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body (@{account=$Account;password=$Password}|ConvertTo-Json)
    Assert-True([bool]$r.data.accessToken) "login token for $Account";return $r.data.accessToken
}
function Register-User([string]$Account,[string]$Password,[string]$Nickname){
    $r=Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/register" -ContentType "application/json" -Body (@{account=$Account;password=$Password;nickname=$Nickname;phone="13800138000"}|ConvertTo-Json)
    Assert-True($r.data.roleCode-eq"USER") "registered USER $Account"
}
function Has-Property($Object,[string]$Name){return $null-ne$Object.PSObject.Properties[$Name]}

$suffix=([guid]::NewGuid().ToString("N")).Substring(0,10);$password="PetLinkUser123!";$managed="m08m$suffix";$promoted="m08p$suffix"
Write-Host "[1/12] health and actors"
Assert-True((Invoke-RestMethod -Method Get -Uri "$BaseUrl/actuator/health").status-eq"UP") "health"
Register-User $managed $password "M08 Managed";Register-User $promoted $password "M08 Promoted"
$adminToken=Login $AdminAccount $AdminPassword;$managedToken=Login $managed $password;$promotedToken=Login $promoted $password
$ah=@{Authorization="Bearer $adminToken"};$mh=@{Authorization="Bearer $managedToken"};$ph=@{Authorization="Bearer $promotedToken"}

Write-Host "[2/12] user list filters and fixed projection"
$users=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/users?page=1&size=100&roleCode=USER&status=ENABLED&keyword=$suffix" -Headers $ah).data
Assert-True($users.total-ge2) "two filtered users"
$managedRow=@($users.records|Where-Object{$_.account-eq$managed})[0];$promotedRow=@($users.records|Where-Object{$_.account-eq$promoted})[0]
Assert-True([bool]$managedRow.id-and[bool]$promotedRow.id) "registered users listed"
Assert-True(-not(Has-Property $managedRow "passwordHash")) "passwordHash omitted"

Write-Host "[3/12] user detail derived statistics"
$detail=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/users/$($managedRow.id)" -Headers $ah).data
foreach($name in @("publishedClueCount","rescueTaskCount","adoptionApplicationCount","adoptionRecordCount")){Assert-True(Has-Property $detail.statistics $name) "statistics.$name present"}

Write-Host "[4/12] self-disable protection and role enforcement"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/1/disable" -Headers $ah|Out-Null} 403 "ADMIN cannot disable self"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/users" -Headers $mh|Out-Null} 403 "USER cannot list users"

Write-Host "[5/12] disable invalidates existing JWT immediately"
$disabled=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/$($managedRow.id)/disable" -Headers $ah).data
Assert-True($disabled.status-eq"DISABLED") "disabled response"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/users/me" -Headers $mh|Out-Null} 403 "disabled JWT rejected"

Write-Host "[6/12] enable restores existing JWT and state conflict"
$enabled=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/$($managedRow.id)/enable" -Headers $ah).data
Assert-True($enabled.status-eq"ENABLED") "enabled response"
Assert-True((Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/users/me" -Headers $mh).data.account-eq$managed) "old JWT works after enable"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/$($managedRow.id)/enable" -Headers $ah|Out-Null} 409 "enable already enabled conflicts"

Write-Host "[7/12] promotion refreshes role from database"
$promotion=(Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/$($promotedRow.id)/promote-rescuer" -Headers $ah).data
Assert-True($promotion.roleCode-eq"RESCUER") "promotion response"
Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/rescue-tasks/me?page=1&size=20" -Headers $ph|Out-Null
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/$($promotedRow.id)/promote-rescuer" -Headers $ah|Out-Null} 409 "repeat promotion conflicts"
Expect-HttpStatus {Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/admin/users/999999999/enable" -Headers $ah|Out-Null} 404 "missing user action"

Write-Host "[8/12] rescue task supervision filters"
$tasks=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/rescue-tasks?page=1&size=100" -Headers $ah).data
Assert-True($tasks.total-ge1) "task supervision has prerequisite rows"
$task=$tasks.records[0];$taskFiltered=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/rescue-tasks?page=1&size=100&status=$($task.status)&clueId=$($task.clueId)" -Headers $ah).data
Assert-True(@($taskFiltered.records|Where-Object{$_.id-eq$task.id}).Count-eq1) "task filters"

Write-Host "[9/12] animal supervision filters"
$animals=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/animals?page=1&size=100" -Headers $ah).data
Assert-True($animals.total-ge1) "animal supervision has prerequisite rows"
$animal=$animals.records[0];$animalFiltered=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/animals?page=1&size=100&status=$($animal.status)&species=$($animal.species)" -Headers $ah).data
Assert-True(@($animalFiltered.records|Where-Object{$_.id-eq$animal.id}).Count-eq1) "animal filters"

Write-Host "[10/12] adoption record supervision filters"
$records=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/adoption-records?page=1&size=100" -Headers $ah).data
Assert-True($records.total-ge1) "adoption record supervision has prerequisite rows"
$record=$records.records[0];$recordFiltered=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/adoption-records?page=1&size=100&userId=$($record.userId)&animalId=$($record.animalId)&applicationId=$($record.applicationId)" -Headers $ah).data
Assert-True(@($recordFiltered.records|Where-Object{$_.id-eq$record.id}).Count-eq1) "record filters"

Write-Host "[11/12] complete overview status maps"
$overview=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/stats/overview" -Headers $ah).data
foreach($name in @("total","enabled","disabled","users","rescuers","admins")){Assert-True(Has-Property $overview.users $name) "users.$name present"}
foreach($name in @("PENDING_REVIEW","REJECTED","WITHDRAWN","WAITING_ACCEPT","CONVERTED","CLOSED")){Assert-True(Has-Property $overview.rescueClues $name) "rescueClues.$name present"}
foreach($name in @("WAITING_START","IN_PROGRESS","SUCCESS","FAILED","CANCELED")){Assert-True(Has-Property $overview.rescueTasks $name) "rescueTasks.$name present"}
foreach($name in @("TREATING","OBSERVING","AVAILABLE","SUSPENDED","ADOPTED")){Assert-True(Has-Property $overview.animals $name) "animals.$name present"}
foreach($name in @("PENDING","APPROVED","REJECTED","WITHDRAWN","INVALIDATED")){Assert-True(Has-Property $overview.adoptionApplications $name) "adoptionApplications.$name present"}
Assert-True(Has-Property $overview.adoptionRecords "total") "adoptionRecords.total present";Assert-True(Has-Property $overview.followUps "total") "followUps.total present"

Write-Host "[12/12] inclusive DAY trends and invalid ranges"
$to=(Get-Date).ToString("yyyy-MM-dd");$from=(Get-Date).AddDays(-2).ToString("yyyy-MM-dd")
$trends=(Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/stats/trends?from=$from&to=$to&granularity=DAY" -Headers $ah).data
Assert-True($trends.granularity-eq"DAY"-and$trends.zoneId-eq"Asia/Shanghai") "trend metadata"
Assert-True(@($trends.points).Count-eq3) "inclusive zero-filled date points"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/stats/trends?from=$from&to=$to&granularity=WEEK" -Headers $ah|Out-Null} 400 "invalid granularity"
Expect-HttpStatus {Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/admin/stats/trends?from=$to&to=$from&granularity=DAY" -Headers $ah|Out-Null} 400 "reverse range"
Write-Host "M08 smoke flow PASSED. managed=$($managedRow.id) promoted=$($promotedRow.id) task=$($task.id) animal=$($animal.id) record=$($record.id)"
