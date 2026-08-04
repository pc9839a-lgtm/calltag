# CallTag 결제·추천 구현 현황

## 현재 버전

- Android: `0.42.2`
- versionCode: `62`
- Draft PR: `pc9839a-lgtm/calltag#40`
- 공통 서버 Draft PR: `pc9839a-lgtm/inlet#69`

## 제품 경계

- 콜태그는 전화관리·문자자동화 Android 앱이다.
- 페이지로는 랜딩페이지 제작·문의 수집 웹서비스다.
- 페이지로 단독 사용자는 콜태그 앱 없이 사용할 수 있다.
- 페이지로 웹 결제와 CallTag Google Play 결제는 동일 서버 이용권을 기준으로 중복을 차단한다.

## 구현 완료

### 더보기

- 이용권·결제
- 친구 초대·파트너
- 추천코드 복사·공유·등록
- 추천 회원·유료 회원·예상 수익·확정 수익

### 상품

| 상품 코드 | 상품명 | 월 이용료 | 범위 |
|---|---:|---:|---|
| `call_monthly` | 전화관리 | 1,900원 | 통화 후 고객관리 |
| `message_monthly` | 문자자동화 | 990원 | 자동문자·단체문자 |
| `all_monthly` | 통합권 | 6,000원 | 전화·문자·페이지로 |

### 무료기간

- 기본 3일
- 추천인 최초 등록 시 +5일
- 최대 8일
- 앱 삭제·재설치로 초기화되지 않음
- 서버 시각 기준 만료 판정
- 무료기간 종료 후 자동 결제 없음

## 만료 후 정책

유지:

- 고객 데이터
- 상담 이력
- 메모·일정
- 문자 템플릿
- 기존 발송 내역
- 백업·복원

제한:

- 신규 통화 후 정리
- 신규 자동문자
- 신규 그룹·단체문자
- 실제 SMS 전송

종료 24시간 전과 종료 후 안내 화면을 제공한다. 같은 안내는 서버 기준 날짜당 한 번 표시한다.

## 서버 시각 판정

`GET /api/billing/entitlements`의 `serverNow`를 기기 시각과 함께 저장한다.

오프라인 상태에서는 마지막 서버 확인 이후 경과시간을 더해 예상 서버 시각을 계산한다. 기기 날짜를 뒤로 돌려도 무료기간이 다시 늘어나지 않는다.

## Google Play 미등록 준비 모드

현재 Google Play Console에 앱과 구독 상품이 등록되지 않았다.

서버 `billingAvailability.googlePlay.available=false`일 때:

- BillingClient 연결하지 않음
- 상품 조회하지 않음
- 결제 버튼 `출시 준비 중`
- 구매 복원 비활성화
- Play 구독 관리 숨김

서버 최종 차단:

```text
GOOGLE_PLAY_PRODUCTS_READY=0
GOOGLE_PLAY_BILLING_ENABLED=0
```

구버전 APK나 변조 앱이 구매 검증 API를 직접 호출해도 서버가 Android Publisher API 호출 전에 차단한다.

## 중복결제 방지

- 결제 전 서버 이용권 재조회
- 웹 활성 구독 시 Play 결제 차단
- Play 활성 구독 시 웹 checkout 사전 차단
- 서버 확인 실패 시 결제 미진행
- Play 구매 성공만으로 권한을 열지 않음
- 서버 구매 검증 성공 후에만 이용권 반영

## 추천 링크 자동 귀속

공유 링크:

```text
https://pagero.kr/r/{추천코드}
```

앱 링크:

```text
calltag://referral?code={추천코드}
```

- 로그인 전 최대 30일 코드 보관
- 로그인 후 자동 등록
- 일시적 실패는 다음 전면 실행에서 재시도
- 본인 추천·중복 등록·유료 전환 후 등록·존재하지 않는 코드는 재시도하지 않음
- 수동 입력 fallback 유지

Play Console 미등록 상태에서는 설치 전 완전한 deferred deep link는 지원하지 않는다. 현재는 설치된 앱 자동 귀속과 코드 표시·수동 입력을 지원한다.

## 파트너센터 제외

현재 패치에서는 다음을 구현하지 않는다.

- 파트너센터 모바일 웹
- 파트너센터 열기 버튼
- 출금 신청
- 계좌 등록
- 추천 회원별 상세 화면
- 세금·정산 서류 처리

파트너 요약 숫자만 앱에 유지한다.

## 서버 API

```text
GET  /api/billing/entitlements
GET  /api/billing/subscriptions
GET  /api/billing/readiness
POST /api/billing/web/precheck
POST /api/billing/google/verify
POST /api/billing/google/restore
GET  /api/referrals/me
POST /api/referrals/apply
GET  /api/referrals/summary
```

## 기능 게이트

- 전화관리: `call_monthly` 또는 `all_monthly`
- 문자자동화: `message_monthly` 또는 `all_monthly`
- 무료 이용 중: `all_monthly` 범위 체험

템플릿과 기존 발송 기록은 만료 후에도 확인할 수 있다. 자동문자·단체문자 진입과 실제 SMS 전송 단계에서는 이용권을 다시 확인한다.

## 결제 활성화 전 필수

1. Play Console 앱 등록
2. `call_monthly`, `message_monthly`, `all_monthly` 등록
3. 기본 요금제와 판매 국가 설정
4. 서비스 계정 API 액세스 연결
5. Cloudflare 서비스 계정 환경변수 등록
6. 라이선스 테스터 구매·복원 검증
7. `GOOGLE_PLAY_PRODUCTS_READY=1`
8. 최종 승인 후 `GOOGLE_PLAY_BILLING_ENABLED=1`

## 로컬 저장 금지

- Google Play 구매 토큰 원문
- 웹 PG 결제 식별자 원문
- 파트너 계좌·세금정보
- 다른 회원 개인정보

## 안전 규칙

- Draft PR에서 운영 배포하지 않음
- 운영 D1 migration 실행하지 않음
- Play flag 활성화하지 않음
- 고객·상담·문자 데이터를 삭제하지 않음
- 파트너 수익을 앱에서 직접 계산하지 않음
- 파트너센터가 구현됐다고 표시하지 않음

## 상세 문서

- `docs/V0421_PLAY_PREREGISTRATION_MODE_KO.md`
- `docs/V0422_ENTITLEMENT_LIFECYCLE_REFERRAL_ATTRIBUTION_KO.md`
- 서버 `docs/CALLTAG_ENTITLEMENT_LIFECYCLE_WEB_PRECHECK_KO.md`
