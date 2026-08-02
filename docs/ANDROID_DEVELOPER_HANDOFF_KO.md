# 콜태그 Android 개발자 인수인계

기준 버전: **0.38.2**  
versionCode: **46**  
기준일: **2026-08-02**  
저장소: `pc9839a-lgtm/calltag`  
개발 브랜치: `agent/calltag-foundation`  
개발 PR: Draft PR `#1`  
패키지: `kr.pagero.calltag`  
최소 Android: API 26  
Target/Compile SDK: 35

> 이 문서는 다음 Android 개발자가 현재 구현을 파악하고 안전하게 이어서 작업하기 위한 정본이다. 기획 문서보다 실제 코드와 `app/build.gradle`을 우선한다. 빌드 성공과 실제 휴대전화 동작 성공은 반드시 구분한다.

---

## 1. 작업 시작 전 반드시 확인할 것

1. 작업 저장소는 `pc9839a-lgtm/calltag` 하나다.
2. 작업 브랜치는 `agent/calltag-foundation`이다.
3. 사용자 명시 지시 전 `main`에 병합하지 않는다.
4. PR `#1`은 Draft 상태를 유지한다.
5. 최신 버전은 `app/build.gradle`에서 다시 확인한다.
6. 기존 고객·문자·일정·캠페인 데이터를 초기화하지 않는다.
7. DB 스키마 변경 시 기존 데이터를 보존하는 마이그레이션을 작성한다.
8. 기능 구현 후 임시 검증 브랜치와 Draft PR로 Android 빌드를 확인하고, 검증 PR은 병합하지 않고 닫는다.
9. GitHub Actions 성공을 실제 단말 검증 완료로 기록하지 않는다.

---

## 2. 제품 구조

콜태그는 전화 전후의 고객관리를 한 앱에서 처리한다.

```text
전화 수신/발신
→ 고객정보 확인
→ 통화 종료 정리
→ 고객 상태·메모·오늘 할 일 저장
→ 고객별 문자 또는 후속 예약
→ 캘린더·통계·단체문자로 후속 관리
```

### 하단 내비게이션

왼쪽부터 다음 5개다.

```text
고객 / 캘린더 / 홈 / 통계 / 더보기
```

- `홈`은 중앙에 있지만 과도한 원형 FAB가 아니다.
- 다른 메뉴보다 아이콘과 라벨만 한 단계 강조한다.
- `MainActivity` 외의 하위 Activity는 일반 Android 뒤로가기를 사용한다.
- `MainActivity`에서 다른 탭의 뒤로가기 → 홈 이동.
- 홈에서 다시 뒤로가기 → `앱을 종료할까요?` 확인창.

---

## 3. 현재 0.38.2 핵심 변경

### 일정 고객 선택

- 일정 추가 시 고객 전체를 화면 높이만큼 나열하지 않는다.
- 옵션이 9개 이상이면 `ActionChoiceDialog`에 검색창이 자동 표시된다.
- 고객명·전화번호·부제목 검색을 지원한다.
- 검색 결과 수를 표시한다.
- 목록은 고정 최대 높이 안에서 내부 스크롤한다.
- 이 기능은 공통 선택창에 적용되므로 고객 외의 대량 옵션 선택에도 영향을 줄 수 있다.

### 뒤로가기 종료 방지

- `MainActivityExitGuard`가 `CallTagApplication`의 lifecycle callback에서 `MainActivity`에 설치된다.
- Android 13 이상은 `OnBackInvokedDispatcher`를 사용한다.
- Android 12 이하는 `MainActivity.onBackPressed()` 경로를 사용한다.
- 다른 탭에서는 홈으로 이동하고 홈에서만 종료 확인창을 띄운다.

### 통계 고도화

상단 기간:

```text
오늘 / 7일 / 30일 / 선택
```

- 직접 선택은 시작일과 종료일을 같은 설정창에서 확인한 뒤 적용한다.
- 종료일이 시작일보다 빠르면 차단한다.
- 미래 날짜를 선택하지 못하게 한다.
- 직접 조회 최대 범위는 365일이다.
- 7일·30일 또는 직접 선택 2~30일 범위에서는 일별 추이 차트를 표시한다.
- 차트 시리즈:
  - 파란색: 일별 통화 건수
  - 초록색: 일별 페이지로 유입 고객 수
