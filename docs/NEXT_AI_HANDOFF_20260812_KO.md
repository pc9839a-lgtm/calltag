# CallTag 다음 작업 인수인계 — 2026-08-12

## 현재 기준

- 저장소: `pc9839a-lgtm/calltag`
- 브랜치: `agent/calltag-v04422-billing-live`
- 기준 출발 버전: `0.44.22 / 2026081208`
- 현재 배포 후보: **`0.44.24 / 2026081210`**
- 서버: `pc9839a-lgtm/inlet` / `main`
- 패키지: `kr.pagero.calltag`

## 빌드

- Workflow: `CallTag 0.44.24 Google Login Hardened`
- Run ID: `31571247100`
- Artifact ID: `9131477125`
- Artifact: `calltag-v0.44.24-code2026081210-google-login`
- AAB SHA-256: `227f77b6d9de44995f7946a915d35181787dec9b47a7070daf0650da45395878`
- signed AAB / debug APK 빌드 성공
- 기존 Play upload key fingerprint 검증 성공

## Google Play 결제 현재 상태

**신규 결제 E2E 성공 확인됨.**

운영 D1 최신 확인:

- `call_monthly`
- `channel=google_play`
- `status=active`
- `verificationState=verified`
- `autoRenewing=true`
- `lastVerifiedAt=2026-08-12 06:31:11 UTC`
- `expiresAt=2026-09-12T06:31:04.910Z`

현재 Play 상품:

- `call_monthly` 사용
- `message_monthly` 사용
- `all_monthly` 미생성 / 사용 안 함

서버 credential/Publisher API 검증:

- OAuth token 성공
- Android Publisher API 성공
- subscription catalog 성공
- `call_monthly`, `message_monthly` 확인

앱/서버 안전패치:

- `offers.get(0)` 제거
- 모호한 복수 offer/base plan 구매 차단
- 서버 verified 이후 entitlement 반영
- purchaseToken hash 저장
- server acknowledge
- restore
- Web↔Play 중복결제 차단

## Google 로그인 현재 상태

구조:

`Credential Manager → Google ID Token → /api/call/google/id-token → JWT 검증 → CallTag session`

운영 확인:

- 앱 Server Client ID와 서버 audience 일치
- Google JWKS HTTP 200
- 공개키 조회 성공
- `nativeLoginReady=true`

0.44.24 패치:

- Cloudflare 호환 문제를 만들 수 있는 JWKS `AbortSignal.timeout()` 제거
- legacy OAuth token/userinfo 동일 패턴 제거
- Credential Activity 재생성 시 로그인 흐름 복구
- 계정 선택 timeout 90초
- 서버 처리 timeout 25초
- Server Client ID 빈 값 사전 차단
- account picker 유지 (`filterByAuthorizedAccounts=false`, `autoSelect=false`)
- nonce/aud/iss/exp/signature/email_verified 검증 유지

## 지금 바로 할 일

### P0

1. `0.44.24 / 2026081210` AAB를 Play 내부 테스트 업로드
2. Play 설치본에서 Google 로그인 E2E
3. 결제한 계정으로 로그아웃 → Google 재로그인 → `call_monthly` entitlement 유지 확인
4. 앱 삭제/재설치 → 구매 복원 확인

### P1

RTDN/Pub/Sub 구현:

- renewal
- cancel
- expiry
- grace
- account hold
- resume
- refund/voided purchase
- reconciliation

RTDN 수신 후 Android Publisher API를 재조회하여 서버 entitlement를 갱신한다.

## 정책 고정

- CallTag 무료체험: 7일
- 가입 시 추천인: +7일
- 최대 14일
- 무료 종료 후 자동결제 없음
- 추천인 입력은 회원가입 시에만
- `all_monthly`는 사용자가 요청하기 전 생성하지 않음

## 금지

- 기존 고객/통화/메모/일정/문자 데이터 초기화 금지
- purchase callback만 보고 기능 개방 금지
- private key/purchaseToken 원문 노출 금지
- Google 로그인 안정화와 무관한 UI 대규모 변경 혼합 금지
- RTDN 완료 전 구독 lifecycle 전체 완료로 기록 금지

## 우선 읽을 문서

1. `docs/CURRENT_RELEASE_STATUS_20260812_KO.md`
2. `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`
3. 이 문서 `docs/NEXT_AI_HANDOFF_20260812_KO.md`
