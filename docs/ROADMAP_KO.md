# 콜태그 개발 로드맵

기준일: **2026-08-23**  
현재 버전: **0.44.45 / versionCode 2026082101**

## 완료된 핵심

- [x] 이메일/Google 로그인
- [x] Google 로그인 실제 단말 성공
- [x] 고객 생성·수정·상태·메모
- [x] 고객 삭제 현재 화면 팝업
- [x] 고객 연락처 저장
- [x] 오늘 할 일 / 확인할 통화
- [x] 확인할 통화 카드 탭 고객정보/메모 수정 팝업
- [x] 확인할 통화 `할 일 등록` 현재 화면 팝업 처리
- [x] 캘린더 / 일정
- [x] 일정 시간 선택 휠 UI
- [x] 월간 캘린더 본문 접기/펼치기
- [x] 캘린더 접힘 상태 저장
- [x] 통계
- [x] 더보기 설정 검색/섹션 구조
- [x] 블랙/화이트 앱 테마 선택
- [x] 화이트 모드 Light Material parent 분리
- [x] 화이트 secondary button 다크 하드코딩 제거
- [x] 통화 감지 foreground service
- [x] 수신 등록고객 정보 표시
- [x] 통화 종료 작은 오버레이
- [x] 통화 종료 팝업 고객명 + 메모 즉시 수정
- [x] 통화 종료 후 앱 Activity 자동 실행 제거
- [x] overlay 실패 시 알림 fallback
- [x] CallLog 기반 종료 누락 복구
- [x] 중복 통화 처리 ledger
- [x] fallback 불가 시 미전달 recovery queue 유지
- [x] WorkManager 15분 주기 독립 CallLog 복구
- [x] 최근 12시간 + recovery cursor + 5분 grace 재검사
- [x] Worker에서 기존 ledger 재사용
- [x] 앱 시작 시 recovery worker 스케줄 재확인
- [x] 재부팅/앱 업데이트 시 immediate recovery enqueue
- [x] foreground service 시작 실패 시 monitor 설정 임의 OFF 방지
- [x] 고객선택후 문자
- [x] 통화후 자동문자
- [x] 페이지로 문의접수문자
- [x] 문자 템플릿 / 그룹문자 / 발송내역
- [x] 템플릿 선택 카드에 수정 버튼 직접 노출
- [x] 템플릿 카드 간격 확대
- [x] 페이지로 문의 → 콜태그 고객 자동 동기화
- [x] 페이지로 문의 상세 필드 보존 강화
- [x] 일반 고객까지 페이지로로 표시되던 출처 배지 버그 수정
- [x] 실제 customer.source 기반 페이지로 출처 판정으로 제한
- [x] Google Play Billing Library 연동
- [x] `call_monthly` 서버 검증
- [x] Billing 상품조회와 서버 entitlement 조회 분리
- [x] 다른 계정 purchase token 재귀속 방지
- [x] 계정 전환 시 entitlement 관련 cache 정리
- [x] 기존 Play 업로드 키 검증 CI
- [x] API 36 대응
- [x] 0.44.45 signed AAB 빌드 성공

## P0 — 다음 패치 순서

### 1. 화이트 테마 전 화면 마감

- [ ] 고객/캘린더/홈/통계/더보기 전 화면 실기기 확인
- [ ] 입력창에 다크 배경/다크 stroke 잔재 확인
- [ ] Switch/Toggle/Chip 화이트 대비 확인
- [ ] AlertDialog/BottomSheet 화이트 대비 확인
- [ ] 하단탭/상단바 아이콘·텍스트 대비 확인
- [ ] 삭제/경고/비활성 버튼 대비 확인
- [ ] 커스텀 drawable의 직접 `#1.../#2.../#3...` 다크 색상 전수조사
- [ ] 테마 변경 직후 현재 Activity와 다음 Activity 색상 불일치 확인

### 2. 통화 종료 작은 팝업 실기기 확정

