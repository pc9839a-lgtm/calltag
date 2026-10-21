# 콜태그 Google 색인 확인 체크리스트 — 2026-10-22

## 현재 확인된 사실
- 운영 사이트: `https://calltag.pagero.kr/`
- GitHub Actions 운영 검사에서 홈페이지·가이드 허브·개별 가이드 등 17개 URL의 HTTP 200, canonical, noindex 부재가 확인됨 (2026-10-21).
- 기존 Search Console 캡처에서 색인된 페이지 2개, 클릭 3회가 보고됨. 이 수치는 최신 상태를 의미하지 않음.
- GSC Wizard는 현재 구독 제한으로 URL 검사와 사이트맵 제출 요청을 수행하지 못함. 제출 여부를 코드 검사로 추정하지 말 것.
- 2026-10-22: 홈페이지에서 중요 실무 가이드 5개를 직접 연결하는 HTML 섹션을 추가하고 sitemap의 홈페이지 lastmod를 갱신함.

## Search Console에서 실제로 해야 할 순서
1. URL-prefix 속성 `https://calltag.pagero.kr/` 또는 등록된 도메인 속성을 연다.
2. **색인 생성 → 사이트맵**에서 `https://calltag.pagero.kr/sitemap.xml`의 제출 여부, 마지막 읽은 날짜, 성공/오류, 발견 URL 수를 기록한다. 아직 등록되지 않았다면 해당 URL을 한 번 제출한다.
3. **URL 검사**에서 아래 6개 페이지를 순서대로 검사하고, 아래 항목을 기록한다.
   - Google에 등록됨 / Google에 등록되지 않음
   - 페이지 색인 생성의 구체적 사유
   - Google이 마지막으로 크롤링한 날짜
   - 사용자 선언 canonical / Google 선택 canonical
   - 사이트맵 발견 여부
4. 색인되지 않은 페이지에서 **실제 URL 테스트**를 수행한다. 문제없는 소수 핵심 페이지에 한해 색인 생성 요청을 사용하고 같은 URL을 반복 제출하지 않는다.
5. **실적 → 검색결과**에서 28일 기준 노출수·클릭수·CTR·평균 순위, 검색어, 페이지별 수치를 확보한다. 리포트에는 최종 데이터 날짜를 기재한다.

## 우선 검사할 페이지
| 우선순위 | URL | 주제 |
|---|---|---|
| 1 | `https://calltag.pagero.kr/` | 서비스 홈 |
| 2 | `https://calltag.pagero.kr/guide/` | 가이드 허브 |
| 3 | `https://calltag.pagero.kr/guide/customer-management-app/` | 고객관리 프로그램 선택 |
| 4 | `https://calltag.pagero.kr/guide/after-call-customer-management/` | 통화 후 고객관리 |
| 5 | `https://calltag.pagero.kr/guide/phone-consultation-record/` | 전화 상담일지 |
| 6 | `https://calltag.pagero.kr/guide/follow-up-schedule-management/` | 재연락·팔로업 |

영업 CRM 가이드: `https://calltag.pagero.kr/guide/sales-customer-management/`도 다음 순서로 확인.

## 원인별 대응
| GSC 상태 | 다음 조치 |
|---|---|
| 발견됨 - 현재 색인이 생성되지 않음 | 사이트맵 제출 및 HTML 내부 링크 발견 경로 확인, 이후 재평가 |
| 크롤링됨 - 현재 색인이 생성되지 않음 | 다른 페이지와의 정보 중복, 독창적 실무 예시, 검색 의도 충족 여부 조사 |
| 중복, Google에서 다른 표준 페이지를 선택 | Google 선택 canonical과 응답·리다이렉트를 비교해 실제 중복 원인을 해결 |
| robots.txt에 의해 차단됨 | 특정 URL의 차단 경로와 운영 응답 확인 |
| noindex로 제외됨 | HTML meta와 X-Robots-Tag 실제 응답 확인 |
| 색인 생성됨 | 검색 실적의 쿼리·노출·순위·CTR 최적화로 전환 |

**중요:** 사이트맵 제출 또는 URL 검사상 '색인 생성 가능'은 실제 Google 색인이나 검색 순위 상승을 보장하지 않는다. Google이 반환한 색인 사유를 확보하기 전에는 제외 원인을 확정하지 않는다.
