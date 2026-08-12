# CallTag 다음 AI 인수인계 정본 — 2026-08-12

## 0. 가장 먼저 읽을 것

다음 AI는 이 문서를 먼저 읽고 작업을 시작한다.

1. `docs/NEXT_AI_HANDOFF_20260812_KO.md` — **현재 최우선 정본**
2. `docs/PLAY_BILLING_P0_EXECUTION_20260812_KO.md` — **Google Play 결제 P0 실행 정본**
3. `docs/CURRENT_RELEASE_STATUS_20260812_KO.md` — 최신 릴리스/QA 상태
4. `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md` — 결제 상세 구조
5. 실제 Android/Server 코드

현재 다음 패치의 핵심은 **Google Play 정기결제의 외부 설정을 완료하고 라이선스 테스터 실제 결제 E2E를 끝내는 것**이다.

### 2026-08-12 중요 정정

과거 문서의 `Play Console ↔ Google Cloud 프로젝트 연결` 표현을 그대로 따라가면 안 된다.

현재 Google 공식 절차에서는 **Play Console 개발자 계정을 Google Cloud 프로젝트에 별도로 연결할 필요가 없다.**

현재 순서는:

`Google Cloud 프로젝트 → Google Play Android Developer API 활성화 → service account 생성 → Play Console 사용자/권한에서 service account 초대 + 결제 권한 → 서버 credential → 상품/base plan → 라이선스 테스터 E2E`

세부 클릭 순서는 `docs/PLAY_BILLING_P0_EXECUTION_20260812_KO.md`를 따른다.

---

## 1. 현재 Android 기준

- 저장소: `pc9839a-lgtm/calltag`
- 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`
- 관련 PR: `#80`
- 패키지: `kr.pagero.calltag`
- versionName: **0.44.23**
- versionCode: **2026081209**
- minSdk: 26
- target/compile SDK: 36
- Billing Library: `com.android.billingclient:billing:9.1.0`
- Play 업로드키 signed AAB 빌드 성공
- 성공 workflow: `Build CallTag Play Internal`
- 성공 Run ID: `31558514997`
- Artifact ID: `9126905168`
- Artifact: `calltag-v0.44.23-code2026081209-play-internal`
- Artifact ZIP SHA-256: `71a0d62074d97bc229bb986a09c32dfbdd980558630bb8fff81cc3d761f5eb74`

Artifact 내부:

- `CallTag-v0.44.23-code2026081209.aab`
- `CallTag-v0.44.23-code2026081209-debug.apk`
- `CALLTAG_AAB_SHA256.txt`
- `CALLTAG_UPLOAD_KEY_FINGERPRINTS.txt`

Play Console에 한 번 업로드한 versionCode는 재사용하지 않는다.

**2026081209를 Play에 업로드한 뒤 다음 versionCode는 2026081210 이상**을 사용한다.

---

## 2. 방금 끝난 0.44.23 결제 안전패치

0.44.22까지 `PlayBillingManager.purchase()`는 `subscriptionOfferDetails.get(0)`의 offerToken을 사용했다.

이 방식은 Play Console에 base plan 또는 offer가 여러 개면 잘못된 상품 조건을 임의로 선택할 수 있다.

0.44.23에서는:

- 구매 가능한 offer가 1개뿐이면 사용
- 여러 offer가 있어도 `offerId`가 없는 기본 base plan이 정확히 1개면 그것만 사용
- 기본 base plan 후보가 여러 개면 결제창을 열지 않음
- 임의 `offers.get(0)` 선택 제거

향후 할인/무료체험/연간 base plan 등을 실제 도입할 때는 `basePlanId` / `offerId` / offer tag를 명시적으로 매핑한다.

### CI도 같이 정리됨

과거 공용 workflow가 `0.43.0 / code 68`을 하드코딩해 최신 빌드를 실패시키던 문제를 수정했다.

현재 공용 Play workflow는:

