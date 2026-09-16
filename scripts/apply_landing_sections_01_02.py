from pathlib import Path
import hashlib
import sys

INDEX = Path("index.html")
EXPECTED_BLOB = "6771850f90b6b512bbb843ffb347a7d7fba13ff7"

def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(f"blob {len(data)}\0".encode() + data).hexdigest()

def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {count}")
    return text.replace(old, new, 1)

raw = INDEX.read_bytes()
text = raw.decode("utf-8")

if 'class="pain-section"' in text and 'class="hero-preview"' in text:
    print("landing sections 01-02 already applied")
    sys.exit(0)

actual_blob = git_blob_sha(raw)
if actual_blob != EXPECTED_BLOB:
    raise RuntimeError(
        f"index.html baseline changed: expected {EXPECTED_BLOB}, got {actual_blob}. "
        "Refuse to patch an unknown landing baseline."
    )

text = text.replace(
    '<meta name="description" content="통화가 끝난 뒤 고객을 태그하고 상담 상태, 다음 할 일, 재연락 일정을 관리하는 Android 고객관리 서비스 콜태그." />',
    '<meta name="description" content="통화가 끝난 뒤 고객을 분류하고 상담 내용과 다음 할 일을 바로 남기는 Android 고객관리 서비스 콜태그. PC 웹에서도 고객과 후속 일정을 이어서 관리합니다." />'
)
text = text.replace(
    '<meta property="og:title" content="콜태그 | 통화 후 고객관리, 1명의 고객도 놓치지 않습니다" />',
    '<meta property="og:title" content="콜태그 | 전화가 끝난 뒤, 고객관리는 그때부터" />'
)
text = text.replace(
    '<meta property="og:description" content="통화 종료 후 태그만 하세요. 고객 상태와 다음 할 일이 바로 정리됩니다." />',
    '<meta property="og:description" content="통화 직후 고객을 분류하고 상담 내용과 다음 할 일을 바로 남기세요. Android 앱과 PC 웹에서 이어집니다." />'
)
text = text.replace(
    '<title>콜태그 | 통화 후 고객관리</title>',
    '<title>콜태그 | 통화 후 고객관리 CRM</title>'
)

old_nav_css = '''    .nav{margin-left:auto;display:flex;gap:26px}.nav a{color:var(--muted);font-size:14px;font-weight:760}.nav a:hover{color:#fff}
'''
new_nav_css = '''    .nav{margin-left:auto;display:flex;gap:26px}.nav a{color:var(--muted);font-size:14px;font-weight:760}.nav a:hover{color:#fff}
    .header-cta{min-height:42px;display:inline-flex;align-items:center;justify-content:center;padding:0 17px;border:1px solid rgba(124,153,255,.35);border-radius:12px;background:var(--blue);color:#fff;font-size:13px;font-weight:900;white-space:nowrap;box-shadow:0 8px 24px rgba(59,111,255,.2);transition:transform .2s ease,background .2s ease}
    .header-cta:hover{transform:translateY(-1px);background:#315fdc}
'''
text = replace_once(text, old_nav_css, new_nav_css, "header CTA CSS")

