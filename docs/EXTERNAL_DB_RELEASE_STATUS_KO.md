# CallTag 외부 입력폼·문의 연동 출시 상태

기준: **0.44.58 / versionCode 2026091102**  
기준일: **2026-09-11**

## 지원 경로

1. PageRo
2. Meta Lead Ads
3. Google Forms
4. Generic Webhook
5. Direct API

## Android 완료 범위

- `더보기 → 서비스 → 외부 문의 연동`
  - PageRo 연결 관리
  - Meta OAuth → 실제 Lead Form 목록 조회 → 받을 리드폼 선택 → 연결
  - Google OAuth → 실제 Google Form 목록 조회 → Form 선택 → 연결
  - Webhook 생성/상태확인/URL 교체/해제
  - 새 문의 수동 확인 및 Universal Lead pull/ACK
- `더보기 → 서비스 → Webhook 필드 매핑`
  - 실제 수신 샘플 분석
  - 전화번호 필드 필수 선택
  - 이름/이메일/문의내용 선택 또는 건너뛰기
  - 서버 mapping 저장 후 이후 문의 Canonical Lead 변환
- Google Forms/Universal Lead 자동수신 안전망
  - 앱 foreground에서는 기존 Universal Lead sync가 Google Forms를 먼저 동기화
  - 앱 background에서는 WorkManager가 15분 주기로 Universal Lead sync 실행
  - Google Forms provider 장애가 Meta/Webhook/Direct API 문의 수신을 막지 않음
- `더보기 → 서비스 → Direct API`
  - API Key 목록
  - 신규 Key 발급
  - Key rotate
  - Key revoke
  - raw API Key는 발급/rotate 직후 1회만 표시
  - raw API Key를 SharedPreferences/SQLite에 저장하지 않음
  - 사용자가 명시적으로 누른 경우에만 Clipboard 복사

Direct API 입력 계약:

```text
POST https://pagero.kr/api/calltag/v1/leads
Authorization: Bearer <ctk_...>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

서버는 `Idempotency-Key` 또는 stable `event_id/external_id` 중 하나를 요구한다.

## 서버 완료 범위

`pc9839a-lgtm/inlet` main 기준 아래 route가 구현되어 있다.

- `GET/POST/PATCH /api/calltag/v1/connections`
- `GET /api/calltag/v1/connections/{id}/samples`
- `GET/POST /api/calltag/v1/keys`
- `GET/POST /api/calltag/v1/leads`
- Meta OAuth/connection/webhook routes
- Meta Lead Form 선택 필터
- Google Forms OAuth/forms/connect/connections/sync routes
- Generic Webhook mapper + mapping version 저장
- Universal Lead Android pull + ACK

Meta는 선택된 Lead Form 이벤트만 허용한다. Google Forms는 refresh token을 암호화 저장하고 Responses API를 통해 증분 동기화한다. Generic Webhook은 샘플 payload의 JSON Pointer 후보를 분석해 앱에서 필드 매핑을 확정한다.

Direct API POST는 API Key로 owner를 결정하며 body의 owner id를 권한 근거로 사용하지 않는다. 중복 요청은 idempotency key/external id 기준으로 처리한다. 저장 성공 후 FCM `lead_available`는 best-effort pull trigger로만 사용하며 FCM 실패가 저장된 Lead를 rollback하지 않는다.

## CRM 반영

- 외부 문의 전화번호 신규: 고객 생성
- 기존 번호: 기존 고객 재사용 + 새 상담/event 기록
- 유입 채널 원본 source 보존
- 고객 목록 `외부 문의` 필터
- 고객 카드 source badge
- 고객 상세 `유입 채널` 표시
- Android receipt + server ACK로 재수신 중복 방지

## 현재 남은 실환경 검증

코드/계약이 아니라 실제 공급자 계정 또는 외부 시스템이 필요한 검증만 남는다.

- 실제 Meta Lead Ads 테스트 리드 1건 → Android CRM 도착
- 실제 Google Form 응답 1건 → 백그라운드/foreground 동기화 후 Android CRM 도착
- 운영 Webhook 샘플 POST 1건 → 필드 매핑 저장 → 두 번째 문의 CRM 도착
- 운영 Direct API Key 발급 → 외부 POST 1건 → Android CRM 도착
- PageRo 실제 문의 1건 → 기존 PageRo 경로 중복 없이 도착

실제 공급자 E2E가 완료되기 전에는 `실연동 검증 완료`라고 표기하지 않는다.
