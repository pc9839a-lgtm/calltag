# 페이지로 콜링크·콜태그 통합 구조

기준일: 2026-07-30

## 1. 통합 원칙

콜링크와 콜태그는 기능이 다른 별도 앱이지만 사용자 관점에서는 하나의 페이지로 업무 서비스다.

- 콜링크: 통화 후 문자 발송
- 콜태그: 고객·상담·일정 관리
- 공통: 계정, 고객, 통화, 구독, 기기, 개인정보 설정

앱 설치 여부와 도메인이 달라도 같은 계정으로 로그인하면 동일한 고객과 활동 이력을 확인해야 한다.

## 2. 서비스 경계

### 콜링크가 소유하는 기능

- 문자 템플릿
- 이미지 첨부
- 자동·수동 발송
- 발송 성공·실패 상태
- 통화 종료 후 문자 규칙

### 콜태그가 소유하는 기능

- 고객 관계 상태
- 상담 결과
- 고객 메모
- 후속 일정
- 일정 완료 상태
- 고객 활동 타임라인

### 공통 플랫폼이 소유하는 기능

- 사용자 계정
- 워크스페이스
- 연결 기기
- 전화번호 정규화
- 고객 기본정보
- 통화 이벤트
- 결제·구독
- 동의·탈퇴·데이터 삭제

## 3. 공통 식별자

모든 시스템은 다음 식별자를 공유한다.

- `user_id`: 로그인 사용자
- `workspace_id`: 데이터 소유 공간
- `device_id`: Android 기기
- `customer_id`: 고객
- `call_event_id`: 통화 이벤트
- `interaction_id`: 상담·활동 기록
- `task_id`: 일정·후속업무
- `message_id`: 문자 발송 건
- `template_id`: 문자 템플릿

전화번호 문자열을 시스템 간 기본키로 사용하지 않는다. 전화번호는 정규화 후 고객 연결에만 사용한다.

## 4. 공통 데이터 모델

### users

- id
- email 또는 휴대폰 로그인 식별자
- display_name
- status
- created_at

### workspaces

- id
- owner_user_id
- name
- plan
- created_at

### devices

- id
- workspace_id
- platform
- app_type: `CALLLINK`, `CALLTAG`
- push_token
- last_seen_at
- app_version

### customers

- id
- workspace_id
- display_name
- normalized_phone
- relation_status
- memo
- created_at
- updated_at
- version

### call_events

- id
- workspace_id
- device_id
- normalized_phone
- direction
- started_at
- ended_at
- duration_sec
- customer_id
- classification_status

### interactions

- id
- workspace_id
- customer_id
- call_event_id
- source_app
- type
- result
- note
- occurred_at

### follow_up_tasks

- id
- workspace_id
- customer_id
- interaction_id
- task_type
- title
- due_at
- status
- completed_at
- updated_at

### message_templates

- id
- workspace_id
- title
- body
- image_refs
- trigger_type
- enabled

### messages

- id
- workspace_id
- customer_id
- call_event_id
- template_id
- device_id
- body_snapshot
- status
- sent_at
- error_code

## 5. 통합 타임라인

고객 상세 타임라인에서는 앱 구분보다 시간 흐름을 우선한다.

표시 예:

1. 수신 통화
2. 상담 결과 `견적 요청`
3. 일정 `자료 보내기` 등록
4. 콜링크 문자 발송 완료
5. 일정 완료
6. 고객 상태 `상담 중 → 기존`

각 이벤트에는 출처를 작은 배지로만 표시한다.

- 통화
- 콜태그
- 콜링크
- 웹

## 6. 앱·웹 동기화 API 초안

### 인증

- `POST /v1/auth/login`
- `POST /v1/auth/refresh`
- `POST /v1/devices/register`

### 동기화

- `POST /v1/sync/push`
- `GET /v1/sync/pull?cursor=`

### 고객

- `GET /v1/customers`
- `POST /v1/customers`
- `GET /v1/customers/{id}`
- `PATCH /v1/customers/{id}`

### 통화

- `POST /v1/call-events`
- `GET /v1/call-events`
- `PATCH /v1/call-events/{id}/classification`

### 일정

- `GET /v1/tasks`
- `POST /v1/tasks`
- `PATCH /v1/tasks/{id}`
- `POST /v1/tasks/{id}/complete`
- `POST /v1/tasks/{id}/reopen`

### 문자

- `GET /v1/templates`
- `POST /v1/messages/prepare`
- `POST /v1/messages/{id}/result`
- `GET /v1/customers/{id}/messages`

## 7. 메시지 발송 연결 방식

웹 브라우저가 직접 일반 SMS를 발송하지 않는다.

기본 흐름:

1. 웹에서 고객과 템플릿 선택
2. 서버에 발송 요청 생성
3. 연결된 콜링크 Android 앱에 푸시
4. 앱에서 사용자 확인 또는 설정된 규칙에 따라 발송
5. 성공·실패 결과를 서버로 전송
6. 웹과 콜태그 타임라인에 결과 표시

사용자 동의 없이 웹에서 앱을 원격 조작해 문자를 자동 발송하지 않는다.

## 8. 구독 통합

초기 원칙:

- 페이지로 계정당 하나의 구독 상태를 가진다.
- 구독 상품은 기능 권한으로 분리한다.
  - `CALLLINK_ENABLED`
  - `CALLTAG_ENABLED`
  - `WEB_ENABLED`
- 향후 묶음 상품은 `PAGERO_BUSINESS_BUNDLE`로 추가한다.
- 앱별 중복 결제 여부를 서버에서 검사한다.

## 9. 도메인과 인증

권장 구조:

- 콜태그: `calltag.pagero.kr`
- 콜링크: 기존 콜링크 공개 주소 유지
- 통합 포털: `app.pagero.kr`
- API: `api.pagero.kr`
- 인증: `auth.pagero.kr` 또는 API 내부 인증 경로

초기에는 콜태그 도메인만 구축해도 API와 계정 구조는 통합 포털 확장을 전제로 설계한다.

## 10. 단계별 구현

### 1단계

- 서버 계정
- 기기 등록
- 고객·일정 양방향 동기화
- 웹 고객·캘린더

### 2단계

- 통화 이벤트 동기화
- 미정리 통화 웹 처리
- 고객 통합 타임라인

### 3단계

- 콜링크 템플릿 조회
- 웹 발송 요청 → Android 콜링크 전달
- 문자 결과 타임라인 반영

### 4단계

- 통합 구독
- `app.pagero.kr` 통합 포털
- 팀 워크스페이스

## 11. 검수 기준

- 콜링크와 콜태그에서 같은 번호가 서로 다른 고객으로 생성되지 않는다.
- 고객명 수정이 양쪽 앱과 웹에 반영된다.
- 문자 발송 결과가 올바른 고객과 통화에 연결된다.
- 일정 완료 상태가 앱과 웹에서 일치한다.
- 하나의 계정으로 두 앱과 웹에 로그인한다.
- 앱 삭제·기기 교체 후에도 서버 데이터가 유지된다.
- 회원탈퇴 시 공통 계정과 모든 제품 데이터가 함께 삭제된다.