- 차트는 `StatsTrendChartView`가 Canvas로 직접 그린다.
- 차트는 현재 표시용이며 확대·드래그·툴팁은 없다.

통계 화면 위계:

1. 전체 통화 대표 숫자
2. 연락 고객·신규 고객·완료한 일
3. 일별 추이 차트
4. 통화유형: 수신·발신·부재중·거절
5. 페이지로: 유입 고객·연락률·연락 완료·미연락
6. 처리할 일: 오늘 할 일·기한 지남·확인할 통화

### 검증 결과

- Workflow: `Validate CallTag Android`
- Run ID: `30731475511`
- Job ID: `91452540504`
- Android 리소스 처리: 성공
- Java 컴파일: 성공
- Debug APK 패키징: 성공
- APK 업로드: 성공
- Artifact ID: `8828114600`
- Artifact digest: `sha256:92dcc96bfb35b2d2fe28f21d69bd4e3c967273b4c35f55256d0849010b810499`
- 실제 APK SHA-256: `d13ffe43d261be6ca3ff7af73d00830bbc426aff44ebc587d42b8d7f5c4876d5`
- 실제 APK 크기: `2,657,934 bytes`
- 임시 검증 PR `#18`: 병합 없이 종료

---

## 4. 주요 화면과 코드 위치

### 앱 진입·전역 lifecycle

| 역할 | 주요 파일 |
|---|---|
| Application 초기화·복구·Activity lifecycle | `CallTagApplication.java` |
| 로그인·앱 진입 분기 | `AuthGateActivity.java`, `LoginActivity.java` |
| 초기 설정 준비 여부 | `SetupRequirements.java` |
| 메인 5개 탭 | `MainActivity.java`, `activity_main.xml` |
| 메인 종료 확인 | `MainActivityExitGuard.java` |

`SetupRequirements.isReady()`는 현재 강제 중간 설정 화면으로 보내지 않도록 구성돼 있다. 기능별 권한은 해당 기능 화면에서 요청한다.

### 홈

| 역할 | 주요 파일 |
|---|---|
| 홈 레이아웃 | `section_today.xml` |
| 오늘 할 일·빠른 메뉴 렌더링 | `MainActivity.java` |
| 홈 내비게이션 강조 | `HomeNavItemTextView.java` |
| 오늘 할 일 페이지로 오표시 방지 | `TodayTaskSourceCleanerView.java` |

홈 빠른 메뉴의 기본 방향:

```text
고객 추가 / 고객 목록 / 문자
```

오늘 할 일 카드에는 일정 종류, 고객명, 예정시간, 핵심 실행 버튼만 보여준다. 페이지로 여부는 고객명으로 추정하면 안 된다.

### 고객

| 역할 | 주요 파일 |
|---|---|
| 고객 탭 레이아웃 | `section_customers.xml` |
| 고객 카드 후처리·문자 버튼 | `CustomerListView.java` |
| 고객 상세 | `CustomerDetailActivity.java` |
| 고객 추가 | `CustomerAddActivity.java` |
| 고객 유입 판별 | `CustomerSourceResolver.java` |
| 페이지로 배지 | `CustomerSourceBadge.java` |
| 고객 인사이트·최근 메모 | `CustomerInsightResolver.java` |

고객 카드 정보 순서:

```text
고객명·상태
연락처 + 페이지로 배지(해당 고객만)
메모 한 줄
최근 연락
문자 / 상태 변경
```

#### 페이지로 판별 규칙

- `customer.source`에 `pagero`, `페이지로`, `landing`, `lead_form`이 포함된 경우만 페이지로로 본다.
- 고객명, 일정명, 전화 통화 여부로 페이지로 고객을 추정하지 않는다.
- 직접 등록 고객과 전화 유입 고객은 별도 유입 배지를 화면에 표시하지 않는다.

### 캘린더·일정

