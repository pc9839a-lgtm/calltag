# 콜태그 Android 개발자 인수인계

기준 버전: **0.44.38**  
versionCode: **2026081224**  
기준일: **2026-08-14**  
저장소: `pc9839a-lgtm/calltag`  
정본 브랜치: `agent/calltag-v04422-billing-live`  
패키지: `kr.pagero.calltag`  
minSdk: **26**  
compileSdk / targetSdk: **36 / 36**

> 이 문서를 현재 Android 구현의 정본으로 사용한다. 충돌 시 `app/build.gradle`과 실제 코드가 최우선이다. 과거 버전별 HOTFIX/릴리스 문서는 정본으로 사용하지 않는다.

## 1. 현재 릴리스

- 앱 버전: `0.44.38`
- versionCode: `2026081224`
- 현재 브랜치 HEAD 기준 릴리스/CI 정리 커밋: `4260165f70a03a73cd31221523ec007f8acd3cc3`
- 통화 안정화 핵심 패치: `0f0e55eca88e71521197c9bdd580463651e06730`
- Google Play 내부테스트용 AAB 빌드 성공
- GitHub Actions run: `31793522034`
- AAB artifact: `9216491193`
- 테스트 APK artifact: `9216491634`

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

### 0.44.38 안정화

기존에는 `CallScreeningService`가 고객정보를 띄운 뒤 `CallMonitorService`가 초기/지연 `IDLE` 콜백을 받으면 실제 통화 시작 여부와 관계없이 오버레이를 닫을 수 있었다.

현재는:

- 실제 `RINGING/OFFHOOK`을 관찰한 뒤의 `IDLE`에서만 통화 종료 처리를 한다.
- 수신 오버레이는 `CallerOverlayCallStateWatcher`가 `TelecomManager.isInCall()`로 실제 통화 lifecycle을 보조 감시한다.
- 초기 가짜/선행 `IDLE` 때문에 수신정보가 바로 사라지는 경로를 막았다.

## 4. 통화 종료 후 작은 팝업

핵심 파일:

- `CallMonitorService.java`
- `PostCallActivity.java`
- `PostCallActivityLauncher.java`
- `PostCallDeliveryGuard.java`
- `PostCallRecoveryStore.java`
- `PostCallLaunchReceipt.java`
- `CallProcessingLedger.java`

현재 기준:

- 통화 종료 후 전체화면이 아니라 작은 `PostCallActivity` 팝업 1개가 기본이다.
- Android가 백그라운드 Activity 표시를 막으면 고우선 알림 fallback을 사용한다.
- `PostCallRecoveryStore`가 미전달 건을 보존한다.
- `CallProcessingLedger`가 동일 CallLog row의 중복 처리를 막는다.

### 0.44.38 안정화

이전 구조는 `RINGING/OFFHOOK → IDLE` 콜백을 모두 받아야 종료 처리를 시작했다. 제조사/OEM/프로세스 상태 때문에 앞 이벤트를 놓치면 CallLog가 생겨도 팝업이 누락될 수 있었다.

현재는 두 경로를 사용한다.

1. Telephony call state → 실시간 1차 트리거
2. CallLog `ContentObserver` → 2차 복구 트리거

따라서 전화 상태 콜백을 놓쳐도 새 CallLog row가 생성되면 ledger 확인 후 종료 후속처리를 다시 시도한다.

남은 실기기 QA:

- 앱 전면 상태 연속 수신/발신
- 앱 백그라운드
- 화면 잠금
- 장시간 미사용 후 첫 통화
- 부재중/거절/짧은 통화
- 연속 통화에서 중복 팝업 여부

## 5. 고객 화면 UX

현재 기준:

- 고객목록 빠른 액션: `상태 변경` / `문자 보내기`는 텍스트 버튼 유지.
- 삭제만 작은 휴지통 아이콘.
- 삭제 아이콘은 `ImageButton` 중앙정렬 터치영역을 사용한다.
- 고객 삭제는 별도 Activity로 이동하지 않고 현재 화면 위 확인 팝업에서 처리한다.
- 삭제 확인: 취소=회색, 삭제=빨강.
- 고객수정/상세에서 연락처 저장 기능을 제공한다.
- Android 연락처 INSERT 화면에 고객명/전화번호를 채워 연다.

## 6. 문자 기능

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

## 7. 페이지로 문의 → 콜태그

정본 상세 문서: `PAGERO_CUSTOMER_INTEGRATION_KO.md`

현재 주요 동작:

