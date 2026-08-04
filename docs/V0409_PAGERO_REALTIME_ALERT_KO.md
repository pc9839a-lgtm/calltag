# 콜태그 v0.40.9 페이지로 실시간 문의 알림

기준일: **2026-08-04**

## 1. 구현 상태

### Android

- 실시간 문의 알림 PR `#36`을 `agent/calltag-foundation`에 병합
- 병합 SHA: `ec37673e76e0145fb2db0665b1a83562d2ee5092`
- Firebase 빌드 검증 PR `#37` 병합
- 검증 병합 SHA: `9b8318c606f08674aca9cbd20ac9f0cacf52e202`
- versionName `0.40.9`
- versionCode `57`
- CallTag `main` 미병합

### 페이지로 서버

- 실시간 푸시 전용 PR `pc9839a-lgtm/inlet#56` main 병합
- 병합 SHA: `2f016e152f4fb589fb948db6c5a92488591843f2`
- 문의 큐 등록
- ownerId별 Android 기기 등록
- FCM HTTP v1 데이터 신호
- 만료 토큰 비활성화
- D1 migration `0008_calltag_realtime_push.sql`
- 푸시 장애와 문의 저장 성공 분리

## 2. 처리 흐름

1. 페이지로 `/api/leads`가 문의를 저장한다.
2. 서버가 `eventId` 기준으로 문의 큐에 중복 없이 등록한다.
3. 서버가 소유자의 Android 기기로 개인정보 없는 FCM 신호를 보낸다.
4. 앱은 신호 수신 후 로그인 세션으로 미처리 문의를 조회한다.
5. 전화번호 기준 신규 고객 생성 또는 기존 고객 갱신을 수행한다.
6. 문의를 고객 메모와 `PAGERO_INQUIRY` 상담이력으로 저장한다.
7. ACK 완료 건만 처리 완료로 기록한다.
8. 실제 고객 DB 반영 후에만 `페이지로 문의 접수` 알림을 표시한다.

## 3. Android 안정장치

- 동기화 중 추가 푸시 수신 시 강제 재동기화 1회 예약
- 동기화 스레드 무제한 중복 실행 차단
- eventId receipt와 ACK 기반 중복방지
- 문의 반영 후 연락처 고객명·최근 메모 즉시 동기화
- 실시간 연결 전 앱 전면 30초 보조 동기화
- 실시간 연결 후 앱 전면 5분 누락 점검
- 백그라운드 무한 폴링 없음
- Android 13 이상 알림 권한이 없어도 데이터 동기화는 진행

## 4. 개인정보

FCM payload 포함:

- 이벤트 종류
- 비식별 eventId
- queueId
- 발송 시각

FCM payload 포함 금지:

- 고객명
- 전화번호
- 이메일
- 문의 내용
- 고객 메모

Firebase 서비스 계정 비공개 키는 Android APK·GitHub 공개 코드·문서에 포함하지 않는다.

## 5. 운영 서버 준비 완료

최신 Cloudflare Production 배포의 `/api/call/push/readiness`를 GitHub Actions에서 직접 조회했다.

- 확인 배포 `https://89a7a596.inlet-8mr.pages.dev`
- Workflow `Verify CallTag Push Readiness`
- Run ID `30871387043`
- Job ID `91875065527`
- 확인 시각 `2026-08-04T02:26:12.061Z`

결과:

```json
{
  "ready": true,
  "firebase": {
    "configured": true,
    "projectId": true,
    "clientEmail": true,
    "privateKey": true
  },
  "d1": {
    "bound": true,
    "pushDevicesTable": true
  }
}
```

## 6. Android Firebase 설정·APK 빌드 완료

- Workflow Run ID `30872373416`
- Job ID `91876823885`
- Secret 4개 모두 `configured`
- 생성된 BuildConfig Firebase 필드 4개 모두 `configured`
- 리소스·Java·Manifest·Debug APK 빌드 성공
- Artifact ID `8878338508`
- APK SHA-256 `2fb039d9782dedc01abefa02507dd2b5a7401867e5fd0862804c12cd6c101719`
- APK 크기 `4,461,827 bytes`

CI 검증:

- Firebase Secret 4개 중 하나라도 비면 빌드 실패
- 생성된 BuildConfig Firebase 필드 중 하나라도 비면 빌드 실패
- 비밀값 원문은 로그에 출력하지 않음

## 7. 2026-08-04 실기기 운영 확인

사용자가 실제 페이지로 문의를 제출해 콜태그 알림 수신을 확인했다.

확인 완료:

- 실제 운영 페이지로 문의 → 콜태그 FCM 알림
- 앱 완전 종료 상태 알림
- 휴대전화 잠금화면 알림

기능적 판정:

- Firebase Android 설정 포함 APK 정상 초기화
- FCM 기기 토큰 발급·운영 서버 등록 경로 정상 동작
- 페이지로 서버 FCM HTTP v1 발송이 실제 기기로 전달
- 앱 종료·잠금 상태에서 사용자 알림 표시

## 8. 남은 실기기 확인

1. 알림 터치 후 해당 고객 화면 또는 고객목록 이동
2. 고객 자동 생성 또는 기존 고객 갱신
3. 문의 내용의 고객 메모 반영
4. `PAGERO_INQUIRY` 상담이력 생성
5. 동일 eventId 중복 미생성
6. 빠른 연속 문의 3건 전부 반영

사용자 확인 범위보다 넓게 완료로 처리하지 않는다.

상세 등록·운영 절차:

- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`
- `docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`