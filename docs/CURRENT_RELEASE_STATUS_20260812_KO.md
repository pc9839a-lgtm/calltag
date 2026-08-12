# 콜태그 최신 릴리스·Google 로그인 상태

기준일: **2026-08-12**  
저장소: `pc9839a-lgtm/calltag`  
현재 작업 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`  
관련 PR: `#80`  
패키지명: `kr.pagero.calltag`

> 이 문서는 2026-08-12 기준 인증·회원가입·Play 배포·앱 아이콘 변경의 최신 정본이다. 기존 `README.md`, `ANDROID_DEVELOPER_HANDOFF_KO.md`, `DEVELOPMENT_STATUS_AND_ROADMAP_KO.md`의 오래된 버전/Google 로그인 설명과 충돌하면 이 문서와 실제 코드를 우선한다.

## 1. 최신 Android 릴리스

- versionName: **0.44.20**
- versionCode: **2026081206**
- minSdk: **26**
- targetSdk / compileSdk: **36**
- applicationId: `kr.pagero.calltag`
- Play 업로드키로 release AAB 서명 빌드 성공
- GitHub Actions workflow: `CallTag 0.44.20 signed Play AAB`
- Run ID: `31549775038`
- Artifact ID: `9123840577`
- Artifact: `calltag-v0.44.20-code2026081206-play-aab`

### Play versionCode 규칙

Google Play Console에 한 번 업로드된 versionCode는 출시를 취소하거나 제거해도 재사용하지 않는다.

현재 사용/소비된 최근 코드:

- `2026081202`
- `2026081203`
- `2026081204`
- `2026081205`
- 최신: `2026081206`

다음 Play 빌드는 **반드시 `2026081207` 이상**을 사용한다.

## 2. Google 로그인 — 현재 정본

### 2.1 사용자 UX

현재 목표 UX는 브라우저 OAuth가 아니다.

```text
콜태그 로그인 화면
→ Google로 계속하기
→ 앱 위에 Google 계정 선택창
→ 계정 선택
→ Google ID Token 발급
→ 콜태그 서버 검증
→ 콜태그 세션 생성
→ 앱 진입
```

**Google 버튼을 눌렀을 때 `pagero.kr` 또는 Chrome/브라우저가 열리면 실패다.**

### 2.2 Android 구현

`LoginActivity.startGoogleLogin()`은 외부 URL을 열지 않는다.

현재 구현:

```java
startActivity(new Intent(this, GoogleCredentialLoginActivity.class));
```

`GoogleCredentialLoginActivity`는 Android Credential Manager를 사용한다.

사용 라이브러리:

- `androidx.credentials:credentials:1.6.0`
- `androidx.credentials:credentials-play-services-auth:1.6.0`
- `com.google.android.libraries.identity.googleid:googleid:1.2.0`

핵심 클래스:

- `CredentialManager`
- `GetSignInWithGoogleOption`
- `GoogleIdTokenCredential`

### 2.3 서버 인증

Android에서 받은 Google ID Token은 다음 운영 API로 전송한다.

```text
POST https://pagero.kr/api/call/google/id-token
```

서버 저장소: `pc9839a-lgtm/inlet`

운영 배포 후 잘못된 토큰을 보내는 smoke test에서 `401 / GOOGLE_ID_TOKEN_INVALID` 응답을 확인했다. 즉 해당 라우트가 실제 `pagero.kr` 운영에 존재하고 토큰 검증 계약이 동작한다.

### 2.4 OAuth Client 구분 — 절대 혼동 금지

#### Android OAuth Client

용도: Google이 실제 Android 앱을 식별한다.

- 유형: `Android`
- 패키지명: `kr.pagero.calltag`
- Client ID: `31346298247-ih26h65v8i4ct5927tqqncqpqu9r7e20.apps.googleusercontent.com`
- SHA-1: **Google Play Console의 `앱 서명 키 인증서` SHA-1**을 등록해야 한다.