- 같은 Inlet 계정 owner 기준 자동 연결
- 일반 사용자가 webhook/비밀키를 직접 넣지 않는다.
- 페이지로 문의가 콜태그 고객으로 생성/갱신된다.
- 문의 메타데이터의 `answers`, `values`, `pageTitle`, `site`, `campaign`, `source`, `email`, `content`와 동적 필드를 가능한 한 보존한다.
- 문의 전체 내용을 고객 memo 구성에 사용한다.
- 페이지로 자동문자 기본값은 OFF.
- eventId 기준 중복 수신/발송을 방지한다.
- 페이지로 문의 eventId를 일반 단체문자 campaign 상태머신으로 오인하지 않게 분리되어 있다.

## 8. Google Play Billing

정본 상세 문서: `GOOGLE_PLAY_BILLING_SETUP_KO.md`

현재 Play 정기결제 상품은 **2개만** 사용한다.

| Product ID | 가격 | 범위 |
|---|---:|---|
| `call_monthly` | 1,900원/월 | 전화관리 |
| `message_monthly` | 990원/월 | 문자자동화 |

현재 `all_monthly` 통합권은 만들지 않는다.

Billing UI 성능 기준:

- 화면 진입 즉시 Google Play Billing 연결/상품조회를 시작한다.
- 서버 `playBillingAvailable` 응답을 기다린 뒤 BillingClient를 시작하지 않는다.
- Play 상품조회와 서버 entitlement 조회는 서로 독립적으로 실행한다.
- 상품정보가 오면 결제 버튼을 즉시 활성화한다.
- 무한 `결제 준비 중` 금지.
- 실패/타임아웃은 `다시 시도`로 전환한다.
- Billing 연결 끊김 시 재연결한다.

서버 purchase token 검증은 구현되어 있고 실제 `call_monthly` 구매 검증까지 확인됐다.

### 아직 미구현

Google Play RTDN(Pub/Sub) 기반 구독 lifecycle 동기화는 아직 완료되지 않았다.

미구현 범위:

- Pub/Sub topic
- Play notification publisher 권한
- RTDN 설정
- subscriber endpoint
- renewal/cancel/expiry/grace/hold/resume/refund 이벤트 재조회 및 entitlement 갱신

이를 완료하기 전에는 구독 lifecycle 자동 동기화 완료라고 기록하지 않는다.

## 9. 결제 국가 오류 확인 항목

사용자에게 `거주 중인 국가에서는 결제할 수 없습니다`가 뜨면 앱 코드만 보지 않는다.

확인 순서:

1. 테스트 Google Play 계정의 Play 국가가 대한민국인지
2. Google 결제 프로필 국가가 대한민국인지
3. 비공개 테스트 트랙 대상 국가에 대한민국 포함 여부
4. `call_monthly`, `message_monthly` 기본 요금제의 대한민국 판매 가능 여부
5. 현재 Play Store에 선택된 테스트 계정이 올바른지
6. 필요 시 라이선스 테스터 등록 여부

## 10. CI / 릴리스

현재 `Build CallTag Play Internal` workflow는:

- `app/build.gradle`에서 versionName/versionCode 자동 인식
- 현재 Firebase 필수값 확인
- Android API 36 빌드
- 저장된 기존 Play 업로드 키만 사용
- 업로드 키 SHA-256 고정 검증
- signed release AAB 생성
- 테스트 APK 생성

예전처럼 특정 버전 `0.43.0 / code68`을 하드코딩하거나 업로드키가 없다고 새 키를 생성하지 않는다.

## 11. 작업 시 금지사항

- `all_monthly`를 임의로 추가하지 않는다.
- Google Android OAuth Client ID를 `requestIdToken()` server client ID로 넣지 않는다.
- Play 업로드 키를 새로 생성/교체하지 않는다.
- 통화 종료 팝업을 전체화면으로 되돌리지 않는다.
- 고객목록의 상태/문자 버튼을 아이콘만으로 바꾸지 않는다.
- 페이지로 문의 내용을 일부 필드만 남기고 버리지 않는다.
- 사용자에게 서비스계정 private key, keystore, 비밀번호를 요구하지 않는다.
- RTDN이 없는 상태에서 구독 lifecycle 동기화 완료라고 쓰지 않는다.

## 12. 현재 우선순위

1. 0.44.38 통화 전/후 팝업 실기기 반복 QA
2. 결제 화면 실제 계정별 속도/국가 오류 QA
3. 페이지로 문의 자동문자 실사용 QA
4. 고객목록/삭제/연락처 저장 UI 최종 확인
5. Google Play RTDN 구현 여부 결정 및 구현
6. Play 내부테스트 안정화 후 프로덕션 준비
