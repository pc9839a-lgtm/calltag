# CallTag 0.44.61 통합 출시 점검 — 실제 패치 결과 (2026-10-10)

> 이 문서는 **코드 병합/실기기 운영 검증이 끝났다는 확인서가 아니다.** 테스트 결과와 미검증 항목을 명시적으로 분리한다.

## 릴리스 범위 및 정본

- Android 후보 브랜치: `patch/calltag-next-production-20261006` / PR #119 (Draft/Open)
- Inlet 서버 수정 브랜치: `fix/calltag-04461-release-gates-20261010` / PR #393 (Draft/Open)
- `calltag main`, `inlet main`으로 이 패치를 병합하거나 Production을 배포하지 않았다.
- 제품명/앱 버전: CallTag 0.44.61, versionCode 2026100701.
- Google Forms는 앱 sync + 15분 WorkManager 안전망이며 즉시 push라고 주장하지 않는다.

## 앱 수정 및 증거

| 영역 | 수정 내용 | 검증 상태 |
|---|---|---|
| 초기 권한 | 기본 전화 앱 역할 변경이 아니며 고객관리 목적임을 설명 | 소스 반영 + Android 빌드 PASS / 실기기 미검증 |
| 발신자 역할 | `ROLE_CALL_SCREENING` 전에 별도 안내, 건너뛰기 추가. `ROLE_DIALER` 요청 아님 | 소스 반영 + Android 빌드 PASS / OEM UI 미검증 |
| 무료체험 만료 | `TRIAL_EXPIRED`인 경우 매 앱 콜드 스타트에 만료 안내 표시 | 소스 반영 + Android 빌드 PASS / 실제 만료 계정 미검증 |
| 서버 만료 공지 | 서버 `notice.code=TRIAL_EXPIRED` 우선 처리. `status=inactive`도 인식 | 소스 반영 + Android 빌드 PASS |
| 안내 순서 | 이용권 만료 공지 우선 표시, 전화 권한 설정에 가로막히지 않음 | 소스 반영 + Android 빌드 PASS |
| 단일 API origin | ExternalLead/PageroLead/CallTagSync/Auth/CallTagPush의 `BASE_URLS`를 `https://pagero.kr` 단일화 | 소스 반영 + 회귀 검사 + Android 빌드 PASS / 네트워크 실기기 미검증 |
| 문서정책 | 추천 가입자 7일+5일=12일(서버 기본값), 추천인 수수료 기본 20% | README 수정 |

Android CI: [CallTag 0.44.61 release build](https://github.com/pc9839a-lgtm/calltag/actions/runs/38029494577) — success. Debug APK 및 signed optimized release AAB 빌드 완료. 이것이 실기기 통화권한·결제 E2E 성공을 뜻하지는 않는다.

## 서버 패치 및 증거

| 영역 | 수정 내용 | 검증 상태 |
|---|---|---|
| 추천수익 수수료율 | 관리자 설정 `commissionRateBps=0`이 `||` 때문에 2000으로 재설정되는 오류 수정 | 회귀 QA 추가, Billing Referral QA 확인 필요 |
| 기본 정책 | 신규 7일, 추천 회원 추가 5일, 수수료 기본 20% | 소스 확인. 운영 D1 관리자 설정값 검증은 별도 |
| Meta OAuth | Production smoke가 `meta.oauthReady!==true`면 실패 | 브랜치 코드 반영. Production 비밀키 복구 미실시 |
| Firebase | 외부연동 smoke가 Firebase config + D1 push readiness를 검사 | 브랜치 코드 반영. 실제 Android FCM 수신 미검증 |
| Webhook | CRUD 외에 매핑된 QA lead POST 및 중복 무시 검사 추가 | 브랜치 코드 반영. Production smoke 실실행 미검증 |
| Direct API | QA API 키로 실제 lead POST 및 idempotency 검사 추가 | 브랜치 코드 반영. Production smoke 실실행 미검증 |
| Google Forms | 일반 Webhook 테스트를 Google Forms E2E처럼 표시하던 검사명 정정 | 브랜치 코드 반영. 실제 Google Forms 제출 E2E 미검증 |

서버 PR: https://github.com/pc9839a-lgtm/inlet/pull/393

**QA fixture 주의:** Webhook/Direct API의 합성 문의는 비마스터 QA 계정에만 들어가며 감사용 기록으로 남는다. 실사용자 계정에 테스트를 수행하지 않는다. QA 서버 pull+ACK가 실제 Android 로컬 CRM 저장 성공 증거는 아니다.

## 출시 차단 항목 (모두 충족해야 GO)

1. Cloudflare Production Meta: `CALLTAG_META_APP_ID`, `CALLTAG_META_APP_SECRET`, `CALLTAG_META_OAUTH_REDIRECT_URI` 복구 후 `meta.oauthReady=true` 증명.
2. Firebase Push: Project ID/Client Email/Private Key readiness 외에 테스트 등록 기기로 실제 FCM 전송 및 Android sync/ACK 확인.
3. 실제 만료 계정: 마지막 무료일/종료 직후/앱 종료-재실행/재로그인/결제 성공/결제 실패/구매 복원 시나리오 실기기 통과.
4. 추천 가입: 새 사용자 코드 +5일, 중복/자기추천 차단, 서버 운영 설정, 실제 Play 첫 결제·갱신·환불·정산까지 데이터 비교.
5. Direct API / Webhook 신규 QA smoke의 배포 후 PASS 확인. PageRo 실제 lead → local CRM → receipt → ACK 시나리오 별도 E2E 통과.
6. Google Forms 15분 수집 지연 안내 확인. 실제 Forms 제출과 서버 sync E2E 통과.
7. Android 10~16 주요 기기에서 기본 전화 앱 미변경, 권한 거부·건너뛰기·팝업 동작 확인.
8. `inlet` / `calltag` PR Draft 해제는 모든 필수 회귀 PASS 이후에만. 승인 없이 main 병합 및 Production deploy 금지.

## 진단 범례

- **PATCHED**: GitHub 후보 브랜치에 반영.
- **BUILD PASS**: 빌드 및 코드 정적 검사 통과.
- **CI PASS**: 지정 GitHub Actions 실행 success.
- **PRODUCTION PASS**: 실제 Production에서 안전한 합성 요청 E2E 성공을 기록한 경우에만.
- **DEVICE E2E PASS**: 실제 기기에서 고객 로컬 저장 후 ACK까지 확인한 경우에만.

현 출시 결론: **NO-GO**. CI 빌드가 성공하더라도 Meta/FCM 설정, 정산, 실기기 E2E가 미검증이므로 운영 승인 불가.
