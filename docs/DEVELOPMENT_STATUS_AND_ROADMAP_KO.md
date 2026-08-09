# 콜태그 개발 현황·로드맵

기준일: **2026-08-09**  
저장소: `pc9839a-lgtm/calltag`  
제품 브랜치: `agent/play-internal-v0430-run2`  
현재 앱 버전: **0.44.9**  
versionCode: **87**  
패키지명: `kr.pagero.calltag`

> 이 문서는 현재 구현 상태의 정본이다. 오래된 `0.3x~0.41.x`, Classic/Pro, 기본 3일 무료체험, 추천 +5일, 통합 2,500원, 큰 통화 종료창, 시스템 CallLog 메모 쓰기, 연락처 이름 변조 방식, 시스템 기본 DatePicker/TimePicker 사용은 현재 기준이 아니다.

## 1. 현재 제품 구조

콜태그는 Android 전화 기반 고객관리 앱이며 기능군은 다음과 같이 분리한다.

| 상품 | 월 가격 | 현재 제품 정의 |
|---|---:|---|
| 전화관리 | 1,900원 | 수신 고객 표시, 통화 후 고객관리, 상태·메모·일정·통계 |
| 문자자동화 | 990원 | 통화 후 자동문자, 템플릿, 그룹·단체문자, 발송 관리 |
| 페이지로 | 3,500원 | 모바일 랜딩페이지 제작·문의 수집 |
| 통합권 | 6,000원 | 전화관리 + 문자자동화 + 페이지로 전체 |

앱에는 `com.android.billingclient:billing:9.1.0` 의존성이 존재하지만 **실제 Google Play 구매·구독·복원·만료 서버 검증은 아직 완료되지 않았다.** 결제 화면의 디자인 완성과 실제 결제 기능 완성을 혼동하지 않는다.

## 2. 무료체험·추천 정책

현재 확정 정책:

- 일반 신규가입: **통합권 7일 무료**
- 회원가입 시 추천인 코드 입력: **+7일**
- 추천가입자: **통합권 총 14일 무료**
- 추천인 코드는 **회원가입 시 1회만 입력**
- 가입 이후 추천코드 재적용 경로 없음
- 무료기간은 서버 entitlement로 처리하며 결제수단 등록을 요구하지 않음
- 무료체험 종료 후 자동결제하지 않음
- 추천인과 파트너 프로그램은 분리

페이지로 웹의 기존 추천 정책과 콜태그 앱 추천 정책은 서버에서 분리한다.

## 3. 통화 CRM 안정화

### 3.1 통화 감지·복구

- `CallProcessingLedger`: 최근 CallLog ID 다중 영속 dedupe
- `CallInteractionDeduper`: 동일 통화 interaction DB 중복 삽입 방지
- `PostCallRecoveryStore`: 통화 후 팝업 전달 실패·프로세스 재시작 복구
- `CallLogRepository.findRecent`: 재시작 후 최근 통화 읽기 복구
- `CallDisposition`: 수신/발신/부재중/거절/0초 발신 규칙 중앙화
- ROLE_CALL_SCREENING 상태 추적
- 삼성/Pixel/OEM 진단 로그 유지

Android 16/API36 에뮬레이터 회귀검사는 code87에서도 통과했다. **삼성·Pixel·에이닷 실기기 전체 회귀를 CI가 대신한 것으로 표현하지 않는다.**

### 3.2 Play 정책 안전 구조

현재 Play용 구조에서는 다음을 금지한다.

- `WRITE_CALL_LOG`
- `CallLog.Calls.CACHED_NAME` 쓰기
- 시스템 기본 전화앱 최근통화 메모 변조
- 사용자 원본 연락처 이름 변경
- 새로운 RawContact 생성으로 발신자 이름 강제 덮어쓰기

콜태그 메모·고객 상태·일정은 **콜태그 DB/UI에서 관리**한다.

### 3.3 통화 종료 팝업 — 절대 기준

현재 UX 기준은 다음과 같다.

