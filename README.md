# 콜태그 (CallTag)

전화 전 고객 맥락 확인부터 통화 종료 후 상태·메모·일정·문자 처리, 후속문자와 단체문자까지 연결하는 Android 고객관리 앱입니다.

## 다음 개발자가 먼저 읽을 문서

1. [`docs/GOOGLE_LOGIN_REALTIME_SYNC_V040_KO.md`](docs/GOOGLE_LOGIN_REALTIME_SYNC_V040_KO.md) — Google 로그인·페이지로 계정 매핑·FCM 실시간 문의·수신 메모 복구
2. [`docs/ANDROID_DEVELOPER_HANDOFF_KO.md`](docs/ANDROID_DEVELOPER_HANDOFF_KO.md) — 현재 코드 구조·파일별 역할·안전 규칙·실기기 검수·다음 작업 정본
3. [`docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md) — 최신 구현 상태와 남은 패치
4. [`docs/PAGERO_CUSTOMER_INTEGRATION_KO.md`](docs/PAGERO_CUSTOMER_INTEGRATION_KO.md) — 페이지로 문의 조회·ACK·중복 방지
5. [`docs/DESIGN_SYSTEM_KO.md`](docs/DESIGN_SYSTEM_KO.md) — Android UX/UI 규격
6. [`docs/PRODUCT_SPEC_KO.md`](docs/PRODUCT_SPEC_KO.md) — 제품 정의

기획 문서에 적혀 있다는 이유만으로 구현 완료로 판단하지 않습니다. 코드, 버전, 기능 커밋과 빌드 결과가 확인된 항목만 코드 완료입니다. APK 빌드 성공과 실제 휴대전화 검증 성공도 구분합니다.

## 제품 기준

- 제품명: **콜태그(CallTag)**
- 패키지명: `kr.pagero.calltag`
- 대표 도메인: `https://calltag.pagero.kr`
- 개발 브랜치: `agent/calltag-foundation`
- 개발 PR: Draft PR `#1`
- `main` 병합: 사용자 명시 지시 전 금지

## 현재 Android 상태

- versionName: `0.40.0`
- versionCode: `48`
- 최소 Android: API 26
- compileSdk/targetSdk: 35
- 개발 HEAD: `fb390783560c34be66cc840351d9107553258b94`
- 검증 Workflow: `Validate CallTag Android`
- Run ID: `30754617608`
- Job ID: `91514547627`
- 결과: Android 리소스 처리·Java 컴파일·Firebase Messaging 패키징·Debug APK·아티팩트 업로드 성공
- Artifact ID: `8835543621`
- Artifact ZIP digest: `sha256:c6afc72a401b20e66d1131bc4311fb3958459ce38320a230eabd0d0620b2f975`
- 실제 APK SHA-256: `cd8d3b9b3e2a69029c457cf34570eab911fa6b341be6cc7c4294effe6f7510e2`

임시 검증 PR `#23`, `#24`는 병합하지 않고 닫았습니다. 개발 PR `#1`은 계속 Draft·미병합 상태입니다.

## 0.40.0 핵심 변경

- 이메일/비밀번호 로그인 유지
- 로그인 화면에 `Google로 계속하기` 추가
- 브라우저 OAuth 완료 후 `calltag://auth/google` 딥링크 복귀
- 장기 세션 대신 2분 유효·1회 사용 티켓 교환
- 기존 이메일 계정과 Google 이메일이 같으면 같은 `ownerId` 유지
- 로그인 직후 페이지로 프로젝트 보유 여부 확인
- 페이지로 계정 없음·확인 실패여도 콜태그 로그인 허용
- 페이지로 연결 화면에 계정 연결 상태와 실시간 알림 상태 표시
- 개인정보 없는 FCM 신호 수신 후 페이지로 문의 동기화 시작
- Firebase 운영값이 없으면 `실시간 설정 필요`로 표시하고 기존 동기화 유지
- 전화 수신 오버레이에 `전화번호 · 최근 메모 요약` 복구
- 긴 번호·메모 줄은 최대 2줄, 메모 요약 24자 후 말줄임
- 오버레이 실패 시 대체 수신 알림에도 번호와 메모 표시

## 운영 활성화 전 필수 작업

서버 Draft PR: `pc9839a-lgtm/inlet#48`

- Google OAuth client ID/secret 등록
- redirect URI `https://pagero.kr/api/call/google/callback` 등록
- Firebase 서비스 계정 환경변수 등록
- Android Firebase application ID/API key/project ID/sender ID 등록
- D1 migration 적용
- 서버 PR 검토·배포

운영값이 없는 현재 검증 APK에서는 Google 로그인과 FCM 실전송이 활성화되지 않습니다. 이메일 로그인과 앱 실행·화면 재진입·`지금 동기화` 경로는 유지됩니다.

## 현재 주요 기능

- 통화 수신 고객정보·상태·전화번호·최근 메모 표시
- 통화 종료 후 고객 상태·메모·일정·문자 정리
- 고객·캘린더·홈·통계·더보기 5개 내비게이션
- 고객 상태·일정 종류와 사용자 지정 색상
- 저장형 문자 템플릿·변수 치환·이미지 첨부
- 고객별 문자 허용/비허용
- 문자 발송 제외·중복발송 방지
- 통화 후·부재중·후속 예약 자동문자
- 수동 그룹·스마트 그룹·단체문자 캠페인
- 캠페인 일시정지·재개·취소·안전 복구
- Google 캘린더·삼성 캘린더 등 Android 일정 앱 공유
- 페이지로 고객문의 신규 등록·갱신·ACK·중복 방지
- 암호화 `.ctbackup` 백업·복원
- DB·예약·캠페인 정합성 복구와 진단

## 데이터·발송 안전 규칙

- 기존 데이터를 초기화하지 않습니다.
- DB 변경 시 보존 마이그레이션을 작성합니다.
- 페이지로 데이터와 FCM 기기는 로그인 세션의 `ownerId`로 격리합니다.
- FCM payload에 고객명·전화번호·이메일·문의 내용·메모를 넣지 않습니다.
- 푸시 실패로 고객 문의 저장을 실패시키지 않습니다.
- 발송 직전 허용 여부·발송 제외·중복방지·SIM·캠페인 상태를 다시 검사합니다.
- 불명확한 `SENDING` 작업은 자동 재발송하지 않습니다.
- 일시정지 캠페인은 자동 재개하지 않습니다.
- 누락 작업을 추측해 생성하거나 고아 작업을 자동 발송하지 않습니다.
- 이미지 문자는 시스템 메시지 앱에서 사용자가 최종 전송합니다.
- CSV·XLSX·고객 목록·캠페인 결과 외부 반출을 구현하지 않습니다.
- `.ctbackup` 백업·복원과 일반 데이터 내보내기를 합치지 않습니다.

## 다음 작업 우선순위

1. Google OAuth 운영 설정·D1 migration·Firebase 환경변수·서버 배포
2. 실기기 Google 로그인·페이지로 계정 있음/없음·FCM 신규 문의 E2E
3. 실제 전화 수신 시 번호 옆 메모 위치·잘림·잠금화면·삼성 전화 앱 QA
4. 캠페인 수신자 검색·상태/실패 필터·선택 재시도·선택 취소
5. 캠페인 작성 최종 미리보기·중복 시작 방지
6. 실제 Play Billing·구독 만료/복원/환불 검증
7. Android 8~15·릴리스 서명·AAB·Crash/ANR 출시 QA

## 빌드

```bash
gradle :app:assembleDebug --stacktrace
```

- JDK 17
- Gradle 8.9

작업 완료 후 버전·검증 Run·실기기 확인 여부·남은 패치를 문서에 업데이트합니다.
