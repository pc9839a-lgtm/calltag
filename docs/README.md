# CallTag 문서 인덱스

기준일: **2026-08-14**

## 정본 문서

| 우선순위 | 문서 | 용도 |
|---|---|---|
| 1 | `ANDROID_DEVELOPER_HANDOFF_KO.md` | 현재 Android 구현 상태·최신 버전·주의사항 |
| 2 | `PRODUCT_SPEC_KO.md` | 현재 제품 정책·UX·상품 범위 |
| 3 | `GOOGLE_PLAY_BILLING_SETUP_KO.md` | Google Play 결제·상품·서버 검증 |
| 4 | `PAGERO_CUSTOMER_INTEGRATION_KO.md` | 페이지로 문의 → 콜태그 연동 |
| 5 | `MESSAGE_AUTOMATION_SPEC_KO.md` | 문자 자동화 상세 |
| 6 | `ROADMAP_KO.md` | 남은 작업과 QA |

충돌 시 실제 코드와 `app/build.gradle`이 최우선이다.

## 유지하는 기능 문서

- `ACCOUNT_PRIVACY_FLOW_KO.md`
- `ANDROID_CRASH_HARDENING_KO.md`
- `BILLING_REFERRAL_APP_IMPLEMENTATION_KO.md`
- `BRAND_WEB_CONSOLIDATION_KO.md`
- `CALL_FLOW_MANAGEMENT_KO.md`
- `CRM_STAGE_CUSTOMIZATION_KO.md`
- `CUSTOMER_CLASSIFICATION_KO.md`
- `DATA_MODEL_KO.md`
- `DESIGN_SYSTEM_KO.md`
- `FIREBASE_REGISTRATION_GUIDE_KO.md`
- `GOOGLE_PLAY_STORE_VISUAL_ASSETS_BRIEF_KO.md`
- `SCREEN_FLOW_KO.md`
- `WEB_SERVICE_SPEC_KO.md`

## 문서 운영 규칙

1. 새 버전이 나와도 `V0xxx_*.md` 릴리스 문서를 계속 만들지 않는다.
2. 일회성 `HOTFIX`, `NEXT_AI_HANDOFF`, 날짜별 `CURRENT_RELEASE_STATUS` 문서를 새로 쌓지 않는다.
3. 최신 변경은 `ANDROID_DEVELOPER_HANDOFF_KO.md`에 반영한다.
4. 제품 정책 변경은 `PRODUCT_SPEC_KO.md`에 반영한다.
5. 결제 변경은 `GOOGLE_PLAY_BILLING_SETUP_KO.md`에 반영한다.
6. 페이지로 연동 변경은 `PAGERO_CUSTOMER_INTEGRATION_KO.md`에 반영한다.
7. 구현 완료/남은 작업은 `ROADMAP_KO.md`에 반영한다.
8. 비밀번호, 서비스계정 private key, keystore 파일 내용 등 비밀정보는 문서에 기록하지 않는다.
