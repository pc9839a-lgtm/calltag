# 콜태그 개발 현황·로드맵

기준일: **2026-08-04**  
저장소: `pc9839a-lgtm/calltag`  
개발 정본: `agent/calltag-foundation`  
개발 PR: Draft PR `#1`  
현재 패치 버전: **0.41.1**  
versionCode: **59**  
패키지명: `kr.pagero.calltag`

> 사용자 명시 지시 전 CallTag `main`에는 병합하지 않는다. 코드 구현·빌드·운영 설정·실기기 확인을 구분한다.

## 1. 사용자 실기기 확인 완료

전화 수신:

- 에이닷 실제 수신에서 고객명·최근 메모 표시
- 삼성 전화 실제 수신에서 고객명·최근 메모 표시

페이지로 문의:

- 실제 문의 접수 후 콜태그 알림 수신
- 앱 완전 종료 상태 알림 수신
- 잠금화면 알림 수신
- 알림 후 고객 반영 흐름 확인
- 중복 문의 처리 등 후처리 동작 확인

사용자 확인 범위를 넘어 강제 종료·네트워크 단절 같은 장애 상황까지 검증 완료로 표현하지 않는다.

## 2. 서버 운영 준비 완료

- 페이지로 서버 PR `pc9839a-lgtm/inlet#56` main 병합 완료
- Cloudflare `inlet` Production Firebase 변수 3개 정상
- D1 `inlet-prod` migration `0008_calltag_realtime_push.sql` 적용 완료
- `calltag_push_devices` 테이블 정상
- 운영 readiness `ready=true`
- Readiness Run ID `30871387043`
- Readiness Job ID `91875065527`

## 3. Android Firebase·페이지로 실시간 연결 완료

- CallTag GitHub Actions Secret 4개 configured
- 생성된 BuildConfig Firebase 필드 4개 configured
- Firebase 검증 PR `#37` 개발 정본 병합 완료
- v0.41.0 알림 이동·연속 문의 처리 PR `#38` 개발 정본 병합 완료
- 실제 운영 FCM 발송·앱 종료·잠금화면 수신 확인
- 한 고객 문의 알림은 고객 상세로 이동
- 여러 고객 문의는 방금 접수된 고객 목록으로 이동
- 전화번호 기준 고객 생성·갱신
- 고객 메모와 페이지로 문의 상담이력 저장
- 같은 문의 반복 처리 방지

## 4. v0.41.1 사용자 문구·페이지로 연결 화면 개선 완료

- PR `#39`을 `agent/calltag-foundation`에 병합
- 병합 SHA: `0e7059fefee09f71260038ffc1c9bc3421f97829`
- CallTag `main`: 미병합

페이지로 화면:

- `지금 동기화` → `새 문의 확인`
- `동기화 중` → `문의 확인 중`
- `실시간 서버 확인 중` → `새 문의 알림 준비 중`
- `이 기기 실시간 등록 필요` 제거
- Firebase·토큰·서버·내부 오류 코드 노출 제거
- 연결 계정·연결 여부·알림 여부·마지막 확인만 표시
- 문의가 콜태그에 들어오는 3단계 안내 추가

더보기 화면:

- `앱 진단` 메뉴 제거
- `페이지로 문의 연결`로 메뉴명 명확화
- 문자·고객·백업 메뉴 설명을 실제 사용 목적 중심으로 변경

권한·계정 화면:

- Android 권한창·미허용·SMS 같은 표현 제거
- 회원탈퇴·회원정보 확인 실패 시 내부 오류 원문 노출 제거
- `회원정보 다시 불러오기`로 표현 정리

재발 방지:

- GitHub Actions 사용자 문구 검사 추가
- 페이지로 화면에 개발 용어가 다시 들어오면 빌드 실패
- 앱 진단 메뉴가 다시 연결되면 빌드 실패
- 권한·계정 화면에서 내부 표현이나 예외 원문이 다시 노출되면 빌드 실패

빌드:

- Run ID: `30885474095`
- Job ID: `91915611094`
- Artifact ID: `8882921194`
- Artifact ZIP digest: `sha256:96861356be9cbe9da02d5f3c56513a2695d498253489e0433a859910ecd83aa4`
- APK SHA-256: `586e49fc25f9b64e24c05b61eedfc1eade2e9855852a5cf9fe236a0438e53e10`
- APK 크기: `4,461,863 bytes`

통과:

- Firebase 빌드 설정
- 사용자 문구 검사
- 페이지로 기존 기능 회귀검사
- Java·리소스·Manifest·APK 패키징
- Firebase BuildConfig 확인

상세 문서:

- `docs/V0411_USER_COPY_PAGERO_UX_KO.md`
- `docs/V0410_PAGERO_NOTIFICATION_ROUTING_INTEGRITY_KO.md`
- `docs/V0409_PAGERO_REALTIME_ALERT_KO.md`
- `docs/FIREBASE_REGISTRATION_GUIDE_KO.md`

## 5. 다음 우선순위

P0 통화 종료 팝업 실기기 회귀:

- 통화 종료 후 자동 실행
- 30초 이상 유지
- 에이닷·삼성 종료 화면에 밀리지 않음
- 잠금·홈·다른 앱 사용 중 실행
- 저장·닫기·제외 전 자동 종료 없음

P1 사용자 화면 전수 점검:

- 남은 테스트·진단·임시 버튼 제거
- 사용자에게 노출되는 원문 오류 제거
- 권한 안내를 사용 목적 중심으로 통일
- 작은 화면·큰 글자·키보드 상태 점검
- 페이지로 유입 통계와 실제 등록 수 비교
- 문의 후속 문자 자동화 정책

P2·P3:

- SIM 1개·2개 문자 발송 회귀
- 단문·장문·분할·이미지 문자
- 예약 발송·재부팅 복구
- 캠페인 중단·재개·실패 안전장치
- Play Billing·AAB·릴리스 서명·Play Console QA

## 6. 안전 규칙

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
