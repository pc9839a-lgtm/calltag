# CallTag 외부 문의 연동 / Universal Lead Intake 정본

기준일: **2026-08-26**  
Android 기준 버전: **0.44.48 / versionCode 2026082603**  
상태: **앱 네이티브 연동 관리 구현 및 signed AAB 빌드 성공 / Production D1·서버 rollout 및 실제 채널 E2E는 아직 미완료**  
대상 저장소: `pc9839a-lgtm/calltag`, `pc9839a-lgtm/inlet`

> 제품 문장: **어디서 문의가 들어오든 콜태그로 받고, 바로 전화하고, 끝나면 자동으로 관리한다.**

이 문서는 외부 문의 연동의 현재 정본이다. 앱 코드와 서버 코드가 이 문서보다 우선한다.

---

## 1. 고정 지원 채널

현재 제품 범위는 아래 5개다.

1. PageRo
2. Meta Lead Ads
3. Google Forms
4. Generic Webhook
5. Direct API

Naver/Kakao placeholder는 제거한다.

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
          PII 없는 FCM lead_available
                    ↓
              Android signed pull
                    ↓
              local CRM import
                    ↓
          ACK: IMPORTED / REJECTED
```

원칙:

- FCM은 알림 신호일 뿐 데이터 정본이 아니다.
- FCM에 이름/전화번호/이메일/문의내용을 넣지 않는다.
- Android가 로그인 세션으로 실제 Lead를 pull한다.
- FCM 실패가 저장된 Lead를 rollback하면 안 된다.
- 같은 정규화 전화번호면 기존 고객을 재사용하고 문의/event는 새로 만든다.
- owner는 body/query가 아니라 서버 인증/session/API key/stored connection에서 결정한다.

---

## 3. Android v0.44.48 — 앱에서 직접 연동 관리

진입:

```text
더보기
→ 서비스
→ 외부 문의 연동
```

실제 메뉴는 숨겨진 legacy `moreMenuList`가 아니라 `MoreSettingsHubView`의 **서비스** 섹션에 존재한다.

v0.44.47의 문제였던 아래 구조는 제거했다.

```text
웹에서 연동 설정 열기
→ https://calltag.pagero.kr/connect
→ production 미배포 시 홈 fallback
```

**v0.44.48부터 외부 문의 설정 때문에 `/connect`로 보내지 않는다.**

앱 화면에서 직접 가능한 작업:

### PageRo

- 기존 `PageroConnectionCompactActivity`로 앱 내 연결 관리
- 기존 PageRo 전용 queue/SMS 경로 유지

### Meta Lead Ads

```text
앱에서 Meta 연결
→ signed POST /api/calltag/v1/meta/oauth/start
→ Facebook OAuth 화면만 시스템 브라우저로 열기
→ 서버 callback
→ /api/calltag/v1/meta/oauth/android-return
→ calltag://external-lead/meta
→ 앱 복귀
→ signed GET /meta/oauth/session
→ 관리 페이지 선택
→ signed POST /meta/oauth/complete
```

- WebView 사용 금지
- `https://*.facebook.com` OAuth URL만 허용
- provider access token을 deep link에 넣지 않음
- deep link에는 OAuth session id/status만 전달
- Android callback intent filter는 `calltag://external-lead/meta`로 제한

### Google Forms

현재 구현은 Native Forms API + Pub/Sub가 아니라 **Apps Script → Generic Webhook bridge**다.

앱 흐름:

```text
Google Forms 연결
→ 앱이 Webhook connection 생성
→ 1회용 endpoint URL 수신
→ URL이 이미 들어간 Apps Script 생성
→ 앱에서 Apps Script 복사
→ Google Form Apps Script에 붙여넣기
→ installCallTag() 1회 실행
→ 테스트 응답 제출
→ 앱에서 테스트·매핑 확인
→ 서버 mapper 추천 전화번호 후보 확인
→ 사용자 승인 후 mapping 저장
→ 이후 제출 수집
```

중요:

- 테스트 sample을 자동 replay하지 않는다.
- 추천 매핑 저장만으로 테스트 고객을 자동 생성하지 않는다.
- `sampleCount > 0` + `mappingReady=true`일 때만 `수집 준비`로 본다.

