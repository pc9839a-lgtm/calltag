# CallTag 외부 입력 API / Lead Intake 플랫폼 명세

기준일: **2026-08-23**  
상태: **기획 확정 / 구현 전**  
대상 저장소: `pc9839a-lgtm/calltag`, `pc9839a-lgtm/inlet`  
관련 문서: `PRODUCT_SPEC_KO.md`, `PAGERO_CUSTOMER_INTEGRATION_KO.md`, `ROADMAP_KO.md`

---

## 1. 목적

콜태그를 `페이지로 문의를 받아 통화 후 관리하는 앱`에 한정하지 않고, **어떤 광고·입력폼·웹사이트·외부 서비스에서 발생한 잠재고객도 받아서 즉시 전화·메모·할 일·문자 후속관리로 연결하는 독립 Lead CRM 플랫폼**으로 확장한다.

핵심 제품 문장:

> **어디서 문의가 들어오든 콜태그로 받고, 바로 전화하고, 끝나면 자동으로 관리한다.**

현재 구조:

```text
페이지로 입력폼
→ 페이지로 전용 문의 큐
→ 콜태그 고객 생성/갱신
→ 전화·메모·일정·문자
```

목표 구조:

```text
Meta Lead Ads ──────┐
Google Forms ───────┤
페이지로 ────────────┤
아임웹 ──────────────┤
WordPress ──────────┤
Typeform/Tally ─────┤
Webflow/Jotform ────┤
Zapier/Make/n8n ────┤
자체 홈페이지/API ───┘
          ↓
   CallTag Lead Intake
          ↓
 인증 / 원문보존 / 필드매핑
          ↓
 정규화 / 중복검사 / 고객매칭
          ↓
 고객·문의·타임라인 생성
          ↓
 자동화 / FCM 실시간 알림
          ↓
      콜태그 Android
          ↓
 전화 → 통화 종료 메모 → 할 일 → 문자
```

페이지로는 콜태그의 필수 입력원이 아니라 **가장 간단하게 연결되는 공식 랜딩페이지 입력원**으로 위치를 변경한다.

---

## 2. 제품 방향

### 2.1 콜태그의 역할

콜태그는 다음 3개 계층을 담당한다.

1. **Lead Intake** — 외부 문의 수집
2. **Mobile Lead CRM** — 고객/문의/통화/메모/할 일 관리
3. **Lead Automation** — 신규 문의 알림, 자동문자, 후속 할 일

장기적으로는 다음 계층까지 확장할 수 있다.

4. **Outbound Integration** — 상담/계약 결과를 외부 시스템으로 전송

### 2.2 페이지로의 역할

페이지로는 독립적으로 유지하되 CallTag Lead Intake의 공식 입력원 중 하나로 통합한다.

```text
페이지로
= 콜태그와 별도 설정 없이 가장 쉽게 연결되는 공식 랜딩 제작 도구

CallTag Connect
= 외부 입력원 연결 계층

콜태그 앱
= 문의 처리·전화 영업 CRM
```

### 2.3 핵심 경쟁력

경쟁력은 `POST /leads` 엔드포인트 하나를 공개하는 데서 생기지 않는다.

핵심은 **연결 난이도를 사용자에게 숨기는 것**이다.

사용자 목표 UX:

```text
Meta
연결하기 → Facebook 로그인 → 페이지 선택 → 폼 선택 → 완료

Google Forms
연결하기 → Google 로그인 → 폼 선택 → 필드 확인 → 완료

기타 웹폼
Webhook URL 복사 → 테스트 전송 → 자동 필드매핑 확인 → 완료
```

사용자가 API, JSON, HMAC, Pub/Sub 구조를 이해하지 않아도 연결 가능해야 한다.

---

## 3. 지원 연결 방식

외부 서비스의 기능 수준에 따라 4단계 연결 방식을 제공한다.

### A. Native Connector

콜태그가 해당 서비스의 OAuth/API/Webhook을 직접 지원한다.

우선 대상:

- Meta Lead Ads
- Google Forms
- 페이지로
- 향후 국내 주요 폼/웹빌더

장점:

