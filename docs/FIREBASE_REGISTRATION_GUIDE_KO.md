# 콜태그 Firebase 등록·운영 설정 가이드

기준일: **2026-08-03**  
대상 앱: **콜태그 Android**  
Android 패키지명: **`kr.pagero.calltag`**

## 1. 왜 Firebase 등록이 필요한가

페이지로 문의가 접수됐다는 사실을 앱이 완전히 종료됐거나 휴대전화가 잠긴 상태에서도 즉시 전달하려면 Firebase Cloud Messaging(FCM)이 필요하다.

현재 콜태그 코드에는 다음 기능이 구현되어 있다.

- FCM 기기 토큰 발급 및 서버 등록
- `pagero_lead_available` 데이터 메시지 수신
- 수신 즉시 페이지로 문의 큐 동기화
- 실제 고객 DB 반영 완료 후 사용자 알림 표시
- 동일 문의 중복 등록 방지

그러나 Firebase 프로젝트에 콜태그 Android 앱을 등록하고 운영 설정값을 넣지 않으면 백그라운드 즉시 알림은 활성화되지 않는다.

## 2. 비용

콜태그가 사용하는 Firebase Cloud Messaging은 무료 제품이다. Firebase 프로젝트는 결제수단 없이 Spark 요금제로 생성할 수 있다.

콜태그는 고객 DB를 Firebase에 저장하지 않는다. Firebase는 `새 문의가 있음`이라는 개인정보 없는 신호 전달에만 사용한다.

## 3. Firebase 프로젝트 생성

1. Firebase Console에 Google 계정으로 로그인한다.
2. `프로젝트 만들기`를 누른다.
3. 프로젝트 이름 예시: `calltag-pagero`.
4. Google Analytics는 FCM 수신 자체에는 필수가 아니다. 메시지 전송 보고가 필요하면 켠다.
5. 프로젝트 생성을 완료한다.

기존 페이지로용 Google Cloud/Firebase 프로젝트가 있고 운영 권한을 명확히 관리할 수 있다면 같은 프로젝트를 사용해도 된다. 다만 개발·운영 환경이 섞이지 않도록 운영 프로젝트를 하나로 확정해야 한다.

## 4. 콜태그 Android 앱 등록

Firebase 프로젝트 개요에서 Android 아이콘 또는 `앱 추가 > Android`를 선택한다.

입력값:

- Android 패키지 이름: **`kr.pagero.calltag`**
- 앱 닉네임: `콜태그` 또는 `CallTag` — 선택사항
- 디버그 서명 인증서 SHA-1: FCM만 사용할 때는 생략 가능

주의:

- 패키지명은 공백 없이 정확히 `kr.pagero.calltag`로 입력한다.
- 페이지로 웹 앱 패키지나 기존 콜링크 패키지를 넣지 않는다.
- Google 로그인까지 Firebase Authentication으로 연결할 때는 SHA-1/SHA-256 등록이 추가로 필요할 수 있다.

등록을 완료한 뒤 `google-services.json`을 다운로드한다.

## 5. Android 설정값 확인

다운로드한 `google-services.json`에서 패키지명이 `kr.pagero.calltag`인 `client` 항목을 찾는다.

콜태그 GitHub Actions Secret과의 대응은 다음과 같다.

| GitHub Secret | `google-services.json` 위치 |
|---|---|
| `CALLTAG_FIREBASE_APPLICATION_ID` | `client[].client_info.mobilesdk_app_id` |
| `CALLTAG_FIREBASE_API_KEY` | `client[].api_key[].current_key` |
| `CALLTAG_FIREBASE_PROJECT_ID` | `project_info.project_id` |
| `CALLTAG_FIREBASE_SENDER_ID` | `project_info.project_number` |

현재 콜태그는 `google-services.json`을 저장소에 직접 커밋하는 방식이 아니라 GitHub Actions Secret을 BuildConfig에 주입하는 방식을 사용한다.

Firebase Android API 키는 프로젝트 식별용 값이며 Firebase 서비스에만 제한된 키라면 일반적인 서버 비밀키와 성격이 다르다. 그러나 운영 설정 변경과 환경 분리를 위해 콜태그에서는 GitHub Secret으로 관리한다.

## 6. GitHub Actions Secret 등록

GitHub 저장소 `pc9839a-lgtm/calltag`에서 다음 순서로 등록한다.

1. 저장소 `Settings`
2. `Secrets and variables`
3. `Actions`
4. `New repository secret`
5. 아래 4개를 각각 등록

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

등록 후 기존 APK는 자동으로 바뀌지 않는다. 반드시 새 버전 APK를 다시 빌드하고 APK 내부 BuildConfig에 값이 들어갔는지 확인해야 한다.

## 7. Firebase Cloud Messaging API 확인

Firebase Console에서 다음을 확인한다.

