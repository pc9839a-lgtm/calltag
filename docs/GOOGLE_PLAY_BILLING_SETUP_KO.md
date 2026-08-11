# CallTag Google Play 결제 실연동 가이드

기준일: **2026-08-11**  
앱: **CallTag**  
패키지: `kr.pagero.calltag`  
클라이언트 Billing Library: `com.android.billingclient:billing:9.1.0`

이 문서는 Google Play Console 로그인 이후 실제 구독 상품 생성 → 서버 검증 → 내부 테스트까지의 작업 순서를 고정한다.

---

## 0. 확정 상품

| 기능 | Play subscription product ID | 월 가격 |
|---|---|---:|
| 전화관리 | `call_monthly` | 1,900원 |
| 문자자동화 | `message_monthly` | 990원 |
| 통합권 | `all_monthly` | 6,000원 |

**주의**

- Product ID는 생성 후 변경/재사용이 어렵기 때문에 위 값을 그대로 사용한다.
- 페이지로 3,500원 단독권은 현재 CallTag Android Play 상품으로 만들지 않는다.
- 서버 무료체험 정책은 `7일 + 추천 7일`이며 무료체험 종료 후 자동결제하지 않는다.
- 따라서 현재는 Google Play subscription Offer로 별도 무료체험을 만들지 않는다.

---

## 1. Play Console 로그인 후 첫 확인

CallTag 앱 선택 후 아래를 먼저 확인한다.

1. **결제 프로필 / 판매자 계정**이 연결되어 있는지 확인
2. 앱 패키지가 `kr.pagero.calltag`인지 확인
3. 내부 테스트 트랙에 최신 AAB가 배포되어 있는지 확인
4. 결제 테스트용 Google 계정을 정한다

판매자 계정이 없으면 Play Console에서 Google Payments profile을 먼저 연결한다.

### 1.1 `앱 초안` 상태에서 `제품/정기 결제` 메뉴가 보이지 않는 경우

CallTag처럼 새 앱이 아직 `앱 초안`이고 수익화 하위 메뉴가 노출되지 않으면 **구독 상품부터 만들려고 하지 않는다.** 먼저 Google Play Billing Library가 포함된 유효한 AAB를 **내부 테스트 트랙에 게시**한다.

현재 CallTag AAB에는 Billing Library가 포함되어 있으므로 아래 순서로 진행한다.

1. `테스트 및 출시 → 테스트 → 내부 테스트`
2. 내부 테스트 트랙 생성 또는 기존 트랙 선택
3. 테스터 Gmail 등록
4. 최신 signed AAB 업로드
5. 버전 저장 후 내부 테스트에 게시
6. 첫 내부 테스트 게시가 처리된 뒤 `Play를 통한 수익 창출 → 제품 → 정기 결제` 메뉴 재확인

Google Play 공식 기준상 내부 테스트는 앱 설정을 완료하기 전에도 사용할 수 있고, 유효한 App Bundle만 있으면 시작할 수 있다. 또한 Play 내부 테스트 트랙은 Subscriptions / In-app purchases 같은 Play 서비스 검증에 사용하는 권장 경로다.

`앱 콘텐츠`, `스토어 등록정보`, 비공개 테스트용 초기 설정을 전부 끝내야 내부 테스트를 시작하는 것은 아니다. **비공개 테스트와 프로덕션 준비 조건을 내부 테스트 조건과 혼동하지 않는다.**

---

## 2. 구독 상품 3개 생성

Play Console:

`Play를 통한 수익 창출(Monetize with Play) → 제품(Products) → 정기 결제(Subscriptions)`

### 2.1 전화관리

- Product ID: `call_monthly`
- 이름: `콜태그 전화관리`
- 혜택 예시:
  - 통화 후 고객관리
  - 고객 상태·메모·일정
  - 수신 고객 표시
  - 통화 통계

### 2.2 문자자동화

- Product ID: `message_monthly`
- 이름: `콜태그 문자자동화`
- 혜택 예시:
  - 통화 후 자동문자
  - 문자 템플릿
  - 그룹·단체문자
  - 발송 관리

