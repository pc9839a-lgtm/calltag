# CallTag Google Play 결제 실연동 가이드

기준일: **2026-08-11**  
앱: **CallTag**  
패키지: `kr.pagero.calltag`  
현재 내부 테스트 릴리스: **0.44.14 / versionCode 2026081101**  
클라이언트 Billing Library: `com.android.billingclient:billing:9.1.0`

## 현재 상태 — 이미 완료

- 결제 프로필 생성 완료
- 내부 테스트 트랙 생성 완료
- `0.44.14 / 2026081101` 내부 테스트 게시 완료
- Play Console 화면에서 `내부 테스터에게 제공됨` 상태 확인 완료
- 같은 `versionCode 2026081101` AAB 재업로드 금지
- 앱에 Google Play Billing Library 및 `com.android.vending.BILLING` 포함
- 앱 클라이언트에 상품 조회/구매/복원/서버 검증 호출 구현됨

**중요:** 현재 `제품 → 정기 결제` 메뉴가 안 보이는 문제를 AAB 재업로드 문제로 판단하지 않는다. 최신 AAB는 이미 유효하게 내부 테스트에 게시되어 있다.

---

## 1. 확정 Play 구독 상품

| 기능 | Product ID | 월 가격 |
|---|---|---:|
| 전화관리 | `call_monthly` | 1,900원 |
| 문자자동화 | `message_monthly` | 990원 |
| 통합권 | `all_monthly` | 6,000원 |

페이지로 3,500원 단독권은 현재 CallTag Android Play 상품으로 만들지 않는다.

무료체험은 Play Offer가 아니라 CallTag 서버 entitlement로 처리한다.

- 일반 가입: 7일
- 추천코드 가입: +7일
- 종료 후 자동결제 없음

---

## 2. 현재 최우선 — `정기 결제` 메뉴 미노출 원인 확인

공식 Play Console 경로:

`Play를 통한 수익 창출(Monetize with Play) → 제품(Products) → 정기 결제(Subscriptions)`

현재 최신 내부 테스트 릴리스가 이미 게시됐으므로 아래만 확인한다.

### 2.1 사용자 권한 확인

Play Console → `사용자 및 권한`

현재 로그인 계정이 **계정 소유자(Account owner)**라면 권한 문제는 아님.

소유자가 아니라면 최소한 해당 앱에 **스토어 등록정보 관리(Manage store presence)** 권한이 있어야 한다. Google 공식 권한 정의에서 이 권한에는 앱 가격 편집과 인앱 제품 관리가 포함된다.

`주문 및 정기 결제 관리` 권한은 이미 생성된 주문 조회/환불/구독 취소용이며, 상품 카탈로그 생성 권한과 혼동하지 않는다.

### 2.2 결제 프로필 상태 확인

결제 프로필이 존재하는지만이 아니라 Play 개발자 계정에 연결된 merchant/payments profile인지 확인한다.

현재 확인된 상태:

- 결제 프로필 화면 접근 가능
- KRW 수익 화면 노출
- 지급수단 설정 가능

따라서 프로필 자체는 생성되어 있다.

### 2.3 메뉴 위치 확인

왼쪽 사이드바의 `Play를 통한 수익 창출` 섹션을 열고 그 하위의 `제품`을 확인한다.

Play Academy의 `시작하기` 버튼은 결제 설정 버튼이 아니다. 교육 페이지는 결제 활성화와 무관하므로 사용하지 않는다.

### 2.4 여기까지 정상인데 메뉴가 없으면

AAB를 다시 올리지 않는다.

다음 순서로 진단한다.

1. 현재 로그인 계정의 `사용자 및 권한` 화면 확인
2. 계정 소유자 여부 확인
3. `Manage store presence` 권한 확인
4. `Play를 통한 수익 창출` 섹션 전체 메뉴 캡처 확인
5. 필요하면 Play Console 지원 문의

---

## 3. 정기 결제 메뉴가 보인 뒤 생성할 값

### 전화관리

- Product ID: `call_monthly`
- 이름: `콜태그 전화관리`
- Base plan ID: `monthly`
- 유형: Auto-renewing
- 기간: 1개월
- 대한민국 가격: ₩1,900

### 문자자동화

- Product ID: `message_monthly`
- 이름: `콜태그 문자자동화`
- Base plan ID: `monthly`
- 유형: Auto-renewing
- 기간: 1개월
- 대한민국 가격: ₩990

### 통합권

- Product ID: `all_monthly`
- 이름: `콜태그 통합권`
- Base plan ID: `monthly`
- 유형: Auto-renewing
- 기간: 1개월
- 대한민국 가격: ₩6,000

각 Base plan은 저장 후 **Activate**까지 완료한다.

별도 무료체험 Offer는 만들지 않는다.

---

## 4. 현재 앱 클라이언트 구현 상태

이미 구현됨:

- BillingClient 연결
- SUBS 상품 3개 조회
- `ProductDetails` 로드
- 구매창 실행
- `obfuscatedAccountId`
- PENDING 구매 entitlement 미부여
- PURCHASED 구매 서버 검증 요청
- 구매 복원
- Google Play 구독 관리 링크
- 웹 구독/기존 구독 중복결제 사전 차단

클라이언트 Product ID:

```text
call_monthly
message_monthly
all_monthly
```

서버 호출:

```text
GET  /api/billing/entitlements
POST /api/billing/google/verify
POST /api/billing/google/restore
```

---

## 5. Play 상품 생성 후 서버 작업

Google Cloud:

1. Google Play Android Developer API 활성화
2. Service Account 생성
3. Play Console에서 서비스 계정 권한 부여
4. 서버 Secret에 서비스 계정 인증정보 보관

서버 구매 검증:

1. 로그인 세션 확인
2. productId 허용목록 검사
3. purchaseToken으로 Google Play Developer API 조회
4. packageName / 상품 / 구독상태 검증
5. purchaseToken 중복 사용 방지
6. entitlement 저장
7. acknowledgement 처리
8. 최신 entitlement 반환

RTDN + Pub/Sub도 연결해 갱신/취소/만료/grace/account hold 상태를 서버에서 동기화한다.

---

## 6. 내부 테스트 결제 QA

1. 라이선스 테스터 Gmail 등록
2. 같은 계정을 내부 테스트 테스터로 등록
3. Play Store 내부 테스트 링크로 CallTag 설치
4. CallTag 로그인
5. `더보기 → 이용권·결제`
6. Google Play 상품 3개 로드 확인
7. 테스트 결제 진행
8. 서버 entitlement 즉시 반영 확인
9. 앱 재설치 후 구매 복원 확인
10. 취소/갱신/결제실패/만료 상태 확인

---

## 7. 현재 체크리스트

- [x] 결제 프로필 생성
- [x] 내부 테스트 트랙 생성
- [x] `0.44.14 / 2026081101` 내부 테스트 게시
- [x] Billing Library 포함
- [ ] 현재 로그인 계정의 Play Console 권한 확인
- [ ] `Play를 통한 수익 창출 → 제품 → 정기 결제` 메뉴 노출
- [ ] `call_monthly` 생성 + monthly 활성
- [ ] `message_monthly` 생성 + monthly 활성
- [ ] `all_monthly` 생성 + monthly 활성
- [ ] 라이선스 테스터 등록
- [ ] Google Play Developer API 서비스 계정
- [ ] 서버 purchaseToken 검증
- [ ] acknowledgement
- [ ] RTDN
- [ ] 실제 테스트 결제 성공

**현재 다음 액션은 AAB 업로드가 아니라 Play Console 사용자 권한/수익화 메뉴 노출 원인 확인이다.**
