# 콜태그 개발 로드맵

기준일: **2026-08-14**  
현재 버전: **0.44.38 / versionCode 2026081224**

## 완료된 핵심

- [x] 이메일/Google 로그인
- [x] Google 로그인 실제 단말 성공
- [x] 고객 생성·수정·상태·메모
- [x] 고객 삭제 현재 화면 팝업
- [x] 고객 연락처 저장
- [x] 오늘 할 일 / 확인할 통화
- [x] 캘린더 / 일정
- [x] 통계
- [x] 통화 감지 foreground service
- [x] 수신 등록고객 정보 표시
- [x] 통화 종료 작은 팝업
- [x] CallLog 기반 종료 누락 복구
- [x] 중복 통화 처리 ledger
- [x] 고객선택후 문자
- [x] 통화후 자동문자
- [x] 페이지로 문의접수문자
- [x] 문자 템플릿 / 그룹문자 / 발송내역
- [x] 페이지로 문의 → 콜태그 고객 자동 동기화
- [x] 페이지로 문의 상세 필드 보존 강화
- [x] Google Play Billing Library 연동
- [x] `call_monthly` 실제 서버 검증
- [x] Billing 상품조회와 서버 entitlement 조회 분리
- [x] 기존 Play 업로드 키 검증 CI
- [x] API 36 대응

## 최우선 QA

- [ ] 0.44.38 수신 고객정보 연속 반복 테스트
- [ ] 0.44.38 통화 종료 팝업 연속 반복 테스트
- [ ] 앱 백그라운드 상태 통화 테스트
- [ ] 화면 잠금 상태 통화 테스트
- [ ] 장시간 미사용 후 첫 통화 테스트
- [ ] 부재중/거절/짧은 통화 테스트
- [ ] 연속 통화 중복 팝업 여부 확인
- [ ] 고객 삭제/연락처 저장 UI 최종 확인
- [ ] 페이지로 문의접수 자동문자 실사용 확인
- [ ] 결제 화면 속도 실제 Play 설치본 확인
- [ ] 결제 국가/프로필 오류 계정별 확인

## 결제 남은 작업

현재 Play 상품:

- `call_monthly` — 1,900원/월
- `message_monthly` — 990원/월

`all_monthly`는 현재 만들지 않는다.

남은 서버 lifecycle:

- [ ] Pub/Sub topic 구성
- [ ] Google Play RTDN 설정
- [ ] subscriber endpoint
- [ ] 갱신 이벤트 처리
- [ ] 취소 이벤트 처리
- [ ] 만료 이벤트 처리
- [ ] grace/account hold 처리
- [ ] resume/refund/revoke 처리
- [ ] 이벤트 수신 후 Developer API 재검증
- [ ] entitlement 자동 갱신

## 통화 안정화 후속

- [ ] 제조사별 배터리 최적화 영향 확인
- [ ] 재부팅 직후 foreground service 복구 확인
- [ ] 듀얼 SIM 실기기 QA
- [ ] 통화중 다른 전화 수신 QA
- [ ] 최근 통화 대량 데이터 성능 확인
- [ ] 내부 진단 화면에서 최근 통화 트리거 단계 확인 기능 검토

## 페이지로 연동 후속

- [ ] 문의 메타데이터 업종별 샘플 QA
- [ ] 페이지별 자동문자 override 실사용 QA
- [ ] 앱 종료/잠금 상태 문의 알림 QA
- [ ] 대량 문의 동기화 성능 확인
- [ ] 중복 eventId 장기 운영 확인

## 출시 전 필수

- [ ] Google 로그인 재로그인/세션 유지 QA
- [ ] 기존 이메일 계정과 Google 동일 이메일 중복계정 확인
- [ ] Play 내부테스트에서 결제·복원 QA
- [ ] 통화 전/후 기능 장시간 반복 QA
- [ ] 개인정보처리방침/계정삭제 흐름 최종 검토
- [ ] Play 데이터 보안 설문과 실제 수집 데이터 일치 확인
- [ ] 스토어 스크린샷/아이콘/설명 최종 검토

## 문서 운영 원칙

- 최신 구현 현황은 `ANDROID_DEVELOPER_HANDOFF_KO.md`
- 제품 정책은 `PRODUCT_SPEC_KO.md`
- 결제는 `GOOGLE_PLAY_BILLING_SETUP_KO.md`
- 페이지로 연동은 `PAGERO_CUSTOMER_INTEGRATION_KO.md`
- 버전별 `V0xxx_*`, `HOTFIX`, 날짜별 인수인계 문서를 새로 쌓지 않는다.
- 변경사항은 위 정본 문서에 누적 갱신한다.
