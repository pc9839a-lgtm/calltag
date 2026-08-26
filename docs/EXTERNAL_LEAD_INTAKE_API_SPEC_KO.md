# CallTag 외부 문의 연동 / Universal Lead Intake 정본

기준일: **2026-08-26**  
상태: **코드 구현 완료 / Production D1·서버 배포 및 실제 채널 E2E는 아직 미완료**  
대상 저장소: `pc9839a-lgtm/calltag`, `pc9839a-lgtm/inlet`  
제품 문장: **어디서 문의가 들어오든 콜태그로 받고, 바로 전화하고, 끝나면 자동으로 관리한다.**

> 이 문서는 외부 문의 연동의 현재 정본이다. 과거의 “기획 확정 / 구현 전” 상태 문구와 Native Google Forms 전제는 더 이상 현재 구현을 설명하지 않는다. 실제 코드와 충돌하면 `inlet`의 현재 Universal Lead Intake 구현과 `calltag` Android 코드가 우선한다.

---

## 1. 현재 고정 지원 채널

현재 제품 범위는 아래 **5개**다.

1. **PageRo**
2. **Meta Lead Ads**
3. **Google Forms**
4. **Generic Webhook**
5. **Direct API**

Naver/Kakao placeholder는 제거한다. 현재 제품 UI에 “준비 중” 채널로 남기지 않는다.

향후 WordPress, Typeform, Tally, Webflow, Jotform, Zapier, Make, n8n, 아임웹 등은 **Generic Webhook / Direct API / Automation Bridge**로 먼저 연결하고, 필요할 때 Native Connector를 추가한다.

---

## 2. Canonical 처리 흐름

```text
PageRo / Meta Lead Ads / Google Forms / Generic Webhook / Direct API
                    ↓
             Canonical Lead Intake
                    ↓
        owner scope / 인증 / idempotency
                    ↓
            D1 customer + inquiry/event
                    ↓
          PII 없는 FCM `lead_available`
                    ↓
              Android signed pull
                    ↓
              local CRM import
                    ↓
          ACK: IMPORTED / REJECTED
```

핵심 원칙:

- **FCM은 알림 신호일 뿐 데이터 정본이 아니다.**
- FCM payload에 이름/전화번호/이메일/문의내용을 넣지 않는다.
- Android는 로그인 세션으로 서명된 API를 호출해 실제 Lead를 pull한다.
- FCM 실패는 이미 저장된 Lead를 rollback하지 않는다.
- 앱이 꺼져 있거나 Push를 놓쳐도 다음 sync에서 복구한다.

---

## 3. Tenant / owner 결정 원칙

외부 요청 body/query/provider payload의 `ownerId`를 신뢰하지 않는다.

owner는 서버가 다음 중 하나로 결정한다.

- 로그인 세션
- API Key에 저장된 owner
- Generic Webhook connection에 저장된 owner
- Meta connection에 저장된 owner
- PageRo 내부 계정 매핑

즉:

```text
외부 payload ownerId
≠ 권한 근거
```

모든 조회/저장/dedupe/ACK는 서버가 결정한 owner scope 안에서만 처리한다.

---

## 4. 고객 매칭과 문의 이벤트

기본 고객 매칭 키는 정규화된 전화번호다.

같은 번호가 다시 들어오면:

```text
기존 Customer 재사용
+ 새 Inquiry/Event 생성
+ 새 Interaction 생성
```

고객을 문의 1건마다 중복 생성하지 않는다.

중복 방지는 connection/provider 범위 안에서 처리한다.

우선 키:

1. provider event id
2. external id
3. `Idempotency-Key`
4. connection 범위 fingerprint

동일 event를 반복 전송해도 문의/고객이 중복 생성되면 안 된다.

---

## 5. Direct API

Direct API는 외부 개발자가 canonical Lead를 직접 넣는 방식이다.

대표 경로:

