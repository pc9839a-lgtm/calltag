# CallTag 결제 구현 인수인계 정본

기준일: **2026-08-12 15:53 KST**  
Android: `pc9839a-lgtm/calltag` / `agent/calltag-v04422-billing-live`  
Server: `pc9839a-lgtm/inlet` / `main`  
패키지: `kr.pagero.calltag`  
현재 앱 배포 후보: **0.44.24 / versionCode 2026081210**

> 결제 작업은 이 문서와 `CURRENT_RELEASE_STATUS_20260812_KO.md`를 우선한다. 과거 문서의 3일/+5일 정책, `all_monthly` 즉시 생성 지시, 수동 readiness 플래그 지시는 현재 상태와 다를 수 있다.

## 1. 현재 Play 상품 정본

현재 Play Console과 앱 결제 대상은 아래 두 개다.

| productId | 용도 | 상태 |
|---|---|---|
| `call_monthly` | 전화관리 | 사용 |
| `message_monthly` | 문자자동화 | 사용 |
| `all_monthly` | 통합권 | 현재 미생성 / Play 구매 대상 제외 |

사용자가 통합권은 지금 만들지 않기로 했으므로 임의로 `all_monthly`를 만들거나 앱 결제 목록에 다시 넣지 않는다.

0.44.24의 `PlayBillingManager`는 `call_monthly`, `message_monthly`만 ProductDetails 조회/구매/복원 대상으로 처리한다.

## 2. 실제 결제 성공 확인

2026-08-12 실제 Google Play 테스트 구매 후 운영 D1에서 다음 상태를 확인했다.

- productCode: `call_monthly`
- channel: `google_play`
- status: `active`
- verificationState: `verified`
- autoRenewing: `true`
- lastVerifiedAt: `2026-08-12 06:31:11 UTC`
- expiresAt: `2026-09-12T06:31:04.910Z`

따라서 신규 결제 핵심 E2E는 통과했다.

`Play checkout → purchaseToken → POST /api/billing/google/verify → subscriptionsv2.get → 서버 검증 → DB verified upsert → entitlement active`

## 3. 서버 Google Play 검증 구조

핵심 파일:

- `inlet/functions/api/billing/_shared.js`
- `inlet/functions/api/billing/_readiness.js`
- `inlet/functions/api/billing/google/verify.js`
- `inlet/functions/api/billing/google/restore.js`

서버는 다음을 수행한다.

1. packageName `kr.pagero.calltag` 확인
2. productId를 `call_monthly`, `message_monthly`로 제한
3. Android Publisher `purchases.subscriptionsv2.get` 호출
4. 실제 line item productId 일치 확인
5. 상태/만료 확인
6. Web 활성 구독과 충돌하면 차단
7. purchaseToken 원문 대신 SHA-256 hash 저장
8. `verification_state=verified` 저장
9. acknowledgement pending이면 서버에서 acknowledge
10. 최종 entitlement 반환

앱에서 별도 acknowledge를 중복 구현하지 않는다.

## 4. 운영 credential 검증 결과

서비스 계정 키를 채팅/GitHub에 노출하지 않고 운영 서버에서 실제 API 접근만 검증했다.

- Google OAuth token 발급: 성공
- Android Publisher API 접근: 성공
- Google Play subscription catalog 조회: 성공
- `call_monthly`: 확인
- `message_monthly`: 확인

Google Play private key는 Cloudflare Secret에만 둔다.

## 5. readiness 현재 구조

과거 문서의 아래 두 수동 플래그를 반드시 1로 바꿔야 한다는 지시는 더 이상 정본이 아니다.

- `GOOGLE_PLAY_BILLING_ENABLED`
- `GOOGLE_PLAY_PRODUCTS_READY`

현재 서버는 유효한 `GOOGLE_PLAY_CLIENT_EMAIL` + `GOOGLE_PLAY_PRIVATE_KEY`가 구성되어 있고 명시적 중지 상태가 아니면 Play 결제를 사용 가능으로 판단한다.

긴급 중지:

`GOOGLE_PLAY_BILLING_DISABLED=1`

