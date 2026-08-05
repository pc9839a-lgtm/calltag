# CallTag v0.43.0 Google Play 내부 테스트 출시 인계

최종 갱신: 2026-08-05

## 출시 파일

Google Play 내부 테스트에는 APK가 아니라 다음 서명된 Android App Bundle을 업로드한다.

```text
calltag-v0.43.0-play-internal-aab/app-release.aab
```

- applicationId: `kr.pagero.calltag`
- versionName: `0.43.0`
- versionCode: `68`
- compileSdk: `36`
- targetSdk: `36`
- minSdk: `26`
- build type: `release`
- debuggable: `false`

신규 Google Play 앱은 Android App Bundle 형식으로 게시하고 Play App Signing을 사용한다.

## 업로드 키

### 저장소 Secret이 이미 등록된 경우

아래 GitHub Actions Secret을 사용해 고정 업로드 키로 AAB를 서명한다.

```text
CALLTAG_UPLOAD_KEYSTORE_BASE64
CALLTAG_UPLOAD_STORE_PASSWORD
CALLTAG_UPLOAD_KEY_ALIAS
CALLTAG_UPLOAD_KEY_PASSWORD
```

### 아직 업로드 키가 없는 경우

Play 내부 테스트 workflow가 1회용 bootstrap upload key를 생성한다.

생성되는 별도 artifact:

```text
calltag-v0.43.0-upload-key-backup
```

내부 파일:

```text
calltag-upload.jks
CALLTAG_UPLOAD_KEY_CREDENTIALS.txt
CALLTAG_UPLOAD_KEY_FINGERPRINTS.txt
```

이 backup을 반드시 내려받아 오프라인으로 보관한다. 첫 AAB를 Google Play에 올린 뒤 다음 버전도 동일 업로드 키로 서명해야 한다.

backup에 적힌 값을 GitHub 저장소 Settings > Secrets and variables > Actions에 위 네 Secret으로 등록한다. keystore 파일은 base64 한 줄로 변환해 `CALLTAG_UPLOAD_KEYSTORE_BASE64`에 넣는다.

```bash
base64 -w 0 calltag-upload.jks
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('calltag-upload.jks'))
```

업로드 키나 비밀번호를 저장소 파일, PR 본문, 이슈, 로그에 넣지 않는다.

## Google Play Console 업로드 순서

1. Play Console에서 CallTag 앱 `kr.pagero.calltag`를 연다.
2. 테스트 및 출시 > 테스트 > 내부 테스트로 이동한다.
3. 새 버전을 만든다.
4. Play App Signing이 표시되면 등록한다.
5. 서명된 `app-release.aab`를 업로드한다.
6. 출시명 예시: `0.43.0-internal-1`.
7. 출시 노트를 입력한다.
8. 오류·경고를 확인한다.
9. 테스터 이메일 목록을 연결한다.
10. 내부 테스트 버전을 출시한다.

## 내부 테스트 출시 노트

```text
콜태그 첫 Google Play 내부 테스트 버전입니다.
- 통화 종료 후 고객 태그·메모·후속 일정 관리
- 자동문자 템플릿과 후속 예약
- 페이지로 문의 연동
- 고객 데이터 보호·복구 기반
- 연결 기기 관리와 백그라운드 자동 보호
```

## 제한 권한 선언

현재 Manifest에는 통화기록과 SMS 관련 제한 권한이 포함돼 있다.

```text
READ_CALL_LOG
SEND_SMS
```

AAB를 올리면 Play Console의 앱 콘텐츠에 권한 선언 알림이 나타날 수 있다. 내부 테스트 트랙도 선언 대상이다.

실제 사용 목적을 정확하게 설명한다.

- `READ_CALL_LOG`: 통화 종료를 식별하고 해당 번호의 고객 기록·후속 업무 화면을 제공하는 CallTag 핵심 CRM 기능
- `SEND_SMS`: 사용자가 직접 설정한 통화 후·부재중·후속 문자 자동화
- 통화 녹음 수집 없음
- 전체 SMS 수신함 읽기 없음
- 광고·판매·프로파일링 사용 없음
- 서버 데이터 보호는 별도 동의 기본 OFF

Google이 요구하는 허용 사용 사례와 앱 실제 동작이 일치하지 않으면 권한을 유지한 채 제출하지 말고 정책 구조를 다시 검토해야 한다.

## 심사용 로그인

로그인이 필요한 앱이므로 Play Console 앱 액세스 항목에 전용 심사 계정을 등록한다.

- 실제 운영자 계정 사용 금지
- 이메일은 Gmail일 필요 없음
- 이메일 인증 완료 상태
- 정지되지 않은 계정
- 테스트에 필요한 무료 이용권 또는 entitlement 부여
- 로그인 후 권한 설정 방법을 영어로 기재

## 출시 전 확인

- AAB 서명 검증 성공
- package `kr.pagero.calltag` 확인
- versionCode `68`이 기존 업로드보다 큼
- target API 36 확인
- Firebase BuildConfig 네 값이 비어 있지 않음
- release 빌드가 debug 서명 아님
- 업로드 키 backup 다운로드 완료
- 개인정보처리방침 URL 준비
- 이용약관 URL 준비
- 데이터 보안 양식 작성 준비
- SMS·Call Log 권한 선언 준비
- Foreground service·full-screen intent 선언 준비
- 심사용 계정 준비

## 현재 서버 상태

- CallTag 서버 동기화 운영 flag OFF
- 운영 D1 migration 미적용
- 고객 데이터 서버 저장 미시작
- Google 로그인 별도 작업 중
- Google Play Billing 실제 상품 활성화 전

따라서 내부 테스트 앱에서 기존 이메일 로그인을 사용하며, 데이터 보호 메뉴를 켜더라도 서버가 준비 중이라는 상태로 안전하게 종료된다.
