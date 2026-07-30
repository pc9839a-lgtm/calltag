# 콜태그 문자자동화 모듈 이전 구조

기준일: 2026-07-31  
상세 제품 기획: [`CALLTAG_UNIFIED_PRODUCT_PLAN_KO.md`](CALLTAG_UNIFIED_PRODUCT_PLAN_KO.md)

> 기존의 `콜링크·콜태그 별도 앱 연동` 구조는 폐기한다. 콜링크의 검증된 문자 기능을 콜태그 한 앱 안의 `문자자동화` 모듈로 이전한다.

---

## 1. 최종 구조

```text
콜태그 Android 앱
├ 공통 계정·구독
├ 공통 고객 DB
├ 공통 통화 감지
├ 전화관리 모듈
│  ├ 고객 상태
│  ├ 통화 결과·메모
│  ├ 다음 할 일·캘린더
│  ├ 관리공백
│  └ 통계
├ 문자자동화 모듈
│  ├ 템플릿
│  ├ 자동·수동 발송
│  ├ 부재중 자동발송
│  ├ 지연·예약 발송
│  ├ 이미지 MMS
│  ├ SIM·eSIM
│  └ 발송내역
└ 통합 타임라인
   ├ 통화
   ├ 메모
   ├ 상태 변경
   ├ 일정
   └ 문자
```

별도 콜링크 앱 설치, 별도 로그인, 별도 고객 ID, 별도 결제는 만들지 않는다.

---

## 2. 저장소 역할

### 최종 제품 저장소

`pc9839a-lgtm/calltag`

- 최종 Android 앱
- 최종 패키지 `kr.pagero.calltag`
- 최종 UI와 데이터 모델
- 전화관리·문자자동화·통합 구독

### 문자 기능 원본 저장소

`pc9839a-lgtm/call-auto-sms-android`

- 이전 대상 코드의 출처
- 기존 통화 후 자동문자 실기기 검증 기준
- 최종 사용자에게 별도 제품으로 노출하지 않음
- 기능 이전 완료 전까지 원본 보존

---

## 3. 이전 원칙

1. 검증된 문자 발송 로직을 불필요하게 다시 작성하지 않는다.
2. 기존 앱 전체 화면과 DB를 복사하지 않는다.
3. 통화 감지 서비스는 콜태그에서 하나만 유지한다.
4. 고객은 정규화된 전화번호로 하나만 연결한다.
5. 문자 발송 결과는 반드시 고객과 통화 이벤트에 연결한다.
6. 자동 발송과 예약 발송은 사용자가 규칙을 활성화한 경우에만 실행한다.
7. 기존 콜링크 패키지·서명 유지 요구는 없다. 아직 스토어 등록 전이므로 콜태그 패키지를 최종 기준으로 한다.

---

## 4. 이전할 기능

### P0 — 핵심 발송

- 수신 통화 종료 후 문자 처리
- 부재중·거절 후 문자 처리
- 연결된 발신 통화 종료 후 문자 처리
- 자동 발송
- 발송 전 사용자 확인 방식
- 백그라운드·화면 꺼짐 동작
- 중복 발송 방지
- 제외번호·수신거부

### P1 — 메시지 운영

- 상황별 메시지
- 템플릿 저장·복제·삭제·적용
- SMS/LMS
- 이미지 MMS와 일반 문자 대체
- 발송 성공·실패 기록
- 검색과 기간 필터

### P2 — 회선과 예약

- SIM·eSIM 선택
- 회선별 자동문자 사용 여부
- 회선별 메시지·이미지·시간 설정
- 즉시·예약 발송
- 단체 순차 발송
- 재부팅 후 예약 복구

### P3 — 신규 통합 자동화

- 통화 종료 N일 뒤 발송
- 통화 결과별 템플릿 추천
- 상태 변경 시 예약 취소
- 일정 생성과 예약 문자 연결
- 문자 발송 완료 시 관련 일정 처리

---

## 5. 제거할 중복 구조

- 콜링크 전용 고객 테이블
- 콜링크 전용 제외번호 테이블
- 콜링크 전용 로그인·회원가입
- 콜링크 전용 구독 상태
- 콜링크 전용 하단 메뉴
- 별도 고객 추가·수정 화면
- 중복 통화 모니터 서비스
- 별도 발송내역 고객 연결 방식

필요한 레코드는 콜태그 데이터 모델로 변환한다.

---

## 6. 공통 식별자

```text
user_id
workspace_id
device_id
customer_id
call_event_id
interaction_id
task_id
message_id
template_id
automation_rule_id
scheduled_message_id
```

전화번호 문자열은 외부 식별자가 아니다. 정규화된 번호는 고객 후보를 찾는 데 사용하고 실제 연결은 `customer_id`로 저장한다.

---

## 7. 통합 데이터 모델

### customers

- id
- workspace_id
- display_name
- primary_phone
- normalized_phone
- relation_status
- memo
- pinned_call_memo
- created_at
- updated_at

### call_events

- id
- workspace_id
- device_id
- customer_id
- normalized_phone
- direction
- call_type
- started_at
- ended_at
- duration_sec
- classification_status

### interactions

