# 콜태그 통합 제품 기획서

기준일: **2026-08-08**  
제품명: **콜태그(CallTag)**  
Android 패키지: `kr.pagero.calltag`

> **중요:** 2026-07-31 이전 기획을 그대로 구현하지 않는다. 현재 제품 정의는 [`PRODUCT_SPEC_KO.md`](PRODUCT_SPEC_KO.md), 실제 구현 상태는 [`DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](DEVELOPMENT_STATUS_AND_ROADMAP_KO.md)를 우선한다.

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
- 고객 추가의 최근통화는 실제 Android CallLog 최신순을 사용
- 등록 고객과 동일 번호 반복 통화를 숨기지 않음
- 통계 차트 터치 시 해당 날짜 수치 표시
- `통화 후 자동문자`는 더보기 상단의 독립 대형 카드

## 페이지로

페이지로와 콜태그는 연동 가능하지만 서로 다른 서비스다.

앱에서 반드시 분리:

- `페이지로 연동`: 연결 상태·문의 확인
- `페이지로 서비스 안내`: 페이지로 서비스 설명

정산:

`https://calltag.pagero.kr/web/settlement`

## Play 정책 원칙

현재 Play용 앱에서는 다음을 하지 않는다.

- `WRITE_CALL_LOG`
- `CallLog.Calls.CACHED_NAME` 수정
- 삼성/Google 기본 전화앱 최근통화 메모 강제 삽입
- 사용자 원본 연락처 이름 변경

콜태그 고객·메모·일정·통화 interaction은 콜태그 DB/UI에서 관리한다.

## 현재 릴리스

- `v0.44.5`
- `versionCode 83`
- `targetSdk 36`
- Android 16/API36 회귀검사 PASS
- signed Play AAB 검증 PASS

## 다음 최우선

실제 Google Play 결제 완성:

- CALL / MESSAGE / PAGERO / ALL_IN_ONE 권한
- 구매/acknowledgement
- 서버 검증
- 구매 복원
- 만료/해지/grace/account hold
- 웹 결제 중복 방지
- 무료체험 entitlement에서 유료 entitlement 전환

상세 구현·화면·회귀금지 항목은 반드시 현재 정본 문서를 확인한다.
