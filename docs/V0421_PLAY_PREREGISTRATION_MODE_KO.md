# CallTag v0.42.1 Google Play 미등록 준비 모드

- 작성일: 2026-08-04
- 앱 버전: `0.42.1` / versionCode `61`
- 기준 브랜치: `docs/billing-referral-app-handoff-20260804`
- 서버 연동: `pc9839a-lgtm/inlet` PR #69

## 1. 현재 운영 전제

현재 CallTag 앱과 구독 상품은 Google Play Console에 아직 등록되지 않았다.

따라서 앱에 Billing Library 코드가 포함되어 있더라도 다음 작업을 수행하면 안 된다.

- 앱 실행 직후 BillingClient 연결
- 등록되지 않은 상품 조회 오류를 사용자에게 노출
- 상품 카드의 결제 버튼 활성화
- 구매 복원 요청 실행
- Play 구매 검증 API 호출
- 무료 이용 종료 후 자동으로 결제창 표시

## 2. 이번 패치 동작

서버 이용권 응답의 다음 값을 앱이 정본으로 사용한다.

```json
{
  "billingAvailability": {
    "googlePlay": {
      "available": false,
      "stage": "pre_registration",
      "reasonCode": "PLAY_RELEASE_DISABLED",
      "message": "앱 결제 기능을 준비하고 있습니다."
    }
  }
}
```

`available=false`이면 앱은 다음처럼 동작한다.

1. BillingClient에 연결하지 않는다.
2. Play 상품 조회를 하지 않는다.
3. 상품 가격과 포함 기능은 안내용으로 유지한다.
4. 모든 결제 버튼을 `출시 준비 중`으로 표시하고 비활성화한다.
5. 구매 복원 버튼을 비활성화한다.
6. Google Play 구독 관리 버튼을 숨긴다.
7. 무료 3일과 추천인 +5일은 정상 표시한다.
8. 페이지로 웹 구독 중복결제 판정은 계속 수행한다.

## 3. 결제 활성화 조건

Google Play 결제는 다음 조건을 모두 만족해야 열린다.

- 서버 `GOOGLE_PLAY_BILLING_ENABLED=1`
- 서버 `GOOGLE_PLAY_PRODUCTS_READY=1`
- 서버 `GOOGLE_PLAY_CLIENT_EMAIL` 등록
- 서버 `GOOGLE_PLAY_PRIVATE_KEY` 등록
- Play Console 앱 패키지 `kr.pagero.calltag` 등록
- 구독 상품 3개 등록
  - `call_monthly`
  - `message_monthly`
  - `all_monthly`
- 각 상품의 기본 요금제와 판매 국가 설정 완료
- Play Console API 액세스에 서비스 계정 연결
- 라이선스 테스터 구매·복원 검증 완료

하나라도 빠지면 서버는 계속 `available=false`를 반환한다.

## 4. 활성화 순서

결제 오픈은 아래 순서를 바꾸지 않는다.

1. Play Console 앱 등록
2. 구독 상품·기본 요금제 등록
3. 서비스 계정과 Play Console API 액세스 연결
4. Cloudflare Production에 서비스 계정 환경변수 등록
5. `GOOGLE_PLAY_PRODUCTS_READY=1` 설정
6. 내부 테스트 트랙 APK 업로드
7. 라이선스 테스터 구매·복원·취소 확인
8. 서버 검증·구매 승인·중복결제 차단 확인
9. 마지막에 `GOOGLE_PLAY_BILLING_ENABLED=1` 설정

`GOOGLE_PLAY_BILLING_ENABLED`는 최종 오픈 스위치다. 상품 등록만 끝났다는 이유로 먼저 켜면 안 된다.

## 5. 실패 안전 기준

- 서버 이용권 확인 실패 시 결제를 시작하지 않는다.
- 서버가 결제 준비 완료를 반환하지 않으면 BillingClient에 연결하지 않는다.
- Play 상품이 3개 모두 조회되지 않아도 해당 상품 버튼은 활성화하지 않는다.
- Play 구매 성공 콜백만으로 기능을 열지 않는다.
- 서버 Android Publisher 검증 성공 후에만 이용권을 반영한다.
- 구매 토큰은 앱 이용권 캐시에 저장하지 않는다.
- 고객·상담·문자·일정 데이터는 결제 준비 상태와 무관하게 삭제하지 않는다.

## 6. QA 항목

- `출시 준비 중` 버튼 표시
- 준비 모드에서 BillingClient `startConnection` 미호출
- 준비 모드에서 상품 조회 미호출
- 준비 모드에서 구매·복원 미호출
- 무료 3일·추천 +5일 정상 표시
- 페이지로 웹 구독 중복결제 차단 유지
- 준비 완료 후에만 상품별 버튼 활성화
- v0.42.1 Debug APK 빌드 성공

## 7. 다음 패치

Google Play Console 등록 전에는 결제 기능을 추가 확장하지 않는다. 다음 개발 우선순위는 다음과 같다.

1. 만료 후 데이터 열람·신규 작업 제한 정책
2. 무료기간 만료 안내
3. 웹 구독 상태 표시 고도화
4. 파트너센터 모바일 웹 연결
5. Play 등록 완료 후 내부 테스트 결제 연결