- [ ] 0.44.45에서 통화 종료 후 앱 화면이 앞으로 열리지 않는지 20회 이상 반복
- [ ] 고객명 + 메모 입력/수정/저장 확인
- [ ] 앱 전면 상태 수신/발신
- [ ] 앱 백그라운드 상태 수신/발신
- [ ] 화면 잠금 상태
- [ ] 장시간 미사용 후 첫 통화
- [ ] 부재중/거절/1~3초 짧은 통화
- [ ] 연속 통화 중복 팝업 여부
- [ ] 연속 통화 중복 자동문자 여부
- [ ] 삼성/픽셀/기타 OEM별 overlay 노출률
- [ ] overlay 권한 OFF + 알림 ON fallback
- [ ] overlay 권한 OFF + 알림 OFF recovery queue 유지

### 3. 홈 확인할 통화 실기기 확정

- [ ] 카드 빈 영역 탭 → 고객명/상태/메모 수정 팝업
- [ ] `할 일 등록` → 별도 Activity 이동 없이 팝업 처리
- [ ] 할 일 저장 후 카드/오늘 할 일 즉시 갱신
- [ ] 다시 전화/삭제 액션 충돌 없음
- [ ] 기존 고객과 미등록 번호 모두 정상 처리

### 4. WorkManager recovery 실기기 검증

- [ ] foreground service 종료 후 15분 내 누락 CallLog 복구
- [ ] 재부팅 후 immediate recovery + 첫 통화 감지
- [ ] 앱 업데이트 직후 immediate recovery + 첫 통화 감지
- [ ] Force stop 후 사용자가 앱 재실행했을 때 worker 재초기화
- [ ] recovery 과정에서 고객/할 일/자동문자 중복 생성이 없는지 확인

### 5. 권한 UX 전체 통일

- [ ] `권한이 없습니다`만 표시하는 화면 전수조사
- [ ] 전화 상태 권한 요청/설정 이동 통일
- [ ] 통화기록 권한 요청/설정 이동 통일
- [ ] 알림 권한 요청/채널 설정 이동 통일
- [ ] 오버레이 권한 요청/설정 이동 통일
- [ ] 연락처 저장 관련 시스템 화면 안내 통일
- [ ] 권한 거부 후 기능 재탭 시 즉시 허용/설정 액션 제공

### 6. 결제 이용 상태/실계정 QA

현재 Play 상품:

- `call_monthly` — 1,900원/월
- `message_monthly` — 990원/월

- [ ] Play 설치본에서 `call_monthly` 구매/복원 재확인
- [ ] `message_monthly` 구매/복원 재확인
- [ ] 두 상품 동시 이용 표시 확인
- [ ] 구매 직후 `이용 중` 상태 즉시 반영
- [ ] 결제 취소 후 앱 표시 확인
- [ ] 국가/결제프로필/테스트트랙별 `ITEM_UNAVAILABLE` 재현 확인

### 7. Google Play RTDN / 구독 lifecycle

- [ ] Pub/Sub topic 구성
- [ ] Google Play notification publisher 권한 설정
- [ ] Play Console RTDN 연결
- [ ] subscriber endpoint 구현
- [ ] 갱신 이벤트 처리
- [ ] 취소 이벤트 처리
- [ ] 만료 이벤트 처리
- [ ] grace/account hold 처리
- [ ] resume/refund/revoke 처리
- [ ] 이벤트 수신 후 Developer API 재검증
- [ ] entitlement 자동 갱신

## P1 — 외부 Lead Intake / CallTag Connect

상세 명세: `EXTERNAL_LEAD_INTAKE_API_SPEC_KO.md`

### Phase 0. 기존 페이지로 구조 일반화

- [ ] PageRo 문의를 canonical Lead Event로 변환하는 adapter
- [ ] 범용 lead queue/data model
- [ ] Customer와 Inquiry Event 분리
- [ ] first source / last source / 문의별 source snapshot
- [ ] 재문의 고객 중복생성 방지 공통화

### Phase 1. Universal Lead API

