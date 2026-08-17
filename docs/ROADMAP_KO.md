# 콜태그 개발 로드맵

기준일: **2026-08-17**  
현재 버전: **0.44.42 / versionCode 2026081702**

## 완료된 핵심

- [x] 이메일/Google 로그인
- [x] Google 로그인 실제 단말 성공
- [x] 고객 생성·수정·상태·메모
- [x] 고객 삭제 현재 화면 팝업
- [x] 고객 연락처 저장
- [x] 오늘 할 일 / 확인할 통화
- [x] 캘린더 / 일정
- [x] 일정 시간 선택 휠 UI
- [x] 월간 캘린더 본문 접기/펼치기
- [x] 캘린더 접힘 상태 저장
- [x] 통계
- [x] 더보기 설정 검색/섹션 구조
- [x] 블랙/화이트 앱 테마 선택
- [x] 화이트 모드 Light Material parent 분리
- [x] 통화 감지 foreground service
- [x] 수신 등록고객 정보 표시
- [x] 통화 종료 작은 팝업
- [x] 통화 종료 후 앱 Activity 자동 실행 제거
- [x] 작은 post-call overlay 우선 전달
- [x] overlay 실패 시 알림 fallback
- [x] CallLog 기반 종료 누락 복구
- [x] 중복 통화 처리 ledger
- [x] 알림/오버레이 fallback 불가 시 미전달 recovery queue 유지
- [x] 재부팅/앱 업데이트 후 알림 권한 때문에 통화감지가 꺼지던 문제 수정
- [x] WorkManager 15분 주기 독립 CallLog 복구
- [x] 최근 12시간 + recovery cursor + 5분 grace 재검사
- [x] Worker에서 기존 처리 ledger 재사용하여 중복 후처리 방지
- [x] 앱 시작 시 recovery worker 스케줄 재확인
- [x] 재부팅/앱 업데이트 시 immediate recovery enqueue
- [x] foreground service 시작 실패 시 monitor 설정을 임의 OFF하지 않도록 보강
- [x] 고객선택후 문자
- [x] 통화후 자동문자
- [x] 페이지로 문의접수문자
- [x] 문자 템플릿 / 그룹문자 / 발송내역
- [x] 페이지로 문의 → 콜태그 고객 자동 동기화
- [x] 페이지로 문의 상세 필드 보존 강화
- [x] Google Play Billing Library 연동
- [x] `call_monthly` 실제 서버 검증
- [x] Billing 상품조회와 서버 entitlement 조회 분리
- [x] 다른 계정 purchase token 재귀속 방지
- [x] 계정 전환 시 entitlement 관련 cache 정리
- [x] 기존 Play 업로드 키 검증 CI
- [x] API 36 대응
- [x] 0.44.42 signed AAB/APK 빌드 및 인증서 검증 성공

## 최우선 실기기 QA

- [ ] 0.44.42 통화 종료 작은 오버레이 연속 20회 이상 반복 테스트
- [ ] 통화 종료 후 앱 화면이 자동으로 앞으로 뜨지 않는지 확인
- [ ] 앱 백그라운드 상태 통화 테스트
- [ ] 화면 잠금 상태 통화 테스트
- [ ] 장시간 미사용 후 첫 통화 테스트
- [ ] 부재중/거절/1~3초 짧은 통화 테스트
- [ ] 연속 통화 중복 팝업 여부 확인
- [ ] 연속 통화 중복 자동문자 여부 확인
- [ ] 삼성/픽셀/기타 OEM별 작은 오버레이 실제 노출률 비교
- [ ] 오버레이 권한 OFF + 알림 ON 상태 fallback 확인
- [ ] 오버레이 권한 OFF + 알림 OFF 상태 recovery queue 유지 확인
- [ ] foreground service가 OEM/메모리 정리로 죽은 뒤 15분 WorkManager가 누락 CallLog를 복구하는지 확인
- [ ] 재부팅 후 immediate recovery + 첫 통화 감지 확인
- [ ] 앱 업데이트 직후 immediate recovery + 첫 통화 감지 확인

## 0.44.42 UI 회귀 QA

- [ ] 블랙 테마 전체 화면 텍스트/카드/다이얼로그 가독성 확인
- [ ] 화이트 테마 전체 화면 텍스트/카드/다이얼로그 가독성 확인
- [ ] 화이트 테마 입력창/스위치/하단탭/상단바/다이얼로그에 다크 잔재가 없는지 확인
- [ ] 테마 변경 후 현재 화면/다음 화면 색상 불일치 여부 확인
- [ ] 시간 휠 오전/오후 12시 변환 확인
- [ ] 시간 휠 55분 → 다음 시간 보정 확인
- [ ] 일정 신규 등록/수정 시 선택 시간이 정확히 저장되는지 확인
- [ ] 캘린더 접은 상태에서 날짜/일정 UI가 사라지지 않는지 확인
- [ ] 캘린더 접힘 상태가 화면 재진입 후 유지되는지 확인
- [ ] 캘린더 월 변경/일정 추가 후 접힘 wrapper가 중복 생성되지 않는지 확인

## 통화 안정화 남은 패치/QA

- [x] `CallPopupNotificationManager.showPostCall()`에서 overlay 우선 + 알림 준비상태 검사
- [x] 강제 앱 Activity 자동실행 경로 제거
- [x] 15분 주기 WorkManager 독립 복구
- [x] 앱 시작/재부팅/업데이트 시 worker 재등록
- [x] service start 실패 시 recovery worker가 같이 죽는 경로 차단
- [ ] 제조사별 배터리 최적화/백그라운드 제한 안내 UX 정리
- [ ] Android 명시적 Force stop 이후 사용자가 앱을 재실행했을 때 recovery 재초기화 QA
- [ ] 듀얼 SIM 실기기 QA 및 필요 시 SIM 정보 구분
- [ ] 통화중 다른 전화 수신 QA
- [ ] 최근 통화 대량 데이터에서 CallLog observer/ledger/worker 성능 확인
- [ ] 내부 진단 화면에서 최근 통화의 trigger → resolve → overlay → notification → recovery 단계 확인 기능 검토

