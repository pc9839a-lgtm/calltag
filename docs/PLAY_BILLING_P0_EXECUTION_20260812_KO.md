# CallTag Google Play 결제 P0 실행 정본 — 2026-08-12

## 0. 목적

이 문서는 `docs/NEXT_AI_HANDOFF_20260812_KO.md`의 Google Play 결제 P0를 실제로 실행하기 위한 최신 절차다.

기존 인수인계 문서의 `Play Console ↔ Google Cloud 프로젝트 연결` 표현은 현재 Google Play Developer API 절차와 맞지 않는다.

**현재 절차에서는 Play Console 개발자 계정을 Google Cloud 프로젝트에 별도로 연결할 필요가 없다.**

실행 순서는 아래와 같다.

1. Google Cloud 프로젝트 준비
2. Google Play Android Developer API 활성화
3. 서버용 service account 생성
4. Play Console `사용자 및 권한`에서 service account 이메일 초대
5. 결제/구독 관리에 필요한 최소 권한 부여
6. 서버 secret 등록
7. Play Console subscription/base plan 준비
8. readiness 플래그 활성화
9. 라이선스 테스터 실제 결제 E2E
10. E2E 성공 후 RTDN/Pub/Sub 구성

---

## 1. 현재 코드 기준

Android 저장소: `pc9839a-lgtm/calltag`

- branch: `agent/calltag-auth-ux-google-upgrade-fix`
- package: `kr.pagero.calltag`
- versionName: `0.44.23`
- versionCode: `2026081209`
- Billing Library: `9.1.0`

서버 저장소: `pc9839a-lgtm/inlet`

현재 Android productId:

- `all_monthly`
- `call_monthly`
- `message_monthly`

현재 서버 검증:

- `POST /api/billing/google/verify`
- `POST /api/billing/google/restore`
- Android Publisher API `purchases.subscriptionsv2.get`
- 서버 acknowledgement
- 서버 entitlement 반영
- Web ↔ Play 중복결제 차단

결제 구조를 새로 만들지 않는다.

---

## 2. Android P0 안전패치

기존 `PlayBillingManager.purchase()`는 `subscriptionOfferDetails.get(0)`을 바로 사용했다.

이 방식은 Play Console에 base plan 또는 offer가 여러 개 있으면 잘못된 offerToken을 선택할 수 있다.

0.44.23에서는 다음 원칙으로 변경한다.

- purchasable offer가 1개뿐이면 사용
- 여러 offer가 있더라도 `offerId`가 없는 기본 base plan이 정확히 1개면 그것만 사용
- 기본 base plan 후보가 여러 개면 결제창을 열지 않음
- Play Console 구성이 확정되기 전 임의 offer 선택 금지

향후 할인/무료체험 offer를 실제 도입하면 `basePlanId` / `offerId` / offer tag를 코드 또는 서버 카탈로그에서 명시적으로 매핑한다.

---

# 3. P0-1 Google Cloud

## 3-1. Cloud 프로젝트 선택

CallTag 결제 서버가 사용할 Google Cloud 프로젝트를 하나 고정한다.

Google Cloud Console에서:

`API 및 서비스 → 라이브러리`

검색:

`Google Play Android Developer API`

해당 API를 활성화한다.

## 3-2. service account 생성

Google Cloud Console에서:

`IAM 및 관리자 → 서비스 계정 → 서비스 계정 만들기`

권장 이름 예시:

`calltag-play-billing`

생성 후 service account 이메일을 기록한다.

예시 형식:

`calltag-play-billing@PROJECT_ID.iam.gserviceaccount.com`

### 키

서버가 현재 JWT service-account 방식으로 인증하므로 JSON 키가 필요하다.

`서비스 계정 → 키 → 키 추가 → 새 키 만들기 → JSON`

JSON 전체를 저장소에 올리지 않는다.

서버에 필요한 값은 JSON의 다음 두 필드다.

- `client_email` → `GOOGLE_PLAY_CLIENT_EMAIL`
- `private_key` → `GOOGLE_PLAY_PRIVATE_KEY`

비공개 키는 GitHub, 문서, 로그, 이슈, PR 본문에 넣지 않는다.

---

# 4. P0-2 Play Console 권한

Play Console 개발자 계정 수준에서:

`사용자 및 권한 → 새 사용자 초대`

