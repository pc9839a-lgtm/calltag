# CallTag 다음 프로덕션 패치 우선순위

기준일: **2026-10-06**  
현재 Google Play 프로덕션: **0.44.59 / 2026091601**  
기준 브랜치: `release/calltag-v04459-r8-optimization`  
다음 패치 준비 브랜치: `patch/calltag-next-production-20261006`

> 목적: 현재 프로덕션에서 실제 사용자 사용을 막거나 “연동이 안 된다”고 느끼게 만드는 권한/외부서비스 연동 문제를 최우선으로 정리한다. 신규 기능 추가보다 복구 가능성, 서비스 연결 신뢰도, 오류 가시성을 먼저 해결한다.

## P0-1. 권한 거부 후 복구 UX

### 현재 0.44.59
`InitialPermissionActivity`에는 다음 동작이 이미 있다.

- 최초 필수 권한 자동 요청
- 거부 후 `남은 권한 허용` 표시
- `권한 설정 열기` 표시
- 앱 설정 복귀 시 `onResume()` 재검사
- 모두 허용되면 다음 설정 단계 진행

### 확인된 문제
현재는 `shouldShowRequestPermissionRationale(...)` 기반의 “시스템 팝업 재요청 가능 / 불가능” 분기가 없다.

따라서 Android가 더 이상 권한 팝업을 표시하지 않는 상태에서도 `남은 권한 허용`이 다시 `requestPermissions()`를 호출해, 사용자에게 버튼이 먹통처럼 보일 수 있다.

### 다음 패치
1. 권한 허용됨 → 정상 진행
2. 권한 없음 + 재요청 가능 → 시스템 권한창
3. 권한 없음 + 재요청 불가 → `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`로 즉시 이동
4. 설정 복귀 → 재검사 → 완료 시 자동 다음 단계
5. 버튼 문구도 상태에 따라 `남은 권한 허용` / `권한 설정에서 허용`으로 구분
6. SMS/알림 선택 권한과 전화 CRM 핵심 권한 구분 유지

### QA
- 1회 거부 후 재요청
- 반복 거부/재요청 불가 후 설정 이동
- 일부 권한만 허용
- 설정 복귀 후 자동 재검사
- 앱 재실행 후 복구 경로
- Samsung / Pixel 최신 Android 우선

---

## P0-2. 외부 서비스 연동 전체 재검증

대상:

- PageRo
- Meta Lead Ads
- Google Forms
- Generic Webhook
- Direct API / Universal Lead
- 외부문의 백그라운드 동기화
- Connect Hub 상세 화면 라우팅

### 0.44.59에 이미 포함된 수정/기능
0.44.59는 `release/calltag-v04458-external-input-final`을 기반으로 하므로 아래는 이미 앱 코드에 포함되어 있다.

- Meta OAuth / 리드폼 선택 / Webhook 수신
- Google Forms OAuth / Form 선택 / Responses API 동기화
- Generic Webhook 샘플 기반 필드 매핑 UI
- 전화번호 필수 + 이름/이메일/문의내용 선택 매핑
- Google Forms / Universal Lead 15분 WorkManager 안전망
- foreground 자동 동기화
- Direct API / PageRo 기존 경로 유지

이 항목은 다음 패치에서 새로 재구현하지 말고 **0.44.59 기준 그대로 carry-forward 후 회귀 QA**한다.

### 서버에서 이미 수정된 항목
`pc9839a-lgtm/inlet` main 기준 다음 수정이 이미 병합되어 있다.

1. 외부 연동 readiness + 인증된 production CRUD smoke gate
   - Webhook / Google Forms bridge / Direct API
   - 운영에서 UI만 있고 실제 API가 죽은 상태를 배포 성공으로 처리하지 않도록 fail-closed

2. 외부 연동 API hot path 성능 수정
   - 매 요청마다 CREATE/INDEX DDL 실행하던 경로 제거
   - 기존 schema probe 후 필요 시에만 DDL fallback
   - Webhook 목록 GET에서 만료 payload DELETE 제거
   - 외부 연동 메뉴 진입 timeout 완화

3. PageRo readiness 테이블명 오류 수정
   - 잘못된 `pagero_leads` 검사 대신 실제 `calltag_pagero_leads` 검사

4. Connect Hub section 라우팅 수정
   - `?section=meta`
   - `?section=google-forms`
   - `?section=webhook`
   - `?section=direct-api`
   요청 시 해당 상세 화면으로 진입

**주의:** 위 4개는 서버 main에 있는 수정이다. Android에 복붙하지 않는다. 다음 APK 출시에 앞서 production 배포/실동작을 다시 검증하고, 앱이 해당 경로를 정상 사용한다는 것만 확인한다.

---

## P0-3. 외부 연동 실패를 앱이 숨기는 문제

### 확인된 코드
`ExternalLeadIntegrationActivity.refreshRemoteStatus()`에서 Webhook / Meta / Google Forms 상태 조회 실패를 각각 `catch (Exception ignored) {}`로 버리고 있다.

결과적으로 서버가 실패하거나 세션/연동 문제가 있어도 사용자 화면에서는:
- 이전 상태가 그대로 남거나
- 단순히 미연결처럼 보이거나
- 왜 실패했는지 알 수 없는
상태가 발생할 수 있다.

`syncGoogleFormsThenPull()`도 Google Forms sync 실패를 무시하고 Universal Lead pull을 계속한다.

