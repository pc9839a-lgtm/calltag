# CallTag 관리자 최소노출 보안 기준 — 2026-08-12

## 목적

`https://calltag.pagero.kr/admin`은 일반 백오피스가 아니라 침해를 전제로 한 읽기 전용 운영 콘솔이다.
관리자 웹 또는 관리자 API 한 계층이 침해되어도 원문 개인정보·결제 비밀값·CRM 업무내용을 대량으로 꺼내기 어렵게 만드는 것이 최우선 목표다.

## 인증 경계

관리자 데이터 API는 다음 조건을 모두 통과해야 한다.

1. Cloudflare Access JWT의 RS256 서명·issuer·audience·만료 검증
2. Access JWT 이메일과 활성/이메일인증 완료 CallTag 계정의 일치
3. CallTag 관리자 allowlist

Access 설정이나 allowlist가 없으면 API는 fail-closed로 닫힌다.
기존 `CALLLINK_ADMIN_TOKEN` / `INLET_API_TOKEN`을 브라우저 관리자 인증에 사용하지 않는다.

## 관리자 비밀번호/세션을 만들지 않는 이유

- `/admin`에는 CallTag 비밀번호 입력폼이 없다.
- `/admin` 전용 CallTag 세션 쿠키도 만들지 않는다.
- `localStorage`, `sessionStorage`, 브라우저 JavaScript에 재사용 가능한 관리자 자격증명을 보관하지 않는다.
- 따라서 관리자 정적 페이지가 변조되어도 별도 CallTag 비밀번호나 CallTag 세션을 훔칠 대상 자체를 두지 않는다.
- 실제 인증은 Cloudflare Access에서 처리하고, `inlet`이 매 요청마다 Access JWT를 다시 검증한다.
- 운영 Access 정책은 관리자 개인 계정 + MFA를 권장한다.

## 관리자 화면에 허용되는 데이터

- 내부 회원 식별자
- 마스킹 이메일
- 마스킹 전화번호
- 가입/수정 시각
- 체험 시작/종료 및 추천 보너스 일수
- 구독 상품 코드
- 결제 채널
- 구독 상태
- 검증 상태
- 시작/다음결제/만료/최근검증 시각
- 추천 여부/추천 인원 집계
- 파트너 적립 건수 및 합계

## 관리자 화면에서 금지되는 데이터

- 비밀번호 및 비밀번호 해시
- Google Play purchaseToken 원문
- purchaseToken hash
- Google/Cloudflare 서비스 계정 키 및 모든 서버 secret
- Google Play 주문번호
- 외부 구독 ID
- 결제 고객 원문 식별자
- 고객 통화내용
- 고객 메모
- 문자 원문
- PageRo 문의 원문
- 추천 결제 reference
- 계좌/정산 원문정보
- 전체 이메일/전체 전화번호
- 임의 SQL 결과 및 DB dump

## 2중 응답 필터

1. `inlet` Admin API가 명시적 SELECT와 DTO로 최소 필드만 반환한다.
2. `calltag`의 `/admin` 게이트웨이가 응답을 그대로 전달하지 않고 허용 필드만 다시 객체로 구성한다.

따라서 백엔드 Admin API에 향후 실수로 필드가 추가되어도 해당 필드는 관리자 브라우저로 전달되지 않는다.

## 읽기 전용 원칙

현재 V1 `/admin`은 조회만 허용한다.

- 이용권 수동 지급 없음
- 결제 상태 수동 변경 없음
- 정산 확정/지급 없음
- 회원 정지/탈퇴 없음
- CRM 데이터 수정 없음
- CSV 전체 내보내기 없음

향후 변경 기능은 별도 권한, 재인증, 사유 입력, before/after 감사로그를 갖춘 뒤 추가한다.

## 감사로그

서버는 `calltag_admin_audit`에 다음 최소정보만 기록한다.

- 관리자 owner ID
- 작업명
- 대상 owner ID
- 결과
- 생성시각
- 선택적 IP HMAC 해시

원문 IP는 저장하지 않는다. `CALLTAG_ADMIN_AUDIT_SALT`가 없으면 IP 해시도 저장하지 않는다.
요청 본문이나 조회 결과는 감사로그에 저장하지 않는다.

## 필요한 서버 환경설정 — inlet

```text
CALLTAG_ADMIN_ENABLED=1
CALLTAG_ADMIN_ACCESS_ISS=https://<team>.cloudflareaccess.com
CALLTAG_ADMIN_ACCESS_AUD=<Cloudflare Access application AUD>
CALLTAG_ADMIN_OWNER_IDS=<권장: 허용 owner ID, 쉼표 구분>
CALLTAG_ADMIN_AUDIT_SALT=<별도 랜덤 secret>
```

owner ID를 아직 확정하지 못한 초기 설정에서는 다음 allowlist를 사용할 수 있지만, 운영 안정화 후 owner ID allowlist로 옮기는 것을 권장한다.

```text
CALLTAG_ADMIN_EMAILS=<관리자 이메일, 쉼표 구분>
```

`CALLTAG_ADMIN_AUDIT_SALT`는 Secret으로 저장하며 저장소에 커밋하지 않는다.

## Admin API origin

CallTag 관리자 게이트웨이는 Access JWT가 임의 외부 origin으로 전달되는 설정 실수를 막기 위해 Admin API origin을 코드에서 다음 주소로 고정한다.

```text
https://inlet-8mr.pages.dev
```

Admin API origin 변경이 필요하면 런타임 환경변수로 임의 변경하지 않고 코드리뷰를 거쳐 고정값을 변경한다.

## Cloudflare Access

CallTag 도메인의 `/admin*` 경로를 Access Application으로 보호한다.
관리자 개인 계정만 허용하고 MFA 정책을 적용하는 것을 권장한다.
Access가 구성되기 전에는 관리자 화면/API를 공개하지 않는다.
원본 `inlet` API도 전달받은 `Cf-Access-Jwt-Assertion`의 서명, issuer, audience, 만료를 다시 검증한다.

## 응답 보안 헤더

`/admin*`에는 다음 정책을 강제한다.

- `Cache-Control: no-store`
- `Content-Security-Policy` self-only
- `frame-ancestors 'none'`
- `Referrer-Policy: no-referrer`
- `X-Content-Type-Options: nosniff`
- `X-Robots-Tag: noindex, nofollow, noarchive, nosnippet`
- 카메라/마이크/위치/payment/USB/Bluetooth Permissions Policy 차단
- third-party CDN/font/analytics 미사용

## 전용 보안 QA

양쪽 저장소에 `CallTag Admin Security QA`를 둔다.

- 새 관리자 JavaScript 구문 검사
- 로그인/password/session storage 재도입 차단
- purchase token/hash/order/external subscription ID 등의 관리자 노출 회귀 차단
- `innerHTML` 계열 위험 렌더링 재도입 차단
- Access 검증 필수 설정과 핵심 보안 헤더 존재 확인

## 침해 범위에 대한 경계

이 구조는 관리자 웹 또는 관리자 API 계층 침해 시 피해반경을 크게 줄인다.
전체 Cloudflare 계정, main application runtime, D1 binding과 모든 Secret이 동시에 탈취되는 수준의 침해는 이 관리자 화면만으로 완전히 무해화할 수 없다. 그 범위까지 방어하려면 애플리케이션 데이터 자체의 별도 키 관리, 필드 암호화, 또는 별도 최소정보 projection 저장소 설계가 필요하다.
