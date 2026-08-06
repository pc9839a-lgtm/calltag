# CallTag v0.42.5 서버 동기화·보안 인계

최종 갱신: 2026-08-05

## 현재 앱 데이터 구조

- 고객·상담·메모·단계·후속 일정은 로컬 SQLite DB가 정본이다.
- 문자 템플릿·자동문자 설정·발송 제외·이미지는 로컬 DB·SharedPreferences·files에 저장한다.
- `android:allowBackup="false"`이므로 Android 자동복원은 사용하지 않는다.
- 앱 삭제, 저장공간 데이터 삭제, 휴대폰 초기화 시 로컬 데이터는 삭제된다.
- 삭제 전에 `.ctbackup`을 외부 저장소에 만들었을 때만 수동 복원이 가능하다.
- 로그인·무료기간·결제·추천코드는 서버 데이터이므로 같은 계정으로 다시 로그인하면 재조회할 수 있다.

## 서버 P0 계약

연결 서버: `pc9839a-lgtm/inlet`

예정 API:

- `POST /api/calltag-sync/push`
- `GET /api/calltag-sync/pull?cursor=&limit=`
- `GET /api/calltag-sync/status`
- `POST|DELETE /api/calltag-sync/erase`

필수 헤더:

- `X-Inlet-Session`: 로그인 세션
- `X-CallTag-Device`: 앱이 생성해 안전하게 보관하는 임의 기기 ID
- `X-CallTag-Device-Label`: 선택값, 사용자에게 보여줄 기기명
- `X-CallTag-App-Version`: 앱 버전

클라이언트는 `ownerId`를 보내지 않는다. 서버가 로그인 세션에서 계정을 확정한다.

## Android P1 구현 순서

1. 앱 최초 실행 시 128비트 이상 임의 device ID 생성
2. device ID는 Android Keystore로 암호화 저장
3. 로컬 각 엔터티에 `sync_id`, `sync_version`, `sync_state` 추가
4. 고객·상담·할 일 변경 시 outbox 기록
5. 한 번에 최대 100건 push
6. 성공 항목만 outbox 제거
7. version conflict 발생 시 server record pull 후 병합 화면 또는 안전 규칙 적용
8. cursor를 저장하고 증분 pull
9. 첫 로그인·재설치 시 cursor 0부터 bootstrap
10. 서버 복구 완료 전 자동문자·문자 발송을 시작하지 않음
11. 더보기에 마지막 동기화 시각·오류·재시도 표시
12. 로그아웃 시 로컬 고객 데이터 처리 방식을 사용자에게 확인

## 우선 동기화 대상

P1:

- customer
- interaction
- task
- stage

P2:

- template
- automation
- 문자 발송 결과 최소 메타데이터

별도 후순위:

- 템플릿 첨부 이미지는 서버 sync JSON에 넣지 않고 암호화 R2 업로드로 분리

## 서버 업로드 금지

- 통화 녹음
- 전체 연락처
- CallTag와 관계없는 전체 통화기록
- 휴대폰 전체 문자함
- 로그인 세션 원문
- Google ID Token 원문
- Google Play purchase token 원문
- 클립보드·위치·광고식별자

## Google 로그인 미완료 작업

- Android Credential Manager 적용
- Google Cloud Android/Web OAuth Client 등록
- 앱 서명 SHA-256 등록
- 서버 ID Token 서명·audience·issuer·expiry 검증
- Google `sub` 기반 외부 계정 테이블
- 기존 이메일 계정 연결 시 재인증
- 계정 연결·해제와 전체 세션 해제
- 정지·탈퇴 계정 로그인 차단

이메일 주소만 일치한다고 자동 병합하지 않는다.

## 결제 미완료 작업

Google Play:

- Play Console 앱 등록
- 3개 구독 상품·base plan 등록
- 서비스 계정과 Play Developer API 연결
- 내부 테스트·라이선스 테스터
- 구매·갱신·취소·유예·환불·만료 실기기 검증
- RTDN 처리와 idempotency
- 조건 완료 후에만 Play release flag 활성화

웹 결제:

- PG 선택·계약
- checkout
- webhook 서명 검증
- 결제 이벤트 idempotency
- 취소·환불
- Play·웹 중복 구독 차단

## 보안 요구사항

- 고객 원문은 서버에서 AES-256-GCM 암호화
- 전화번호 검색은 HMAC 값 사용
- 기기 ID 원문 서버 저장 금지
- 한 요청 최대 100건
- 계정·기기별 rate limit
- 민감정보와 세션을 로그에 남기지 않음
- 삭제는 tombstone 동기화 후 보관기간 종료 시 물리 삭제
- 서버 기능 flag와 secret이 없으면 동기화 API 사용 금지

## 앱 활성화 조건

다음 조건 전에는 Android sync worker를 릴리스하지 않는다.

- 서버 PR 병합
- D1 migration 적용
- 암호화·검색 secret 등록
- staging 기능 flag 활성화
- 두 계정 간 격리 테스트
- 앱 삭제·재설치 복구 테스트
- 충돌·오프라인·중복 push 테스트
- 서버 장애 시 로컬 데이터 보존 확인

## 현재 비목표

- 즉시 운영 동기화 활성화
- 통화 녹음 백업
- 전체 연락처 클라우드 백업
- 관리자 고객 원문 열람 화면
- 파트너센터
