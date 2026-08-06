# CallTag Google 로그인 외부 작업 인계서

최종 갱신: 2026-08-05

이 문서는 CallTag Google 로그인만 별도 작업하는 개발자 또는 AI에게 전달하기 위한 독립 인계서다. 고객 데이터 동기화, Google Play 결제, 웹 결제 구현은 이 문서의 범위가 아니다.

## 1. 프로젝트 정보

- Android 저장소: `pc9839a-lgtm/calltag`
- Android 작업 브랜치: `docs/billing-referral-app-handoff-20260804`
- Android package/applicationId: `kr.pagero.calltag`
- 백엔드 저장소: `pc9839a-lgtm/inlet`
- 운영 API origin: `https://pagero.kr`
- 기존 로그인: 이메일·비밀번호 + 이메일 인증
- 앱 세션 저장: `AuthSessionStore`
- Android API client: `AuthApiClient`
- 서버 인증 공통 코드: `inlet/functions/api/auth/_shared.js`
- 서버 계정 DB: D1 `accounts`

Google 로그인은 기존 이메일 로그인과 병행한다. Google ID Token을 앱 세션으로 직접 사용하지 않고, 서버 검증 완료 후 기존 CallTag 세션을 새로 발급해야 한다.

## 2. 구현 목표

1. Android에서 Credential Manager 기반 `Google 계정으로 계속하기` 제공
2. 앱이 받은 Google ID Token을 HTTPS로 서버에 전달
3. 서버에서 Google 서명과 claims를 검증
4. Google `sub`를 외부 계정의 고유 식별자로 저장
5. 검증 후 기존 형식의 CallTag session 발급
6. 동일 이메일 계정이 있어도 이메일 문자열만으로 자동 병합하지 않음
7. 정지·탈퇴·미인증 정책을 기존 계정 정책과 동일하게 적용
8. 로그아웃 시 CallTag 세션 삭제와 Credential Manager 상태 초기화

## 3. Google Cloud에서 준비할 값

### 3.1 Google Auth Platform

하나의 Google Cloud 프로젝트에서 다음을 설정한다.

- 앱 이름: `콜태그` 또는 스토어에 등록할 최종 명칭
- 사용자 지원 이메일
- 개발자 연락처 이메일
- 홈페이지: `https://calltag.pagero.kr` 또는 최종 서비스 URL
- 개인정보처리방침 URL
- 이용약관 URL
- 승인된 도메인: `pagero.kr`

기본 로그인만 구현하므로 Drive, Gmail, 연락처 등 추가 OAuth scope를 요청하지 않는다. 기본 프로필 범위만 사용한다.

### 3.2 Web OAuth Client

백엔드용 Web application OAuth Client를 생성한다.

- 이름 예시: `CallTag Backend`
- 결과값: `WEB_CLIENT_ID`
- Android의 `setServerClientId()`에 이 Web Client ID를 넣는다.
- 서버의 ID Token `aud` 허용값에도 같은 Web Client ID를 넣는다.
- ID Token 검증만 할 때 Web Client Secret은 Android에 필요하지 않다.
- Client Secret이 생성되더라도 앱·저장소·문서에 넣지 않는다.

기록할 값:

```text
GOOGLE_OAUTH_WEB_CLIENT_ID=
```

### 3.3 Android OAuth Clients

OAuth Android client는 package name과 SHA-1 인증서 지문 조합으로 각각 만든다.

공통 package name:

```text
kr.pagero.calltag
```

최소 등록 대상:

1. 로컬 debug keystore SHA-1
2. 직접 배포용 release/upload key SHA-1
3. Google Play App Signing 활성화 후 Play 앱 서명키 SHA-1

Play Console 등록 전에는 1·2를 준비하고, Play 앱 생성 후 3을 반드시 추가한다. SHA-256 지문도 별도로 기록해 향후 앱 링크·개발자 검증·무결성 설정에 사용한다.

기록 표:

| 구분 | SHA-1 | SHA-256 | OAuth Client ID |
|---|---|---|---|
| Debug |  |  |  |
| Upload/Release |  |  |  |
| Play App Signing | 등록 후 작성 | 등록 후 작성 | 등록 후 작성 |