| 역할 | 주요 파일 |
|---|---|
| 캘린더 탭 | `section_consultations.xml`, `MainActivity.java` |
| 일정 종류 저장 | `TaskTypeStore.java`, `TaskTypeSettingsActivity.java` |
| 외부 캘린더 공유 | `CalendarShareActivity.java` |
| 일정 DB | `CallTagDbHelper.java`, `FollowUpTask.java` |

일정 추가 흐름:

```text
날짜 선택
→ + 일정 추가
→ 고객 검색·선택
→ 일정 종류 선택
→ 시간 선택
→ FollowUpTask 저장
```

외부 캘린더 공유:

- Android `CalendarContract.Events.CONTENT_URI`의 INSERT Intent를 사용한다.
- Google 캘린더·삼성 캘린더 등 설치된 앱에서 저장 계정을 사용자가 선택한다.
- 고객명·전화번호·일정 종류·메모·시간을 채운다.
- 현재는 콜태그 → 외부 캘린더 단방향 등록이다.
- 외부 캘린더 수정·삭제가 콜태그 일정에 자동 반영되지 않는다.
- 콜태그 일정 수정·삭제도 기존 외부 캘린더 이벤트를 자동 변경하지 않는다.

### 통계

| 역할 | 주요 파일 |
|---|---|
| 통계 탭 레이아웃 | `section_stats.xml` |
| 통계 집계·필터·화면 구성 | `CustomerStatsView.java` |
| 추이 차트 | `StatsTrendChartView.java` |
| 통계 탭 동작 | `StatsNavItemTextView.java`, `StatsSectionScrollView.java` |

집계 데이터:

- 통화: `InteractionRecord.type`의 `INCOMING_CALL`, `OUTGOING_CALL`, `MISSED_CALL`, `REJECTED_CALL`
- 완료한 일: `TASK_COMPLETE`, `TASK_AUTO_COMPLETE`
- 신규 고객: `Customer.firstContactAt`
- 페이지로 유입: 생성일이 기간 안이고 `CustomerSourceResolver.isPagero(customer)`가 true
- 페이지로 연락 완료: 선택 기간의 통화 interaction에 해당 고객 ID가 포함됨

주의:

- `listRecentInteractions(5000)` 상한이 있다. 장기 운영·대량 데이터에서 통계 정확도를 유지하려면 기간 기반 DB 쿼리로 바꾸는 것이 다음 개선점이다.
- 직접 선택은 최대 365일이지만 차트는 최대 30일까지만 표시한다. 31일 이상은 숫자 집계만 표시한다.

### 통화 수신·종료

| 역할 | 주요 파일 |
|---|---|
| 수신 고객정보 | `CallTagScreeningService.java`, `CallerInfoActivity.java` |
| 수신 역할 요청 | `CallerIdSetupButton.java`, `CallerIdSetupActivity.java` |
| 통화 감지 | `CallMonitorService.java` |
| 통화 종료 정리 | `PostCallActivity.java`, `activity_post_call.xml` |
| 통화 종료 화면 호출·알림 | `CallPopupNotificationManager.java` |
| 미정리 통화 | `PendingCallStore.java` |

통화 종료 처리:

1. 통화 기록을 확인한다.
2. `PostCallActivity` 직접 실행을 시도한다.
3. Android 14 이상에서는 백그라운드 Activity 실행 허용 옵션이 있는 PendingIntent 경로도 사용한다.
4. 제조사·OS 정책이 직접 실행을 막으면 전체 화면 알림으로 재진입한다.

중요 제한:

- Android와 제조사 정책상 백그라운드 Activity 실행을 100% 강제할 수 없다.
- 알림 권한, 전체 화면 알림 권한, 배터리 최적화, 잠금화면 설정에 따라 다르다.
- 코드 빌드 성공만으로 큰 화면이 실제 단말에서 떴다고 기록하지 않는다.

실기기에서 반드시 확인:

- 화면 켜짐/꺼짐
- 앱 foreground/background/종료 상태
- 삼성·픽셀 등 제조사별 동작
- Android 13·14·15
- 알림 채널 중요도 HIGH
- 전체 화면 알림 허용 설정

### 문자 작성·템플릿