- `app/build.gradle`에서 versionName/versionCode 자동 인식
- package/API 36/release signing 확인
- Billing 9.1.0 확인
- 서버 purchase verify 호출 확인
- `offers.get(0).getOfferToken()` 금지 확인
- 기존 검증된 Play upload key 사용
- debug APK + signed release AAB 생성
- AAB 서명 검증
- 손상된 launcher bitmap 유입 차단

문서 수정만으로 AAB가 반복 빌드되지 않도록 앱/Gradle 변경에만 push build를 걸었다.

과거 `calltag-v04422-play-aab.yml`은 최신 브랜치에서 제거했다.

---

## 3. Google 로그인 — 0.44.22 구조 유지

현재 구조:

`LoginActivity → GoogleCredentialLoginActivity → Android Credential Manager → Google ID Token → /api/call/google/id-token → CallTag session`

현재 고정:

- `GetGoogleIdOption` 사용
- `setFilterByAuthorizedAccounts(false)`
- `setAutoSelectEnabled(false)`
- Web/server client ID를 `serverClientId`로 사용
- Credential Manager callback은 main executor
- provider timeout 30초
- ID token 서버 exchange timeout 20초
- `GoogleCredentialLoginActivity`는 `exported=false`
- 불필요한 `calltag://credential/google` 외부 딥링크 제거

Android OAuth Client:

- package: `kr.pagero.calltag`
- Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- SHA-1: Play 앱 서명 키 인증서 SHA-1

Web/server Client ID:

- `31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`

**주의:** 최신 설치본에서 실제 계정 선택 → 세션 생성 완료까지 단말 E2E는 아직 재확인해야 한다.

실패하면 실제 화면/로그 기준으로 진단한다. 브라우저 OAuth 방식으로 되돌리지 않는다.

---

## 4. 더보기 현재 구조

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

UI:

- 메뉴 각각 독립 카드
- 메뉴 높이 64dp
- 메뉴 사이 12dp
- 섹션 사이 34dp
- 통화 후 자동문자는 더보기 별도 대형 카드 금지. `문자 관리` 안에 둔다.

계정 화면은 이름/연락처/이메일, 다시 불러오기, 로그아웃, 회원탈퇴 중심으로 유지한다.

---

## 5. 앱 아이콘 현재 구조

0.44.21의 깨진 WebP launcher foreground 방식은 폐기했다.

현재:

- vector foreground 사용
- Adaptive Icon 사용
- release manifest에서 `@mipmap/ic_launcher_calltag` / round icon 고정
- `calltag_launcher_safe.webp`가 release AAB에 들어가면 CI 실패

실제 삼성/Pixel 런처와 Google 계정 선택창에서 아이콘이 정상인지 단말 확인이 남아 있다.

---

## 6. 고객센터 확정 구조

`더보기 → 앱 정보 → 고객센터`

앱 내 문의 폼 → `POST /api/call/support` → 서버 → AWS SES → **roadfor@kakao.com**.

고객 이메일은 `Reply-To`로 사용한다.

완료:

- 서버 route 배포
- 인증 없는 요청 401 smoke 성공

남음:

- 실제 로그인 사용자 문의 전송
- `roadfor@kakao.com` 수신 확인
- Reply-To 확인

---

# 7. 가장 중요: Google Play 결제는 이미 구현되어 있다

**다음 AI는 결제를 새로 만드는 작업부터 시작하면 안 된다.**

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
- suspended subscription 복원 조회
- Google Play 구독 관리 화면
- 서버 entitlement 기반 기능 활성화
- 결제 직전 entitlement 재조회
- 활성 Web 구독 중 Play 중복결제 차단
- 기존 Play 구독 중 추가결제 차단
- 서버 상태를 확인할 수 없으면 결제 시작 금지
- 모호한 복수 base plan/offer 임의 선택 차단

현재 Android productId:

