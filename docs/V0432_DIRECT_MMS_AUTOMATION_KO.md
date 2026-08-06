# CallTag v0.43.2 사진 MMS 자동발송 복원

최종 갱신: 2026-08-06

## 목적

페이지로 콜링크에서 제공하던 사진 포함 자동문자 기능을 CallTag 발송 큐에 복원한다.
템플릿에 사진이 지정되어 있으면 기본 메시지 앱 작성창을 열지 않고 통화 종료·부재중·후속 예약 시점에 MMS를 직접 요청한다.

## 적용 범위

- 연결된 수신 통화 종료 후 사진 MMS 자동발송
- 연결된 발신 통화 종료 후 사진 MMS 자동발송
- 부재중·거절 전화 후 사진 MMS 자동발송
- 1~30일 후속문자 규칙별 사진 MMS 자동발송
- 텍스트 템플릿은 기존 SMS 발송 유지
- MMS 실패 시 같은 본문을 일반 SMS로 한 번만 대체발송
- 발송 성공·실패·대체발송 진단 기록
- 고객 타임라인과 발송내역 상태 반영
- 선택한 SIM 회선 사용

## 발송 구조

1. 사용자가 템플릿에 사진 1장을 저장한다.
2. 통화 후 또는 후속문자 설정에서 해당 템플릿을 선택한다.
3. 자동화 작업 생성 시 템플릿 사진을 메시지 작업별 스냅샷으로 복사한다.
4. Klinker MMS PDU builder로 본문과 JPEG 사진을 포함한 완성 PDU를 만든다.
5. 읽기 전용 `MmsPduProvider`를 통해 Android telephony 프로세스에 PDU를 제공한다.
6. 선택 SIM의 `SmsManager.sendMultimediaMessage()`로 자동 발송한다.
7. 결과 PendingIntent를 `MmsStatusReceiver`가 받아 발송내역을 확정한다.

## 이미지 제한

- 템플릿 입력: 기기에서 읽을 수 있는 이미지
- 발송 형식: JPEG
- 최대 긴 변: 1280px
- 목표 이미지 크기: 540KB 이하
- 전체 MMS PDU: 600KB 이하
- 투명 배경은 흰색으로 변환
- 사진 1장만 지원

## 실패 처리

MMS 데이터망·APN·통신사 서버·PDU 생성 문제로 MMS가 실패하면 사진 스냅샷을 제거하고 같은 메시지 작업을 텍스트 SMS로 한 번만 대체 발송한다. 반복 대체발송은 SharedPreferences guard로 차단한다.

## 보안·권한

- 추가 SMS 수신함 읽기 권한 없음
- 기존 `SEND_SMS` 권한 사용
- PDU provider는 `exported=false`, `grantUriPermissions=true`
- PDU는 cache directory에 임시 생성하고 결과 콜백 후 삭제
- 템플릿 원본 사진은 앱 전용 저장소 유지

## Google Play 내부 테스트

- package: `kr.pagero.calltag`
- versionName: `0.43.2`
- versionCode: `70`
- targetSdk: `36`
- v0.43.0·v0.43.1과 동일 Play upload key 사용

## 필수 실제 기기 검수

- SKT / KT / LG U+
- 삼성 메시지 / Google 메시지
- 수신 통화 / 발신 통화 / 부재중 / 거절
- 듀얼 SIM 각각
- 모바일 데이터 ON/OFF
- MMS 차단 요금제 또는 APN 오류
- 사진 1장 정상 발송
- MMS 실패 시 텍스트 대체발송 1회
- 중복발송 방지

CI 성공은 컴파일·서명·정적 계약 검증이다. 실제 통신사 수신 성공 여부는 내부 테스트 기기에서 반드시 확인한다.
