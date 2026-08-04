# 콜태그 개발 현황·로드맵

기준일: **2026-08-04**  
저장소: `pc9839a-lgtm/calltag`  
개발 브랜치: `agent/calltag-foundation`  
개발 PR: Draft PR `#1`  
현재 Android 버전: **0.40.9**  
versionCode: **57**  
패키지명: `kr.pagero.calltag`

> CallTag `main`에는 사용자 명시 지시 전 병합하지 않는다. 코드 구현·빌드·운영 설정·실기기 E2E를 구분한다.

## 1. 완료 확인

2026-08-03 사용자 실기기 확인:

- 에이닷 실제 전화 수신에서 고객명·최근 메모 표시
- 삼성 전화 실제 전화 수신에서 고객명·최근 메모 표시

남은 전화 수신 경계 조건:

- 미저장 번호
- 이름없는고객
- Google·삼성 동일 번호 결합
- 기능 해제 시 콜태그 연락처만 제거
- 메모 수정 후 5초 이내 반영

## 2. 페이지로 문의 연동 구현 상태

Android v0.40.9:

- 미처리 문의 큐 조회
- 전화번호 기준 신규 고객 생성·기존 고객 갱신
- 고객 메모와 `PAGERO_INQUIRY` 상담이력 저장
- eventId receipt + ACK 중복방지
- FCM 신호 수신 즉시 강제 동기화
- 고객 DB 반영 완료 후에만 알림
- 동기화 중 추가 신호 재동기화 1회 예약
- 앱 전면 30초 보조 동기화

페이지로 서버:

- `pc9839a-lgtm/inlet#56` main 병합 완료
- `/api/call/push/register`
- `/api/call/push/status`
- `/api/call/push/unregister`
- `/api/leads` 저장 후 FCM HTTP v1 신호
- 개인정보 없는 payload
- 만료 토큰 자동 비활성화
- 푸시 실패와 문의 접수 성공 분리

## 3. Firebase·D1 운영 서버 준비 완료

2026-08-04 최신 Cloudflare Production 배포와 운영 D1 `inlet-prod`를 실제 조회했다.

- 확인 배포: `https://89a7a596.inlet-8mr.pages.dev`
- Readiness endpoint: `/api/call/push/readiness`
- Workflow Run ID: `30871387043`
- Job ID: `91875065527`
- 확인 시각: `2026-08-04T02:26:12.061Z`

| 항목 | 결과 |
|---|---|
| `FIREBASE_PROJECT_ID` | true |
| `FIREBASE_CLIENT_EMAIL` | true |
| `FIREBASE_PRIVATE_KEY` | true |
| Firebase configured | true |
| D1 `DB` 바인딩 | true |
| `calltag_push_devices` 테이블 | true |
| 최종 ready | true |

판정:

- Cloudflare `inlet` Production 서버용 Firebase 변수 3개 등록 완료
- D1 `migrations/0008_calltag_realtime_push.sql` 적용 완료
- `calltag_push_devices` 테이블과 조회 인덱스 생성 완료
- 페이지로 서버는 FCM 발송 준비 완료
- 비공개 키 원문은 로그·문서·응답에 노출하지 않음

## 4. CallTag Android Firebase 설정·빌드 완료

사용자가 CallTag GitHub Actions Secrets에 Android용 Firebase 값 4개를 이미 등록한 상태임을 실제 빌드로 확인했다.

검증 PR:

- PR `#37`을 `agent/calltag-foundation`에 병합
- 병합 SHA: `9b8318c606f08674aca9cbd20ac9f0cacf52e202`

검증 Build:

- Workflow Run ID: `30872373416`
- Job ID: `91876823885`
- `CALLTAG_FIREBASE_APPLICATION_ID`: configured
- `CALLTAG_FIREBASE_API_KEY`: configured
- `CALLTAG_FIREBASE_PROJECT_ID`: configured
- `CALLTAG_FIREBASE_SENDER_ID`: configured
- 생성된 `BuildConfig.FIREBASE_APPLICATION_ID`: configured
- 생성된 `BuildConfig.FIREBASE_API_KEY`: configured
- 생성된 `BuildConfig.FIREBASE_PROJECT_ID`: configured
- 생성된 `BuildConfig.FIREBASE_SENDER_ID`: configured
- Java·리소스·Manifest·Debug APK 빌드 성공