## 권한 UX 남은 작업

- [ ] 권한 부족 시 `권한이 없습니다`만 표시하는 화면 전수조사
- [ ] 전화 상태 권한 요청/설정 이동 통일
- [ ] 통화기록 권한 요청/설정 이동 통일
- [ ] 알림 권한 요청/채널 설정 이동 통일
- [ ] 오버레이 권한 요청/설정 이동 통일
- [ ] 연락처 저장 관련 권한/시스템 화면 안내 통일
- [ ] 권한 거부 후 다시 기능을 눌렀을 때 즉시 복구 액션 제공

## 결제 남은 작업

현재 Play 상품:

- `call_monthly` — 1,900원/월
- `message_monthly` — 990원/월

`all_monthly`는 현재 만들지 않는다.

### RTDN / 구독 lifecycle

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

### 결제 실제 계정 QA

- [ ] Play 설치본에서 `call_monthly` 구매/복원 재확인
- [ ] `message_monthly` 구매/복원 재확인
- [ ] 두 상품 동시 이용 표시 확인
- [ ] 결제 취소 후 앱 표시 확인
- [ ] 국가/결제프로필/테스트트랙 조건별 `ITEM_UNAVAILABLE` 재현 확인
- [ ] 구매 완료 직후 UI에 이용 중 상태가 명확히 반영되는지 확인

## 페이지로 연동 후속

- [ ] 문의 메타데이터 업종별 샘플 QA
- [ ] 실제 문의에서 모든 입력 답변이 고객 memo에 빠짐없이 보이는지 확인
- [ ] 페이지별 자동문자 override 실사용 QA
- [ ] 앱 종료/잠금 상태 문의 알림 QA
- [ ] 대량 문의 동기화 성능 확인
- [ ] 중복 eventId 장기 운영 확인

## 고객/CRM 후속

- [ ] 고객목록 삭제/상태 변경/문자 보내기 실제 회귀 QA
- [ ] 고객 연락처 저장 후 중복 연락처 처리 UX 확인
- [ ] 홈에서 고객 상세 진입 경로 통일 확인
- [ ] 홈 `오늘 할 일`이 오늘 일정만 표시하는지 장기 회귀 확인
- [ ] 통화목록 메모 표시와 연락처 이름 변경이 서로 섞이지 않는지 확인
- [ ] 고객 상태/일정 커스텀 값 편집 UX 최종 정리

## 문자 후속

- [ ] 통화 후 자동문자 수신/발신/부재중별 실제 발송 QA
- [ ] 이미지 첨부 문자 실사용 여부/지원 범위 최종 결정
- [ ] 예약·후속문자 1/3/7일 및 직접 지정 QA
- [ ] 중복 발송 방지 1/7/30일/영구 옵션 QA
- [ ] 업무시간 외 발송 제한 QA

## 성능/운영 후속

- [ ] 정산/파트너 데이터 로딩 속도 API 병목 확인
- [ ] 고객 수천 건 환경 목록/검색 성능 확인
- [ ] 최근 통화 수천 건 환경 조회 성능 확인
- [ ] WorkManager 동기화/CallLog recovery 중복 작업 여부 확인
- [ ] CrashTelemetry의 통화 팝업 누락 원인을 관리자/진단 화면에서 볼 수 있게 할지 결정

## 출시 전 필수

- [ ] Google 로그인 재로그인/세션 유지 QA
- [ ] 기존 이메일 계정과 Google 동일 이메일 중복계정 확인
- [ ] Play 내부테스트에서 결제·복원 QA
- [ ] 통화 전/후 기능 장시간 반복 QA
- [ ] 블랙/화이트 테마 전체 화면 QA
- [ ] 개인정보처리방침/계정삭제 흐름 최종 검토
- [ ] Play 데이터 보안 설문과 실제 수집 데이터 일치 확인
- [ ] 스토어 스크린샷/아이콘/설명 최종 검토
- [ ] 프로덕션 배포 직전 versionCode 증가 및 signed AAB 재생성

## 현재 0.44.42 릴리스 기록

- release HEAD: `c4b3eec0b093f52b04f71bfda2468a89fdbc3876`
- recovery safety-net HEAD: `b66aced0be9e4129cd8ce3aceb51b2f52f54e1ca`
- Current Signed Release run: `32038904833`
- artifact: `9291546414`
- AAB: `CallTag-v0.44.42-code2026081702.aab`
- AAB SHA-256: `f6b2b7cba9d606fbcd85ac9bf9ab77ad6685c8e9e6b26e3b848fde3b36f5f9a5`

## 문서 운영 원칙

- 최신 구현 현황은 `ANDROID_DEVELOPER_HANDOFF_KO.md`
- 제품 정책은 `PRODUCT_SPEC_KO.md`
- 결제는 `GOOGLE_PLAY_BILLING_SETUP_KO.md`
- 페이지로 연동은 `PAGERO_CUSTOMER_INTEGRATION_KO.md`
- 버전별 `V0xxx_*`, `HOTFIX`, 날짜별 인수인계 문서를 새로 쌓지 않는다.
- 변경사항은 위 정본 문서에 누적 갱신한다.
