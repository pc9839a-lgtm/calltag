# CallTag v0.44.6 / code84 릴리스 인수인계

기준일: **2026-08-08**

## 릴리스 정보

- versionName: `0.44.6`
- versionCode: `84`
- targetSdk: `36`
- 패키지: `kr.pagero.calltag`
- 제품 브랜치: `agent/play-internal-v0430-run2`
- 제품 PR: `#67`
- 제품 병합 SHA: `33e9daa376c6ea073a84259ffafd1fcffe46d8d9`
- artifact-only 빌드 PR: `#68` — 제품에 병합하지 않음
- signed build workflow run: `31233511666`

## 산출물

Play AAB artifact:

- artifact ID: `9014694531`
- name: `calltag-v0.44.6-code84-play-aab`
- AAB SHA-256: `c6493cd9c7fa6f9e18ea1903b71a0bf9ab1fae68f3ae92c7c8fb4a0b1a96809a`

Debug APK:

- artifact ID: `9014646024`
- APK SHA-256: `88380f064667e64ee12ebd3dcefc7ee404eda46bc02241215a70561a42eb5234`

검증:

- release contract PASS
- Debug APK assemble PASS
- Android 16 / API36 regression PASS
- 기존 Play upload key 지문 검증 PASS
- signed release AAB build PASS
- `jarsigner` 검증 PASS

## code84 변경사항

### 1. 통계 차트 숫자 가독성

- 일별 추이 차트를 터치하면 해당 날짜의 `통화 N건 · 페이지로 N명` 표시
- 툴팁 배경을 다크 패널로 고정
- 숫자/텍스트를 밝은 `text_primary`로 고정
- 저대비 때문에 숫자가 안 보이던 문제 수정

### 2. 이용권·결제 화면

- 별도 밝은 화면 느낌 제거
- 콜태그 다크 카드/버튼 톤으로 통일
- 무료체험 문구를 기본 7일 / 추천 +7일 / 총 14일로 수정

주의:

- Google Play 실제 구매/구독/서버 검증은 아직 미완료
- UI 완성과 결제 기능 완성을 혼동하지 않음

### 3. 데이터 보호·복구 화면

- CallTag 다크 배경/카드/버튼 적용
- 기본 흰 Button 노출 제거
- 동기화/복구 상태 화면을 앱 전체 디자인과 통일

### 4. 할 일 시간 선택

`HomeTaskEditorActivity` 경로:

- 시스템 AM/PM `TimePickerDialog` 제거
- 오전/오후 선택
- 1~12시 칩 선택
- 00/10/20/30/40/50분 선택
- 최종 선택 시간을 상단에 즉시 표시

잔여점:

- `MainActivity` 캘린더 일정 추가/변경 경로에는 아직 시스템 `TimePickerDialog`가 남아 있음
- 다음 패치에서 동일 시간선택 UI로 통일 필요

### 5. 그룹·단체문자 UI

- 그룹/단체문자 허브 카드·간격·버튼 정리
- 수동/스마트 그룹 Dialog를 CallTag 다크 스타일로 통일
- 스마트그룹 Spinner를 `CallTagSpinnerAdapter`로 교체
- 시스템 기본 흰 dropdown 제거

### 6. 스마트그룹 거래여부 제거

제거:

- 거래 여부
- 거래 고객만
- 미거래 고객만

동작:

- 기존 저장 그룹의 transaction mode가 남아 있어도 필터 계산에서 무시
- 저장 시 `TRANSACTION_ANY`로 정규화

남는 주요 조건:

- 고객 상태
- 최근 연락/미접촉 기간
- 미완료 일정 여부

### 7. 고객탭 문자 접근

- 고객 카드 자체 터치 → 고객 상세
- 카드 빠른 액션 → `문자 보내기`
- 상태 변경 버튼 유지
- 고객 ID/전화번호를 문자 작성 화면으로 연결

### 8. 문자탭 구조

우선순위:

1. `고객 선택 후 문자` — 최상단 primary
2. `통화 후 자동문자` — 별도 대형 카드
3. 문자 템플릿
4. 그룹·단체문자
5. 발송 내역

`통화 후 자동문자`는 일반 문자 관리 리스트에서 제거하고 별도 접근점으로 유지한다.

## 이전 릴리스에서 유지되는 핵심 계약

- 통화 후 작은 팝업 1개
- 전체화면 통화 후 화면 금지
- 팝업 중복 실행 금지
- `WRITE_CALL_LOG` 금지
- `CACHED_NAME` 수정 금지
- 사용자 원본 연락처 이름 변조 금지
- 고객 추가 최근통화는 raw Android CallLog 최신순
- 등록 고객/반복 통화를 고객추가 최근통화에서 숨기지 않음
- 일정 화면에 최근통화 넣지 않음
- 확인할 통화 `할 일 등록`은 일정 등록
- 페이지로 연동/서비스 안내 분리
- 정산 URL: `https://calltag.pagero.kr/web/settlement`
- 일반 신규가입 통합권 7일
- 추천인 가입 +7일, 총 14일
- 추천코드는 가입 시 1회만 입력

## 다음 작업 우선순위

### P0

Google Play 실제 결제 완성:

- Play 상품 ID
- BillingClient purchase flow
- acknowledgement
- 서버 영수증 검증
- entitlement
- restore
- expiry/cancel/grace/account hold
- 웹 결제와 중복 방지

### P1

- 캘린더 일정 추가/변경 TimePicker UI 통일
- 삼성/Google Phone/에이닷 실기기 통화 회귀
- 남은 OEM 기본 Dialog/Picker 전수 제거
- 작은 화면/큰 글자/키보드 대응

## 절대 회귀 금지

- 고객탭 `문자 보내기` 빠른 동선 제거 금지
- 문자탭 `고객 선택 후 문자`를 일반 목록 안에 묻지 않음
- `통화 후 자동문자`를 일반 관리 목록에 다시 섞지 않음
- 스마트그룹 `거래 여부` 재추가 금지
- 통계 툴팁 저대비 색상 복귀 금지
- 이용권·결제 / 데이터 보호·복구만 별도 밝은 디자인으로 복귀 금지
