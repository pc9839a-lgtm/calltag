# 콜태그 앱 실행·유지율 — Firebase Analytics 연동 준비

## 범위와 현재 상태

- 대상 Android 패키지: `kr.pagero.calltag`.
- 기준 브랜치: `agent/calltag-v04456-referral-abuse-postcall-hotfix` (앱 소스 보유).
- 웹 관리자 메인 `main`과 Android 개발 브랜치는 별도이며, 이번 작업은 Android 준비 코드만 수정한다.
- 기존 Firebase Cloud Messaging 푸시 및 가입·통화·문자·결제·복구 코드는 그대로 유지한다.
- **실제 Firebase 프로젝트 연결, 운영 AAB 배포, 앱 사용 데이터 수집은 아직 완료되지 않았다.**
- 앱 업데이트 전에 Play 개인정보처리방침 및 데이터 보안 항목 재검토가 필요하다.

## 수집할 지표와 정의

| 지표 | 근거 이벤트 | 집계 |
|---|---|---|
| 앱 실제 열기 | `calltag_app_open` | 전면 진입마다 1회, 자동 통화 팝업 제외 |
| 최초 사용 | `first_open` | Firebase Analytics 자동 수집. 신규 설치 후 첫 사용(재설치 가능) |
| 세션 | `session_start` | Firebase Analytics 자동 수집. **앱 실행 횟수와 동일하지 않음** |
| 활성 사용자 DAU/WAU/MAU | GA4 활성 사용자 | 하루/7일/30일의 중복 제거 사용자 |
| D1/D7/D30 재방문 유지율 | `calltag_app_open` 코호트 또는 GA4 유지율 보고서 | 최초 사용 코호트 중 정확히 +1/+7/+30일 재방문 비율 |

**주의:** 표준 GA4 유지율은 `session_start`/참여 데이터를 기준으로 통화 팝업 진입을 포함할 수 있다. 실제 사용자가 앱을 다시 연 비율이 필요하면 `calltag_app_open`을 기준으로 별도 코호트 분석을 실시한다. 신규 사용자 코호트는 Analytics 측 `first_open` 또는 익명 앱 인스턴스 단위로 정한다. 아직 관측되지 않은 구간은 0%가 아니라 `—` 로 표시한다. 한국시간(Asia/Seoul) 기준으로 보고서를 고정한다.

## 빌드 연결