- id
- customer_id
- call_event_id
- type
- result
- note
- occurred_at

### follow_up_tasks

- id
- customer_id
- interaction_id
- task_type
- title
- due_at
- status
- completed_at

### message_templates

- id
- title
- body
- image_refs
- category
- enabled

### automation_rules

- id
- name
- trigger_type
- condition_json
- action_json
- enabled
- priority

### scheduled_messages

- id
- customer_id
- call_event_id
- rule_id
- template_id
- body_snapshot
- scheduled_at
- status
- cancellation_reason
- sent_at
- error_code

### messages

- id
- customer_id
- call_event_id
- template_id
- scheduled_message_id
- body_snapshot
- status
- sent_at
- error_code

---

## 8. 통화 이벤트 처리

하나의 통화 감지 계층이 다음 두 모듈에 이벤트를 배포한다.

```text
CallEventDetector
→ PhoneCrmProcessor
→ MessageAutomationProcessor
```

### 전화관리 구매자

- 고객 조회
- 통화 결과 정리
- 메모·상태·일정
- 관리공백·통계

### 문자자동화 구매자

- 자동발송 규칙 평가
- 템플릿 선택
- 발송 또는 예약
- 발송 결과 기록

### 통합 구매자

두 프로세서가 같은 `call_event_id`와 `customer_id`를 사용한다.

---

## 9. 예약 발송 구조

3일 뒤 발송처럼 긴 지연 작업은 앱 프로세스가 종료돼도 유지돼야 한다.

```text
사용자 규칙 저장
→ scheduled_messages 생성
→ 지속 가능한 작업 스케줄 등록
→ 예정 시각에 조건 재검사
→ 발송 또는 취소·건너뜀
→ 결과 저장
```

발송 직전 재검사:

- 구독이 유효한가
- 자동화 규칙이 켜져 있는가
- 고객이 제외·수신거부가 아닌가
- 동일 문자가 이미 발송되지 않았는가
- 고객 상태가 완료·종료로 바뀌지 않았는가
- 새로운 통화나 수동 처리가 예약 목적을 대체하지 않았는가

예약 발송은 생성 시점 조건만 믿고 무조건 보내면 안 된다.

---

## 10. 구독 기능 권한

| 권한 | 전화 1,900원 | 문자 990원 | 통합 2,500원 |
|---|:---:|:---:|:---:|
| 통화 CRM | O | - | O |
| 고객 상태·통계 | O | - | O |
| 일정·관리공백 | O | - | O |
| 수동 문자 | - | O | O |
| 자동·부재중 문자 | - | O | O |
| 예약·지연 문자 | - | O | O |
| 통화 종료 통합 정리 | 일부 | 일부 | 전체 |
| 통합 타임라인 | 통화 중심 | 문자 중심 | 전체 |

내부 권한:

```text
PHONE_CRM_ENABLED
MESSAGE_AUTOMATION_ENABLED
```

통합 상품은 두 권한을 모두 활성화한다.

---

## 11. UI 통합

하단 메뉴:

1. 홈
2. 고객
3. 메시지
4. 일정

문자 모듈 미구매자는 메시지 탭에서 기능 미리보기와 `월 990원` 구독 버튼을 본다.

전화 모듈 미구매자는 고객 통계·상태·통화 정리 등 고급 CRM 기능에 `월 1,900원` 잠금을 표시한다.

통합 업셀은 통화 종료 화면에서 가장 자연스럽게 제시한다.

```text
상태·메모 저장 후 문자까지 한 번에 처리
통합 이용 월 2,500원
```

---

## 12. 이전 순서

### 1차

- 콜링크 핵심 통화 후 발송 클래스 식별
- 권한·서비스·리시버 충돌 목록 작성
- 콜태그 통화 이벤트 인터페이스 확정

### 2차

- 자동·수동 문자 발송 이전
- SIM·eSIM 이전
- 템플릿 이전
- 기존 실기기 시나리오 재검증

### 3차

- 발송내역을 콜태그 고객 타임라인에 연결
- 부재중·거절·발신 통화 통합
- 중복 감지 서비스 제거

### 4차

- 3일 뒤 등 지연 자동발송
- 조건 재검사·자동 취소
- 일정·상태 연동

### 5차

- 전화 1,900원·문자 990원·통합 2,500원 결제 권한 적용
- 만료·복원·오프라인 유예 검증

---

## 13. 검수 기준

- 하나의 통화가 중복 처리되지 않는다.
- 같은 번호가 두 고객으로 생성되지 않는다.
- 전화관리만 구매하면 문자가 발송되지 않는다.
- 문자자동화만 구매해도 설정한 자동 문자가 정상 동작한다.
- 통합 구매자는 상태·메모·일정·문자를 한 번에 처리한다.
- 3일 뒤 예약이 앱 종료·재부팅 후에도 유지된다.
- 수동 발송 또는 고객 상태 변경 시 불필요한 예약이 취소된다.
- 듀얼 SIM에서 설정한 회선으로 발송된다.
- 제외번호·수신거부·중복방지가 모든 발송 경로에 공통 적용된다.
- 통화와 문자 결과가 올바른 고객 타임라인에 표시된다.
