# 콜태그 최신 릴리스·운영 상태

기준일: **2026-08-12**  
저장소: `pc9839a-lgtm/calltag`  
현재 작업 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`  
관련 PR: `#80`  
패키지명: `kr.pagero.calltag`

> 이 문서는 2026-08-12 현재 릴리스 상태의 정본이다. 과거 버전 문서와 충돌하면 이 문서와 실제 코드를 우선한다.

## 1. 최신 Android 릴리스

- versionName: **0.44.21**
- versionCode: **2026081207**
- minSdk: **26**
- targetSdk / compileSdk: **36**
- applicationId: `kr.pagero.calltag`
- Play 업로드키로 signed release AAB 빌드 및 jarsigner 검증 성공
- GitHub Actions workflow: `CallTag 0.44.21 signed Play AAB`
- 성공 Run ID: `31553364381`
- Artifact ID: `9125103041`
- Artifact: `calltag-v0.44.21-code2026081207-play-aab`
- AAB SHA-256: `e3e71aeb2f67784cc2f1a69df25e4220b2de8fd26537b8032cbba68ba64d6ef5`

### Play versionCode 규칙

Google Play Console에 한 번 업로드된 versionCode는 출시 취소·삭제 여부와 상관없이 재사용하지 않는다.

현재 최신 코드는 `2026081207`이다. **다음 Play 업로드용 빌드는 반드시 `2026081208` 이상을 사용한다.**

## 2. 0.44.21 더보기 메뉴 개편

`앱·계정`에 기능을 몰아넣던 구조를 폐기했다. 더보기의 최상위 진입점은 아래 8개로 고정한다.

1. **계정**
2. **이용권**
3. **문자 관리**
4. **고객 관리**
5. **페이지로**
6. **파트너**
7. **데이터 관리**
8. **앱 정보**

### 계정

계정은 더보기 맨 위에 둔다. 화면에는 계정 식별/관리 정보만 남긴다.

- 이름
- 연락처
- 이메일
- 회원정보 다시 불러오기
- 로그아웃
- 회원탈퇴

브랜드·업종·이용상품·약관·백업 등 다른 성격의 기능은 계정 화면에 섞지 않는다.

### 이용권

상위 메뉴 및 화면 제목은 `이용권·결제`가 아니라 **`이용권`**으로 표시한다.

현재 결제 로직 자체는 이번 메뉴 패치에서 변경하지 않았다. 결제 구현을 이어갈 때는 다음 문서를 먼저 읽는다.

- `docs/CALLTAG_PAYMENT_HANDOFF_20260812_KO.md`

### 문자 관리

`통화 후 자동문자` 대형 독립 카드는 제거했다. 문자 관리 첫 항목으로 통합한다.

- 통화 후 자동문자
- 문자 문구·이미지
- 그룹·단체문자
- 발송 관리

### 고객 관리

- 고객 상태
- 일정 종류
- 통화 후 팝업 제외

### 데이터 관리

- 동기화 상태
- 백업 및 복원

### 앱 정보

- 버전 정보
- 서비스 이용약관
- 개인정보처리방침
- 고객센터

법적 문서 URL:

- 이용약관: `https://call.pagero.kr/terms/`
- 개인정보처리방침: `https://call.pagero.kr/privacy/`

## 3. 고객센터 최신 구조

고객센터는 외부 메일 앱을 여는 `mailto:` 방식이 아니다.

```text
더보기
→ 앱 정보
→ 고객센터
→ 앱 내 문의 작성
→ POST /api/call/support
→ 인증된 콜태그 서버
→ AWS SES
→ roadfor@kakao.com
```

문의 폼:

- 문의 유형: 일반문의 / 결제 / 오류 / 기타
- 이름
- 연락처
- 답변 받을 이메일
- 문의 내용

서버 메일은 고객이 입력한 이메일을 `Reply-To`로 지정한다. 따라서 `roadfor@kakao.com`에서 답장을 누르면 고객 이메일로 회신할 수 있다.

서버 파일:

- `pc9839a-lgtm/inlet/functions/api/call/support.js`

운영 확인:

