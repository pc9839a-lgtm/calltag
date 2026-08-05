# CallTag v0.42.7 백그라운드 자동 데이터 보호

최종 갱신: 2026-08-05

## 목적

사용자가 데이터 보호를 켠 뒤 앱을 계속 열어두거나 매번 `지금 동기화`를 누르지 않아도 고객·상담·메모·후속 일정의 변경사항을 안전하게 서버 복구본으로 반영할 수 있도록 Android WorkManager 기반 백그라운드 실행 구조를 추가한다.

운영 서버 동기화 flag가 꺼져 있는 현재 상태에서는 작업이 실행돼도 서버 readiness 확인만 수행하며 고객 payload는 만들거나 전송하지 않는다.

## 기본 원칙

- 사용자 동의 기본값은 계속 OFF다.
- 로그인 세션과 계정별 데이터 보호 동의가 모두 있어야 작업을 예약한다.
- 데이터 보호를 끄거나 로그아웃하면 예약된 작업을 모두 취소한다.
- 작업 이름에는 owner ID나 이메일 원문을 넣지 않고 계정 키의 SHA-256 일부만 사용한다.
- 네트워크 연결과 배터리 부족 아님 조건을 만족할 때만 실행한다.
- 서버 복구본 삭제 maintenance 중에는 백그라운드 작업을 시작하지 않는다.
- 통화 녹음·휴대폰 전체 연락처·전체 문자함은 여전히 동기화 대상이 아니다.

## WorkManager 구성

사용 라이브러리:

```gradle
implementation 'androidx.work:work-runtime:2.11.2'
```

### 주기 작업

- 최소 주기: 15분
- `NetworkType.CONNECTED`
- `requiresBatteryNotLow=true`
- 계정별 unique periodic work
- `ExistingPeriodicWorkPolicy.UPDATE`
- 일시적 오류는 exponential backoff

Android가 배터리·Doze·제조사 정책을 고려해 실행하므로 정확히 15분마다 실행된다는 의미는 아니다. 15분은 WorkManager 주기 작업의 최소 간격이며 실제 실행 시각은 시스템이 조정한다.

### 즉시 작업

다음 시점에는 계정별 unique one-time work를 예약한다.

- 데이터 보호를 처음 켰을 때
- 앱이 백그라운드로 내려갔을 때
- 기기 재부팅 후
- 앱 업데이트 후 `MY_PACKAGE_REPLACED`

`ExistingWorkPolicy.KEEP`을 사용해 같은 계정의 즉시 작업이 중복으로 쌓이지 않게 한다.

## 실행 흐름

```text
WorkManager 조건 충족
→ 로그인·동의 재확인
→ maintenance 여부 확인
→ CallTagSyncManager 실행 요청
→ status readiness
→ bootstrap 또는 로컬 변경 scan
→ push
→ pull
→ 최종 상태 확인
```

Worker는 실행 요청 후 실제 동기화가 종료될 때까지 기다린다. 최대 대기시간은 8분이다.

## 재시도 정책

재시도:

- 네트워크·서버 일시 오류
- rate limit으로 `WAITING`
- 일반 `ERROR`
- Worker 중단 또는 제한시간 초과

자동 재시도하지 않음:

- 서버 기능 준비 중
- 로그인 재확인 필요
- 데이터 충돌
- 데이터 보호 OFF
- 로그아웃
- 서버 복구본 삭제 maintenance

충돌은 반복 재시도로 해결되지 않기 때문에 배터리와 API 요청만 낭비하지 않도록 성공 종료하고, 후속 충돌 해결 UI에서 사용자가 선택하게 한다.

## 계정 격리

WorkManager unique name은 다음 원문을 사용하지 않는다.

- owner ID
- 이메일
- 전화번호
- 고객정보

`CallTagSyncLocalStore.accountKey()`를 SHA-256 처리한 일부 값만 작업 이름에 사용한다.

로그인 계정이 변경되면 기존 tag 작업을 취소하고 새 계정 작업을 등록한다.

## 생명주기

### 앱 시작

`CallTagApplication.onCreate()`에서 예약 상태를 현재 로그인·동의와 맞춘다.

### 앱 백그라운드

마지막 Activity가 멈추면 one-time work를 예약한다. 앱 사용 중 변경된 고객·메모·일정이 foreground UI를 지연시키지 않고 백그라운드에서 반영된다.

### 재부팅·업데이트

`BootReceiver`가 데이터 복구·무결성 검사 후 WorkManager 예약을 재확인하고 즉시 작업을 요청한다. WorkManager 자체도 재부팅 지속성을 제공하지만 앱 설정과 로그인 상태를 다시 검증하기 위해 명시적으로 reconcile한다.

### 로그인

`AuthSessionStore.save()` 완료 후 예약을 재확인한다. 기존에 해당 계정의 데이터 보호 동의가 켜져 있으면 주기 작업이 다시 등록된다.

### 로그아웃

`AuthSessionStore.clear()`가 세션 삭제 전에 모든 secure-sync tag 작업을 취소한다.

### 데이터 보호 OFF

계정별 동의 저장 후 모든 secure-sync tag 작업을 취소한다. 서버 복구본 자체는 삭제하지 않는다.

## 주요 파일

- `CallTagSyncWorkScheduler.java`
- `CallTagSyncWorker.java`
- `CallTagSyncPreferenceStore.java`
- `CallTagApplication.java`
- `BootReceiver.java`
- `AuthSessionStore.java`
- `app/build.gradle`

## 완료 기준

- 사용자 동의 OFF에서 WorkManager 예약 없음
- 로그인 없이 예약 없음
- 동의 ON에서 계정별 periodic work 1개
- 앱 백그라운드 전환 시 immediate work 중복 없이 1개
- 로그아웃 후 모든 secure-sync work 취소
- 재부팅·업데이트 후 예약 복구
- maintenance 중 신규 work 차단
- 네트워크 없음·배터리 부족 시 실행 보류
- 일시적 오류 exponential retry
- 충돌은 반복 retry 금지
- Java 17 컴파일 및 Debug APK 빌드 성공

## 남은 다음 작업

- 충돌 목록과 고객별 비교 화면
- `이 기기 내용 유지` / `다른 기기 내용 적용`
- WorkManager 실행 시각과 다음 예약 상태 UI
- 실제 staging에서 Doze·오프라인·재부팅 실기기 검증