- 통화 종료 후 **작은 팝업 1개만 표시**
- **전체화면 팝업/Activity 절대 금지**
- 작은 팝업 + 전체화면 동시 표시 금지
- 같은 통화에 작은 팝업 2개 이상 중복 표시 금지
- 고객명 + 메모 + 저장/닫기 중심의 최소 UI
- 전화번호·통화시간·상담단계 등 복잡한 CRM 입력을 통화 직후 팝업에 다시 넣지 않음
- 배경 전체 강한 dim 금지
- 팝업 표시 실패 시 full-screen Activity로 fallback하지 않음
- 실패 fallback은 **알림 또는 미처리 통화 큐**만 사용
- 앱 복귀·프로세스 재시작 시 이미 처리한 통화 팝업 재표시 금지
- 잠금/백그라운드/연속통화에서도 작은 팝업 1개 원칙 유지

실기기 QA에서는 아래 중 하나라도 발생하면 실패로 본다.

1. 전체화면 통화 종료창이 한 번이라도 표시됨
2. 같은 통화에서 작은 팝업이 2개 이상 표시됨
3. 이미 처리된 통화 팝업이 앱 복귀 후 다시 표시됨

## 4. 홈·고객·일정·통계 현재 동작

### 홈

- `오늘 할 일`은 오늘 일정만 표시
- 확인할 통화 카드에 최근 메모 표시
- 확인할 통화 `할 일 등록` → `HomeTaskEditorActivity`
- `할 일 등록`이 통화 후 메모 팝업으로 연결되지 않음
- 확인할 통화 `삭제` 제공
- 할 일 저장 후 홈 복귀 시 즉시 갱신

### 고객 추가 > 최근 통화

실제 Android `CallLog`를 `DATE DESC` 최신순으로 읽는다.

- 동일 번호 여러 통화를 임의 dedupe하지 않음
- 이미 콜태그에 등록된 고객 번호를 숨기지 않음
- 등록된 번호는 `등록됨`으로 표시
- 등록된 번호 선택 시 새 고객을 만들지 않고 기존 고객으로 이동
- 화면 복귀 시 최근통화를 다시 로드
- 최대 60건 표시

삼성/Pixel 기본 전화앱은 연속 통화를 시각적으로 묶을 수 있으므로 화면 모양은 다를 수 있지만, 콜태그는 raw CallLog row 기준이다.

### 고객 탭 UX

- 고객 카드 자체 터치 → **고객 상세**
- 고객 카드에 최근 메모 표시 유지
- 카드의 빠른 액션에서 **`문자 보내기`**를 명확한 진입점으로 제공
- 문자 발송 시 등록 고객 ID와 전화번호를 함께 전달
- 상태 변경은 별도 버튼으로 유지

### 문자 탭 UX

문자 메뉴는 고객 중심으로 정리한다.

1. **고객 선택 후 문자** — 최상단 primary 액션
2. **통화 후 자동문자** — 일반 문자 관리 목록에서 분리한 대형 독립 카드
3. 문자 템플릿
4. 그룹·단체문자
5. 발송 내역

`통화 후 자동문자`를 일반 목록 안에 다시 섞지 않는다.

### 일정 — code87

- 일정 화면에는 최근통화 목록을 넣지 않음
- 월간 캘린더 + 선택일 일정 중심
- 일정 추가·변경·완료·다시 열기·삭제
- `HomeTaskEditorActivity`, `MainActivity`, `CustomerDetailActivity`, 고객 통계 기간선택까지 **시스템 `DatePickerDialog` / `TimePickerDialog` 제거 완료**
- 날짜는 `TaskDateChoiceDialog`, 시간은 `TaskTimeChoiceDialog` 사용
- 시간 선택은 오전/오후 + 시 + 분 칩 UI
- 날짜/시간 선택 Dialog는 `ScrollView` 기반으로 작은 화면·큰 글자에서 스크롤 가능
- 고객 통계의 직접 기간 선택은 기존처럼 **오늘 이후 날짜 선택 금지** 유지

현재 앱 Java 소스 기준:

- `DatePickerDialog` 사용: **0건**
- `TimePickerDialog` 사용: **0건**
- `android.R.layout.simple_spinner_*` 사용: **0건**

### 통계

