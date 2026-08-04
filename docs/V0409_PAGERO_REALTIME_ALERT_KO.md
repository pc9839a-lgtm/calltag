# 콜태그 v0.40.9 페이지로 실시간 문의 알림

기준일: **2026-08-04**

## 1. 구현 상태

### Android

- PR `#36`을 `agent/calltag-foundation`에 병합
- 병합 SHA: `ec37673e76e0145fb2db0665b1a83562d2ee5092`
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
3. 서버가 소유자의 Android 기기로 개인정보 없는 신호를 보낸다.
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

## 5. 빌드 검증

Android:

- Workflow Run ID `30821634434`
- Job ID `91712722719`
- 리소스·Java·Manifest·Debug APK 성공
- Artifact ID `8859117965`
- APK SHA-256 `003a6e1ed3ab704de050fff30f35b47b187f34011acb9ed1c064ebf339b8f4e9`
- APK 크기 `4,461,827 bytes`

페이지로 서버:

- Bridge Run ID `30822112193`
- JavaScript syntax·Bridge contract·Pages Functions regression·Production build 성공
- 전체 QA Run ID `30871387085`
- Full offline QA와 브라우저 회귀 성공

## 6. 2026-08-04 운영 서버 준비 완료

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

판정:

- Cloudflare `inlet` Production의 Firebase 서비스 계정 변수 3개 정상
- 운영 D1 `inlet-prod` 연결 정상
- `calltag_push_devices` 테이블 생성 완료
- 페이지로 서버의 FCM 발송 준비 완료
- 비공개 키 원문은 확인·출력하지 않고 존재 여부만 검사

## 7. 현재 가능한 범위

서버:

- FCM HTTP v1 발송 인증 준비
- 사용자별 Android 기기 토큰 저장
- 잘못된·만료 토큰 비활성화
- 문의 저장 성공과 푸시 실패 분리
- 개인정보 없는 신호 전송

앱에서 확정된 범위:

- 앱 실행·재진입 문의 동기화
- 앱을 열어둔 동안 최대 약 30초 보조 동기화
- 고객 DB 반영 후 알림 로직
- 동일 문의 중복방지

아직 미확인:

- 앱 완전 종료 상태 즉시 알림
- 잠금화면 즉시 알림
- FCM 기기 토큰 운영 등록
- 실제 운영 FCM 발송

현재 v0.40.9 APK는 Android Firebase BuildConfig 값 4개가 빈 상태다.

## 8. 다음 조치

CallTag GitHub 저장소 `pc9839a-lgtm/calltag`의 Actions Secrets에 다음 4개를 등록한다.

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

등록 후:

1. Firebase 값 포함 APK 재빌드
2. APK 내부 값 비어 있지 않음 확인
3. 기존 앱 위에 덮어 설치
4. 로그인 후 알림 권한 허용
5. FCM 기기 토큰 서버 등록 확인
6. 실제 페이지로 문의 1건 접수
7. 앱 종료·백그라운드·잠금화면 알림 확인
8. 고객·메모·`PAGERO_INQUIRY` 정상 반영 확인
9. 동일 eventId 중복 미생성 확인
10. 빠른 연속 문의 3건 전부 반영 확인

## 9. 운영 완료 기준

- 서버 readiness `ready=true` — 완료
- Firebase Android 값이 포함된 APK — 미완료
- 기기 토큰 서버 등록 — 미확인
- 실제 페이지로 문의 후 종료·잠금화면 알림 — 미확인
- 고객·메모·상담이력 정상 반영 — 미확인
- 동일 eventId 중복 미생성 — 미확인

상세 등록·운영 절차:

- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`
- `docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`
