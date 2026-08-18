# 콜태그 Android 개발자 인수인계

기준 버전: **0.44.43**  
versionCode: **2026081801**  
기준일: **2026-08-18**  
저장소: `pc9839a-lgtm/calltag`  
정본 브랜치: `agent/calltag-v04422-billing-live`  
패키지: `kr.pagero.calltag`  
minSdk: **26**  
compileSdk / targetSdk: **36 / 36**

> 이 문서를 현재 Android 구현의 정본으로 사용한다. 충돌 시 `app/build.gradle`과 실제 코드가 최우선이다. 과거 버전별 HOTFIX/릴리스 문서는 정본으로 사용하지 않는다.

## 1. 현재 릴리스

- 앱 버전: `0.44.43`
- versionCode: `2026081801`
- 앱 코드 기준 UI 수정 HEAD: `b27568283c29d48b400da35e9b7153e4d39bb60c`
- 릴리스 버전 bump: `5415f44693c30ad75b3f26eb11b6c55a536571d8`
- signed AAB 빌드 기준 commit: `32044e79867b1dbcdffb20107281655967a68cd1`
- 임시 릴리스 workflow 정리 후 브랜치 HEAD: `acdbed3c2a9a7548c9f5f4e427b9025d2c0c47be`
- Signed Release run: `32106739436` — 성공
- AAB: `CallTag-v0.44.43-code2026081801.aab`
- AAB SHA-256: `6bbffb0ec122eb4a161b51391dbfc18677cc4698c56088d0b561da6ffa52c680`
- GitHub prerelease tag: `calltag-v0.44.43-code2026081801`

### Play 업로드 키

공개 인증서 지문만 기록한다. 키 파일/비밀번호는 문서에 기록하지 않는다.

- SHA-1: `79:80:FD:C6:4E:BE:DD:2B:80:54:5B:60:87:03:6D:5F:78:05:75:8B`
- SHA-256: `C3:4C:98:88:9B:0C:88:8A:BB:39:94:6C:80:16:96:C2:89:E2:82:6C:10:0F:41:7A:0B:CE:25:A3:92:C4:72:A7`
- 릴리스 CI는 위 SHA-256과 다르면 중단한다.
- 업로드 키가 없다고 새 키를 자동 생성하지 않는다.

## 2. Google 로그인 — 해결 완료

2026-08-14 실제 단말에서 Google 로그인 성공 확인.

- Android OAuth Client ID: `31346298247-26okq7jrsac89q8pucjeuui6jrfofvqn.apps.googleusercontent.com`
  - Android 앱 패키지 + SHA-1 식별용.
  - `requestIdToken()` server client ID로 사용하지 않는다.
- Web/Backend OAuth Client ID: `31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`
  - Android 앱 ID Token audience / backend 검증용.
  - `BuildConfig.GOOGLE_SERVER_CLIENT_ID`는 이 값을 사용한다.

## 3. 통화 전 고객정보 표시

핵심 파일:

- `CallTagScreeningService.java`
- `CallerOverlayManager.java`
- `CallerOverlayCallStateWatcher.java`
- `CallerIdSetupButton.java`

현재 기준:

- Android `ROLE_CALL_SCREENING` 사용.
- `수신 전화 고객정보 표시`가 ON이고 등록 고객이면 고객명/최근 메모 표시.
- 오버레이 표시 실패 시 알림 fallback.
- 역할 권한이 없는데 기능을 켜면 시스템 역할 요청창을 연다.
- 실제 `RINGING/OFFHOOK` 이후의 `IDLE`에서만 통화 종료 처리.
- `TelecomManager.isInCall()`로 lifecycle을 보조 감시한다.

## 4. 통화 종료 후 작은 팝업 — 현재 제품 원칙

핵심 파일:

- `CallMonitorService.java`
- `PostCallActivity.java`
- `PostCallActivityLauncher.java`
- `PostCallOverlayManager.java`
- `CallPopupNotificationManager.java`
- `PostCallRecoveryStore.java`
- `CallProcessingLedger.java`
- `CallRecoveryProcessor.java`
- `CallMonitorRecoveryWorker.java`
- `CallMonitorRecoveryScheduler.java`

제품 원칙:

- 통화 종료 후 **앱 Activity를 자동으로 앞으로 열지 않는다.**
- 기본 전달은 **작은 오버레이 팝업 1개**다.
- 전체화면 + 작은 팝업 동시 노출 금지.
- `PostCallActivity`는 사용자가 fallback 알림을 직접 누른 경우 등 명시적 액션에서만 진입 가능.

현재 전달 구조:

1. Telephony state → 실시간 1차 트리거
2. CallLog `ContentObserver` → 종료 누락 2차 트리거
3. `CallProcessingLedger`로 CallLog ID 중복 검사
4. 고객/할 일/자동문자/후처리를 한 번만 수행
5. `PostCallActivityLauncher`는 자동 Activity 실행을 하지 않음
6. `PostCallOverlayManager` 작은 오버레이 우선
7. 오버레이 불가/실패 시 고우선 알림 fallback
8. 오버레이/알림 모두 불가하면 `PostCallRecoveryStore`에 미전달 상태 유지