- 일별 추이 차트 터치 지원
- 터치한 날짜의 `통화 N건 · 페이지로 N명` 상세 수치 표시
- 툴팁 배경을 다크 패널로 고정
- 툴팁 숫자/문자색을 `text_primary`로 고정해 어두운 화면에서 숫자가 묻히지 않게 수정
- 직접 기간 선택도 콜태그 전용 날짜 선택 UI 사용

## 5. 더보기·설정 UI

더보기 핵심 구성:

- `통화 후 자동문자` — 검색창 아래 **대형 독립 카드**
- 문자 문구·이미지
- 그룹·단체문자
- 발송 관리
- 고객 상태
- 일정 종류
- 계정 및 개인정보
- 데이터 보호·복구
- 이용권·결제
- 친구 초대·파트너
- 파트너 정산
- 페이지로 연동
- 페이지로 서비스 안내
- 백업 및 복원

### 이용권·결제

콜태그 다크 카드 톤으로 통일한다.

- 배경 다크
- 카드/버튼 콜태그 스타일
- 7일 + 추천 7일 = 총 14일 안내 문구
- 실제 Play 결제 기능은 아직 P0 미완료

### 데이터 보호·복구

- CallTag 헤더/카드/버튼 스타일 사용
- 기본 흰색 버튼 노출 제거
- 동기화/복구 상태를 같은 디자인 언어로 표시

## 6. Dialog / Picker / Spinner UI 통일 — code87

code87에서 OEM 기본 UI 잔여를 전수 정리했다.

### Dialog

- `AlertDialog.Builder` 사용 화면을 CallTag Dialog 테마로 전환
- `Theme.CallTag.Dialog` 기반 다크 UI 사용
- `CallTagDialogStyler`에서 공통 버튼/텍스트/배경 스타일 적용
- 좁은 화면에서 기존 최소 280dp 강제폭 제거
- 실제 화면 폭에서 좌우 여백을 뺀 값 기준으로 폭 계산
- `SOFT_INPUT_ADJUST_RESIZE` 적용으로 키보드 표시 시 가용 영역 재계산

### Date / Time Picker

- 시스템 `DatePickerDialog` 제거
- 시스템 `TimePickerDialog` 제거
- `TaskDateChoiceDialog` / `TaskTimeChoiceDialog`로 통일
- Dialog 내부 ScrollView 적용
- 기존 일정 생성/변경 시간 초기값 보존
- 고객 통계 최대 날짜 제한 기능 보존

### Spinner

- `CallTagSpinnerAdapter`가 Android 기본 `simple_spinner_*` 레이아웃을 사용하지 않음
- 그룹·단체문자와 자동문자 설정의 회선 선택도 CallTag 다크 행 사용

## 7. 그룹·단체문자 / 스마트그룹

### 그룹·단체문자

- 허브 카드/여백/버튼을 콜태그 다크 UI에 맞춤
- 수동그룹 편집 Dialog도 CallTag Dialog 스타일 사용
- 스마트그룹 Spinner는 `CallTagSpinnerAdapter` 사용
- 자동문자 설정의 발송 회선 Spinner도 `CallTagSpinnerAdapter` 사용
- 단체문자 작성/목록은 기존 다크 화면 흐름 유지

### 스마트그룹

**`거래 여부` 조건은 제거한다.**

이유:

- 현재 콜태그 핵심 고객 모델에서 거래/미거래는 사용자에게 명확하게 관리되는 확정 필드가 아님
- 조건 의미가 애매하고 실제 필터 결과를 오해하게 만들 수 있음

현재 처리:

- 스마트그룹 생성/수정 UI에서 `거래 여부`, `거래 고객만`, `미거래 고객만` 제거
- 기존 저장 그룹의 transaction mode가 남아 있어도 필터 계산에서 무시
- 저장 시 `TRANSACTION_ANY`로 정규화

남기는 조건은 실제 관리 데이터 중심이다.

- 고객 상태
- 최근 연락/미접촉 기간
- 미완료 일정 여부

## 8. 페이지로·정산

페이지로 관련 화면은 반드시 분리한다.

- `페이지로 연동`: 계정 연결 상태·문의 확인 중심
- `페이지로 서비스 안내`: 페이지로 서비스 설명

연결 화면에 `이렇게 사용하세요` 같은 서비스 설명을 다시 섞지 않는다.

