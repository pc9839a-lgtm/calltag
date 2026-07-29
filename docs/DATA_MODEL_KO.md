# 콜태그 데이터 모델

## 1. Customer

고객 개인 또는 업체의 장기 관계 정보를 저장한다.

| 필드 | 설명 |
|---|---|
| id | 내부 고객 ID |
| displayName | 고객명 또는 업체명 |
| primaryPhone | 표시 전화번호 |
| normalizedPhone | 비교용 정규화 번호 |
| relationStatus | NEW, CONSULTING, EXISTING, VIP, DORMANT, OPT_OUT, EXCLUDED |
| source | 네이버, 소개, 홈페이지, 기존 고객 등 |
| memo | 고객 공통 메모 |
| firstContactAt | 최초 상담일 |
| lastContactAt | 최근 상담일 |
| firstTransactionAt | 최초 거래 완료일 |
| createdAt | 생성일 |
| updatedAt | 수정일 |

## 2. Opportunity

고객의 개별 문의·견적·계약 진행건을 저장한다. 한 고객은 여러 상담건을 가질 수 있다.

| 필드 | 설명 |
|---|---|
| id | 상담건 ID |
| customerId | 고객 ID |
| title | 문의명 |
| category | 홈페이지, 보험, 부동산 등 |
| stage | NEW, CONSULTING, MATERIAL_REQUIRED, QUOTED, REVIEWING, FOLLOW_UP, CONTRACT_PENDING, WON, HOLD, LOST |
| expectedAmount | 예상 금액 |
| confirmedAmount | 확정 금액 |
| summary | 현재 상황 요약 |
| openedAt | 상담 시작일 |
| closedAt | 완료·종료일 |
| createdAt | 생성일 |
| updatedAt | 수정일 |

## 3. Interaction

통화와 상담 기록을 시간순으로 저장한다.

| 필드 | 설명 |
|---|---|
| id | 기록 ID |
| customerId | 고객 ID |
| opportunityId | 상담건 ID, 없을 수 있음 |
| type | INCOMING_CALL, OUTGOING_CALL, NOTE, SMS, MEETING |
| startedAt | 시작 시각 |
| endedAt | 종료 시각 |
| durationSec | 통화시간 |
| result | 관심, 자료발송, 재연락, 완료 등 |
| note | 짧은 상담 메모 |
| createdAt | 생성일 |

## 4. FollowUpTask

놓치면 안 되는 다음 행동을 저장한다.

| 필드 | 설명 |
|---|---|
| id | 업무 ID |
| customerId | 고객 ID |
| opportunityId | 상담건 ID |
| interactionId | 생성 원인이 된 상담 기록 |
| taskType | CALL, SMS, SEND_QUOTE, SEND_MATERIAL, VISIT, PAYMENT_CHECK, CUSTOM |
| title | 업무 제목 |
| dueAt | 처리 예정일 |
| status | PENDING, DONE, SNOOZED, CANCELED |
| completedAt | 완료 시각 |
| createdAt | 생성일 |

## 5. PhoneRule

고객 등록 후보 표시 여부를 제어한다.

| 필드 | 설명 |
|---|---|
| normalizedPhone | 정규화 번호 |
| ruleType | EXCLUDED, OPT_OUT, BUSINESS, PERSONAL |
| reason | 설정 이유 |
| createdAt | 생성일 |

## 6. 주요 관계

- Customer 1:N Opportunity
- Customer 1:N Interaction
- Customer 1:N FollowUpTask
- Opportunity 1:N Interaction
- Opportunity 1:N FollowUpTask

## 7. 상태 변경 규칙

- Opportunity가 `WON`으로 변경되면 Customer를 `EXISTING`으로 전환한다.
- FollowUpTask의 기한이 지났고 완료되지 않으면 `기한 초과`로 표시한다.
- Customer가 `OPT_OUT`이면 신규 FollowUpTask 생성 시 경고한다.
- Customer가 `EXCLUDED`이면 통화 후 처리창을 기본 표시하지 않는다.
