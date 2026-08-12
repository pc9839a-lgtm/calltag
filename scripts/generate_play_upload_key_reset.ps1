param(
    [string]$OutputDirectory = "$PSScriptRoot\..\private\play-upload-reset"
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Resolve-Keytool {
    $command = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
        if (Test-Path $candidate) { return $candidate }
    }

    $androidStudioKeytool = 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
    if (Test-Path $androidStudioKeytool) { return $androidStudioKeytool }

    throw 'keytool.exe를 찾지 못했습니다. Android Studio 또는 JDK 17을 설치한 뒤 다시 실행하세요.'
}

$keytool = Resolve-Keytool
$out = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $out | Out-Null

$keystore = Join-Path $out 'calltag-upload-reset.jks'
$certificate = Join-Path $out 'upload_certificate.pem'
$fingerprint = Join-Path $out 'upload_key_fingerprint.txt'

if (Test-Path $keystore) {
    throw "기존 키를 덮어쓰지 않습니다: $keystore`n다른 OutputDirectory를 지정하거나 기존 폴더를 안전하게 백업한 뒤 다시 실행하세요."
}

Write-Host ''
Write-Host 'CallTag Google Play 업로드키 재설정용 새 키를 이 PC에서만 생성합니다.'
Write-Host '비밀번호는 keytool이 직접 물어봅니다. 이 스크립트는 비밀번호를 저장하거나 출력하지 않습니다.'
Write-Host '키 비밀번호 질문에서는 Enter를 눌러 keystore 비밀번호와 동일하게 사용해도 됩니다.'
Write-Host ''

& $keytool -genkeypair -v `
    -keystore $keystore `
    -storetype JKS `
    -alias calltag-upload `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -dname 'CN=CallTag Upload, OU=Mobile, O=Pagero, L=Jeongeup, ST=Jeollabuk-do, C=KR'
if ($LASTEXITCODE -ne 0) { throw '새 upload keystore 생성에 실패했습니다.' }

& $keytool -exportcert -rfc `
    -keystore $keystore `
    -alias calltag-upload `
    -file $certificate
if ($LASTEXITCODE -ne 0) { throw 'PEM 인증서 export에 실패했습니다.' }

$details = & $keytool -list -v -keystore $keystore -alias calltag-upload 2>&1
if ($LASTEXITCODE -ne 0) { throw '업로드키 fingerprint 확인에 실패했습니다.' }
$details | Set-Content -Encoding UTF8 $fingerprint

$shaLine = $details | Where-Object { $_ -match 'SHA256:' } | Select-Object -First 1

Write-Host ''
Write-Host '생성 완료'
Write-Host "JKS(절대 공유/커밋 금지): $keystore"
Write-Host "Play Console 제출용 공개 PEM: $certificate"
Write-Host "Fingerprint 기록: $fingerprint"
if ($shaLine) { Write-Host "새 공개 SHA-256: $shaLine" }
Write-Host ''
Write-Host 'Play Console에서 upload key reset이 승인된 뒤 GitHub Actions Secrets에 다음 4개를 직접 등록하세요:'
Write-Host '  CALLTAG_UPLOAD_KEYSTORE_BASE64'
Write-Host '  CALLTAG_UPLOAD_STORE_PASSWORD'
Write-Host '  CALLTAG_UPLOAD_KEY_ALIAS   (값: calltag-upload)'
Write-Host '  CALLTAG_UPLOAD_KEY_PASSWORD'
Write-Host ''

$copy = Read-Host 'JKS의 Base64를 Windows 클립보드에만 복사할까요? (y/N)'
if ($copy -match '^[Yy]$') {
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
    Set-Clipboard -Value $base64
    Remove-Variable base64 -ErrorAction SilentlyContinue
    Write-Host 'Base64가 클립보드에 복사되었습니다. GitHub Secret 값에 붙여넣은 뒤 다른 값을 복사해 클립보드를 덮어쓰세요.'
}

Write-Host ''
Write-Host '주의: upload_certificate.pem과 SHA-256 fingerprint는 공개 정보지만 JKS와 비밀번호는 비밀입니다.'
