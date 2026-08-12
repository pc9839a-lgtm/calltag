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

function Convert-SecureStringToPlainText([Security.SecureString]$SecureString) {
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureString)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

$keytool = Resolve-Keytool
$out = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $out | Out-Null

$keystore = Join-Path $out 'calltag-upload-reset.jks'
$certificate = Join-Path $out 'upload_certificate.pem'
$fingerprint = Join-Path $out 'upload_key_fingerprint.txt'
$existing = Test-Path $keystore

Write-Host ''
if ($existing) {
    Write-Host '기존에 생성된 CallTag 업로드 JKS를 찾았습니다. 새 키를 만들지 않고 이어서 진행합니다.'
    Write-Host "JKS: $keystore"
    $securePassword = Read-Host '기존 키 저장소 비밀번호 입력' -AsSecureString
}
else {
    Write-Host 'CallTag Google Play 업로드키 재설정용 새 키를 이 PC에서만 생성합니다.'
    Write-Host '비밀번호는 화면에 표시되지 않으며 파일에 저장하지 않습니다.'
    $securePassword = Read-Host '새 키 저장소 비밀번호 입력' -AsSecureString
    $securePasswordConfirm = Read-Host '새 비밀번호 다시 입력' -AsSecureString

    $plain1 = Convert-SecureStringToPlainText $securePassword
    $plain2 = Convert-SecureStringToPlainText $securePasswordConfirm
    try {
        if ($plain1.Length -lt 6) { throw '키 저장소 비밀번호는 6자 이상이어야 합니다.' }
        if ($plain1 -cne $plain2) { throw '비밀번호가 일치하지 않습니다.' }
    }
    finally {
        $plain1 = $null
        $plain2 = $null
        $securePasswordConfirm.Dispose()
    }
}

$plainPassword = Convert-SecureStringToPlainText $securePassword
$securePassword.Dispose()
$env:CALLTAG_KEYTOOL_STOREPASS = $plainPassword
$plainPassword = $null

try {
    if (-not $existing) {
        & $keytool -genkeypair -v `
            -keystore $keystore `
            -storetype JKS `
            -alias calltag-upload `
            -keyalg RSA `
            -keysize 4096 `
            -validity 10000 `
            -dname 'CN=CallTag Upload, OU=Mobile, O=Pagero, L=Jeongeup, ST=Jeollabuk-do, C=KR' `
            '-storepass:env' CALLTAG_KEYTOOL_STOREPASS `
            '-keypass:env' CALLTAG_KEYTOOL_STOREPASS
        if ($LASTEXITCODE -ne 0) { throw '새 upload keystore 생성에 실패했습니다.' }
    }

    # Validate the password/alias before doing anything else.
    $details = & $keytool -list -v `
        -keystore $keystore `
        -alias calltag-upload `
        '-storepass:env' CALLTAG_KEYTOOL_STOREPASS
    if ($LASTEXITCODE -ne 0) { throw 'JKS 비밀번호 또는 calltag-upload alias 확인에 실패했습니다.' }

    if (-not (Test-Path $certificate)) {
        & $keytool -exportcert -rfc `
            -keystore $keystore `
            -alias calltag-upload `
            -file $certificate `
            '-storepass:env' CALLTAG_KEYTOOL_STOREPASS
        if ($LASTEXITCODE -ne 0) { throw 'PEM 인증서 export에 실패했습니다.' }
    }
    else {
        Write-Host "기존 PEM 인증서를 그대로 사용합니다: $certificate"
    }

    $details | Set-Content -Encoding UTF8 $fingerprint
    $shaLine = $details | Where-Object { $_ -match 'SHA256:' } | Select-Object -First 1
    if (-not $shaLine) { throw 'SHA-256 fingerprint를 읽지 못했습니다.' }

    Write-Host ''
    Write-Host '생성/확인 완료'
    Write-Host "JKS(절대 공유/커밋 금지): $keystore"
    Write-Host "Play Console 제출용 공개 PEM: $certificate"
    Write-Host "Fingerprint 기록: $fingerprint"
    Write-Host "새 공개 SHA-256: $shaLine"
    Write-Host ''
    Write-Host 'Play Console에서 upload key reset이 승인된 뒤 GitHub Actions Secrets에 다음 4개를 직접 등록하세요:'
    Write-Host '  CALLTAG_UPLOAD_KEYSTORE_BASE64'
    Write-Host '  CALLTAG_UPLOAD_STORE_PASSWORD'
    Write-Host '  CALLTAG_UPLOAD_KEY_ALIAS   (값: calltag-upload)'
    Write-Host '  CALLTAG_UPLOAD_KEY_PASSWORD (store password와 동일)'
    Write-Host ''

    $copy = Read-Host 'JKS의 Base64를 Windows 클립보드에만 복사할까요? (y/N)'
    if ($copy -match '^[Yy]$') {
        $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
        Set-Clipboard -Value $base64
        $base64 = $null
        Write-Host 'Base64가 클립보드에 복사되었습니다. GitHub Secret에 붙여넣은 뒤 다른 값을 복사해 클립보드를 덮어쓰세요.'
    }

    Write-Host ''
    Write-Host '주의: upload_certificate.pem과 SHA-256 fingerprint는 공개 정보지만 JKS와 비밀번호는 비밀입니다.'
}
finally {
    Remove-Item Env:CALLTAG_KEYTOOL_STOREPASS -ErrorAction SilentlyContinue
}
