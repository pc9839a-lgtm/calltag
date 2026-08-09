# CallTag v0.44.10 / code88 security hardening

보안 우선 릴리스. 신규 기능보다 Android 공격면 축소를 먼저 적용한다.

## 적용
- cleartext HTTP 전면 차단 (`usesCleartextTraffic=false`, network security config)
- 더 이상 사용하지 않는 `WRITE_CONTACTS` 권한 제거
- `LoginActivity` 비공개화; 외부 Google OAuth 진입은 최소 전용 trampoline Activity로 분리
- Google OAuth 콜백은 최근 사용자 시작 flow가 존재할 때만 1회 허용하며 URI 구조/길이를 검증
- 추천코드는 외부 Intent extra 및 `calltag://`/HTTP 입력을 거부하고 `https://pagero.kr/r/...` 계열만 엄격히 허용
- `BootReceiver` 비공개화 + BOOT_COMPLETED/MY_PACKAGE_REPLACED action allowlist
- 수신 고객정보 overlay에 `FLAG_SECURE` 적용해 일반 스크린샷/화면 캡처 방지
- crash telemetry에서 raw Throwable message 저장 제거
- PageRo 동기화 logcat에서 lead ID와 raw Throwable 출력 제거
- PendingIntent mutable 사용, WebView JS bridge, custom TrustManager/HostnameVerifier, world-readable storage를 CI 금지 항목으로 고정
- 기존 Android Keystore AES-GCM 로그인 세션 암호화와 `allowBackup=false` 유지

## 남은 경계
현재 Google OAuth 서버 복귀 방식은 `calltag://auth/google` custom scheme이다. code88은 비로그인 임의 콜백 주입과 exported UI 노출을 크게 줄이지만, 동일 custom scheme을 선점한 악성 앱이 사용자가 실제 로그인 중일 때 콜백을 가로채는 위험을 플랫폼 수준에서 완전히 제거하지는 못한다. 완전 제거는 Google Play 앱 서명 인증서 지문으로 `pagero.kr/.well-known/assetlinks.json`을 배포한 뒤 검증된 HTTPS App Link OAuth callback으로 전환해야 한다.