주의: 동일 package name과 SHA-1 조합은 다른 Google Cloud/Firebase 프로젝트에 중복 등록할 수 없다. 중복 오류가 나면 기존 프로젝트의 OAuth client부터 찾는다.

## 4. Android 구현 기준

### 4.1 방식

- 최신 Android 권장 방식인 Credential Manager의 Sign in with Google을 사용한다.
- 구형 `GoogleSignInClient`, One Tap 신규 구현은 사용하지 않는다.
- 라이브러리는 `latest` 또는 동적 버전으로 두지 않고 구현 시점에 검증한 정확한 버전을 고정한다.
- 현재 Google 공식 문서는 Credential Manager, Play Services Auth, `googleid` 의존성 사용을 안내한다.

예상 의존성 형태:

```groovy
dependencies {
    implementation "androidx.credentials:credentials:<verified-version>"
    implementation "androidx.credentials:credentials-play-services-auth:<verified-version>"
    implementation "com.google.android.libraries.identity.googleid:googleid:<verified-version>"
}
```

CallTag는 Java 기반이므로 Java API로 구현한다.

### 4.2 UX

로그인 화면에 다음 두 흐름을 둔다.

1. 앱 진입 시 기존에 승인된 Google 계정 대상 Credential Manager bottom sheet
2. bottom sheet를 닫았거나 계정이 없을 때 항상 보이는 `Google 계정으로 계속하기` 버튼

이메일·비밀번호 로그인은 제거하지 않는다.

### 4.3 요청 옵션

- `setServerClientId(WEB_CLIENT_ID)` 사용
- 첫 시도: 승인된 계정만 표시
- 계정이 없거나 `NoCredentialException`이면 버튼 흐름에서 모든 계정 표시
- 자동 선택은 사용자가 혼동하지 않도록 초기 출시에서는 꺼도 된다.
- 가능한 경우 서버에서 발급한 1회용 nonce를 `setNonce()`에 넣어 재사용 공격을 줄인다.

권장 서버 흐름:

```text
GET  /api/auth/google/nonce
POST /api/auth/google
```

nonce API를 이번 작업에서 구현하지 않는 경우에도 ID Token 자체의 서명·aud·iss·exp 검증은 반드시 수행한다.

### 4.4 Credential 응답 처리

- 응답 credential이 `CustomCredential`인지 확인
- type이 `GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL`인지 확인
- `GoogleIdTokenCredential.createFrom()`으로 파싱
- `getIdToken()` 결과만 서버로 전달
- 앱에서 ID Token payload를 디코딩한 결과를 신뢰하지 않음
- ID Token, 이메일, Google `sub`를 Logcat에 출력하지 않음

서버 요청 예시:

```json
POST /api/auth/google
{
  "idToken": "<google-id-token>",
  "nonce": "<server-issued-nonce-if-used>",
  "linkIntent": false
}
```

### 4.5 성공 처리

서버 응답은 기존 로그인과 같은 형태를 유지한다.

```json
{
  "ok": true,
  "session": "<calltag-session>",
  "profile": {
    "ownerId": "...",
    "name": "...",
    "email": "...",
    "phone": "..."
  },
  "entitlement": {
    "active": true,
    "status": "trial"
  },
  "isNewAccount": false,
  "provider": "google"
}
```

앱은 응답을 `AuthSessionStore.save()`로 저장하고 기존 로그인 완료 후 흐름을 그대로 실행한다.

### 4.6 로그아웃

로그아웃 시:

1. `AuthSessionStore.clear()`
2. 서버 세션 폐기 API가 있으면 호출
3. Credential Manager의 `clearCredentialState()` 호출
4. 이용권·추천 캐시 중 계정 종속 캐시 초기화
5. 로컬 고객 데이터 삭제 여부는 Google 로그아웃 기능과 섞지 말고 별도 정책으로 처리

## 5. 서버 구현 기준

### 5.1 환경변수

기본 비활성으로 시작한다.

```text
GOOGLE_LOGIN_ENABLED=0
GOOGLE_OAUTH_WEB_CLIENT_ID=
GOOGLE_OAUTH_ALLOWED_CLIENT_IDS=
```

