param(
    [string]$BaseUrl = "http://localhost:8082/api",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [int]$PageSize = 5,
    [Nullable[long]]$SubmitProblemId = $null,
    [string]$SubmitLanguage = "PYTHON",
    [int]$PollTimes = 10,
    [int]$PollIntervalSec = 1
)

$ErrorActionPreference = "Stop"

function Assert-Success {
    param(
        [string]$Step,
        $Resp
    )

    if ($null -eq $Resp) {
        throw "[$Step] Empty response"
    }

    if ($Resp.code -ne 200) {
        throw "[$Step] Failed: code=$($Resp.code), message=$($Resp.message)"
    }

    Write-Host "[$Step] OK"
}

Write-Host "BaseUrl: $BaseUrl"

$loginBody = @{
    username = $Username
    password = $Password
} | ConvertTo-Json

$loginResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -Body $loginBody -ContentType "application/json"
Assert-Success -Step "Login" -Resp $loginResp

$token = $loginResp.data.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "[Login] token is empty"
}
Write-Host "[Login] token received"

$headers = @{
    Authorization = "Bearer $token"
}

$problemResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/problem/list?page=1&size=$PageSize" -Headers $headers
Assert-Success -Step "ProblemList" -Resp $problemResp
$problemCount = @($problemResp.data.records).Count
Write-Host "[ProblemList] records: $problemCount"

$submissionResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/submission/list?page=1&size=$PageSize" -Headers $headers
Assert-Success -Step "SubmissionList" -Resp $submissionResp
$submissionCount = @($submissionResp.data.records).Count
Write-Host "[SubmissionList] records: $submissionCount"

if ($SubmitProblemId -ne $null) {
    $code = @"
print(input())
"@
    if ($SubmitLanguage -eq "JAVA") {
        $code = @"
import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        if (sc.hasNextLine()) System.out.print(sc.nextLine());
    }
}
"@
    } elseif ($SubmitLanguage -eq "CPP") {
        $code = @"
#include <bits/stdc++.h>
using namespace std;
int main() {
    string s;
    if (getline(cin, s)) cout << s;
    return 0;
}
"@
    }

    $submitBody = @{
        problemId = $SubmitProblemId
        language = $SubmitLanguage
        code = $code
    } | ConvertTo-Json

    $submitResp = Invoke-RestMethod -Method Post -Uri "$BaseUrl/submission/submit" -Headers $headers -Body $submitBody -ContentType "application/json"
    Assert-Success -Step "SubmitCode" -Resp $submitResp
    $submissionId = $submitResp.data.id
    Write-Host "[SubmitCode] submission id: $submissionId"

    for ($i = 1; $i -le $PollTimes; $i++) {
        $statusResp = Invoke-RestMethod -Method Get -Uri "$BaseUrl/submission/$submissionId/status" -Headers $headers
        Assert-Success -Step "SubmissionStatus#$i" -Resp $statusResp
        $status = $statusResp.data.status
        Write-Host "[SubmissionStatus#$i] $status"
        if ($status -notin @("PENDING", "JUDGING")) {
            break
        }
        Start-Sleep -Seconds $PollIntervalSec
    }
}

Write-Host "Smoke test completed."
