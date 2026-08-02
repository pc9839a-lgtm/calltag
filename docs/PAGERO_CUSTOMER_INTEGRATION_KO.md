# 페이지로 고객정보 → 콜태그 연동

기준일: 2026-08-02

## 1. 목표

페이지로 랜딩페이지에서 문의가 접수되면 고객정보를 서버에 안전하게 보관하고, 해당 계정으로 로그인한 콜태그 앱만 문의를 가져가 로컬 고객 DB에 반영한다.

```text
페이지로 문의 폼
→ 서버 간 서명된 Webhook
→ CallTag D1 대기열
→ 로그인 세션 검증
→ CallTag 앱 동기화
→ 로컬 customers 저장
→ 서버 IMPORTED ACK
```

운영 웹사이트의 일반 페이지 요청과 연동 API는 분리한다. DB 또는 보안키가 설정되지 않아도 일반 웹사이트는 계속 열려야 한다.

## 2. API

### 2.1 문의 수신

`POST /api/integrations/pagero/leads`

필수 헤더:

```text
Content-Type: application/json
X-Pagero-Timestamp: Unix timestamp seconds
X-Pagero-Signature: sha256=<hex HMAC>
```

서명 원문:

```text
{timestamp}.{raw JSON body}
```

서명 방식:

```text
HMAC-SHA256(PAGERO_WEBHOOK_SECRET, signature_source)
```

허용 시간 오차는 5분이다. 같은 `event_id`는 한 건만 저장된다.

요청 예시:

```json
{
  "event_id": "pg_evt_20260802_000001",
  "workspace_key": "owner@example.com",
  "site_id": "pg_site_insurance_01",
  "submitted_at": "2026-08-02T03:20:00Z",
  "customer": {
    "name": "김민수",
    "phone": "010-1234-5678",
    "email": ""
  },
  "inquiry": {
    "content": "보험 상담을 받고 싶어요",
    "source_url": "https://pagero.kr/p/insurance-01",
    "campaign": "naver-search"
  },
  "metadata": {
    "utm_source": "naver",
    "landing_version": "1"
  }
}
```

`workspace_key`는 인증 서버가 반환하는 안정적인 workspace ID를 우선 사용한다. 현재 workspace ID가 없다면 계정 이메일 소문자 값을 임시 키로 사용한다.

### 2.2 앱 문의 조회

`GET /api/call/pagero/leads?after=0&limit=50`

필수 헤더:

```text
X-Inlet-Session: <CallTag login session>
```

서버는 기존 `/api/call/session`으로 세션을 검증한 후 해당 계정의 `PENDING`, `DELIVERED` 문의만 반환한다. 조회했다고 삭제하지 않는다. 앱이 중간에 종료되어도 다시 받을 수 있다.

### 2.3 앱 처리 완료

`POST /api/call/pagero/leads/ack`

```json
{
  "leadIds": [1, 2],
  "status": "IMPORTED",
  "result": "고객 생성 1건, 기존 고객 갱신 1건"
}
```

상태:

- `IMPORTED`: 앱 로컬 DB 반영 성공
- `REJECTED`: 잘못된 번호 등 앱에서 반영할 수 없음

## 3. Cloudflare 설정

필수 바인딩 및 시크릿:

```text
D1 binding name: DB
Secret: PAGERO_WEBHOOK_SECRET
Optional variable: CALLTAG_AUTH_BASE_URL
```

DB 마이그레이션:

```text
migrations/0001_pagero_lead_integration.sql
```

설정 전 API는 명확한 `503` JSON을 반환하지만 일반 사이트 요청은 영향을 받지 않는다.

## 4. 데이터 상태

```text
PENDING   문의 저장 완료
DELIVERED 앱에 전달됨, 아직 ACK 없음
IMPORTED  앱 고객 DB 반영 완료
REJECTED  앱에서 반영 거절
```

## 5. 앱 반영 규칙

다음 Android 패치에서 아래 규칙으로 로컬 DB에 반영한다.

1. `normalized_phone`으로 기존 고객 조회
2. 없으면 `relation_status=신규`, `source=페이지로`로 고객 생성
3. 있으면 고객을 중복 생성하지 않고 문의 메모와 최근 접수 시각 갱신
4. `event_id`를 로컬 수신 이력에 저장해 앱 재시작·재조회 중복 차단
5. 고객 저장 성공 후에만 서버에 `IMPORTED` ACK
6. 네트워크 실패는 고객 저장을 롤백하지 않고 ACK만 재시도

## 6. 보안·데이터 원칙

- 브라우저에 `PAGERO_WEBHOOK_SECRET`을 넣지 않는다. 페이지로 서버가 Webhook을 호출한다.
- 서명 없는 요청, 5분을 초과한 요청, 잘못된 전화번호는 저장하지 않는다.
- 앱 조회는 콜태그 로그인 세션 검증 후 처리한다.
- 계정 간 고객정보가 섞이지 않도록 모든 조회·ACK에 `workspace_key` 조건을 건다.
- `event_id` UNIQUE 제약으로 재전송 중복을 차단한다.
- 원본 메타데이터는 8KB까지만 저장한다.
