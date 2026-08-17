# 콜태그 Android 개발자 인수인계

기준 버전: **0.44.41**  
versionCode: **2026081701**  
기준일: **2026-08-17**  
저장소: `pc9839a-lgtm/calltag`  
정본 브랜치: `agent/calltag-v04422-billing-live`  
패키지: `kr.pagero.calltag`  
minSdk: **26**  
compileSdk / targetSdk: **36 / 36**

> 이 문서를 현재 Android 구현의 정본으로 사용한다. 충돌 시 `app/build.gradle`과 실제 코드가 최우선이다. 과거 버전별 HOTFIX/릴리스 문서는 정본으로 사용하지 않는다.

## 1. 현재 릴리스

- 앱 버전: `0.44.41`
- versionCode: `2026081701`
- 최신 브랜치 HEAD: `c0bd8e9b10848bb8e4adbf1b30ded05b863d358e`
- 0.44.41 UI 기능 반영 커밋: `32239441e117d6e4f857931c0a5efd342bf7ee2a`
- 테마 컴파일 수정 커밋: `c0bd8e9b10848bb8e4adbf1b30ded05b863d358e`
- 통화 종료 팝업 전달 안정화: `90fe040d933540aa9c8e809036bd9d225b8ea086`
- 재부팅/업데이트 후 통화감지 권한 판정 수정: `2f92dfa0f3ba48f8a364c86bcce94c21c1b95ed6`
- Compile Check run `32000865080`: 성공
- Current Signed Release run `32000865038`: 성공
- signed AAB/APK 생성 및 기존 Play 업로드 인증서 검증: 성공
- signed release artifact: `9278382448`

### Play 업로드 키

공개 인증서 지문만 기록한다. 키 파일/비밀번호는 문서에 기록하지 않는다.

- SHA-1: `79:80:FD:C6:4E:BE:DD:2B:80:54:5B:60:87:03:6D:5F:78:05:75:8B`
- SHA-256: `C3:4C:98:88:9B:0C:88:8A:BB:39:94:6C:80:16:96:C2:89:E2:82:6C:10:0F:41:7A:0B:CE:25:A3:92:C4:72:A7`
- CI는 위 SHA-256과 다르면 릴리스 빌드를 중단한다.
- 업로드 키가 없다고 새 키를 자동 생성하지 않는다.

## 2. Google 로그인 — 해결 완료

2026-08-14 실제 단말에서 Google 로그인 성공 확인.

OAuth 역할을 절대 혼동하지 않는다.

- Android OAuth Client ID: `31346298247-26okq7jrsac89q8pucjeuui6jrfofvqn.apps.googleusercontent.com`
  - 용도: Android 앱 패키지 + SHA-1 식별
  - `requestIdToken()`의 server client ID로 사용하지 않는다.
- Web/Backend OAuth Client ID: `31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`
  - 용도: Android 앱 `requestIdToken()` / 서버 ID Token audience 검증
  - `BuildConfig.GOOGLE_SERVER_CLIENT_ID` 기본값은 이 값이어야 한다.

과거 코드 10(`DEVELOPER_ERROR`) 원인은 Android Client ID를 Web server client ID 위치에 넣은 것이었다. CI에서 완성 APK의 DEX 전체를 검사해 Web Client가 들어가고 Android Client가 server client 위치에 들어가지 않도록 유지한다.

## 3. 통화 전 고객정보 표시

핵심 파일:

- `CallTagScreeningService.java`
- `CallerOverlayManager.java`
- `CallerOverlayCallStateWatcher.java`
- `CallerIdSetupButton.java`

현재 기준:

- Android `ROLE_CALL_SCREENING` 역할을 사용한다.
- 사용자 설정 `수신 전화 고객정보 표시`가 ON이고 등록 고객이면 고객명/최근 메모를 표시한다.
- 오버레이 권한이 없거나 표시 실패 시 알림 fallback을 사용한다.
- 홈의 `통화 감지` / `수신 전화 고객정보 표시`는 ON/OFF 텍스트를 별도로 쓰지 않고 스위치 자체로 상태를 표현한다.
- 역할 권한이 없는데 사용자가 켜면 시스템 역할 요청창을 즉시 연다.
- 실제 `RINGING/OFFHOOK`을 관찰한 뒤의 `IDLE`에서만 통화 종료 처리를 한다.
- `CallerOverlayCallStateWatcher`가 `TelecomManager.isInCall()`로 실제 통화 lifecycle을 보조 감시한다.

## 4. 통화 종료 후 작은 팝업

핵심 파일:

- `CallMonitorService.java`
- `PostCallActivity.java`
- `PostCallActivityLauncher.java`
- `PostCallDeliveryGuard.java`
- `PostCallRecoveryStore.java`
- `PostCallLaunchReceipt.java`
- `CallProcessingLedger.java`

