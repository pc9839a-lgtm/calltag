# CallTag v0.44.9 / code87 릴리스·인수인계

기준일: **2026-08-09**

## 1. 현재 릴리스

- versionName: `0.44.9`
- versionCode: `87`
- targetSdk: `36`
- package: `kr.pagero.calltag`
- 제품 브랜치: `agent/play-internal-v0430-run2`
- 제품 PR: `#72`
- 병합 SHA: `74c63272b20a5a4e802d5c3f8ff40224a19533d5`

## 2. code87 핵심 변경

### Dialog / Picker / Spinner 전수 정리

- 시스템 `DatePickerDialog` 제거
- 시스템 `TimePickerDialog` 제거
- `TaskDateChoiceDialog` 추가/적용
- `TaskTimeChoiceDialog` 전 경로 통일
- 날짜/시간 Dialog 내부 `ScrollView` 적용
- 작은 화면/큰 글자 대응
- `CallTagDialogStyler` 좁은 화면 폭 계산 수정
- `SOFT_INPUT_ADJUST_RESIZE` 적용
- `AlertDialog.Builder` 사용 화면 CallTag 다크 테마 전환
- `CallTagSpinnerAdapter`에서 `simple_spinner_*` 제거
- 자동문자 설정 회선 Spinner도 CallTag 전용 어댑터 사용

현재 Java 소스 기준:

- `DatePickerDialog`: 0건
- `TimePickerDialog`: 0건
- `android.R.layout.simple_spinner_*`: 0건

### 일정/통계 기능 보존

- 홈 할 일 날짜·시간 선택: CallTag 전용 UI
- 메인 캘린더 일정 추가/변경: CallTag 전용 UI
- 고객 상세 일정 추가/변경: CallTag 전용 UI
- 고객 통계 직접 기간 선택: CallTag 전용 날짜 UI
- 고객 통계의 미래 날짜 선택 금지 규칙 유지
- 일정 변경 시 기존 시간 초기값 유지

## 3. 통화 후 팝업 절대 계약

통화 종료 후 UX는 다음을 절대 기준으로 한다.

- **작은 팝업 1개만 표시**
- **전체화면 팝업/Activity 금지**
- 작은 팝업 + 전체화면 동시 표시 금지
- 같은 통화 팝업 중복 표시 금지
- 고객명 / 메모 / 저장 / 닫기 중심
- 전화번호·통화시간·상담단계 등 복잡한 입력 재도입 금지
- 팝업 표시 실패 시 full-screen Activity fallback 금지
- fallback은 알림 또는 미처리 통화 큐
- 이미 처리한 통화의 팝업 재표시 금지
- 잠금/백그라운드/연속통화에서도 같은 규칙 유지

실기기에서 전체화면 팝업이 한 번이라도 뜨거나 같은 통화에 작은 팝업이 2번 뜨면 실패다.

## 4. code87 CI 검증

최종 workflow run:

`31314835218`

결과:

- framework UI 사용처 인벤토리 PASS
- 정적 contract PASS
- Debug APK 빌드 PASS
- Android 16/API36 instrumentation PASS
- 통화 후 full-screen 경로 금지 PASS
- `WRITE_CALL_LOG` 금지 PASS
- 원본 연락처 이름 변조 금지 PASS

Artifacts:

- Debug APK artifact ID: `9038441483`
- Instrumentation artifact ID: `9038468464`
- Debug artifact digest: `sha256:d06721bea8e46c88aeb77e1234ae2e31f23cac95037f309680ff8e33b2b445d9`
- Instrumentation artifact digest: `sha256:51465afe6b29a186d53bdf4bc94b0bc232c2204e8fbbc754b8335147d295300a`

## 5. Play AAB 상태

**code87 signed AAB는 아직 생성하지 않았다.**

현재 검증된 최신 signed Play AAB는 code84다.

- versionName: `0.44.6`
- versionCode: `84`
- workflow run: `31233511666`
- AAB artifact ID: `9014694531`
- SHA-256: `c6493cd9c7fa6f9e18ea1903b71a0bf9ab1fae68f3ae92c7c8fb4a0b1a96809a`

Play Console에 현재 제품을 올릴 때는 기존 Play upload key로 **code87 signed AAB를 새로 빌드**해야 한다.

## 6. 다음 작업 우선순위

### P0 Google Play 결제

- Play 상품 ID 확정
- BillingClient 구매
- acknowledgement
- 서버 영수증 검증
- CALL / MESSAGE / PAGERO / ALL_IN_ONE entitlement
- 구매 복원
- 해지/만료/grace/account hold
- 웹 페이지로 결제와 Play 중복결제 방지
- 7일/14일 무료 entitlement 전환
- License Tester E2E

### P0 통화 실기기 회귀

- 삼성 기본 전화
- Pixel / Google Phone
- 에이닷
- 수신/발신/부재중/거절/0초 발신
- 연속 통화
- foreground/background
- 잠금 상태
- 작은 팝업 단일 표시
- 실제 CallLog와 고객 추가 최근통화 비교

### P1 UI 실기기 QA

- 작은 화면
- 큰 글자
- 키보드
- 긴 고객명/메모
- 버튼 줄바꿈
- 상태바/하단 네비게이션 inset
- 메뉴 중복 점검

### P1.5 데이터/연동 E2E

- 삭제 후 재설치 복구
- 기기변경 복구
- 페이지로 문의 → CallTag FCM
- 앱 종료/잠금 상태
- 문의 dedupe

### P2 제품 고도화

- 최근통화 검색/필터
- 고객별 통화·메모·문자·일정·페이지로 문의 통합 타임라인
- 통화 카드 빠른 메모

## 7. 회귀 금지 체크리스트

- 전체화면 통화 종료 팝업 재도입 금지
- 팝업 중복 표시 금지
- `DatePickerDialog` 재도입 금지
- `TimePickerDialog` 재도입 금지
- `simple_spinner_*` 재도입 금지
- 밝은 OEM 기본 Dialog 재도입 금지
- `WRITE_CALL_LOG` 재도입 금지
- `CallLog.Calls.CACHED_NAME` 쓰기 금지
- 원본 연락처 이름 변경 금지
- 일정 탭 최근통화 재추가 금지
- 홈 할 일에 전체 일정 표시 금지
- 페이지로 연동/서비스 안내 재결합 금지
- 스마트그룹 `거래 여부` 재추가 금지
- 추천코드 가입 후 재입력 경로 금지
- 3일/5일 무료체험 정책 회귀 금지
