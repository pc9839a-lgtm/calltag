# 콜태그 개발 현황·로드맵

기준일: **2026-08-03**  
저장소: `pc9839a-lgtm/calltag`  
개발 브랜치: `agent/calltag-foundation`  
개발 PR: Draft PR `#1`  
현재 Android 버전: **0.40.9**  
versionCode: **57**  
패키지명: `kr.pagero.calltag`

> CallTag `main`에는 사용자 명시 지시 전 병합하지 않는다. 코드 구현, 빌드 성공, 운영 설정, 실기기 E2E 성공을 반드시 구분한다.

---

## 1. 완료 확인

### 에이닷·삼성 전화 실제 수신 메모

2026-08-03 사용자 실기기에서 확인 완료:

- 실제 전화 수신 시 고객명 표시
- 실제 전화 수신 시 최근 메모 표시
- 에이닷 전화 화면 표시
- 삼성 전화 화면 표시

아직 남은 경계 조건:

- 미저장 번호
- 이름없는고객
- Google·삼성 동일 번호 결합
- 기능 해제 후 콜태그 연락처만 제거
- 메모 수정 후 5초 이내 다음 수신 반영

---

## 2. 현재 핵심 기능

### 전화 수신 메모

- 콜태그 전용 RawContact를 동일 번호의 원본 연락처와 결합
- Google·삼성 원본 연락처 직접 수정·삭제 없음
- `고객명 · 최근 메모` 표시
- 최근 메모 최대 16자
- 앱 사용 중 5초 동기화

### 통화 종료 정리

- 통화 종료 후 중앙 소형 팝업
- 가로 최대 420dp, 세로 최대 560dp, 최소 높이 300dp
- 내부 스크롤과 하단 저장 버튼 고정
- 바깥 터치 종료 차단
- 동일 통화 중복 실행 차단
- 사용자 선택 전 자동 종료 없음
- Android 11 이상 키보드 높이 대응

### 페이지로 문의 자동등록

- 콜태그 로그인 세션으로 미처리 문의 큐 조회
- 전화번호 기준 신규 고객 생성 또는 기존 고객 갱신
- 문의를 고객 메모와 `PAGERO_INQUIRY` 상담이력으로 저장
- eventId receipt + 서버 ACK 중복방지
- 문의 반영 후 연락처 고객명·최근 메모 즉시 동기화

### v0.40.9 실시간 문의 알림

- 개인정보 없는 `pagero_lead_available` FCM 신호 수신
- 신호 수신 직후 문의 강제 동기화
- 실제 고객 DB 반영 후에만 `페이지로 문의 접수` 알림
- 신규·기존 고객 반영 건수 안내
- 동기화 중 추가 푸시가 오면 강제 재동기화 1회 예약
- 실시간 연결 전 앱 전면 30초 보조 동기화
- 실시간 연결 후 앱 전면 5분 누락 안전 확인
- 백그라운드 무한 폴링 없음
- 사용자 화면에 Firebase·토큰·서버 내부 용어 미노출

---

## 3. 페이지로 서버 반영 상태

실시간 푸시 전용 서버 PR `pc9839a-lgtm/inlet#56`을 `main`에 병합했다.

- 서버 병합 SHA: `2f016e152f4fb589fb948db6c5a92488591843f2`
- `/api/call/push/register`
- `/api/call/push/status`
- `/api/call/push/unregister`
- `/api/leads` 저장 후 FCM HTTP v1 데이터 신호
- 프로젝트 ownerId 호환 조회
- 만료·잘못된 토큰 자동 비활성화
- 푸시 실패와 문의 저장 성공 분리
- D1 migration `0008_calltag_realtime_push.sql`
- 고객 개인정보는 FCM payload에 포함하지 않음

Google 로그인 변경이 함께 있는 서버 PR `#48`은 Draft로 유지했다.

서버 정본 문서:

- `pc9839a-lgtm/inlet/docs/CALLTAG_PAGERO_REALTIME_PUSH_KO.md`

---

## 4. v0.40.9 검증

### Android

- PR `#36`을 `agent/calltag-foundation`에 병합
- 앱 병합 SHA: `ec37673e76e0145fb2db0665b1a83562d2ee5092`
- Workflow Run ID: `30821634434`
- Job ID: `91712722719`
- Android 리소스 처리: 성공
- Java 컴파일: 성공
- Manifest 병합: 성공
- Debug APK 패키징: 성공
- Artifact ID: `8859117965`
- Artifact ZIP digest: `sha256:448d3f52c0bfb5dc50ab5abc481ca2e260832bd94fd45657025bcd7d957efa04`
- APK SHA-256: `003a6e1ed3ab704de050fff30f35b47b187f34011acb9ed1c064ebf339b8f4e9`
- APK 크기: `4,461,827 bytes`

### 페이지로 서버

- Validate Pagero CallTag Bridge Run ID: `30822112193`
- JavaScript syntax: 성공
- Bridge contract: 성공
- Pages Functions regression: 성공
- Production build: 성공
- 전체 QA Run ID: `30822112885`
- Full offline QA: 성공
- form·editor·landing·template mobile 브라우저 회귀: 성공