제품 원칙:

- 통화 종료 후 전체화면이 아니라 **작은 팝업 1개**가 기본이다.
- 큰 전체화면 + 작은 팝업을 동시에 띄우지 않는다.
- 팝업 핵심 입력은 고객명/메모 중심으로 유지한다.

현재 전달 구조:

1. Telephony call state → 실시간 1차 트리거
2. CallLog `ContentObserver` → 종료 누락 2차 복구 트리거
3. Activity 실행 후 실제 화면 노출 여부를 `PostCallLaunchReceipt`로 확인
4. 첫 Activity가 실제 노출되지 않으면 2.4초 후 `retryOnce()`로 1회 재실행
5. 재실행 후 2.2초 뒤에도 실제 노출되지 않았을 때만 고우선 알림 fallback
6. 알림 권한/채널이 사용할 수 없으면 전달 완료로 오인하지 않고 `PostCallRecoveryStore`에 미전달 상태 유지
7. `CallProcessingLedger`로 같은 CallLog row의 중복 처리를 방지

### 재부팅/앱 업데이트 복구

`BootReceiver`에서 통화감지를 다시 켤 수 있는 필수 권한은 아래 두 개만 판단한다.

- `READ_PHONE_STATE`
- `READ_CALL_LOG`

`POST_NOTIFICATIONS`가 꺼져 있다는 이유로 재부팅/앱 업데이트 후 통화감지 자체를 OFF 처리하지 않는다. 알림 권한은 fallback 표시 가능 여부에만 사용한다.

### 남은 실기기 QA

- 앱 전면 상태 연속 수신/발신
- 앱 백그라운드
- 화면 잠금
- 장시간 미사용 후 첫 통화
- 부재중/거절/짧은 통화
- 연속 통화에서 중복 팝업 여부
- 삼성/픽셀/기타 OEM별 종료 후 실제 팝업 노출률
- 알림 권한 OFF 상태에서 recovery queue 재시도 확인
- 재부팅/앱 업데이트 직후 첫 통화 확인

## 5. 고객 화면 UX

현재 기준:

- 고객목록 빠른 액션: `상태 변경` / `문자 보내기`는 텍스트 버튼 유지.
- 삭제만 작은 휴지통 아이콘.
- 삭제 아이콘은 `ImageButton` 중앙정렬 터치영역을 사용한다.
- 고객 삭제는 별도 Activity로 이동하지 않고 현재 화면 위 확인 팝업에서 처리한다.
- 삭제 확인: 취소=회색, 삭제=빨강.
- 고객수정/상세에서 연락처 저장 기능을 제공한다.
- Android 연락처 INSERT 화면에 고객명/전화번호를 채워 연다.

## 6. 일정 시간 선택 — 0.44.41

핵심 파일: `TaskTimeChoiceDialog.java`

기존 시간 선택 UI를 콜태그 전용 휠 방식으로 교체했다.

- 오전/오후 세그먼트
- 시: 1~12 순환 휠
- 분: `00/05/10/.../55` 5분 단위 순환 휠
- 선택 시간 실시간 요약
- 액션: `취소` / `이 시간으로 등록`
- 현재 시각을 5분 단위로 보정해 초기값으로 사용
- 블랙/화이트 테마 공통 색상 리소스를 사용

## 7. 캘린더 접기/펼치기 — 0.44.41

핵심 파일: `CollapsibleConsultationLayout.java`

캘린더 화면의 큰 `접기` 버튼을 제거하고 `월간 캘린더` 헤더 행 자체를 토글로 사용한다.

- 작은 화살표로 펼침/접힘 상태 표시
- **월간 달력 본문만** 숨김/표시
- 선택 날짜, 일정 추가, 일정 목록은 그대로 유지
- 사용자의 마지막 펼침/접힘 상태를 `SharedPreferences`에 저장
- 화면이 다시 그려져도 상태 유지

## 8. 앱 블랙/화이트 테마 — 0.44.41

핵심 파일:

- `CallTagThemeManager.java`
- `MoreSettingsHubView.java`
- `res/values/colors.xml`
- `res/values-night/colors.xml`

진입 경로:

`더보기 → 앱 관리 → 테마`

정책:

- 선택값: `블랙` / `화이트`
- 기본값: `블랙`
- 선택값을 기기에 저장
- Android 12 이상은 `UiModeManager.setApplicationNightMode()` 사용
- 상태바/내비게이션바도 선택 테마에 맞춰 적용
- 화면별 하드코딩 색상을 늘리지 말고 공통 color resource를 우선 사용

## 9. 문자 기능

문자 메인 상단 3개 우선 메뉴:

1. `고객선택후 문자`
2. `통화후 자동문자`
3. `페이지로 문의접수문자`

