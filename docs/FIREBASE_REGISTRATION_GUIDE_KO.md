# 콜태그 Firebase 등록·운영 설정 가이드

기준일: **2026-08-04**  
대상 앱: **콜태그 Android**  
Android 패키지명: **`kr.pagero.calltag`**

## 1. 현재 운영 상태

2026-08-04 기준 서버와 Android 빌드 설정까지 완료됐다.

서버:

- Cloudflare `inlet` Production Firebase 환경변수 3개: 정상
- 운영 D1 `inlet-prod` 바인딩: 정상
- `calltag_push_devices` 테이블: 생성 완료
- readiness 최종 상태: `ready=true`
- 확인 Run ID: `30871387043`
- 확인 Job ID: `91875065527`

Android:

- CallTag GitHub Actions Secret 4개: 등록 확인 완료
- 생성된 BuildConfig Firebase 필드 4개: 비어 있지 않음 확인
- Firebase 설정 포함 v0.40.9 Debug APK 빌드 완료
- 확인 Run ID: `30872373416`
- 확인 Job ID: `91876823885`

남은 작업은 새 APK를 실제 기기에 덮어 설치하고 페이지로 문의 알림 E2E를 검증하는 것이다.

## 2. 비용

콜태그가 사용하는 Firebase Cloud Messaging은 무료 제품이다. Firebase 프로젝트는 결제수단 없이 Spark 요제로 생성할 수 있다.

콜태그는 고객 DB를 Firebase에 저장하지 않는다. Firebase는 `새 문의가 있음`이라는 개인정보 없는 신호 전달에만 사용한다.

## 3. Firebase 프로젝트·Android 앱

등록 완료 기준:

1. Firebase 운영 프로젝트 선택
2. Android 앱 추가
3. 패키지명 `kr.pagero.calltag`
4. `google-services.json` 다운로드
5. FCM HTTP v1 사용 상태 확인

FCM만 사용할 때 SHA-1은 생략 가능하다.

## 4. CallTag GitHub Actions Secret

등록 위치:

1. GitHub `pc9839a-lgtm/calltag`
2. `Settings`
3. `Secrets and variables`
4. `Actions`
5. 아래 4개 등록

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

`google-services.json` 대응:

| GitHub Secret | `google-services.json` 위치 |
|---|---|
| `CALLTAG_FIREBASE_APPLICATION_ID` | `client[].client_info.mobilesdk_app_id` |
| `CALLTAG_FIREBASE_API_KEY` | `client[].api_key[].current_key` |
| `CALLTAG_FIREBASE_PROJECT_ID` | `project_info.project_id` |
| `CALLTAG_FIREBASE_SENDER_ID` | `project_info.project_number` |

2026-08-04 실제 GitHub Actions 빌드에서 4개가 모두 `configured`로 확인됐다. 비밀값 원문은 출력하지 않았다.

## 5. Android BuildConfig 검증

콜태그는 GitHub Actions Secret을 `app/build.gradle`에서 BuildConfig로 주입한다.

확인 필드:

- `BuildConfig.FIREBASE_APPLICATION_ID`
- `BuildConfig.FIREBASE_API_KEY`
- `BuildConfig.FIREBASE_PROJECT_ID`
- `BuildConfig.FIREBASE_SENDER_ID`

검증 PR `#37`을 개발 정본 브랜치에 병합했다.

- 병합 SHA: `9b8318c606f08674aca9cbd20ac9f0cacf52e202`
- Secret 4개 중 하나라도 비면 빌드 실패
- 생성된 BuildConfig 필드 중 하나라도 비면 빌드 실패
- 실제 값은 로그에 출력하지 않음

## 6. 서버 서비스 계정

서버용 서비스 계정은 Cloudflare `inlet` Production에 반영 완료됐다.

사용 변수:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

서비스 계정 비공개 키는 다음 위치에 넣지 않는다.

- Android APK
- Android BuildConfig
- GitHub 공개 코드
- 문서·이슈·스크린샷

## 7. 운영 D1

적용 완료:

- database: `inlet-prod`
- migration: `migrations/0008_calltag_realtime_push.sql`
- table: `calltag_push_devices`
- index: `idx_calltag_push_owner_enabled`

이 migration은 기존 고객·문의·회원·랜딩 데이터를 수정하거나 삭제하지 않는다.

## 8. Firebase 설정 포함 APK

검증 결과:

- Workflow Run ID: `30872373416`
- Job ID: `91876823885`
- Artifact ID: `8878338508`
- Artifact ZIP digest: `sha256:41f23f1e483308ed7f3c02af8571ccba1a95d388494936df5624f9197f6796dd`
- APK SHA-256: `2fb039d9782dedc01abefa02507dd2b5a7401867e5fd0862804c12cd6c101719`
- APK 크기: `4,461,827 bytes`

이전 v0.40.9 APK의 Firebase 값이 비어 있었다는 기록은 이전 빌드에 대한 것이다. 위 검증 APK에는 Firebase Android 설정 4개가 정상 주입됐다.

## 9. 실기기 검증 순서

1. 새 Firebase 설정 포함 APK를 기존 앱 위에 덮어 설치
2. 콜태그 로그인
3. Android 알림 권한 허용
4. 앱을 한 번 실행해 FCM 토큰 발급
5. 서버의 `calltag_push_devices`에 활성 기기 등록 확인
6. 페이지로 랜딩에서 실제 문의 1건 접수
7. 앱 실행 상태 알림 확인
8. 앱 백그라운드 상태 알림 확인
9. 앱 완전 종료 상태 알림 확인
10. 잠금화면 알림 확인
11. 알림 터치 후 고객목록 확인
12. 고객 메모와 `PAGERO_INQUIRY` 상담이력 확인
13. 동일 eventId 재처리 시 중복 없음 확인
14. 빠른 연속 문의 3건 모두 반영 확인

## 10. 정상 판단 기준

정상:

- 서버 readiness `ready=true`
- Firebase 설정이 포함된 APK 설치
- FCM 기기 토큰 서버 등록
- 문의 접수 후 백그라운드·잠금화면 알림 도착
- 알림 내용에 고객 개인정보가 포함되지 않음
- 고객 자동 생성 또는 기존 고객 갱신
- 문의 내용이 메모와 상담이력에 저장
- 동일 eventId 중복 미생성

비정상:

- 기기 토큰이 서버에 등록되지 않음
- 문의는 저장되지만 알림이 오지 않음
- 앱을 열어야만 문의가 들어옴
- 동일 문의가 두 번 생성됨

## 11. 현재 완료·미완료 구분

완료:

- Firebase 프로젝트·Android 앱 등록
- CallTag GitHub Secret 4개 등록 확인
- Firebase 설정 포함 APK 빌드
- Cloudflare 서버 변수 3개
- 운영 D1 migration
- 서버 readiness `ready=true`
- Android 실시간 문의 처리 코드

실기기 확인 필요:

- FCM 기기 토큰 운영 등록
- 앱 종료·백그라운드·잠금화면 알림 E2E
- 고객·메모·상담이력 반영 E2E
- 중복방지·연속 문의 E2E

빌드 성공과 실기기 E2E 성공을 같은 상태로 표현하지 않는다.