- `all_monthly` — 통합권 — 코드 화면상 6,000원
- `call_monthly` — 전화관리 — 코드 화면상 1,900원
- `message_monthly` — 문자자동화 — 코드 화면상 990원

**가격/상품 구성은 Play 공개 직전 실제 Play Console 상품과 최종 대조한다. 앱 하드코딩값만 보고 Play 상품을 임의 생성하지 않는다.**

---

## 8. 서버 결제도 이미 구현되어 있다

서버 저장소: `pc9839a-lgtm/inlet`, branch `main`.

핵심 API:

- `/api/billing/entitlements`
- `/api/billing/subscriptions`
- `/api/billing/readiness`
- `/api/billing/google/verify`
- `/api/billing/google/restore`
- `/api/billing/web/precheck`

Google purchase verify는 서버에서 Android Publisher API를 호출하도록 구현되어 있다.

서버가 수행하는 핵심:

- packageName 검증 (`kr.pagero.calltag`)
- productId 허용 목록 검증
- `purchases.subscriptionsv2.get`으로 실제 purchase token 검증
- 응답 line item productId 일치 확인
- 구독 상태/만료 확인
- Web ↔ Google Play 중복구독 차단
- purchase token 원문 대신 SHA-256 hash 저장
- subscription DB upsert
- acknowledgement 필요 시 **서버에서 acknowledge**
- entitlement 반환
- 추천 관계가 있으면 partner commission ledger 기록

앱에 별도 acknowledge 로직을 중복 추가하지 않는다.

### 서버 운영 설정

현재 readiness가 확인하는 값:

- `GOOGLE_PLAY_BILLING_ENABLED`
- `GOOGLE_PLAY_PRODUCTS_READY`
- `GOOGLE_PLAY_CLIENT_EMAIL`
- `GOOGLE_PLAY_PRIVATE_KEY`

서버 저장소 문서:

`docs/CALLTAG_GOOGLE_PLAY_BILLING_ENV_20260812_KO.md`

실제 service account private key/JSON은 저장소에 커밋하지 않는다.

---

# 9. 다음 패치 P0 — Google Play 외부 연결 + 실제 결제 E2E

현재 **코드/AAB 준비까지 완료**되었고, 외부 Play Console/Google Cloud 설정과 실제 라이선스 테스터 결제가 남아 있다.

## P0-1. Google Cloud

1. CallTag 결제 서버용 Google Cloud 프로젝트 선택
2. `Google Play Android Developer API` 활성화
3. 서버용 service account 생성
4. service account JSON 키 생성
5. JSON의 `client_email`과 `private_key`를 서버 secret으로 사용할 준비

JSON/private key는 GitHub/문서/이슈/PR에 올리지 않는다.

## P0-2. Play Console service account 권한

Play Console 개발자 계정 수준:

`사용자 및 권한 → 새 사용자 초대`

service account 이메일을 입력한다.

Google Play Billing API에 필요한 최소 권한:

- `재무 데이터, 주문 및 취소 설문 응답 보기`
- `주문 및 정기 결제 관리`

가능하면 `kr.pagero.calltag` 앱 범위로 제한한다.

**Cloud 프로젝트를 Play Console에 별도 연결하는 단계를 찾느라 시간을 쓰지 않는다. 현재 공식 절차는 service account 초대 방식이다.**

## P0-3. 서버 credential

등록:

- `GOOGLE_PLAY_CLIENT_EMAIL`
- `GOOGLE_PLAY_PRIVATE_KEY`

처음에는:

- `GOOGLE_PLAY_BILLING_ENABLED=0`
- `GOOGLE_PLAY_PRODUCTS_READY=0`

유지한다.

credential 문자열이 존재하는 것만으로 권한 검증 완료라고 보지 않는다. 실제 purchaseToken verify가 최종 확인이다.

## P0-4. Play Console subscription 상품 대조

정확히 대조:

- `all_monthly`
- `call_monthly`
- `message_monthly`