정산 연결:

`https://calltag.pagero.kr/web/settlement`

## 9. UI 디자인 기준

일반 화면은 다크 UI를 기본으로 한다.

```text
배경        #101113
카드        #1C1E22
강조        #4389FF
주요텍스트  #F4F5F7
보조텍스트  #A8ADB5
```

원칙:

- 기본 Android 흰색 Dialog/Picker를 사용자에게 그대로 노출하지 않음
- Dialog는 `Theme.CallTag.Dialog` 또는 CallTag 전용 커스텀 Dialog 사용
- Date/Time Picker는 CallTag 전용 UI 사용
- Spinner 드롭다운은 CallTag 전용 어댑터 사용
- 확인/등록/저장: 파란 primary
- 취소/닫기: 다크 secondary
- 상태바 시간·배터리·신호 아이콘은 흰색
- 상태바와 앱 콘텐츠는 겹치지 않음
- system bar inset은 한 경로만 적용
- `fitsSystemWindows`와 전역 inset 중복 금지
- 기능별 화면만 따로 밝은 디자인으로 만드는 것 금지
- 작은 화면/큰 글자/키보드에서 Dialog 내용이 잘리지 않아야 함

## 10. 페이지로 실시간 문의 연동

```text
페이지로 문의 접수
→ 서버 저장
→ CallTag FCM 알림
→ 전화번호 기준 고객 생성/갱신
→ 문의 내용을 고객 상담이력에 반영
```

원칙:

- 앱 종료·잠금 상태 알림 대응
- 같은 문의 반복 처리 방지
- FCM 실패가 페이지로 문의 접수 자체를 실패시키지 않음
- FCM payload에 불필요한 개인정보를 넣지 않음

## 11. 데이터 보호·복구 원칙

콜태그 고객 데이터는 앱 내부 DB를 기본 정본으로 사용한다.

- 앱 삭제·기기변경 대비 데이터 보호/복구 기능 유지
- 기존 고객·메모·일정·문자 데이터 초기화 금지
- 서버 복구본과 기기 데이터 삭제는 다른 작업으로 취급
- 로그인 세션/토큰을 일반 백업 데이터와 혼합하지 않음
- 원본 휴대폰 전체 연락처/문자함/통화녹음을 서버로 올리는 제품이 아님

## 12. 최신 제품 릴리스 검증 — v0.44.9 / code87

현재 제품 릴리스:

- versionName: `0.44.9`
- versionCode: `87`
- targetSdk: `36`
- 제품 PR: `#72`
- 제품 병합 SHA: `74c63272b20a5a4e802d5c3f8ff40224a19533d5`

최종 검증 workflow:

- workflow run: `31314835218`
- Debug APK artifact ID: `9038441483`
- Instrumentation artifact ID: `9038468464`
- Debug artifact digest: `sha256:d06721bea8e46c88aeb77e1234ae2e31f23cac95037f309680ff8e33b2b445d9`
- Instrumentation artifact digest: `sha256:51465afe6b29a186d53bdf4bc94b0bc232c2204e8fbbc754b8335147d295300a`

검증 결과:

- framework UI 사용처 전수 인벤토리 PASS
- code87 정적 contract 검사 PASS
- `DatePickerDialog` 0건 검증 PASS
- `TimePickerDialog` 0건 검증 PASS
- `simple_spinner_*` 0건 검증 PASS
- Debug APK 빌드 PASS
- Android 16/API36 전화 CRM instrumentation PASS
- 통화 후 전체화면 팝업 금지 contract PASS
- Play-safe `WRITE_CALL_LOG` 금지 contract PASS

### Play 서명 AAB 상태

**code87 패치에서는 signed AAB를 새로 만들지 않았다.**

현재 저장소에서 검증된 최신 signed Play AAB는 code84이다.

- versionName: `0.44.6`
- versionCode: `84`
- workflow run: `31233511666`
- Play AAB artifact ID: `9014694531`
- AAB SHA-256: `c6493cd9c7fa6f9e18ea1903b71a0bf9ab1fae68f3ae92c7c8fb4a0b1a96809a`