Play 내부/비공개/정식 배포 앱에서는 업로드키 SHA-1이 아니라 **Play 앱 서명 키 SHA-1**을 사용한다.

로컬 APK를 별도 서명해 직접 설치하는 경우에는 해당 로컬 서명키 SHA-1을 별도 Android OAuth Client에 등록할 수 있다.

#### Web / Server OAuth Client

용도: Google ID Token의 server client ID / audience.

현재 앱 기본값:

```text
31346298247-o5jfdetjs84mu02c8tp68qg19ifo89en.apps.googleusercontent.com
```

`BuildConfig.GOOGLE_SERVER_CLIENT_ID`로 전달한다.

**Android Client ID를 `serverClientId`로 넣지 않는다.**

### 2.5 이전 웹 OAuth 구조

이전 구현은 다음 흐름이었다.

```text
앱 → /api/call/google/start → accounts.google.com → /api/call/google/callback → calltag://auth/google
```

이 방식은 0.44.20의 Google 버튼 기본 흐름이 아니다.

레거시 callback/ticket 코드는 아직 일부 남아 있을 수 있으나 새 Google 버튼은 이를 호출하지 않아야 한다. 리팩터링 시 기존 이메일 로그인이나 이전 세션 복구에 영향이 없는지 확인한 후 제거한다.

## 3. 0.44.19 문제와 0.44.20 수정

### 0.44.19 / code 2026081205

Credential Manager 관련 클래스와 서버 ID Token API는 추가됐지만 `LoginActivity`의 실제 버튼이 여전히 `Intent.ACTION_VIEW` 기반 경로를 사용했다.

결과:

- 사용자가 `Google로 계속하기`를 눌러도 웹페이지가 열릴 수 있었다.
- 코드 추가와 실제 버튼 연결 완료를 혼동한 릴리스였다.

### 0.44.20 / code 2026081206

수정:

- Google 버튼에서 `GoogleCredentialLoginActivity.class`를 **명시적으로 직접 실행**
- 웹 OAuth URL 진입 제거
- 빌드 검증에서 LoginActivity에 기존 Google 웹 URL 호출이 남아 있으면 실패하도록 계약 추가

현재 CI 빌드는 성공했다.

**단, 0.44.20의 Google 계정 선택창 실제 휴대전화 E2E는 별도 실기기 확인이 필요하다. CI 성공을 실기기 성공으로 기록하지 않는다.**

## 4. 회원가입 UX 최신 기준

회원가입 화면은 장문 설명보다 입력을 우선한다.

### 필수/선택 표시

- 필수 항목: 라벨 뒤 **빨간 `*`**만 사용
- 선택 항목: 별도 `선택` 배지/문구 없음
- `[필수]`, `[선택]`, `필수 정보`, `선택 정보` 같은 텍스트를 입력창마다 반복하지 않음

필수값 현재 기준:

- 이름
- 휴대폰번호
- 이메일
- 이메일 인증번호
- 비밀번호

선택값 예:

- 브랜드/상호
- 업종
- 추천인 코드

### 추천인

- 추천인 코드는 회원가입 시 입력
- 안내는 입력칸 아래 짧은 한 줄 수준으로 유지
- 긴 혜택 설명 카드 반복 금지

### 약관

- 이메일 인증번호 요청 단계에서 약관 체크를 강제하지 않음
- 최종 회원가입 제출 시 필수 약관 동의 검사

## 5. 앱 아이콘 최신 기준

Play 스토어 등록정보 아이콘과 휴대폰에 설치되는 런처 아이콘은 별개다.

설치 아이콘은 Android 앱 리소스와 Manifest에서 지정해야 한다.

현재 Play release workflow는 다음을 사용한다.

- 원본 안전영역 이미지: `app/src/main/res/drawable-nodpi/calltag_launcher_safe.webp`
- Adaptive Icon: `mipmap-anydpi-v26`
- legacy icon: `mipmap`
- Manifest: `android:icon`, `android:roundIcon`

