# 콜태그 14차 패치 — 암호화 앱 데이터 백업·복원

기준일: 2026-08-01  
완료 버전: `0.33.0` / versionCode `35`  
기능 HEAD: `91e300ca27c824ff0785d2db35e64fb66252d61e`  
대상 브랜치: `agent/calltag-foundation`

## 상태

- Android 코드 구현: 완료
- Java 컴파일: 성공
- Debug APK 패키징: 성공
- 실제 휴대전화 백업·복원 검수: 남음
- `main` 병합: 하지 않음

## 목적

기기 변경, 앱 장애, 데이터 손상에 대비해 콜태그 앱 데이터를 하나의 전용 암호화 파일로 보존하고 같은 콜태그 앱에서 복원한다. 이 기능은 고객 목록이나 캠페인 결과를 외부 업무에 활용하는 데이터 내보내기가 아니다.

## 사용자 경로

```text
계정 및 개인정보
→ 백업 및 복원
```

사용자는 다음 작업을 할 수 있다.

- 8자 이상 암호 설정
- Android 파일 선택기로 백업 위치 지정
- `.ctbackup` 파일 생성
- 백업 파일 선택
- 암호 입력
- 데이터 교체 경고 확인
- 복원 결과와 이미지 누락 건수 확인
- 복원 완료 후 앱 화면 다시 시작

## 파일 형식

확장자: `.ctbackup`

```text
CTBK 전용 헤더
├─ 형식 버전
├─ PBKDF2 반복 횟수
├─ Salt
└─ AES-GCM IV

암호화 영역
└─ ZIP
   ├─ manifest.json
   ├─ databases/calltag*.db
   ├─ preferences/*.json
   └─ files/message_images/*
```

암호화:

- PBKDF2-HMAC-SHA256
- 210,000회 반복
- 256비트 키
- AES-256-GCM
- 사용자 암호는 앱이나 백업 파일에 저장하지 않음

암호를 잊으면 복원할 수 없다.

## 백업 대상

### SQLite DB

현재 기기에 존재하는 `calltag*.db`를 포함한다.

- `calltag.db`
- `calltag_messages.db`
- `calltag_groups.db`
- `calltag_campaigns.db`
- `calltag_task_types.db`
- `calltag_pending.db`
- 향후 추가되는 CallTag 전용 DB

백업 전 각 DB에 `PRAGMA wal_checkpoint(FULL)`을 실행한 뒤 단일 DB 파일을 복사한다.

### 설정

- `calltag_settings`
- `calltag_message_automation`
- `calltag_message_templates_v1`
- `calltag_message_exclusions`
- `calltag_task_message_links_v1`

String, Int, Long, Float, Boolean, String Set 타입을 보존한다.

### 파일

- `files/message_images`의 앱 전용 템플릿 이미지

## 백업 제외

- 로그인 세션과 인증 토큰
- 구독·결제·영수증 상태
- SMS 분할 콜백 임시 상태
- 진단 이벤트와 실기기 체크리스트
- 예약·정합성 복구 로그
- 캐시와 임시 파일
- 외부 연락처 원본

새 기기에서는 자신의 콜태그 계정으로 다시 로그인해야 한다. 구독 권한은 서버·스토어 기준으로 다시 확인한다.

## manifest와 무결성 검사

`manifest.json`에 다음을 기록한다.

- 백업 형식 버전
- 패키지명
- 생성 시각
- 앱 버전과 versionCode
- Android API
- DB·설정·이미지 건수
- 각 내부 파일 경로
- 각 파일 크기
- 각 파일 SHA-256

복원 전 검사:

- `CTBK` 헤더
- 지원 형식 버전
- AES-GCM 인증 태그
- 패키지명 `kr.pagero.calltag`
- 현재 앱보다 새 버전에서 만든 백업인지
- 필수 `databases/calltag.db`
- manifest에 없는 파일 포함 여부
- manifest 파일 누락 여부
- 파일별 크기와 SHA-256
- 허용된 DB·설정·이미지 경로인지
- ZIP 경로 탈출 여부
- 중복 ZIP 경로
- 압축 해제 최대 1GB
- 설정 JSON 최대 크기

검증이 끝나기 전 현재 앱 데이터는 변경하지 않는다.

## 복원 정책

복원은 병합이 아니라 **백업 시점 데이터로 교체**한다.

- 현재 고객과 백업 고객을 합치지 않음
- 현재 캠페인과 백업 캠페인을 중복 생성하지 않음
- 현재 템플릿 이미지 폴더를 백업 이미지 폴더로 교체
- 로그인 세션과 결제 권한은 현재 기기 값을 유지
- 백업에 없는 선택 설정은 앱 기본값으로 다시 생성

