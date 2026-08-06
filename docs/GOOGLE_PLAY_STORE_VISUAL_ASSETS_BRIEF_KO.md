# 콜태그 Google Play 스토어 시각 애셋 제작 가이드

최종 업데이트: 2026-08-06
대상 앱: 콜태그 - 통화 후 고객관리
패키지: `kr.pagero.calltag`

## 1. 문서 목적

이 문서는 디자이너 또는 이미지 생성 AI가 콜태그 Google Play 스토어용 앱 아이콘, 그래픽 이미지, 휴대전화 스크린샷, 태블릿 스크린샷을 일관된 형태로 제작하기 위한 기준서다.

AI가 앱 화면을 새로 상상해 그리는 작업이 아니다. 실제 콜태그 앱 화면을 원본으로 사용하고, AI는 배경·프레임·레이아웃·강조 요소만 제작한다. 실제 앱에 없는 기능, 버튼, 통계, 결제 화면, 알림 화면을 만들어 넣으면 안 된다.

## 2. 서비스 핵심 정보

콜태그는 통화가 끝난 뒤 고객을 등록하고 태그, 상담 메모, 다음 할 일, 후속 일정, 문자 발송 내역을 관리하는 Android 고객관리 앱이다.

핵심 가치:

- 전화번호만 남는 통화기록을 다시 연락할 수 있는 고객정보로 바꾼다.
- 통화 종료 직후 고객 상태와 상담 내용을 남긴다.
- 오늘 해야 할 재연락 일정과 미완료 업무를 확인한다.
- 고객별 통화·메모·문자·일정 이력을 한곳에서 확인한다.
- 문자 템플릿과 후속 문자 기능으로 반복 업무를 줄인다.
- 통화 내용을 녹음하거나 듣는 앱이 아니다.

주요 사용자:

- 보험설계사
- 부동산 중개업자
- 병원·미용실·학원·예약매장
- 자동차·법무·상담업 종사자
- 전화 상담 후 고객을 다시 관리해야 하는 1인 사업자와 소규모 팀

## 3. 브랜드 시각 기준

앱 코드 기준 색상:

| 역할 | 색상 |
|---|---|
| 메인 블루 | `#4389FF` |
| 딥 블루 | `#2F73E8` |
| 메인 배경 | `#101113` |
| 카드 배경 | `#1C1E22` |
| 보조 배경 | `#16181B` |
| 메인 텍스트 | `#F4F5F7` |
| 보조 텍스트 | `#A8ADB5` |
| 약한 텍스트 | `#747A84` |
| 경계선 | `#292C31` |
| 오류 강조 | `#FF7A7A` |

권장 스타일:

- 전체 분위기: 신뢰감 있는 업무용 SaaS, 현대적인 Android 생산성 앱
- 기본 배경: 짙은 차콜 또는 검정
- 포인트: 콜태그 블루 한 가지를 중심으로 사용
- 글자: 흰색과 회색 위주
- 폰트: Pretendard 또는 Noto Sans KR 계열
- 이미지 구성: 단순하고 명확하게, 작은 장식 최소화
- 화면 수: 한 애셋에 핵심 앱 화면 1개 또는 최대 2개
- 사용자 선호에 맞춰 텍스트는 최소화하고 중앙 정렬 또는 명확한 좌우 분할 사용

## 4. 절대 준수 규칙

1. 실제 앱 스크린샷을 원본으로 사용한다.
2. AI가 앱 UI를 다시 그리거나 버튼·숫자·메뉴를 임의로 바꾸지 않는다.
3. 앱에 없는 기능을 추가하지 않는다.
4. 실제 고객명, 실제 전화번호, 실제 이메일, 실제 상담 내용을 노출하지 않는다.
5. 테스트 데이터는 `테스트 고객`, `010-0000-0000`처럼 명확한 가상 정보만 사용한다.
6. `1위`, `최고`, `무료`, `평생`, `무조건`, `수익 보장` 같은 과장 문구를 넣지 않는다.
7. 파트너 현금 보상은 스토어 대표 애셋에서 핵심 기능처럼 강조하지 않는다.
8. Google Play 로고, 별점, 리뷰 수, 다운로드 수를 임의로 넣지 않는다.
9. 시스템 알림처럼 보이는 가짜 배지, 빨간 알림점, 허위 팝업을 넣지 않는다.
10. 휴대전화 스크린샷을 태블릿 비율로 단순 확대하거나 늘리지 않는다.
11. 모든 애셋은 같은 배경색, 같은 블루, 같은 폰트, 같은 여백 체계를 사용한다.
12. 이미지 생성 AI가 한글을 깨뜨릴 수 있으므로 배경과 프레임을 먼저 만들고, 한글 문구는 Canva·Figma·Photoshop에서 정확히 입력한다.
13. 실제 앱 화면의 개인정보는 생성 전에 가리고, 생성 후에도 한 번 더 검수한다.
14. 화면 안의 앱 버전, 테스트 계정, 디버그 메뉴, 내부 테스트 문구가 보이지 않게 한다.

