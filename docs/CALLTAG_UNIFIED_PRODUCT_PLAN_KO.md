# 콜태그 통합 제품 기획서

기준일: **2026-08-08**  
제품명: **콜태그(CallTag)**  
Android 패키지: `kr.pagero.calltag`

> **중요:** 현재 제품 정의는 [`PRODUCT_SPEC_KO.md`](PRODUCT_SPEC_KO.md), 실제 구현 상태는 [`DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](DEVELOPMENT_STATUS_AND_ROADMAP_KO.md)를 우선한다.

이 문서는 오래된 통합 기획에서 현재까지 확정된 제품 경계만 남기는 요약본이다.

## 현재 확정 상품

| 상품 | 월 가격 |
|---|---:|
| 전화관리 | 1,900원 |
| 문자자동화 | 990원 |
| 페이지로 | 3,500원 |
| 통합권 | 6,000원 |

`통합 2,500원`, `Classic/Pro`는 폐기된 가격안이다.

## 무료체험·추천

- 일반 신규가입: 통합권 7일 무료
- 회원가입 시 추천코드 입력: +7일
- 추천가입자: 총 14일 무료
- 가입 후 추천코드 재입력 불가
- 무료체험 종료 후 자동결제하지 않음

## 현재 핵심 UX

- 하단 탭: `고객 / 캘린더 / 홈 / 통계 / 더보기`
- 통화 종료 후 **작은 팝업 1개**
- 작은 팝업: 고객명 + 메모 + 저장/닫기 중심
- 일정 화면에는 최근통화를 넣지 않음
- 홈 확인할 통화: 다시 전화 / 할 일 등록 / 삭제 / 최근 메모
- 고객 추가 최근통화는 실제 Android CallLog 최신순
- 등록 고객과 동일 번호 반복 통화를 숨기지 않음
- 고객 카드: 카드 탭은 상세, 빠른 액션은 **문자 보내기**
- 고객 문자탭: **고객 선택 후 문자**를 최상단 primary 액션으로 사용
- `통화 후 자동문자`: 일반 메뉴에 섞지 않고 별도 대형 카드
- 통계 차트 터치: 해당 날짜 `통화 N건 · 페이지로 N명`을 고대비 툴팁으로 표시
- HomeTaskEditor 시간선택: 오전/오후 + 시 + 분 칩 UI
- 이용권·결제 / 데이터 보호·복구: 콜태그 다크 UI
- 그룹·단체문자: 다크 카드/다이얼로그/Spinner 스타일 통일
- 스마트그룹: **거래 여부 조건 제거**

## 스마트그룹 현재 조건

사용:

- 고객 상태
- 최근 연락/미접촉 기간
- 미완료 일정 여부

사용하지 않음:

- 거래 여부
- 거래 고객만
- 미거래 고객만

기존 저장된 transaction mode도 계산에서 무시한다.

## 페이지로

페이지로와 콜태그는 연동 가능하지만 서로 다른 서비스다.

앱에서 반드시 분리:

- `페이지로 연동`: 연결 상태·문의 확인
- `페이지로 서비스 안내`: 페이지로 서비스 설명

정산:

`https://calltag.pagero.kr/web/settlement`

## UI 원칙

- 다크 테마 기본
- 기능별 화면만 별도 밝은 디자인으로 만들지 않음
- 상태바 시간·배터리·신호 아이콘은 보이게 유지
- inset 중복 금지
- Android 기본 흰 Dialog/Picker는 가능한 한 제거
- primary 액션은 파란색, secondary는 다크 버튼

현재 잔여 UI 작업:

- 캘린더 일정 추가/변경의 시스템 `TimePickerDialog`를 콜태그 전용 시간선택 UI로 통일

## Play 정책 원칙

현재 Play용 앱에서는 다음을 하지 않는다.

- `WRITE_CALL_LOG`
- `CallLog.Calls.CACHED_NAME` 수정
- 삼성/Google 기본 전화앱 최근통화 메모 강제 삽입
- 사용자 원본 연락처 이름 변경

콜태그 고객·메모·일정·통화 interaction은 콜태그 DB/UI에서 관리한다.

## 현재 릴리스

- `v0.44.6`
- `versionCode 84`
- `targetSdk 36`
- 제품 PR `#67`
- 제품 병합 SHA `33e9daa376c6ea073a84259ffafd1fcffe46d8d9`
- Android 16/API36 회귀검사 PASS
- signed Play AAB 검증 PASS
- AAB SHA-256 `c6493cd9c7fa6f9e18ea1903b71a0bf9ab1fae68f3ae92c7c8fb4a0b1a96809a`

## 다음 최우선

실제 Google Play 결제 완성:

- CALL / MESSAGE / PAGERO / ALL_IN_ONE 권한
- 구매/acknowledgement
- 서버 검증
- 구매 복원
- 만료/해지/grace/account hold
- 웹 결제 중복 방지
- 무료체험 entitlement → 유료 entitlement 전환

상세 구현·화면·회귀금지 항목은 반드시 현재 정본 문서를 확인한다.