1. 프로젝트 설정
2. `클라우드 메시징` 탭
3. Firebase Cloud Messaging API(V1) 사용 상태 확인
4. 발신자 ID가 `project_info.project_number`와 같은지 확인

콜태그 서버는 레거시 서버 키 방식이 아니라 FCM HTTP v1 API를 사용한다.

## 8. 서버용 서비스 계정 키 생성

Firebase Console에서 다음 순서로 이동한다.

1. 프로젝트 설정
2. `서비스 계정`
3. `Firebase Admin SDK`
4. `새 비공개 키 생성`
5. JSON 파일 다운로드

이 JSON은 **서버 비밀키**다.

절대 금지:

- APK에 포함
- Android BuildConfig에 포함
- GitHub 공개 코드에 커밋
- 채팅·문서·스크린샷에 전체 내용 노출

서비스 계정 JSON에서 페이지로 Cloudflare 환경변수에 사용할 값:

| Cloudflare 환경변수 | 서비스 계정 JSON 위치 |
|---|---|
| `FIREBASE_PROJECT_ID` | `project_id` |
| `FIREBASE_CLIENT_EMAIL` | `client_email` |
| `FIREBASE_PRIVATE_KEY` | `private_key` |

`FIREBASE_PRIVATE_KEY`는 `-----BEGIN PRIVATE KEY-----`와 `-----END PRIVATE KEY-----`를 포함한 전체 값을 사용한다. Cloudflare 입력 과정에서 줄바꿈이 보존되는지 반드시 확인한다.

## 9. Cloudflare 운영 환경변수 등록

페이지로 Cloudflare Pages 프로젝트의 운영 환경에 다음 값을 등록한다.

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

Preview에만 넣고 Production에 빠뜨리면 실제 `pagero.kr` 문의에서는 푸시가 발송되지 않는다. 운영 환경과 미리보기 환경을 구분해 확인한다.

환경변수 등록 후 페이지로 프로젝트를 다시 배포해야 한다.

## 10. D1 migration

페이지로 운영 D1에 다음 migration 적용을 확인한다.

- `migrations/0008_calltag_realtime_push.sql`

생성 대상:

- `calltag_push_devices` 테이블
- ownerId·deviceId 유일 처리
- FCM token 유일 처리
- 활성 기기 조회 인덱스

서버 코드에는 `CREATE TABLE IF NOT EXISTS` 방어가 있지만 운영 이력과 재현성을 위해 migration 적용 상태를 별도로 기록한다.

## 11. 재빌드와 검증

설정 완료 후 진행 순서:

1. 콜태그 APK 재빌드
2. APK 내부 Firebase 4개 값이 비어 있지 않은지 정적 확인
3. 기존 앱 위에 덮어 설치
4. 콜태그 로그인
5. 알림 권한 허용
6. 앱을 한 번 실행해 FCM 토큰을 서버에 등록
7. 페이지로 랜딩에서 실제 문의 1건 접수
8. 앱 종료 상태에서 알림 수신 확인
9. 알림 터치 후 고객목록 확인
10. 고객 메모와 `PAGERO_INQUIRY` 상담이력 확인
11. 동일 문의 재처리 시 중복 고객·상담이력 없음 확인
12. 빠른 연속 문의 3건이 모두 반영되는지 확인

## 12. 정상 판단 기준

정상:

- 문의 접수 후 백그라운드·잠금화면에서 알림 도착
- 알림 내용에 고객 개인정보가 직접 포함되지 않음
- 알림 터치 후 콜태그 진입
- 고객 자동 생성 또는 기존 고객 갱신
- 문의 내용이 메모와 상담이력에 저장
- 동일 eventId 중복 미생성

비정상:

- 앱을 열어야만 문의가 들어옴
- `실시간 문의 알림 연결이 필요합니다` 상태가 계속됨
- 서버에 기기 토큰이 등록되지 않음
- 문의는 저장되지만 알림이 오지 않음
- 한 문의가 고객·상담이력에 두 번 생성됨

## 13. 현재 상태

2026-08-03 빌드된 콜태그 v0.40.9 APK는 Firebase Android 설정 4개가 빈 값이다.

따라서 현재 확정된 기능:

- 앱 실행·재진입 시 문의 동기화
- 앱을 열어둔 동안 최대 약 30초 보조 동기화
- 실제 고객 DB 반영 후 알림 표시 로직
- 동일 문의 중복방지

아직 운영 완료가 아닌 기능:

- 앱 완전 종료 상태 즉시 알림
- 잠금화면 즉시 알림
- 운영 FCM 기기 토큰 등록·발송 E2E

Firebase 프로젝트·Android 앱 등록, GitHub Secret, Cloudflare 환경변수, D1 migration, APK 재빌드가 완료된 뒤 실제 문의 E2E를 통과해야 운영 완료로 처리한다.
