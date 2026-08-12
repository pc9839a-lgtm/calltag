# 콜태그 최신 릴리스·운영 상태

기준일: **2026-08-12 15:53 KST**  
Android 저장소: `pc9839a-lgtm/calltag`  
현재 작업 브랜치: `agent/calltag-v04422-billing-live`  
서버 저장소: `pc9839a-lgtm/inlet` / `main`  
패키지명: `kr.pagero.calltag`

> 이 문서를 현재 릴리스 상태의 정본으로 사용한다. 과거 0.44.20~0.44.23 문서와 충돌하면 이 문서와 실제 코드를 우선한다.

## 1. 현재 Android 배포 후보

- 기준 출발 버전: **0.44.22 / versionCode 2026081208**
- 현재 배포 후보: **0.44.24 / versionCode 2026081210**
- minSdk: **26**
- targetSdk / compileSdk: **36**
- applicationId: `kr.pagero.calltag`
- JDK: 17
- Google Play Billing: `9.1.0`
- signed release AAB 빌드 및 jarsigner 검증: **성공**
- Workflow: `CallTag 0.44.24 Google Login Hardened`
- Run ID: **31571247100**
- Artifact ID: **9131477125**
- Artifact: `calltag-v0.44.24-code2026081210-google-login`
- AAB SHA-256: `227f77b6d9de44995f7946a915d35181787dec9b47a7070daf0650da45395878`

Play Console에 `2026081210`을 한 번 업로드하면 이후 versionCode는 반드시 그보다 큰 값을 사용한다.

## 2. Google Play 결제 — 실제 성공 확인

2026-08-12 실제 Play 테스트 결제 후 운영 D1에서 개인정보 없이 상태를 확인했다.

- productCode: `call_monthly`
- channel: `google_play`
- status: `active`
- verificationState: `verified`
- autoRenewing: `true`
- 서버 검증/저장: `2026-08-12 06:31:11 UTC` = `2026-08-12 15:31:11 KST`
- 현재 expiry: `2026-09-12T06:31:04.910Z`

따라서 아래 E2E는 실제로 통과한 것으로 본다.

`Play 구매 → purchaseToken → /api/billing/google/verify → Android Publisher API 검증 → 서버 DB verified 저장 → entitlement active`

### 현재 Play 상품

- `call_monthly` — 사용
- `message_monthly` — 사용
- `all_monthly` — **현재 Play Console에 만들지 않았으며 이번 앱 결제 대상에서도 제외**

0.44.24 앱은 `call_monthly`, `message_monthly`만 조회/구매한다. 서버 Google Play 검증도 이 두 productId만 허용한다.

### 결제 안전 패치

- `offers.get(0)` 임의 선택 제거
- 여러 offer/base plan이 애매하면 결제 중단
- PENDING 구매 처리 유지
- 서버 검증 전 기능 개방 금지
- purchaseToken 원문 장기 저장 금지; hash 저장
- 서버 acknowledge 유지
- 구매 복원 유지
- Web ↔ Google Play 중복 결제 차단 유지

### Google Play 서버 환경

실제 운영 서버에서 서비스 계정 credential로 다음을 확인했다.

- Google OAuth 토큰 발급 성공
- Android Publisher API 접근 성공
- Play 구독 카탈로그 조회 성공
- `call_monthly`, `message_monthly` 조회 성공

현재 readiness는 유효한 Play credential이 존재하면 활성화된다. 긴급 중지는 `GOOGLE_PLAY_BILLING_DISABLED=1`을 사용한다. 과거 `GOOGLE_PLAY_BILLING_ENABLED` / `GOOGLE_PLAY_PRODUCTS_READY` 수동 플래그는 현재 핵심 게이트로 사용하지 않는다.

## 3. Google 로그인 — 재검토 및 패치 완료

기본 로그인 구조:

`Google로 계속하기 → Android Credential Manager → Google ID Token → POST /api/call/google/id-token → 서버 JWT 검증 → CallTag 세션 생성`

