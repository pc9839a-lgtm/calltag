# 콜태그 랜딩 런타임 구조

## 정본

콜태그 공개 랜딩의 운영 정본은 저장소 루트 기준으로 관리합니다.

- HTML: `/index.html`
- Cloudflare Worker: `/_worker.js`
- 런타임 로더: `/assets/calltag-runtime-loader.js`
- 런타임 자산: `/assets/calltag-*.js`, `/assets/calltag-*.css`
- 공개 도메인: `https://calltag.pagero.kr/`

`/web` 아래에는 과거 배포 구조와 동기화를 위해 만들어진 미러가 남아 있습니다. 운영 수정 시 루트와 `/web`을 혼동하지 않습니다. 실제 배포 설정을 변경하지 않는 한 루트를 우선 정본으로 봅니다.

## 2026-08-07 단순화 1단계

기존 `/_worker.js`가 30개 이상의 UI 패치 스크립트를 직접 HTML에 주입하던 구조를 변경했습니다.

현재 Worker 책임:

1. 구형 URL 리다이렉트
2. robots/sitemap 응답 헤더
3. SEO 메타/구조화 데이터 적용
4. `calltag-enhance.css` 연결
5. `calltag-runtime-loader.js` 1개 연결

실제 UI 패치 스크립트 목록과 실행 순서는 `assets/calltag-runtime-loader.js` 한 곳에서 관리합니다.

## 아직 남은 기술 부채

현재 최종 화면은 다수의 과거 패치 스크립트가 순차 실행된 결과입니다. 다음 항목은 아직 제거하지 않았습니다.

- DOM 섹션 재배치용 MutationObserver
- 일정 시간마다 재실행하는 보정 타이머
- 과거 스크립트가 만든 DOM을 후속 스크립트가 다시 덮는 구조
- JS 내부에서 동적으로 `<style>`을 삽입하는 방식
- 페이지로 섹션, 가격표, 모바일 보정 코드의 다단계 후처리

디자인을 유지해야 하므로 검증 없이 기존 패치 파일을 일괄 삭제하지 않습니다.

## 다음 단순화 순서

1. 현재 운영 화면의 최종 DOM/스타일을 정본으로 확정
2. 섹션 순서를 `index.html`에 직접 고정
3. 페이지로/가격/기능 카드처럼 런타임 생성되는 영역을 정적 HTML로 이동
4. 동적으로 생성되는 CSS를 통합 CSS로 이동
5. 실제 인터랙션만 `calltag.js`에 남김
6. MutationObserver와 반복 타이머 제거
7. 미사용 과거 패치 자산 삭제

최종 목표:

```text
index.html
assets/calltag.css
assets/calltag.js
_worker.js
```

모바일 전용 파일이 분리될 필요가 있더라도 핵심 런타임 파일은 4~6개 수준을 목표로 합니다.

## 원복 기준

- 구조 단순화 시작 전: `backup/runtime-before-simplify-20260807-2323`
- 단순화 1단계 안정 기준: `backup/runtime-simplified-stage1-20260807-2341`

화면 이상이 생기면 먼저 `backup/runtime-simplified-stage1-20260807-2341`로 되돌리고, 필요 시 전체 구조를 `backup/runtime-before-simplify-20260807-2323` 기준으로 복원합니다.
