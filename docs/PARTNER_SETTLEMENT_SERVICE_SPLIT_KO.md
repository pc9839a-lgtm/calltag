# 콜태그 × 페이지로 정산 서비스 분리 계약

최종 갱신: 2026-08-07

## 1. 화면 구조

정산 웹은 다음 3개 범위를 제공한다.

1. `ALL` — 콜태그 + 페이지로 전체 합계
2. `CALLTAG` — 콜태그 유료 구독에서 발생한 추천 수익
3. `PAGERO` — 페이지로 유료 구독에서 발생한 추천 수익

전체 합계는 항상 상단에 고정하고, 서비스 탭을 바꿔도 총 추천 회원, 이번 달 총 예상 수익, 총 정산 가능 금액, 총 누적 지급 금액을 유지한다.

## 2. API 요청 계약

아래 조회 API는 `service` 쿼리 파라미터를 지원해야 한다.

- `GET /api/partner/dashboard?service=CALLTAG`
- `GET /api/partner/dashboard?service=PAGERO`
- `GET /api/partner/referrals?service=CALLTAG`
- `GET /api/partner/referrals?service=PAGERO`
- `GET /api/partner/earnings?service=CALLTAG`
- `GET /api/partner/earnings?service=PAGERO`
- `GET /api/partner/settlements?service=CALLTAG`
- `GET /api/partner/settlements?service=PAGERO`

파라미터가 없거나 `service=ALL`이면 두 서비스를 합산한다.

지급 요청:

```json
POST /api/partner/settlements/request
{
  "service": "ALL | CALLTAG | PAGERO"
}
```

## 3. 응답 필수값

각 추천 회원, 수익 원장, 정산 원장에는 반드시 다음 필드를 포함한다.

```json
{
  "service": "CALLTAG | PAGERO"
}
```

대시보드 권장 응답:

```json
{
  "totals": {
    "referralCount": 0,
    "paidReferralCount": 0,
    "estimatedEarnings": 0,
    "availableAmount": 0,
    "totalPaidAmount": 0
  },
  "services": {
    "CALLTAG": {},
    "PAGERO": {}
  }
}
```

## 4. 분류 원칙

- 한 결제 건은 하나의 서비스에만 귀속한다.
- 콜태그와 페이지로를 함께 이용해도 각 상품 결제 원장을 분리한다.
- 전체 합계는 두 서비스 원장의 합이며 별도 수익을 추가로 발생시키지 않는다.
- 기존 `service` 값이 없는 레거시 원장은 콜태그로 이관한다.
- 페이지로 탭에는 `service=PAGERO`가 확인된 원장만 표시한다.
- 환불, 취소, 차지백도 원 결제와 동일한 서비스 원장에서 차감한다.
- 한 서비스가 보류돼도 다른 서비스의 정상 확정 수익은 별도로 지급할 수 있어야 한다.

## 5. 추천 귀속

통합 추천코드는 사용할 수 있지만 수익은 실제 결제 상품 기준으로 분리한다.

예시:

- 추천 회원이 콜태그만 결제: `CALLTAG`
- 추천 회원이 페이지로만 결제: `PAGERO`
- 추천 회원이 두 서비스를 결제: 결제 건별로 각각 `CALLTAG`, `PAGERO`

추천 회원 수 전체 합계는 동일 회원 중복 여부를 서버 정책으로 명시해야 한다. 권장 기준은 전체 합계에서는 고유 계정 수, 서비스별 화면에서는 해당 서비스 유료·가입 계정 수다.

## 6. 프론트엔드 안전 처리

현재 정산 웹은 다음 방식으로 처리한다.

- 서비스 탭별 API 요청에 `service` 값을 전달한다.
- 응답 항목의 `service`, `serviceCode`, `productService`, `productCode`, `productName`을 확인한다.
- 미분류 레거시 데이터는 콜태그로만 표시한다.
- 미분류 데이터를 페이지로 탭에 중복 노출하지 않는다.
- 서버가 `PAGERO` 원장을 제공하기 전까지 페이지로 탭은 빈 상태로 유지한다.

## 7. 약관·동의 기록

- 지급 요청 전 파트너 이용약관과 정산정책 동의를 받는다.
- 계좌·세금정보 저장 전 파트너 약관, 정산정책, 개인정보 처리 안내 동의를 받는다.
- 서버는 약관 버전, 동의 시각, 계정, 서비스 범위, 요청 IP를 감사 로그로 저장해야 한다.
- 브라우저 저장값만을 법적 동의 원본으로 사용하지 않는다.
