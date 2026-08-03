# 콜태그 v0.40.9 페이지로 실시간 문의 알림

기준일: **2026-08-03**

## 목적

페이지로 랜딩페이지에서 문의가 접수되면 콜태그가 문의를 가져와 고객으로 등록하고 사용자에게 알림을 표시한다.

## 최종 코드 반영 상태

### 콜태그 Android

- PR `#36`을 `agent/calltag-foundation`에 병합했다.
- 병합 SHA: `ec37673e76e0145fb2db0665b1a83562d2ee5092`
- versionName: `0.40.9`
- versionCode: `57`
- CallTag `main`에는 병합하지 않았다.

### 페이지로 서버

실시간 문의 푸시 전용 PR `pc9839a-lgtm/inlet#56`을 `main`에 병합했다.

- 서버 병합 SHA: `2f016e152f4fb589fb948db6c5a92488591843f2`
- 문의 큐 등록
- 프로젝트 소유자 확인
- Android 기기 등록·상태·해제 API
- FCM HTTP v1 데이터 신호
- 잘못되거나 만료된 토큰 자동 비활성화
- 문의 응답과 푸시 실패 분리
- D1 migration `0008_calltag_realtime_push.sql`

Google 로그인 변경이 함께 있는 `inlet#48`은 Draft로 유지하며 이번 운영 서버 병합에 포함하지 않았다.

## Firebase 등록이 반드시 필요한 이유

코드와 서버가 병합되어 있어도 Firebase 프로젝트에 콜태그 Android 앱이 등록되지 않으면 앱 종료·백그라운드·잠금화면 즉시 알림은 작동하지 않는다.

필수 등록 대상:

- Firebase 프로젝트
- Android 앱 패키지 `kr.pagero.calltag`
- Android 앱 설정 4개
- 서버 서비스 계정 3개
- 운영 D1 migration

상세 등록 절차는 다음 문서를 따른다.

- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`

## Firebase Console 등록 요약

1. Firebase Console에서 프로젝트를 생성하거나 운영 프로젝트를 선택한다.
2. `앱 추가 > Android`를 선택한다.
3. Android 패키지명에 정확히 `kr.pagero.calltag`를 입력한다.
4. 앱 닉네임은 `콜태그`로 입력할 수 있다.
5. FCM만 사용할 때 SHA-1은 생략 가능하다.
6. 앱 등록 후 `google-services.json`을 다운로드한다.
7. 프로젝트 설정의 `클라우드 메시징`에서 FCM HTTP v1 API 사용 상태를 확인한다.
8. 프로젝트 설정의 `서비스 계정`에서 서버용 비공개 키 JSON을 발급한다.

## Android GitHub Secret 대응

`google-services.json`에서 다음 값을 확인한다.

| GitHub Secret | 설정 파일 값 |
|---|---|
| `CALLTAG_FIREBASE_APPLICATION_ID` | `client[].client_info.mobilesdk_app_id` |
| `CALLTAG_FIREBASE_API_KEY` | `client[].api_key[].current_key` |
| `CALLTAG_FIREBASE_PROJECT_ID` | `project_info.project_id` |
| `CALLTAG_FIREBASE_SENDER_ID` | `project_info.project_number` |

GitHub 저장소 `pc9839a-lgtm/calltag`의 `Settings > Secrets and variables > Actions`에 4개 값을 등록한다.

현재 빌드 방식은 `google-services.json`을 저장소에 직접 커밋하지 않고 GitHub Actions Secret을 BuildConfig에 주입한다.

## 페이지로 서버 환경변수 대응

Firebase 서비스 계정 JSON에서 다음 값을 확인한다.

| Cloudflare 환경변수 | 서비스 계정 JSON 값 |
|---|---|
| `FIREBASE_PROJECT_ID` | `project_id` |
| `FIREBASE_CLIENT_EMAIL` | `client_email` |
| `FIREBASE_PRIVATE_KEY` | `private_key` 전체 값 |

서비스 계정 JSON과 비공개 키는 APK·GitHub 코드·문서에 포함하지 않는다. Cloudflare Pages **Production** 환경변수로만 등록한다.

## 처리 흐름

1. 페이지로 `/api/leads`가 문의를 저장한다.
2. 서버가 `eventId` 기준으로 콜태그 문의 큐에 중복 없이 등록한다.
3. 프로젝트 소유자의 등록 Android 기기로 개인정보 없는 FCM 신호를 보낸다.
4. 콜태그는 신호를 받으면 로그인 세션으로 미처리 문의를 조회한다.
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

## 개인정보와 키 관리

FCM payload 포함:

- 이벤트 종류
- 비식별 이벤트 ID
- 큐 ID
- 발송 시각

FCM payload 포함 금지:

- 고객명
- 전화번호
- 이메일
- 문의 내용
- 고객 메모

Firebase Android API 키는 Firebase 프로젝트 식별용 값이지만 운영 환경 분리를 위해 GitHub Secret으로 관리한다.

Firebase 서비스 계정의 `private_key`는 서버 인증용 비밀키다. 절대 APK 또는 공개 저장소에 포함하지 않는다.

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

## 현재 운영 제한

이번 APK의 BuildConfig 정적 확인 결과 다음 값은 모두 빈 문자열이다.

- `FIREBASE_APPLICATION_ID`
- `FIREBASE_API_KEY`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_SENDER_ID`

따라서 현재 확정된 범위:

- 앱 실행·재진입 시 문의 동기화
- 앱을 열어둔 동안 30초 보조 동기화
- 실제 고객 DB 반영 후 알림 표시 로직
- 중복 문의 방지 로직

아직 확정되지 않은 범위:

- 앱 완전 종료 상태 즉시 알림
- 잠금화면 즉시 알림
- Firebase 기기 토큰 서버 등록
- 운영 서버 FCM 실제 발송

## 운영 설정 후 검증 순서

1. Firebase 프로젝트 생성 또는 운영 프로젝트 선택
2. Android 앱 `kr.pagero.calltag` 등록
3. GitHub Actions Secret 4개 등록
4. Firebase 서비스 계정 발급
5. Cloudflare Production 환경변수 3개 등록
6. D1 `0008_calltag_realtime_push.sql` 운영 적용
7. 페이지로 재배포
8. 콜태그 APK 재빌드
9. APK 내부 Firebase 값 비어 있지 않음 확인
10. 기존 앱 위에 덮어 설치
11. 로그인 후 알림 권한 허용
12. 실제 페이지로 문의 1건 접수
13. 앱 종료·잠금화면 알림 확인
14. 알림 터치 후 고객·메모·`PAGERO_INQUIRY` 확인
15. 동일 문의 재처리 시 중복 없음 확인
16. 빠른 연속 문의 3건 모두 반영 확인

## 데이터 안전

- 기존 고객·메모·문자·일정·캠페인 DB 변경 없음
- 앱 삭제 없이 덮어 설치
- FCM 실패가 페이지로 문의 접수를 실패시키지 않음
- 고객 개인정보를 푸시에 포함하지 않음
- 빌드 성공을 운영 실시간 알림 성공으로 표현하지 않음