병합 복원은 ID 충돌·중복발송 위험 때문에 구현하지 않았다.

## 복원 순서

1. 선택한 파일의 암호화와 manifest를 검증한다.
2. `SENDING` 문자 작업이 있으면 복원을 차단한다.
3. 통화 모니터 서비스를 중지한다.
4. 현재 DB·설정·이미지를 앱 내부 롤백 스냅샷으로 보존한다.
5. 현재 문자 작업 PendingIntent를 취소한다.
6. 기존 CallTag DB를 제거하고 백업 DB를 설치한다.
7. 설정과 템플릿 이미지 폴더를 교체한다.
8. 현재 앱의 SQLiteOpenHelper를 열어 보존 마이그레이션을 실행한다.
9. 모든 CallTag DB에 `PRAGMA quick_check`를 실행한다.
10. 문자자동화·템플릿 기본값을 보완한다.
11. 참조된 템플릿 이미지 누락을 계산한다.
12. 데이터 정합성 복구를 실행한다.
13. 예약문자 복구를 실행한다.
14. 성공하면 롤백 스냅샷을 정리하고 앱 화면을 다시 시작한다.

교체나 검증 중 오류가 발생하면 내부 롤백 스냅샷으로 기존 데이터를 자동 복구하고 오류를 표시한다.

## 안전장치

- 발송 중 `SENDING` 작업이 있으면 백업·복원 차단
- 백업·복원 중 통화 모니터 서비스 일시 중지
- 백업 완료 후 기존 모니터 설정에 따라 재시작
- 복원 전 기존 예약 알람 취소
- 복원 후 정합성·예약 복구
- 잘못된 암호와 AES 인증 실패를 사용자용 오류로 변환
- 현재 앱보다 높은 versionCode의 백업 복원 차단
- 로그인 세션·결제 권한 덮어쓰기 금지
- CSV·XLSX·범용 데이터 형식 생성 금지

## 구현 파일

- `app/src/main/java/kr/pagero/calltag/CallTagBackupManager.java`
- `app/src/main/java/kr/pagero/calltag/BackupRestoreActivity.java`
- `app/src/main/java/kr/pagero/calltag/AccountActivity.java`
- `app/src/main/java/kr/pagero/calltag/CallTagApplication.java`
- `app/src/main/res/layout/activity_account.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `.github/workflows/build-apk.yml`

## 빌드 검증

Android 전용 임시 Draft PR `#7`에서 검증했다.

- Workflow: `Validate CallTag Android`
- Run ID: `30646727731`
- Job ID: `91209824186`
- Java 컴파일: 성공
- Debug APK 패키징: 성공
- APK 업로드: 성공
- Artifact ID: `8799741185`
- Artifact size: `2,543,216 bytes`
- digest: `sha256:2e8134adf50095665fd040725209cf0f42eb956cfe9dbe76a78ceb8970cfe112`

검증 기준 브랜치의 아티팩트 표시명은 과거 `calltag-v0.30.0-debug-apk`지만 실제 빌드 대상은 `versionName 0.33.0`, `versionCode 35`다. 개발 브랜치 워크플로 아티팩트명은 `calltag-v0.33.0-debug-apk`다.

임시 PR `#7`은 병합하지 않고 닫았다.

## 남은 실기기 검수

- Android 파일 선택기로 백업 생성
- 내부 저장소·Google Drive·제조사 파일 앱 저장
- 올바른 암호 복원
- 잘못된 암호 차단
- 백업 파일 잘림·변조 차단
- 현재 앱보다 새 버전 백업 차단
- 이전 DB 버전 백업 마이그레이션
- 실제 다른 기기로 복원
- 로그인·구독 권한이 덮어써지지 않는지
- 템플릿 이미지 복원
- 이미지 누락 경고
- 예약문자와 캠페인 상태 복원
- 일시정지 캠페인이 자동 재개되지 않는지
- 복원 실패 유도 후 기존 데이터 자동 롤백
- 큰 캠페인·다량 이미지 백업 성능

코드와 APK 빌드는 완료됐지만 위 실기기 검수는 완료로 기록하지 않는다.

## 현재 구현 금지

- CSV 내보내기
- XLSX 내보내기
- 고객 목록 파일
- 캠페인 결과표
- 범용 연락처 파일
- 외부 CRM 이전 형식
- `.ctbackup` 일반 데이터 변환

데이터 내보내기는 사용자가 별도로 승인한 이후 유료 기능으로만 진행한다.
