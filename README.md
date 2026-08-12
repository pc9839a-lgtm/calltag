# 콜태그 (CallTag)

전화 전 고객 맥락 확인부터 통화 종료 후 메모·할 일·문자 자동화까지 연결하는 Android 고객관리 앱입니다.

## 다음 개발자가 먼저 읽을 문서

1. [`docs/CURRENT_RELEASE_STATUS_20260812_KO.md`](docs/CURRENT_RELEASE_STATUS_20260812_KO.md) — **0.44.21 최신 정본. 더보기 개편·고객센터·Google 로그인·Play 배포·실기기 확인 상태**
2. [`docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`](docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md) — **Google Play 결제/이용권을 다른 AI가 이어서 구현할 때 사용하는 결제 정본**
3. [`docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md) — 제품 기능 현황과 남은 패치. 버전/Google 로그인 정보는 최신 정본 우선
4. [`docs/ANDROID_DEVELOPER_HANDOFF_KO.md`](docs/ANDROID_DEVELOPER_HANDOFF_KO.md) — 코드 구조·데이터/발송 안전 규칙·실기기 검수 기준
5. [`docs/PAGERO_CUSTOMER_INTEGRATION_KO.md`](docs/PAGERO_CUSTOMER_INTEGRATION_KO.md) — 페이지로 문의 조회·ACK·중복 방지
6. [`docs/DESIGN_SYSTEM_KO.md`](docs/DESIGN_SYSTEM_KO.md) — Android UX/UI 규격
7. [`docs/GOOGLE_PLAY_STORE_VISUAL_ASSETS_BRIEF_KO.md`](docs/GOOGLE_PLAY_STORE_VISUAL_ASSETS_BRIEF_KO.md) — Play 스토어 이미지 제작 기준

기획 문서만 보고 구현 완료로 판단하지 않습니다. **실제 코드 → 빌드 결과 → 실제 휴대전화 동작**을 구분해 기록합니다.

## 제품 기준

- 제품명: **콜태그(CallTag)**
- 패키지명: `kr.pagero.calltag`
- 대표 도메인: `https://calltag.pagero.kr`
- 서버 API 대표 도메인: `https://pagero.kr`
- 현재 작업 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`
- 관련 PR: `#80`

## 현재 Android 릴리스

- versionName: **0.44.21**
- versionCode: **2026081207**
- minSdk: **26**
- targetSdk / compileSdk: **36**
- applicationId: `kr.pagero.calltag`
- JDK: 17
- Play 업로드키 서명 release AAB 빌드 및 검증 성공
- Workflow: `CallTag 0.44.21 signed Play AAB`
- 성공 Run ID: `31553364381`
- Artifact ID: `9125103041`
- AAB SHA-256: `e3e71aeb2f67784cc2f1a69df25e4220b2de8fd26537b8032cbba68ba64d6ef5`

Play Console에 한 번 업로드된 versionCode는 재사용하지 않습니다. 현재 최신은 `2026081207`이며 **다음 Play 업로드용 빌드는 `2026081208` 이상**을 사용합니다.

## 더보기 — 0.44.21 정본

더보기 상위 메뉴는 아래 8개만 노출합니다.

1. 계정
2. 이용권
3. 문자 관리
4. 고객 관리
5. 페이지로
6. 파트너
7. 데이터 관리
8. 앱 정보

### 계정

- 이름
- 연락처
- 이메일
- 회원정보 다시 불러오기
- 로그아웃
- 회원탈퇴

계정에는 이용권·약관·백업 등 다른 성격의 기능을 섞지 않습니다.

### 문자 관리

- 통화 후 자동문자
- 문자 문구·이미지
- 그룹·단체문자
- 발송 관리

`통화 후 자동문자`는 더보기의 별도 대형 카드가 아니라 문자 관리 안에 둡니다.

### 고객 관리

- 고객 상태
- 일정 종류
- 통화 후 팝업 제외

### 데이터 관리

- 동기화 상태
- 백업 및 복원

### 앱 정보

- 버전 정보
- 서비스 이용약관
- 개인정보처리방침
- 고객센터

## 고객센터

앱 내 폼으로 문의를 작성하면 로그인 세션이 포함된 `POST /api/call/support` 요청으로 서버에 전달됩니다.

서버는 AWS SES를 통해 기본 수신 주소 **`roadfor@kakao.com`**으로 메일을 보내며 고객이 입력한 이메일을 `Reply-To`로 사용합니다.

문의 유형:

- 일반문의
- 결제
- 오류
- 기타

서버 라우트 운영 배포 및 인증 없는 요청 401 차단 smoke는 성공했습니다. **실제 로그인 사용자 문의 → `roadfor@kakao.com` 받은편지함 도착은 실기기에서 1건 보내 최종 확인해야 합니다.**

## Google 로그인 — 현재 정본

Google 로그인은 브라우저 OAuth가 아니라 **Android Credential Manager**를 사용합니다.

```text
Google로 계속하기
→ GoogleCredentialLoginActivity 직접 실행
→ 앱 위 Google 계정 선택창
→ Google ID Token
→ POST /api/call/google/id-token
→ 콜태그 세션 생성
```

Google 버튼에서 Chrome 또는 `pagero.kr` 웹페이지가 열리면 정상 동작이 아닙니다.

Android OAuth Client:

- package: `kr.pagero.calltag`
- Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- SHA-1: Play Console의 **앱 서명 키 인증서** SHA-1

Server/Web Client ID:

- `31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`
- `BuildConfig.GOOGLE_SERVER_CLIENT_ID`에 사용
- Android Client ID를 server client ID로 바꾸지 않음

## 회원가입 UX 기준

- 필수 항목만 라벨 뒤 빨간 `*`
- 선택 항목은 `[선택]` 등의 반복 문구 없음
- 이름 / 휴대폰번호 / 이메일 / 인증번호 / 비밀번호 필수
- 브랜드/상호 / 업종 / 추천인 코드는 선택
- 추천인 코드는 회원가입 시에만 입력
- 이메일 인증 요청 단계에서 약관 동의를 선행 강제하지 않음
- 최종 가입 제출 시 필수 약관 검사

## 앱 아이콘 기준

- `calltag_launcher_safe.webp` 안전영역 이미지 사용
- legacy `mipmap` + Adaptive Icon `mipmap-anydpi-v26`
- Manifest `android:icon` / `android:roundIcon`
- 삼성·Pixel 런처 마스크에서 전화기/태그 심볼이 잘리지 않아야 함

## 현재 주요 기능

- 통화 수신 고객정보 표시
- 통화 종료 후 작은 팝업 1개로 고객명·메모 처리
- 고객·캘린더·홈·통계·더보기 5개 내비게이션
- 홈 `오늘 할 일`은 오늘 일정만 표시
- 고객 상태·메모·일정 관리
- 문자 템플릿·통화 후 자동문자·후속 예약
- 그룹·단체문자 및 발송 내역
- 페이지로 문의 연동
- 추천인·파트너·정산
- Google Play Billing 기반 및 서버 entitlement 구조
- 암호화 백업·복원 및 복구 흐름

## Play 정책/배포 핵심

- `targetSdk 36`
- `USE_FULL_SCREEN_INTENT` 사용 금지
- 통화기록/SMS/FGS 관련 Play 선언은 실제 앱 사용 목적과 일치해야 함
- Play 앱 서명 SHA-1과 업로드키 SHA-1을 혼동하지 않음
- 내부 테스트 / 비공개 테스트 / 프로덕션은 별도 트랙
- 새 Play AAB는 항상 기존 업로드 versionCode보다 큰 값 사용

## 데이터·발송 안전 규칙

- 기존 고객·일정·메모·문자 데이터를 초기화하지 않음
- DB 변경 시 보존 마이그레이션 작성
- 발송 직전 고객별 허용 여부·발송 제외·중복 방지·SIM·캠페인 상태 재검사
- 불명확한 `SENDING` 작업 자동 재발송 금지
- 일시정지 캠페인 자동 재개 금지
- 고아 작업 자동 발송 금지
- 이미지 문자는 시스템 메시지 앱에서 사용자가 최종 전송
- CI 빌드 성공과 실제 통화/Google 로그인/고객센터 수신 성공을 구분

## 0.44.21 실기기 확인

1. `0.44.21 / 2026081207` 설치 버전 확인
2. 더보기 8개 메뉴 순서 확인
3. 계정이 맨 위인지 확인
4. 통화 후 자동문자가 문자 관리 안에 있는지 확인
5. 앱 정보의 약관/개인정보/버전/고객센터 확인
6. 고객센터 문의 1건 전송 후 `roadfor@kakao.com` 수신 및 Reply-To 확인
7. Google 계정 선택창과 로그인 E2E 확인
8. 런처 아이콘 잘림 확인
9. 통화 종료 후 작은 팝업만 1개 표시되는지 확인

## 빌드

```bash
gradle :app:assembleDebug --stacktrace
gradle :app:bundleRelease --stacktrace
```

release 빌드는 기존 Play 업로드키 환경변수가 필요합니다.

작업 완료 후 **버전·versionCode·Workflow Run·실기기 확인 여부·남은 문제**를 `docs/CURRENT_RELEASE_STATUS_20260812_KO.md`에 먼저 업데이트합니다.
