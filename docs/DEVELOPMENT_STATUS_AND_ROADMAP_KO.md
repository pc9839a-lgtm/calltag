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

## 3. Firebase 운영 확인 결과

2026-08-04 최신 Cloudflare Production 배포를 실제 조회했다.

- Readiness endpoint: `/api/call/push/readiness`
- Workflow Run ID: `30870665532`
- Job ID: `91871834819`
- 확인 시각: `2026-08-04T02:04:19.406Z`

| 항목 | 결과 |
|---|---|
| `FIREBASE_PROJECT_ID` | false |
| `FIREBASE_CLIENT_EMAIL` | false |
| `FIREBASE_PRIVATE_KEY` | false |
| Firebase configured | false |
| D1 `DB` 바인딩 | true |
| `calltag_push_devices` 테이블 | false |
| 최종 ready | false |

판정:

- Cloudflare 화면 입력 완료 주장과 실제 Production 런타임 상태가 일치하지 않는다.
- 다른 프로젝트 또는 Preview에 등록했거나, 저장 후 Production 재배포가 빠졌을 가능성이 높다.
- 운영 실시간 푸시는 아직 완료가 아니다.

상세 조치:

- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`
- `docs/V0409_PAGERO_REALTIME_ALERT_KO.md`

## 4. 현재 동작 가능한 범위

확정:

- 앱 실행·재진입 문의 동기화
- 앱을 열어둔 동안 최대 약 30초 보조 동기화
- 실제 고객 DB 반영 후 알림 로직
- 동일 문의 중복방지

미확정:

- 앱 완전 종료 상태 즉시 알림
- 잠금화면 즉시 알림
- Firebase 기기 토큰 운영 등록
- 운영 FCM 실제 발송
- 알림 터치 후 고객·상담이력 E2E

현재 v0.40.9 APK도 Firebase Android BuildConfig 4개가 빈 값이다.

## 5. P0 — Firebase 운영 복구

1. Cloudflare `Workers & Pages > inlet` 확인
2. `Settings > Variables and Secrets`
3. Environment를 **Production**으로 선택
4. 아래 세 변수 이름·값 재확인
   - `FIREBASE_PROJECT_ID`
   - `FIREBASE_CLIENT_EMAIL`
   - `FIREBASE_PRIVATE_KEY`
5. 저장 후 Production 재배포
6. readiness 재검사에서 Firebase 항목 모두 true 확인
7. D1 `inlet-prod`에 `0008_calltag_realtime_push.sql` 적용
8. `d1.pushDevicesTable=true` 확인
9. 최종 `ready=true` 확인
10. CallTag GitHub Firebase Secret 4개 등록
11. Firebase 값 포함 APK 재빌드
12. APK 정적 확인 후 덮어 설치
13. 실제 문의 → 종료·잠금화면 알림 E2E
14. 고객·메모·상담이력·중복방지 확인

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
- 화면 입력·빌드 성공을 운영 E2E 완료로 표현하지 않음
