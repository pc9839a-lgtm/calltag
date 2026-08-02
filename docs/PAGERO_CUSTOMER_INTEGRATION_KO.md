# 페이지로 고객문의 → 콜태그 앱 연동

기준 버전: **콜태그 Android 0.39.0**  
기준일: **2026-08-02**

## 1. 현재 운영 구조

페이지로와 콜태그는 같은 Inlet 계정의 `ownerId`를 기준으로 연결한다. 일반 사용자가 Webhook 주소, 비밀키, workspace 키를 직접 입력하지 않는다.

```text
페이지로 공개 랜딩에서 문의 접수
→ POST /api/leads 저장 성공
→ PageRo 미들웨어가 같은 D1의 CallTag 문의 큐에 복제
→ 문의 소유 프로젝트의 owner_id 저장
→ 콜태그 앱 로그인 세션 검증
→ 같은 ownerId 문의만 앱으로 전달
→ 로컬 고객 생성 또는 기존 고객 갱신
→ 서버에 IMPORTED ACK
```

운영 큐 상태 확인:

```text
GET https://pagero.kr/api/call/pagero/health
```

정상 응답:

```json
{
  "ok": true,
  "service": "pagero-calltag-lead-queue",
  "database": "ready"
}
```

## 2. 일반 사용자 연결 방법

별도 연동키를 발급하거나 붙여넣지 않는다.

1. 페이지로에 사용할 이메일 계정으로 로그인한다.
2. 콜태그 앱에도 **동일한 계정**으로 로그인한다.
3. 페이지로에서 랜딩페이지를 만들고 공개한다.
4. 공개 랜딩페이지에서 고객 문의가 접수되면 문의가 자동으로 콜태그 전달 대기열에 들어간다.
5. 콜태그 앱에서 `더보기 → 페이지로 연결 → 지금 동기화`를 누른다.
6. 신규 고객 수와 기존 고객 갱신 수를 확인한 뒤 고객목록으로 돌아간다.

앱의 `페이지로 연결` 화면에는 다음 정보가 보인다.

- 현재 콜태그 로그인 이메일
- 자동 연결 준비 여부
- 마지막 동기화 시간
- 신규 고객 수
- 기존 고객 갱신 수
- 확인이 필요한 문의 수
- 마지막 오류 메시지와 오류 코드
- 페이지로 열기
- 지금 동기화

계정이 다르면 문의가 보이지 않는다. 콜태그에서 로그아웃한 뒤 페이지로와 같은 계정으로 다시 로그인해야 한다.

## 3. 테스트 계정

현재 연동 테스트 계정:

```text
pc9839a@naver.com
```

테스트 조건:

```text
페이지로 프로젝트 소유 계정 = pc9839a@naver.com
콜태그 앱 로그인 계정       = pc9839a@naver.com
```

테스트 문의는 기존 고객과 구분하기 위해 아직 콜태그에 없는 전화번호를 사용한다.

권장 테스트값:

```text
이름: 페이지로 연동 테스트
문의내용: 콜태그 자동등록 테스트 2026-08-02
전화번호: 앱에서 확인 가능한 미등록 테스트 번호
```

테스트 순서:

1. `pc9839a@naver.com` 소유의 페이지로 랜딩을 공개한다.
2. 공개 페이지에서 테스트 문의 1건을 접수한다.
3. 콜태그 0.39.0 앱을 `pc9839a@naver.com`으로 로그인한다.
4. `더보기 → 페이지로 연결`로 이동한다.
5. 화면의 연결 계정이 `pc9839a@naver.com`인지 확인한다.
6. `지금 동기화`를 누른다.
7. `신규 1건` 또는 `기존 고객 갱신 1건` 결과를 확인한다.
8. 뒤로 이동한 뒤 고객목록에서 `[페이지로]` 문의 메모와 유입 배지를 확인한다.

## 4. 서버 API

### 앱 문의 조회

```text
GET /api/call/pagero/leads?after=0&limit=50
X-Inlet-Session: <콜태그 로그인 세션>
```

