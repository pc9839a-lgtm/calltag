# CallTag 결제 구현 인수인계 정본

- 작성일: 2026-08-12
- 목적: 다른 AI/개발자가 추가 질문 없이 CallTag 결제 작업을 이어서 진행할 수 있도록 현재 정책, 실제 코드 상태, 서버 구조, 미완료 항목을 한 문서로 고정한다.
- Android 저장소: `pc9839a-lgtm/calltag`
- 기준 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`
- 현재 Android 버전: `0.44.20` / `versionCode 2026081206`
- 서버 저장소: `pc9839a-lgtm/inlet` / `main`
- Android 패키지: `kr.pagero.calltag`

> **이 문서를 결제 구현의 우선 인수인계 문서로 사용한다.**
> 과거 `BILLING_REFERRAL_APP_IMPLEMENTATION_KO.md` 및 서버의 오래된 결제 문서에는 3일/+5일, 1,900원/990원 등 이전 정책이 섞여 있으므로 그대로 복사해 구현하지 않는다. 현재 코드와 최신 제품 지시를 아래에서 분리해 적었다.

---

## 1. 서비스와 결제 경계

CallTag와 PageRo는 같은 계정을 사용할 수 있지만 기능적으로 별도 서비스다.

- **CallTag**: Android 통화 기반 CRM, 통화 후 고객관리, 문자 자동화.
- **PageRo**: 웹 랜딩페이지 제작, 문의 수집, 페이지 관리.
- PageRo 문의를 CallTag 고객으로 연결할 수 있지만, PageRo 단독 사용자에게 CallTag 설치를 강제하지 않는다.
- 결제 채널이 여러 개여도 최종 기능 권한은 **서버 entitlement** 한 곳에서 결정한다.

### 결제 채널

- CallTag Android: `google_play`
- PageRo 웹: `web`
- 앱 또는 웹에서 결제 성공 화면을 봤다는 이유만으로 기능을 직접 열지 않는다.
- 서버 검증 결과가 `active`인 경우에만 유료 기능을 활성화한다.

---

## 2. 최신 제품 표시 정책

### PageRo 웹 요금제

최신 제품 지시 기준으로 PageRo는 다음 체계를 사용한다.

| 구분 | 월 금액 | 비고 |
|---|---:|---|
| 무료 | 0원 | 기본 무료 플랜 |
| 클래식 | 3,500원 | PageRo 유료 플랜 |
| 프로 | 5,500원 | PageRo 상위 플랜 |
| CallTag 통합 | 6,000원 | PageRo 설정에서 CallTag 연동/통합 상품으로만 표기 |

**표기 원칙**

- PageRo 설정 화면에서 `콜태그 요금제`라는 별도 표를 만들지 않는다.
- PageRo 쪽에는 `CallTag 통합 6,000원`만 별도 통합 옵션으로 노출한다.
- 추천인, 파트너, 정산은 같은 메뉴로 합치지 말고 각각 분리한다.

### 현재 CallTag Android 코드에 남아 있는 상품

현재 앱 코드는 아래 3개 Play 구독 상품을 조회하도록 작성되어 있다.

| productId | 현재 코드 표시 | 현재 코드 금액 |
|---|---|---:|
| `all_monthly` | 통합권 | 6,000원 |
| `call_monthly` | 전화관리 | 1,900원 |
| `message_monthly` | 문자자동화 | 990원 |

관련 파일:

- `app/src/main/java/kr/pagero/calltag/FeatureEntitlementStore.java`
- `app/src/main/java/kr/pagero/calltag/BillingEntitlementActivity.java`
- `app/src/main/java/kr/pagero/calltag/PlayBillingManager.java`

**중요:** 1,900원/990원은 현재 코드에 존재하는 기존 상품값이지, 새 Play Console 상품을 무조건 이 값으로 생성하라는 뜻이 아니다. 결제 공개 전 최종 판매 카탈로그와 맞춰야 한다. 특히 PageRo 쪽 최신 지시와 혼동하지 않는다.

---

## 3. 무료 이용 / 추천인 정책

### 현재 실제 CallTag 서버 동작

현재 `inlet/main/functions/api/billing/trial-policy.js`는 CallTag 요청에 대해 다음 정책을 강제한다.

- 일반 가입: 통합 기능 7일 무료
- 가입 시 추천인 코드 적용: +7일
- 최대 총 14일
- 무료기간 종료 후 자동 결제 없음
- 서버 시각 기준
- 앱 재설치 또는 기기 날짜 변경으로 무료기간을 다시 만들지 않음

CallTag 앱은 `X-Pagero-Product: calltag` 헤더를 보내므로 `/api/billing/entitlements`에서 `resolveCallTagEntitlement()`가 적용된다.

### 추천인 UI 정책

- 추천인 코드 **입력은 회원가입 시에만 선택 항목으로 제공**한다.
- 가입 완료 후 설정 화면에서 추천인 코드를 새로 입력하는 UI를 노출하지 않는다.
- 본인 추천 금지.
- 한 계정 한 번만 적용.
- 추천인과 파트너 기능은 분리한다.
- 파트너는 추천코드 복사/성과/정산 영역으로 별도 취급한다.

### 현재 서버의 주의할 불일치

`inlet/main/functions/api/billing/_shared.js`의 generic 기본값에는 아직 과거 값이 남아 있다.

- `TRIAL_BASE_DAYS = 3`
- `REFERRAL_BONUS_DAYS = 5`

반면 CallTag 전용 `trial-policy.js`는 7일/+7일을 사용한다. 현재 CallTag entitlement 경로는 전용 정책이 우선 적용되므로 앱에서는 7일/+7일로 보이지만, 향후 서버 리팩터링 시 이 두 정책을 하나의 정본 상수로 통합해야 한다.

---

## 4. 현재 Android 결제 구현 상태

### 이미 구현됨

`app/build.gradle`

- `com.android.billingclient:billing:9.1.0` 적용.

`BillingEntitlementActivity.java`

- 서버 이용권 조회.
- 현재 이용 상태 표시.
- Play 결제 가능 여부 표시.
- 상품 선택 버튼.
- 구매 복원.
- Google Play 구독 관리 화면 이동.
- 결제 직전 서버 entitlement 재조회.
- 활성 웹 구독이 있으면 앱 중복결제 차단.
- 활성 Play 구독이 있으면 추가 구매 차단.
- 서버 확인 실패 시 결제 자체를 시작하지 않음.

`PlayBillingManager.java`

- BillingClient 연결.
- `SUBS` 상품 조회.
- 구매 플로우 실행.
- `obfuscatedAccountId` 설정.
- 구매 결과 수신.
- PENDING 상태 처리.
- 서버 `/api/billing/google/verify` 검증.
- 기존 구독 `/api/billing/google/restore` 복원.
- 서버 검증 완료 후 entitlement 로컬 캐시 갱신.

`FeatureEntitlementStore.java`

- 서버 응답 캐시.
- 서버 시각 기반 만료 추정.
- `web` / `google_play` 채널 구분.
- 기능별 접근권한 판정.
- Play 결제 공개 여부 게이트.

`AuthApiClient.java`

- `GET /api/billing/entitlements`
- `GET /api/billing/subscriptions`
- `GET /api/billing/readiness`
- `POST /api/billing/web/precheck`
- `POST /api/billing/google/verify`
- `POST /api/billing/google/restore`

---

## 5. 현재 서버 결제 구현 상태

서버 핵심 위치:

- `functions/api/billing/entitlements.js`
- `functions/api/billing/subscriptions.js`
- `functions/api/billing/readiness.js`
- `functions/api/billing/_readiness.js`
- `functions/api/billing/_shared.js`
- `functions/api/billing/trial-policy.js`
- `functions/api/billing/google/verify.js`
- `functions/api/billing/google/restore.js`
- `functions/api/billing/web/*`
- `functions/api/billing/_commissions.js`

### Google Play 검증

서버 `verifyGoogleSubscription()`은 다음을 수행한다.

1. packageName이 `kr.pagero.calltag`인지 확인.
2. productId 허용 목록 확인.
3. Google Android Publisher API `purchases.subscriptionsv2.get`으로 실제 구매 조회.
4. 응답 line item의 productId가 앱 전달값과 일치하는지 확인.
5. 구독 상태/만료일 확인.
6. 다른 채널의 활성 구독이 있으면 `DUPLICATE_CHANNEL_SUBSCRIPTION`으로 차단.
7. purchase token 원문 대신 SHA-256 hash를 DB에 저장.
8. 서버 검증 완료 상태로 subscription upsert.
9. Google이 아직 acknowledgement를 요구하면 서버에서 acknowledge 호출.
10. 최종 entitlement 반환.

즉 **구매 승인(acknowledge)은 서버 쪽에서 이미 처리하도록 구현되어 있다.** 앱에 중복 acknowledge 로직을 추가하지 않는다.

### 주요 D1 테이블

- `billing_accounts`
- `billing_subscriptions`
- `referral_codes`
- `referrals`
- `partner_commissions`

구독 토큰은 DB에 원문 저장하지 않고 hash만 저장한다.

---

## 6. 중복결제 방지 원칙

이 부분은 반드시 유지한다.

### Android → Google Play 결제 전

1. `GET /api/billing/entitlements`
2. 서버 확인 성공 여부 검사.
3. `channel=web` + active이면 Play 결제창 열지 않음.
4. 기존 active Play 구독이 있으면 추가 결제창 열지 않음.
5. 서버 연결 실패 시 결제창 열지 않음.

### Web 결제 전

`POST /api/billing/web/precheck`

서버가 활성 Play/Web 구독을 확인한 후 checkout 생성 여부를 결정한다.

### 절대 금지

- 앱 로컬 상태만 보고 결제 시작.
- Play purchase callback만 보고 기능 즉시 개방.
- 웹 결제와 Play 결제를 각각 별도 권한으로 관리.
- 활성 구독이 있는데 새 checkout 생성.
- purchase token 원문 장기 저장.

---

## 7. 파트너 / 추천 수익

추천인과 파트너는 UI와 데이터 의미를 분리한다.

### 추천인

- 가입 시 선택 입력.
- 추천 관계 1회 생성.
- 무료 이용 보너스 처리.

### 파트너

- 자신의 추천코드 조회/복사/공유.
- 추천 회원 수.
- 유료 전환 회원 수.
- 예상 수익.
- 확정 수익.
- 정산 페이지 진입.

현재 서버에는 `partner_commissions` ledger와 `_commissions.js`가 존재한다. Google Play 검증 성공 시 `recordReferralCommission()`이 호출된다.

수익 계산은 앱에서 직접 하지 말고 서버 ledger 결과만 보여준다. 동일 결제에 대한 중복 적립은 `payment_reference` 고유값 기준으로 막는다.

---

## 8. 결제 공개 전에 반드시 끝낼 작업

다음 AI는 아래 순서로 진행한다.

### P0 — 상품 정책 정리

- 앱에 남아 있는 `call_monthly` 1,900원 / `message_monthly` 990원을 계속 판매할지 최종 정책과 대조한다.
- PageRo의 `무료 / 클래식 3,500 / 프로 5,500 / CallTag 통합 6,000` 표시와 충돌하지 않도록 서버 product catalog를 하나의 정본으로 만든다.
- 가격/상품명을 Activity 안에 문자열로 중복 하드코딩하지 않는다.

### P0 — 무료기간 상수 통합

- `_shared.js`의 3일/+5일 legacy 상수와 `trial-policy.js`의 7일/+7일 CallTag 상수를 정리한다.
- CallTag 실제 정책을 깨뜨리지 않도록 migration/기존 계정 trial expiry 영향 검토 후 수정한다.

### P0 — Play Console

- 최종 승인된 subscription product ID와 서버 product code를 1:1로 맞춘다.
- Base plan / 월간 자동갱신 설정.
- 한국 판매/가격 설정.
- 라이선스 테스터로 실제 purchase → verify → entitlement → restore 확인.
- Play Console에 한 번 사용한 `versionCode`는 재사용하지 않는다.

### P0 — Publisher API / 운영 환경

서버 readiness가 실제로 사용 가능해지려면 최소 다음 운영값이 필요하다.

- `GOOGLE_PLAY_BILLING_ENABLED=1`
- `GOOGLE_PLAY_PRODUCTS_READY=1`
- `GOOGLE_PLAY_CLIENT_EMAIL`
- `GOOGLE_PLAY_PRIVATE_KEY`

플래그는 상품/권한/테스트가 끝나기 전에 켜지 않는다.

### P1 — 실시간 상태 동기화

- RTDN(Real-time Developer Notifications) 또는 동등한 서버 동기화 구현.
- 취소, 환불, 만료, grace period, account hold, 갱신 실패 반영.
- 정기 reconciliation job 추가.
- 앱을 열지 않아도 서버 entitlement가 최신 상태를 유지해야 한다.

### P1 — offer 선택

현재 `PlayBillingManager.purchase()`는 `getSubscriptionOfferDetails().get(0)`을 사용한다.

Base plan/intro offer가 여러 개가 되면 첫 번째 offer를 무조건 사용하는 구조를 제거하고, 명시한 basePlanId/offerId/tag 기준으로 선택해야 한다.

### P1 — 테스트 케이스

반드시 실제 Play 테스트 계정으로 아래를 검증한다.

- 신규 결제 성공.
- 사용자 취소.
- pending purchase.
- 앱 종료 후 복원.
- 재설치 후 복원.
- 웹 구독 중 Play 결제 차단.
- Play 구독 중 웹 checkout 차단.
- 갱신.
- 취소 후 만료 전 접근.
- 만료 후 기능 제한.
- 환불.
- grace/account hold.
- 동일 purchase token 재전송의 idempotency.
- 추천 회원 첫 결제 commission 1회만 생성.

---

## 9. 데이터 보존 / 만료 정책

이용권이 만료되어도 아래 데이터는 삭제하지 않는다.

- 고객
- 상담 이력
- 메모
- 일정
- 문자 템플릿
- 발송 기록

권한이 없을 때 제한할 대상은 유료 실행 기능이다. 예: 신규 통화 후 정리 자동화, 문자 자동화 등.

사용자가 다시 구독하면 기존 데이터 위에서 기능이 재개되어야 한다.

---

## 10. 다음 AI가 먼저 읽을 파일

Android:

1. `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md` — 이 문서
2. `app/src/main/java/kr/pagero/calltag/BillingEntitlementActivity.java`
3. `app/src/main/java/kr/pagero/calltag/PlayBillingManager.java`
4. `app/src/main/java/kr/pagero/calltag/FeatureEntitlementStore.java`
5. `app/src/main/java/kr/pagero/calltag/AuthApiClient.java`
6. `app/build.gradle`

Server:

1. `functions/api/billing/entitlements.js`
2. `functions/api/billing/trial-policy.js`
3. `functions/api/billing/_shared.js`
4. `functions/api/billing/_readiness.js`
5. `functions/api/billing/google/verify.js`
6. `functions/api/billing/google/restore.js`
7. `functions/api/billing/_commissions.js`
8. `functions/api/billing/web/*`

---

## 11. 작업 금지선

- 결제 때문에 고객/통화/문자/일정 기존 데이터를 초기화하지 않는다.
- 결제 코드 수정과 unrelated UI 대규모 개편을 한 커밋에 섞지 않는다.
- 서버 entitlement 검증을 우회하지 않는다.
- Play Console 준비 전 release flag를 강제로 켜지 않는다.
- PageRo 단독 사용자에게 CallTag 결제를 강제하지 않는다.
- 추천인 코드 입력창을 가입 이후 설정에 다시 노출하지 않는다.
- 추천인과 파트너/정산 메뉴를 하나로 합치지 않는다.
- 과거 문서의 가격/무료기간 값을 현재 정책으로 자동 간주하지 않는다.

---

## 12. 완료 기준

결제 기능은 다음이 모두 통과해야 완료로 본다.

1. 최종 상품/가격 정본 1개.
2. Android Play 상품 조회 성공.
3. 실제 테스트 결제 성공.
4. 서버 Publisher API 검증 성공.
5. acknowledgement 성공.
6. 서버 entitlement 활성화.
7. 앱 기능 권한 반영.
8. 복원 성공.
9. 웹↔Play 중복결제 차단.
10. 취소/환불/만료/갱신 상태 동기화.
11. 추천 commission 중복 없이 생성.
12. 기존 사용자 데이터 무손실.

이 12개가 끝나기 전에는 `결제 완료`로 표기하지 않는다.
