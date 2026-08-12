# CallTag Google Play 업로드키 재설정 절차

기준일: 2026-08-12

## 왜 재설정하는가

현재 Play Console이 받아들이는 CallTag 기존 upload certificate SHA-256은 아래와 같다.

`C3:4C:98:88:9B:0C:88:8A:BB:39:94:6C:80:16:96:C2:89:E2:82:6C:10:0F:41:7A:0B:CE:25:A3:92:C4:72:A7`

직전 정상 0.44.24 AAB도 이 인증서로 서명되었다.

그러나 이 인증서의 private upload key가 들어 있던 GitHub Actions artifact `8922836146`은 보관기간 만료로 더 이상 내려받을 수 없다. 현재 살아 있는 과거 백업 artifact `8952526712`는 SHA-256이 다른 별도 키이므로 절대 사용하지 않는다.

Play App Signing에서 upload key는 앱 배포용 app signing key와 별개다. upload key를 재설정해도 Google Play가 사용자 기기에 배포할 때 쓰는 app signing key는 변경되지 않는다.

## 보안 원칙

- JKS/private key를 GitHub 저장소에 commit하지 않는다.
- JKS/private key를 채팅, 이메일, 문서에 올리지 않는다.
- 비밀번호를 채팅이나 문서에 적지 않는다.
- 공개 PEM 인증서와 SHA-256 fingerprint만 Play Console 등록 및 검증에 사용한다.
- 기존 사용자 데이터, 로그인 데이터, 결제 데이터는 건드리지 않는다.

## 1. 새 upload key를 사용자 PC에서만 생성

저장소 루트의 PowerShell에서 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate_play_upload_key_reset.ps1
```

스크립트는 다음을 생성한다.

- `private/play-upload-reset/calltag-upload-reset.jks` — 비밀, 외부 공유 금지
- `private/play-upload-reset/upload_certificate.pem` — Play Console 제출용 공개 인증서
- `private/play-upload-reset/upload_key_fingerprint.txt` — 공개 fingerprint 확인용

`private/play-upload-reset/`은 `.gitignore`에 등록되어 있다.

키 생성 중 비밀번호는 `keytool`이 로컬 콘솔에서 직접 입력받는다. 스크립트는 비밀번호를 저장하거나 출력하지 않는다.

## 2. Play Console에서 upload key reset 요청

Google 공식 절차 기준:

1. CallTag 앱의 Play App Signing 사용 여부를 확인한다.
2. 새 upload key를 생성한다.
3. 위 스크립트가 만든 `upload_certificate.pem`을 준비한다.
4. 개발자 계정 소유자(Account owner)가 Play Console의 upload key reset 절차를 시작한다.
5. 요청 화면에서 `upload_certificate.pem`을 제출한다.
6. Google Play가 새 upload key 등록 완료를 알릴 때까지 기다린다.

공식 도움말은 upload key를 잃었거나 유출된 경우 새 upload key를 만들고 PEM 인증서를 제출해 reset할 수 있다고 안내한다. reset은 app signing key와 기존 사용자에게 영향을 주지 않는다.

## 3. reset 승인 후 저장소 공개 fingerprint 갱신

승인된 새 upload key의 SHA-256만 다음 파일에 한 줄로 기록한다.

`config/play-upload-key-sha256.txt`

SHA-256은 공개 fingerprint이므로 private key가 아니다.

## 4. GitHub Actions Secrets 등록

GitHub 저장소 Settings → Secrets and variables → Actions에 사용자가 직접 다음 4개를 등록한다.

- `CALLTAG_UPLOAD_KEYSTORE_BASE64`
- `CALLTAG_UPLOAD_STORE_PASSWORD`
- `CALLTAG_UPLOAD_KEY_ALIAS`
- `CALLTAG_UPLOAD_KEY_PASSWORD`

alias 기본값은 `calltag-upload`이다.

JKS Base64는 로컬 생성 스크립트 마지막 질문에서 `y`를 선택하면 Windows 클립보드에만 복사할 수 있다. 채팅에는 붙여넣지 않는다.

## 5. 0.44.25 signed build

Workflow:

`.github/workflows/calltag-v04425-entitlement-fix.yml`

이 workflow는 다음 조건을 모두 만족해야만 signed build를 만든다.

- 4개 GitHub Secret 모두 존재
- JKS가 정상 decode됨
- JKS 인증서 SHA-256이 `config/play-upload-key-sha256.txt`와 일치
- 0.44.25 / versionCode 2026081211 계약 검사 통과
- `all_monthly`를 조회하지 않음
- release APK 최종 인증서가 accepted upload key와 일치
- release AAB 최종 인증서가 accepted upload key와 일치

하나라도 다르면 즉시 실패하며 다른 artifact/key로 fallback하지 않는다.

## 6. 절대 하지 말 것

- `8952526712`의 JKS를 정상 키라고 착각해 사용하지 않는다.
- signed APK/AAB에서 private key를 복구하려고 하지 않는다. 서명 산출물에는 private key가 없다.
- 새 app signing key를 만들거나 교체하지 않는다. 이번 문제는 upload key만 대상이다.
- 기존 CallTag 앱을 새 패키지명으로 다시 만들지 않는다.

## 7. 현재 앱 패치 상태

0.44.25 / 2026081211 소스 컴파일은 이미 성공했다.

- 전화관리/문자자동화 entitlement 독립 처리
- 보유 상품 `이용 중` 표시
- 미보유 상품 추가 구매 허용
- 개발자용 결제 문구 제거
- Google Credential Manager 비취소 오류 시 browser OAuth fallback
- `all_monthly` 미사용 유지

따라서 upload key reset과 GitHub Secret 등록이 끝나면 같은 0.44.25 소스로 signed AAB/APK를 생성하면 된다.