각 product:

- productId
- base plan
- 월간 자동 갱신 여부
- 가격
- 한국 판매 활성
- 테스트 트랙 ProductDetails 조회 가능 여부

첫 E2E에서는 상품당 구매 가능한 기본 base plan을 **1개로 단순화**하는 것을 권장한다.

할인 offer/무료체험 offer/복수 base plan을 먼저 섞지 않는다.

상품 준비 완료 후:

`GOOGLE_PLAY_PRODUCTS_READY=1`

service account/API 검증 준비까지 완료 후:

`GOOGLE_PLAY_BILLING_ENABLED=1`

플래그를 먼저 켜지 않는다.

## P0-5. 라이선스 테스터 실제 결제 E2E

Play 내부/비공개 테스트 설치본으로 반드시 실제 테스트한다.

검증 순서:

`이용권 → 상품 조회 → 결제 버튼 → Play 결제창 → 테스트 결제 → purchaseToken → /api/billing/google/verify → Publisher API 확인 → server acknowledgement → entitlement active → 앱 이용권 즉시 반영`

그리고:

- 앱 완전 종료/재실행 후 entitlement 유지
- 로그아웃/로그인 후 유지
- 재설치 후 구매 복원
- Play 구독 관리 이동
- Web 구독 중 Play 결제 차단
- Play 구독 중 Web 결제 차단
- pending 상태에서는 entitlement 선반영 금지

여기까지 통과해야 **Google Play 결제 E2E 완료**로 기록한다.

현재 테스트 후보 AAB는 `0.44.23 / code 2026081209`이다.

---

## 10. 실패 분기

### ProductDetails가 안 나옴

확인:

- Play 테스트 트랙 설치본인지
- 설치 Google 계정이 테스터인지
- productId 오탈자
- subscription/base plan 활성
- 한국 판매/가격 활성
- package `kr.pagero.calltag`

### `/api/billing/readiness`에서 비활성

가능한 코드:

- release flag 미활성
- products ready 미활성
- service account credential 미설정

플래그를 억지로 켜지 말고 원인을 해결한다.

### Google Publisher API 401/403

확인:

- Google Play Android Developer API 활성
- service account 이메일 Play Console 초대
- 재무 데이터 보기 권한
- 주문/정기 결제 관리 권한
- CallTag 앱 권한 범위
- 서버 client email/private key

### verify 성공인데 앱 entitlement 미반영

확인:

- `/api/billing/google/verify` 응답
- `billing_subscriptions` upsert
- entitlement response
- `FeatureEntitlementStore.saveServerEntitlement()`
- `onServerVerified()` 재렌더

로컬에서 임의 active 처리하지 않는다.

---

# 11. 다음 패치 P1 — RTDN/구독 수명주기

P0 실제 구매 플로우가 성공한 뒤 진행한다.

현재 서버에는 `verify.js`, `restore.js`가 있지만 RTDN 전용 수신 구조는 별도 구축이 필요하다.

Google Cloud:

- Pub/Sub API 활성화
- topic 생성
- `google-play-developer-notifications@system.gserviceaccount.com`에 topic의 Pub/Sub Publisher 권한 부여
- push subscription 또는 서버 subscriber 구성

Play Console:

`CallTag → 수익 창출 → 수익 창출 설정 → 실시간 개발자 알림`

- RTDN 활성화
- topic: `projects/{project_id}/topics/{topic_name}`
- 테스트 메시지 성공 확인
- 우선 범위: **정기 결제 + 무효화된 구매**

서버:

- RTDN 수신 endpoint/subscriber
- notification 수신 후 Publisher API 재조회
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

RTDN 알림만 보고 entitlement를 결정하지 않는다. 알림 수신 후 Publisher API를 재조회한다.

Play Console의 정기 결제 `일시중지` 기능은 서버 상태 처리가 완성되기 전에는 켜지 않는다.

---

