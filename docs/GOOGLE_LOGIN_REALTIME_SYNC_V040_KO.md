# 콜태그 v0.40.0 Google 로그인·페이지로 실시간 문의 연동

## 사용자 동작

1. 로그인 화면에서 이메일/비밀번호 또는 `Google로 계속하기`를 선택한다.
2. Google 로그인은 시스템 브라우저에서 진행한다.
3. 인증 완료 후 `calltag://auth/google` 딥링크로 앱에 복귀한다.
4. 앱은 2분 유효·1회 사용 로그인 티켓을 서버에 교환해 콜태그 세션을 저장한다.
5. 로그인 직후 같은 `ownerId`의 페이지로 프로젝트 존재 여부를 확인한다.
6. 페이지로 계정이 없거나 확인에 실패해도 콜태그 로그인은 허용한다.
7. 미연결 안내는 `더보기 → 페이지로 연결`에서 다시 확인하도록 표시한다.

## 계정 매핑 원칙

- Google 인증 이메일과 기존 계정 이메일이 같으면 기존 계정을 재사용한다.
- 기존 계정의 `ownerId`를 변경하거나 새 계정으로 분리하지 않는다.
- 신규 Google 이메일이면 이메일 기반 안정 `ownerId`로 계정을 만든다.
- 페이지로 문의 조회·ACK·실시간 푸시 대상은 모두 이 `ownerId`로 격리한다.
- Android 딥링크에는 장기 세션을 넣지 않고 짧은 1회용 티켓만 전달한다.

## 실시간 문의 경로

```text
페이지로 문의 저장
→ CallTag 큐 적재
→ 프로젝트 ownerId 조회
→ 해당 ownerId의 활성 Android 기기 토큰 조회
→ 개인정보 없는 FCM data 신호 전송
→ 앱이 /api/call/pagero/leads 동기화 실행
→ 로컬 고객 신규 등록 또는 갱신
→ 서버 ACK
```

FCM payload 허용값:

- `type=pagero_lead_available`
- `eventId`
- `queueId`

금지값:

- 고객명
- 전화번호
- 이메일
- 문의 내용
- 고객 메모

푸시 실패는 페이지로 문의 저장과 큐 적재를 실패시키지 않는다. 앱 실행·화면 재진입·`지금 동기화` 경로를 항상 유지한다.

## 서버 운영 설정

inlet/페이지로 서버:

- `GOOGLE_LOGIN_CLIENT_ID`
- `GOOGLE_LOGIN_CLIENT_SECRET`
- `GOOGLE_LOGIN_REDIRECT_URI=https://pagero.kr/api/call/google/callback`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

Android 빌드 환경:

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

Android Firebase 값이 없으면 빌드는 성공하지만 실시간 상태는 `실시간 설정 필요`로 표시한다. 기존 폴링 동기화는 계속 작동한다.

## 신규 서버 API

- `GET /api/call/google/start`
- `GET /api/call/google/callback`
- `POST /api/call/google/exchange`
- `GET /api/call/pagero/account`
- `POST /api/call/push/register`
- `POST /api/call/push/unregister`
- `GET /api/call/push/status`

## Android 구성

- `LoginActivity`: Google 버튼, 브라우저 이동, 딥링크 수신, 티켓 교환
- `PageroAccountConnectionManager`: 로그인 후 페이지로 프로젝트 소유 여부 확인
- `PageroAccountStatusStore`: connected/not_connected/unknown 저장
- `CallTagFirebaseInitializer`: 빌드 환경값이 있을 때만 Firebase 초기화
- `CallTagPushManager`: FCM 토큰 등록·상태 확인·로그아웃 해제
- `CallTagMessagingService`: 실시간 신호 수신 후 페이지로 동기화 요청
- `PageroConnectionActivity`: 계정 연결과 실시간 연결 상태를 구분해 표시

## 전화 수신 메모 복구

v0.40.0부터 등록 고객 전화 수신 오버레이에 다음 형식을 다시 표시한다.

```text
010-1234-5678 · 최근 메모 한 줄 요약…
```

- 전화번호 옆 요약은 최대 24자로 제한한다.
- 오버레이 줄은 최대 2줄이며 길면 말줄임 처리한다.
- 기존 `최근 메모` 전체 카드 최대 4줄은 유지한다.
- 오버레이 실패 시 수신 알림에도 전화번호와 메모 요약을 표시한다.
- 수신 화면 테스트도 동일 형식을 사용한다.

## 검증 필요 항목

- Google OAuth 동의화면 및 redirect URI 운영 등록
- 기존 이메일 계정 Google 로그인 시 같은 ownerId 유지
- 페이지로 프로젝트 있음/없음/조회 실패 세 경우 로그인 허용
- FCM 값 있음/없음 빌드 모두 앱 시작 성공
- 신규 문의 FCM 수신 후 중복 없이 고객 등록
- 앱 종료·절전·삼성 백그라운드 제한 상태
- 실제 전화 수신 시 번호 옆 메모 표시 및 잘림 여부

빌드 성공은 Google OAuth 운영 설정, FCM 실전송, 실제 수신 전화 오버레이 성공을 의미하지 않는다. 반드시 실기기 E2E 검증이 필요하다.
