# 콜태그 문자자동화 통합 아키텍처

기준일: 2026-07-31

상위 문서:

- [`CALLTAG_UNIFIED_PRODUCT_PLAN_KO.md`](CALLTAG_UNIFIED_PRODUCT_PLAN_KO.md)
- [`MESSAGE_AUTOMATION_SPEC_KO.md`](MESSAGE_AUTOMATION_SPEC_KO.md)

> 콜링크를 별도 앱으로 연결하지 않는다. 검증된 문자 기능을 콜태그 한 앱 안의 문자자동화 모듈로 이전한다.

---

## 1. 최종 모듈 구조

```text
CallTag App
├ Auth & Subscription
├ Contact Sync
├ Call Event Detector
├ Phone CRM
│  ├ Customer
│  ├ Stage
│  ├ Interaction
│  ├ Task
│  ├ Calendar
│  └ Statistics
├ Message Automation
│  ├ Template Library
│  ├ Variable Resolver
│  ├ Automation Rules
│  ├ Exclusion Policy
│  ├ Dedupe Engine
│  ├ Message Queue
│  ├ Scheduler
│  ├ SIM Router
│  ├ Group Manager
│  ├ Bulk Campaign
│  └ Message History
└ Unified Timeline
```

통화 감지, 고객 DB, 로그인, 구독, 제외번호를 모듈마다 중복 생성하지 않는다.

---

## 2. 이벤트 흐름

```text
CallEventDetector
→ CallEventRepository
→ PhoneCrmProcessor
→ MessageAutomationProcessor
→ TimelineRepository
```

통합 상품 사용자는 두 프로세서가 동일한 `customer_id`와 `call_event_id`를 사용한다.

### 2.1 통화 종료

```text
통화 종료
→ CallEvent 확정
→ 고객 조회
→ 자동발송 후보 생성
→ 큰 통화 정리창 표시
→ 상태·메모·일정·문자 확정
→ MessageJob 한 건 생성
→ 발송 또는 후속 예약
```

자동화 후보와 정리창 수동발송을 별도 작업으로 만들지 않는다. 같은 통화의 같은 목적이면 하나의 작업으로 병합한다.

### 2.2 부재중

```text
부재중·거절
→ CallEvent 확정
→ 제외·중복 검사
→ 부재중 자동화 규칙
→ 즉시 발송 또는 예약
```

부재중은 사용자 설정에 따라 정리창을 기다리지 않고 처리할 수 있다.

---

## 3. 공통 식별자

```text
user_id
workspace_id
device_id
customer_id
call_event_id
interaction_id
task_id
template_id
automation_rule_id
message_job_id
group_id
campaign_id
bulk_recipient_id
```

정규화된 전화번호는 고객 후보 조회와 중복 제거에 사용한다. 내부 관계는 가능하면 ID로 연결한다.

---

## 4. 데이터 모델

### 4.1 customers

```text
id
workspace_id
display_name
primary_phone
normalized_phone
relation_status
memo
pinned_call_memo
sms_exclusion_state
created_at
updated_at
```

`sms_exclusion_state`는 빠른 조회용 캐시이며 상세 제외 규칙은 별도 테이블을 사용한다.

### 4.2 call_events

```text
id
workspace_id
device_id
customer_id
normalized_phone
direction
call_type
started_at
ended_at
duration_sec
processed_at
```

### 4.3 interactions

```text
id
customer_id
call_event_id
type
result
note
occurred_at
```

### 4.4 follow_up_tasks

```text
id
customer_id
interaction_id
task_type
title
due_at
status
completed_at
created_at
```

### 4.5 message_templates

```text
id
workspace_id
title
body
category
favorite
image_refs_json
created_at
updated_at
last_used_at
usage_count
```

템플릿은 자동화 설정에 본문 문자열을 직접 저장하지 않고 `template_id`를 참조한다.

### 4.6 message_automation_rules

```text
id
workspace_id
trigger_type
name
template_id
enabled
confirm_in_post_call
send_delay_sec
business_hours_json
subscription_id
cooldown_sec
priority
created_at
updated_at
```

`trigger_type`:

```text
INCOMING_CONNECTED
OUTGOING_CONNECTED
MISSED_OR_REJECTED
FOLLOW_UP
```

### 4.7 message_exclusion_rules

```text
id
workspace_id
customer_id
normalized_phone
scope
trigger_types_json
reason
created_at
expires_at
disabled_at
```

`scope`:

```text
AUTOMATION_ONLY
ALL_MESSAGES
SELECTED_TRIGGERS
```

### 4.8 message_jobs