| 역할 | 주요 파일 |
|---|---|
| 고객 선택 문자 시작 | `CustomerMessagePickerActivity.java` |
| 문자 작성·예약 | `ManualMessageActivity.java` |
| 문자 작성 UX 후처리 | `ManualMessageUxEnhancer.java` |
| 템플릿 목록 | `MessageTemplateLibraryActivity.java` |
| 템플릿 편집 | `MessageTemplateEditorActivity.java` |
| 템플릿 저장 | `MessageTemplateStore.java` |
| 중복 템플릿 정리 | `MessageTemplateCleanup.java` |
| 이미지 저장 | `MessageAttachmentStore.java` |
| MMS 작성 | `MmsComposeActivity.java` |
| 발송 내역 | `MessageHistoryActivity.java` |

현재 문자 작성 화면 원칙:

- 고객과 연결된 경우 전화번호 입력칸을 다시 보여주지 않는다.
- 고객명·전화번호를 상단에 표시한다.
- 템플릿 선택은 한 줄 compact 설정이다.
- 선택한 템플릿명과 수정 버튼을 표시한다.
- 이미지 첨부는 이미지가 있을 때만 미리보기·삭제를 보인다.
- 발송·후속 예약 핵심 행동은 하단에 고정한다.

템플릿 원칙:

- 사용자가 입력하는 필드는 이름·내용·이미지다.
- 내부 purpose 값은 사용자에게 노출하지 않는다.
- `수신`, `발신`, `후속`, `일반` 같은 내부 구분을 템플릿명 옆에 나열하지 않는다.
- 별표 즐겨찾기 UI를 사용하지 않는다.
- 기본 템플릿은 `기본` 배지로 표시한다.
- 이미지 템플릿은 자동 SMS 발송 기본값으로 지정하지 않는다.
- 이미지 문자는 시스템 메시지 앱을 열고 사용자가 최종 전송한다.

### 자동문자·문자 허용

| 역할 | 주요 파일 |
|---|---|
| 자동문자 설정 | `MessageAutomationSettingsActivity.java`, `MessageAutomationStore.java` |
| 고객별 문자 허용/차단 | `CustomerMessagePermissionStore.java` 및 고객 상세 관련 코드 |
| 발송 제외 | `MessageExclusionActivity.java` |
| 중복발송 방지 | 문자 발송 직전 검사 코드·Store |

자동문자 화면에는 다음 3개만 주요 시점으로 보인다.

```text
통화 후 / 부재중 / 후속 예약
```

공통 발송 설정에 업무시간·중복 방지·회선·후속 시점을 묶는다.

고객별 문자 설정은 사용자 화면에서 `허용 / 비허용`만 제공한다. 복잡한 상태나 내부 코드를 노출하지 않는다.

### 그룹·단체문자

| 역할 | 주요 파일 |
|---|---|
| 그룹 목록·편집 | `MessageGroupActivity.java`, `MessageGroupStore.java` |
| 그룹·단체문자 허브 | `GroupCampaignHubActivity.java` |
| 캠페인 목록 | `CampaignListActivity.java` |
| 캠페인 작성 | `CampaignComposerActivity.java` |
| 캠페인 상세 | `CampaignDetailActivity.java` |
| 캠페인 저장·실행 | 캠페인 Store·Runner·Receiver 관련 클래스 |

수동그룹 편집:

- 고객명·전화번호·상태 검색
- 검색 결과 기준 전체 선택
- 검색 결과 기준 전체 해제
- 선택 고객 수 표시

현재 남은 핵심 작업은 캠페인 수신자 관리다.

- 수신자 검색
- 상태 필터
- 실패 사유 필터
- 선택 모드
- 선택 재시도
- 선택 취소
- 대량 목록 성능

### 더보기·계정·백업·진단

| 역할 | 주요 파일 |
|---|---|
| 더보기 | `section_more.xml`, `MoreSettingsHubView.java` |
| 계정 | `AccountActivity.java`, `activity_account.xml` |
| 진단 | `DiagnosticActivity.java` |
| 백업·복원 | `BackupRestoreActivity.java`, `CallTagBackupManager.java` |

더보기 메뉴는 텍스트만 떠 있는 영역이 아니라 전체 행이 눌리는 버튼이어야 한다. 메뉴는 다음 범주로 묶는다.

```text
문자 / 고객·일정 / 앱·계정
```