서버는 세션에서 `ownerId`를 확인하고 해당 소유자의 `PENDING`, `DELIVERED` 문의만 반환한다.

### 앱 처리 완료

```text
POST /api/call/pagero/leads/ack
X-Inlet-Session: <콜태그 로그인 세션>
Content-Type: application/json
```

```json
{
  "leadIds": [1, 2],
  "status": "IMPORTED",
  "result": "신규 고객 1건, 기존 고객 갱신 1건"
}
```

지원 상태:

- `PENDING`: 페이지로 문의 저장 완료
- `DELIVERED`: 앱에 전달했지만 ACK 전
- `IMPORTED`: 앱 고객 DB 반영 완료
- `REJECTED`: 번호 오류 등 앱 반영 불가

## 5. Android 동작

주요 파일:

```text
PageroLeadApiClient.java
PageroLeadSyncManager.java
PageroLead.java
PageroLeadReceiptStore.java
PageroConnectionActivity.java
PageroConnectionStatusStore.java
CustomerSourceResolver.java
```

Android 처리 순서:

1. 운영 API `https://pagero.kr/api/call/pagero/leads`를 우선 호출한다.
2. 문의 전화번호로 기존 고객을 조회한다.
3. 고객이 없으면 `source=페이지로` 신규 고객을 만든다.
4. 같은 번호가 있으면 고객을 중복 생성하지 않고 기존 고객을 갱신한다.
5. 문의내용, 랜딩 ID, 캠페인, 접수 URL을 고객 메모에 추가한다.
6. `PAGERO_INQUIRY` interaction을 기록한다.
7. 로컬 `event_id` 수신 이력으로 중복 반영을 막는다.
8. 고객 저장 후 서버에 `IMPORTED` ACK를 보낸다.
9. 동기화 성공 여부와 건수를 앱 화면에 저장·표시한다.

자동 동기화 시점:

- 앱 프로세스 시작
- 보호된 앱 화면 재진입
- `페이지로 연결` 화면에서 `지금 동기화` 실행

현재는 앱이 완전히 종료된 상태에서 FCM Push로 문의를 즉시 알리는 방식이 아니다. 앱을 열거나 `지금 동기화`를 실행해야 한다.

## 6. 서버 구현 위치

페이지로 저장소 `pc9839a-lgtm/inlet`:

```text
functions/api/_middleware.js
functions/api/call/pagero/_shared.js
functions/api/call/pagero/leads.js
functions/api/call/pagero/leads/ack.js
functions/api/call/pagero/health.js
migrations/0006_calltag_pagero_lead_queue.sql
```

페이지로 `/api/leads` 저장이 성공한 경우에만 CallTag 큐에 복제한다. 큐 복제가 실패해도 기존 페이지로 문의 접수 자체는 실패시키지 않는다.

## 7. 보안·데이터 원칙

- 일반 사용자에게 서버 비밀키를 노출하지 않는다.
- 이메일 문자열을 직접 고객 조회 조건으로 사용하지 않는다.
- PageRo 프로젝트 소유자의 `owner_id`와 CallTag 로그인 세션의 `ownerId`를 사용한다.
- 모든 조회와 ACK에 `ownerId` 조건을 적용한다.
- `event_id` UNIQUE 제약과 Android 수신 이력으로 중복을 이중 차단한다.
- 고객 저장이 성공하기 전에는 `IMPORTED` ACK를 보내지 않는다.
- 잘못된 전화번호는 `REJECTED` 처리한다.
- 기존 고객·문의 데이터를 초기화하지 않는다.

## 8. 현재 제한과 후속 개선

- 앱 종료 상태의 즉시 Push 알림은 아직 없다.
- 문의 동기화는 한 번에 최대 200건이다.
- 동일 번호 기존 고객은 새 고객을 만들지 않고 기존 고객을 갱신한다.
- 현재 고객의 `source`는 페이지로 문의가 들어오면 `페이지로`로 갱신된다. 장기적으로는 `최초 유입 경로`와 `최근 문의 경로`를 별도 필드로 분리해야 한다.
- 실기기 E2E 검증 전에는 운영 연동 완료로 기록하지 않는다.