```text
id
workspace_id
customer_id
call_event_id
task_id
campaign_id
template_id
body_snapshot
trigger_type
source_type
source_id
scheduled_at
status
subscription_id
idempotency_key
cancellation_reason
sent_at
error_code
created_at
updated_at
```

상태:

```text
DRAFT
PENDING_CONFIRMATION
QUEUED
SCHEDULED
SENDING
SENT
FAILED
UNKNOWN
SKIPPED
CANCELLED
```

### 4.9 message_dedupe_keys

```text
idempotency_key
message_job_id
status
created_at
expires_at
```

동일 활성 키는 한 건만 허용한다.

### 4.10 customer_groups

```text
id
workspace_id
name
description
group_type
filter_json
created_at
updated_at
```

`group_type`:

```text
MANUAL
SMART
```

### 4.11 customer_group_members

```text
group_id
customer_id
created_at
```

수동 그룹에만 사용한다. 스마트 그룹은 `filter_json`으로 계산한다.

### 4.12 bulk_campaigns

```text
id
workspace_id
name
template_id
body_snapshot
scheduled_at
status
subscription_id
created_at
updated_at
```

상태:

```text
DRAFT
READY
SCHEDULED
SENDING
PAUSED
COMPLETED
PARTIAL_FAILED
CANCELLED
```

### 4.13 bulk_recipients

```text
id
campaign_id
customer_id
normalized_phone
body_snapshot
status
skip_reason
sent_at
error_code
```

상태:

```text
PENDING
EXCLUDED
DUPLICATE
INVALID
SENDING
SENT
FAILED
CANCELLED
```

---

## 5. 템플릿 저장과 변수 처리

### 5.1 저장

- 템플릿은 별도 테이블에 영구 저장
- 자동화 규칙은 템플릿 ID 참조
- 예약·캠페인 생성 시 실제 발송 본문은 `body_snapshot` 보존

### 5.2 변수 처리 계층

```text
TemplateValidator
→ VariableResolver
→ MessagePreview
→ PreSendValidator
```

지원 변수:

```text
{고객명}
{전화번호}
{통화일자}
{통화시간}
{내이름}
{상호명}
{다음일정}
```

발송 전 미치환 토큰이 남으면 작업을 `FAILED`가 아니라 `SKIPPED_VALIDATION` 성격으로 중단하고 사용자 수정이 가능하게 한다.

---

## 6. 제외 정책

모든 발송 경로는 하나의 정책 검사기를 통과한다.

```text
ManualSend
AutomaticSend
ScheduledSend
BulkSend
→ MessageExclusionPolicy.check()
```

검사 순서:

1. 전체 문자 제외
2. 자동문자 제외
3. 특정 트리거 제외
4. 만료 여부
5. 관리자 해제 여부

전체 문자 제외는 어떤 수동 강제 옵션으로도 우회하지 않는다. 먼저 제외를 해제해야 한다.

---

## 7. 중복방지 엔진

### 7.1 멱등키 생성

```text
workspace_id
+ normalized_phone
+ source_type
+ source_id
+ trigger_type
+ template_id
+ body_hash
```

`source_type`:

```text
CALL
TASK
MANUAL
CAMPAIGN
```

### 7.2 중복 검사

- 같은 통화의 같은 목적
- 같은 일정의 활성 후속문자
- 같은 캠페인의 같은 번호
- 설정 시간 안의 같은 템플릿·본문
- 자동 후보와 정리창 수동발송 충돌

### 7.3 실패

`FAILED`는 성공 중복으로 처리하지 않는다. `UNKNOWN`은 실제 발송 여부가 불명확하므로 재시도 전 사용자 확인이 필요하다.

---

## 8. 통화 종료 큰 정리창

정리창은 다음 모듈을 한 번에 호출한다.

```text
CustomerRepository
StageRepository
InteractionRepository
TaskRepository
TemplateRepository
MessageAutomationRepository
MessageQueue
```

저장 트랜잭션 순서:

1. 고객 생성·수정
2. 상태 변경
3. 통화 interaction 저장
4. 일정 생성
5. 문자 본문 치환·검증
6. 즉시 문자 작업 생성
7. 후속문자 작업 생성
8. 타임라인 이벤트 생성
9. 통화 처리 완료 표시

문자 권한 오류가 발생해도 고객·메모·일정 저장은 롤백하지 않는다. 문자 작업만 실패 상태로 남긴다.

---

## 9. 고객 상세와 일정 연결

### 고객 상세

```text
전화 / 일정 추가 / 문자
```

문자 버튼은 템플릿 선택 화면을 먼저 연다.

### 일정

