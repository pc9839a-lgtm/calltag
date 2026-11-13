# CallTag 0.44.65 / 2026102601

기존 0.44.64의 SMS 동의·계정 분리 수정 위에 Firebase Analytics와 Crashlytics를 추가한다.
기존 FCM과 동일한 Firebase Android 앱(1:352819510264:android:4417a574453ae5b289340d)을 사용한다.
CI에 저장된 기존 네 가지 Firebase 구성 값으로 google-services.json을 생성하며, 값은 출력하거나 저장소에 커밋하지 않는다.

## 동작

- Google Services 플러그인이 기본 앱 리소스를 생성하여 앱 프로세스 시작 시 Firebase를 초기화한다.
- Analytics의 기본 앱 실행·세션·화면 이벤트와 Crashlytics의 크래시·ANR 보고를 사용한다.
- 전화번호·문자 본문·고객명·메모·로그인 ID를 커스텀 이벤트, 사용자 속성, Crashlytics 사용자 ID로 전달하지 않는다. 기존 로컬 진단 기록도 업로드하지 않는다.
- 광고 ID 수집과 광고 저장·광고 사용자 데이터·광고 개인화 기본값을 끄고 광고 관련 manifest 권한을 제거한다.
- Debug 변형은 수집 기본 OFF, Release 변형은 ON이다. 운영 코드에 강제 크래시를 넣지 않는다.
- Crashlytics 플러그인이 난독화 mapping과 build ID를 생성하고 Release mapping 업로드를 수행한다.

## 출시 전 공개 내용

Firebase SDK 추가에 따라 privacy/index.html에 앱 이용 이벤트·앱 설치 식별자·진단 목적을 반영했다. 이 PR의 파일 변경만으로 공개 웹사이트가 게시된 것은 아니다.
Play Console 데이터 보안에는 앱 상호작용(분석), 크래시 로그 및 진단(분석·앱 기능), 기기 또는 기타 ID(설치 식별자)를 실제 구성에 맞게 반영한다. Analytics는 IP로 대략적인 위치를 도출할 수 있으므로 대략적 위치의 분석 수집도 반영한다. 광고 ID는 수집하지 않는다. 기존에 신고한 고객·통화·문자 데이터는 유지하고 Firebase에 문자 본문이나 연락처가 전달된다고 기재하지 않는다.
Google의 서비스 제공자 처리 예외 등 데이터 공유 분류는 기존 계약·설정에 맞게 확인한다.

참고: https://firebase.google.com/docs/android/play-data-disclosure
https://firebase.google.com/docs/analytics/android/configure-data-collection

## 검증 범위

기존 회귀 검사, Debug·Release 빌드, 서명, 생성된 Firebase 리소스·초기화 provider·merged manifest·R8 mapping 내 SDK 클래스와 Crashlytics build ID를 검사한다.
실제 Firebase 대시보드의 첫 Analytics 이벤트와 테스트 크래시 수신은 기기 실행 후 별도로 확인해야 한다. SDK 포함 검증과 클라우드 수신 검증은 구분한다.

## 심사 영상

사용자가 제공한 새 URL: https://youtube.com/shorts/b1FrefMbV2c
Play Console 심사 제출과 앱 출시는 소유자가 진행한다.
