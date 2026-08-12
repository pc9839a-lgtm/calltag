# 콜태그 이용권·결제·추천인 구현 문서 — 폐기/이관

이 문서는 2026-08-04 기준의 과거 구현 명세였으며 현재 정책과 코드 상태가 달라졌습니다.

**결제 작업은 아래 최신 정본을 먼저 읽고 진행합니다.**

- `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`

최신 정본에는 다음이 포함되어 있습니다.

- Android `0.44.20` / `versionCode 2026081206` 기준 실제 결제 코드 상태
- Google Play Billing 9.1.0 구현 위치
- 서버 entitlement / verify / restore / acknowledge 구조
- 웹↔Google Play 중복결제 방지
- PageRo 요금제와 CallTag 통합 표시 정책
- 현재 앱 코드에 남은 legacy 상품값 구분
- CallTag 무료기간 7일 / 추천인 +7일 현재 서버 동작
- `_shared.js`의 3일/+5일 legacy 정책 불일치
- 추천인 입력은 회원가입 시에만 노출하는 UX 원칙
- 파트너/정산 분리
- Play Console 공개 전 P0/P1 작업 순서
- RTDN, 환불, 취소, grace/account hold, reconciliation 등 운영 미완료 항목
- 다음 AI가 읽어야 할 Android/Server 파일 목록

과거 이 문서에 있던 `0.42.x`, Google Play 미연결 전제, 3일/+5일 정책 등을 현재 정본으로 사용하지 마십시오.