- Cloudflare Pages 배포 성공
- 인증 없는 요청은 운영 `/api/call/support`에서 401로 차단
- 전용 안전 smoke workflow 성공: Run `31553422904`

**아직 실제 로그인 사용자 문의를 보내 `roadfor@kakao.com` 받은편지함까지 도착하는 E2E 테스트는 하지 않았다.** 실제 단말에서 한 번 접수해 받은편지함까지 확인해야 최종 완료로 기록한다.

## 4. Google 로그인 — 유지된 현재 정본

0.44.21은 0.44.20의 Credential Manager Google 로그인 수정사항을 그대로 유지한다.

```text
콜태그 로그인 화면
→ Google로 계속하기
→ GoogleCredentialLoginActivity 직접 실행
→ 앱 위 Google 계정 선택창
→ Google ID Token
→ POST /api/call/google/id-token
→ 서버 검증
→ 콜태그 세션 생성
```

Google 버튼은 브라우저 OAuth URL을 열지 않아야 한다.

### OAuth Client 구분

Android OAuth Client:

- 유형: Android
- package: `kr.pagero.calltag`
- Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- SHA-1: **Play Console 앱 서명 키 인증서 SHA-1**

Web / server client ID:

`31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com`

이 Web client ID가 Credential Manager의 server client ID / ID Token audience로 사용된다. Android Client ID로 교체하지 않는다.

## 5. 회원가입 UX 유지 기준

- 필수 항목은 라벨 뒤 빨간 `*`만 표시
- 선택 항목은 `[선택]` 등의 반복 표기 없음
- 이름 / 휴대폰번호 / 이메일 / 인증번호 / 비밀번호 필수
- 브랜드/상호, 업종, 추천인 코드는 선택
- 추천인 코드는 회원가입 시에만 입력
- 이메일 인증 단계에서 약관을 선행 강제하지 않음
- 최종 가입 제출 시 필수 약관 확인

## 6. 앱 아이콘 유지 기준

- 안전영역 원본: `app/src/main/res/drawable-nodpi/calltag_launcher_safe.webp`
- Adaptive Icon과 legacy icon을 모두 생성
- Play 스토어 아이콘과 설치 런처 아이콘을 구분
- 삼성/Pixel 마스크에서 전화기/태그 심볼이 잘리지 않아야 함

## 7. 이번 패치 핵심 파일

Android:

- `app/src/main/java/kr/pagero/calltag/MoreSettingsHubView.java`
- `app/src/main/java/kr/pagero/calltag/SettingsGroupActivity.java`
- `app/src/main/java/kr/pagero/calltag/AppInfoActivity.java`
- `app/src/main/java/kr/pagero/calltag/CustomerSupportActivity.java`
- `app/src/main/java/kr/pagero/calltag/SupportApiClient.java`
- `app/src/main/java/kr/pagero/calltag/AccountActivity.java`
- `app/src/main/java/kr/pagero/calltag/BillingEntitlementActivity.java`
- `app/src/main/res/layout/activity_account.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
- `.github/workflows/calltag-v04421-play-aab.yml`

Server:

- `pc9839a-lgtm/inlet/functions/api/call/support.js`
- `pc9839a-lgtm/inlet/.github/workflows/calltag-support-live-smoke.yml`

## 8. 실기기 확인 항목

1. `0.44.21 / 2026081207` 설치 확인
2. 더보기 최상단이 `계정`인지 확인
3. 더보기에 정확히 8개 목적 메뉴가 보이는지 확인
4. 통화 후 자동문자가 문자 관리 안에 들어갔는지 확인
5. 계정에 이름/연락처/이메일과 로그아웃/회원탈퇴만 필요한 수준으로 남았는지 확인
6. 앱 정보에서 버전/이용약관/개인정보처리방침/고객센터 진입 확인
7. 고객센터 문의 1건 실제 전송 후 `roadfor@kakao.com` 수신 확인
8. 해당 메일에서 답장 시 고객 입력 이메일이 Reply-To로 잡히는지 확인
9. Google 계정 선택창 E2E 재확인
10. 런처 아이콘 잘림 여부 확인

위 단말 확인 전까지 CI 성공을 단말 UX 최종 성공으로 기록하지 않는다.
