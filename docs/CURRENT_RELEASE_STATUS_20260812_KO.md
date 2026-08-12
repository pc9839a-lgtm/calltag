# 콜태그 최신 릴리스·운영 상태

기준일: **2026-08-12 17:40 KST**  
Android 저장소: `pc9839a-lgtm/calltag`  
작업 브랜치: `agent/calltag-v04422-billing-live`  
서버 저장소: `pc9839a-lgtm/inlet` / `main`  
패키지명: `kr.pagero.calltag`

> 과거 문서와 충돌하면 이 문서와 실제 코드를 우선한다.

## 1. 현재 기준 버전

- 사용자 확인 기준 출발점: **0.44.22 / 2026081208**
- 마지막 signed Play 빌드: **0.44.24 / 2026081210**
- 현재 수정 소스: **0.44.25 / 2026081211**
- minSdk 26 / targetSdk 36 / compileSdk 36
- BillingClient 9.1.0
- 0.44.25 debug compile check: **성공**
- Compile Run: **31579132077**
- Compile Artifact: **9134449755**

### 0.44.25 Play release 서명 상태

0.44.25 소스 컴파일은 성공했지만 최종 Play AAB 서명은 현재 차단 상태다.

- 기존 Play 업로드키의 정상 SHA-256: `C3:4C:98:88:9B:0C:88:8A:BB:39:94:6C:80:16:96:C2:89:E2:82:6C:10:0F:41:7A:0B:CE:25:A3:92:C4:72:A7`
- 이전 CI가 사용하던 정상 업로드키 backup artifact `8922836146`은 **2026-08-12 만료**됨.
- 살아 있는 다른 backup artifact `8952526712`는 **다른 키**이며 정상 Play 업로드키가 아니므로 사용 금지.
- 새 랜덤 키로 임의 서명하지 않는다.
- release workflow는 이제 정확한 업로드키 GitHub Actions secrets가 없으면 실패하도록 변경했다.

정상 JKS를 복구해 secrets로 등록하거나, 실제 키가 완전히 유실된 경우 Google Play Console에서 upload key reset 후 새 키를 등록해야 한다.

## 2. Google Play 결제 — 실제 성공 확인

운영 D1에서 실제 결제 상태 확인 완료:

- productCode: `call_monthly`
- channel: `google_play`
- status: `active`
- verificationState: `verified`
- autoRenewing: `true`
- 서버 검증/저장: `2026-08-12 15:31:11 KST`
- expiry: `2026-09-12T06:31:04.910Z`

즉 `Play 구매 → purchaseToken → 서버 verify → Android Publisher API 검증 → DB verified → entitlement active`는 성공했다.

현재 Play 상품:

- `call_monthly` — 전화관리
- `message_monthly` — 문자자동화
- `all_monthly` — 현재 사용 안 함 / 생성하지 않음

## 3. 0.44.25 결제 버그 수정

기존 문제:

- 전화관리 하나를 구독하면 앱/서버가 `활성 구독 있음`으로 전체 구매를 막음.
- 결과적으로 문자자동화 추가 구매도 불가능했음.
- 화면에 `Google Play 결제 사용 가능`, `상품 확인` 등 개발자용 문구가 노출됨.
- 구매한 상품도 `이용 중`이 명확하지 않았음.

0.44.25 수정:

- 서버 entitlement를 상품별 상태로 확장:
  - `activeProducts`
  - `productAccess.call_monthly`
  - `productAccess.message_monthly`
  - `purchaseOptions.call_monthly`
  - `purchaseOptions.message_monthly`
- Google Play 전화관리 구독이 있어도 문자자동화는 별도 구매 가능.
- Google Play 문자자동화 구독이 있어도 전화관리는 별도 구매 가능.
- Web 통합 구독은 기존처럼 Play 중복결제 차단.
- 앱 로컬 캐시도 `phoneSubscribed` / `messageSubscribed`로 분리.

이용권 상단 표시:

- 전화관리만: **`전화관리 이용 중`**
- 문자자동화만: **`문자자동화 이용 중`**
- 둘 다: **`전화관리 · 문자자동화 이용 중`**

상품 버튼:

- 이미 구매: **`이용 중`** 비활성
- 미구매 전화관리: **`월 1,900원 시작`**
- 미구매 문자자동화: **`월 990원 시작`**

개발자용 결제 문구는 사용자 화면에서 제거했다.

서버 per-product entitlement 패치는 Cloudflare 운영 배포 완료.

## 4. Google 로그인 — 0.44.25 수정

0.44.24 실기기에서 Credential Manager 계정 선택 단계 실패가 확인됐다.

0.44.25는 다음 방식으로 수정했다.

1. 우선 Android Credential Manager 네이티브 Google 로그인을 시도.
2. 사용자가 직접 취소한 경우만 조용히 종료.
3. 그 외 provider/configuration/device 오류, timeout, 잘못된 credential 응답은 모두 기존 안전한 OAuth 로그인으로 자동 fallback.
4. fallback은 `https://pagero.kr/api/call/google/start?return_scheme=calltag` → Google 로그인 → signed state → callback → one-time ticket → 앱 세션 교환 흐름을 사용.
5. `앱 서명`, `클라이언트 설정`, `configuration_error` 등 개발자용 문구는 사용자에게 표시하지 않음.

운영 서버 설정은 이전 검증에서 정상 확인됨:

- Server Client ID 일치
- Google JWKS HTTP 200
- native ID token 서버 검증 경로 정상
- legacy OAuth fallback 설정 존재

### 설치 테스트 주의

0.44.24 ZIP의 `debug.apk`는 Play App Signing 인증서와 다르므로 네이티브 Credential Manager 설정 오류가 날 수 있다. 최종 검증은 Play 내부 테스트 AAB 설치본이 우선이다.

0.44.25에서는 네이티브 provider 오류가 나도 OAuth fallback으로 로그인할 수 있도록 보강했다.

## 5. 남은 P0

1. 정상 Play upload JKS 복구 또는 Play upload key reset.
2. `0.44.25 / 2026081211` signed AAB 생성.
3. Play 내부 테스트 설치.
4. Google 로그인 E2E 확인.
5. 전화관리 구독 상태에서 문자자동화 추가 결제 확인.
6. 구매 복원 / 재설치 복원 확인.

## 6. 이후 P1

- RTDN / Pub/Sub
- 갱신, 취소, 만료, grace, account hold, refund lifecycle 동기화
- 주기적 reconciliation

## 7. 고정 정책

- CallTag 무료체험: 기본 7일 + 추천인 7일 = 최대 14일
- 무료 종료 후 자동결제 없음
- 고객/통화/메모/일정/문자 데이터는 구독 만료나 결제 패치로 삭제하지 않음
- purchaseToken 원문 장기 저장 금지
- Google service account private key 저장소/문서/채팅 노출 금지
- `all_monthly`는 사용자가 다시 지시하기 전 생성/판매하지 않음
