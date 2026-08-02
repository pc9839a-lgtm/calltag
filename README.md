# 콜태그 (CallTag)

전화 전 고객 맥락 확인부터 통화 종료 후 상태·메모·일정·문자 처리, 후속문자와 단체문자까지 연결하는 Android 고객관리 앱입니다.

## 다음 개발자가 먼저 읽을 문서

1. [`docs/ANDROID_DEVELOPER_HANDOFF_KO.md`](docs/ANDROID_DEVELOPER_HANDOFF_KO.md) — **현재 코드 구조·파일별 역할·안전 규칙·실기기 검수·다음 작업 정본**
2. [`docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md) — 최신 구현 상태와 남은 패치
3. [`docs/DESIGN_SYSTEM_KO.md`](docs/DESIGN_SYSTEM_KO.md) — Android UX/UI 규격
4. [`docs/PRODUCT_SPEC_KO.md`](docs/PRODUCT_SPEC_KO.md) — 제품 정의
5. [`docs/PATCH_14_BACKUP_RESTORE_PLAN_KO.md`](docs/PATCH_14_BACKUP_RESTORE_PLAN_KO.md) — 암호화 백업·복원 상세 기준

기획 문서에 적혀 있다는 이유만으로 구현 완료로 판단하지 않습니다. 코드, 버전, 기능 커밋과 빌드 결과가 확인된 항목만 코드 완료입니다. APK 빌드 성공과 실제 휴대전화 검증 성공도 구분합니다.

## 제품 기준

- 제품명: **콜태그(CallTag)**
- 패키지명: `kr.pagero.calltag`
- 대표 도메인: `https://calltag.pagero.kr`
- 개발 브랜치: `agent/calltag-foundation`
- 개발 PR: Draft PR `#1`
- `main` 병합: 사용자 명시 지시 전 금지

## 현재 Android 상태

- versionName: `0.38.2`
- versionCode: `46`
- 최소 Android: API 26
- compileSdk/targetSdk: 35
- 검증 Workflow: `Validate CallTag Android`
- Run ID: `30731475511`
- Job ID: `91452540504`
- 결과: Android 리소스 처리·Java 컴파일·Debug APK 패키징·아티팩트 업로드 성공
- Artifact ID: `8828114600`
- Artifact digest: `sha256:92dcc96bfb35b2d2fe28f21d69bd4e3c967273b4c35f55256d0849010b810499`
- 실제 APK SHA-256: `d13ffe43d261be6ca3ff7af73d00830bbc426aff44ebc587d42b8d7f5c4876d5`

임시 검증 PR `#18`은 병합하지 않고 닫았습니다. 개발 PR `#1`은 계속 Draft·미병합 상태입니다.

## 0.38.2 핵심 변경

- 일정 추가 고객 선택창에 고객명·전화번호 검색
- 고객이 9명 이상이면 결과 수와 고정 높이 내부 스크롤 표시
- 메인 다른 탭에서 뒤로가기 시 홈으로 이동
- 홈에서 뒤로가기 시 앱 종료 확인창
- Android 13 이상 제스처 뒤로가기 대응
- 통계 기간 `오늘 / 7일 / 30일 / 선택`
- 직접 선택은 한 화면에서 시작일·종료일을 확인한 뒤 적용
- 미래 날짜·역전 기간·365일 초과 차단
- 7일·30일과 직접 선택 2~30일에 일별 통화·페이지로 유입 추이 차트
- 통계를 대표 요약 → 차트 → 통화유형 → 페이지로 → 처리할 일 순으로 재구성

## 현재 주요 기능

- 통화 수신 고객정보 표시
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
- 암호화 `.ctbackup` 백업·복원
- DB·예약·캠페인 정합성 복구와 진단

## 데이터·발송 안전 규칙

- 기존 데이터를 초기화하지 않습니다.
- DB 변경 시 보존 마이그레이션을 작성합니다.
- 발송 직전 허용 여부·발송 제외·중복방지·SIM·캠페인 상태를 다시 검사합니다.
- 불명확한 `SENDING` 작업은 자동 재발송하지 않습니다.
- 일시정지 캠페인은 자동 재개하지 않습니다.
- 누락 작업을 추측해 생성하거나 고아 작업을 자동 발송하지 않습니다.
- 이미지 문자는 시스템 메시지 앱에서 사용자가 최종 전송합니다.
- CSV·XLSX·고객 목록·캠페인 결과 외부 반출을 구현하지 않습니다.
- `.ctbackup` 백업·복원과 일반 데이터 내보내기를 합치지 않습니다.

## 다음 작업 우선순위

1. `0.38.2` 실기기 QA: 일정 검색, 뒤로가기, 통계 차트, 통화 종료 큰 화면
2. 캠페인 수신자 검색·상태/실패 필터·선택 재시도·선택 취소
3. 캠페인 작성 최종 미리보기·중복 시작 방지
4. 실제 Play Billing·구독 만료/복원/환불 검증
5. Android 8~15·릴리스 서명·AAB·Crash/ANR 출시 QA

## 빌드

```bash
gradle :app:assembleDebug --stacktrace
```

- JDK 17
- Gradle 8.9

작업 완료 후 버전·검증 Run·실기기 확인 여부·남은 패치를 문서에 업데이트합니다.