- [ ] `POST /api/calltag/v1/leads`
- [ ] API Key 발급/회전/revoke
- [ ] `Idempotency-Key`
- [ ] owner scope 강제
- [ ] validation/error contract
- [ ] 개인정보 마스킹 로그

### Phase 2. Generic Webhook + Field Mapper

- [ ] 연결별 Webhook endpoint 발급
- [ ] 임의 JSON raw payload 저장
- [ ] 최근 테스트 payload 보기
- [ ] 이름/전화번호/이메일 alias 자동탐지
- [ ] 중첩 JSON field mapping
- [ ] 수동 매핑 수정
- [ ] mapping version 관리
- [ ] 테스트 Lead 생성

### Phase 3. Android 실시간 Lead Inbox

- [ ] 신규 문의 FCM
- [ ] 앱 종료/잠금 상태 수신
- [ ] 서버 pull + ACK
- [ ] 신규/기존 고객 매칭
- [ ] 문의 질문/답변 전체 표시
- [ ] 알림에서 `전화하기`
- [ ] 통화 종료 고객명+메모 팝업과 자연스럽게 연결
- [ ] Push 누락 후 재동기화 복구

### Phase 4. Meta Lead Ads Native Connector

- [ ] Meta OAuth/App 권한 설계
- [ ] 페이지 선택
- [ ] Lead Form 선택
- [ ] webhook subscription
- [ ] provider event signature 검증
- [ ] lead detail fetch
- [ ] campaign/adset/ad/form attribution
- [ ] 실제 광고계정 E2E
- [ ] 신규 Meta Lead → 휴대폰 즉시알림 측정

### Phase 5. Google Forms Native Connector

- [ ] Google OAuth scope
- [ ] Form 선택
- [ ] response notification/watch
- [ ] watch 자동 갱신
- [ ] 신규 response fetch
- [ ] 질문/답변 mapping
- [ ] Apps Script + Sheets bridge 가이드

### Phase 6. Automation Bridge / 기타 폼

- [ ] Zapier
- [ ] Make
- [ ] n8n
- [ ] WordPress
- [ ] Typeform
- [ ] Tally
- [ ] Webflow/Jotform
- [ ] 자체 홈페이지 REST/Webhook 가이드

### Phase 7. 아임웹

- [ ] 입력폼 submit webhook/API 공개 범위 공식 확인
- [ ] 입력폼 response 접근 scope 확인
- [ ] OAuth/API Key 연동 가능성 확인
- [ ] 필요 시 앱스토어/제휴 절차
- [ ] `제출 완료 후 URL 이동` Redirect Bridge 구현 가능성
- [ ] Redirect에 개인정보 query 직접 전달 금지
- [ ] opaque submission id 기반 server-to-server fetch 가능 여부 확인

### Phase 8. Automation / Outbound

- [ ] 신규 문의 → 접수 자동문자
- [ ] 신규 문의 → 자동 할 일
- [ ] Lead Response Time
- [ ] 미처리 SLA 재알림
- [ ] outbound webhook
- [ ] `lead.created`
- [ ] `customer.stage_changed`
- [ ] `call.completed`
- [ ] `task.completed`
- [ ] 광고 conversion feedback adapter 검토

## P1 — 기존 기능별 회귀/완성도

### 고객/CRM

- [ ] 고객목록 삭제/상태 변경/문자 보내기 회귀 QA
- [ ] 일반 고객에 페이지로 배지가 전혀 안 붙는지 실제 데이터 확인
- [ ] 페이지로 고객에는 배지가 유지되는지 확인
- [ ] 기존 DB의 잘못된 source 값이 있는지 migration 필요성 판단
- [ ] 고객 연락처 저장 후 중복 연락처 UX
- [ ] 홈 고객 상세 진입 경로 통일
- [ ] 홈 `오늘 할 일`이 오늘 일정만 표시하는지 회귀 확인
- [ ] 통화목록 메모 표시와 연락처 이름 변경 로직 분리 확인
- [ ] 고객 상태/일정 커스텀 값 편집 UX 최종 정리