- 가장 쉬운 연결 UX
- provider별 서명 검증 가능
- 캠페인/폼/페이지 메타데이터 활용 가능
- 별도 개발 지식 불필요

### B. Universal Webhook

Webhook을 보낼 수 있는 모든 외부 서비스용 범용 입력 방식이다.

발급 예:

```text
POST https://<calltag-api-host>/v1/hooks/{endpointKey}
```

외부 payload 형식은 고정하지 않는다.

예:

```json
{
  "customer_name": "홍길동",
  "mobile": "01012345678",
  "product": "태아보험",
  "consult_time": "오후 3시 이후"
}
```

콜태그 연결 설정에서 한 번만 다음처럼 매핑한다.

```text
customer_name → 고객명
mobile        → 전화번호
product       → 관심상품
consult_time  → 희망상담시간
```

### C. Automation Bridge

직접 Native Connector가 없는 서비스는 Zapier / Make / n8n 등 자동화 도구를 통해 연결한다.

CallTag는 최소한 다음 액션을 외부 자동화 서비스가 쉽게 호출할 수 있게 한다.

```text
Create Lead
Update Lead
Create Task
```

초기 필수는 `Create Lead`다.

예:

```text
TikTok Lead → Make → CallTag
Typeform → Zapier → CallTag
자체 CRM → n8n → CallTag
```

### D. Redirect Bridge

Webhook/API가 없고 `입력폼 제출 후 URL 이동`만 지원하는 서비스용 보조 연결 방식이다.

예: 아임웹의 `제출 완료 후 URL 이동` 기능.

중요:

- Redirect 자체는 **문의 발생 신호**일 뿐 실제 폼 데이터 전달로 간주하지 않는다.
- 이름·전화번호 등 개인정보를 URL query string에 직접 넣는 방식을 공식 방식으로 사용하지 않는다.
- 외부 서비스가 opaque `submission_id`를 전달하고 서버 API로 해당 제출 내용을 안전하게 다시 조회할 수 있을 때만 완전한 고객 접수로 승격할 수 있다.
- 실제 데이터를 가져올 경로가 없다면 Redirect Bridge는 `제출 신호 수신`까지만 보장한다.

---

## 4. 핵심 서버 아키텍처

앱이 외부 webhook을 직접 받지 않는다. **모든 외부 입력은 서버가 먼저 수신하고 영속화한다.**

```text
External Provider
        ↓
Inbound Gateway
        ↓
Provider/Auth Verification
        ↓
Raw Event Store
        ↓
Field Mapper
        ↓
Lead Normalizer
        ↓
Idempotency / Deduplication
        ↓
Owner Resolution
        ↓
Lead / Customer / Interaction Store
        ↓
Automation Engine
        ↓
Push Queue / FCM
        ↓
CallTag Android Sync
```

### 4.1 서버 우선 원칙

- 앱이 꺼져 있어도 문의는 서버에 저장되어야 한다.
- Push 실패가 데이터 손실로 이어지면 안 된다.
- 앱 재실행 시 미수신 문의를 다시 동기화할 수 있어야 한다.
- 외부 provider 재시도 때문에 동일 문의가 중복 생성되면 안 된다.

---

## 5. 범용 데이터 모델

외부 provider의 필드명을 앱 DB 컬럼에 직접 맞추지 않는다.

### 5.1 Canonical Lead Event

권장 표준 객체:

```json
{
  "event_id": "provider-or-calltag-event-id",
  "external_id": "provider-lead-id",
  "source": {
    "type": "meta_lead_ads",
    "name": "Meta 태아보험 광고",
    "provider": "meta",
    "account_id": "...",
    "campaign_id": "...",
    "campaign_name": "태아보험 8월",
    "adset_id": "...",
    "ad_id": "...",
    "form_id": "...",
    "form_name": "상담 신청"
  },
  "customer": {
    "name": "홍길동",
    "phone": "01012345678",
    "email": "hong@example.com"
  },
  "inquiry": {
    "content": "상담 요청합니다.",
    "fields": [
      {
        "key": "product",
        "label": "관심 상품",
        "value": "태아보험",
        "order": 1
      },
      {
        "key": "preferred_time",
        "label": "희망 상담 시간",
        "value": "오후 3시 이후",
        "order": 2
      }
    ]
  },
  "submitted_at": "2026-08-23T19:00:00+09:00",
  "metadata": {}
}
```

