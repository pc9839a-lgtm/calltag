# 콜태그 v0.40.9 페이지로 실시간 문의 알림

## 목적

페이지로 랜딩페이지에서 문의가 접수되면 콜태그가 문의를 가져와 고객으로 등록하고 사용자에게 알림을 표시한다.

## 최종 코드 반영 상태

### 콜태그 Android

- PR `#36`을 `agent/calltag-foundation`에 병합했다.
- 병합 SHA: `ec37673e76e0145fb2db0665b1a83562d2ee5092`
- versionName: `0.40.9`
- versionCode: `57`
- `main`에는 병합하지 않았다.

### 페이지로 서버

Google 로그인 변경과 분리한 실시간 문의 푸시 전용 PR `pc9839a-lgtm/inlet#56`을 `main`에 병합했다.

- 서버 병합 SHA: `2f016e152f4fb589fb948db6c5a92488591843f2`
- 문의 큐 등록
- 프로젝트 소유자 확인
- Android 기기 등록·상태·해제 API
- FCM HTTP v1 데이터 신호
- 잘못되거나 만료된 토큰 자동 비활성화
- 문의 응답과 푸시 실패 분리
- D1 migration `0008_calltag_realtime_push.sql`

기존 Google 로그인 통합 PR `inlet#48`은 Draft로 유지하며 이번 운영 서버 병합에 포함하지 않았다.

## 처리 흐름

1. 페이지로 `/api/leads`가 문의를 저장한다.
2. 서버가 `eventId` 기준으로 콜태그 문의 큐에 중복 없이 등록한다.
3. 프로젝트 소유자의 등록 Android 기기로 개인정보 없는 FCM 신호를 보낸다.
4. 콜태그는 신호를 받으면 로그인 세션으로 미처리 문의를 다시 조회한다.
5. 전화번호 기준으로 신규 고객 생성 또는 기존 고객 갱신을 수행한다.
6. 문의 내용은 고객 메모와 `PAGERO_INQUIRY` 상담이력으로 저장한다.
7. 서버 ACK가 성공한 건만 처리 완료로 기록한다.
8. 실제 신규·갱신 건수가 있을 때만 `페이지로 문의 접수` 알림을 표시한다.

## Android v0.40.9 변경

### 실제 반영 후 알림

푸시 신호를 받았다는 이유만으로 알림을 먼저 표시하지 않는다. 고객 DB 반영이 완료된 뒤 실제 처리 건수가 있을 때만 알림을 표시한다.

- 신규 고객: `신규 문의 n건이 고객목록에 등록되었습니다.`
- 기존 고객: `기존 고객 문의 n건이 상담이력에 반영되었습니다.`
- 신규·기존 동시 발생 시 합산 안내
- Android 13 이상에서 알림 권한이 없어도 데이터 동기화는 계속 실행

### 동시 문의·중복 푸시

- 동기화 중 새 푸시가 도착하면 강제 재동기화를 1회 예약한다.
- 푸시가 연속 도착해도 동기화 스레드를 무제한 중복 실행하지 않는다.
- `eventId` 수신 기록과 서버 ACK로 동일 문의의 고객·상담이력 중복 생성을 막는다.
- 문의 반영 후 연락처 고객명·최근 메모 동기화도 즉시 요청한다.

### 앱 사용 중 보조 동기화

- 실시간 알림 연결 전: 앱이 열려 있으면 30초 간격으로 문의 확인
- 실시간 알림 연결 후: 앱 사용 중 5분 간격으로 누락 안전 확인
- 앱 백그라운드 무한 폴링 없음
- 앱 종료·잠금화면 즉시 처리는 FCM 데이터 메시지가 담당

## 개인정보

FCM payload에는 다음 값만 포함한다.

- 이벤트 종류
- 비식별 이벤트 ID
- 큐 ID
- 발송 시각

고객명, 전화번호, 이메일, 문의 내용, 고객 메모는 FCM payload에 포함하지 않는다. 실제 고객정보는 콜태그 로그인 세션으로 서버에서 다시 조회한다.

## 빌드 검증

### Android

- Workflow: `Build CallTag APK`
- Run ID: `30821634434`
- Job ID: `91712722719`
- Android 리소스 처리: 성공
- Java 컴파일: 성공
- Manifest 병합: 성공
- Debug APK 패키징: 성공
- Artifact ID: `8859117965`
- Artifact ZIP digest: `sha256:448d3f52c0bfb5dc50ab5abc481ca2e260832bd94fd45657025bcd7d957efa04`
- 실제 APK SHA-256: `003a6e1ed3ab704de050fff30f35b47b187f34011acb9ed1c064ebf339b8f4e9`
- 실제 APK 크기: `4,461,827 bytes`

### 페이지로 서버 PR #56

- Validate Pagero CallTag Bridge Run ID: `30822112193`
- Job ID: `91714345005`
- JavaScript syntax: 성공
- Bridge contract: 성공
- Pages Functions regression: 성공
- Production build: 성공
- QA Run ID: `30822112885`
- Full offline QA: 성공
- form·editor·landing·template mobile 브라우저 회귀: 성공

## 현재 운영 제한 — 반드시 확인

이번 APK의 `BuildConfig` 정적 확인 결과 다음 값은 모두 빈 문자열이다.

- `FIREBASE_APPLICATION_ID`
- `FIREBASE_API_KEY`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_SENDER_ID`

따라서 이번 APK에서 확정할 수 있는 범위는 다음과 같다.

- 앱 실행·재진입 시 문의 동기화
- 앱을 열어둔 동안 실시간 연결 전 30초 보조 동기화
- 실제 고객 DB 반영 후 알림 표시 로직
- 중복 문의 방지 로직

아직 확정할 수 없는 범위:

- 앱이 완전히 종료된 상태의 즉시 알림
- 잠금화면 즉시 알림
- Firebase 기기 토큰 서버 등록
- 운영 서버 FCM 실제 발송

## 운영 설정 필요

### CallTag GitHub Actions Secret

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

Secret 등록 후 APK를 다시 빌드해야 한다.

### 페이지로 Cloudflare 환경 변수

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

### D1

- `migrations/0008_calltag_realtime_push.sql` 운영 적용 확인

서버 코드가 `inlet/main`에 병합된 것은 확인했지만 Cloudflare 운영 배포 완료, 환경 변수 등록, migration 적용은 별도 운영 확인이 필요하다.

## 다음 P0 검증

1. Android Firebase Secret 4개 등록 후 APK 재빌드
2. Cloudflare Firebase 서비스 계정 3개 등록
3. D1 migration 운영 적용
4. 페이지로 실제 문의 1건 제출
5. 앱 종료 상태에서 즉시 알림 도착 확인
6. 알림 터치 후 고객목록과 `PAGERO_INQUIRY` 상담이력 확인
7. 같은 문의 재전송 시 중복 생성 없음 확인
8. 빠른 연속 문의 3건이 모두 반영되는지 확인

## 데이터 안전

- 기존 고객·메모·문자·일정·캠페인 DB 변경 없음
- 앱 삭제 없이 덮어 설치
- FCM 실패가 페이지로 문의 접수를 실패시키지 않음
- 고객 개인정보를 푸시에 포함하지 않음
- 빌드 성공을 운영 실시간 알림 성공으로 표현하지 않음
