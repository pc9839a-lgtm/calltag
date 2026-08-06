# CallTag v0.43.6 이메일 인증 발송 검증

- versionName: `0.43.6`
- versionCode: `74`

## 원인

기존 앱은 `/api/auth/email-verification` 요청이 HTTP 200을 반환하면 `verification.delivery`를 확인하지 않고 사용자에게 인증메일 발송 성공을 표시했다.

또한 공통 API fallback이 `pagero.kr`, `inlet-8mr.pages.dev`, `call.pagero.kr` 순서로 재시도해 운영 메일 발송 실패가 오래된 또는 mock 응답으로 가려질 수 있었다.

## 수정

- 이메일 인증 요청은 `https://pagero.kr/api/auth/email-verification` 한 곳만 사용
- 인증메일 요청에는 legacy/pages.dev fallback 금지
- `verification.delivery.mode == api`
- `verification.delivery.status == sent`
- 응답에 인증 토큰이 직접 노출되지 않음

위 조건을 모두 만족할 때만 발송 성공으로 처리한다.

## 오류 안내

서버가 반환한 코드에 따라 다음 상태를 구분한다.

- SES 설정 미완료
- SES sandbox 수신자 제한
- 발신 도메인 미인증
- 발송 한도 초과
- 발송 서버 timeout
- provider 거절
- 실제 발송 확인 실패

## 운영 확인

이 패치는 앱이 거짓 성공을 표시하는 문제를 해결한다. 실제 이메일 수신을 위해 운영 서버의 AWS SES 자격증명, production access, 발신 도메인·DKIM 설정이 정상이어야 한다.
