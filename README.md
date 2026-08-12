# 콜태그 (CallTag)

전화 전 고객 맥락 확인부터 통화 종료 후 메모·할 일·문자 자동화까지 연결하는 Android 고객관리 앱입니다.

## 현재 정본 문서

다음 순서로 확인합니다.

1. [`docs/CURRENT_RELEASE_STATUS_20260812_KO.md`](docs/CURRENT_RELEASE_STATUS_20260812_KO.md) — **현재 릴리스/운영 상태 정본**
2. [`docs/NEXT_AI_HANDOFF_20260812_KO.md`](docs/NEXT_AI_HANDOFF_20260812_KO.md) — **다음 AI/개발자 즉시 인수인계**
3. [`docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`](docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md) — **Google Play Billing 정본**
4. [`docs/ANDROID_DEVELOPER_HANDOFF_KO.md`](docs/ANDROID_DEVELOPER_HANDOFF_KO.md) — Android 구조/데이터 안전 규칙
5. [`docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md) — 제품 기능 로드맵

과거 문서와 충돌하면 **현재 코드 → CURRENT_RELEASE_STATUS → NEXT_AI_HANDOFF** 순으로 우선합니다.

## 현재 Android 배포 후보

- branch: `agent/calltag-v04422-billing-live`
- base: `0.44.22 / 2026081208`
- current: **0.44.24 / 2026081210**
- applicationId: `kr.pagero.calltag`
- minSdk: 26
- targetSdk / compileSdk: 36
- JDK: 17
- Google Play Billing: 9.1.0
- signed AAB build: **성공**
- Workflow Run: `31571247100`
- Artifact ID: `9131477125`
- AAB SHA-256: `227f77b6d9de44995f7946a915d35181787dec9b47a7070daf0650da45395878`

Play Console에 versionCode `2026081210`을 한 번 업로드하면 이후에는 더 큰 versionCode를 사용합니다.

## Google Play Billing 현재 상태

실제 Google Play 테스트 결제가 운영 서버까지 성공했습니다.

- `call_monthly`
- `channel=google_play`
- `status=active`
- `verificationState=verified`
- `autoRenewing=true`

현재 Play 결제 대상:

- `call_monthly`
- `message_monthly`

`all_monthly`는 현재 Play Console에 만들지 않았고 앱/서버 Play 구매 대상에서도 제외합니다.

서버에서는 실제 Google OAuth, Android Publisher API, subscription catalog 접근과 두 상품 존재까지 확인했습니다.

## Google 로그인 현재 상태

기본 경로:

```text
Google로 계속하기
→ Android Credential Manager
→ Google ID Token
→ POST /api/call/google/id-token
→ 서버 JWT 검증
→ CallTag 세션 생성
```

운영 확인:

- 앱 Server Client ID와 서버 audience 일치
- Google JWKS HTTP 200
- native login readiness 정상

0.44.24에서 Cloudflare JWKS 호출 호환성, Activity 재생성, 계정 선택 timeout을 보강했습니다.

## 다음 우선순위

1. `0.44.24 / 2026081210` Play 내부 테스트 업로드
2. 실제 Play 설치본 Google 로그인 E2E
3. 결제 계정 로그아웃/재로그인 후 entitlement 유지 확인
4. 재설치 후 구매 복원
5. RTDN/Pub/Sub 구현
6. 갱신/취소/만료/refund/grace/account hold lifecycle QA

## 무료 이용 정책

- 신규 가입 7일
- 가입 시 추천인 코드 +7일
- 최대 14일
- 무료 종료 후 자동 결제 없음
- 추천인 코드는 회원가입 시에만 선택 입력

## 데이터 안전 규칙

- 고객·통화·메모·일정·문자 데이터 초기화 금지
- 서버 verified 전 유료 권한 개방 금지
- purchaseToken 원문 저장/로그 금지
- Google 서비스 계정 private key GitHub/채팅 노출 금지
- `all_monthly` 임의 생성 금지

## 빌드

```bash
gradle :app:assembleDebug --stacktrace
gradle :app:bundleRelease --stacktrace
```

release 빌드는 기존 Google Play upload key 설정이 필요합니다.