## 5. 필요한 원본 자료

다른 AI나 디자이너에게 아래 파일을 함께 전달한다.

- 현재 콜태그 앱 아이콘 원본 또는 로고 원본
- 통화 종료 후 고객관리 화면 스크린샷
- 오늘 할 일 화면 스크린샷
- 고객 상세·상담 이력 화면 스크린샷
- 문자 템플릿·문자 발송 화면 스크린샷
- 후속 일정·캘린더 화면 스크린샷
- 통계 화면 스크린샷
- 사용 중인 앱 색상표
- 이 문서

원본 스크린샷은 가능하면 1080×1920 이상의 Android 실제 기기 또는 에뮬레이터 캡처를 사용한다.

## 6. 제작 대상 및 파일명

| 구분 | 권장 크기 | 파일명 |
|---|---:|---|
| 앱 아이콘 | 512×512 | `calltag_play_icon_512.png` |
| 그래픽 이미지 | 1024×500 | `calltag_feature_graphic_1024x500.png` |
| 휴대전화 01 | 1080×1920 | `calltag_phone_01_postcall.png` |
| 휴대전화 02 | 1080×1920 | `calltag_phone_02_today.png` |
| 휴대전화 03 | 1080×1920 | `calltag_phone_03_customer.png` |
| 휴대전화 04 | 1080×1920 | `calltag_phone_04_message.png` |
| 휴대전화 05 | 1080×1920 | `calltag_phone_05_schedule.png` |
| 휴대전화 06 | 1080×1920 | `calltag_phone_06_stats.png` |
| 7인치 태블릿 01~04 | 실제 에뮬레이터 비율 | `calltag_tablet7_01.png` 등 |
| 10인치 태블릿 01~04 | 실제 에뮬레이터 비율 | `calltag_tablet10_01.png` 등 |

## 7. 앱 아이콘 제작 지시

### 디자인 방향

- 텍스트 없는 단순한 심볼
- 전화 수화기와 태그 라벨을 결합한 형태
- 짙은 배경 위에 흰색 또는 블루 심볼
- 작은 크기에서도 전화관리 앱임을 알아볼 수 있어야 함
- 복잡한 그림자, 실사 질감, 너무 얇은 선 금지
- 아이콘 테두리 안쪽에 충분한 여백 유지

### AI 생성 프롬프트

```text
Google Play app icon for a Korean Android productivity app named CallTag. Create one simple geometric symbol combining a phone receiver and a customer tag label. Dark charcoal background, vivid blue accent #4389FF, white foreground, high contrast, clean modern SaaS style, flat vector design, centered composition, thick readable shapes, no text, no letters, no numbers, no badge, no notification dot, no mockup, no 3D object, no photographic texture. Square 512 by 512.
```

### 네거티브 프롬프트

```text
Korean text, English text, letters, numbers, coins, cash, people, hands, office photo, gradient rainbow, excessive glow, thin lines, small details, app-store badge, Google Play logo, star rating, notification badge, 3D mockup
```

기존 콜태그 공식 아이콘이 이미 확정되어 있다면 새 아이콘을 만들지 말고, 기존 아이콘을 512×512 고해상도 버전으로 정리한다.

## 8. 그래픽 이미지 제작 지시

### 규격

- 1024×500
- JPEG 또는 투명 배경 없는 PNG
- 내부 안전 여백: 좌우 약 70px, 상하 약 45px

### 구성

- 배경: `#101113`
- 왼쪽 42%: 메인 문구
- 오른쪽 58%: 실제 콜태그 화면이 들어간 Android 휴대전화 2대
- 앞쪽 휴대전화: 통화 종료 후 태그 화면
- 뒤쪽 휴대전화: 오늘 할 일 또는 고객 목록 화면
- 휴대전화 화면은 반드시 실제 스크린샷을 합성
- 배경 장식은 얇은 블루 라인, 작은 태그 모양, 은은한 블루 빛 정도만 사용

### 최종 문구

```text
통화가 끝나면
고객관리가 시작됩니다
```