- `GOOGLE_OAUTH_ALLOWED_CLIENT_IDS`는 여러 Web Client ID가 필요한 경우 쉼표 구분 allowlist로 사용한다.
- Client ID는 비밀키는 아니지만 운영 환경 설정으로 관리한다.
- Google ID Token 원문은 저장하지 않는다.

### 5.2 신규 D1 테이블

권장 migration:

```sql
CREATE TABLE IF NOT EXISTS auth_external_identities (
  provider TEXT NOT NULL,
  subject TEXT NOT NULL,
  owner_id TEXT NOT NULL,
  email_at_link TEXT NOT NULL DEFAULT '',
  linked_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  disabled_at TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (provider, subject),
  UNIQUE (owner_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_auth_external_identities_owner
ON auth_external_identities(owner_id, provider);
```

- `provider`: `google`
- `subject`: 검증된 ID Token의 `sub`
- 계정 조회 기본키는 이메일이 아니라 `(provider, subject)`다.
- Google `sub`는 로그에 남기지 않거나 HMAC 처리한다.

### 5.3 Google ID Token 검증

Node 서버에서는 공식 `google-auth-library`의 `OAuth2Client.verifyIdToken()` 사용을 우선한다.

검증 항목:

- Google 공개키 기반 JWT 서명
- `iss`가 `accounts.google.com` 또는 `https://accounts.google.com`
- `aud`가 서버 allowlist의 Web Client ID
- `exp`가 현재보다 이후
- `iat`가 비정상적으로 미래가 아님
- `sub` 존재
- `email_verified === true`
- `email` 형식 정상
- `azp`가 있을 경우 승인된 client ID인지 확인
- nonce를 사용한 경우 토큰 nonce와 서버 저장 nonce 일치 및 1회 소비

`tokeninfo` endpoint는 개발 중 디버깅에만 사용할 수 있고 운영 요청마다 호출하는 검증 방식으로 사용하지 않는다.

### 5.4 Endpoint

```text
POST /api/auth/google
Content-Type: application/json
```

허용 body:

```json
{
  "idToken": "string",
  "nonce": "string optional",
  "linkIntent": false
}
```

제한:

- body 최대 32KB
- IP·기기·계정 후보별 rate limit
- 실패 응답에서 Google 내부 검증 상세정보 노출 금지
- ID Token과 claims 원문 로그 금지

권장 오류 코드:

```text
GOOGLE_LOGIN_NOT_ENABLED
GOOGLE_ID_TOKEN_REQUIRED
GOOGLE_ID_TOKEN_INVALID
GOOGLE_ID_TOKEN_AUDIENCE_INVALID
GOOGLE_EMAIL_NOT_VERIFIED
GOOGLE_NONCE_INVALID
GOOGLE_IDENTITY_DISABLED
GOOGLE_ACCOUNT_LINK_REQUIRED
GOOGLE_IDENTITY_ALREADY_LINKED
AUTH_ACCOUNT_SUSPENDED
AUTH_ACCOUNT_DELETED
```

### 5.5 계정 생성

외부 identity가 없고 동일 이메일 계정도 없으면:

1. 신규 `accounts` 생성
2. Google 이메일은 검증 완료 상태로 저장
3. `auth_external_identities` 연결
4. 기존 정책대로 무료 이용기간·추천코드 초기화
5. CallTag session 발급

이름은 Google profile name을 초기값으로 사용할 수 있지만 사용자가 앱에서 수정 가능해야 한다.

### 5.6 기존 계정 연결 정책

이메일 문자열 일치만으로 Google identity를 기존 계정에 자동 연결하지 않는다.

#### 사용자가 기존 CallTag 세션으로 로그인한 상태

- 설정의 `Google 계정 연결`에서 실행
- 현재 계정 재인증 또는 최근 인증 상태 확인
- 검증된 Google identity를 현재 owner에 연결

#### 로그아웃 상태에서 Google 이메일과 동일한 기존 계정 발견

서버는 바로 병합하지 않고 다음을 반환한다.

```json
{
  "ok": false,
  "code": "GOOGLE_ACCOUNT_LINK_REQUIRED",
  "maskedEmail": "k***@example.com",
  "linkMethods": ["password", "email_verification"]
}
```

