# CallTag 다음 작업 인수인계 — 2026-08-12

## 현재 기준

- 저장소: `pc9839a-lgtm/calltag`
- 브랜치: `agent/calltag-v04422-billing-live`
- 사용자 기준 출발: `0.44.22 / 2026081208`
- 마지막 signed Play 빌드: `0.44.24 / 2026081210`
- 현재 수정 소스: **`0.44.25 / 2026081211`**
- 서버: `pc9839a-lgtm/inlet` / `main`
- 패키지: `kr.pagero.calltag`

## 0.44.25 완료

### 결제

실제 운영 결제는 이미 성공 확인:

- `call_monthly`
- `google_play`
- `active`
- `verified`
- 자동갱신 true

0.44.25에서 수정한 핵심 버그:

- 전화관리 구독이 문자자동화 추가 구매까지 막던 global purchase block 제거.
- 서버/앱 모두 상품별 보유 상태로 변경.
- `call_monthly`와 `message_monthly`를 각각 구매 가능.
- Web 통합 구독만 Play 중복결제 차단.
- 상단에 `전화관리 이용 중`, `문자자동화 이용 중`, 또는 `전화관리 · 문자자동화 이용 중` 표시.
- 구매한 상품 버튼은 `이용 중` 비활성.
- 미구매 상품만 결제 가능.
- 개발자용 결제 문구 제거.

서버 `entitlements.js` per-product 패치 운영 배포 완료.

### Google 로그인

0.44.24 실기기에서 Credential Manager 계정 선택 실패 확인.

0.44.25:

- Credential Manager 네이티브 로그인 우선.
- 사용자 직접 취소 외 모든 provider/configuration/timeout/credential 오류는 기존 OAuth 로그인으로 자동 fallback.
- fallback: `/api/call/google/start?return_scheme=calltag` → signed state → Google callback → one-time ticket → 앱 세션.
- 사용자 화면에서 `configuration_error`, 앱 서명, 클라이언트 설정 등 개발자 문구 제거.

서버 Google 설정은 이전 검증에서 Server Client ID 일치 + JWKS HTTP 200 확인됨.

## 컴파일

- 0.44.25 compile check: **SUCCESS**
- Run: `31579132077`
- Artifact: `9134449755`
- artifact name: `calltag-v0.44.25-code2026081211-compile-check`

## 현재 blocker — Play upload key

0.44.25 최종 signed AAB만 아직 못 만들었다.

- 정상 Play upload key SHA-256:
  `C3:4C:98:88:9B:0C:88:8A:BB:39:94:6C:80:16:96:C2:89:E2:82:6C:10:0F:41:7A:0B:CE:25:A3:92:C4:72:A7`
- CI가 사용하던 정상 key backup artifact `8922836146`은 2026-08-12 만료.
- 살아 있는 backup `8952526712`는 다른 fingerprint이므로 절대 사용하지 않음.
- signed workflow는 이제 정확한 `CALLTAG_UPLOAD_*` GitHub Actions secrets가 없으면 실패하도록 고정.

### 다음 처리

1. 사용자가 기존 정상 JKS를 보관하고 있으면 GitHub Actions secrets에 복구.
2. 없다면 Google Play upload key reset 진행.
3. 이후 `0.44.25 / 2026081211` signed AAB 빌드.
4. Play 내부 테스트 설치 후 Google 로그인 + 문자자동화 추가 구매 E2E.

## 그 다음

- 구매 복원 / 재설치 복원
- RTDN / Pub/Sub
- 갱신·취소·만료·grace·hold·refund 동기화

## 고정 정책

- Play 상품: `call_monthly`, `message_monthly`
- `all_monthly`는 현재 만들지 않음
- CallTag 무료체험 7일 + 추천인 7일 = 최대 14일
- 고객/통화/메모/일정/문자 데이터 삭제 금지
- purchaseToken/private key 원문 노출 금지

우선 `docs/CURRENT_RELEASE_STATUS_20260812_KO.md`를 읽고 이어서 작업한다.