### 아이콘 안전영역 규칙

- 전화기/태그 심볼을 512×512 전체에 꽉 채우지 않는다.
- 삼성/Pixel의 원형·둥근사각형 마스크에서 잘리지 않도록 중앙 안전영역 안에 배치한다.
- 배경과 foreground를 Adaptive Icon 구조로 관리한다.
- Play Console 스토어 아이콘만 교체하고 설치 아이콘 변경 완료라고 기록하지 않는다.

## 6. Play Console 현재 주의사항

### 테스트 트랙

내부 테스트와 비공개 테스트는 별도 트랙이다. 특정 AAB가 어느 트랙에 배포됐는지 버전코드 기준으로 확인한다.

### 제한 권한

현재 앱은 통화기록/SMS와 FGS 관련 Play 선언 검토 대상이 될 수 있다.

Play용 Manifest에서는 `USE_FULL_SCREEN_INTENT`를 사용하지 않는다.

### Google OAuth Android Client SHA-1

Google Play에서 설치되는 앱의 Android OAuth Client에는 다음 경로의 SHA-1을 사용한다.

```text
Play Console
→ 콜태그
→ Google Play로 보호됨
→ Play 스토어 보호
→ Play 앱 서명 관리
→ 앱 서명 키 인증서
→ SHA-1
```

`업로드 키 인증서` SHA-1과 혼동하지 않는다.

## 7. 다음 확인 순서

1. Play Console에 `0.44.20 / 2026081206` 배포
2. 내부 테스터 기기에서 실제 업데이트 버전 확인
3. Google Cloud Android OAuth Client에 `kr.pagero.calltag` + **Play 앱 서명 SHA-1** 등록 확인
4. 앱에서 `Google로 계속하기` 클릭
5. 브라우저가 열리지 않고 앱 위 계정 선택창이 뜨는지 확인
6. 계정 선택 후 콜태그 로그인/신규 계정 생성 확인
7. 기존 동일 이메일 계정의 ownerId 유지 확인
8. 로그인 후 페이지로 연결 상태 확인
9. 회원가입 화면의 필수 `*`, 선택 무표시, 약관 최종 검사 확인
10. 앱서랍/홈화면 런처 아이콘 잘림 여부 확인

## 8. 코드 확인 우선 파일

- `app/build.gradle`
- `app/src/main/java/kr/pagero/calltag/LoginActivity.java`
- `app/src/main/java/kr/pagero/calltag/GoogleCredentialLoginActivity.java`
- `app/src/main/java/kr/pagero/calltag/AuthApiClient.java`
- `app/src/main/AndroidManifest.xml`
- `.github/workflows/calltag-v04420-play-aab.yml`
- 서버: `pc9839a-lgtm/inlet/functions/api/call/google/id-token.js`

## 9. 완료/미완료 구분

### 코드·서버·빌드 완료

- Credential Manager 의존성 추가
- Google 계정 선택 Activity 구현
- Google 버튼 명시적 Activity 연결
- Google ID Token 운영 API 배포
- 잘못된 ID Token 거부 smoke test 성공
- 0.44.20 서명 AAB 빌드 성공
- 회원가입 필수 `*` UX 반영
- 안전영역 Adaptive Icon 빌드 구조 반영

### 실제 단말 확인 필요

- 0.44.20에서 Google 계정 선택창 실제 표시
- Play 앱 서명 SHA-1이 Android OAuth Client에 정확히 등록됐는지 최종 확인
- 계정 선택 → 서버 토큰 검증 → 앱 로그인 E2E
- Google 신규가입자의 필수 프로필 보완 흐름
- 홈/앱서랍 아이콘이 삼성 등 실제 런처에서 잘리지 않는지 확인

이 문서는 위 실기기 결과가 확인될 때마다 갱신한다.