문구는 이미지 생성 AI에서 생성하지 말고, 배경 생성 후 디자인 도구에서 직접 입력한다.

### AI 생성 프롬프트

```text
Create a premium Google Play feature graphic background for a Korean Android customer management app. Canvas 1024 by 500. Dark charcoal background #101113 with restrained blue accents #4389FF. Clean modern B2B SaaS visual, subtle flowing line suggesting a phone call becoming organized customer data, spacious layout, strong empty text area on the left, space for two realistic Android phone mockups on the right. Do not draw or invent app user interfaces; leave the phone screen areas ready for real screenshots. Minimal, high contrast, professional, no people, no office stock photo, no cash, no coins, no rewards, no text, no letters, no ratings, no Google Play logo.
```

## 9. 휴대전화 스크린샷 공통 레이아웃

권장 캔버스: 1080×1920 세로형

레이아웃 기준:

- 상단 20~24%: 제목과 짧은 보조 문구
- 중앙·하단 70~76%: 실제 앱 화면
- 배경: `#101113`
- 포인트 라인 또는 작은 도형: `#4389FF`
- 제목: 흰색, 굵게
- 보조 문구: `#A8ADB5`
- 실제 앱 화면을 가리는 장식 금지
- 제목은 최대 2줄
- 보조 설명은 최대 1줄 또는 2줄
- 여섯 장을 나란히 놓았을 때 같은 시리즈처럼 보여야 함

### 공통 AI 프롬프트

```text
Design a vertical Google Play phone screenshot marketing frame, 1080 by 1920, for a professional Korean Android CRM app. Dark charcoal background #101113, blue accent #4389FF, minimal modern SaaS design, large clean headline area at the top and one large real app screenshot area below. Do not redraw the app UI. Preserve the supplied screenshot exactly. Add only framing, spacing, subtle blue shapes and a soft shadow. No people, no stock photography, no fake phone UI, no fake buttons, no text generated inside the app screenshot, no ratings, no price, no cash, no coins, no Google Play logo.
```

## 10. 휴대전화 스크린샷 6장 구성

### 01. 통화 종료 후 고객관리

메인 문구:

```text
통화가 끝나면
태그만 하세요
```

보조 문구:

```text
고객 구분·메모·다음 할 일을 바로 남깁니다
```

사용 화면: 통화 종료 후 큰 고객관리 팝업 또는 상세 오버레이

강조 요소: 신규/기존 태그, 상담 상태, 메모, 다음 할 일

### 02. 오늘 할 일

메인 문구:

```text
오늘 할 일을
바로 확인하세요
```

보조 문구:

```text
재연락 일정과 미완료 업무를 놓치지 않습니다
```

사용 화면: 홈의 오늘 할 일 목록

강조 요소: 예정 시간, 고객명, 완료·미루기 동작

### 03. 고객별 상담 이력

메인 문구:

```text
고객별 상담 이력을
한눈에
```

보조 문구:

```text
통화·메모·상태·일정을 한곳에서 확인합니다
```

사용 화면: 고객 상세 또는 상담 이력 화면

강조 요소: 고객명, 연락처, 메모 요약, 최근 연락, 상태

### 04. 문자 관리

메인 문구:

```text
필요한 문자를
빠르게 발송
```

보조 문구:

```text
템플릿·예약·후속 문자로 반복 업무를 줄입니다
```

사용 화면: 문자 템플릿 선택 또는 문자 발송 화면

강조 요소: 템플릿 이름, 문자 미리보기, 발송 버튼

### 05. 후속 일정

메인 문구:

```text
재연락 일정까지
놓치지 않게
```

보조 문구:

```text
고객별 후속 일정과 캘린더를 함께 관리합니다
```

사용 화면: 콜태그 내부 캘린더 또는 후속 일정 목록

강조 요소: 날짜, 고객명, 일정 상태

### 06. 통계

메인 문구:

```text
상담 흐름을
통계로 확인
```

보조 문구:

```text
기간별 고객·상담 현황을 빠르게 파악합니다
```

사용 화면: 통계 화면

강조 요소: 오늘·7일·30일 필터, 핵심 지표, 그래프

## 11. 선택 가능한 추가 스크린샷

실제 배포 버전에서 완전히 작동하는 것이 확인된 경우에만 추가한다.

### 07. 페이지로 문의 연동

```text
문의가 들어오는 순간
고객으로 자동 등록
```

실시간 서버 연동과 알림이 실제 제출 AAB에서 동작하는 경우에만 사용한다.

### 08. 데이터 보호·복구

```text
기기를 바꿔도
고객정보를 안전하게
```