old_hero_css = '''    .hero{position:relative;overflow:hidden;border-bottom:1px solid var(--line)}
    .hero-app{padding:138px 0 120px}.hero-web{padding:124px 0 132px}
    .hero-app::before,.hero-web::before{content:"";position:absolute;width:900px;height:900px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.22),rgba(59,111,255,0) 68%);pointer-events:none}
    .hero-app::before{top:-540px;left:50%;transform:translateX(-50%)}.hero-web::before{right:-360px;bottom:-560px}
    .hero-heading,.web-heading-copy{text-align:center;position:relative;z-index:1}
    .hero-kicker{margin:0 0 20px;color:var(--blue-2);font-size:18px;font-weight:900;letter-spacing:-.03em}
    .hero h1{margin:0;font-size:clamp(62px,8.2vw,118px);line-height:.94;letter-spacing:-.085em}.hero h1 span{color:var(--blue-2)}
    .hero-heading>p{margin:26px auto 0;max-width:720px;color:var(--muted);font-size:clamp(17px,1.75vw,22px);line-height:1.55;letter-spacing:-.025em}
'''
new_hero_css = '''    .hero{position:relative;overflow:hidden;border-bottom:1px solid var(--line)}
    .hero-app{padding:132px 0 94px}.hero-web{padding:124px 0 132px}
    .hero-app::before,.hero-web::before{content:"";position:absolute;width:900px;height:900px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.22),rgba(59,111,255,0) 68%);pointer-events:none}
    .hero-app::before{top:-500px;left:36%;transform:translateX(-50%)}.hero-web::before{right:-360px;bottom:-560px}
    .hero-grid{position:relative;z-index:1;display:grid;grid-template-columns:minmax(0,1.08fr) minmax(360px,.92fr);gap:72px;align-items:center}
    .hero-copy{min-width:0}.hero-kicker{margin:0 0 20px;color:var(--blue-2);font-size:16px;font-weight:900;letter-spacing:-.03em}
    .hero-copy h1{margin:0;max-width:820px;font-size:clamp(52px,5.6vw,82px);line-height:1.02;letter-spacing:-.075em}
    .hero-copy h1 span{color:var(--blue-2)}
    .hero-lead{margin:26px 0 0;max-width:690px;color:#c9cdd6;font-size:clamp(18px,1.55vw,22px);line-height:1.65;letter-spacing:-.028em}
    .hero-actions{display:flex;flex-wrap:wrap;gap:12px;margin-top:34px}
    .hero-primary,.hero-secondary{min-height:56px;display:inline-flex;align-items:center;justify-content:center;padding:0 22px;border-radius:14px;font-size:15px;font-weight:900;transition:transform .2s ease,border-color .2s ease,background .2s ease}
    .hero-primary{background:var(--blue);color:#fff;box-shadow:0 14px 38px rgba(59,111,255,.24)}.hero-primary:hover{transform:translateY(-2px);background:#315fdc}
    .hero-secondary{border:1px solid var(--line-strong);background:rgba(255,255,255,.035);color:#e7e9ee}.hero-secondary:hover{transform:translateY(-2px);border-color:rgba(124,153,255,.45)}
    .hero-support{display:flex;align-items:center;gap:9px;margin-top:17px;color:var(--muted-2);font-size:12px;font-weight:750}.hero-support i{width:3px;height:3px;border-radius:50%;background:#5e6572}
    .hero-preview{position:relative;min-width:0;padding:22px;border:1px solid var(--line-strong);border-radius:28px;background:linear-gradient(155deg,rgba(28,31,39,.98),rgba(12,14,18,.98));box-shadow:0 36px 100px rgba(0,0,0,.44);overflow:hidden}
    .hero-preview::before{content:"";position:absolute;width:280px;height:280px;right:-130px;top:-130px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.2),rgba(59,111,255,0) 70%);pointer-events:none}
    .hero-preview-head{position:relative;display:flex;align-items:center;justify-content:space-between;gap:14px;padding:2px 2px 18px;border-bottom:1px solid var(--line)}
    .hero-preview-head strong{font-size:16px;letter-spacing:-.03em}.hero-preview-head span{padding:6px 9px;border-radius:999px;background:rgba(50,200,121,.1);color:#78dca5;font-size:10px;font-weight:850}
    .hero-preview-customer{display:flex;align-items:center;gap:13px;margin-top:20px;padding:17px;border:1px solid var(--line);border-radius:16px;background:rgba(255,255,255,.025)}
    .hero-preview-avatar{width:46px;height:46px;display:grid;place-items:center;flex:0 0 auto;border-radius:50%;background:var(--blue-soft);color:#b4c0ff;font-size:11px;font-weight:900}
    .hero-preview-customer strong{display:block;font-size:14px}.hero-preview-customer small{display:block;margin-top:5px;color:var(--muted-2);font-size:10px}
    .hero-preview-tags{display:flex;flex-wrap:wrap;gap:7px;margin-top:13px}.hero-preview-tags span{padding:7px 9px;border:1px solid rgba(59,111,255,.25);border-radius:999px;background:var(--blue-soft);color:#b7c2ff;font-size:10px;font-weight:800}
    .hero-preview-grid{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-top:14px}.hero-preview-field{padding:13px;border-radius:12px;background:#13161c}.hero-preview-field span{display:block;color:var(--muted-2);font-size:9px}.hero-preview-field b{display:block;margin-top:6px;font-size:11px}
    .hero-preview-save{min-height:50px;display:flex;align-items:center;justify-content:center;margin-top:14px;border-radius:12px;background:var(--blue);color:#fff;font-size:12px;font-weight:900}
    .hero-preview-note{margin:11px 2px 0;color:var(--muted-2);font-size:10px;line-height:1.5;text-align:center}

    .pain-section{position:relative;padding:108px 0 114px;border-bottom:1px solid var(--line);background:linear-gradient(180deg,#0d0f13,#090a0d)}
    .pain-head{max-width:760px}.pain-kicker{margin:0 0 15px;color:var(--blue-2);font-size:13px;font-weight:900}
    .pain-head h2{margin:0;font-size:clamp(44px,5vw,70px);line-height:1.06;letter-spacing:-.07em}
    .pain-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-top:48px}
    .pain-card{min-height:230px;display:flex;flex-direction:column;justify-content:space-between;padding:27px;border:1px solid var(--line);border-radius:22px;background:linear-gradient(145deg,#15181e,#101217);transition:transform .25s ease,border-color .25s ease}
    .pain-card:hover{transform:translateY(-4px);border-color:rgba(124,153,255,.35)}
    .pain-card span{color:var(--blue-2);font-size:11px;font-weight:900;letter-spacing:.02em}.pain-card strong{font-size:clamp(22px,2vw,30px);line-height:1.42;letter-spacing:-.045em}
    .pain-conclusion{display:flex;align-items:center;gap:12px;margin-top:28px;color:#dce0e8;font-size:18px;font-weight:850}.pain-conclusion i{width:34px;height:1px;background:var(--blue)}
    .call-flow-shell{padding:112px 0 120px;border-bottom:1px solid var(--line)}.call-flow-shell .phone-stage{margin-top:0}

    .hero-heading,.web-heading-copy{text-align:center;position:relative;z-index:1}
'''
text = replace_once(text, old_hero_css, new_hero_css, "hero CSS")

