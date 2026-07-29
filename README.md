# 콜태그 (CallTag)

통화가 끝나면 신규 고객, 기존 고객, 상담 상태와 다음 할 일이 자동으로 정리되는 Android 기반 통화 고객관리 앱입니다.

## 제품 한 문장

> 전화가 오면 고객을 알아보고, 통화가 끝나면 고객 상태와 다음 행동을 기록한다.

## 현재 단계

- 제품 기획 및 데이터 구조 문서화
- Android MVP 기반 구축
- 패키지명: `kr.pagero.calltag`
- 최소 Android: API 26
- 개발 브랜치: `agent/calltag-foundation`

## 핵심 문서

- `docs/PRODUCT_SPEC_KO.md` — 제품 정의와 MVP 범위
- `docs/CUSTOMER_CLASSIFICATION_KO.md` — 신규·기존 고객 자동분류 기준
- `docs/DATA_MODEL_KO.md` — 고객·상담건·후속업무 데이터 구조
- `docs/SCREEN_FLOW_KO.md` — 주요 화면과 통화 후 처리 흐름
- `docs/ROADMAP_KO.md` — 개발 단계와 출시 순서

## 핵심 원칙

1. 고객과 상담건을 분리한다.
2. 휴대폰 연락처 등록 여부만으로 기존 고객을 판단하지 않는다.
3. 통화 종료 후 10초 안에 처리가 끝나야 한다.
4. 다음 행동이 없는 상담을 자동으로 찾아낸다.
5. 초기 버전은 1인 영업자용으로 제한한다.