---

## 5. 현재 운영 제한

v0.40.9 APK의 BuildConfig 정적 확인 결과 Firebase Android 값은 모두 빈 문자열이다.

- `FIREBASE_APPLICATION_ID`
- `FIREBASE_API_KEY`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_SENDER_ID`

현재 확정된 범위:

- 앱 실행·재진입 문의 동기화
- 앱을 열어둔 동안 30초 보조 동기화
- 실제 DB 반영 후 알림 로직
- 동일 문의 중복방지

아직 확정되지 않은 범위:

- 앱 종료 상태 즉시 알림
- 잠금화면 즉시 알림
- Firebase 기기 토큰 서버 등록
- 운영 서버 FCM 실제 발송
- 알림 터치 후 고객·상담이력 E2E

운영에 필요한 값:

CallTag GitHub Actions Secret:

- `CALLTAG_FIREBASE_APPLICATION_ID`
- `CALLTAG_FIREBASE_API_KEY`
- `CALLTAG_FIREBASE_PROJECT_ID`
- `CALLTAG_FIREBASE_SENDER_ID`

페이지로 Cloudflare 환경 변수:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

D1:

- `migrations/0008_calltag_realtime_push.sql` 운영 적용 확인

---

## 6. 남은 패치 우선순위

### P0 — 페이지로 운영 실시간 E2E

1. Android Firebase Secret 4개 등록
2. Firebase 값이 포함된 APK 재빌드
3. Cloudflare Firebase 서비스 계정 3개 등록
4. D1 migration 운영 적용 확인
5. 실제 페이지로 문의 제출
6. 앱 종료·백그라운드·잠금화면 알림 확인
7. 알림 터치 후 신규 고객·메모·`PAGERO_INQUIRY` 확인
8. 동일 문의 재전송 중복 미생성 확인
9. 빠른 연속 문의 3건 전부 반영 확인

### P0 — 통화 종료 팝업 실기기 회귀

1. 통화 종료 후 자동 실행
2. 손대지 않고 30초 이상 유지
3. 에이닷·삼성 종료 화면에 다시 밀리지 않음
4. 잠금·홈·다른 앱 사용 중 실행
5. 저장·닫기·제외 전 자동 종료 없음

### P1 — 페이지로 연결 UX

1. 로그인 직후 페이지로 연결 상태 확인
2. 미연결 계정의 더보기 연결 안내
3. 알림 터치 시 고객 탭 또는 해당 고객 바로 이동
4. 문의 유입 통계와 실제 고객 등록 수 비교
5. 문의 접수 후속 문자 자동화 정책 확정

### P1 — 전화·앱 UX 경계 조건

1. 미저장 번호·이름없는고객·동일 번호 결합
2. 기능 해제 원본 복원
3. 오프라인 8초 이내 앱 진입
4. 작은 화면·큰 글자·키보드 팝업
5. 사용자 화면 테스트·진단·임시 UI 추가 점검

### P2 — 문자·캠페인 실기기 검증

- SIM 1개·2개
- 단문·장문·분할 문자
- 이미지 문자
- 발송 제외·중복방지
- 예약 발송·재부팅 복구
- 캠페인 일시정지·재개·취소·실패 안전장치

### P3 — 결제·출시

- Play Billing·영수증 검증·환불·복원
- 일반 계정 프로젝트 1개 제한·운영자 무제한
- 릴리스 서명·AAB
- Play Console 권한·개인정보 문서 일치
- Crash·ANR·500명 고객 성능

---

## 7. 사용자 화면 노출 기준

사용자 화면에 노출하지 않는다.

- 테스트·데모·디버그·진단 원문
- Firebase·FCM 토큰·서비스 계정
- RawContact·Provider·CallScreeningService
- 브랜치·커밋·빌드 상태
- 서버 응답 원문·개발 예외명

사용자에게는 현재 기능 상태, 필요한 조치, 처리된 문의 건수, 데이터 보존 여부만 표시한다.

---

## 8. 절대 지켜야 할 규칙

- CallTag 앱은 `pc9839a-lgtm/calltag`에서 작업한다.
- 앱 개발 정본은 `agent/calltag-foundation`이다.
- 사용자 지시 전 CallTag `main`에 병합하지 않는다.
- CallTag PR `#1`은 Draft 상태를 유지한다.
- 기존 고객·메모·문자·일정·캠페인 데이터를 초기화하지 않는다.
- 원본 Google·삼성 연락처를 직접 수정하거나 삭제하지 않는다.
- FCM payload에 고객명·전화번호·이메일·문의 내용·메모를 넣지 않는다.
- 푸시 실패가 페이지로 문의 접수를 실패시키지 않게 한다.
- 알림은 실제 고객 DB 반영 후에만 표시한다.
- 빌드 성공을 운영 실시간 알림 E2E 성공으로 표현하지 않는다.