text = replace_once(
    text,
    '''    @media(max-width:1120px){
      .phone-stage{grid-template-columns:1fr;gap:36px}.step-panel{order:2}.phone-shell{width:min(430px,100%);margin:0 auto}''',
    '''    @media(max-width:1120px){
      .hero-grid{grid-template-columns:1fr;gap:44px}.hero-copy{max-width:860px}.hero-preview{width:min(620px,100%)}.pain-grid{grid-template-columns:1fr 1fr}.pain-card:last-child{grid-column:1/-1}
      .phone-stage{grid-template-columns:1fr;gap:36px}.step-panel{order:2}.phone-shell{width:min(430px,100%);margin:0 auto}''',
    "1120 responsive"
)
text = replace_once(
    text,
    '''    @media(max-width:820px){
      .wrap{width:min(var(--max),calc(100% - 32px))}.nav{display:none}.hero-app{padding-top:112px}.hero h1{font-size:clamp(56px,15vw,86px)}.hero-heading>p{font-size:17px}''',
    '''    @media(max-width:820px){
      .wrap{width:min(var(--max),calc(100% - 32px))}.nav{display:none}.header-inner{gap:12px}.header-cta{margin-left:auto;min-height:40px;padding:0 14px;font-size:12px}.hero-app{padding:108px 0 76px}.hero-grid{gap:36px}.hero-copy h1{font-size:clamp(46px,10vw,66px)}.hero-lead{font-size:17px}.hero-preview{padding:18px;border-radius:23px}.pain-section{padding:82px 0 88px}.pain-grid{grid-template-columns:1fr}.pain-card:last-child{grid-column:auto}.pain-card{min-height:190px}.call-flow-shell{padding:86px 0 96px}''',
    "820 responsive"
)
text = replace_once(
    text,
    '''    @media(max-width:560px){
      .header{height:64px}.logo{font-size:18px}.hero-app{padding:102px 0 72px}.hero-kicker{font-size:15px}.hero h1{font-size:53px}.hero-heading>p{max-width:330px;font-size:16px}.phone-stage{margin-top:46px}.step-panel{padding:6px}.step-title{font-size:40px}.step-sub{font-size:18px}.step-item{grid-template-columns:38px 1fr}.step-item b{width:38px;height:38px}.phone-shell{min-height:640px;border-radius:41px}.phone-screen{min-height:618px;border-radius:32px}.app-screen{padding-inline:18px}.app-title{font-size:24px}''',
    '''    @media(max-width:560px){
      .header{height:64px}.logo{font-size:18px}.logo-mark{width:32px;height:32px}.header-cta{min-height:38px;padding:0 12px;border-radius:10px;font-size:11px}.hero-app{padding:96px 0 64px}.hero-kicker{margin-bottom:15px;font-size:13px}.hero-copy h1{font-size:42px;line-height:1.04}.hero-lead{margin-top:20px;font-size:16px;line-height:1.6}.hero-actions{display:grid;grid-template-columns:1fr;margin-top:26px}.hero-primary,.hero-secondary{width:100%;min-height:52px}.hero-support{justify-content:center}.hero-preview{padding:15px}.hero-preview-grid{grid-template-columns:1fr}.pain-section{padding:72px 0 76px}.pain-head h2{font-size:40px}.pain-grid{margin-top:32px}.pain-card{min-height:170px;padding:22px}.pain-card strong{font-size:23px}.pain-conclusion{font-size:16px}.call-flow-shell{padding:72px 0 82px}.phone-stage{margin-top:0}.step-panel{padding:6px}.step-title{font-size:40px}.step-sub{font-size:18px}.step-item{grid-template-columns:38px 1fr}.step-item b{width:38px;height:38px}.phone-shell{min-height:640px;border-radius:41px}.phone-screen{min-height:618px;border-radius:32px}.app-screen{padding-inline:18px}.app-title{font-size:24px}''',
    "560 responsive"
)

