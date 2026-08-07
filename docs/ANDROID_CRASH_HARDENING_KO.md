# CallTag Android 크래시 재발 방지 기준

기준 버전: 0.43.7 / versionCode 75

## P0 안정화 원칙 10개

1. **고객 화면 진입 단일화**
   - 수신 오버레이, 수신 알림, 홈 고객 카드, 홈 할 일 카드의 고객 수정 진입은 `CustomerLaunchRouter`를 사용한다.
   - 고객 식별은 customerId 우선, 전화번호 fallback 순서로 처리한다.

2. **새 화면 실행 전에 기존 UI를 제거하지 않는다**
   - 수신 오버레이는 고객 수정 Activity 시작 요청이 성공한 뒤에만 닫는다.
   - 실패하면 기존 오버레이를 유지한다.

3. **홈의 다단계 작업은 MainActivity 밖에서 수행한다**
   - 고객 추가: `CustomerAddActivity`
   - 할 일 등록: `HomeTaskEditorActivity`
   - 고객 수정: `CustomerQuickEditActivity`
   - MainActivity는 조회와 진입을 중심으로 유지한다.

4. **화면 전환 목적으로 `performClick()`을 사용하지 않는다**
   - 메인 탭 전환은 `MainSectionRouter`를 사용한다.
   - 저장 완료 후 탭 클릭을 강제로 발생시켜 재렌더링하지 않는다.

5. **외부/비동기 고객 참조에는 ID와 전화번호를 같이 전달한다**
   - 오래된 PendingIntent 또는 동기화 이후 ID가 달라도 전화번호로 복구한다.

6. **크래시/진입 breadcrumb를 남긴다**
   - `CrashTelemetryStore`가 최근 60개 이벤트를 로컬에 보관한다.
   - post-call 실행 시도, 수신 알림, 수신 오버레이, 고객 quick edit, 홈 할 일 등록의 성공/실패 경로를 기록한다.
   - uncaught exception 직전 클래스/스레드 정보도 기록한다.

7. **DB 쓰기와 MainActivity 목록 렌더링을 분리한다**
   - 고객/할 일 저장은 별도 Activity에서 수행하고 완료 후 종료한다.
   - MainActivity 렌더링 callback 안에서 저장 후 synthetic navigation을 발생시키지 않는다.

8. **중복 탭/중복 Activity 실행을 차단한다**
   - `UiLaunchGuard`를 고객추가, 할일등록, 고객수정, 주요 홈 진입에 적용한다.
   - 통화 종료 팝업은 callLogId 기반 기존 중복 방지와 launch receipt를 함께 사용한다.

9. **P0 회귀 테스트 3개를 고정한다**
   - 팝업 고객 ID 실패 → 전화번호 fallback → `CustomerQuickEditActivity` 표시
   - 홈 `+ 할 일 등록` → `HomeTaskEditorActivity` 표시
   - 홈 고객 카드 → `CustomerQuickEditActivity` 표시
   - 테스트 파일: `app/src/androidTest/java/kr/pagero/calltag/CrashRegressionTest.java`

10. **CI에서 실제 Android 에뮬레이터 UI 테스트를 수행한다**
    - Debug APK assemble 성공 후 API 35 x86_64 에뮬레이터를 실행한다.
    - `:app:connectedDebugAndroidTest`가 실패하면 빌드를 실패 처리한다.
    - instrumentation 결과를 Actions artifact로 남긴다.

## 배포 차단 조건

아래 중 하나라도 실패하면 테스트 브랜치 병합 및 Play 배포를 진행하지 않는다.

- `:app:assembleDebug`
- P0 `CrashRegressionTest` 3건
- 앱 버전 코드 중복 여부
- 서명 AAB 빌드가 필요한 배포에서 upload key 확인

## 수동 최종 확인

실기기에서는 아래 순서만 추가 확인한다.

1. 실제 통화 종료 → 팝업 표시 → 고객 수정 화면 진입 → 저장
2. 홈 → 할 일 등록 → 고객/종류/날짜/시간 선택 → 저장 → 홈 복귀
3. 홈 고객 카드 → 고객명/상태/메모 수정 → 저장 → 홈 복귀
4. 같은 버튼 빠르게 연속 탭 → Activity가 중복으로 겹치지 않는지 확인