```text
POST /api/calltag/v1/leads
Authorization: Bearer <api_key>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

예:

```json
{
  "external_id": "lead_123",
  "source": {
    "type": "custom_api",
    "name": "자사 홈페이지 상담폼"
  },
  "customer": {
    "name": "홍길동",
    "phone": "01012345678",
    "email": "hong@example.com"
  },
  "inquiry": {
    "content": "상담 요청",
    "fields": []
  }
}
```

API Key 원칙:

- raw key 평문 저장 금지
- revoke/rotate 가능 구조
- owner는 key에서 서버가 결정
- request body의 owner 값은 무시

---

## 6. Generic Webhook

Generic Webhook은 payload 구조가 제각각인 외부 폼을 받는 범용 입력 방식이다.

대표 경로:

```text
POST /api/calltag/v1/hooks/{endpointKey}
```

구현 범위:

- connection별 전용 endpoint
- raw sample 저장
- 최근 payload 보기
- RFC6901 기반 mapping
- 전화번호 필수 검증
- mapping replay
- connection-scoped dedupe
- PII 없는 FCM
- Android pull/ACK

필드 매핑은 provider payload를 앱 DB 컬럼에 직접 맞추지 않고 canonical Lead로 변환한다.

동적 필드는 원문 label/value/order를 가능한 한 보존한다.

---

## 7. Webhook Mapper UX

현재 Mapper의 역할은 다음과 같다.

```text
테스트 payload 수신
→ 필드 목록 표시
→ 전화번호/이름/이메일 후보 확인
→ 사용자 매핑
→ 저장
→ replay/test
```

중요 안전 원칙:

- provider/raw payload의 동적 값을 `innerHTML`로 삽입하지 않는다.
- 동적 값은 `textContent` 기반으로 렌더링한다.
- Mapper는 connection id로 deterministic binding 한다.
- 진단 화면 때문에 실제 delivery 상태가 변하면 안 된다.

---

## 8. Meta Lead Ads

현재 서버 구현 범위:

- Meta connector
- OAuth
- 연결 상태/health
- Page/Form asset ownership 검증
- webhook 수신
- lead detail fetch
- canonical Lead 변환
- dedupe
- D1 저장
- PII-free FCM
- Android pull/ACK

남은 Production 범위:

- Meta production env 확인
- 필요한 App Review/권한 승인
- 실제 광고 계정/Page/Form 연결
- 실제 Lead 제출 E2E

Meta real E2E 전에는 “운영 완료”로 표시하지 않는다.

---

## 9. Google Forms — 현재 구현 방식

현재 제품 구현은 **Native Forms API + Pub/Sub가 아니다.**

현재 정본은 **Google Forms → Apps Script → CallTag Generic Webhook bridge**다.

사용자 흐름:

```text
CallTag Connect
→ Google Forms 선택
→ Google Forms Webhook 생성
→ 1회용 Webhook URL 복사
→ 제공된 Apps Script를 Form에 붙여넣기
→ <YOUR_CALLTAG_WEBHOOK_URL> 교체
→ installCallTag() 1회 실행
→ 테스트 응답 제출
→ Webhook Mapper에서 전화번호 등 매핑
→ 활성화
```

Apps Script 계약:

- installable `onFormSubmit` trigger 생성
- nested `answers` 전송
- `response.getId()`를 `Idempotency-Key`로 사용
- `source=google_forms`
- form id/title, response id, submitted_at 포함
- 실제 endpoint secret을 예제 코드/저장소에 하드코딩하지 않음

Native Google Forms API + Pub/Sub는 향후 별도 기능이다. 현재 제품이 그것을 구현했다고 문서나 UI에서 표현하지 않는다.

---

## 10. PageRo

PageRo는 기존 전용 흐름을 즉시 제거하지 않는다.

현재 Android 안전 계약:

- Universal Lead pull에서 PageRo canonical copy는 제외
- 기존 PageRo queue/SMS 자동화 경로 유지
- 고객 source는 PageRo로 보존

즉 현재는 PageRo 회귀 방지가 Universal Lead 일반화보다 우선이다.

향후 서버 구조가 완전히 통합되어도 사용자는 PageRo를 가장 간단한 공식 입력원으로 사용할 수 있어야 한다.

---

## 11. Android Universal Lead

현재 Android 구현은 `UniversalLead`, `UniversalLeadApiClient`, `UniversalLeadSyncManager`, receipt store를 사용한다.

동작:

```text
FCM `lead_available`
→ signed GET /api/calltag/v1/leads
→ eventId receipt 확인
→ normalized phone으로 고객 매칭
→ 신규 고객 생성 또는 기존 고객 재사용
→ inquiry/interaction 저장
→ receipt 기록
→ ACK
```

ACK 상태:

- `IMPORTED`
- `REJECTED`

E2E probe source `calltag_e2e_test`는 실제 pull/import/ACK 경로를 사용하지만 기존 실제 고객 metadata를 덮지 않는다.

---

## 12. Android에서 보이는 외부 문의 UI

현재 release candidate는 **v0.44.47 / versionCode 2026082602**다.

진입 경로:

```text
더보기
→ 서비스
→ 외부 문의 연동
```

중요: 이 메뉴는 숨겨진 legacy `moreMenuList`가 아니라 실제 화면을 렌더링하는 `MoreSettingsHubView`의 **서비스 섹션**에 직접 존재해야 한다.

현재 실제 구현 파일:

- `MoreSettingsHubView.java`
- `ExternalLeadIntegrationActivity.java`
- `UniversalLeadSyncManager.java`
- `CallTagMessagingService.java`

전용 화면 기능:

- 로그인/수신 상태 표시
- `지금 문의 확인` → 실제 `UniversalLeadSyncManager.requestSync(..., true)` 실행
- sync broadcast 결과 표시
- 로컬 CRM source 반영 상태 표시
- PageRo / Meta Lead Ads / Google Forms / Generic Webhook / Direct API 카드
- `웹에서 연동 설정 열기` → `https://calltag.pagero.kr/connect`
- WebView 사용 금지, 시스템 브라우저 사용