### Generic Webhook

앱에서:

- Webhook 생성
- 1회용 endpoint URL 표시/복사
- sample 상태 확인
- URL 재발급
- 연결 해제

URL 재발급 시 기존 endpoint는 즉시 폐기된다는 확인 절차를 거친다.

### Direct API

앱에서:

- API Key 발급
- 활성 키 상태 확인
- API Key rotate
- API Key revoke

rotate 시 기존 키가 즉시 폐기되므로 사용자 확인 후 실행한다.

---

## 4. Android one-time secret 정책

Webhook endpoint URL과 Direct API raw key는 민감값이다.

v0.44.48 정책:

- 서버가 발급 직후 한 번만 반환
- 앱은 `transientSecret` Activity 메모리에만 둠
- SharedPreferences/local DB에 저장하지 않음
- dialog dismiss 시 메모리 문자열 비움
- Activity destroy 시 다시 비움
- 사용자가 명시적으로 `복사`를 누른 경우에만 Android Clipboard 사용

---

## 5. Android signed integration API client

핵심 파일:

- `ExternalLeadIntegrationActivity.java`
- `ExternalLeadIntegrationApiClient.java`
- `MoreSettingsHubView.java`
- `UniversalLeadApiClient.java`
- `UniversalLeadSyncManager.java`

앱의 external integration API 요청은 기존 CallTag 세션을 사용한다.

대표 header:

```text
X-Inlet-Session: <encrypted local session에서 읽은 현재 session>
X-Pagero-Product: calltag
X-CallLink-Client: android
```

앱이 ownerId를 요청 body에 넣어 권한 근거로 사용하지 않는다.

현재 API client가 사용하는 대표 route:

```text
GET/POST/PATCH /api/calltag/v1/connections
GET /api/calltag/v1/connections/{id}/samples
GET/POST /api/calltag/v1/keys
GET /api/calltag/v1/meta/connections
POST /api/calltag/v1/meta/oauth/start
GET /api/calltag/v1/meta/oauth/session
POST /api/calltag/v1/meta/oauth/complete
```

현재 production 서버에 route/schema가 아직 없으면 앱은 홈으로 이동하지 않고:

```text
외부 연동 서버 기능이 아직 현재 서버에 준비되지 않았습니다.
```

상태로 현재 화면에 남는다.

---

## 6. Meta Android return 서버 작업

서버 PR: **inlet #158**  
브랜치: `feat/calltag-android-connect-return-20260826`

추가 route:

```text
GET /api/calltag/v1/meta/oauth/android-return
```

역할:

- 기존 Meta OAuth callback이 성공/실패 결과를 이 route로 전달
- `meta`, `metaOAuth`, `reason`만 allowlist
- `calltag://external-lead/meta?...`로 302
- `Cache-Control: no-store`
- `Referrer-Policy: no-referrer`
- provider token/credential 전달 금지
- caller-controlled external redirect URL 금지

D1 migration 추가는 없다.

---

## 7. Generic Webhook 계약

대표 생성/관리 route:

```text
GET   /api/calltag/v1/connections
POST  /api/calltag/v1/connections
PATCH /api/calltag/v1/connections
```

PATCH action:

```text
update_mapping
rotate_endpoint
revoke
set_retention
replay_raw
```

Android v0.44.48은 `replay_raw`를 Google Forms 추천 매핑 저장 과정에서 자동 실행하지 않는다.

Webhook sample:

```text
GET /api/calltag/v1/connections/{id}/samples?limit=5
```

sample의 mapper `draftMapping`을 앱에서 추천 매핑에 사용할 수 있다.

---

## 8. Direct API 계약

Key route:

```text
GET  /api/calltag/v1/keys
POST /api/calltag/v1/keys
```

POST action:

```text
create
rotate
revoke
```

실제 Lead 입력:

```text
POST /api/calltag/v1/leads
Authorization: Bearer <ctk_...>
Idempotency-Key: <unique-key>
```

raw API Key는 DB에 평문 저장하지 않는다.

---

## 9. Android Universal Lead 수신