### 운영 검증 결과

- Native Server Client ID 설정: **정상**
- 앱 BuildConfig의 Server Client ID와 서버 audience: **일치**
- Google JWKS 접근: **HTTP 200**
- 확인 당시 Google 공개키: **4개**
- nativeLoginReady: **true**
- legacy browser OAuth 설정도 존재

Server/Web Client ID:

`31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`

Android OAuth Client:

- package: `kr.pagero.calltag`
- Android Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- Android Client와 Server/Web Client를 서로 바꾸지 않는다.

### 0.44.24 Google 로그인 안정화

- Cloudflare Pages에서 문제가 될 수 있던 Google JWKS `AbortSignal.timeout()` 제거
- legacy Google code exchange / userinfo 경로의 동일 timeout 패턴 제거
- Credential Manager Activity 재생성 시 계정 선택 흐름 재시작 가능하도록 보강
- 계정 선택 타임아웃 30초 → 90초
- 서버 로그인 처리 타임아웃 25초
- Server Client ID 빈 값 선검사
- `setFilterByAuthorizedAccounts(false)` 유지
- `setAutoSelectEnabled(false)` 유지
- nonce / audience / issuer / expiry / signature / email_verified 검증 유지

## 4. 아직 남은 필수 작업

### P0 — 실기기 Google 로그인 E2E

0.44.24 Play 설치본에서 아래를 확인한다.

1. Google 계정 선택창 표시
2. 계정 선택 후 로그인 완료
3. 기존 이메일 회원은 중복 생성 없이 동일 계정으로 로그인
4. 로그아웃 → Google 재로그인
5. Google 로그인 후 기존 `call_monthly` entitlement 복원
6. 여러 Google 계정이 있는 기기에서 계정 선택 가능
7. 사용자 취소 시 앱이 정상 복귀

### P1 — RTDN

결제 신규 구매는 성공했지만 구독 생명주기 자동 동기화는 아직 남아 있다.

- Google Play RTDN + Pub/Sub 연결
- 갱신
- 사용자 취소
- 만료
- grace period
- account hold
- resume
- refund/voided purchase
- 서버 reconciliation

RTDN 메시지만 믿지 말고 알림을 받은 뒤 Android Publisher API를 다시 조회해 최종 entitlement를 갱신한다.

## 5. 무료체험 / 추천인 정본

CallTag 정책은 다음을 유지한다.

- 일반 신규 가입: **7일 무료**
- 가입 시 추천인 코드 입력: **+7일**
- 최대 **14일 무료**
- 무료 종료 후 자동 결제 없음
- 추천인 코드는 회원가입 시에만 선택 입력

`inlet/functions/api/billing/_shared.js`의 generic legacy 3일/+5일 값과 혼동하지 않는다. CallTag 전용 정책을 깨뜨리지 않는다.

## 6. 데이터·운영 금지선

- 결제/로그인 패치 때문에 기존 고객·통화·메모·일정·문자 데이터를 초기화하지 않는다.
- 앱 purchase callback만 보고 유료 권한을 열지 않는다.
- purchaseToken 원문을 로그/문서/DB에 저장하지 않는다.
- Google 서비스 계정 private key를 GitHub/문서/채팅에 넣지 않는다.
- `all_monthly`를 사용자가 요청하기 전 임의 생성하지 않는다.
- 현재 결제 성공을 RTDN까지 끝난 것으로 오해하지 않는다.

## 7. 다음 작업 순서

1. `0.44.24 / 2026081210` AAB를 Play 내부 테스트에 업로드
2. Play 설치본으로 Google 로그인 E2E 확인
3. 결제 계정 로그아웃/재로그인 후 entitlement 유지 확인
4. 재설치 후 Play 구매 복원 확인
5. RTDN/Pub/Sub 구현
6. 취소·만료·환불·grace/account hold lifecycle QA