### 5.2 동적 필드 원칙

- provider가 새 질문을 추가해도 앱 업데이트 없이 보존한다.
- 질문 label과 답변 순서를 보존한다.
- 배열/체크박스 답변은 원본 구조를 최대한 유지한다.
- 이름/전화번호/이메일 외 데이터는 `inquiry.fields`로 보존한다.
- 원문 payload와 정규화된 payload를 분리한다.

### 5.3 고객과 문의 분리

같은 고객이 여러 번 문의할 수 있으므로 `Customer`와 `Lead/Inquiry Event`를 동일 개체로 취급하지 않는다.

```text
Customer 1명
 ├─ Meta 문의 1
 ├─ 페이지로 문의 2
 ├─ Google Forms 문의 3
 ├─ 통화 이력
 ├─ 문자 이력
 └─ 할 일 이력
```

같은 전화번호가 다시 들어오면 기본 정책은:

```text
기존 Customer 유지
+ 신규 Inquiry Event 추가
+ 최근 유입 출처 갱신
+ 타임라인 이벤트 추가
```

고객을 중복 생성하지 않는다.

---

## 6. Source Attribution

현재 `customer.source` 한 칸을 계속 덮어쓰는 방식은 외부 입력원이 늘어나면 부족하다.

장기 구조:

```text
first_source_type
first_source_name
first_source_at
last_source_type
last_source_name
last_source_at
```

문의 이벤트에는 별도 source snapshot을 저장한다.

예:

```text
첫 유입: Meta / 태아보험 캠페인 A
최근 문의: Google Forms / 재상담 신청
```

광고 추적이 가능한 provider는 다음 정보까지 보존한다.

- account
- page
- campaign
- ad set
- ad
- form
- UTM
- landing URL

---

## 7. 공개 API 초안

### 7.1 Direct Lead API

개발자가 정규화된 데이터를 직접 전송하는 방식.

