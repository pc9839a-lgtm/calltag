# 콜태그 개발 현황·로드맵

기준일: **2026-08-04**  
저장소: `pc9839a-lgtm/calltag`  
개발 정본: `agent/calltag-foundation`  
개발 PR: Draft PR `#1`  
현재 패치 버전: **0.41.0**  
versionCode: **58**  
패키지명: `kr.pagero.calltag`

> 사용자 명시 지시 전 CallTag `main`에는 병합하지 않는다. 코드 구현·빌드·운영 설정·실기기 확인을 구분한다.

## 1. 사용자 실기기 확인 완료

전화 수신:

- 에이닷 실제 수신에서 고객명·최근 메모 표시
- 삼성 전화 실제 수신에서 고객명·최근 메모 표시

페이지로 문의 알림:

- 실제 페이지로 문의 접수 후 콜태그 알림 수신
- 앱 완전 종료 상태에서 알림 수신
- 잠금화면에서 알림 수신

위 결과로 Firebase Android 초기화, FCM 기기 등록, 페이지로 서버 발송, 종료·잠금 상태 수신 경로가 기능적으로 동작함을 확인했다.

## 2. 서버 운영 준비 완료

- 페이지로 서버 PR `pc9839a-lgtm/inlet#56` main 병합 완료
- Cloudflare `inlet` Production Firebase 변수 3개 정상
- D1 `inlet-prod` migration `0008_calltag_realtime_push.sql` 적용 완료
- `calltag_push_devices` 테이블 정상
- 운영 readiness `ready=true`
- Readiness Run ID `30871387043`
- Readiness Job ID `91875065527`

## 3. Android Firebase 빌드 완료

- CallTag GitHub Actions Secret 4개 configured
- 생성된 BuildConfig Firebase 필드 4개 configured
- Firebase 검증 PR `#37` 개발 정본 병합 완료
- 검증 Run ID `30872373416`
- 검증 Job ID `91876823885`

Secret 또는 생성된 BuildConfig 값이 하나라도 비면 APK 빌드를 실패시키도록 CI에서 차단한다.

## 4. v0.41.0 페이지로 알림 이동·배치 처리

패치 브랜치:

- `agent/calltag-v0410-pagero-integrity-routing`
- PR `#38`
- 통합 대상 `agent/calltag-foundation`

변경:

- 한 고객 문의 알림을 누르면 해당 고객 상세로 바로 이동
- 여러 고객 문의가 한 동기화에서 반영되면 `방금 접수된 문의` 목록 표시
- 목록에서 고객명·전화번호·메모 요약 확인 후 고객 상세 이동
- 배치 고객 ID를 `LinkedHashSet`으로 수집해 순서를 유지하고 중복 제거
- 동기화 중 추가 FCM 신호는 `PENDING_FORCE`로 후속 강제 동기화 예약
- `eventId` receipt가 `IMPORTED` 또는 `ACKED`이면 동일 문의를 다시 생성하지 않고 ACK만 재시도
- FCM payload에는 고객명·전화번호·문의 내용·메모를 넣지 않음

CI 회귀검사:

- 배치 고객 ID 수집
- receipt 중복방지
- 동기화 중 추가 신호 재실행
- 알림 고객 ID 전달
- 단일 고객 상세 이동
- 여러 고객 목록 이동
- Manifest 등록

상세 문서:

- `docs/V0410_PAGERO_NOTIFICATION_ROUTING_INTEGRITY_KO.md`
- `docs/V0409_PAGERO_REALTIME_ALERT_KO.md`
- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`

## 5. 현재 확정 범위

- 페이지로 서버 Firebase HTTP v1 발송
- 운영 D1 기기 토큰 저장
- 실제 운영 FCM 발송·Android 수신
- 앱 완전 종료·잠금화면 알림
- 전화번호 기준 고객 생성·갱신
- 고객 메모와 `PAGERO_INQUIRY` 상담이력 저장 코드
- eventId receipt·ACK 중복방지 코드
- 동기화 중 추가 문의 재실행
- Firebase 설정 포함 APK 빌드

## 6. 남은 P0 실기기 확인

v0.41.0 설치 후:

1. 문의 1건 접수 후 알림 터치 시 해당 고객 상세 이동
2. 고객 자동 생성 또는 기존 고객 갱신 확인
3. 문의 내용이 고객 메모에 반영되는지 확인
4. `PAGERO_INQUIRY` 상담이력 생성 확인
5. 서로 다른 번호 문의 3건을 빠르게 접수
6. 알림 터치 시 세 고객이 `방금 접수된 문의` 목록에 표시되는지 확인
7. 동일 eventId 재전송 시 고객·메모·상담이력 중복 미생성 확인

## 7. 다른 우선순위

P0 통화 종료 팝업:

- 통화 종료 후 자동 실행
- 30초 이상 유지
- 에이닷·삼성 종료 화면에 밀리지 않음
- 잠금·홈·다른 앱 사용 중 실행
- 저장·닫기·제외 전 자동 종료 없음

P1:

- 미저장 번호·이름없는고객·동일 번호 결합
- 기능 해제 시 원본 연락처 보존
- 오프라인 로그인 8초 제한
- 작은 화면·큰 글자·키보드 팝업
- 사용자 화면의 테스트·진단·임시 UI 추가 점검
- 페이지로 유입 통계와 실제 등록 수 비교
- 문의 후속 문자 자동화 정책

P2·P3:

- SIM 1개·2개 문자 발송 회귀
- 단문·장문·분할·이미지 문자
- 예약 발송·재부팅 복구
- 캠페인 중단·재개·실패 안전장치
- Play Billing·AAB·릴리스 서명·Play Console QA

## 8. 안전 규칙

- CallTag 앱 개발 정본은 `agent/calltag-foundation`
- 사용자 지시 전 CallTag `main` 미병합
- PR `#1` Draft 유지
- 기존 고객·메모·문자·일정·캠페인 데이터 초기화 금지
- 원본 Google·삼성 연락처 직접 수정·삭제 금지
- FCM payload에 고객 개인정보 포함 금지
- Firebase 서비스 계정 비공개 키 공개 금지
- 푸시 실패가 페이지로 문의 접수를 실패시키지 않게 유지
- 알림은 실제 고객 DB 반영 후 표시
- 사용자 확인 범위보다 넓게 실기기 완료를 주장하지 않음