Play Console에 code87을 올리려면 **기존 Play 업로드 키로 code87 signed AAB를 별도 생성**해야 한다.

## 13. 다음 우선순위

### P0 — 실제 Google Play 결제

필수 범위:

1. Play Console 구독/상품 ID 확정
2. `BillingClient` 구매 플로우
3. 구매 acknowledgement
4. 서버 영수증/구독 검증
5. CALL / MESSAGE / PAGERO / ALL_IN_ONE entitlement 분리
6. 구매 복원
7. 해지·만료·grace period·account hold 처리
8. 웹 페이지로 결제와 앱 결제 중복 방지
9. 무료체험 7일/추천 14일 entitlement → 유료 entitlement 전환
10. 실제 Play License Tester E2E

### P0 — 통화 후 팝업 실기기 회귀

반드시 실기기 확인:

- 삼성 기본 전화
- Pixel / Google Phone
- 에이닷 사용 환경
- 수신
- 발신
- 부재중
- 거절
- 0초 발신
- 연속 통화
- foreground / background
- 잠금 상태

실패 기준:

- 전체화면 통화 종료창 표시
- 같은 통화 작은 팝업 중복 표시
- 처리 완료 통화 팝업 재표시
- 실제 CallLog와 콜태그 최근통화 불일치

### P1 — UI 실기기 마감

code87에서 시스템 Picker/Dialog/Spinner 정리는 완료했다. 다음은 실제 화면 검증 중심이다.

- 작은 화면
- 큰 글자
- 키보드 표시
- 긴 고객명/긴 메모
- 버튼 텍스트 줄바꿈
- 하단 네비게이션/상태바 inset
- 고객/문자/설정 메뉴 중복 노출 점검
- 이용권·결제 실제 기능 구현 시 현재 다크 UI 유지

### P1.5 — 데이터 보호·페이지로 연동 E2E

- 앱 삭제 후 재설치 → 백업 복구 E2E
- 기기 변경 복구 E2E
- 페이지로 문의 → CallTag FCM E2E
- 앱 종료/잠금 상태 문의 수신
- 같은 문의 dedupe

### P2 — 사용자용 전화 기록 경험

기본 전화앱 CallLog를 변조하는 대신 콜태그 내부에서:

- 최근통화 검색/필터
- 고객별 통화·메모·문자·일정·페이지로 문의 통합 타임라인
- 통화 카드 빠른 메모 수정

## 14. 절대 회귀 금지

- 통화 후 전체화면 팝업으로 되돌리지 않음
- 작은 팝업을 두 번 띄우지 않음
- 통화 후 팝업 실패 시 full-screen Activity fallback 금지
- 홈 `할 일 등록`을 메모 저장 팝업에 연결하지 않음
- 일정 탭에 최근통화를 다시 넣지 않음
- 시스템 `DatePickerDialog` / `TimePickerDialog`를 다시 사용하지 않음
- Android `simple_spinner_*` 기본 드롭다운을 다시 사용하지 않음
- 기본 흰색 OEM Dialog를 화면별로 따로 다시 만들지 않음
- 고객 추가 최근통화에서 등록 고객/중복 통화를 숨기지 않음
- 고객 카드의 빠른 `문자 보내기` 동선을 제거하지 않음
- 문자탭의 `고객 선택 후 문자`를 일반 메뉴 안에 묻지 않음
- `통화 후 자동문자`를 일반 문자 관리 목록에 다시 섞지 않음
- 스마트그룹에 `거래 여부` 조건을 다시 넣지 않음
- 통계 툴팁을 숫자가 안 보이는 저대비 색상으로 되돌리지 않음
- 이용권·결제 / 데이터 보호·복구만 별도 밝은 디자인으로 되돌리지 않음
- `WRITE_CALL_LOG`를 다시 추가하지 않음
- 시스템 CallLog `CACHED_NAME`을 수정하지 않음
- 사용자 원본 연락처 이름을 덮어쓰지 않음
- 페이지로 연동과 서비스 안내를 한 화면으로 다시 합치지 않음
- 추천인 코드를 가입 이후 다시 입력하게 만들지 않음
- 3일/5일 무료체험 정책으로 되돌리지 않음
- Classic/Pro 요금제로 되돌리지 않음
