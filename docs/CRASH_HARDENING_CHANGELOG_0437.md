# CallTag 0.43.7 Crash Hardening Changelog

- 공통 고객 진입 라우터 추가
- 팝업/알림/홈카드 customerId + phone fallback 적용
- 화면 실행 성공 전 기존 팝업 유지
- 홈 다단계 작업 별도 Activity 구조 유지 및 강제 재렌더 방지
- MainActivity synthetic `performClick()` 내비게이션 제거
- 최근 60건 로컬 crash/launch breadcrumb 기록
- MainActivity 렌더 경로와 고객/할 일 저장 경로 분리 원칙 고정
- 전역 UI launch debounce 적용
- P0 회귀 테스트 3건 추가
- API 35 에뮬레이터 기반 connectedDebugAndroidTest CI 추가
