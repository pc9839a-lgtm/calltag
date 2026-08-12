# 콜태그 최신 릴리스·운영 상태

기준일: **2026-08-12**  
저장소: `pc9839a-lgtm/calltag`  
현재 작업 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`  
관련 PR: `#80`  
패키지명: `kr.pagero.calltag`

> 다음 작업자는 먼저 `docs/NEXT_AI_HANDOFF_20260812_KO.md`를 읽는다. 이 문서는 현재 릴리스/QA 상태를 기록한다.

## 1. 최신 Android 릴리스

- versionName: **0.44.22**
- versionCode: **2026081208**
- minSdk: **26**
- targetSdk / compileSdk: **36**
- applicationId: `kr.pagero.calltag`
- Play 업로드키 signed release AAB 빌드 및 검증 성공
- GitHub Actions workflow: `CallTag 0.44.22 signed Play AAB`
- 성공 Run ID: `31557329238`
- Artifact ID: `9126476904`
- Artifact: `calltag-v0.44.22-code2026081208-play-aab`

### Play versionCode 규칙

Play Console에 한 번 업로드된 versionCode는 재사용하지 않는다.

`2026081208`을 Play에 업로드했다면 **다음은 2026081209 이상**을 사용한다.

---

## 2. 다음 패치 최우선: Google Play 결제 실제 연결/E2E

Google Play Billing은 앱과 서버에 이미 구현되어 있다.

Android:

- Billing Library `9.1.0`
- ProductDetails 조회
- SUBS 구매 플로우
- pending purchase
- purchaseToken 서버 전송
- 구매 복원
- Google Play 구독 관리
- entitlement 기반 권한
- Web ↔ Play 중복결제 차단

서버:

- `/api/billing/entitlements`
- `/api/billing/google/verify`
- `/api/billing/google/restore`
- Android Publisher API 검증
- server acknowledgement
- subscription 저장
- partner commission 기록

**사용자가 다음 패치에서 Play Console ↔ Google Cloud/API access 연결을 진행하기로 확정했다.** 현재 연결 완료로 기록하지 않는다.

다음 순서:

1. Play Console ↔ Google Cloud/API access 연결
2. service account 권한 연결
3. 서버 Publisher API credential 설정
4. Play subscription product/base plan과 앱 productId 대조
5. 라이선스 테스터 실제 결제
6. purchaseToken 검증 → acknowledge → entitlement active
7. 앱 재시작/재설치 구매 복원
8. Web ↔ Play 중복결제 양방향 검증
9. RTDN/Pub/Sub 구축

현재 코드 productId:

- `all_monthly`
- `call_monthly`
- `message_monthly`

Play 공개 전 실제 상품/가격과 다시 대조한다.

---

## 3. Google 로그인 — 0.44.22

정상 구조:

```text
Google로 계속하기
→ GoogleCredentialLoginActivity
→ Android Credential Manager
→ Google ID Token
→ POST /api/call/google/id-token
→ 서버 검증
→ CallTag session
```

0.44.22 수정:

- `GetGoogleIdOption`
- `setFilterByAuthorizedAccounts(false)`
- `setAutoSelectEnabled(false)`
- Web/server client ID를 serverClientId로 사용
- callback main executor
- provider timeout 30초
- server exchange timeout 20초
- Credential Activity `exported=false`
- `calltag://credential/google` 딥링크 제거

Android OAuth Client:

- package: `kr.pagero.calltag`
- Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- SHA-1: Play 앱 서명 키 인증서 SHA-1

Web/server Client ID:

- `31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`

**0.44.22 실제 계정 선택 → 세션 생성 E2E는 아직 단말 재검증 필요.**

---

## 4. 더보기 — 0.44.22

상위 진입점 8개:

1. 계정
2. 이용권
3. 문자 관리
4. 고객 관리
5. 페이지로
6. 파트너
7. 데이터 관리
8. 앱 정보

그룹:

- 내 정보: 계정 / 이용권
- 업무 관리: 문자 관리 / 고객 관리
- 서비스: 페이지로 / 파트너
- 앱 관리: 데이터 관리 / 앱 정보

0.44.22 UI:

- 각 메뉴 독립 카드
- 메뉴 높이 64dp
- 메뉴 사이 12dp
- 섹션 사이 34dp

`통화 후 자동문자`는 더보기 별도 대형 카드로 만들지 않고 `문자 관리` 안에 둔다.

계정 화면은 이름/연락처/이메일, 다시 불러오기, 로그아웃, 회원탈퇴 중심으로 유지한다.

---

## 5. 앱 아이콘 — 0.44.22

0.44.21의 깨진 bitmap/WebP foreground 방식은 폐기했다.

0.44.22:

- vector launcher foreground
- Adaptive Icon
- release manifest icon 고정
- `calltag_launcher_safe.webp`가 release AAB에 포함되면 CI 실패

실제 런처와 Credential Manager 계정 선택창의 작은 CallTag 아이콘은 단말에서 재확인한다.

---

## 6. 고객센터

`더보기 → 앱 정보 → 고객센터`

```text
앱 문의 폼
→ POST /api/call/support
→ 인증된 서버
→ AWS SES
→ roadfor@kakao.com
```

고객 이메일을 Reply-To로 사용한다.

완료:

- 서버 route 배포
- 인증 없는 요청 401 smoke 통과

남음:

- 로그인 사용자 실제 문의 전송
- `roadfor@kakao.com` 수신 확인
- Reply-To 확인

---

## 7. 무료기간/추천인

CallTag 현재 정책:

- 일반 가입 7일 무료
- 가입 시 추천인 코드 입력 +7일
- 최대 14일
- 무료 종료 후 자동결제 없음
- 추천인 코드는 회원가입 시에만 입력

서버 legacy generic 코드의 3일/+5일 값과 혼동하지 않는다.

---

## 8. 회원가입 UX 고정

- 필수 항목만 라벨 뒤 빨간 `*`
- 선택 항목 `[선택]` 반복 금지
- 이메일 인증 요청 단계에서 약관 선행 강제 금지
- 최종 가입 제출 시 필수 약관 확인

---

## 9. 다음 단말 QA 우선순위

1. Play Console/Google Cloud 결제 연결 후 실제 라이선스 테스트 결제
2. purchaseToken 서버 검증/acknowledge/entitlement 확인
3. 구매 복원
4. Web ↔ Play 중복결제 확인
5. Google 로그인 계정 선택 후 세션 생성
6. 더보기 카드 간격 확인
7. 런처/Google 계정선택창 아이콘 확인
8. 고객센터 실제 메일 수신
9. 통화 종료 후 작은 팝업 1개만 표시 확인

CI 성공을 실기기 성공으로 기록하지 않는다.
