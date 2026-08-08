# CallTag v0.44.5 / code83 릴리스 인수인계

기준일: **2026-08-08**

## 릴리스

- versionName: `0.44.5`
- versionCode: `83`
- targetSdk: `36`
- 제품 브랜치: `agent/play-internal-v0430-run2`
- 제품 PR: `#64`
- 제품 병합 SHA: `ca8ccc8bf8422764abfb23d8776f5e755f50f270`
- signed AAB SHA-256: `1af9a6c21cef9e1e6f3e179f9c1bf59ca1f1bda85276e8f354ac00df2160e3c5`

## code83에서 수정된 사용자 이슈

### 고객 추가 > 최근 통화 불일치

기존 문제:

- 같은 번호는 한 건만 남겨 실제 통화기록과 달랐음
- 이미 콜태그에 등록된 번호는 목록에서 제외됨
- 고객 추가 후 같은 고객과 통화하면 새 통화가 목록에서 사라져 보였음

현재:

- Android `CallLog` raw row 최신순
- 같은 번호 반복 통화 유지
- 등록 고객도 유지
- 등록 고객은 `등록됨` 표시
- 등록 고객 선택 시 기존 고객 열기
- 화면 복귀 때 최근통화 다시 읽기

### 상단 상태바

기존 문제:

- Android 15/16 edge-to-edge 대응 과정에서 system inset 중복 가능
- 시간·배터리·신호 아이콘이 어두운 배경에 묻힘
- 일부 화면 상단 여백이 과도함

현재:

- system bar inset 단일 경로
- 흰색 상태바/내비게이션 아이콘
- 고객 추가/빠른 수정의 별도 `fitsSystemWindows` 제거

### 기본 Android 흰색 팝업

기존 문제:

- 고객등록/선택/날짜/시간 선택에서 OEM 기본 밝은 Dialog가 노출돼 CallTag 다크 UI와 불일치

현재:

- `Theme.CallTag.Dialog`
- `Theme.CallTag.PickerDialog`
- `CallTagDialogStyler`
- 확인/등록/저장은 primary 버튼
- 취소/닫기는 secondary 버튼

### 더보기 > 통화 후 자동문자

기존 문제:

- 문자 설정 목록의 일반 행에 묻혀 핵심 기능 접근성이 낮음

현재:

- 더보기 검색창 바로 아래 82dp 독립 대형 카드
- `통화 후 자동문자` 직접 진입
- 기존 일반 `자동문자` 행 제거

## code82에서 유지해야 하는 수정

- 통화 종료 후 작은 팝업 1개만 표시
- 큰 전체화면 팝업 금지
- PostCall Activity 중복 재실행 금지
- 홈 확인할 통화 `할 일 등록` → 일정 등록
- 확인할 통화 삭제 버튼
- 확인할 통화 최근 메모
- 일정 화면 최근통화 제거
- 통계 차트 터치 상세 수치
- 페이지로 연동/서비스 안내 분리
- 정산페이지 연결

## 추천 정책

- 일반 가입: 통합권 7일
- 추천코드: +7일
- 총 14일
- 가입 시 1회만 입력

## 가격

- 전화관리 1,900원
- 문자자동화 990원
- 페이지로 3,500원
- 통합권 6,000원

## 아직 완료되지 않은 것

### 실제 Google Play 결제

Billing Library 의존성은 있으나 다음은 아직 P0:

- Play 상품 ID 확정
- 실제 구매
- acknowledgement
- 서버 검증
- entitlement 동기화
- 구매 복원
- 만료/해지/grace/account hold
- 웹 페이지로 결제 중복 방지

### 실기기 회귀

CI Android 16/API36 Pixel 6 프로필은 통과했다.

별도 실기기 확인 필요:

- 삼성 기본 전화
- Google Phone
- 에이닷
- 잠금/백그라운드/앱 종료
- 연속 통화
- 부재중/거절/0초 발신

## 정책상 하지 말 것

- `WRITE_CALL_LOG` 복원
- `CallLog.Calls.CACHED_NAME` 쓰기
- 기본 전화앱 최근기록에 메모 강제 삽입
- 원본 연락처 이름 변경
- 일정에 최근통화 다시 삽입
- 통화 후 큰 전체화면 정리창 복원
- 페이지로 연동과 서비스 안내 재통합
- Classic/Pro 요금제 복원
- 기본 3일/추천 +5일 무료체험 복원

## 다음 작업자 시작점

1. `docs/DEVELOPMENT_STATUS_AND_ROADMAP_KO.md` 읽기
2. `docs/PRODUCT_SPEC_KO.md` 읽기
3. code83 제품 브랜치 기준으로 작업
4. 다음 P0는 실제 Google Play 결제
5. 통화 UI 변경 시 Android 16 회귀와 Play-safe 금지조건을 같이 확인