```text
POST /api/calltag/v1/leads
Authorization: Bearer <api_key>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

최소 필드:

```json
{
  "customer": {
    "phone": "01012345678"
  }
}
```

권장 필드:

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

응답 예:

```json
{
  "ok": true,
  "eventId": "ct_lead_...",
  "customerId": "...",
  "result": "CREATED"
}
```

가능한 result:

```text
CREATED
MATCHED_EXISTING
DUPLICATE_IGNORED
REJECTED
```

### 7.2 Generic Webhook API

```text
POST /api/calltag/v1/hooks/{endpointKey}
```

특징:

- JSON payload 구조를 강제하지 않는다.
- connection별 field mapping rule로 canonical schema로 변환한다.
- provider별 인증이 가능하면 signature 검증 adapter를 사용한다.

### 7.3 Android Lead Sync API

기존 페이지로 전용 pull/ACK 구조를 장기적으로 일반화한다.

권장:

```text
GET /api/calltag/v1/leads?after=<cursor>&limit=50
POST /api/calltag/v1/leads/ack
```

서버는 API 요청에서 사용자가 보낸 `ownerId`를 신뢰하지 않는다.

```text
CallTag 세션
→ 서버에서 ownerId 결정
→ 해당 ownerId 데이터만 반환
```

---

## 8. Webhook 필드 매퍼

외부 호환성을 결정하는 핵심 기능이다.

### 8.1 자동 인식 alias

초기 자동 탐지 후보:

전화번호:

```text
phone
mobile
mobile_phone
tel
telephone
contact
연락처
전화번호
휴대폰
휴대폰번호
```

이름:

```text
name
full_name
customer_name
applicant_name
성명
이름
고객명
신청자
```

이메일:

```text
email
email_address
이메일
메일
```

### 8.2 자동 매핑 + 사용자 확인

테스트 payload를 1건 받은 후:

```text
010-1234-5678 → 전화번호로 감지
홍길동        → 고객명으로 감지
태아보험      → 추가 문의정보
```

사용자는 틀린 항목만 변경한다.

### 8.3 JSON Path 지원

중첩 payload 지원:

```text
data.customer.name
answers[0].value
payload.contact.mobile
```

UI에서는 개발자용 JSONPath 문자열을 강제하지 말고, 받은 payload를 트리/행 형태로 보여주고 클릭 매핑할 수 있게 한다.

---

## 9. 연결 설정 UX

웹 설정에 `외부 연동` 또는 `CallTag Connect` 메뉴를 추가한다.

목록 예:

```text
Meta Lead Ads       연결하기
Google Forms        연결하기
페이지로            연결됨
아임웹              연결 방법 선택
Webhook             새 연결
Zapier / Make       연결 가이드
```

Generic Webhook 연결 생성 흐름:

```text
1. 연결 이름 입력
2. 전용 Webhook URL 발급
3. 외부 폼에서 테스트 전송
4. 최근 payload 표시
5. 고객명/전화번호/이메일 자동 탐지
6. 나머지 필드 매핑 확인
7. 테스트 고객 생성
8. 연결 활성화
```

API라는 단어를 모르는 사용자도 사용할 수 있어야 한다.

---

## 10. Meta Lead Ads Native Connector

외부 API 개방의 첫 대표 데모로 우선순위가 높다.

목표 UX:

```text
Meta 연결
→ Facebook 로그인/OAuth
→ 페이지 선택
→ Lead Form 선택
→ 필드 확인
→ 연결 활성화
```

런타임 흐름:

```text
고객이 Meta Lead Form 제출
→ Meta webhook 이벤트
→ CallTag 서버가 provider event ID 확인
→ 필요한 경우 Graph API로 Lead 상세 조회
→ canonical lead 변환
→ 중복검사
→ 고객/문의 저장
→ FCM
→ 휴대폰에 새 문의 알림
```

알림 예:

```text
새 문의 · Meta
홍길동 · 태아보험
[전화] [정보보기]
```

고객이 전화를 종료하면 기존 콜태그의 `고객명 + 메모` 작은 팝업 흐름으로 이어진다.

향후:

```text
CallTag에서 계약완료
→ provider conversion/outbound adapter
→ 광고 플랫폼에 영업 결과 전달
```

이 단계는 초기 MVP 범위에 포함하지 않는다.

---

## 11. Google Forms Native Connector

Google Forms는 일반 SaaS의 단순 `Webhook URL 입력` 방식과 다를 수 있으므로 전용 adapter를 둔다.

목표 UX:

```text
Google Forms 연결
→ Google 로그인
→ Form 선택
→ 질문 필드 확인
→ 고객명/전화번호 매핑
→ 활성화
```

가능한 구현 경로:

### 11.1 정식 API 기반

```text
Google Forms event/watch
→ server-side notification
→ 신규 응답 조회
→ canonical lead 변환
```

필요사항:

- OAuth scope 관리
- watch 갱신
- provider 지연/중복 이벤트 처리
- 응답 cursor 관리

### 11.2 보조 경로

필요한 경우:

```text
Google Forms
→ Google Sheets
→ Apps Script onFormSubmit
→ CallTag Universal Webhook
```

이 방식은 Native Connector 개발 전에도 사용할 수 있는 연결 가이드로 제공 가능하다.

---

## 12. 아임웹 연동 정책

아임웹은 특정 기능에 종속해서 CallTag 전체 API 구조를 설계하지 않는다.

### 12.1 우선 확인 순서

1. 입력폼 신규 제출 이벤트 공식 Webhook 지원 여부
2. 입력폼 응답 조회 API 지원 여부
3. 사용자 API Key/OAuth로 필요한 데이터 접근 가능 여부
4. 공식 앱/제휴가 필요한 Scope인지 여부
5. 불가한 경우 Redirect Bridge 가능 여부

### 12.2 `제출 완료 후 URL 이동` 처리

아임웹 입력폼의 완료 URL을 CallTag endpoint로 지정하는 것은 **제출 발생을 감지하는 trigger**로는 사용할 수 있다.

그러나 다음 정보가 자동으로 전달된다고 가정하면 안 된다.

- 이름
- 전화번호
- 이메일
- 질문 답변

따라서:

```text
완료 URL 방문
≠ 완전한 Lead 데이터 수신
```

### 12.3 개인정보 URL 전달 금지

다음과 같은 방식을 공식 연결 방식으로 만들지 않는다.

```text
https://.../complete?name=홍길동&phone=01012345678
```

이유:

- 브라우저 history
- access log
- analytics/referrer
- 외부 모니터링

등에 개인정보가 노출될 수 있다.

허용 가능한 방향:

```text
https://.../complete?submission=<opaque-id>
```

그리고 CallTag 서버가 해당 ID를 이용해 인증된 server-to-server API로 실제 데이터를 조회할 수 있을 때만 사용한다.

### 12.4 제휴 전략

CallTag Universal Lead Intake를 먼저 완성한 뒤 아임웹은 국내 Native Connector 후보로 추진한다.

제휴가 늦어져도 CallTag 플랫폼 전체 개발이 멈추면 안 된다.

---

## 13. 페이지로 통합 전략

기존 페이지로 전용 문의 큐를 즉시 제거하지 않는다.

1단계:

```text
기존 PageRo queue 유지
+ canonical Lead Event 변환 계층 추가
```

2단계:

```text
페이지로 / Meta / Webhook / Google Forms
→ 같은 lead_event / delivery queue 사용
```

3단계:

```text
Android의 PageroLead 전용 모델 의존 축소
→ 범용 ExternalLead 모델로 통합
```

페이지로 전용 기능:

- 같은 owner 계정 자동 연결
- 별도 Webhook URL 설정 불필요
- 필드매핑 자동

이 장점은 계속 유지한다.

---

## 14. 중복 방지 / Idempotency

외부 연동에서 가장 중요한 안정성 요구사항 중 하나다.

우선순위:

```text
1. provider event id
2. external_id
3. Idempotency-Key
4. endpoint + provider + normalized phone + submitted_at 기반 fingerprint
```

서버 DB에서 UNIQUE 제약을 사용한다.

동일 event를 provider가 10회 재전송해도 고객/문의는 1회만 생성되어야 한다.

### 기존 고객 매칭

기본 매칭 키:

```text
normalized_phone
```

기존 고객이 있으면:

```text
새 Customer 생성 X
새 Inquiry Event 생성 O
새 Interaction 생성 O
최근 source 갱신 O
```

---

## 15. 실시간 알림 / 앱 동기화

### 15.1 원칙

FCM은 데이터 저장 수단이 아니다.

```text
Lead 서버 저장 성공
→ delivery queue 생성
→ FCM notification/data message
→ 앱이 서버에서 실제 lead fetch
→ 로컬 반영
→ ACK
```

Push가 누락돼도 앱 다음 실행/주기 동기화에서 복구되어야 한다.

### 15.2 알림 행동

신규 Lead 알림 기본 액션:

```text
전화하기
정보보기
```

향후 옵션:

```text
할 일 등록
담당자 지정
```

### 15.3 목표 지표

provider가 실시간 webhook을 제공하는 경우 CallTag 내부 구간 목표:

```text
Webhook 수신 → 서버 저장: p95 2초 이내
서버 저장 → Push 요청: p95 3초 이내
전체 내부 처리: p95 5초 이내
```

provider 자체 지연과 Android OEM Push 지연은 별도로 측정한다.

---

## 16. 자동화

초기에는 Zapier 수준의 범용 workflow builder를 만들지 않는다.

MVP 규칙:

```text
신규 문의 수신
→ 앱 알림
→ 선택적으로 접수 자동문자
→ 선택적으로 후속 할 일 자동생성
```

연결별 설정 예:

```text
Meta 태아보험 폼
- 담당자 알림: ON
- 고객 접수문자: ON
- 할 일: 접수 즉시