### 2.3 통합권

- Product ID: `all_monthly`
- 이름: `콜태그 통합권`
- 혜택 예시:
  - 전화관리 전체
  - 문자자동화 전체
  - 페이지로 연동 기능

각 상품은 생성 후 subscription details를 저장한다.

---

## 3. 각 상품에 monthly Base Plan 생성

각 subscription 상세에서:

`Add base plan`

공통값:

- Base plan ID: `monthly`
- Type: **Auto-renewing**
- Billing period: **Monthly**
- 국가: 우선 **대한민국**
- 가격:
  - call_monthly → **₩1,900**
  - message_monthly → **₩990**
  - all_monthly → **₩6,000**
- Resubscribe: 사용 권장
- Grace period: 운영정책 확정 후 설정
- Account hold: 활성 유지 권장

저장 후 **Activate**까지 해야 앱에서 ProductDetails 조회가 가능하다.

### 현재 Offer 정책

- 별도 무료체험 Offer 생성하지 않음
- 할인 Offer도 1차 결제 연동 완료 전에는 만들지 않음

이유: CallTag 무료체험은 결제수단 없이 서버 entitlement로 먼저 제공하며 종료 후 자동결제하지 않는 정책이다.

---

## 4. 라이선스 테스터 등록

Play Console:

`설정(Settings) → 라이선스 테스트(License testing)`

- 실제 테스트할 Gmail 계정 등록
- 같은 계정을 내부 테스트 테스터에도 등록
- 테스트 기기 Play Store에도 해당 Google 계정을 로그인
- 가능하면 해당 계정으로 Play Store에서 내부 테스트 버전을 직접 설치

중요:

- 기기에 Google 계정이 여러 개면 **앱을 다운로드한 계정** 기준으로 결제가 진행될 수 있다.
- subscription/base plan이 활성화되어 있어야 테스트 결제가 가능하다.

---

## 5. 현재 앱 클라이언트 구현 상태

이미 구현됨:

- BillingClient 연결
- SUBS 상품 3개 조회
- `ProductDetails` 로드
- 구매창 실행
- `obfuscatedAccountId` 사용
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

## 6. Google Play Developer API 서비스 계정

서버가 purchaseToken을 직접 검증해야 한다.

### 6.1 Google Cloud

1. Google Cloud 프로젝트 준비
2. **Google Play Android Developer API** 활성화
3. Service Account 생성
4. 서비스 계정 키/인증정보는 서버 Secret으로만 보관

### 6.2 Play Console 권한

Play Console:

`Users and permissions → Invite new users`

서비스 계정 이메일을 사용자로 추가하고 최소 다음 권한을 부여한다.

- View financial data, orders, and cancellation survey responses
- Manage orders and subscriptions

서비스 계정 키를 Android APK/AAB에 포함하면 안 된다.

---

## 7. 서버 구매 검증 정본

클라이언트에서 받은 값:

- packageName: `kr.pagero.calltag`
- productId
- purchaseToken
- orderId (참고값)

서버는 **purchaseToken을 정본 키**로 사용한다.

### 신규 구매 검증

1. 로그인 세션 확인
2. productId 허용목록 확인
3. Google Play Developer API의 subscription 상태 조회
4. packageName / 상품 / 상태 확인
5. PURCHASED 상태인지 확인
6. purchaseToken 중복 사용 여부 확인
7. 사용자 entitlement 저장
8. acknowledgement 미완료면 서버에서 acknowledge
9. 최신 entitlement JSON 반환

**orderId를 유일키로 사용하지 않는다.**

### acknowledgement

구독 entitlement 부여 후 acknowledgement를 즉시 처리한다.

acknowledge되지 않은 테스트 구매는 빠르게 환불될 수 있고 실제 구매도 정해진 시간 내 acknowledgement가 없으면 환불될 수 있으므로 서버 처리로 고정한다.

---

## 8. entitlement DB 권장 필드

최소:

