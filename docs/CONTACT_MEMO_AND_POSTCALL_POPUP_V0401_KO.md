# 콜태그 v0.40.1 연락처 메모·통화 종료 팝업 복구

## 사용자 기준 동작

### 전화 수신 전 연락처 표시

콜태그 고객의 최근 메모를 시스템 연락처 표시 이름에 다음 형식으로 반영한다.

```text
고객명 · 최근 메모
```

기본 전화 앱은 이 연락처 표시 이름을 읽으므로 별도 콜태그 전체 화면을 열지 않아도 전화 화면에서 고객 메모를 볼 수 있다.

### 기능 해제와 앱 삭제

- 원본 Google·삼성·휴대전화 연락처 RawContact는 수정하지 않는다.
- 콜태그 전용 계정 `kr.pagero.calltag.contacts / 콜태그 메모`에 표시용 RawContact를 만든다.
- 기능 해제 시 콜태그 계정 RawContact와 과거 로컬 방식의 `calltag:*` RawContact를 제거한다.
- 앱 삭제 시 Android AccountManager에서 콜태그 인증 계정이 제거되고 그 계정에 속한 연락처 데이터가 함께 정리되는 구조를 사용한다.
- 앱 삭제 후에도 원본 연락처는 남아야 한다.

## 구현 파일

- `CallTagContactsAccount.java`
- `CallTagAccountAuthenticator.java`
- `CallTagAuthenticatorService.java`
- `CallTagContactSyncAdapter.java`
- `CallTagContactSyncService.java`
- `ContactNameSyncManager.java`
- `res/xml/calltag_account_authenticator.xml`
- `res/xml/calltag_contact_sync_adapter.xml`

## 레거시 마이그레이션

과거 `ACCOUNT_TYPE=null`로 만든 `SOURCE_ID LIKE 'calltag:%'` 로컬 RawContact는 새 동기화 시작 시 제거한다. 이후 고객별 별칭을 콜태그 전용 계정에 다시 만든다.

## 통화 종료 동작

통화 종료 후 `PostCallActivity`는 전체 앱 화면으로 보이면 안 된다.

- `Theme.CallTag.PostCallPopup` 사용
- 화면 너비에서 좌우 12dp, 높이에서 상하 28dp를 제외한 대형 팝업
- 배경 화면을 dim 처리하되 기존 전화 화면·작업 화면이 뒤에 남아 있어야 함
- 닫기·저장·저장하고 문자 기능 유지
- 팝업에서 사용자가 명시적으로 이동하기 전 `MainActivity`를 열지 않음

관련 파일:

- `PostCallPopupWindowInstaller.java`
- `PostCallActivity.java`
- `CallPopupNotificationManager.java`
- `CallMonitorService.java`
- `AndroidManifest.xml`
- `styles.xml`

## 절대 변경 금지

- 연락처 원본 이름을 직접 덮어쓰지 않는다.
- `ACCOUNT_TYPE=null` 로컬 연락처로 회귀하지 않는다.
- 통화 종료 직후 `MainActivity`를 강제 실행하지 않는다.
- 앱 삭제 후 콜태그 메모 연락처가 남는 구조로 변경하지 않는다.
- APK 컴파일 성공을 실제 삼성 전화 화면 표시 성공으로 기록하지 않는다.

## 실기기 QA

1. 등록 고객 메모 저장 후 연락처 앱에서 `고객명 · 메모` 확인
2. 실제 수신 전화에서 기본 전화 앱 표시 확인
3. 메모 변경 후 이름 자동 갱신 확인
4. 기능 끄기 후 콜태그 메모 이름 제거·원본 연락처 유지 확인
5. 재활성화 후 메모 이름 재생성 확인
6. 앱 삭제 후 콜태그 전용 연락처 제거·원본 연락처 유지 확인
7. 통화 종료 후 전체 앱 화면이 아니라 대형 팝업으로 표시되는지 확인
8. 팝업 닫기 후 전화 화면 또는 직전 화면으로 복귀하는지 확인
