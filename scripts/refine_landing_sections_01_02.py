from pathlib import Path
import re

p = Path('index.html')
s = p.read_text(encoding='utf-8')

# Replace the over-designed HERO/pain CSS with the existing CallTag visual language:
# centered oversized type, dark surface, blue accent, minimal copy.
css_pattern = re.compile(r'    \.header-cta\{.*?(?=    \.hero-heading,\.web-heading-copy)', re.S)
css_replacement = '''    .header-cta{min-height:40px;display:inline-flex;align-items:center;justify-content:center;padding:0 15px;border-radius:10px;background:var(--blue);color:#fff;font-size:12px;font-weight:900;white-space:nowrap;transition:.2s ease}
    .header-cta:hover{background:#315fdc;transform:translateY(-1px)}

    .hero-app{padding:138px 0 94px}
    .hero-simple{text-align:center;position:relative;z-index:1}
    .hero-simple .hero-kicker{margin:0 0 18px;color:var(--blue-2);font-size:18px;font-weight:900;letter-spacing:-.03em}
    .hero-simple h1{margin:0 auto;max-width:1120px;font-size:clamp(60px,7.6vw,106px);line-height:.96;letter-spacing:-.082em}
    .hero-simple h1 span{color:var(--blue-2)}
    .hero-simple .hero-lead{margin:25px auto 0;max-width:660px;color:var(--muted);font-size:clamp(17px,1.65vw,21px);line-height:1.55;letter-spacing:-.025em}
    .hero-actions{display:flex;justify-content:center;align-items:center;gap:10px;margin-top:30px}
    .hero-primary{min-height:52px;display:inline-flex;align-items:center;justify-content:center;padding:0 22px;border-radius:12px;background:var(--blue);color:#fff;font-size:14px;font-weight:900;transition:.2s ease}
    .hero-primary:hover{background:#315fdc;transform:translateY(-2px)}
    .hero-secondary{min-height:52px;display:inline-flex;align-items:center;justify-content:center;padding:0 18px;color:#c4c8d1;font-size:13px;font-weight:800}
    .hero-support{display:flex;justify-content:center;align-items:center;gap:8px;margin-top:15px;color:var(--muted-2);font-size:11px;font-weight:750}
    .hero-support i{width:3px;height:3px;border-radius:50%;background:#5e6572}

    .pain-section{padding:86px 0 96px;border-bottom:1px solid var(--line);background:#0b0d11}
    .pain-head{text-align:center}.pain-kicker{margin:0 0 12px;color:var(--blue-2);font-size:13px;font-weight:900}
    .pain-head h2{margin:0;font-size:clamp(42px,4.7vw,66px);line-height:1.05;letter-spacing:-.07em}
    .pain-strip{display:grid;grid-template-columns:repeat(3,1fr);gap:1px;margin-top:38px;overflow:hidden;border:1px solid var(--line);border-radius:18px;background:var(--line)}
    .pain-item{min-height:128px;display:flex;flex-direction:column;justify-content:center;align-items:center;padding:22px;background:#111319;text-align:center}
    .pain-item b{font-size:24px;letter-spacing:-.045em}.pain-item span{margin-top:8px;color:var(--muted-2);font-size:13px}
    .pain-conclusion{margin-top:24px;text-align:center;color:#d9dde5;font-size:17px;font-weight:850}
    .pain-conclusion strong{color:var(--blue-2)}
    .call-flow-shell{padding:112px 0 120px;border-bottom:1px solid var(--line)}.call-flow-shell .phone-stage{margin-top:0}

'''
if not css_pattern.search(s):
    raise SystemExit('target css block not found')
s = css_pattern.sub(css_replacement, s, count=1)

hero_pattern = re.compile(r'    <section class="hero hero-app" id="app">.*?\n    <section class="pain-section" id="pain">', re.S)
hero_replacement = '''    <section class="hero hero-app" id="app">
      <div class="wrap">
        <div class="hero-simple reveal">
          <p class="hero-kicker">통화 후 고객관리</p>
          <h1>전화가 끝난 뒤,<br><span>고객관리는 시작됩니다.</span></h1>
          <p class="hero-lead">고객 · 상담 · 다음 할 일. 통화 직후 바로 남기세요.</p>
          <div class="hero-actions">
            <a class="hero-primary" href="https://pagero.kr/app" target="_blank" rel="noopener">7일 무료로 시작하기</a>
            <a class="hero-secondary" href="#call-flow">사용방법 보기 ↓</a>
          </div>
          <div class="hero-support"><span>Android 앱</span><i></i><span>PC 웹 연동</span></div>
        </div>
      </div>
    </section>

    <section class="pain-section" id="pain">'''
if not hero_pattern.search(s):
    raise SystemExit('hero block not found')
s = hero_pattern.sub(hero_replacement, s, count=1)

pain_pattern = re.compile(r'    <section class="pain-section" id="pain">.*?\n    <section class="call-flow-shell" id="call-flow">', re.S)
pain_replacement = '''    <section class="pain-section" id="pain">
      <div class="wrap">
        <div class="pain-head reveal">
          <p class="pain-kicker">통화 뒤에 남는 일</p>
          <h2>기억으로 관리하면<br>하나씩 놓칩니다.</h2>
        </div>
        <div class="pain-strip reveal">
          <div class="pain-item"><b>누구였지?</b><span>상담 내용</span></div>
          <div class="pain-item"><b>언제 다시?</b><span>재연락 일정</span></div>
          <div class="pain-item"><b>보냈나?</b><span>자료 · 견적</span></div>
        </div>
        <div class="pain-conclusion reveal">기억 말고, <strong>콜태그에 남기세요.</strong></div>
      </div>
    </section>

    <section class="call-flow-shell" id="call-flow">'''
if not pain_pattern.search(s):
    raise SystemExit('pain block not found')
s = pain_pattern.sub(pain_replacement, s, count=1)

marker = '    @media(prefers-reduced-motion:reduce)'
responsive = '''    @media(max-width:820px){.pain-strip{grid-template-columns:1fr 1fr}.pain-item:last-child{grid-column:1/-1}.hero-simple h1{font-size:clamp(54px,12vw,78px)}}
    @media(max-width:560px){.hero-app{padding:104px 0 66px}.hero-simple .hero-kicker{font-size:14px}.hero-simple h1{font-size:49px}.hero-simple .hero-lead{max-width:320px;margin-top:20px;font-size:16px}.hero-actions{display:grid;grid-template-columns:1fr;margin-top:25px}.hero-primary,.hero-secondary{width:100%;min-height:50px}.pain-section{padding:68px 0 72px}.pain-head h2{font-size:39px}.pain-strip{grid-template-columns:1fr;margin-top:28px}.pain-item,.pain-item:last-child{grid-column:auto;min-height:98px}.pain-item b{font-size:22px}.pain-conclusion{font-size:16px}}
'''
if marker not in s:
    raise SystemExit('responsive marker missing')
s = s.replace(marker, responsive + marker, 1)

checks = [
    '고객 · 상담 · 다음 할 일. 통화 직후 바로 남기세요.',
    '기억 말고, <strong>콜태그에 남기세요.</strong>',
    '7일 무료로 시작하기',
    'Android 앱',
    'PC 웹 연동',
]
for token in checks:
    if token not in s:
        raise SystemExit(f'missing: {token}')
for banned in ['hero-preview reveal', '아까 그 고객,', '견적 보내준다고 했는데']:
    if banned in s:
        raise SystemExit(f'old verbose fragment remains: {banned}')

p.write_text(s, encoding='utf-8')
print('refined sections 01-02')
