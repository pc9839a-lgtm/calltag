# 콜태그 Firebase Analytics → 관리자 보고서 연결

현재 상태: 웹 서버측 연결 코드는 준비됐으나 운영 GA4 권한과 Android Analytics 수집 활성화는 별개다.
ChatGPT의 Firebase 플러그인 설치만으로 Cloudflare Pages 환경변수에 GA4 권한이 자동 전달되지 않는다.

1. Firebase 콘솔에서 `kr.pagero.calltag` 앱이 등록되어 있고 Google Analytics와 연결된 기존 프로젝트를 확인한다.
2. GA4 속성 ID(숫자)를 확인한다. 프로젝트 ID/Android 앱 ID와 혼동하지 않는다.
3. GA4 Data API를 Google Cloud 프로젝트에서 사용 설정한다.
4. 읽기 전용 서비스 계정에 해당 **GA4 속성** 최소 Viewer 권한을 부여한다.
5. Cloudflare Pages 런타임 Secrets에 다음 3개를 등록한다.
   - `CALLTAG_GA4_PROPERTY_ID`: 숫자 GA4 속성 ID.
   - `CALLTAG_GA4_CLIENT_EMAIL`: 읽기 전용 서비스 계정 이메일.
   - `CALLTAG_GA4_PRIVATE_KEY`: 서비스 계정 PEM 개인키. **이 값은 GitHub, 채팅, 브라우저에 붙여 넣지 않는다.**
6. Android 앱은 Firebase Analytics가 ON인 빌드로 출시해야 실제 이벤트가 들어온다. 기존 앱만으로 과거 실행·유지율을 소급 수집하지 않는다.
7. Firebase Analytics에서 이벤트 범위 커스텀 차원 `subscriber_status`를 등록해야 유료 이용자 7일 활성 사용자가 나온다.
8. Cloudflare 배포 후 관리자 인증 상태에서 `GET /admin/api/app-analytics`의 응답을 확인한다.
9. 연결 전에는 관리자 화면에 의미 없는 0과 ‘권한 확인 필요’ 카드가 나오지 않는다. 연결 완료 시 앱 이용 현황 섹션이 표시된다.
10. 이 유지율은 **32일 전 최초 사용자 단일 코호트**의 D1/D7/D30이다. 해당 날짜 코호트가 없거나 보고서가 비어 있으면 —. 구독자 유지율을 별도 정확한 코호트로 보려면 GA4 BigQuery Export/가입 및 구독 상태 이력 분석이 추가로 필요하다.

보안: 관리자 Cloudflare Access JWT를 **기존 보호된 운영 서버에서 재검증한 뒤에만** GA4를 조회한다. GA4 자격증명은 클라이언트에 반환하지 않는다. GA4 조회 실패는 결제/가입에 영향을 주지 않는다. 요청 결과는 공개 캐시에서 보관하지 않는다.