서버 동기화 기능이 실제 운영 환경에서 활성화되고 사용자가 직접 켤 수 있는 경우에만 사용한다.

파트너 현금 보상 화면은 앱의 핵심 고객관리 가치보다 우선하지 않으므로 기본 6장에는 넣지 않는다.

## 12. 태블릿 애셋 제작 기준

- Android Studio의 7인치·10인치 에뮬레이터에서 실제 앱을 실행해 캡처한다.
- 휴대전화 스크린샷을 확대하거나 좌우로 늘리지 않는다.
- 화면이 휴대전화 폭으로 고정되어 빈 공간이 큰 경우, 빈 공간을 임의 UI로 채우지 않는다.
- 실제 태블릿 레이아웃이 완성되지 않았다면 앱 화면을 중앙에 배치하고 주변 배경만 브랜드 색으로 정돈한다.
- 추천 화면: 오늘 할 일, 고객 목록, 고객 상세, 통계 총 4장

## 13. 테스트 데이터 기준

사용 가능한 예시:

```text
테스트 고객 01
010-0000-0000
상담 내용 확인 예정
내일 오후 2시 재연락
견적서 전달 후 검토 중
```

사용 금지:

- 실제 고객 이름
- 실제 휴대전화 번호
- 실제 주소
- 실제 보험·계약 정보
- 실제 이메일 주소
- 검토 계정 아이디나 비밀번호

## 14. 이미지 생성 AI에 전달할 마스터 지시문

```text
콜태그는 통화 종료 후 고객을 등록하고 태그, 상담 메모, 오늘 할 일, 후속 일정, 문자 발송 내역을 관리하는 한국어 Android 고객관리 앱이다. Google Play 스토어용 시각 애셋을 제작한다. 브랜드는 짙은 차콜 배경 #101113, 카드 #1C1E22, 메인 블루 #4389FF, 흰색 텍스트 #F4F5F7을 사용한다. 전체 스타일은 현대적이고 신뢰감 있는 B2B SaaS이며 텍스트와 장식은 최소화한다. 실제 제공된 콜태그 앱 스크린샷은 픽셀과 UI 구조를 그대로 유지해야 하며, 앱 화면을 새로 그리거나 기능을 상상해 추가하면 안 된다. 실제 고객 개인정보는 모두 가상 데이터로 교체한다. 가격, 무료, 1위, 평점, 리뷰, 현금 보상, 수익 보장, Google Play 로고, 알림 배지, 가짜 시스템 팝업을 넣지 않는다. 이미지 생성 단계에서는 한글 문구를 만들지 말고 문구가 들어갈 안전 영역만 남긴다. 최종 한글은 디자인 도구에서 정확히 입력한다.
```

## 15. 최종 검수 체크리스트

- [ ] 앱 아이콘이 512×512이고 작은 크기에서도 구분되는가
- [ ] 그래픽 이미지가 1024×500이고 투명 배경이 없는가
- [ ] 휴대전화 이미지는 1080×1920으로 통일했는가
- [ ] 실제 앱 UI가 변형되지 않았는가
- [ ] 제출 AAB에 없는 기능이 보이지 않는가
- [ ] 실제 개인정보가 남아 있지 않은가
- [ ] 한글 오탈자와 깨진 글자가 없는가
- [ ] 여섯 장의 폰트·색상·여백이 일치하는가
- [ ] 가격·무료·순위·별점·현금보상 홍보 문구가 없는가
- [ ] 앱 화면의 핵심 버튼과 정보가 장식에 가려지지 않는가
- [ ] 태블릿 화면이 단순 확대본이 아닌 실제 캡처인가
- [ ] PNG 또는 JPEG로 올바르게 내보냈는가
- [ ] 앱 아이콘 파일이 1MB 이하인가
- [ ] 각 스크린샷 파일이 8MB 이하인가

## 16. 최종 납품 폴더 구조

```text
play-store-assets/
├─ source/
│  ├─ logo/
│  └─ raw-screenshots/
├─ icon/
│  └─ calltag_play_icon_512.png
├─ feature/
│  └─ calltag_feature_graphic_1024x500.png
├─ phone/
│  ├─ calltag_phone_01_postcall.png
│  ├─ calltag_phone_02_today.png
│  ├─ calltag_phone_03_customer.png
│  ├─ calltag_phone_04_message.png
│  ├─ calltag_phone_05_schedule.png
│  └─ calltag_phone_06_stats.png
├─ tablet-7/
├─ tablet-10/
└─ editable/
   └─ 원본 편집 파일
```