외부 문의 설정과 실제 문의 수신은 분리된 기능이다.

```text
FCM lead_available
→ signed GET /api/calltag/v1/leads
→ eventId receipt 검사
→ 전화번호 고객 매칭
→ local CRM import
→ receipt 기록
→ ACK
```

- PageRo canonical copy는 Universal pull에서 제외하고 기존 PageRo 전용 경로를 유지한다.
- E2E source `calltag_e2e_test`는 실제 pull/import/ACK를 타지만 기존 실제 고객 metadata를 덮지 않는다.

---

## 10. v0.44.48 release build

Android PR: **calltag #102**  
브랜치: `feat/calltag-external-lead-ui-20260826`  
HEAD: `1878c522b4fde429da371da351845a4aa7edd829`

버전:

```text
versionName 0.44.48
versionCode 2026082603
```

GitHub Actions:

```text
Build CallTag Play Internal
run 32919964679
결과 SUCCESS
```

성공 확인:

- Universal Lead Android contract
- Play release contract
- API 36
- 기존 Play upload key 검증
- Debug APK build
- signed release AAB build
- release file verify/stage
- AAB artifact upload

AAB:

```text
CallTag-v0.44.48-code2026082603.aab
SHA-256: 15f116725c59e02db05fef8d46fc7c0a6ce3e27b9851dea0ce488e1c3b1ff54b
```

---

## 11. Production 상태 — 착각 금지

**앱 코드가 완성되고 signed AAB가 빌드된 것과 production 외부 연동이 실제 동작하는 것은 별개다.**

아직 production에서 완료되지 않은 항목:

- D1 migration 0010~0013 적용
- server runtime stack production rollout
- Meta production env/App Review/실제 Lead E2E
- Google Forms 실제 제출 → production → Android 실수신 E2E
- Generic Webhook production E2E
- Direct API production E2E

따라서 v0.44.48을 Play 내부테스트에 올려도 server production이 준비되기 전에는 일부 연결 버튼이 `서버 기능 준비 필요`로 끝날 수 있다.

중요한 차이는 **이제 사용자를 미구축 웹 `/connect` 또는 홈으로 보내지 않는다는 것**이다.

---

## 12. D1 migration 상태

migration-only 정본은 inlet PR #123의 0010~0013이다.

```text
0010_calltag_universal_lead_intake.sql
0011_calltag_generic_webhook_mapper.sql
0012_calltag_meta_lead_ads.sql
0013_calltag_meta_oauth.sql
```

현재 production D1은 legacy schema baseline audit까지는 끝났으나 위 migration을 production에 적용하지 않았다.

이번 Android v0.44.48 개발 작업은 D1 write/apply를 수행하지 않는다.

---

## 13. 현재 개발선

Server 기능 stack:

```text
#117 Universal Lead Intake / Direct API / pull / ACK
#118 Generic Webhook / sample / mapper
#119 PII-free FCM
#121 Meta Lead Ads
#122 Meta OAuth
#125 Meta health
#126 Connect hub
#127 Mapper UX
#133 Direct API guide
#135 Webhook guide
#146 Google Forms bridge
#151 Google Forms ready script
#153 Google Forms status
#154 recommended mapping
#155 manual sample refresh
#156 rotate/revoke
#157 channel summary separation
#158 Android Meta OAuth return
```

Android:

```text
#100 Universal Lead pull/import/ACK
#101 E2E real-customer isolation
#102 visible + native external integration management
```

---

## 14. 다음 실제 검증 순서

개발 우선 기준:

```text
1. Android v0.44.48 Play 내부테스트 설치/화면 QA
2. 앱에서 각 채널 버튼/실패 UX 확인
3. server/D1 production 준비 후 API 활성화
4. Google Forms 테스트 제출 1건 → 앱 고객 생성 확인
5. Generic Webhook 테스트 → 앱 고객 생성 확인
6. Direct API test lead → 앱 고객 생성 확인
7. Meta OAuth + Page 연결 + 실제 Lead E2E
```

서버 production이 준비되기 전에 `/connect`로 우회해서 동작한 것처럼 보이게 만들지 않는다.