## 12. 무료기간/추천인 현재 정책

CallTag 현재 서버 정책:

- 일반 가입 7일 무료
- 가입 시 추천인 코드 입력 +7일
- 최대 14일
- 무료 종료 후 자동결제 없음
- 추천인 코드는 회원가입 시에만 선택 입력

서버 legacy generic 코드에 3일/+5일 값이 남아 있으므로 결제/무료기간 리팩터링 시 CallTag 전용 7일/+7일을 훼손하지 않는다.

추천인과 파트너는 UI/데이터 의미를 분리한다.

---

## 13. 작업 금지선

- 결제를 새로 처음부터 구현하지 않는다.
- 기존 `PlayBillingManager` / verify / restore / entitlement를 활용한다.
- 앱 purchase callback만 믿고 권한을 열지 않는다.
- 서버 Publisher API 검증을 우회하지 않는다.
- 앱에서 acknowledge를 중복 처리하지 않는다.
- Web/Play 중복결제 차단을 제거하지 않는다.
- 활성 구독 상태를 SharedPreferences만으로 최종 판정하지 않는다.
- Play 상품 ID/가격을 추측하지 않는다.
- 복수 offer에서 첫 번째 offer를 임의 선택하지 않는다.
- 결제 때문에 고객/메모/일정/문자 데이터를 삭제하지 않는다.
- service account JSON/private key를 저장소에 올리지 않는다.
- Google 로그인 문제를 브라우저 OAuth로 되돌리지 않는다.
- 결제 P0보다 자잘한 UI 수정을 먼저 하지 않는다.

---

## 14. 다음 작업 절대 우선순위

### 1순위 — Google Play 외부 설정

Google Play Android Developer API → service account → Play Console 사용자/권한 → 서버 credential.

### 2순위 — subscription/base plan 대조

`all_monthly`, `call_monthly`, `message_monthly` 실제 Play 상품 구성/가격/한국 판매 상태 확인.

### 3순위 — 라이선스 테스터 실제 결제 E2E

0.44.23 Play 설치본으로 purchase → verify → acknowledge → entitlement → restore → 중복결제 차단까지 확인.

### 4순위 — E2E에서 나온 결제 오류만 수정

구조를 이유 없이 갈아엎지 않는다.

### 5순위 — RTDN/Pub/Sub

갱신/취소/환불/만료 자동 동기화.

### 6순위 — Google 로그인 최신 설치본 단말 재검증

계정 선택 후 실제 세션 생성 완료 확인.

### 7순위 — UI/단말 QA

- 더보기 카드 간격
- 런처 아이콘
- Google 계정 선택창 아이콘
- 고객센터 실제 메일 수신/Reply-To
- 통화 종료 후 작은 팝업 1개만 표시

---

## 15. P0 완료 정의

다음이 모두 통과해야 한다.

- [ ] Google Play Android Developer API 활성화
- [ ] service account 생성
- [ ] Play Console 사용자 초대
- [ ] 재무 데이터/주문 보기 권한
- [ ] 주문/정기 결제 관리 권한
- [ ] 서버 client email/private key 등록
- [ ] 3개 productId 대조
- [ ] base plan/가격/한국 판매 활성
- [ ] ProductDetails 조회
- [ ] 라이선스 테스터 Play 결제창
- [ ] 테스트 결제 완료
- [ ] purchaseToken 서버 verify
- [ ] `subscriptionsv2.get` 검증 성공
- [ ] 서버 acknowledgement 성공
- [ ] entitlement active
- [ ] 앱 즉시 반영
- [ ] 앱 재시작 유지
- [ ] 재설치 구매 복원
- [ ] Web → Play 중복결제 차단
- [ ] Play → Web 중복결제 차단

P0 완료 전에는 결제 완료라고 문서화하지 않는다.

CI 성공/AAB 생성은 **코드 빌드 성공**일 뿐 실제 Google Play 결제 성공이 아니다.
