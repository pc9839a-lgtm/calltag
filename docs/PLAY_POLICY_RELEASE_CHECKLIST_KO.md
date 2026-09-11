# CallTag Google Play 정책 제출 체크리스트

기준 버전: **0.44.57 / versionCode 2026091101**  
기준일: **2026-09-11**

## 1. 데이터 보안 / 계정 삭제

실제 앱 동작:

- 앱에서 계정 생성 가능
- 앱 내 경로: `더보기 → 계정 → 회원탈퇴 → 계정 삭제`
- 서버 삭제 성공 후 기기 내 콜태그 DB/세션/설정 삭제
- 서버 계정 프로필 및 CallTag 관련 서버 데이터 삭제
- 추천 혜택 중복 악용 방지를 위한 **복원 불가능한 전화번호 HMAC 식별값만** 유지 가능
- 원본 전화번호/이름/이메일/상담내용은 해당 HMAC tombstone에 유지하지 않음

Play Console 제출:

- `계정 삭제 지원`: **예**
- `앱 내 계정 삭제 경로`: **있음**
- 외부 계정 삭제 URL: `https://calltag.pagero.kr/account-deletion/`
- 기존 `데이터 삭제가 지원되지 않습니다` 표시는 현재 구현과 맞지 않으므로 삭제 지원으로 변경
- 보존 예외를 묻는 항목에는 부정 사용 방지용 비가역 HMAC 식별값과 법령상 필요한 거래 기록만 명시

공개 문서:

- 개인정보처리방침: `https://calltag.pagero.kr/privacy/`
- 서비스 이용약관: `https://calltag.pagero.kr/terms/`
- 계정 삭제 안내: `https://calltag.pagero.kr/account-deletion/`

> 위 URL은 production 배포 후 실제 200 응답을 확인한 다음 Play Console에 제출한다.

## 2. READ_CALL_LOG / SEND_SMS

콜태그의 핵심 기능은 사용자가 활성화하는 **통화 후 고객관리 자동화**다.

권장 Play Permissions Declaration 용도:

- `READ_CALL_LOG`: 통화 종료 후 실제 완료된 통화 내역을 확인하고 동일 통화를 중복 처리하지 않으며 고객/상담/후속 업무를 생성하기 위해 사용
- `SEND_SMS`: 사용자가 직접 설정한 조건과 템플릿에 따라 통화 후 후속 문자를 전송하기 위해 사용
- 두 권한을 함께 설명할 때는 **Device automation / 사용자가 설정한 트리거 기반 자동화**에 맞춰 설명

제출 설명문:

> 콜태그의 핵심 기능은 사용자가 활성화한 통화 후 고객관리 자동화입니다. 사용자가 통화를 종료하면 READ_CALL_LOG로 실제 통화 완료 내역을 확인해 중복 없이 고객/상담/후속업무를 생성하고, 사용자가 사전에 설정한 조건과 템플릿에 따라 SEND_SMS로 후속 문자를 전송합니다. 사용자가 해당 기능을 끄면 통화 감지 및 자동화가 중지됩니다.

심사용 데모 영상에 반드시 포함:

1. 콜태그 로그인
2. 통화 감지/후속관리 기능 활성화
3. 권한 허용 화면
4. 실제 통화 후 종료
5. 작은 `통화 메모` 팝업 또는 대체 알림 표시
6. 고객/상담 기록 생성
7. 사용자가 설정한 자동문자 규칙에 따른 문자 전송
8. 기능을 끄면 자동화가 중지되는 화면

스토어 설명 핵심 문구:

> 통화가 끝나면 고객을 자동 정리하고, 사용자가 설정한 조건에 따라 후속 문자를 자동 발송합니다.

주의:

- 권한이 단순 보조기능처럼 보이면 안 됨. 앱의 핵심 사용자 흐름으로 설명한다.
- 기업 전용 로그인 요건이 없는 현재 앱에서 `Enterprise CRM`만을 단독 근거로 제출하지 않는다.
- 전화번호 인증, OTP 인증 목적으로 READ_CALL_LOG를 사용한다고 설명하지 않는다.

## 3. FOREGROUND_SERVICE_SPECIAL_USE

Manifest 현재 계약:

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- `CallMonitorService`
- `android:foregroundServiceType="specialUse"`

Play Console 설명문:

> User-enabled phone CRM and SMS automation that observes call state, creates follow-up work, and sends configured messages after calls.

사용자 영향 설명:

> 사용자가 통화 후 자동관리를 활성화한 동안 콜태그는 foreground service로 통화 상태를 관찰합니다. 서비스가 중단되면 실시간 통화 종료 감지, 후속 고객관리 작업 또는 사용자가 설정한 통화 후 자동문자가 누락될 수 있습니다. 사용자는 앱 설정에서 이 기능을 끌 수 있으며 동작 중에는 Android의 지속 알림이 표시됩니다.

FGS 심사용 영상:

1. 기능 활성화
2. 지속 알림 표시
3. 실제 통화
4. 통화 종료 감지
5. 후속관리 팝업/업무 생성
6. 기능 비활성화 후 서비스 중지

## 4. 데이터 보안 실제 처리 기준

- 전송: HTTPS only
- 로그인 세션: Android Keystore 기반 암호화 저장
- 외부 연동 Webhook URL / Direct API raw key: 발급 직후 1회 표시, SharedPreferences/SQLite 원문 저장 금지
- 외부 문의: 로그인 세션 또는 API Key 기반 owner scope
- FCM lead notification: PII를 정본으로 사용하지 않고 Android가 인증된 API로 실제 Lead pull
- 계정 삭제 시 외부 연동/문의/푸시/결제 연결 데이터 포함 서버 연관 데이터 삭제

## 5. 금융 기능 선언 별도 확인 필요

CallTag의 금전 파트너 수익/정산 기능은 Play 앱에서 제거되어 있다. 다만 현재도:

- 추천 코드로 가입한 사용자: +7일
- 추천한 사용자: 성공 추천 1명당 +5일, 횟수 제한 없음

이 이용기간 혜택은 Google Play의 `Rewards / other incentives` 문구와 해석 충돌 가능성이 있으므로 **금융 기능을 무조건 '없음'으로 바꾸기 전에 Play의 현재 선언 문구 기준으로 최종 확인한다.**

## 6. 출시 전 최종 게이트

- Android contract check PASS
- Debug APK build PASS
- Signed release AAB build PASS
- 기존 Play upload key 서명 검증 PASS
- 발신/수신 실제 통화 종료 후 앱 전체화면 자동 실행 없음
- 대신 작은 통화 메모 팝업 정상 표시
- 팝업 저장 후 고객/상담 메모 반영
- 외부 문의 Direct API create/rotate/revoke 실서버 확인
- 개인정보/약관/계정삭제 URL production 200 확인