### 문자

- [ ] 템플릿 선택 화면에서 `수정` 버튼과 카드 선택 터치 충돌 확인
- [ ] 템플릿 수정 후 목록 즉시 갱신 확인
- [ ] 통화 후 자동문자 수신/발신/부재중별 실제 발송 QA
- [ ] 이미지 첨부 문자 실사용 여부/지원 범위 최종 결정
- [ ] 예약·후속문자 1/3/7일 및 직접 지정 QA
- [ ] 중복 발송 방지 1/7/30일/영구 옵션 QA
- [ ] 업무시간 외 발송 제한 QA

### 페이지로 연동

- [ ] 업종별 실제 문의 샘플 QA
- [ ] 모든 입력 답변이 고객 memo에 빠짐없이 보이는지 확인
- [ ] 페이지별 자동문자 override 실사용 QA
- [ ] 앱 종료/잠금 상태 문의 알림 QA
- [ ] 대량 문의 동기화 성능
- [ ] 중복 eventId 장기 운영 확인

### 일정/캘린더

- [ ] 시간 휠 오전/오후 12시 변환
- [ ] 시간 휠 55분 경계값
- [ ] 일정 신규/수정 시 저장 시간 일치
- [ ] 캘린더 접은 상태에서 선택 날짜/일정 추가/목록 유지
- [ ] 화면 재진입 후 접힘 상태 유지
- [ ] 월 변경/일정 추가 후 wrapper 중복 생성 방지

## P2 — 안정성/성능/운영

- [ ] 제조사별 배터리 최적화/백그라운드 제한 안내 UX
- [ ] 듀얼 SIM 실기기 QA 및 필요 시 SIM 구분
- [ ] 통화중 다른 전화 수신 QA
- [ ] 고객 수천 건 목록/검색 성능
- [ ] 최근 통화 수천 건 조회 성능
- [ ] CallLog observer/ledger/worker 대량 데이터 성능
- [ ] WorkManager 동기화/recovery 중복 작업 여부
- [ ] 정산/파트너 데이터 로딩 속도 API 병목
- [ ] 내부 진단 화면에서 `trigger → resolve → overlay → notification → recovery` 단계 확인 기능 검토
- [ ] Lead Intake 운영 진단 `receive → verify → map → dedupe → persist → fcm → ack`

## 출시 전 필수

- [ ] Google 로그인 재로그인/세션 유지 QA
- [ ] 이메일 계정과 Google 동일 이메일 중복계정 확인
- [ ] Play 내부테스트 결제·복원 QA
- [ ] 통화 전/후 장시간 반복 QA
- [ ] 블랙/화이트 전체 화면 QA
- [ ] 개인정보처리방침/계정삭제 흐름 최종 검토
- [ ] Play 데이터 보안 설문과 실제 수집 데이터 일치 확인
- [ ] 스토어 스크린샷/아이콘/설명 최종 검토
- [ ] 프로덕션 직전 versionCode 증가 + signed AAB 재생성

## 현재 0.44.45 릴리스 기록

- versionName: `0.44.45`
- versionCode: `2026082101`
- signed build commit: `6458c6be90c8a963c228ccf6e984311069c91e28`
- Signed Release run: `32488755570`
- AAB: `CallTag-v0.44.45-code2026082101.aab`

## 문서 운영 원칙

- 최신 구현 현황: `ANDROID_DEVELOPER_HANDOFF_KO.md`
- 제품 정책: `PRODUCT_SPEC_KO.md`
- 외부 Lead Intake/API: `EXTERNAL_LEAD_INTAKE_API_SPEC_KO.md`
- 결제: `GOOGLE_PLAY_BILLING_SETUP_KO.md`
- 페이지로 연동: `PAGERO_CUSTOMER_INTEGRATION_KO.md`
- 버전별 `V0xxx_*`, `HOTFIX`, 날짜별 인수인계 문서를 새로 쌓지 않는다.
- 변경사항은 위 정본 문서에 누적 갱신한다.