홈페이지 렌탈 폼
- 담당자 알림: ON
- 고객 접수문자: OFF
- 할 일: 10분 내 전화
```

장기적으로 event 기반 자동화로 확장한다.

```text
lead.created
lead.updated
call.completed
task.completed
customer.stage_changed
message.sent
```

---

## 17. 보안 원칙

외부 Lead API는 이름/전화번호/상담내용을 다루므로 일반 공개 API보다 강하게 설계한다.

### 17.1 Tenant 격리

- request body의 `ownerId`를 신뢰하지 않는다.
- API key / OAuth connection / endpointKey가 서버 내부 owner에 귀속된다.
- 모든 쿼리에 owner scope를 강제한다.

### 17.2 API Key

- raw API Key를 DB에 평문 저장하지 않는다.
- prefix + hash 구조 권장.
- key별 revoke/rotate 지원.
- 마지막 사용 시각 기록.

### 17.3 Webhook 검증

provider가 서명을 지원하면 전용 verifier를 사용한다.

Generic Webhook은 선택적으로:

```text
X-CallTag-Timestamp
X-CallTag-Signature
```

HMAC 검증을 지원한다.

### 17.4 Replay 방지

- timestamp 허용 범위
- provider event id UNIQUE
- nonce/idempotency key

### 17.5 Rate Limit

connection/API key 단위 rate limit을 둔다.

초기 정확한 수치는 운영 데이터 후 확정한다.

### 17.6 로그 개인정보 최소화

운영 로그에 다음 값을 원문으로 반복 기록하지 않는다.

- 전화번호
- 이메일
- 고객명
- 상담내용

필요한 경우 마스킹 또는 event id만 기록한다.

### 17.7 Raw Payload 보존

원문 payload는 디버깅과 재매핑에 유용하지만 개인정보를 포함할 수 있다.

따라서:

- 암호화/접근제어
- 보존기간 정책
- 삭제정책
- 관리자 접근로그

를 구현 전에 확정한다.

---

## 18. 실패 / 재시도 정책

### 수신 단계

외부 provider에 가능한 빨리 성공/실패를 응답한다.

내부 처리는 durable queue를 사용해 분리하는 방향을 권장한다.

```text
Webhook 수신
→ 인증 확인
→ Raw Event 영속화
→ 2xx 응답
→ 비동기 normalize/process
```

### 처리 실패

상태 예:

```text
RECEIVED
NORMALIZING
ACCEPTED
DELIVERED
IMPORTED
RETRY
REJECTED
```

실패 이유는 운영자가 확인할 수 있어야 한다.

예:

```text
PHONE_NOT_FOUND
INVALID_SIGNATURE
FIELD_MAPPING_FAILED
DUPLICATE_EVENT
OWNER_NOT_FOUND
PROVIDER_FETCH_FAILED
```

---

## 19. 운영/진단 화면

연결별로 최소 다음 정보를 제공한다.

```text
연결 상태
마지막 정상 수신 시각
최근 20개 이벤트
성공 / 실패 건수
마지막 오류
필드 매핑 상태
Webhook URL 재발급
API Key 회전
테스트 전송
연결 중지
```

개발자/운영 진단에는 다음 pipeline 단계를 추적 가능하게 한다.

```text
receive
→ verify
→ store_raw
→ map
→ normalize
→ dedupe
→ customer_match
→ persist
→ automation
→ fcm
→ android_fetch
→ ack
```

---

## 20. 구현 단계

### Phase 0 — 기존 구조 일반화

- [ ] PageRo 전용 문의 모델을 canonical Lead Event로 변환하는 adapter 추가
- [ ] 범용 lead queue/data model 설계
- [ ] first source / last source / inquiry event 분리
- [ ] 기존 고객 중복 생성 방지 정책 공통화

### Phase 1 — Universal Lead API

- [ ] `POST /v1/leads`
- [ ] API Key 발급/회전/revoke
- [ ] Idempotency-Key
- [ ] validation
- [ ] owner scope 강제
- [ ] request/response audit

### Phase 2 — Generic Webhook + Mapper

- [ ] endpoint 생성
- [ ] raw payload 저장
- [ ] 최근 테스트 payload 보기
- [ ] 자동 필드 탐지
- [ ] 수동 field mapping
- [ ] 테스트 lead 생성
- [ ] mapping version 관리

### Phase 3 — Android 실시간 Lead Inbox

- [ ] FCM 신규 문의 push
- [ ] 앱 종료/잠금 상태 수신
- [ ] 서버 pull/ACK
- [ ] 고객 신규/기존 매칭
- [ ] 문의 상세 전체 표시
- [ ] `전화하기` 액션
- [ ] 통화 종료 메모 흐름 연결

### Phase 4 — Meta Lead Ads

- [ ] OAuth
- [ ] 페이지 선택
- [ ] 폼 선택
- [ ] webhook subscription
- [ ] lead detail fetch
- [ ] provider signature 검증
- [ ] campaign/form attribution
- [ ] 중복 event 검증
- [ ] 실제 광고 계정 E2E

### Phase 5 — Google Forms

- [ ] OAuth
- [ ] Form 목록/선택
- [ ] 응답 event/watch
- [ ] watch 자동 갱신
- [ ] 신규 응답 fetch
- [ ] 질문/답변 mapping
- [ ] Apps Script/Sheets bridge 가이드

### Phase 6 — Connector 확장

- [ ] Zapier
- [ ] Make
- [ ] n8n
- [ ] WordPress
- [ ] Typeform
- [ ] Tally
- [ ] Webflow/Jotform
- [ ] 국내 주요 웹빌더

### Phase 7 — 아임웹

- [ ] 공식 API/Webhook 가능 범위 확인
- [ ] input form 응답 접근 scope 확인
- [ ] 필요 시 제휴/앱 심사
- [ ] Native Connector 가능 여부 결정
- [ ] Redirect Bridge fallback

### Phase 8 — Outbound / Closed Loop

- [ ] outbound webhook
- [ ] `lead.created`
- [ ] `customer.stage_changed`
- [ ] `call.completed`
- [ ] `task.completed`
- [ ] 외부 CRM/ERP 연동
- [ ] 광고 conversion feedback adapter 검토

---

## 21. MVP 출시 기준

Universal Lead Intake MVP는 최소 다음 조건을 만족해야 한다.

1. 서로 다른 JSON 구조 5종 이상을 Generic Webhook으로 정상 수신한다.
2. 테스트 payload 기반 고객명/전화번호 매핑이 가능하다.
3. 필수 전화번호가 없으면 명확한 실패 상태로 보관한다.
4. 동일 event를 10회 재전송해도 문의 1건만 생성된다.
5. 같은 전화번호의 기존 고객은 중복 생성하지 않는다.
6. 기존 고객에게 새 문의 이벤트와 source가 타임라인으로 추가된다.
7. webhook 수신 후 앱 Push가 동작한다.
8. Push를 놓쳐도 앱 재진입 시 서버에서 복구된다.
9. 사용자 A의 endpoint로 들어온 Lead가 사용자 B에게 절대 노출되지 않는다.
10. URL query에 고객 이름/전화번호를 요구하지 않는다.
11. 운영 로그에서 개인정보가 기본 마스킹된다.
12. PageRo 기존 연동이 회귀하지 않는다.

---

## 22. 확장 아이디어

### 22.1 Lead Response Time

```text
문의 접수 19:00:00
첫 전화 19:02:14
최초 응답시간 2분 14초
```

콜태그의 대표 성과 지표로 만들 수 있다.

### 22.2 SLA 알림

```text
신규 문의 5분 미처리
→ 담당자 재알림
```

### 22.3 연결별 자동 담당자

```text
Meta 태아보험 → A 담당자
홈페이지 렌탈 → B 담당자
```

### 22.4 업무시간 Routing

업무시간 외 문의는 다음 영업일 첫 할 일로 자동 예약한다.

### 22.5 문의 재유입 감지

```text
기존 고객이 30일 만에 다시 문의했습니다.
최근 문의: Google Forms
이전 문의: Meta
```

### 22.6 Source별 전환율

```text
Meta A: 문의 100 / 계약 12
Google Form: 문의 40 / 계약 9
페이지로: 문의 70 / 계약 14
```

### 22.7 캠페인별 실제 계약 성과

광고 클릭/문의가 아니라 실제 전화 결과/계약 결과까지 연결한다.

### 22.8 폼별 자동문자 Preset

각 Lead Source마다 별도 접수문자를 설정한다.

### 22.9 폼별 필수 연락 시간

```text
보험 DB → 5분 내
병원 문의 → 10분 내
일반 문의 → 당일
```

### 22.10 AI Field Mapper

초기에는 alias dictionary를 사용하고, 복잡한 payload에 한해 AI 보조 매핑을 추가한다.

AI가 raw 개인정보를 불필요하게 외부 처리하지 않도록 별도 개인정보 정책을 마련한 뒤 도입한다.

### 22.11 Form Template Marketplace

```text
Meta 보험폼
아임웹 상담폼
WordPress Contact Form 7
Elementor Form
Tally
```

등 자주 쓰는 payload mapping preset을 공유한다.

### 22.12 Partner API

광고대행사/DB 공급사가 자신의 고객 계정으로 Lead를 전달할 수 있는 partner scope를 장기 검토한다.

### 22.13 Lead Quality Flag

중복 번호, 잘못된 번호, 짧은 시간 대량 접수 등을 품질 플래그로 표시한다.

### 22.14 Outbound Webhook

콜태그에서 발생한 영업 결과를 외부 시스템이 실시간으로 받을 수 있게 한다.

### 22.15 광고 Closed-loop Optimization

```text
광고
→ Lead
→ CallTag
→ 통화
→ 계약완료
→ 광고 플랫폼에 결과 반환
```

장기적으로 CallTag를 단순 CRM이 아니라 **Lead-to-Sale 데이터 허브**로 확장할 수 있다.

---

## 23. 과금 방향 — 미확정

현재 Android 전화/문자 상품과 별개로 API 기능은 장기적으로 다음 기준을 조합할 수 있다.

- 월 수신 Lead 수
- 활성 Connector 수
- Native Connector 사용 여부
- 자동화 실행 수
- 팀원 수
- API 호출량

**현재 이 문서에서 가격은 확정하지 않는다.**

제품 검증 전 복잡한 엔터프라이즈 요금제를 먼저 만들지 않는다.

---

## 24. 반드시 확인해야 할 외부 의존성

구현 전에 provider별 최신 공식 문서를 확인한다.

### Meta

- Lead Ads webhook 권한
- Graph API 버전
- App Review 필요 범위
- Page/Form subscription
- Lead Retrieval 권한

### Google

- Forms API OAuth scope
- response notification/watch 지원 범위
- Pub/Sub 설정
- watch 만료/갱신 정책

### 아임웹

- 입력폼 submit event 공개 여부
- 입력폼 response API 공개 여부
- OAuth/API Key scope
- 앱스토어/제휴 필요 범위
- 완료 URL에서 안전한 submission identifier 전달 가능 여부

외부 플랫폼 정책은 변할 수 있으므로 구현 시점에 공식 문서를 다시 검증한다.

---

## 25. 결정 사항 요약

확정:

- CallTag 외부 입력 API를 개방한다.
- 특정 폼 서비스 하나에 종속되지 않는 canonical Lead Intake 구조로 만든다.
- PageRo도 장기적으로 같은 범용 intake pipeline을 사용한다.
- Generic Webhook + Field Mapper를 핵심 기반 기능으로 둔다.
- Meta Lead Ads는 우선 Native Connector 후보로 둔다.
- Google Forms는 Native Connector + Apps Script bridge 양쪽을 고려한다.
- 아임웹 완료 URL은 Lead 데이터 자체가 아니라 trigger로 취급한다.
- URL query에 이름/전화번호를 직접 싣는 방식을 공식 지원하지 않는다.
- FCM은 알림 수단이며 서버 저장/queue가 정본이다.
- 같은 번호 재문의는 고객 중복 생성이 아니라 신규 Inquiry Event로 기록한다.
- provider event id / external id / idempotency key 기반 중복 방지는 필수다.
- 복잡한 CRM 기능보다 `문의 → 즉시 연락 → 통화 후 기록 → 후속관리` 핵심 루프를 우선한다.

미확정:

- 최종 API host/domain
- Raw payload 보존기간
- API 과금 기준
- 아임웹 공식 제휴 여부
- 팀 단위 Lead routing 범위
- outbound conversion API의 1차 지원 provider

---

## 26. 한 줄 제품 정의

> **CallTag Connect는 Meta, Google Forms, 페이지로, 웹사이트, 자동화 도구 등 어디서 발생한 문의든 하나의 Lead 형식으로 받아 콜태그의 전화·메모·할 일·문자 CRM으로 즉시 연결하는 외부 입력 플랫폼이다.**