### 다음 패치
- 채널별 마지막 조회 성공/실패 상태를 별도 보존
- 실패 시 `미연결`로 오인 표시하지 않음
- `연결 상태 확인 실패` / `로그인 다시 확인` / `서버 응답 지연` 등 사용자 메시지 분리
- 401/403은 로그인 복구 경로
- 429는 잠시 후 재시도
- 5xx/timeout은 연결 유지 상태 + 재시도 안내
- 내부 예외 원문/비밀값은 사용자에게 노출하지 않음
- 하나의 provider 실패가 다른 provider 상태 조회를 막지 않는 구조 유지

---

## P0-4. WorkManager가 실제 동기화 실패를 성공으로 끝낼 수 있는 문제

### 확인된 코드
`ExternalLeadSyncWorker`는:

1. `UniversalLeadSyncManager.requestSync(...)` 실행
2. RUNNING이 끝날 때까지 대기
3. RUNNING이 false가 되면 `Result.success()`

구조다.

하지만 `UniversalLeadSyncManager` 내부 API 오류는 자체적으로 catch하고 방송만 보낸 뒤 RUNNING을 종료한다. 따라서 **실제 동기화가 실패해도 Worker가 success로 끝날 수 있다.**

이 경우 WorkManager의 retry/backoff가 작동하지 않아 15분 안전망의 의미가 약해질 수 있다.

### 다음 패치
다음 중 하나로 수정한다.

권장:
- Worker 전용 synchronous sync 결과 API 추가
- 성공 / 재시도 가능 실패 / 영구 실패를 명시적으로 반환
- network/5xx/429/timeout → `Result.retry()`
- 인증 만료/계정 없음 등 사용자 조치 필요 → 상태 저장 후 `Result.failure()` 또는 정책에 맞는 종료

대안:
- `UniversalLeadSyncManager`의 마지막 실행 결과를 thread-safe 상태로 전달해 Worker가 실제 결과를 읽음

### QA
- 네트워크 끊김
- 서버 500
- 429
- 401
- 정상 응답 + 신규 0건
- 정상 응답 + 신규 문의
- Worker timeout
- 연속 enqueue 중복 방지

---

## P1. 실제 계정 E2E 미검증 항목

0.44.58 릴리스 문서에서 아래 항목은 실제 계정 E2E 미완료 상태로 남아 있었다. 다음 패치 배포 전 반드시 다시 확인한다.

1. Meta 테스트 Lead 1건 → 콜태그 고객/문의 도착
2. Google Form 실제 응답 1건 → 콜태그 고객/문의 도착
3. Generic Webhook 샘플 → 필드 매핑 → 다음 실문의 정상 반영
4. PageRo 실제 문의 → 신규/기존 갱신 + 중복 생성 없음
5. Direct API key 생성 → 문의 1건 → 고객/문의 도착 → key revoke
6. 앱 foreground / background / 강제 종료 후 재실행 각각 확인
7. 동일 event/idempotency 재전송 시 중복 고객/상담 생성 없음
8. 공급자 하나 장애 시 다른 채널 문의 수신 유지

실계정 E2E 전에는 “전체 외부연동 정상”으로 표시하지 않는다.

---

## P1. PageRo 실시간/수동 동기화 교차검증

현재 PageRo는:
- 계정 연결 상태
- FCM 실시간 알림
- 수동 `새 문의 확인`
- foreground fallback sync
를 사용한다.

확인할 것:

- 푸시 수신 후 로컬 고객 반영이 실제 DB 반영 뒤에만 표시되는지
- 앱 종료/잠금화면 알림
- 푸시 실패 시 수동 동기화로 복구
- 같은 문의를 푸시 + 수동 sync가 동시에 잡아도 중복 생성되지 않는지
- 기존 고객 번호로 문의가 왔을 때 신규 고객 중복 생성 없이 문의 이력만 추가되는지
- PageRo source badge가 일반 고객에게 잘못 붙지 않는지

---

## P2. 동기화 운영성

- `requiresBatteryNotLow(true)` 때문에 저배터리 상태에서 15분 fallback이 지연되는 영향 확인
- provider별 마지막 성공시간/오류시간 기록
- 외부 연동 화면에서 `마지막 확인`을 채널별로 표시 검토
- CrashTelemetry에는 개인정보 없이 provider/error-code 수준만 기록
- 오류가 반복되어도 사용자에게 매번 Toast 폭탄이 발생하지 않도록 제한

---

## 다음 패치 배포 게이트

다음 조건이 모두 PASS일 때만 다음 production AAB 후보로 올린다.

- 권한 거부 → 복구 흐름 PASS
- Meta E2E PASS
- Google Forms E2E PASS
- Webhook mapping E2E PASS
- PageRo E2E + 중복방지 PASS
- Direct API CRUD + lead ingest PASS
- external WorkManager 실패 시 retry 확인
- server production external CRUD smoke PASS
- Connect Hub section 라우팅 PASS
- 통화 종료 작은 팝업 회귀 없음
- Google 로그인 회귀 없음
- 결제 entitlement 회귀 없음
- versionCode 신규 값 사용
- 기존 Play upload key 유지

## 금지

- 외부 연동 오류를 `catch ignored`로 숨기고 정상/미연결처럼 표시
- 실제 E2E 전 “실연동 완료” 표기
- PageRo/Meta/Google/Webhook 중 하나의 장애로 다른 채널 동기화를 전체 중단
- 서버에서 이미 수정된 hot-path/readiness 수정사항을 Android에 중복 구현
- 다음 패치를 신규 기능 위주로 확대하여 위 P0 항목을 뒤로 미루는 것