## 6. Android 결제 안전장치

0.44.24에서 유지해야 하는 사항:

- BillingClient `9.1.0`
- 결제 직전 서버 entitlement 재조회
- 서버 확인 실패 시 결제창 미오픈
- Web 활성 구독 시 Play 중복결제 차단
- 기존 활성 Play 구독 시 추가 구매 차단
- PENDING 상태 별도 처리
- 서버 verified 이후에만 권한 갱신
- Google Play 구매 복원
- `obfuscatedAccountId`
- 임의 `offers.get(0)` 선택 금지
- 복수 offer/base plan이 모호하면 구매 차단

## 7. 무료 이용 / 추천인 정본

CallTag 정책:

- 일반 신규 가입: **7일 무료**
- 가입 시 추천인 코드 적용: **+7일**
- 최대 **14일**
- 무료 종료 후 자동 결제 없음
- 추천인 코드는 회원가입 시에만 선택 입력

서버 generic `_shared.js`의 legacy 3일/+5일 값을 CallTag 정책으로 간주하지 않는다. CallTag 전용 trial 경로를 우선한다.

## 8. 아직 미완료 — RTDN

신규 결제 검증은 성공했지만 구독 생명주기 자동 동기화는 아직 남아 있다.

다음 작업:

1. Google Cloud Pub/Sub topic 생성
2. `google-play-developer-notifications@system.gserviceaccount.com`에 Pub/Sub Publisher 권한
3. Play Console RTDN topic 등록
4. test notification 확인
5. 서버 subscriber/push endpoint 구현
6. 알림 수신 후 Android Publisher API 재조회
7. 아래 상태 entitlement 반영
   - renewal
   - user cancel
   - expiry
   - grace period
   - account hold
   - resume
   - refund / voided purchase
8. 정기 reconciliation 추가

RTDN payload만으로 최종 상태를 결정하지 않는다. 알림 후 Publisher API를 다시 조회한다.

## 9. 추가 E2E 체크

남은 실제 단말 테스트:

- 앱 종료 후 구매 복원
- 로그아웃/재로그인 후 entitlement 유지
- 재설치 후 복원
- Web 구독 중 Play 결제 차단
- Play 구독 중 Web checkout 차단
- pending purchase
- 취소 후 만료 전 접근
- 만료 후 기능 제한
- refund
- grace/account hold
- 동일 purchaseToken 재전송 idempotency

## 10. 데이터 보존 원칙

구독이 만료되어도 삭제하지 않는다.

- 고객
- 통화/상담 이력
- 메모
- 일정
- 문자 템플릿
- 발송 기록

제한할 것은 유료 실행 권한이다. 재구독 시 기존 데이터 위에서 기능이 재개되어야 한다.

## 11. 다음 AI가 먼저 볼 파일

Android:

1. `docs/CURRENT_RELEASE_STATUS_20260812_KO.md`
2. `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`
3. `app/src/main/java/kr/pagero/calltag/PlayBillingManager.java`
4. `app/src/main/java/kr/pagero/calltag/BillingEntitlementActivity.java`
5. `app/src/main/java/kr/pagero/calltag/FeatureEntitlementStore.java`
6. `app/src/main/java/kr/pagero/calltag/AuthApiClient.java`

Server:

1. `functions/api/billing/_shared.js`
2. `functions/api/billing/_readiness.js`
3. `functions/api/billing/google/verify.js`
4. `functions/api/billing/google/restore.js`
5. `functions/api/billing/entitlements.js`
6. `functions/api/billing/trial-policy.js`

## 12. 작업 금지선

- `all_monthly` 임의 생성 금지
- 서버 entitlement 검증 우회 금지
- purchase callback만 보고 기능 개방 금지
- purchaseToken 원문 저장/로그 금지
- 서비스 계정 private key GitHub/채팅 노출 금지
- 결제 패치와 무관한 대규모 UI 개편 혼합 금지
- 기존 고객/통화/문자/일정 데이터 초기화 금지
- 신규 결제 성공을 RTDN 완료로 오해하지 않음
