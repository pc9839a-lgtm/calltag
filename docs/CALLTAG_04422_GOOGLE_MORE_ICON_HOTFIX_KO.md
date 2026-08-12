# CallTag 0.44.22 긴급 수정 정본

기준일: 2026-08-12

이 문서는 `0.44.21 / 2026081207` 이후 발생한 Google 로그인, 더보기 메뉴 간격, 런처 아이콘 문제에 대한 최신 정본이다. 이 항목들은 이전 릴리스 문서보다 본 문서를 우선한다.

## 릴리스

- versionName: `0.44.22`
- versionCode: `2026081208`
- 브랜치: `agent/calltag-auth-ux-google-upgrade-fix`
- signed AAB workflow: `CallTag 0.44.22 signed Play AAB`
- workflow run: `31556844439`
- artifact: `calltag-v0.44.22-code2026081208-play-aab`
- artifact id: `9126319845`

## 1. Google 로그인 계정 선택 후 무반응

### 기존 문제

`GoogleCredentialLoginActivity`가 `windowIsTranslucent=true`인 투명 브리지 Activity로 Credential Manager를 호스팅했다.

Google 계정 선택창은 열렸지만 일부 기기/Google Play services 조합에서 계정 선택 후 provider result 복귀가 불안정할 수 있는 구조였다. 사용자는 계정을 눌러도 다음 상태가 보이지 않았다.

### 0.44.22 변경

- Credential Manager 호스트 Activity를 불투명한 실제 foreground Activity로 변경
- Google 계정 선택 result를 받을 때까지 Activity lifecycle 유지
- `CancellationSignal` 추가
- 30초 provider timeout 추가
- 계정 선택 후 `선택한 Google 계정을 확인하고 있습니다…` 표시
- ID Token 서버 교환 중 `콜태그 로그인 정보를 확인하고 있습니다…` 표시
- Credential Manager exception type을 logcat에 기록
- configuration/no credential 오류를 사용자 메시지로 구분

실기기 E2E는 반드시 `0.44.22` 설치 후 다시 확인한다. CI 성공만으로 Google 로그인 성공이라고 기록하지 않는다.

## 2. 더보기 메뉴 구조

`8개 메뉴를 한 카드에 연속 배치`한 0.44.21 구조를 폐기한다.

0.44.22 고정 구조:

- 내 정보
  - 계정
  - 이용권
- 업무 관리
  - 문자 관리
  - 고객 관리
- 서비스
  - 페이지로
  - 파트너
- 앱 관리
  - 데이터 관리
  - 앱 정보

각 그룹은 별도 카드이고 그룹 사이 24dp 수준의 분리 여백을 둔다. 메뉴 행 높이는 60dp로 유지한다.

## 3. 아이콘 깨짐

### 원인

기존 `app/src/main/res/drawable-nodpi/calltag_launcher_safe.webp` 파일 자체에 가로 노이즈와 색상 깨짐이 들어 있었다. 단순 adaptive icon crop 문제가 아니었다.

### 0.44.22 변경

- 깨진 WebP 파일 완전 삭제
- `calltag_launcher_foreground.xml` 벡터 foreground 사용
- 파란 배경 + 흰 전화기 + 흰 태그 심볼
- adaptive icon과 legacy icon 모두 동일한 벡터 소스 사용
- release/debug manifest에서 `@mipmap/ic_launcher_calltag` / `@mipmap/ic_launcher_calltag_round` 사용
- CI에서 깨진 WebP가 다시 포함되면 release 실패하도록 검사
- 생성된 AAB 내부에 `calltag_launcher_safe.webp`가 없음을 검증

## 다음 확인

1. Play Console에 `0.44.22 / 2026081208` 업로드
2. 테스트 기기 업데이트
3. Google로 계속하기 → 계정 선택
4. 계정 선택창이 닫히고 로그인 처리 상태가 표시되는지 확인
5. 로그인 완료 또는 구체적인 오류 메시지 확인
6. 더보기 4개 그룹 간격 확인
7. Google 계정 선택창 / 홈 / 앱서랍 아이콘 깨짐이 없는지 확인

`2026081208`을 Play Console에 한 번이라도 업로드하면 다음 versionCode는 `2026081209` 이상을 사용한다.
