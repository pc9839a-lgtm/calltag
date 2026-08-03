# 콜태그 v0.40.9 페이지로 실시간 문의 알림

## 목적

페이지로 랜딩페이지에서 문의가 접수되면 콜태그가 문의를 즉시 가져와 고객으로 등록하고 사용자에게 알림을 표시한다.

## 처리 흐름

1. 페이지로 `/api/leads`가 문의를 저장한다.
2. 서버가 콜태그 문의 큐에 중복 없이 등록한다.
3. 프로젝트 소유자의 등록된 Android 기기로 개인정보 없는 FCM 신호를 보낸다.
4. 콜태그는 신호를 받으면 로그인 세션으로 문의 목록을 다시 조회한다.
5. 전화번호를 기준으로 신규 고객 생성 또는 기존 고객 갱신을 수행한다.
6. 문의 내용과 페이지 정보는 고객 메모와 `PAGERO_INQUIRY` 상담이력으로 저장한다.
7. 서버 ACK가 성공한 건만 처리 완료로 기록한다.
8. 실제 신규·갱신 건수가 있을 때만 `페이지로 문의 접수` 알림을 표시한다.

## v0.40.9 변경

### 실제 반영 후 알림

기존에는 푸시 신호를 받는 즉시 동기화 중이라는 알림을 먼저 표시했다. v0.40.9부터는 고객 DB 반영이 완료된 뒤 실제 처리 건수가 있을 때만 알림을 표시한다.

- 신규 고객: `신규 문의 n건이 고객목록에 등록되었습니다.`
- 기존 고객: `기존 고객 문의 n건이 상담이력에 반영되었습니다.`
- 신규·기존 동시 발생 시 합산 안내
- Android 13 이상 알림 권한이 없으면 시스템 알림은 표시하지 않지만 데이터 동기화는 계속 실행

### 동시 문의·중복 푸시

- 동기화 중 새 푸시가 도착하면 강제 재동기화를 1회 예약한다.
- 여러 푸시가 연속 도착해도 동기화 작업을 무제한 중복 실행하지 않는다.
- `eventId` 수신 기록과 서버 ACK를 이용해 동일 문의의 고객·상담이력 중복 생성을 막는다.
- 문의 반영 후 연락처 메모 동기화도 즉시 요청한다.

### 앱 사용 중 안전 보조 동기화

FCM이 일시적으로 지연되거나 운영 설정이 빠진 경우를 보완한다.

- 앱 화면이 열려 있고 실시간 알림 연결 전이면 30초 간격으로 문의를 확인한다.
- 실시간 알림 연결 후에는 5분 간격으로 누락 여부만 안전 점검한다.
- 앱이 백그라운드일 때 주기적 무한 폴링은 하지 않는다.
- 백그라운드 즉시 알림은 FCM 데이터 메시지가 담당한다.

### APK Firebase 설정 주입

GitHub Actions 빌드가 다음 저장소 Secret을 Android BuildConfig에 주입하도록 변경했다.

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

Secret이 없더라도 APK 빌드는 성공하고 앱 실행 중 보조 동기화는 작동한다. 다만 앱이 닫힌 상태의 즉시 알림은 위 Firebase 설정과 서버 FCM 설정이 모두 있어야 한다.

## 서버 측 변경

서버 구현 브랜치: `pc9839a-lgtm/inlet`의 `agent/calltag-google-realtime-auth`

- 프로젝트 소유자를 `owner_account_id`, `owner_id`, `account_id` 호환 방식으로 확인
- 저장된 문의 또는 제출 프로젝트에 ownerId가 있으면 우선 사용
- FCM 데이터 메시지에 collapse key 적용
- 짧은 시간에 여러 문의가 들어오면 신호는 합쳐질 수 있지만 앱 동기화가 큐의 모든 문의를 가져옴
- 잘못되거나 만료된 FCM 토큰은 자동 비활성화
- 푸시 실패가 페이지로 문의 접수 자체를 실패시키지 않음

## 개인정보

FCM에는 다음 값만 포함한다.

- 이벤트 종류
- 비식별 이벤트 ID
- 큐 ID
- 발송 시각

고객명, 전화번호, 이메일, 문의 내용, 고객 메모는 FCM payload에 포함하지 않는다. 실제 고객정보는 콜태그 로그인 세션으로 서버에서 다시 조회한다.

## 운영 전 필수 조건

### Android APK 빌드 Secret

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

### 페이지로 서버 환경 변수

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

### D1

- `calltag_push_devices` 테이블과 인덱스
- Google 로그인 티켓을 함께 사용하는 경우 migration `0007_calltag_google_push.sql`

## 검증 구분

빌드로 확인 가능한 범위:

- Android Java 컴파일
- Firebase Messaging 의존성 연결
- BuildConfig 필드 생성
- Manifest의 MessagingService 등록
- APK 패키징

실제 운영 환경에서 별도로 확인할 범위:

- GitHub Secret이 실제 APK에 주입됐는지
- Cloudflare 운영 환경에 Firebase 서비스 계정이 설정됐는지
- D1 migration이 운영 DB에 적용됐는지
- 페이지로 문의 접수 후 잠금화면·백그라운드 앱에서 즉시 알림이 오는지
- 알림 터치 후 고객목록에 신규 고객과 상담이력이 있는지
- 같은 문의가 두 번 등록되지 않는지

## 버전

- versionName: `0.40.9`
- versionCode: `57`
- 개발 브랜치: `agent/calltag-foundation`
- `main` 미병합 유지
