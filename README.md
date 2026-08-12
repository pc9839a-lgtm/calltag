# 콜태그 (CallTag)

전화 전 고객 맥락 확인부터 통화 종료 후 메모·할 일·문자 자동화까지 연결하는 Android 고객관리 앱입니다.

## 다음 AI가 반드시 먼저 읽을 문서

1. [`docs/NEXT_AI_HANDOFF_20260812_KO.md`](docs/NEXT_AI_HANDOFF_20260812_KO.md) — **최우선 정본. 다음 패치 P0 = Google Play 결제 실제 연결 및 테스트 결제 E2E**
2. [`docs/CURRENT_RELEASE_STATUS_20260812_KO.md`](docs/CURRENT_RELEASE_STATUS_20260812_KO.md) — 릴리스/실기기 QA 상태
3. [`docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`](docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md) — 기존 결제 서버/entitlement 상세 구조
4. [`docs/ANDROID_DEVELOPER_HANDOFF_KO.md`](docs/ANDROID_DEVELOPER_HANDOFF_KO.md) — Android 구조와 데이터 안전 규칙
5. [`docs/PAGERO_CUSTOMER_INTEGRATION_KO.md`](docs/PAGERO_CUSTOMER_INTEGRATION_KO.md) — PageRo 연동

**문서만 보고 구현 여부를 판단하지 않는다. 실제 코드 → Play/서버 설정 → signed AAB → 실제 휴대전화 E2E를 구분한다.**

## 현재 Android 기준

- package: `kr.pagero.calltag`
- branch: `agent/calltag-auth-ux-google-upgrade-fix`
- versionName: **0.44.22**
- versionCode: **2026081208**
- minSdk: 26
- target/compile SDK: 36
- Play 업로드키 signed AAB 빌드 성공
- Workflow run: `31557329238`
- Artifact ID: `9126476904`
- 다음 Play versionCode: **2026081209 이상**

Play Console에 한 번 업로드된 versionCode는 재사용하지 않는다.

## 다음 패치 핵심 — Google Play 결제

Google Play 결제는 **앱 코드와 서버 코드에 이미 붙어 있다. 새로 처음부터 만들지 않는다.**

현재 Android:

- `com.android.billingclient:billing:9.1.0`
- BillingClient 연결
- 정기구독 ProductDetails 조회
- 구매 플로우
- pending purchase
- purchaseToken 서버 전송
- 구매 복원
- Google Play 구독 관리
- 서버 entitlement 기반 권한
- Web ↔ Play 중복결제 사전 차단

핵심 Android 파일:

- `BillingEntitlementActivity.java`
- `PlayBillingManager.java`
- `FeatureEntitlementStore.java`
- `AuthApiClient.java`

현재 코드 productId:

- `all_monthly`
- `call_monthly`
- `message_monthly`

현재 서버 `pc9839a-lgtm/inlet`:

- `/api/billing/entitlements`
- `/api/billing/google/verify`
- `/api/billing/google/restore`
- Android Publisher API 검증 구조
- 서버 acknowledgement
- subscription DB 저장
- partner commission 기록

### 다음 패치에서 할 일

사용자가 **Play Console ↔ Google Cloud/API access 연결을 다음 패치에서 진행**하기로 확정했다.

순서:

1. Play Console과 Google Cloud/API access 연결
2. 서버용 service account 권한 연결
3. Publisher API credential 서버 설정
4. Play Console subscription product/base plan과 앱 productId 대조
5. 라이선스 테스터로 실제 Play 결제
6. purchaseToken → 서버 verify → Publisher API → acknowledge → entitlement active 확인
7. 앱 재시작/재설치 구매 복원 확인
8. Web ↔ Play 중복결제 양방향 확인
9. 이후 RTDN/Pub/Sub로 갱신·취소·환불·만료 자동 동기화

세부 체크리스트는 `docs/NEXT_AI_HANDOFF_20260812_KO.md`를 따른다.

## Google 로그인 — 0.44.22

브라우저 OAuth를 사용하지 않는다.

`Google로 계속하기 → GoogleCredentialLoginActivity → Credential Manager → Google ID Token → /api/call/google/id-token → CallTag session`

0.44.22 변경:

- `GetGoogleIdOption`
- authorized account filter false
- auto select false
- main executor callback
- provider timeout 30초
- token exchange timeout 20초
- Credential Activity `exported=false`
- `calltag://credential/google` 딥링크 제거

**계정 선택 → 실제 세션 생성 E2E는 0.44.22 설치본에서 다시 확인해야 한다.**

## 더보기 — 0.44.22

8개 진입점:

1. 계정
2. 이용권
3. 문자 관리
4. 고객 관리
5. 페이지로
6. 파트너
7. 데이터 관리
8. 앱 정보

그룹:

- 내 정보: 계정 / 이용권
- 업무 관리: 문자 관리 / 고객 관리
- 서비스: 페이지로 / 파트너
- 앱 관리: 데이터 관리 / 앱 정보

각 메뉴를 독립 카드로 분리했으며 메뉴 높이 64dp, 메뉴 사이 12dp, 섹션 사이 34dp다.

`통화 후 자동문자`는 더보기의 별도 대형 카드가 아니라 **문자 관리 안**에 둔다.

## 앱 정보 / 고객센터

앱 정보:

- 버전 정보
- 서비스 이용약관
- 개인정보처리방침
- 고객센터

고객센터:

`앱 폼 → POST /api/call/support → 서버/AWS SES → roadfor@kakao.com`

고객 이메일은 Reply-To로 사용한다. 서버 배포/401 smoke는 통과했으며 실제 로그인 사용자 문의 메일 수신은 단말 E2E가 남아 있다.

## 앱 아이콘 — 0.44.22

0.44.21의 깨진 WebP Adaptive Icon foreground 방식은 폐기했다.

0.44.22는 vector foreground + Adaptive Icon을 사용하며 release AAB에 `calltag_launcher_safe.webp`가 들어오면 CI가 실패한다.

실제 런처와 Google 계정 선택창 아이콘은 단말에서 확인한다.

## 회원가입 UX 고정

- 필수 항목만 빨간 `*`
- 선택 항목 `[선택]` 반복 금지
- 추천인 코드는 회원가입 시에만 선택 입력
- 이메일 인증 요청 단계에서 약관 선행 강제 금지
- 최종 가입 제출 시 필수 약관 검사

## 데이터 안전

- 고객/메모/일정/문자 데이터 초기화 금지
- DB 변경은 보존 마이그레이션
- 결제 만료로 기존 고객 데이터를 삭제하지 않음
- 서버 entitlement 검증 우회 금지
- 앱 purchase callback만으로 유료 기능 개방 금지
- Web/Play 중복결제 방지 제거 금지
- 결제 때문에 Google 로그인 구조를 브라우저 OAuth로 되돌리지 않음

## 현재 단말 QA 우선순위

1. Google Play 실제 테스트 결제 E2E
2. Google 로그인 계정 선택 후 세션 생성
3. 더보기 간격/그룹 확인
4. 런처 및 Google 계정 선택창 아이콘 확인
5. 고객센터 `roadfor@kakao.com` 실제 수신/Reply-To 확인
6. 통화 종료 후 작은 팝업 하나만 표시되는지 확인