사용자가 기존 비밀번호 또는 이메일 인증을 완료한 뒤 identity를 연결한다.

#### 금지

- 클라이언트가 전달한 email로 account 결정
- Google profile email만 보고 기존 owner에 즉시 연결
- 하나의 Google subject를 여러 owner에 연결
- 하나의 owner에 여러 Google subject를 무제한 연결

### 5.7 세션

- Google ID Token은 로그인 확인용 1회성 자격증명이다.
- 검증 후 기존 `createSessionToken()`으로 CallTag session을 발급한다.
- 앱의 이후 API는 Google ID Token이 아니라 `X-Inlet-Session`을 사용한다.
- 비밀번호 변경·계정 정지·탈퇴 시 기존 세션 폐기 정책을 Google 로그인 세션에도 동일 적용한다.

## 6. 보안 로그

기록 가능:

- 로그인 성공·실패 시각
- 익명화된 owner/device/IP 식별자
- provider=`google`
- 결과 코드
- 신규 계정 여부

기록 금지:

- Google ID Token
- access token·authorization code
- Google `sub` 원문
- 이메일 원문
- 이름·프로필 사진 URL
- CallTag session 원문

## 7. 테스트 매트릭스

### Android

- 승인된 Google 계정 1개
- 승인되지 않은 계정만 존재
- 기기에 Google 계정 없음
- bottom sheet 닫기 후 버튼 재시도
- 여러 Google 계정 선택
- 네트워크 끊김
- Credential parsing 실패
- 서버 401·403·429·503
- 로그아웃 후 다른 Google 계정 선택

### 서명 인증서

- Debug APK
- Release/upload key APK
- Play Internal Testing 설치본
- Play App Signing certificate 등록 전·후

### 서버 토큰 검증

- 정상 token
- 만료 token
- 잘못된 issuer
- 다른 audience
- 변조된 signature
- `email_verified=false`
- subject 없음
- nonce 불일치·재사용
- rate limit 초과

### 계정 정책

- 완전 신규 Google 계정
- 기존 이메일 계정과 동일한 Google 이메일
- 현재 로그인 계정에 Google 연결
- 이미 다른 owner에 연결된 Google 계정
- 정지 계정
- 탈퇴 보관기간 계정
- Google identity 연결 해제 후 이메일 로그인

## 8. 완료 기준

다음이 모두 충족돼야 `GOOGLE_LOGIN_ENABLED=1`로 전환한다.

- Google Auth Platform 브랜드·도메인 설정 완료
- Web OAuth Client ID 확보
- Debug·release Android OAuth Client 등록
- Play 등록 후 Play App Signing client 추가
- Credential Manager bottom sheet와 명시 버튼 모두 동작
- 서버 공식 라이브러리 기반 ID Token 검증
- `sub` 기반 외부 identity 저장
- 기존 이메일 계정 자동 병합 금지
- 신규·기존·연결 필요 흐름 실기기 테스트
- ID Token·세션·이메일 로그 노출 없음
- 정지·탈퇴 계정 차단
- 로그아웃 시 `clearCredentialState()` 실행
- 기존 이메일 로그인 회귀 없음
- Google 장애 시 이메일 로그인 사용 가능

## 9. 현재 작업에서 건드리지 말 것

- 고객·메모 서버 동기화 로직
- Google Play Billing 구매 흐름
- 추천인 귀속 규칙
- 이메일 로그인 제거
- Drive·Gmail·Contacts scope
- Firebase Auth로 계정 체계 전체 교체
- 앱 package name 변경

## 10. 공식 참고 문서

- Android Credential Manager Sign in with Google 개요: `https://developer.android.com/identity/sign-in/credential-manager-siwg`
- Android 구현: `https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation`
- 서버 ID Token 검증: `https://developers.google.com/identity/sign-in/android/backend-auth`
- OAuth client 관리: `https://support.google.com/cloud/answer/15549257`
- Google OpenID Connect: `https://developers.google.com/identity/openid-connect/reference`

공식 문서의 예제 버전은 변경될 수 있으므로 구현 시 정확한 의존성 버전을 다시 확인하고 고정한다.