이메일에는 방금 만든 service account 이메일을 입력한다.

CallTag 앱에 필요한 결제 API 권한은 최소 다음 두 항목이다.

- 재무 데이터, 주문 및 취소 설문 응답 보기
- 주문 및 정기 결제 관리

가능하면 `kr.pagero.calltag` 앱에만 권한 범위를 제한한다.

전체 개발자 계정의 불필요한 앱/관리 권한은 주지 않는다.

초대 완료 후 service account가 Play Console 사용자 목록에 표시되는지 확인한다.

---

# 5. P0-3 서버 secret

현재 `inlet` readiness가 확인하는 값:

```text
GOOGLE_PLAY_BILLING_ENABLED
GOOGLE_PLAY_PRODUCTS_READY
GOOGLE_PLAY_CLIENT_EMAIL
GOOGLE_PLAY_PRIVATE_KEY
```

초기 연결 중에는 다음처럼 유지한다.

```text
GOOGLE_PLAY_BILLING_ENABLED=0
GOOGLE_PLAY_PRODUCTS_READY=0
```

service account를 등록한다.

```text
GOOGLE_PLAY_CLIENT_EMAIL=<service account client_email>
GOOGLE_PLAY_PRIVATE_KEY=<service account private_key>
```

`GOOGLE_PLAY_PRIVATE_KEY`는 실제 줄바꿈 또는 `\n` 이스케이프 문자열 모두 현재 서버 파서가 처리한다.

**주의:** readiness는 credential 문자열의 존재 여부만 확인한다. 실제 권한이 맞는지는 테스트 purchaseToken을 Publisher API로 조회할 때 최종 검증된다.

---

# 6. P0-4 Play Console subscription 상품

Play Console에서:

`CallTag → 수익 창출 → 제품 → 정기 결제`

앱 코드와 정확히 일치해야 하는 productId:

- `all_monthly`
- `call_monthly`
- `message_monthly`

각 상품에서 확인:

- productId 오탈자 없음
- base plan 활성
- 월간 자동 갱신 여부
- 한국 판매 가능
- 실제 판매 가격
- 테스트 트랙 설치본에서 ProductDetails 조회 가능

## 첫 E2E 권장 구성

첫 실결제 E2E 전에는 상품당 구매 가능한 기본 base plan을 **1개로 단순화**한다.

할인 offer, 무료체험 offer, 복수 base plan을 먼저 추가하지 않는다.

0.44.23 앱은 구성이 모호하면 임의 선택 대신 결제를 중단한다.

상품 구성이 모두 끝나고 앱에서 ProductDetails 조회가 확인된 뒤:

```text
GOOGLE_PLAY_PRODUCTS_READY=1
```

service account/API 검증 준비까지 완료된 뒤 최종적으로:

```text
GOOGLE_PLAY_BILLING_ENABLED=1
```

두 플래그를 먼저 켜서 운영 사용자에게 미완성 결제창을 노출하지 않는다.

---

# 7. P0-5 라이선스 테스터 실제 결제 E2E

Play Console에서 라이선스 테스터 계정을 등록하고 내부 테스트 또는 비공개 테스트 트랙의 Play 설치본을 사용한다.

APK 사이드로드만으로 최종 결제 E2E 완료 판정을 하지 않는다.

검증 순서:

1. Play 스토어 테스트 트랙에서 `0.44.23 / 2026081209` 설치
2. CallTag 로그인
3. `더보기 → 이용권`
4. 서버 readiness `googlePlay.available=true`
5. `all_monthly`, `call_monthly`, `message_monthly` ProductDetails 조회
6. 대상 상품 결제 버튼
7. Google Play 테스트 결제창 표시
8. 테스트 결제 완료
9. 앱이 purchaseToken 수신
10. `/api/billing/google/verify` 호출
11. 서버가 `purchases.subscriptionsv2.get`으로 Google 검증
12. packageName/productId/상태/만료 확인
13. acknowledgement pending이면 서버에서 acknowledge
14. `billing_subscriptions` upsert
15. entitlement `active`
16. 이용권 화면 즉시 반영

### 반드시 이어서 확인

