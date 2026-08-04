# 콜태그 Firebase 등록·운영 설정 가이드

기준일: **2026-08-04**  
대상 앱: **콜태그 Android**  
Android 패키지명: **`kr.pagero.calltag`**

## 1. 목적

페이지로 문의 접수 사실을 앱 종료·백그라운드·잠금화면에서도 즉시 전달하기 위해 Firebase Cloud Messaging(FCM)을 사용한다.

콜태그에는 다음 코드가 이미 구현되어 있다.

- FCM 기기 토큰 발급·서버 등록
- `pagero_lead_available` 데이터 신호 수신
- 문의 큐 즉시 동기화
- 전화번호 기준 고객 생성·갱신
- 고객 DB 반영 완료 후 알림 표시
- `eventId`와 ACK 기반 중복방지

Firebase는 고객 DB 저장 용도가 아니라 `새 문의가 있음`이라는 개인정보 없는 신호 전달에만 사용한다. FCM은 무료로 사용할 수 있다.

## 2. Firebase Android 앱 등록

Firebase Console에서 운영 프로젝트를 생성하거나 선택한다.

1. `프로젝트 개요 > 앱 추가 > Android`
2. Android 패키지 이름: **`kr.pagero.calltag`**
3. 앱 닉네임: `콜태그` 또는 `CallTag`
4. FCM만 사용할 때 SHA-1은 생략 가능
5. 앱 등록 완료
6. `google-services.json` 다운로드
7. `프로젝트 설정 > 클라우드 메시징`에서 FCM HTTP v1 사용 상태 확인

패키지명은 기존 콜링크나 페이지로 웹 앱 값이 아니라 정확히 `kr.pagero.calltag`를 사용한다.

## 3. CallTag GitHub Actions Secret

`google-services.json`에서 다음 값을 찾는다.

| GitHub Secret | JSON 위치 |
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

4개를 각각 등록한 뒤 APK를 다시 빌드해야 한다. 기존 APK에는 나중에 등록한 값이 자동으로 들어가지 않는다.

## 4. 서버 서비스 계정 발급

Firebase Console에서:

1. `프로젝트 설정`
2. `서비스 계정`
3. `Firebase Admin SDK`
4. `새 비공개 키 생성`
5. JSON 파일 다운로드

Cloudflare 환경변수 대응:

| Cloudflare 변수 | 서비스 계정 JSON 값 |
|---|---|
| `FIREBASE_PROJECT_ID` | `project_id` |
| `FIREBASE_CLIENT_EMAIL` | `client_email` |
| `FIREBASE_PRIVATE_KEY` | `private_key` 전체 값 |

`FIREBASE_PRIVATE_KEY`는 `BEGIN PRIVATE KEY`부터 `END PRIVATE KEY`까지 전체를 사용한다. 실제 줄바꿈 또는 JSON의 `\n` 형식 모두 서버 코드에서 처리한다.

절대 금지:

- 서비스 계정 JSON을 GitHub에 커밋
- 비공개 키를 APK·BuildConfig에 포함
- 채팅·문서·스크린샷에 전체 키 노출

## 5. Cloudflare Production 등록 위치

반드시 다음 프로젝트와 환경에 등록한다.

- Cloudflare 프로젝트: **`inlet`**
- 경로: `Workers & Pages > inlet > Settings > Variables and Secrets`
- Environment: **Production**

등록할 이름:

```text
FIREBASE_PROJECT_ID
FIREBASE_CLIENT_EMAIL
FIREBASE_PRIVATE_KEY
```

확인 사항:

- 변수 이름의 앞뒤 공백 없음
- 대문자와 밑줄까지 정확히 일치
- `FIREBASE_PRIVATE_KEY`는 Secret으로 저장 권장
- Preview에만 등록하지 않음
- 저장 후 반드시 새 Production 배포 실행

환경변수는 이미 배포된 버전에 소급 적용되지 않으므로 저장 후 `Deployments`에서 재배포하거나 `main` 배포를 다시 실행해야 한다.

## 6. 2026-08-04 실제 운영 확인 결과

최신 Production 배포 주소의 `/api/call/push/readiness`를 GitHub Actions에서 직접 호출했다.

검증 Run:

- Workflow: `Verify CallTag Push Readiness`
- Run ID: `30870665532`
- Job ID: `91871834819`
- 확인 시각: `2026-08-04T02:04:19.406Z`

결과:

| 확인 항목 | 결과 |
|---|---|
| `FIREBASE_PROJECT_ID` | 미인식 |
| `FIREBASE_CLIENT_EMAIL` | 미인식 |
| `FIREBASE_PRIVATE_KEY` | 미인식 |
| Firebase 전체 configured | false |
| D1 `DB` 바인딩 | 정상 |
| `calltag_push_devices` 테이블 | 없음 |
| 최종 `ready` | false |

즉, 사용자가 값을 입력했더라도 현재 `inlet` Production 배포에서는 세 값이 전달되지 않고 있다.

가능성이 높은 원인:

1. 다른 Cloudflare Pages 프로젝트에 등록
2. Preview 환경에만 등록
3. 변수 이름 오타 또는 앞뒤 공백
4. 저장 후 Production 재배포 미실행
5. `FIREBASE_PRIVATE_KEY` 값이 비어 있는 상태로 저장

## 7. 지금 다시 해야 할 작업

1. Cloudflare `Workers & Pages > inlet` 진입
2. `Settings > Variables and Secrets`
3. 환경을 **Production**으로 선택
4. 세 변수 이름과 값 재확인
5. 저장
6. `Deployments`에서 Production 재배포
7. 운영 준비상태 검사 재실행
8. 결과가 모두 `true`인지 확인

정상 목표:

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

실제 값은 반환하지 않고 설정 유무만 boolean으로 확인한다.

## 8. D1 migration

운영 D1 데이터베이스:

- binding: `DB`
- database name: `inlet-prod`
- migration: `migrations/0008_calltag_realtime_push.sql`

현재 D1 바인딩은 정상이나 `calltag_push_devices` 테이블은 존재하지 않는다. migration 적용 또는 첫 정상 기기 등록 전 서버 스키마 생성이 필요하다.

생성 대상:

- `calltag_push_devices` 테이블
- ownerId + deviceId 유일 제약
- FCM token 유일 제약
- 활성 기기 조회 인덱스

## 9. 서버 준비 후 Android 작업

서버 `ready=true` 확인 후:

1. CallTag GitHub Secret 4개 등록 확인
2. APK 재빌드
3. APK 내부 Firebase 값이 비어 있지 않은지 정적 확인
4. 기존 앱 삭제 없이 덮어 설치
5. 로그인
6. Android 알림 권한 허용
7. 앱을 한 번 열어 FCM 토큰 등록
8. 페이지로 문의 실제 접수
9. 앱 종료·백그라운드·잠금화면 알림 확인
10. 고객·메모·`PAGERO_INQUIRY` 상담이력 확인
11. 동일 문의 중복 미생성 확인

## 10. 운영 완료 기준

다음이 모두 확인되어야 완료로 처리한다.

- 서버 readiness `ready=true`
- Firebase 값이 포함된 APK 빌드
- 기기 토큰 서버 등록
- 실제 문의 후 백그라운드 알림 도착
- 알림 터치 후 고객 자동등록 확인
- 고객 개인정보가 푸시 payload에 없음
- 동일 eventId 중복 고객·상담이력 없음

코드 빌드 성공이나 Cloudflare 화면 입력만으로 운영 완료로 판정하지 않는다.
