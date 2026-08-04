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

상세 문서:

- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`
- `docs/V0409_PAGERO_REALTIME_ALERT_KO.md`
- 서버 `pc9839a-lgtm/inlet/docs/CALLTAG_PAGERO_REALTIME_PUSH_KO.md`

## 4. 현재 동작 가능한 범위

서버 확정:

- Firebase HTTP v1 발송 설정
- 운영 D1 기기 토큰 저장 테이블
- 문의 저장과 푸시 실패 분리
- 개인정보 없는 신호 payload
- 잘못된·만료 토큰 비활성화

앱에서 이미 확정된 범위:

- 앱 실행·재진입 문의 동기화
- 앱을 열어둔 동안 최대 약 30초 보조 동기화
- 실제 고객 DB 반영 후 알림 로직
- 동일 문의 중복방지

아직 미확정:

- 앱 완전 종료 상태 즉시 알림
- 잠금화면 즉시 알림
- Firebase 기기 토큰 운영 등록
- 운영 FCM 실제 발송 E2E
- 알림 터치 후 고객·상담이력 E2E

현재 v0.40.9 APK는 Firebase Android BuildConfig 4개가 빈 값이다.

## 5. P0 — Android Firebase 빌드·실기기 E2E

1. CallTag GitHub Actions Secret 4개 등록
   - `CALLTAG_FIREBASE_APPLICATION_ID`
   - `CALLTAG_FIREBASE_API_KEY`
   - `CALLTAG_FIREBASE_PROJECT_ID`
   - `CALLTAG_FIREBASE_SENDER_ID`
2. Firebase 값 포함 APK 재빌드
3. APK 내부 BuildConfig 4개 값 비어 있지 않음 확인
4. 기존 앱에 덮어 설치
5. 로그인 후 알림 권한 허용
6. FCM 기기 토큰이 `calltag_push_devices`에 등록되는지 확인
7. 실제 페이지로 문의 1건 제출
8. 앱 종료·백그라운드·잠금화면 알림 확인
9. 알림 터치 후 고객·메모·`PAGERO_INQUIRY` 확인
10. 동일 eventId 중복 미생성 확인
11. 빠른 연속 문의 3건 전부 반영 확인

## 6. P0 — 통화 종료 팝업 실기기 회귀

- 통화 종료 후 자동 실행
- 30초 이상 유지
- 에이닷·삼성 종료 화면에 밀리지 않음
- 잠금·홈·다른 앱 사용 중 실행
- 저장·닫기·제외 전 자동 종료 없음

## 7. P1

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

## 8. P2·P3

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

## 9. 절대 지켜야 할 규칙

- CallTag 앱 개발 정본은 `agent/calltag-foundation`
- 사용자 지시 전 CallTag `main` 미병합
- PR `#1` Draft 유지
- 기존 고객·메모·문자·일정·캠페인 데이터 초기화 금지
- 원본 Google·삼성 연락처 직접 수정·삭제 금지
- FCM payload에 고객 개인정보 포함 금지
- Firebase 서비스 계정 비공개 키 공개 금지
- 푸시 실패가 페이지로 문의 접수를 실패시키지 않게 유지
- 알림은 실제 고객 DB 반영 후 표시
- 서버 `ready=true`와 앱 실기기 E2E 완료를 구분