1. Firebase 콘솔(<https://console.firebase.google.com/>)에서 기존 CallTag Firebase 프로젝트를 열고 **Google Analytics 연결 여부**를 확인한다. Firebase 프로젝트가 이미 있다면 새로 만들지 않는다.
2. Android 앱을 `kr.pagero.calltag` 패키지로 연결한다. FCM이 사용하는 프로젝트와 같은 Firebase 프로젝트인지 확인한다.
3. Firebase에서 다운로드한 `google-services.json`을 로컬의 `app/google-services.json`에 배치한다. 이 파일은 Git에 커밋하지 않는다.
4. 기존 FCM용 `CALLTAG_FIREBASE_APPLICATION_ID`, `CALLTAG_FIREBASE_API_KEY`, `CALLTAG_FIREBASE_PROJECT_ID`, `CALLTAG_FIREBASE_SENDER_ID` 환경변수는 그대로 유지한다. JSON과 프로젝트가 동일한지 확인한다.
5. 개인정보처리방침/Play Console 데이터 보안 검토 후 빌드 환경에 `CALLTAG_ENABLE_ANALYTICS=true`를 설정한다. JSON 파일이 없거나 이 플래그가 없으면 Analytics 자동 수집을 비활성화한다.
6. 기존 업로드 서명과 versionCode 정책을 유지한다. 이 준비 작업에서는 앱 버전이나 서명키를 변경하지 않는다.

조건부 Google Services 플러그인 + Firebase BoM + Analytics SDK 방식이다. **동작하는 FCM 환경변수를 임의로 교체하지 않는다.**

## 검증 순서

- Google Services JSON 없는 상태: `:app:assembleDebug`가 계속 빌드되는지 확인; Analytics는 꺼져야 한다.
- JSON 있는 상태 + 플래그 OFF: 자동 Analytics 수집이 비활성화되어야 한다.
- JSON 있는 상태 + 플래그 ON: 앱 실행 → Firebase Analytics DebugView에서 `first_open`, `session_start`, `calltag_app_open` 확인.
- 화면 간 이동로 `calltag_app_open`가 증가하면 안 된다. 백그라운드에서 다시 전면으로 돌아올 때만 1회 기록한다.
- `PostCallActivity` / `CallerInfoActivity`의 자동 표시로 커스텀 앱 실행 수가 증가하면 안 된다.
- FCM 신규 문의 알림, Google 로그인, 결제·구독, 통화 종료·자동문자, 기존 데이터 유지 회귀 테스트.
- 운영 릴리스 후 최소 1일이 지나야 D1 코호트가 생성되고 D7/D30은 각각 해당 일수가 경과해야 검증할 수 있다.

## 웹 관리자 대시보드 연결 — 다음 단계

Firebase/GA4 사용 측정이 실기기에서 확인되면 별도 서버측 GA4 Data API 연동을 한다.
- 관리자 인증 뒤 서버 API만 호출. 브라우저에 서비스계정 키를 노출하지 않는다.
- Google Analytics Data API 측정용 GA4 Property ID와 읽기 권한 서비스 계정을 연결한다.
- 응답: 데이터 기준일·타임존·앱 오픈 횟수·DAU/WAU/MAU·D1/D7/D30·연결 상태.
- 값 없는 경우 `—`로 처리, 추정치·더미 수치를 운영 카드에 사용 금지.
- 일별 1회 이상 갱신하면서 보고서 집계 지연을 표시한다.
- 기존 CallTag 관리자 페이지의 `회원 현황` 카드와 구별되는 `앱 이용` 섹션만 추가한다.
- 운영 Firebase 연결 없이 빈 카드나 `권한 확인 필요` 대형 경고를 먼저 노출하지 않는다.

## 데이터 최소화

고객 이름, 휴대전화번호, 이메일, 계정 owner ID, 통화기록, 상담 메모, 문자내용, 위치와 Firebase 푸시 토큰을 Analytics 파라미터로 전송하지 않는다. 광고 ID 수집은 manifest에서 비활성화한다. Firestore, Realtime Database, Cloud Functions는 본 작업 범위에 포함하지 않는다.

## 구독자 분석 — 2026-10-21 추가

- `calltag_app_open` 이벤트에 `subscriber_status`와 `subscriber_plan`을 함께 기록한다.
- GA4 User Property에도 동일한 저카디널리티 그룹을 설정하고 서버 entitlement 응답이 저장될 때 갱신한다.
- `subscriber_status`: `subscriber`(구독 이용), `trial`(무료체험), `granted`(별도 지급), `free`(무료/비활성), `unknown`(검증 전/오래된 정보).
- `subscriber_plan`: `call`, `message`, `both`, `web_bundle`, `none`.
- 서버 확인 기록이 24시간을 넘었으면 `unknown`으로 보내, 오래된 유료 권한 상태가 최근 유료 이용으로 과장되지 않게 한다.
- 사용자가 로그아웃/계정을 전환하면 기존 유저 프로퍼티를 즉시 삭제한다. 계정의 고유 ID, 이메일 및 결제 토큰은 Firebase Analytics로 전송하지 않는다.
- **이 분류는 서버 entitlement의 구독 접근 상태이지, 확정 주문/실매출/실제 유료 결제자 원장이 아니다.** 공식 유료 결제자 수·신규 결제자·해지자·환불·정산은 결제 원장을 기준으로 집계한다.
- DAU/WAU/MAU와 D1/D7/D30을 `subscriber_status`별로 구분하되, 그 시점의 구독 상태(event parameter)와 현재 구독 상태(user property)를 혼동하지 않는다. 정확한 유료 전환 코호트는 원장의 최초 결제일/상태전이와 GA4 이용 코호트를 연계하는 별도 데이터 파이프라인이 있어야 한다.
- GA4의 커스텀 디멘션에 `subscriber_status`와 `subscriber_plan`을 등록하고, DebugView에서 구독/체험/지급 시나리오의 이벤트 값을 검증한다. 소규모 코호트는 Google Analytics 개인정보 보호 기준에 따라 표시가 제한될 수 있다.

### 구독자 지표 설계
| 지표 | 권위 있는 데이터/측정 |
|---|---|
| 현재 유료 구독자 | 관리자 서버 `activePaid` (계정 중복 제거) |
| 신규 유료 구독자 | 결제 원장의 최초 검증 결제일에 따른 계정 집계 (서버 API 추가 필요) |
| 해지/만료 계정 | 서버 구독 상태 전이 이력 (서버 API 추가 필요) |
| 무료체험→유료 전환율 | 무료체험 시작 코호트 중 특정 기간 내 최초 검증 결제 계정 / 전체 적격 체험 코호트 (별도 API) |
| 유료 구독자 활동/유지율 | `calltag_app_open` + `subscriber_status=subscriber` 조건의 GA4 분석 |
| 비구독자와 비교 | `trial`, `free`, `granted`, `subscriber`를 구분해 계산 |

관리자 대시보드의 단순 `activePaid/totalMembers`는 **유료 회원 비중**이지 무료체험→유료 전환율이 아니다.