old_header = '''      <nav class="nav" aria-label="주요 메뉴"><a href="#app">앱</a><a href="#web">웹</a><a href="#tasks">기능</a><a href="#reviews">후기</a><a href="#pricing">요금</a><a href="#faq">FAQ</a></nav>
'''
new_header = '''      <nav class="nav" aria-label="주요 메뉴"><a href="#app">앱</a><a href="#web">웹</a><a href="#tasks">기능</a><a href="#reviews">후기</a><a href="#pricing">요금</a><a href="#faq">FAQ</a></nav>
      <a class="header-cta" href="https://pagero.kr/app" target="_blank" rel="noopener">7일 무료 시작</a>
'''
text = replace_once(text, old_header, new_header, "header CTA markup")

hero_start = text.index('    <section class="hero hero-app" id="app">')
web_start = text.index('    <section class="hero hero-web" id="web">')
hero_old = text[hero_start:web_start]
phone_start = hero_old.index('        <div class="phone-stage reveal">')
phone_tail = '\n        </div>\n      </div>\n    </section>\n\n'
phone_end_marker = hero_old.rfind(phone_tail)
if phone_end_marker == -1:
    raise RuntimeError("phone stage tail not found")
phone_block = hero_old[phone_start:phone_end_marker + len('\n        </div>')]

