# 콜태그 통합 제품 기획서

기준일: **2026-08-11**  
제품명: **콜태그(CallTag)**  
Android 패키지: `kr.pagero.calltag`

> **중요:** 현재 제품 정의는 [`PRODUCT_SPEC_KO.md`](PRODUCT_SPEC_KO.md), 실제 구현 상태는 [`DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`](DEVELOPMENT_STATUS_AND_ROADMAP_KO.md)를 우선한다. Google Play 결제 작업은 [`GOOGLE_PLAY_BILLING_SETUP_KO.md`](GOOGLE_PLAY_BILLING_SETUP_KO.md)를 정본으로 사용한다.

이 문서는 오래된 통합 기획에서 현재까지 확정된 제품 경계만 남기는 요약본이다.

## 현재 확정 상품

| 상품 | 월 가격 | Play 상품 ID |
|---|---:|---|
| 전화관리 | 1,900원 | `call_monthly` |
| 문자자동화 | 990원 | `message_monthly` |
| 페이지로 | 3,500원 | 앱 내 Play 상품 아님 / 웹 단독 관리 |
| 통합권 | 6,000원 | `all_monthly` |

`통합 2,500원`, `Classic/Pro`는 폐기된 가격안이다.

## 무료체험·추천

- 일반 신규가입: 통합권 7일 무료
- 회원가입 시 추천코드 입력: +7일
- 추천가입자: 총 14일 무료
- 가입 후 추천코드 재입력 불가
- 무료체험은 **콜태그 서버 entitlement**로 부여
- 무료체험 종료 후 자동결제하지 않음
- 현재 정책과 충돌하므로 Play Console에 별도 7일 무료체험 Offer를 만들지 않음

## 현재 핵심 UX

- 하단 탭: `고객 / 캘린더 / 홈 / 통계 / 더보기`
- 통화 종료 후 **작은 팝업 1개를 자동 표시**
- 작은 팝업: 고객명 + 메모 + 저장 중심
- 통화 후 팝업에서 `이 번호 제외` 가능
- 더보기 > 통화 > `통화 후 팝업 제외`에서 제외번호 직접 추가/해제 가능
- 제외번호는 이후 통화 종료 팝업을 표시하지 않음
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

Play 배포 앱에서 디지털 서비스의 외부 웹 결제를 유도하는 버튼/링크를 임의로 추가하지 않는다. 페이지로 웹 구독 사용자는 서버 entitlement로 중복 결제를 차단하되, Play 정책상 허용되지 않은 외부 결제 CTA를 앱에 넣지 않는다.

## UI 원칙

- 다크 테마 기본
- 기능별 화면만 별도 밝은 디자인으로 만들지 않음
- 상태바 시간·배터리·신호 아이콘은 보이게 유지
- inset 중복 금지
- Android 기본 흰 Dialog/Picker는 가능한 한 제거
- 기존 콜태그 primary 블루는 유지
- 새 통화후/제외 액션은 **Concept B** 기준: 차콜 표면 + 뮤트 블루 포인트 + 아이스블루 primary
- 새 제외 UI에서 빨간 danger 버튼 사용 금지
- 닫기/뒤로가기 아이콘은 고스트 스타일 우선

## Play 정책 원칙

현재 Play용 앱에서는 다음을 하지 않는다.

- `WRITE_CALL_LOG`
- `CallLog.Calls.CACHED_NAME` 수정
- 삼성/Google 기본 전화앱 최근통화 메모 강제 삽입
- 사용자 원본 연락처 이름 변경

콜태그 고객·메모·일정·통화 interaction은 콜태그 DB/UI에서 관리한다.

## Google Play 결제 구현 상태

앱 클라이언트에는 다음이 구현돼 있다.

- `com.android.billingclient:billing:9.1.0`
- 구독 상품 조회 (`ProductDetails`)
- 월 구독 구매창 실행
- `obfuscatedAccountId` 적용
- PENDING/PURCHASED 상태 분기
- 구매 완료 후 서버 `/api/billing/google/verify` 호출
- 구매 복원 후 서버 `/api/billing/google/restore` 호출
- 서버 entitlement 조회 후 웹 구독/기존 구독 중복결제 차단
- Google Play 구독 관리 화면 진입

현재 Play Console에 만들 상품 ID:

- `call_monthly` — 전화관리 — 1,900원/월
- `message_monthly` — 문자자동화 — 990원/월
- `all_monthly` — 통합권 — 6,000원/월

페이지로 3,500원 단독권은 현재 CallTag Android Play 상품으로 만들지 않는다.

서버에서 반드시 완료/검증해야 하는 항목:

- Google Play Developer API 서비스 계정
- `purchases.subscriptionsv2.get` 기반 purchaseToken 검증
- 검증 성공 후 서버 entitlement 반영
- 구독 acknowledgement
- purchaseToken 기준 중복 방지
- RTDN(Pub/Sub) 수신
- RENEWED / CANCELED / EXPIRED / GRACE_PERIOD / ON_HOLD / RECOVERED 상태 동기화
- 웹 구독과 Play 구독 상호 중복 방지

상세 절차는 `GOOGLE_PLAY_BILLING_SETUP_KO.md`를 따른다.

## 현재 릴리스

- `v0.44.14`
- `versionCode 2026081101`
- `targetSdk 36`
- Android 16/API36 회귀검사 PASS
- signed Play AAB 검증 PASS
- 최신 UI: Concept B 블루 포인트 버튼 시스템
- 통화 후 자동 작은 팝업 + 팝업 제외목록 포함

## 다음 최우선

**Google Play 결제 실연동을 P0로 진행한다.**

순서:

1. Play Console 결제 프로필 확인
2. `call_monthly`, `message_monthly`, `all_monthly` 구독 생성
3. 각 상품에 `monthly` 자동갱신 base plan 생성/활성화
4. 라이선스 테스터 등록
5. Google Play Developer API 서비스 계정 생성 및 권한 부여
6. 서버 purchaseToken 검증 + acknowledgement 완성
7. RTDN Pub/Sub 연결
8. 내부 테스트 트랙에서 실제 테스트 결제
9. 구매/갱신/취소/보류/복원 회귀 테스트
10. 결제 활성화 플래그를 운영으로 전환

상세 구현·화면·회귀금지 항목은 반드시 현재 정본 문서를 확인한다.