관리 기능:

- 문자 템플릿
- 그룹·단체문자
- 발송 내역

페이지로 문의접수 자동문자 설정 화면은 설명문을 최소화하고 아래만 남긴다.

- 사용 여부
- 문자 내용/템플릿
- 발송 지연
- 페이지별 설정
- 저장

권한이 부족할 때만 짧은 상태와 권한 허용 액션을 표시한다.

## 10. 페이지로 문의 → 콜태그

정본 상세 문서: `PAGERO_CUSTOMER_INTEGRATION_KO.md`

현재 주요 동작:

- 같은 Inlet 계정 owner 기준 자동 연결
- 일반 사용자가 webhook/비밀키를 직접 넣지 않는다.
- 페이지로 문의가 콜태그 고객으로 생성/갱신된다.
- 문의 메타데이터의 `answers`, `values`, `pageTitle`, `site`, `campaign`, `source`, `email`, `content`와 동적 필드를 가능한 한 보존한다.
- 문의 전체 내용을 고객 memo 구성에 사용한다.
- 페이지로 자동문자 기본값은 OFF.
- eventId 기준 중복 수신/발송을 방지한다.

## 11. Google Play Billing

정본 상세 문서: `GOOGLE_PLAY_BILLING_SETUP_KO.md`

현재 Play 정기결제 상품은 **2개만** 사용한다.

| Product ID | 가격 | 범위 |
|---|---:|---|
| `call_monthly` | 1,900원/월 | 전화관리 |
| `message_monthly` | 990원/월 | 문자자동화 |

현재 `all_monthly` 통합권은 만들지 않는다.

현재 구현:

- 화면 진입 즉시 Play Billing 연결/상품조회 시작
- 서버 entitlement 조회와 ProductDetails 조회를 독립 실행
- 상품정보 수신 후 결제 버튼 활성화
- purchase token 서버 검증 구현
- 계정 전환 시 이전 계정 entitlement/referral/pagero 상태 cache 제거
- 백엔드에서 purchase token owner가 다른 계정으로 재귀속되지 않도록 검증

### 아직 미구현

Google Play RTDN(Pub/Sub) 기반 구독 lifecycle 동기화는 아직 완료되지 않았다.

- Pub/Sub topic
- Play notification publisher 권한
- RTDN 설정
- subscriber endpoint
- renewal/cancel/expiry/grace/hold/resume/refund 이벤트 재조회 및 entitlement 갱신

## 12. CI / 릴리스

현재 기준:

- `app/build.gradle`의 versionName/versionCode를 릴리스 기준으로 사용
- Android API 36 빌드
- 저장된 기존 Play 업로드 키만 사용
- 업로드 키 SHA-256 고정 검증
- signed release AAB/APK 생성
- debug compile-check APK 생성
- 0.44.41 compile check와 signed release 모두 성공

과거 버전 전용 workflow의 contract check 실패는 현재 버전 compile 실패로 해석하지 않는다. 현재 릴리스 판단은 `CallTag 0.44.25 Compile Check`, `CallTag Current Signed Release`, `Build CallTag Play Internal`의 실제 최신 HEAD 결과를 우선한다.

## 13. 작업 시 금지사항

- `all_monthly`를 임의로 추가하지 않는다.
- Google Android OAuth Client ID를 `requestIdToken()` server client ID로 넣지 않는다.
- Play 업로드 키를 새로 생성/교체하지 않는다.
- 통화 종료 팝업을 전체화면으로 되돌리지 않는다.
- 통화 종료 시 전체화면과 작은 팝업을 동시에 띄우지 않는다.
- 고객목록의 상태/문자 버튼을 아이콘만으로 바꾸지 않는다.
- 페이지로 문의 내용을 일부 필드만 남기고 버리지 않는다.
- 사용자에게 서비스계정 private key, keystore, 비밀번호를 요구하지 않는다.
- RTDN이 없는 상태에서 구독 lifecycle 동기화 완료라고 쓰지 않는다.
- 개별 화면에 블랙/화이트 색상을 하드코딩해 테마 일관성을 깨지 않는다.

## 14. 현재 우선순위

1. 0.44.41 통화 종료 팝업 OEM별 반복 실기기 QA
2. 0.44.41 블랙/화이트 테마 전 화면 깨짐/가독성 QA
3. 시간 휠 입력값 저장 및 수정 플로우 QA
4. 캘린더 접힘 상태/일정 추가/날짜 변경 회귀 QA
5. 결제 화면 실제 계정별 속도/국가 오류 QA
6. 페이지로 문의 자동문자 실사용 QA
7. 고객목록/삭제/연락처 저장 UI 최종 확인
8. Google Play RTDN 구현
9. Play 내부테스트 안정화 후 프로덕션 준비