백업 형식은 `.ctbackup`이며 앱 복구 목적이다. CSV·XLSX·CRM 이전용 데이터 내보내기와 합치지 않는다.

---

## 5. 데이터 계층과 변경 원칙

### 주요 데이터 객체

- `Customer`
- `FollowUpTask`
- `InteractionRecord`
- `CallRecord`
- 문자 작업·캠페인·수신자 관련 모델

### 주요 저장소

- `CallTagDbHelper`: 고객·상태·interaction·일정 중심 SQLite
- `MessageTemplateStore`: 문자 템플릿
- `MessageAutomationStore`: 자동문자 설정
- `MessageAttachmentStore`: 앱 전용 이미지
- `PendingCallStore`: 통화 종료 후 미정리 통화
- 그룹·캠페인 Store 클래스

### DB 변경 규칙

- 데이터베이스 삭제·재생성으로 마이그레이션하지 않는다.
- 기존 ID와 연결 관계를 보존한다.
- 고객 → interaction → 일정 → 문자 → 캠페인 수신자의 참조를 확인한다.
- 마이그레이션 후 `PRAGMA quick_check`와 정합성 복구 흐름을 확인한다.
- 백업·복원 호환성도 같이 검토한다.

---

## 6. 절대 변경하면 안 되는 발송 안전 규칙

1. 발송 직전 고객별 문자 허용 여부를 다시 확인한다.
2. 발송 제외 번호를 다시 확인한다.
3. 중복발송 방지 시간을 다시 확인한다.
4. 선택 SIM과 현재 활성 SIM 상태를 다시 확인한다.
5. 캠페인 일시정지·취소 상태를 다시 확인한다.
6. 불명확한 `SENDING` 작업은 자동 재발송하지 않는다.
7. 일시정지 캠페인을 앱 시작·재부팅 시 자동 재개하지 않는다.
8. 누락 작업을 추측해 새로 만들지 않는다.
9. 고아 작업을 자동 발송하지 않는다.
10. 이미지 문자는 시스템 메시지 앱에서 사용자가 최종 전송한다.
11. 복구는 상태를 복원하는 작업이지 발송을 새로 시작하는 작업이 아니다.

---

## 7. UX/UI 규칙

정본: `docs/DESIGN_SYSTEM_KO.md`

핵심 규칙:

- 주요 색상은 파란색 1개와 무채색 중심.
- 위험·경고·성공은 의미가 있을 때만 제한적으로 사용.
- 앱바 높이 56dp.
- 화면 좌우 여백 16dp.
- 화면 제목 21~22sp.
- 버튼 기본 높이 48~52dp.
- 주요 CTA는 한 화면에 1개를 우선한다.
- 카드 안에 카드와 버튼을 반복해서 중첩하지 않는다.
- 텍스트를 버튼처럼 보이게 두지 않는다. 전체 행에 클릭 배경·터치 영역·화살표를 제공한다.
- 긴 고객명·템플릿명·메모는 말줄임 처리한다.
- 360dp 폭과 큰 글자 설정에서 깨지지 않아야 한다.
- 고객·템플릿·그룹이 많아지는 화면은 검색과 내부 스크롤을 기본으로 고려한다.

### 호환 ID 주의

일부 레이아웃에는 이전 `MainActivity` 바인딩과의 호환을 위해 숨김 View ID가 남아 있을 수 있다. 화면에서 안 보인다는 이유로 제거하면 `findViewById()` 이후 NPE가 날 수 있다.

리팩터링 전 반드시 다음을 검색한다.

```text
customerListTab
customerStatsTab
customerStatsPanel
customerStatsContent
stageSettingsButton
moreMenuList
```

ID를 제거하려면 `MainActivity`의 바인딩과 동작을 먼저 함께 제거한다.

---

## 8. 빌드와 검증 절차

### 로컬 기준

```bash
gradle :app:assembleDebug --stacktrace
```

- JDK 17
- Gradle 8.9
- compileSdk 35

### GitHub Actions 검증 방식

