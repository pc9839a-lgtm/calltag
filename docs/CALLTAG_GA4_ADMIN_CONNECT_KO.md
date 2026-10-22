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
10. 유지율은 **D1=2일 전, D7=8일 전, D30=31일 전** 최초 사용자 코호트의 각 해당일 재방문이다. 초기에 D1부터 집계할 수 있으며 D30까지 소급 생성되지 않는다. 코호트가 없거나 보고서가 비어 있으면 —. 구독자 유지율을 별도 정확한 코호트로 보려면 GA4 BigQuery Export/가입 및 구독 상태 이력 분석이 추가로 필요하다.

보안: 관리자 Cloudflare Access JWT를 **기존 보호된 운영 서버에서 재검증한 뒤에만** GA4를 조회한다. GA4 자격증명은 클라이언트에 반환하지 않는다. GA4 조회 실패는 결제/가입에 영향을 주지 않는다. 요청 결과는 공개 캐시에서 보관하지 않는다.

## 2026-10-22 정확도·안정성 보완

- `newUsers7d`는 GA4 공식 `newUsers` 메트릭으로 조회한다. 설치 이벤트 수를 사용자 수로 간주하지 않는다.
- D1/D7/D30은 Google Analytics Data API 문서의 `cohortRetentionFraction` + `cohortActiveUsers/cohortTotalUsers` 형식을 사용한다.
- DAU/WAU/MAU의 측정 기준은 GA4 `activeUsers`이고, 실행 횟수는 `calltag_app_open` 이벤트 수다. 두 지표를 합산하거나 대체하지 않는다.
- Firebase `subscriber_status` 이벤트 커스텀 차원이 등록되면, 7일 동안 앱을 연 유료·무료체험·무료·관리자 지급 이용자를 각기 집계한다. 기간 중 상태가 바뀐 이용자는 여러 구분에 등장할 수 있으므로 단순 합산하지 않는다. **확정 유료 구독자 총수는 기존 결제 서버**를 정본으로 유지한다.
- D1/7/30 각각 충분한 기간이 지난 별도 단일일자 최초 실행 코호트를 사용한다. D30이 아직 없더라도 D1을 표시한다.
- GA4 미연결·데이터 없음은 `—`로 취급하고, 전체 분석 섹션을 숨긴다. GA4 보고 실패가 회원/구독/정산 API를 중단시키지 않는다.
- GA4 API는 인증 확인 후 최대 8초 제한, 동일 프로세스에서 완료된 집계 5분간 재사용한다. 관리자 인증은 매 요청 검증한다. 캐시에는 **집계 결과만** 보관하고 서비스 계정 키나 JWT를 저장하지 않는다.
- Firebase/GA4 속성의 보고 시간대를 **Asia/Seoul**로 지정하는지 확인한다. 사용자 앱을 Analytics 활성 빌드로 업데이트하지 않았다면 전일 수치를 임의 생성할 수 없다.
