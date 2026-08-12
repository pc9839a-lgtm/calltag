# CallTag 다음 AI 인수인계 정본 — 2026-08-12

## 0. 가장 먼저 읽을 것

다음 AI는 이 문서를 먼저 읽고 작업을 시작한다.

1. `docs/NEXT_AI_HANDOFF_20260812_KO.md` — **현재 최우선 정본**
2. `docs/CURRENT_RELEASE_STATUS_20260812_KO.md` — 최신 릴리스/QA 상태
3. `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md` — 결제 상세 구조
4. 실제 Android/Server 코드

현재 다음 패치의 핵심은 **Google Play 정기결제를 실제 Play Console/Google Cloud와 연결하고 테스트 결제 E2E를 끝내는 것**이다.

---

## 1. 현재 Android 기준

- 저장소: `pc9839a-lgtm/calltag`
- 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`
- 패키지: `kr.pagero.calltag`
- versionName: **0.44.22**
- versionCode: **2026081208**
- target/compile SDK: 36
- Play 업로드키 signed AAB 빌드 성공
- Workflow run: `31557329238`
- Artifact: `9126476904`
- 다음 Play versionCode는 **2026081209 이상** 사용

Play Console에 한 번 업로드한 versionCode는 재사용하지 않는다.

---

## 2. 방금 끝난 0.44.22 패치

### Google 로그인

현재 구조:

`LoginActivity` → `GoogleCredentialLoginActivity` 내부 직접 호출 → Credential Manager → Google ID Token → `/api/call/google/id-token` → CallTag session.

최신 수정:

- `GetGoogleIdOption` 사용
- `setFilterByAuthorizedAccounts(false)`
- `setAutoSelectEnabled(false)`
- Web/server client ID를 `serverClientId`로 사용
- Credential Manager callback은 main executor 사용
- provider timeout 30초
- ID token 서버 exchange timeout 20초
- `GoogleCredentialLoginActivity`는 `exported=false`
- 불필요한 `calltag://credential/google` 외부 딥링크 제거

**주의:** 0.44.22에서 실제 계정 선택 → 세션 생성 완료까지 단말 E2E는 아직 확인 전이다. 실패하면 화면/로그 기준으로 진단한다. 다시 브라우저 OAuth로 되돌리지 않는다.

### 더보기

상위 메뉴 8개:

1. 계정
2. 이용권
3. 문자 관리
4. 고객 관리
5. 페이지로
6. 파트너
7. 데이터 관리
8. 앱 정보

4개 그룹:

- 내 정보: 계정 / 이용권
- 업무 관리: 문자 관리 / 고객 관리
- 서비스: 페이지로 / 파트너
- 앱 관리: 데이터 관리 / 앱 정보

0.44.22에서 메뉴는 각각 독립 카드로 변경했다.

- 메뉴 높이 64dp
- 메뉴 사이 12dp
- 섹션 사이 34dp
- 통화 후 자동문자 별도 대형 카드 금지. `문자 관리` 안에 둔다.

### 아이콘

0.44.21의 깨진 WebP launcher foreground 방식은 폐기했다.

0.44.22:

- vector foreground 사용
- Adaptive Icon 사용
- release manifest에서 `@mipmap/ic_launcher_calltag` / round icon 고정
- `calltag_launcher_safe.webp`가 release AAB에 들어가면 CI 실패

실제 삼성/Pixel/Google 계정 선택창에서 아이콘이 정상인지 단말 확인 필요.

---

## 3. 고객센터 확정 구조

`더보기 → 앱 정보 → 고객센터`

앱 내 문의 폼 → `POST /api/call/support` → 서버 → AWS SES → **roadfor@kakao.com**.

고객 이메일은 `Reply-To`로 사용한다.

- 서버 배포 완료
- 인증 없는 요청 401 smoke 성공
- 실제 로그인 사용자 문의 → roadfor@kakao.com 수신 E2E는 아직 확인 필요

---

# 4. 가장 중요: Google Play 결제는 이미 코드에 붙어 있다

**다음 AI는 결제를 새로 만드는 작업부터 시작하면 안 된다.** 이미 Android와 서버 모두 상당 부분 구현되어 있다.

Android 의존성:

`com.android.billingclient:billing:9.1.0`

핵심 Android 파일:

- `app/src/main/java/kr/pagero/calltag/BillingEntitlementActivity.java`
- `app/src/main/java/kr/pagero/calltag/PlayBillingManager.java`
- `app/src/main/java/kr/pagero/calltag/FeatureEntitlementStore.java`
- `app/src/main/java/kr/pagero/calltag/AuthApiClient.java`

현재 앱에 구현된 것:

- BillingClient 연결
- `SUBS` ProductDetails 조회
- Google Play 구매 플로우
- pending purchase 처리
- `obfuscatedAccountId`
- 구매 결과 수신
- 서버 purchaseToken 검증
- 구매 복원
- Google Play 구독 관리 화면
- 서버 entitlement 기반 기능 활성화
- 결제 직전 entitlement 재조회
- 활성 Web 구독 중 Play 중복결제 차단
- 기존 Play 구독 중 추가결제 차단
- 서버 상태를 확인할 수 없으면 결제 시작 금지

현재 Android productId:

- `all_monthly` — 통합권 — 코드상 6,000원
- `call_monthly` — 전화관리 — 코드상 1,900원
- `message_monthly` — 문자자동화 — 코드상 990원

**가격/상품 구성은 Play 공개 직전 실제 Play Console 상품과 최종 대조한다. 앱 하드코딩값만 보고 Play 상품을 임의 생성하지 않는다.**

---

## 5. 서버 결제도 이미 구현되어 있다

서버 저장소: `pc9839a-lgtm/inlet`, branch `main`.

핵심:

- `/api/billing/entitlements`
- `/api/billing/subscriptions`
- `/api/billing/readiness`
- `/api/billing/google/verify`
- `/api/billing/google/restore`
- `/api/billing/web/precheck`

Google purchase verify는 서버에서 Android Publisher API를 호출하도록 구현되어 있다.

서버가 수행하는 핵심:

- packageName 검증 (`kr.pagero.calltag`)
- productId 검증
- purchase token을 Google Publisher API로 검증
- 구독 상태/만료 확인
- Web ↔ Google Play 중복구독 차단
- purchase token 원문 대신 hash 저장
- subscription DB upsert
- acknowledgement 필요 시 **서버에서 acknowledge**
- entitlement 반환
- 추천 관계가 있으면 partner commission ledger 기록

앱에 별도 acknowledge 로직을 중복 추가하지 않는다.

---

# 6. 다음 패치 P0 — Play Console/Google Cloud 실제 연결

사용자가 **다음 패치에서 Play Console의 결제/API 연결 작업을 진행하기로 확정**했다.

현재 이것은 **예정 상태**이며 완료로 기록하지 않는다.

다음 AI가 해야 할 순서:

### P0-1. Play Console ↔ Google Cloud/API 액세스 연결

- CallTag가 사용하는 Google Cloud 프로젝트를 Play Console API access에 연결
- Google Play Developer API(Android Publisher API) 사용 가능 상태 확인
- 서버용 service account를 Play Console에 연결/권한 부여
- 최소 권한 원칙 적용
- 결제/구독 조회 및 필요한 acknowledgement에 필요한 권한 확인

사용자가 화면에서 직접 연결해야 하는 단계가 있으면 **현재 Play Console 화면 기준으로 정확히 클릭 위치를 안내**하고, 연결 후 서버 설정을 이어간다.

### P0-2. 서버 인증정보 연결

현재 readiness 코드가 확인하는 운영 설정:

- `GOOGLE_PLAY_BILLING_ENABLED`
- `GOOGLE_PLAY_PRODUCTS_READY`
- `GOOGLE_PLAY_CLIENT_EMAIL`
- `GOOGLE_PLAY_PRIVATE_KEY`

service account 연결과 실제 상품 준비가 끝나기 전에 release flag를 억지로 켜지 않는다.

### P0-3. Play Console subscription 상품 대조

앱 코드와 Play Console의 상품 ID를 정확히 맞춘다.

- `all_monthly`
- `call_monthly`
- `message_monthly`

각 product의 base plan/offer/가격/국가 활성 상태 확인.

**중요:** 현재 `PlayBillingManager.purchase()`는 첫 번째 subscription offer (`offers.get(0)`)를 사용한다. base plan/offer가 여러 개면 잘못된 offer를 선택할 수 있으므로, Play Console 설정에 맞춰 basePlanId/offerId/tag 기준 선택으로 패치한다.

### P0-4. 실제 라이선스 테스터 결제 E2E

Play 내부/비공개 테스트 설치본으로 반드시 실제 테스트한다.

검증 순서:

`이용권 → 상품 조회 → 결제 버튼 → Play 결제창 → 테스트 결제 → purchaseToken → /api/billing/google/verify → Publisher API 확인 → server acknowledgement → entitlement active → 앱 이용권 즉시 반영`

그리고:

- 앱 종료/재실행 후 entitlement 유지
- 재설치 후 구매 복원
- Play 구독 관리 이동
- Web 구독 중 Play 결제 차단
- Play 구독 중 Web 결제 차단