- 앱 완전 종료 후 재실행 → entitlement 유지
- 로그아웃/로그인 → entitlement 유지
- 앱 재설치 → 구매 복원 성공
- Google Play 구독 관리 화면 이동
- Web 활성 구독 계정 → Play 결제 차단
- Play 활성 구독 계정 → Web checkout 차단
- pending 테스트가 가능하면 pending 상태에서 entitlement를 미리 열지 않음

1~16과 후속 검증이 통과하기 전에는 `Google Play 결제 E2E 완료`라고 기록하지 않는다.

---

# 8. 실패 시 분기

## ProductDetails가 0개

우선 확인:

- 설치본이 Play 테스트 트랙에서 설치됐는지
- 설치한 Google 계정이 테스터인지
- productId 3개가 정확한지
- subscription/base plan이 활성인지
- 판매 국가/가격이 활성인지
- 앱 package가 `kr.pagero.calltag`인지

## Play 결제창 전 단계에서 서버 준비중 메시지

`GET /api/billing/readiness` 확인.

- `PLAY_RELEASE_DISABLED` → `GOOGLE_PLAY_BILLING_ENABLED`
- `PLAY_PRODUCTS_NOT_READY` → `GOOGLE_PLAY_PRODUCTS_READY`
- `PLAY_VERIFICATION_NOT_CONFIGURED` → client email/private key

## verify가 401/403 계열 Google 오류

- Android Publisher API 활성화
- service account Play Console 초대 여부
- 재무 데이터 보기 권한
- 주문 및 정기 결제 관리 권한
- CallTag 앱 권한 범위

## verify 성공, entitlement 미반영

- `/api/billing/google/verify` 응답
- `billing_subscriptions` upsert
- `FeatureEntitlementStore.saveServerEntitlement()`
- Activity `onServerVerified()` 재조회 흐름

구매 callback만 보고 로컬에서 강제로 active 처리하지 않는다.

---

# 9. E2E 다음 작업 — RTDN

실제 purchase/verify/acknowledge/restore가 성공한 다음 진행한다.

Google Cloud:

1. Pub/Sub API 활성화
2. topic 생성
3. `google-play-developer-notifications@system.gserviceaccount.com`에 해당 topic의 Pub/Sub Publisher 권한 부여

Play Console:

`CallTag → 수익 창출 → 수익 창출 설정 → 실시간 개발자 알림`

- topic: `projects/PROJECT_ID/topics/TOPIC_NAME`
- 테스트 메시지 전송 성공 확인
- 알림 범위: 정기 결제 + 무효화된 구매

서버:

- RTDN 수신 endpoint/subscriber
- notification의 purchaseToken/product 정보로 Publisher API 재조회
- DB entitlement 갱신
- 갱신/취소/만료/결제 실패/grace/account hold/재개/환불 처리

RTDN이 완성되기 전 정기 결제 `일시중지` 기능을 활성화하지 않는다.

---

# 10. 보안 금지선

- service account JSON 저장소 커밋 금지
- private key GitHub Secret 외 평문 저장 금지
- purchaseToken 원문 DB 장기 저장 금지
- 앱에서 Publisher API 직접 호출 금지
- 앱에서 별도 acknowledge 중복 구현 금지
- 서버 Publisher API 검증 우회 금지
- Web ↔ Play 중복결제 차단 제거 금지
- 테스트 성공 전 release/product-ready 플래그 선활성화 금지

키가 채팅/로그/커밋/스크린샷으로 노출되면 즉시 해당 키를 폐기하고 새 키로 교체한다.

---

# 11. P0 완료 정의

아래가 모두 확인돼야 P0 완료다.

- [ ] Google Play Android Developer API 활성화
- [ ] service account 생성
- [ ] Play Console 사용자 초대
- [ ] 재무 데이터 보기 권한
- [ ] 주문 및 정기 결제 관리 권한
- [ ] 서버 client email/private key 등록
- [ ] 3개 productId 대조
- [ ] base plan/가격/한국 판매 활성
- [ ] ProductDetails 조회
- [ ] 라이선스 테스터 Play 결제창
- [ ] purchaseToken 서버 verify
- [ ] Publisher API 검증
- [ ] 서버 acknowledgement
- [ ] entitlement active
- [ ] 앱 재시작 유지
- [ ] 앱 재설치 구매 복원
- [ ] Web → Play 중복결제 차단
- [ ] Play → Web 중복결제 차단

이후 바로 RTDN/Pub/Sub를 P1로 진행한다.