hero_new = f'''    <section class="hero hero-app" id="app">
      <div class="wrap hero-grid">
        <div class="hero-copy reveal">
          <p class="hero-kicker">전화로 상담하는 사람을 위한 고객관리</p>
          <h1>전화가 끝난 뒤,<br><span>고객관리는 그때부터 시작됩니다.</span></h1>
          <p class="hero-lead">통화가 끝나면 고객을 분류하고 상담 내용과 다음 할 일을 바로 남기세요.</p>
          <div class="hero-actions">
            <a class="hero-primary" href="https://pagero.kr/app" target="_blank" rel="noopener">7일 무료로 시작하기</a>
            <a class="hero-secondary" href="#pain">30초 만에 기능 보기 ↓</a>
          </div>
          <div class="hero-support"><span>Android 앱</span><i></i><span>PC 웹 연동</span></div>
        </div>

        <div class="hero-preview reveal" aria-label="콜태그 통화 종료 후 고객관리 예시">
          <div class="hero-preview-head"><strong>통화 종료 후 고객 정리</strong><span>방금 통화</span></div>
          <div class="hero-preview-customer">
            <div class="hero-preview-avatar">신규</div>
            <div><strong>010-4821-****</strong><small>수신 통화 · 2분 18초</small></div>
          </div>
          <div class="hero-preview-tags"><span>신규 문의</span><span>상품 문의 · 진행 중</span></div>
          <div class="hero-preview-grid">
            <div class="hero-preview-field"><span>다음 할 일</span><b>자료·견적 발송</b></div>
            <div class="hero-preview-field"><span>재연락</span><b>7월 31일 · 10:30</b></div>
          </div>
          <div class="hero-preview-save">저장하면 오늘 할 일에 추가</div>
          <p class="hero-preview-note">통화 내용은 자동 분석하지 않습니다. 사용자가 직접 분류하고 기록합니다.</p>
        </div>
      </div>
    </section>

    <section class="pain-section" id="pain">
      <div class="wrap">
        <div class="pain-head reveal">
          <p class="pain-kicker">이런 적 있나요?</p>
          <h2>통화는 끝났는데,<br>할 일은 계속 남습니다.</h2>
        </div>
        <div class="pain-grid">
          <article class="pain-card reveal"><span>01 · 기억 누락</span><strong>“아까 그 고객,<br>무슨 이야기 했더라?”</strong></article>
          <article class="pain-card reveal"><span>02 · 재연락 누락</span><strong>“내일 다시 전화하기로 했는데<br>깜빡했다.”</strong></article>
          <article class="pain-card reveal"><span>03 · 후속업무 누락</span><strong>“견적 보내준다고 했는데<br>다른 일 하다 놓쳤다.”</strong></article>
        </div>
        <div class="pain-conclusion reveal"><i></i><span>콜태그는 이걸 기억으로 관리하지 않게 만듭니다.</span></div>
      </div>
    </section>

    <section class="call-flow-shell" id="call-flow">
      <div class="wrap">
{phone_block}
      </div>
    </section>

'''
text = text[:hero_start] + hero_new + text[web_start:]

# Mandatory policy consistency. Pricing redesign itself is a later section.
text = text.replace('3일 동안 써보고', '7일 동안 써보고')
text = text.replace('3일 무료체험 신청', '7일 무료체험 신청')
text = text.replace('3일 무료체험', '7일 무료체험')

if "3일" in text:
    raise RuntimeError("stale 3-day copy remains")
if "+7일" in text:
    raise RuntimeError("stale +7-day referral copy remains")

INDEX.write_text(text, encoding="utf-8")
print("landing sections 01-02 applied")