`follow_up_tasks.id`를 `message_jobs.task_id`에 연결한다.

- 일정 변경: 연결 문자 시간 갱신
- 일정 완료: 완료 후 문자 실행, 남은 사전 문자 취소
- 일정 삭제: 연결 미발송 문자 취소

취소 사유는 `cancellation_reason`에 저장한다.

---

## 10. 그룹과 단체문자

### 10.1 그룹 계산

```text
ManualGroupResolver
SmartGroupResolver
→ Customer Set
```

스마트 그룹 필터 예시:

```json
{
  "match": "ALL",
  "conditions": [
    {"field": "stage", "operator": "IN", "value": ["진행 중"]},
    {"field": "last_contact_days", "operator": "GTE", "value": 7}
  ]
}
```

### 10.2 캠페인 대상 생성

```text
그룹 고객 수집
→ 추가 고객 합산
→ 정규화 번호 중복 제거
→ 전체 문자 제외 제거
→ 자동문자 제외 기본 제거
→ 번호 오류 제거
→ 최근 동일문자 중복 제거
→ BulkRecipient 생성
```

### 10.3 순차 발송

- 캠페인별 큐
- 수신자별 변수 치환
- 설정 간격 적용
- 앱 재시작 후 복구
- 남은 작업 일시정지·취소
- 실패 대상만 재시도

---

## 11. 예약·후속문자 재검사

예정 시각에 바로 발송하지 않고 다음을 다시 검사한다.

- 구독 유효
- 규칙 활성
- 문자 권한
- SIM 유효
- 전체·자동·유형별 제외
- 중복키
- 고객 상태
- 새 통화 발생
- 일정 변경·완료·삭제
- 같은 목적 수동발송

검사 결과에 따라 `SENT / SKIPPED / CANCELLED / FAILED`로 분기한다.

---

## 12. 콜링크 이전 원칙

이전 대상:

- 템플릿 저장 UX
- 템플릿 적용
- 실제 SMS/LMS/MMS 발송
- SIM·eSIM 선택
- 자동발송
- 예약발송
- 이미지 첨부
- 발송내역
- 중복방지
- 제외번호
- 단체 순차발송

복사하지 않을 구조:

- 콜링크 전용 로그인
- 콜링크 전용 고객 DB
- 콜링크 전용 통화 감지 서비스
- 콜링크 전용 하단 메뉴
- 별도 구독 상태
- 별도 고객 식별자

---

## 13. 구독 권한

| 기능 | 전화 1,900원 | 문자 990원 | 통합 2,500원 |
|---|:---:|:---:|:---:|
| 고객·통화 CRM | O | - | O |
| 일정·통계 | O | - | O |
| 저장형 템플릿 | - | O | O |
| 수신·발신·부재중 자동문자 | - | O | O |
| 후속문자 | - | O | O |
| 문자 발송 제외 | - | O | O |
| 중복방지 | - | O | O |
| 그룹·단체문자 | - | O | O |
| 통화 종료 원스톱 정리 | CRM만 | 문자만 | 전체 |
| 일정 연동 후속문자 | - | 제한 | O |
| 통합 타임라인 | 통화 중심 | 문자 중심 | 전체 |

---

## 14. 구현 단계

### A1

- 사용자 기술 설정 화면 제거
- 통화 종료 큰 정리창
- 수신·발신·부재중·후속 유형 분리

### A2

- 템플릿 CRUD
- 변수 검증·치환·미리보기
- 고객 상세 문자

### A3

- 제외 정책
- 공통 중복방지
- 자동발송과 정리창 병합

### A4

- 일정 연동 후속문자
- 통합 타임라인
- 예약 발송 재검사

### A5

- 그룹
- 단체문자
- 캠페인 큐와 결과

### A6

- 이미지 MMS
- 회선별 규칙
- 웹 동기화
- 결제 검증

---

## 15. 아키텍처 검수

- 동일 통화 이벤트를 전화·문자 모듈이 따로 생성하지 않는다.
- 템플릿은 ID로 참조하고 예약 본문은 스냅샷으로 보존한다.
- 모든 발송 경로가 동일한 제외 정책을 통과한다.
- 모든 발송 경로가 동일한 중복 엔진을 통과한다.
- 일정과 후속문자는 `task_id`로 연결된다.
- 그룹 단체문자는 수신자별 치환 결과를 저장한다.
- 같은 번호가 여러 그룹에 있어도 캠페인 수신자는 한 건이다.
- 문자 실패가 고객·메모·일정 저장을 삭제하지 않는다.
- 취소·제외·중복·오류 이유가 데이터와 화면에 남는다.