Play Internal build run `32915353235`는 v0.44.47 release AAB/debug APK 생성까지 성공했다.

단, Play Internal에 빌드가 생성된 것과 외부 Lead 서버/D1가 production 동작하는 것은 별개다.

---

## 13. FCM / 개인정보

FCM `lead_available`에는 Lead 상세를 넣지 않는다.

금지:

- 이름
- 전화번호
- 이메일
- 상담내용
- raw payload

FCM은 “새 문의가 있으니 서버에서 확인하라”는 best-effort 신호만 전달한다.

실제 개인정보는 authenticated signed pull로만 가져온다.

---

## 14. 상태 전이

대표 상태:

```text
ACCEPTED
DELIVERED
IMPORTED
REJECTED
```

중요:

- `GET /api/calltag/v1/leads`는 실제 delivery 동작의 일부라 `ACCEPTED → DELIVERED`를 만들 수 있다.
- 따라서 이 endpoint를 단순 웹 diagnostics 용도로 호출하지 않는다.
- diagnostics는 실제 delivery 상태를 mutate하지 않는 별도 경로/조회만 사용한다.

403도 caller별 의미를 구분한다. 모든 403을 전역적으로 “세션 만료”로 처리하지 않는다.

---

## 15. 서버 구현 정본

현재 서버 runtime 정본은 `pc9839a-lgtm/inlet` PR **#146**이다.

포함 스택:

```text
#117 Universal Lead Intake / Direct API / pull / ACK
#118 Generic Webhook / raw sample / mapper / replay
#119 Generic PII-free FCM
#121 Meta Lead Ads connector
#122 Meta OAuth
#125 Meta health
#126 unified /connect
#127 Webhook mapping UX
#128 read-only activity
#130 guarded real E2E harness
#133 Direct API guide
#135 Webhook guide
#136 mobile/accessibility/risk UX
#139 failure audit/E2E summary isolation
#140 Korean mapper errors
#141 asset ownership
#142 deterministic mapper binding
#143 event-driven Connect UI
#144 expired-session lifecycle
#145 auth reset secret/OAuth-state cleanup
#146 Google Forms bridge + Naver/Kakao 제거
```

서버 #146 QA는 성공 상태지만 **main merge / production deploy는 아직 완료로 간주하지 않는다.**

---

## 16. D1 migration 상태

Universal Lead 관련 migration-only 정본은 `inlet` PR **#123**의 0010~0013이다.

```text
0010_calltag_universal_lead_intake.sql
0011_calltag_generic_webhook_mapper.sql
0012_calltag_meta_lead_ads.sql
0013_calltag_meta_oauth.sql
```

현재 production D1은 과거 schema가 있으나 migration history가 없던 상태를 확인했다.

완료된 안전 작업:

- Cloudflare D1 read-only preflight 성공
- legacy 0001~0009 schema baseline audit 성공
- guarded baseline-history write 코드/QA 준비

아직 해야 할 것:

1. baseline history를 안전하게 기록
2. 기록 후 pending이 정확히 0010~0013인지 재검증
3. 별도 승인으로 backup + 0010~0013 apply
4. server rollout

**0010~0013을 baseline-history repair와 한 번에 적용하지 않는다.**

---

## 17. 현재 rollout 순서

```text
1. D1 baseline-history repair
2. read-only preflight로 pending 0010~0013 exact 확인
3. backup + 0010~0013 apply
4. server stack merge/retarget + production rollout
5. Direct API 실제 E2E
6. Generic Webhook 실제 E2E
7. Google Forms Apps Script 실제 E2E
8. Meta production 권한/App Review/실제 Lead E2E
9. Android v0.44.47 내부테스트 실기기 E2E
10. Play rollout 판단
```

순서를 건너뛰고 Android UI만 보고 “외부 문의 연동 운영 완료”로 판단하지 않는다.

---

## 18. MVP 완료 기준

MVP를 운영 완료로 부르려면 최소 다음을 실제 production 경로에서 확인한다.

- Direct API 실제 Lead → Android import → ACK
- Generic Webhook 실제 payload → mapper → Android import → ACK
- Google Forms 실제 제출 → Apps Script → Webhook → Android import → ACK
- Meta 실제 Lead → webhook/fetch → Android import → ACK
- 같은 번호 재문의 시 고객 중복 없이 새 inquiry 생성
- 동일 event 반복 시 중복 생성 없음
- Push 누락 후 foreground/startup sync 복구
- owner A 데이터가 owner B에 노출되지 않음
- FCM에 PII 없음
- PageRo 기존 흐름 회귀 없음

---

## 19. 금지사항

- FCM에 PII 넣기
- request body/query의 owner 신뢰
- raw API key/Webhook secret 평문 저장
- Push 실패로 저장된 Lead rollback
- diagnostics에서 delivery status 변경
- 동적 provider 값을 `innerHTML`로 렌더링
- 모든 403을 동일한 세션 만료로 처리
- PageRo를 universal pull로 중복 import
- Google Forms를 Native API/Pub/Sub 구현 완료라고 표기
- D1 baseline repair와 0010~0013 apply를 묶어서 실행
- 실제 E2E 전 production 완료라고 표기

---

## 20. 한 줄 정의

> **CallTag Connect는 PageRo, Meta Lead Ads, Google Forms, Generic Webhook, Direct API에서 발생한 문의를 서버에 안전하게 저장하고, PII 없는 알림과 Android signed pull을 통해 콜태그 CRM으로 가져와 전화·메모·할 일·문자 후속관리로 연결하는 Universal Lead Intake 플랫폼이다.**