1. `agent/calltag-foundation` 최신 HEAD에서 임시 브랜치를 생성한다.
2. 임시 Draft PR을 `agent/android-ci-base` 대상으로 연다.
3. Workflow `Validate CallTag Android`를 기다린다.
4. Java 컴파일·APK 패키징·아티팩트 업로드를 확인한다.
5. APK를 내려받아 실제 버전과 SHA-256을 확인한다.
6. 임시 PR은 병합하지 않고 닫는다.
7. PR `#1`과 `main`은 건드리지 않는다.

주의: 검증 Workflow 아티팩트 이름이 과거 버전명으로 고정돼 표시될 수 있다. 실제 버전은 APK의 `versionName/versionCode`와 `app/build.gradle`로 확인한다.

---

## 9. 실제 단말 필수 검수

### 공통

- Android 8~15
- 360dp 폭
- 큰 글자·화면 확대
- 다크모드 영향
- 키보드가 열린 상태
- 긴 고객명·전화번호·메모·템플릿명
- 고객 500명 이상

### 뒤로가기

- 각 메인 탭에서 뒤로가기 → 홈
- 홈에서 뒤로가기 → 종료 확인
- Android 13 이상 제스처 뒤로가기
- 확인창 중복 표시 여부
- 하위 Activity에서는 일반 뒤로가기 유지

### 일정

- 고객 8명 이하 선택창
- 고객 9명 이상 검색형 선택창
- 고객 500명 검색 성능
- 한글 고객명·숫자 전화번호 검색
- 일정 저장 후 캘린더 표시
- Google 캘린더·삼성 캘린더 공유

### 통계

- 데이터 없는 기간
- 오늘
- 최근 7일
- 최근 30일
- 직접 선택 2일·30일·31일·365일
- 시작일 > 종료일 차단
- 미래 날짜 차단
- 통화 차트와 페이지로 차트 값 비교
- interaction 5,000건 이상에서 정확도 확인

### 통화 종료 큰 화면

- 수신·발신·부재중·거절
- 앱 foreground/background/종료
- 화면 잠금 상태
- 알림 권한 허용/거부
- 전체 화면 알림 허용/거부
- 제조사 배터리 최적화 켜짐/꺼짐

### 문자·캠페인

- 단문·장문·분할 문자
- 예약 발송
- 중복 방지
- 허용/비허용
- 발송 제외
- SIM 교체
- 일시정지·재개·취소
- 재부팅·앱 업데이트 복구

---

## 10. 다음 개발 우선순위

### 1순위 — 0.38.2 실기기 QA

- 일정 고객 검색형 선택창
- Android 13+ 뒤로가기 종료 확인
- 통계 차트 값과 날짜 범위 선택
- 통화 종료 큰 화면 제조사별 동작

### 2순위 — 캠페인 수신자 관리

- 검색
- 상태 필터
- 실패 사유 필터
- 선택 재시도
- 선택 취소
- 대량 목록 성능

### 3순위 — 캠페인 작성 최종 확인

- 실제 수신자 수
- 제외·중복 예상 수
- 변수 치환 샘플
- 장문 분할 예상
- 선택 SIM
- 중복 시작 방지

### 4순위 — 결제·구독

- 실제 Play Billing
- 영수증 검증
- 만료·환불·복원
- 오프라인 유예
- 운영자·일반 계정 권한

### 5순위 — 출시 QA

- 릴리스 서명
- AAB
- Play Console 권한 설명
- 개인정보처리방침 일치
- Crash·ANR

---

## 11. 다음 개발자가 첫날 할 일

1. `agent/calltag-foundation` checkout.
2. `app/build.gradle`에서 `0.38.2 / 46` 확인.
3. `README.md`, 이 문서, `DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`, `DESIGN_SYSTEM_KO.md` 순서로 읽기.
4. Debug APK 빌드.
5. 실제 기기에서 일정 고객 검색·뒤로가기·통계 차트·통화 종료 큰 화면 확인.
6. 문제를 재현한 뒤 관련 파일만 최소 범위로 수정.
7. 데이터·발송 안전 규칙을 건드리는 변경은 별도 검토.
8. 임시 검증 PR로 빌드 후 병합 없이 종료.
9. 패치 종료 시 버전·검증 Run·실기기 확인 여부·남은 패치를 문서에 업데이트.
