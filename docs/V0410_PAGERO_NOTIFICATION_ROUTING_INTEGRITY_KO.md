# 콜태그 v0.41.0 페이지로 알림 이동·무결성 패치

기준일: **2026-08-04**

## 확인된 운영 상태

사용자 실기기 확인:

- 실제 페이지로 문의 접수 후 콜태그 알림 수신
- 앱 완전 종료 상태에서 알림 수신
- 잠금화면에서 알림 수신

서버:

- Cloudflare `inlet` Production Firebase 설정 정상
- D1 `calltag_push_devices` 테이블 정상
- 운영 readiness `ready=true`

## v0.41.0 변경

### 알림 터치 이동

- 한 명의 고객 문의가 반영된 알림은 해당 `CustomerDetailActivity`로 바로 이동
- 여러 고객 문의가 한 번에 반영되면 `방금 접수된 문의` 목록 표시
- 목록에서 고객명·전화번호·메모 요약 확인 후 고객 상세 이동
- 알림에 포함되는 값은 앱 내부 고객 ID뿐이며 고객 개인정보를 FCM payload에 추가하지 않음

### 빠른 연속 문의

- 한 번의 동기화에서 반영된 고객 ID를 `LinkedHashSet`으로 수집
- 같은 고객에게 문의가 여러 건 들어와도 알림 이동 대상은 중복 표시하지 않음
- 동기화 중 추가 FCM 신호가 들어오면 `PENDING_FORCE`로 후속 강제 동기화 1회 예약
- 한 실행에서 최대 200건을 페이지 단위로 처리

### 동일 문의 중복방지

- `eventId` receipt가 `IMPORTED` 또는 `ACKED`이면 고객·메모·상담이력을 다시 생성하지 않고 ACK만 재시도
- 전화번호 고유키로 같은 고객의 중복 생성 차단
- 메모 병합 시 동일 문구가 이미 포함돼 있으면 재삽입하지 않음

주의:

- receipt DB와 고객 DB가 분리돼 있어 프로세스가 두 저장 사이에서 강제 종료되는 극단적인 구간까지 원자적 exactly-once를 보장한다고 표현하지 않는다.
- 동일 eventId 실제 재전송은 실기기·운영 E2E로 별도 확인한다.

### CI 회귀검사

빌드 전에 다음 계약을 정적 확인한다.

- 배치 고객 ID 수집
- `eventId` receipt 중복방지
- 동기화 중 추가 신호 재실행
- 알림 고객 ID 전달
- 한 고객 상세 이동
- 여러 고객 문의 목록
- Manifest Activity 등록

Firebase Secret 4개와 생성된 BuildConfig 값 4개가 비면 기존과 동일하게 빌드를 실패시킨다.

## 버전·병합

- versionName: `0.41.0`
- versionCode: `58`
- 패치 브랜치: `agent/calltag-v0410-pagero-integrity-routing`
- PR: `#38`
- 개발 정본 병합 SHA: `16eaa88035c5ad9e5ca6167b9bdda3f14749a3e8`
- 통합 대상: `agent/calltag-foundation`
- CallTag `main`: 미병합

## 빌드 검증

- Workflow: `Build CallTag APK`
- Run ID: `30878263366`
- Job ID: `91894045986`
- Firebase Secret 검증: 성공
- 페이지로 알림 라우팅 계약 검증: 성공
- Java·리소스·Manifest·Debug APK 빌드: 성공
- 생성된 Firebase BuildConfig 검증: 성공
- APK 업로드: 성공

Artifact:

- Artifact ID: `8880329516`
- 이름: `calltag-v0.41.0-pagero-integrity-routing-debug-apk`
- ZIP 크기: `4,214,595 bytes`
- ZIP digest: `sha256:a7c5247437d9631e5dc51f9d7367c90b49560d46f01655eaf78ba37b5bdc53db`

APK:

- 파일명: `calltag-v0.41.0-debug.apk`
- 크기: `4,461,859 bytes`
- SHA-256: `9e01e02f99e873bb613bf6dfcb8efb25593ac189484075b67e57979a562b85f2`

## 실기기 확인 순서

1. 기존 앱 위에 v0.41.0 덮어 설치
2. 페이지로 문의 1건 접수
3. 알림 터치 시 해당 고객 상세로 바로 이동 확인
4. 고객 상세에서 메모와 문의 상담이력 확인
5. 다른 번호 3건을 빠르게 접수
6. 알림 터치 시 `방금 접수된 문의` 목록에 세 고객 표시 확인
7. 같은 eventId 재전송 시 고객·메모·상담이력 중복 없음 확인

## 완료와 미완료 구분

코드·빌드 확인 완료:

- 알림 고객 ID 전달
- 한 고객·여러 고객 분기 이동
- 배치 고객 ID 중복 제거
- receipt 기반 중복방지 계약
- Android 컴파일·Manifest·APK 패키징

사용자 실기기 확인 필요:

- 알림 터치 후 실제 고객 화면 이동
- 고객 메모·문의 상담이력 내용 확인
- 빠른 연속 문의 3건 목록 표시
- 동일 eventId 실제 재전송 결과