### 프로세스/OEM 종료 복구 안전망

- `CallMonitorRecoveryWorker`: WorkManager 15분 periodic 작업
- lookback: 최근 12시간
- recovery cursor + 5분 grace window
- 최대 최근 CallLog 40건 단위 재검사
- `CallProcessingLedger`로 처리 완료 CallLog 재처리 방지
- 누락 row만 `CallRecoveryProcessor.resolveOnce()`로 기존 처리 파이프라인 재사용
- Worker 종료 후 미전달 review가 있으면 최신 1건 재전달 시도
- 앱 프로세스 시작 시 periodic work 재확인
- 재부팅/앱 업데이트 시 periodic work 재등록 + 즉시 1회 recovery enqueue
- foreground service 시작이 OEM 정책으로 막혀도 사용자 통화감지 설정을 임의 OFF하지 않는다.

제한:

- Android 설정에서 사용자가 앱을 명시적으로 **강제 종료(Force stop)** 한 경우는 사용자가 앱을 다시 실행하기 전까지 앱이 스스로 복구할 수 없다.

### 남은 실기기 QA

- 전면/백그라운드/잠금화면에서 연속 수신·발신
- 장시간 미사용 후 첫 통화
- 부재중/거절/1~3초 짧은 통화
- 연속 통화 중복 팝업/중복 자동문자
- 삼성/픽셀/기타 OEM별 오버레이 노출률
- 오버레이 OFF + 알림 ON/OFF 조합
- foreground service 종료 후 15분 recovery
- 재부팅/앱 업데이트 직후 immediate recovery

## 5. 고객 화면 UX

현재 기준:

- 고객목록 빠른 액션: `상태 변경` / `문자 보내기`는 텍스트 버튼.
- 삭제만 작은 휴지통 아이콘.
- 삭제는 현재 화면 위 확인 팝업에서 처리.
- 고객 상세에서 연락처 저장 지원.

### 페이지로 출처 배지 — 0.44.43 수정

핵심 파일:

- `CustomerSourceResolver.java`
- `CustomerSourceBadge.java`
- `CustomerMessagePickerActivity.java`

수정 내용:

- 이전 `CustomerSourceBadge.create()`가 전달받은 label을 무시하고 무조건 `페이지로`를 표시하던 버그 수정.
- 실제 `customer.source`가 `페이지로`, `pagero`, `pagero_lead`, `pagero:*`, `페이지로:*`인 경우에만 페이지로 고객으로 판정.
- 일반 고객은 페이지로 배지를 표시하지 않는다.
- 빈 label은 badge 자체를 `GONE` 처리한다.
- memo에 `pagero` 문구가 있다는 이유만으로 페이지로 유입으로 판정하지 않는다.

## 6. 일정 시간 선택

핵심 파일: `TaskTimeChoiceDialog.java`

- 오전/오후 세그먼트
- 시 1~12 순환 휠
- 분 `00/05/10/.../55` 순환 휠
- 선택 시간 실시간 요약
- `취소` / `이 시간으로 등록`
- 블랙/화이트 공통 리소스 사용

## 7. 캘린더 접기/펼치기

핵심 파일: `CollapsibleConsultationLayout.java`

- 큰 별도 `접기` 버튼 제거.
- `월간 캘린더` 헤더 행 자체를 토글로 사용.
- 작은 chevron으로 상태 표시.
- 월간 달력 본문만 숨김/표시.
- 선택 날짜/일정 추가/일정 목록은 유지.
- 마지막 접힘 상태 저장.

## 8. 블랙/화이트 테마 — 0.44.43 상태

핵심 파일:

- `CallTagThemeManager.java`
- `MoreSettingsHubView.java`
- `res/values/colors.xml`
- `res/values-night/colors.xml`
- `res/values/themes.xml`
- `res/values-night/themes.xml`
- `res/drawable/bg_secondary_button.xml`

진입 경로:

`더보기 → 앱 관리 → 테마`

정책:

- `블랙` / `화이트` 2개만 제공.
- 기본값 블랙.
- 화이트 = Light Material parent, 블랙 = Dark Material parent.
- 상태바/내비게이션바 아이콘 대비도 테마별 적용.
- 화면별 색 하드코딩을 늘리지 않고 공통 resource를 사용한다.

0.44.43 추가 수정:

- 화이트에서 `전체 상태`, `전체 기간`, `상태 변경`, 뒤로가기/보조버튼이 검은 박스로 남던 원인을 수정.
- `bg_secondary_button.xml`의 다크 색상 하드코딩을 제거하고 테마별 color resource를 사용하도록 변경.
- 같은 secondary button drawable을 쓰는 화면에도 함께 적용된다.

아직 남은 것:

- 실기기에서 전 화면 화이트 회귀 QA.
- 입력창/스위치/다이얼로그/하단탭/상단바/커스텀 drawable의 잔여 다크 하드코딩 점검.

## 9. 문자 기능

문자 메인 우선 메뉴:

1. `고객선택후 문자`
2. `통화후 자동문자`
3. `페이지로 문의접수문자`