```text
user_id
channel = google_play
product_id
purchase_token UNIQUE
subscription_state
active
started_at
expires_at
next_billing_at
auto_renew_enabled
acknowledged
order_id nullable
last_verified_at
raw_google_status
```

권한 매핑:

```text
call_monthly    -> CALL
message_monthly -> MESSAGE
all_monthly     -> CALL + MESSAGE + PAGERO
```

웹 구독 active 상태라면 Play 신규 구매를 막는다.
Play 구독 active 상태라면 웹 중복 구매도 막는다.

---

## 9. RTDN(실시간 개발자 알림)

Google Cloud Pub/Sub topic을 만들고 Google Play RTDN을 연결한다.

RTDN 수신 후 알림 body만 믿지 않고 purchaseToken으로 Google Play Developer API를 다시 조회한다.

최소 처리 이벤트:

- PURCHASED
- RENEWED
- CANCELED
- EXPIRED
- IN_GRACE_PERIOD
- ON_HOLD
- RECOVERED
- REVOKED

정책:

- GRACE_PERIOD: 서비스 유지 가능
- ON_HOLD: entitlement 차단
- EXPIRED / REVOKED: entitlement 종료
- RECOVERED / RENEWED: entitlement 재활성/연장

---

## 10. 앱 서버 billingAvailability 플래그

현재 Android 앱은 서버가 Google Play 결제를 활성 상태로 내려주기 전에는 BillingClient 구매를 열지 않는다.

운영 전 서버 응답 예시:

```json
{
  "billingAvailability": {
    "googlePlay": {
      "available": false,
      "stage": "pre_registration",
      "message": "Google Play 결제 준비 중"
    }
  }
}
```

Play 상품 + 서버 검증 + RTDN + 내부테스트가 완료된 뒤:

```json
{
  "billingAvailability": {
    "googlePlay": {
      "available": true,
      "stage": "active",
      "message": "Google Play 결제 사용 가능"
    }
  }
}
```

**준비 전 available=true 전환 금지.**

---

## 11. 내부 테스트 결제 QA

테스터 기기에서:

1. Play Store 내부 테스트 링크로 CallTag 설치
2. CallTag 로그인
3. `더보기 → 이용권·결제`
4. 서버 entitlement 정상 조회 확인
5. `Google Play 결제 사용 가능` 확인
6. 전화관리 결제 테스트
7. 테스트 결제창에 `Test` 표시 확인
8. 결제 후 즉시 CALL 권한 반영 확인
9. 앱 삭제/재설치
10. `Google Play 구매 복원` 확인
11. 구독 취소 후 entitlement 상태 확인
12. 갱신/결제실패/grace/account hold 시나리오 확인

반드시 3상품 각각 최소 1회 테스트한다.

---

## 12. 출시 전 통과 조건

- [ ] 결제 프로필 연결
- [ ] 내부 테스트에 Billing Library 포함 최신 AAB 게시
- [ ] `Play를 통한 수익 창출 → 제품 → 정기 결제` 메뉴 노출 확인
- [ ] `call_monthly` 생성 + monthly 활성
- [ ] `message_monthly` 생성 + monthly 활성
- [ ] `all_monthly` 생성 + monthly 활성
- [ ] 대한민국 가격 정확
- [ ] 라이선스 테스터 등록
- [ ] 서비스 계정 생성
- [ ] Google Play Developer API 권한 부여
- [ ] 서버 subscription 검증
- [ ] 서버 acknowledgement
- [ ] purchaseToken UNIQUE
- [ ] RTDN Pub/Sub
- [ ] 갱신 반영
- [ ] 취소 반영
- [ ] grace/account hold 반영
- [ ] 복원 성공
- [ ] 웹/Play 중복 결제 차단
- [ ] 앱에서 외부 웹 결제 CTA 없음
- [ ] 내부 테스트 실제 결제 성공
- [ ] `billingAvailability.googlePlay.available=true` 운영 전환

이 체크리스트 전부 통과 후 실제 결제 기능 완료로 판정한다.
