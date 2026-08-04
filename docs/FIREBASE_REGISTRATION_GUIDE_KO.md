# 콜태그 Firebase 등록·운영 설정 가이드

기준일: **2026-08-04**  
대상 앱: **콜태그 Android**  
Android 패키지명: **`kr.pagero.calltag`**

## 1. 현재 운영 상태

2026-08-04 기준 서버, Android 빌드 설정, 실제 종료·잠금화면 알림까지 확인됐다.

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

실기기:

- 실제 페이지로 문의 후 콜태그 알림 수신
- 앱 완전 종료 상태 알림 수신
- 휴대전화 잠금화면 알림 수신

## 2. 비용

콜태그가 사용하는 Firebase Cloud Messaging은 무료 제품이다. Firebase 프로젝트는 결제수단 없이 Spark 요금제로 생성할 수 있다.

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

2026-08-04 실제 GitHub Actions 빌드에서 4개가 모두 `configured`로 확인됐다. 비밀값 원문은 출력하지 않았다.

## 5. Android BuildConfig 검증

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

서비스 계정 비공개 키는 Android APK·BuildConfig·GitHub 공개 코드·문서·스크린샷에 넣지 않는다.

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

## 9. 실기기 확인 결과

2026-08-04 사용자 확인:

- 실제 페이지로 문의 접수 후 알림 수신
- 앱 완전 종료 상태에서 알림 수신
- 잠금화면에서 알림 수신

이 결과로 다음 경로가 기능적으로 확인됐다.

- Android Firebase 초기화
- FCM 기기 토큰 발급·운영 서버 등록
- 페이지로 서버 FCM HTTP v1 발송
- Android 종료·잠금 상태 수신
- 사용자 알림 표시

다만 알림 도착만으로 아래 항목까지 완료로 간주하지 않는다.

- 알림 터치 후 해당 고객 이동
- 고객 메모·`PAGERO_INQUIRY` 상담이력 표시
- 동일 eventId 중복 미생성
- 빠른 연속 문의 3건 전부 반영

## 10. 현재 완료·미완료 구분

완료:

- Firebase 프로젝트·Android 앱 등록
- CallTag GitHub Secret 4개 등록 확인
- Firebase 설정 포함 APK 빌드
- Cloudflare 서버 변수 3개
- 운영 D1 migration
- 서버 readiness `ready=true`
- 실제 운영 FCM 발송·수신
- 앱 완전 종료·잠금화면 알림

추가 확인 필요:

- 알림 터치 후 고객 화면 이동
- 고객·메모·상담이력 반영
- 동일 문의 중복방지 실기기 확인
- 빠른 연속 문의 처리 실기기 확인

빌드 성공, 알림 도착, 고객 데이터 반영 검증을 각각 구분한다.