여기까지 통과해야 **Google Play 결제 E2E 완료**로 기록한다.

---

## 7. 다음 패치 P1 — RTDN/구독 수명주기

현재 `inlet/functions/api/billing/google`에는 `verify.js`, `restore.js`가 있지만 RTDN 전용 endpoint는 확인되지 않았다.

즉 앱 안에서 구매 직후 검증은 가능하지만, 사용자가 앱을 열지 않는 동안 발생하는 상태 변경을 서버가 즉시 반영하는 구조는 다음 작업이 필요하다.

다음 구현:

- Google Play Real-time Developer Notifications(RTDN)
- Google Cloud Pub/Sub topic
- Google Play에 Pub/Sub publisher 권한 설정
- 서버 수신 endpoint 또는 subscriber 구성
- subscription notification 수신 후 Publisher API 재조회
- DB entitlement 갱신

최소 처리 상태:

- 갱신
- 사용자 취소
- 만료
- 결제 실패
- grace period
- account hold
- 재개
- 환불/취소된 구매

Play Console의 정기 결제 `일시중지` 기능은 서버 상태 처리가 완성되기 전에는 켜지 않는다.

RTDN의 Play Console 알림 콘텐츠는 CallTag가 현재 정기구독 중심이므로 우선 **정기 결제 + 무효화된 구매** 범위로 설계한다. 일회성 상품을 실제 도입하면 범위를 확장한다.

---

## 8. 다음 패치 순서 — 절대 우선순위

### 1순위: Google Play 결제 실제 연결

Play Console ↔ Google Cloud/API access/service account → server credentials → subscription product 확인 → 실제 테스트 결제.

### 2순위: 결제 E2E 오류 수정

실제 테스트 결제에서 발생한 문제만 증상 기준으로 수정한다. 결제 구조를 이유 없이 갈아엎지 않는다.

### 3순위: RTDN

실제 구매 플로우가 성공한 뒤 갱신/취소/환불/만료 자동 동기화 구축.

### 4순위: Google 로그인 0.44.22 단말 재검증

- 계정 선택 후 실제 로그인 진입
- 무반응 재발 여부
- 실패 시 표시되는 실제 오류/로그 확인

### 5순위: UI 단말 QA

- 더보기 카드 간격
- 런처 아이콘
- Google 계정 선택창의 CallTag 아이콘
- 고객센터 실제 메일 수신

---

## 9. 무료기간/추천인 현재 정책

CallTag 현재 서버 정책:

- 일반 가입 7일 무료
- 가입 시 추천인 코드 입력 +7일
- 최대 14일
- 무료 종료 후 자동결제 없음

추천인 코드 입력은 **회원가입 시에만** 제공한다.

서버 legacy generic 코드에 3일/+5일 값이 남아 있으므로 결제/무료기간 리팩터링 시 CallTag 전용 7일/+7일을 훼손하지 않는다.

---

## 10. 작업 금지선

- 결제를 새로 처음부터 구현하지 않는다. 기존 BillingClient/verify/restore/entitlement를 활용한다.
- 앱 purchase callback만 믿고 권한을 열지 않는다.
- 서버 Publisher API 검증을 우회하지 않는다.
- 앱에서 acknowledge를 중복 처리하지 않는다.
- Web/Play 중복결제 차단을 제거하지 않는다.
- 활성 구독 상태를 로컬 SharedPreferences만으로 최종 판정하지 않는다.
- Play 상품 ID/가격을 추측하지 않는다.
- 결제 때문에 기존 고객/메모/일정/문자 데이터를 삭제하지 않는다.
- Google 로그인 문제를 브라우저 OAuth로 되돌려 해결하지 않는다.
- 깨진 `calltag_launcher_safe.webp` foreground 방식을 다시 사용하지 않는다.
- `versionCode 2026081208`을 Play에 업로드했다면 재사용하지 않는다.

---

## 11. 완료 정의

다음 결제 패치는 아래를 모두 통과해야 완료다.

1. Play Console ↔ Google Cloud/API access 연결
2. service account 권한 연결
3. 서버 Publisher API credential 정상
4. Play subscription ProductDetails 조회 성공
5. 라이선스 테스터 실제 결제창 성공
6. purchaseToken 서버 검증 성공
7. server acknowledgement 성공
8. entitlement active 반영
9. 앱 재시작 후 유지
10. 구매 복원 성공
11. Web ↔ Play 중복결제 차단 확인
12. 이후 RTDN으로 취소/갱신/환불/만료 자동 반영

1~11까지 먼저 끝내고, 12를 운영 안정화 핵심으로 바로 이어간다.
