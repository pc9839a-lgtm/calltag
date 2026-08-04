# 콜태그 Firebase 등록·운영 설정 가이드

기준일: **2026-08-04**  
대상 앱: **콜태그 Android**  
Android 패키지명: **`kr.pagero.calltag`**

## 1. 현재 운영 상태

2026-08-04 기준 페이지로 서버 측 설정은 완료됐다.

- Cloudflare `inlet` Production Firebase 환경변수 3개: 정상
- 운영 D1 `inlet-prod` 바인딩: 정상
- `calltag_push_devices` 테이블: 생성 완료
- readiness 최종 상태: `ready=true`
- 확인 Run ID: `30871387043`
- 확인 Job ID: `91875065527`

남은 작업은 CallTag Android 빌드에 Firebase 설정 4개를 주입하고 실제 문의 알림을 검증하는 것이다.

## 2. 비용

콜태그가 사용하는 Firebase Cloud Messaging은 무료 제품이다. Firebase 프로젝트는 결제수단 없이 Spark 요금제로 생성할 수 있다.

콜태그는 고객 DB를 Firebase에 저장하지 않는다. Firebase는 `새 문의가 있음`이라는 개인정보 없는 신호 전달에만 사용한다.

## 3. Firebase 프로젝트·Android 앱

Firebase Console에서:

1. 운영 프로젝트를 선택한다.
2. Android 앱을 추가한다.
3. 패키지명을 정확히 `kr.pagero.calltag`로 입력한다.
4. 앱 등록 후 `google-services.json`을 다운로드한다.
5. 프로젝트 설정의 `클라우드 메시징`에서 FCM HTTP v1 사용 상태를 확인한다.

FCM만 사용할 때 SHA-1은 생략 가능하다.

## 4. CallTag GitHub Actions Secret

`google-services.json`에서 패키지명이 `kr.pagero.calltag`인 client 항목을 확인한다.

| GitHub Secret | `google-services.json` 위치 |
|---|---|
| `CALLTAG_FIREBASE_APPLICATION_ID` | `client[].client_info.mobilesdk_app_id` |
| `CALLTAG_FIREBASE_API_KEY` | `client[].api_key[].current_key` |
| `CALLTAG_FIREBASE_PROJECT_ID` | `project_info.project_id` |
| `CALLTAG_FIREBASE_SENDER_ID` | `project_info.project_number` |

등록 위치:

1. GitHub `pc9839a-lgtm/calltag`
2. `Settings`
3. `Secrets and variables`
4. `Actions`
5. `New repository secret`
6. 위 4개를 각각 등록

등록 후 기존 APK는 자동으로 바뀌지 않는다. 새 APK를 반드시 재빌드해야 한다.

## 5. 서버 서비스 계정

서버용 서비스 계정은 이미 Cloudflare `inlet` Production에 반영됐다.

사용 변수:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

서비스 계정 비공개 키는 다음 위치에 넣지 않는다.

- Android APK
- Android BuildConfig
- GitHub 공개 코드
- 문서·이슈·스크린샷

## 6. 운영 D1

적용 완료:

- database: `inlet-prod`
- migration: `migrations/0008_calltag_realtime_push.sql`
- table: `calltag_push_devices`
- index: `idx_calltag_push_owner_enabled`

이 migration은 기존 고객·문의·회원·랜딩 데이터를 수정하거나 삭제하지 않는다.

## 7. APK 재빌드 후 검증

1. CallTag GitHub Secret 4개 등록
2. APK 재빌드
3. APK 내부 Firebase BuildConfig 값 4개가 비어 있지 않은지 확인
4. 기존 앱 위에 덮어 설치
5. 콜태그 로그인
6. 알림 권한 허용
7. 앱을 한 번 실행해 FCM 토큰 등록
8. 서버의 `calltag_push_devices`에 기기 등록 확인
9. 페이지로 랜딩에서 실제 문의 1건 접수
10. 앱 종료 상태에서 알림 확인
11. 잠금화면 알림 확인
12. 알림 터치 후 고객목록 확인
13. 고객 메모와 `PAGERO_INQUIRY` 상담이력 확인
14. 동일 eventId 재처리 시 중복 없음 확인
15. 빠른 연속 문의 3건 모두 반영 확인

## 8. 정상 판단 기준

정상:

- 서버 readiness `ready=true`
- FCM 기기 토큰 서버 등록
- 문의 접수 후 백그라운드·잠금화면 알림 도착
- 알림 내용에 고객 개인정보가 포함되지 않음
- 고객 자동 생성 또는 기존 고객 갱신
- 문의 내용이 메모와 상담이력에 저장
- 동일 eventId 중복 미생성

비정상:

- APK Firebase 값이 빈 상태
- 앱을 열어야만 문의가 들어옴
- 기기 토큰이 서버에 등록되지 않음
- 문의는 저장되지만 알림이 오지 않음
- 동일 문의가 두 번 생성됨

## 9. 현재 완료·미완료 구분

완료:

- Firebase 프로젝트·Android 앱 등록
- Cloudflare 서버 변수 3개
- 운영 D1 migration
- 서버 readiness `ready=true`
- Android 실시간 문의 처리 코드

미완료:

- CallTag GitHub Secret 4개 등록 확인
- Firebase 값 포함 APK 재빌드
- 실제 기기 토큰 등록
- 앱 종료·잠금화면 알림 E2E

빌드 성공과 실기기 E2E 성공을 같은 상태로 표현하지 않는다.
