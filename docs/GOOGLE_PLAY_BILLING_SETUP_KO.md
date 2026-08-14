# CallTag Google Play 결제 실연동 가이드

기준일: **2026-08-14**  
앱: **CallTag 0.44.38 / versionCode 2026081224**  
패키지: `kr.pagero.calltag`  
Billing Library: `com.android.billingclient:billing:9.1.0`

## 1. 현재 Play 상품

현재 Android Play 정기결제는 아래 2개만 사용한다.

| 기능 | Product ID | 월 가격 |
|---|---|---:|
| 전화관리 | `call_monthly` | 1,900원 |
| 문자자동화 | `message_monthly` | 990원 |

**현재 `all_monthly`는 만들거나 조회하지 않는다.** 과거 통합권 6,000원 Play 상품 정의는 폐기한다.

페이지로 웹 요금제는 Android Play 구독과 별도다.

## 2. 현재 구현 상태

완료:

- BillingClient 연결
- `call_monthly`, `message_monthly` ProductDetails 조회
- 구매 실행
- 구매 복원
- purchase token 서버 검증
- 실제 `call_monthly` 활성/검증/autoRenew 확인
- entitlement UI 반영
- Billing 연결 끊김 재연결
- 상품조회 실패/타임아웃 재시도 UI

### 성능 구조

결제 화면 진입 시:

1. Google Play Billing 연결/상품조회를 즉시 시작
2. 서버 entitlement 조회를 별도 백그라운드 실행
3. 서버의 `playBillingAvailable` 응답을 기다려 BillingClient를 시작하지 않음
4. ProductDetails가 오면 결제 버튼을 즉시 활성화
5. 연결/상품조회 실패 시 무한 `결제 준비 중` 대신 `다시 시도`

즉 Play 상품조회와 서버 entitlement 조회가 서로 발목을 잡지 않게 분리되어 있다.

## 3. 서버 검증

앱에서 결제 성공만으로 영구 권한을 확정하지 않는다.

```text
Google Play 구매
→ purchaseToken 확보
→ 서버 검증 API
→ Google Play Developer API 조회
→ productId / package / purchase state 확인
→ entitlement 반영
```

민감한 Google Play 서비스계정 private key는 앱이나 문서에 넣지 않는다.

## 4. 아직 미구현: RTDN

Google Play Real-time Developer Notifications 기반 lifecycle 자동 동기화는 아직 완료되지 않았다.

남은 항목:

- Pub/Sub topic
- Google Play notification publisher 권한
- Play Console RTDN 설정
- subscriber endpoint
- 알림 수신 후 Google Play Developer API 재조회
- renewal
- cancel
- expiry
- grace period
- account hold
- resume
- refund/revoke
- 해당 결과에 따른 entitlement 갱신

RTDN이 없으면 사용자가 앱을 열어 복원/조회할 때 상태가 보정될 수는 있지만, 서버가 모든 구독 변경을 실시간으로 추적한다고 기록하면 안 된다.

## 5. 결제 국가 오류

`거주 중인 국가에서는 결제할 수 없습니다`가 나오면 아래를 순서대로 확인한다.

1. 휴대폰 Play Store에서 실제 결제 테스트 중인 Google 계정 확인
2. 그 계정의 Google Play 국가가 대한민국인지
3. Google 결제 프로필 국가가 대한민국인지
4. Play Console 비공개 테스트 트랙 대상 국가에 대한민국 포함 여부
5. `call_monthly` 기본 요금제의 대한민국 판매 가능 여부
6. `message_monthly` 기본 요금제의 대한민국 판매 가능 여부
7. 필요 시 해당 계정 라이선스 테스트 등록

이 오류는 앱 코드 문제가 아니라 계정/상품 국가 설정 때문에 발생할 수 있다.

## 6. Google 로그인과 Billing을 혼동하지 말 것

Google 로그인 OAuth와 Google Play Billing은 별개다.

Google 로그인 Web/Backend Client ID:

`31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`

Android OAuth Client ID:

`31346298247-26okq7jrsac89q8pucjeuui6jrfofvqn.apps.googleusercontent.com`

Billing 상품 조회/결제는 OAuth Client ID를 사용하지 않는다.

## 7. Play 업로드 서명

현재 업로드 인증서:

- SHA-1: `79:80:FD:C6:4E:BE:DD:2B:80:54:5B:60:87:03:6D:5F:78:05:75:8B`
- SHA-256: `C3:4C:98:88:9B:0C:88:8A:BB:39:94:6C:80:16:96:C2:89:E2:82:6C:10:0F:41:7A:0B:CE:25:A3:92:C4:72:A7`

CI는 위 인증서와 다르면 릴리스 AAB 생성을 실패시킨다. 새 업로드 키 자동 생성은 금지한다.

## 8. 배포 체크

Play 업로드 전:

- `versionCode`가 기존 Play 등록값보다 큰지
- `call_monthly`, `message_monthly`만 조회하는지
- AAB가 기존 업로드키로 서명됐는지
- 내부테스트 대상 국가/테스터가 맞는지
- 실제 Play 설치본에서 구매창이 정상 노출되는지
- 구매 후 서버 entitlement가 갱신되는지

현재 기준 릴리스는 `0.44.38 / 2026081224`다.
