# 콜태그 (CallTag)

전화 전 고객 맥락 확인부터 통화 종료 후 메모·할 일·문자 자동화까지 연결하는 Android 고객관리 앱입니다.

## 다음 개발자가 먼저 읽을 문서

1. [`docs/CURRENT_RELEASE_STATUS_20260812_KO.md`](docs/CURRENT_RELEASE_STATUS_20260812_KO.md) — **0.44.20 최신 정본. Google 로그인·회원가입 UX·Play 배포·앱 아이콘·실기기 확인 상태**
2. [`docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md) — 제품 기능 현황과 남은 패치. 버전/Google 로그인 정보는 최신 정본 우선
3. [`docs/ANDROID_DEVELOPER_HANDOFF_KO.md`](docs/ANDROID_DEVELOPER_HANDOFF_KO.md) — 코드 구조·데이터/발송 안전 규칙·실기기 검수 기준
4. [`docs/PAGERO_CUSTOMER_INTEGRATION_KO.md`](docs/PAGERO_CUSTOMER_INTEGRATION_KO.md) — 페이지로 문의 조회·ACK·중복 방지
5. [`docs/DESIGN_SYSTEM_KO.md`](docs/DESIGN_SYSTEM_KO.md) — Android UX/UI 규격
6. [`docs/GOOGLE_PLAY_STORE_VISUAL_ASSETS_BRIEF_KO.md`](docs/GOOGLE_PLAY_STORE_VISUAL_ASSETS_BRIEF_KO.md) — Play 스토어 이미지 제작 기준
7. [`docs/PRODUCT_SPEC_KO.md`](docs/PRODUCT_SPEC_KO.md) — 제품 정의

기획 문서만 보고 구현 완료로 판단하지 않습니다. **실제 코드 → 빌드 결과 → 실제 휴대전화 동작**을 구분해 기록합니다.

## 제품 기준

- 제품명: **콜태그(CallTag)**
- 패키지명: `kr.pagero.calltag`
- 대표 도메인: `https://calltag.pagero.kr`
- 서버 API 대표 도메인: `https://pagero.kr`
- 현재 인증/Play 작업 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`
- 관련 PR: `#80`

## 현재 Android 릴리스

- versionName: **0.44.20**
- versionCode: **2026081206**
- minSdk: **26**
- targetSdk / compileSdk: **36**
- applicationId: `kr.pagero.calltag`
- JDK: 17
- Play 업로드키 서명 release AAB 빌드 성공
- Workflow: `CallTag 0.44.20 signed Play AAB`
- Run ID: `31549775038`
- Artifact ID: `9123840577`

### versionCode 주의

Play Console에 한 번 업로드한 versionCode는 취소해도 재사용하지 않습니다.

현재 최신은 `2026081206`이며 **다음 Play 빌드는 `2026081207` 이상**을 사용합니다.

## Google 로그인 — 0.44.20 정본

현재 Google 로그인은 브라우저 OAuth가 아니라 **Android Credential Manager**를 사용합니다.

정상 UX:

```text
Google로 계속하기
→ 앱 위 Google 계정 선택창
→ Google ID Token
→ POST /api/call/google/id-token
→ 콜태그 세션 생성
→ 앱 진입
```

`LoginActivity`의 Google 버튼은 `GoogleCredentialLoginActivity.class`를 명시적으로 직접 실행합니다.

사용 의존성:

- `androidx.credentials:credentials:1.6.0`
- `androidx.credentials:credentials-play-services-auth:1.6.0`
- `com.google.android.libraries.identity.googleid:googleid:1.2.0`

**Google 버튼을 눌렀을 때 Chrome 또는 `pagero.kr` 웹페이지가 열리면 현재 정본 동작이 아닙니다.** 설치 버전과 빌드 계보를 먼저 확인합니다.

### OAuth Client 구분

Android OAuth Client:

- 유형: Android
- 패키지: `kr.pagero.calltag`
- Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- SHA-1: **Play Console의 앱 서명 키 인증서 SHA-1**

Server/Web Client ID:

- `31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`
- `BuildConfig.GOOGLE_SERVER_CLIENT_ID`에 사용
- Android Client ID를 server client ID로 바꾸지 않음

서버 `pc9839a-lgtm/inlet`의 `/api/call/google/id-token`은 운영 배포되어 있으며 잘못된 토큰을 `401 / GOOGLE_ID_TOKEN_INVALID`로 거부하는 smoke test를 통과했습니다.

### 실제 단말 상태

코드·서버·서명 AAB 빌드는 완료됐지만 **0.44.20 Google 계정 선택창과 로그인 전체 E2E는 실제 휴대전화 확인 전까지 완료로 기록하지 않습니다.**

## 회원가입 UX 최신 기준

- 필수 항목만 라벨 뒤 **빨간 `*`** 표시
- 선택 항목은 `[선택]`/배지 없이 무표시
- `[필수]`, `[선택]`, `필수 정보`, `선택 정보` 반복 금지
- 이메일 인증 요청 단계에서 약관 동의를 먼저 강제하지 않음
- 최종 가입 제출 시 필수 약관 검사
- 추천인 안내는 짧은 한 줄 수준으로 유지
- 장문 설명보다 입력폼을 우선

## 앱 아이콘 최신 기준

Play 스토어 아이콘과 실제 휴대폰 런처 아이콘은 별개입니다.

현재 release 빌드는:

- `calltag_launcher_safe.webp`
- `mipmap` legacy icon
- `mipmap-anydpi-v26` Adaptive Icon
- Manifest `android:icon` / `android:roundIcon`

을 사용합니다.

전화기/태그 심볼은 삼성·Pixel의 원형/둥근사각형 마스크에서 잘리지 않도록 **안전영역 안에 배치**합니다. 스토어 아이콘만 변경하고 설치 아이콘까지 변경됐다고 판단하지 않습니다.

## 현재 주요 기능

- 통화 수신 고객정보 표시
- 통화 종료 후 **작은 팝업 1개**로 고객명·메모 처리
- 고객·캘린더·홈·통계·더보기 5개 내비게이션
- 홈 `오늘 할 일`은 오늘 일정만 표시
- 고객 상태·메모·일정 관리
- 문자 템플릿·통화 후 자동문자·후속 예약
- 그룹·단체문자 및 발송 내역
- 페이지로 문의 연동
- 추천인·파트너·정산 메뉴
- Google Play Billing 의존성 및 결제 UI 기반
- 암호화 백업·복원 및 복구 흐름

## Play 정책/배포 핵심

- `targetSdk 36`
- `USE_FULL_SCREEN_INTENT` 사용 금지
- 통화기록/SMS/FGS 관련 Play 선언은 실제 앱 사용 목적과 일치해야 함
- Play 앱 서명 SHA-1과 업로드키 SHA-1을 혼동하지 않음
- 내부 테스트 / 비공개 테스트 / 프로덕션은 별도 트랙
- 새 AAB는 항상 기존 Play versionCode보다 큰 값을 사용

## 데이터·발송 안전 규칙

- 기존 고객·일정·메모·문자 데이터를 초기화하지 않음
- DB 변경 시 보존 마이그레이션 작성
- 발송 직전 고객별 허용 여부·발송 제외·중복 방지·SIM·캠페인 상태 재검사
- 불명확한 `SENDING` 작업 자동 재발송 금지
- 일시정지 캠페인 자동 재개 금지
- 고아 작업 자동 발송 금지
- 이미지 문자는 시스템 메시지 앱에서 사용자가 최종 전송
- CI 빌드 성공과 실제 통화/Google 로그인/런처 동작 성공을 구분

## 지금 바로 확인할 항목

1. Play Console에 `0.44.20 / 2026081206` 배포
2. 테스트 기기에서 실제 설치 버전 확인
3. Google Cloud Android OAuth Client의 패키지 `kr.pagero.calltag` 확인
4. Android OAuth Client에 **Play 앱 서명 키 SHA-1** 등록 확인
5. `Google로 계속하기` → 앱 위 계정 선택창 확인
6. 계정 선택 → 콜태그 로그인 완료 확인
7. 회원가입 필수 빨간 `*` 및 선택 무표시 확인
8. 홈/앱서랍 아이콘 잘림 여부 확인
9. 통화 종료 후 작은 팝업만 1개 표시되는지 실기기 QA

## 빌드

```bash
gradle :app:assembleDebug --stacktrace
gradle :app:bundleRelease --stacktrace
```

release 빌드는 기존 Play 업로드키 환경변수가 필요합니다.

작업 완료 후 **버전·versionCode·Workflow Run·실기기 확인 여부·남은 문제**를 `docs/CURRENT_RELEASE_STATUS_20260812_KO.md`에 먼저 업데이트합니다.