검증 APK:

- Artifact ID: `8878338508`
- Artifact ZIP digest: `sha256:41f23f1e483308ed7f3c02af8571ccba1a95d388494936df5624f9197f6796dd`
- APK SHA-256: `2fb039d9782dedc01abefa02507dd2b5a7401867e5fd0862804c12cd6c101719`
- APK 크기: `4,461,827 bytes`

앞으로 Firebase Secret 4개 중 하나라도 비어 있거나 생성된 BuildConfig에 값이 들어가지 않으면 APK 빌드가 실패하도록 CI 검증을 추가했다.

## 5. 현재 확정·미확정 범위

확정:

- 페이지로 서버 Firebase HTTP v1 발송 준비
- 운영 D1 기기 토큰 저장 테이블
- Firebase 설정이 포함된 CallTag v0.40.9 APK 빌드
- 문의 저장과 푸시 실패 분리
- 개인정보 없는 신호 payload
- 앱 실행·재진입 문의 동기화
- 앱 전면 30초 보조 동기화
- 실제 고객 DB 반영 후 알림 로직
- 동일 문의 중복방지

실기기 미확정:

- Firebase 기기 토큰 운영 서버 등록
- 앱 완전 종료 상태 즉시 알림
- 백그라운드·잠금화면 즉시 알림
- 실제 운영 FCM 발송·수신 E2E
- 알림 터치 후 고객·상담이력 E2E

## 6. P0 — 실기기 페이지로 문의 알림 E2E

1. 새 Firebase 설정 포함 APK를 기존 앱 위에 덮어 설치
2. 콜태그 로그인
3. Android 알림 권한 허용
4. 앱을 한 번 실행해 FCM 토큰 발급·서버 등록
5. `calltag_push_devices`에 활성 기기 등록 확인
6. 실제 페이지로 문의 1건 제출
7. 앱 실행 중 즉시 알림 확인
8. 앱 백그라운드 상태 알림 확인
9. 앱 완전 종료 상태 알림 확인
10. 잠금화면 알림 확인
11. 알림 터치 후 고객·메모·`PAGERO_INQUIRY` 확인
12. 동일 eventId 중복 미생성 확인
13. 빠른 연속 문의 3건 전부 반영 확인

## 7. P0 — 통화 종료 팝업 실기기 회귀

- 통화 종료 후 자동 실행
- 30초 이상 유지
- 에이닷·삼성 종료 화면에 밀리지 않음
- 잠금·홈·다른 앱 사용 중 실행
- 저장·닫기·제외 전 자동 종료 없음

## 8. P1

페이지로 UX:

- 로그인 직후 연결 상태 확인
- 미연결 계정 더보기 안내
- 알림 터치 시 고객 탭 또는 해당 고객 바로 이동
- 유입 통계와 실제 등록 수 비교
- 문의 후속 문자 자동화 정책

앱 경계 조건:

- 미저장 번호·이름없는고객·동일 번호 결합
- 기능 해제 원본 보존
- 오프라인 8초 이내 진입
- 작은 화면·큰 글자·키보드 팝업
- 사용자 화면의 테스트·진단·임시 UI 추가 점검

## 9. P2·P3

P2:

- SIM 1개·2개 문자 발송
- 단문·장문·분할·이미지 문자
- 발송 제외·중복방지
- 예약 발송·재부팅 복구
- 캠페인 일시정지·재개·취소·실패 안전장치

P3:

- Play Billing·영수증 검증·환불·복원
- 일반 계정 프로젝트 1개 제한·운영자 무제한
- 릴리스 서명·AAB
- Play Console 권한·개인정보 문서 일치
- Crash·ANR·500명 고객 성능

## 10. 절대 지켜야 할 규칙

- CallTag 앱 개발 정본은 `agent/calltag-foundation`
- 사용자 지시 전 CallTag `main` 미병합
- PR `#1` Draft 유지
- 기존 고객·메모·문자·일정·캠페인 데이터 초기화 금지
- 원본 Google·삼성 연락처 직접 수정·삭제 금지
- FCM payload에 고객 개인정보 포함 금지
- Firebase 서비스 계정 비공개 키 공개 금지
- 푸시 실패가 페이지로 문의 접수를 실패시키지 않게 유지
- 알림은 실제 고객 DB 반영 후 표시
- 빌드 성공과 실기기 E2E 완료를 구분