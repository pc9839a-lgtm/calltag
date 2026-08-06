# 콜태그 이용권·결제·추천인 앱 구현 명세

- 작성일: 2026-08-04
- 기준 브랜치: `agent/calltag-foundation`
- 서버 정본: `pc9839a-lgtm/inlet/docs/CALLTAG_PAGERO_UNIFIED_BILLING_REFERRAL_ARCHITECTURE_KO.md`
- 현재 앱 버전: `0.42.3` / versionCode `63`

## 제품 경계

콜태그와 페이지로는 연결되는 별도 서비스다.

- 콜태그는 전화관리·문자자동화 중심 Android 앱이다.
- 페이지로는 랜딩페이지 제작·문의 수집 웹서비스다.
- 페이지로 문의를 콜태그 고객으로 가져오는 기능은 선택 연동이다.
- 페이지로 단독 사용자는 콜태그 앱을 설치하지 않아도 된다.

## 판매 상품

| 상품 코드 | 상품명 | 월 이용료 | 권한 |
|---|---:|---:|---|
| `call_monthly` | 전화관리 | 1,900원 | 전화관리 기능 |
| `message_monthly` | 문자자동화 | 990원 | 문자자동화 기능 |
| `all_monthly` | 통합권 | 6,000원 | 전화관리·문자자동화·페이지로 |

페이지로 단독 상품은 페이지로 웹에서 결제한다. 웹 구독과 Google Play 구독은 서버 이용권을 기준으로 중복 결제를 차단한다.

## 무료 이용

- 기본 3일
- 추천인 코드 최초 등록 시 +5일
- 계정 최대 8일
- 종료 후 자동 결제 없음
- 서버 시각 기준으로 종료 판정
- 기기 날짜 변경으로 무료기간 연장 불가

만료 후에도 고객·상담·메모·일정·템플릿·발송기록은 삭제하지 않는다. 신규 통화 정리와 문자 자동화 실행만 제한한다.

## 더보기

앱·계정 영역:

1. 계정 및 개인정보
2. 이용권·결제
3. 친구 초대·파트너
4. 페이지로 문의 연결
5. 백업 및 복원

## 이용권·결제

표시 항목:

- 현재 이용 상태
- 사용 중 상품
- 무료 또는 유료 상태
- 남은 무료기간
- 다음 결제일
- 결제 경로
- Google Play 준비 상태

Google Play Console 미등록 상태에서는 상품 안내만 표시하고 버튼은 `출시 준비 중`으로 비활성화한다.

결제 활성화 조건:

1. Play Console 앱 등록
2. `call_monthly`, `message_monthly`, `all_monthly` 등록
3. 기본 요금제와 판매 국가 설정
4. 서비스 계정 API 연결
5. 라이선스 테스터 검증
6. `GOOGLE_PLAY_PRODUCTS_READY=1`
7. `GOOGLE_PLAY_BILLING_ENABLED=1`

## 추천인

- 추천코드 조회·복사·공유
- 공유 URL `https://pagero.kr/r/{추천코드}`
- 앱 링크 `calltag://referral?code={추천코드}`
- 로그인 전 최대 30일 보관
- 로그인 후 자동 등록
- 본인 추천 금지
- 한 계정 한 번
- 첫 유료 결제 이후 등록 금지
- 수동 입력 fallback 유지

파트너센터 모바일 웹은 현재 제외한다. 앱에는 추천 회원 수, 유료 회원 수, 예상 수익, 확정 수익 요약만 표시한다.

## 서버 API

### 이용권·결제

- `GET /api/billing/entitlements`
- `GET /api/billing/subscriptions`
- `GET /api/billing/readiness`
- `POST /api/billing/web/precheck`
- `POST /api/billing/google/verify`
- `POST /api/billing/google/restore`

### 추천

- `GET /api/referrals/me`
- `POST /api/referrals/apply`
- `GET /api/referrals/summary`

## 기능별 권한

- 전화관리: `call_monthly` 또는 `all_monthly`
- 문자자동화: `message_monthly` 또는 `all_monthly`
- 실제 SMS 전송 직전에도 이용권 재검사
- 서버 확인 전에는 결제를 강행하지 않음
- 구매 성공만으로 기능을 열지 않고 서버 검증 후 반영

## 통화 종료 팝업 신뢰성

v0.42.3에서 전화 종료 후 큰 정리 화면의 간헐적 누락을 보완했다.

직접 원인:

- 새 통화기록 저장 전 이전 통화기록이 조회되면 재시도가 중단됨
- `PendingIntent.send()` 성공을 실제 화면 표시 성공으로 잘못 간주함
- 제조사 전화 앱의 통화기록 저장 지연을 충분히 추적하지 못함

패치:

- 이전 통화기록이나 이미 처리한 ID가 조회돼도 계속 재시도
- CallLog ContentObserver로 신규 기록 저장 즉시 재조회
- 재시도 구간 최대 약 49초로 확대
- PostCallActivity 실제 생성·시작·재개를 표시 성공 기준으로 사용
- 첫 실행이 막히면 한 번 더 실행
- 실제 표시가 계속 확인되지 않으면 큰 정리 알림 fallback
- 이전 통화 팝업이 남아 있으면 닫고 새 통화 화면 실행

상세 문서:

- `docs/V0423_POST_CALL_POPUP_RELIABILITY_KO.md`

## 로컬 저장 제한

로컬에는 화면 표시와 동작 복구를 위한 최소 정보만 저장한다.

- 이용권 상태
- 서버 확인 시각
- 무료기간 종료일
- 추천코드 pending 값
- 통화기록 ID와 팝업 표시 확인 시각

저장 금지:

- Google Play 구매 토큰 원문 장기 저장
- 파트너 계좌·세금정보
- 다른 회원 개인정보

## 운영 안전

- Draft PR에서 `main` 병합 금지
- 운영 D1 migration 자동 실행 금지
- Google Play release flag 자동 활성화 금지
- 고객·상담·문자·일정 데이터 초기화 금지
- 페이지로 기존 랜딩·편집기 기능 변경 금지