관리 기능:

- 문자 템플릿
- 그룹·단체문자
- 발송 내역

### 템플릿 선택 UX — 0.44.43 수정

핵심 파일: `MessageTemplateLibraryActivity.java`

- 템플릿 선택 카드에 **`수정` 버튼을 직접 노출**.
- 카드 본문을 누르면 기존대로 템플릿 선택.
- `수정` 버튼은 해당 템플릿 편집 화면으로 진입.
- 카드 간격 `7dp → 14dp`로 확대.
- 카드 내부 여백/본문 간격도 확대.

남은 QA:

- 선택모드에서 `수정` 터치와 카드 선택 터치가 충돌하지 않는지 실기기 확인.
- 편집 후 목록으로 돌아왔을 때 즉시 갱신 확인.

## 10. 페이지로 문의 → 콜태그

정본 상세 문서: `PAGERO_CUSTOMER_INTEGRATION_KO.md`

현재 주요 동작:

- 같은 Inlet 계정 owner 기준 자동 연결.
- 페이지로 문의가 콜태그 고객으로 생성/갱신.
- `answers`, `values`, `pageTitle`, `site`, `campaign`, `source`, `email`, `content`와 동적 필드를 가능한 한 보존.
- 문의 전체 내용을 고객 memo 구성에 사용.
- 페이지로 자동문자 기본값 OFF.
- eventId 기준 중복 수신/발송 방지.

## 11. Google Play Billing

정본 상세 문서: `GOOGLE_PLAY_BILLING_SETUP_KO.md`

현재 Play 정기결제 상품:

| Product ID | 가격 | 범위 |
|---|---:|---|
| `call_monthly` | 1,900원/월 | 전화관리 |
| `message_monthly` | 990원/월 | 문자자동화 |

`all_monthly` 통합권은 현재 만들지 않는다.

현재 구현:

- Play Billing 연결/상품조회
- 서버 entitlement 조회 독립 실행
- ProductDetails 수신 후 결제 버튼 활성화
- purchase token 서버 검증
- 다른 CALLTAG 계정의 purchase token 재귀속 방지
- 계정 전환 시 entitlement/referral/pagero cache 정리

아직 미구현:

- Google Play RTDN(Pub/Sub) 기반 renewal/cancel/expiry/grace/hold/resume/refund lifecycle 자동 동기화.

## 12. 권한 UX — 남은 작업

현재 일부 화면은 권한이 없을 때 단순 안내로 끝날 수 있다. 제품 원칙은 기능 버튼을 눌렀을 때 필요한 시스템 권한 요청/설정 화면으로 바로 연결하는 것이다.

남은 범위:

- 전화 상태
- 통화기록
- 알림
- 오버레이
- 연락처 저장
- 권한 거부 후 재시도 흐름

## 13. CI / 릴리스

현재 기준:

- API 36.
- `app/build.gradle`의 versionName/versionCode가 배포 기준.
- 기존 Play 업로드 키만 사용.
- 업로드 키 SHA-256 고정 검증.
- Play 업로드 전 versionCode 증가 필수.
- 0.44.43 signed AAB 생성/서명 검증 성공.
- Signed Release run `32106739436` 성공.
- AAB `CallTag-v0.44.43-code2026081801.aab`.
- SHA-256 `6bbffb0ec122eb4a161b51391dbfc18677cc4698c56088d0b561da6ffa52c680`.

## 14. 작업 시 금지사항

- 통화 종료 후 앱 Activity를 자동으로 앞으로 띄우지 않는다.
- 통화 종료 팝업을 전체화면으로 되돌리지 않는다.
- 전체화면과 작은 팝업을 동시에 띄우지 않는다.
- Worker에서 처리 완료 CallLog를 다시 자동문자/고객처리하지 않는다.
- foreground service 시작 실패만으로 통화감지 설정을 OFF하지 않는다.
- 일반 고객에 페이지로 배지를 표시하지 않는다.
- 페이지로 출처를 memo 문자열로 추정하지 않는다.
- `all_monthly`를 임의 추가하지 않는다.
- Android OAuth Client를 ID Token server client ID로 사용하지 않는다.
- Play 업로드 키를 새로 생성/교체하지 않는다.
- RTDN이 없는 상태에서 구독 lifecycle 동기화 완료라고 쓰지 않는다.
- 블랙/화이트 개별 화면 색상을 임의 하드코딩하지 않는다.

## 15. 현재 우선순위

1. 0.44.43 화이트 테마 전 화면 실기기 회귀 QA 및 잔여 하드코딩 제거
2. 통화 종료 작은 오버레이: 앱 Activity 미실행 + OEM별 반복 QA
3. 15분 WorkManager recovery 실기기 검증
4. 권한 없음 UX 전체 통일
5. 고객 출처 배지 실제 데이터 회귀 QA
6. 템플릿 선택/수정 UX 회귀 QA
7. 결제 실제 계정/국가/복원/이용 중 표시 QA
8. Google Play RTDN 구현
9. 페이지로 문의 전체 내용/자동문자 실사용 QA
10. 출시 전 전체 회귀 및 새 versionCode signed AAB
