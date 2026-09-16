from pathlib import Path
import re

js_path = Path('assets/calltag-landing-live-0102-v1.js')
text = js_path.read_text(encoding='utf-8')

# Upgrade guard only once for this live patch.
text = text.replace('ctLanding0104V1', 'ctLanding0104V2')

css_start = text.find('      /* SECTION 04 — 외부 문의 유입 */')
css_end = text.find('      @keyframes ctLiveHeroUp', css_start)
if css_start < 0 or css_end < 0:
    raise SystemExit('section 04 css block not found')

new_css = r'''      /* SECTION 04 — 문의 유입 → 앱 알림 → 앱 관리 */
      #ct-live-integrations{position:relative;overflow:hidden;padding:230px 0 250px!important;border-top:1px solid var(--line);border-bottom:1px solid var(--line);background:#0d0f13}
      #ct-live-integrations:before{content:"";position:absolute;width:920px;height:920px;left:50%;top:-520px;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.16),rgba(59,111,255,0) 69%);pointer-events:none}
      #ct-live-integrations .ct-int-wrap{position:relative;z-index:1;width:min(1460px,calc(100% - 96px));margin:0 auto}
      #ct-live-integrations .ct-int-head{text-align:center;opacity:0;transform:translateY(38px);transition:opacity .72s ease,transform .82s cubic-bezier(.18,.82,.22,1)}
      #ct-live-integrations h2{margin:0;font-size:clamp(58px,6.2vw,92px);line-height:.98;letter-spacing:-.082em}
      #ct-live-integrations h2 span{color:var(--blue-2)}
      #ct-live-integrations .ct-int-sub{margin:34px auto 0;color:#a2a9b6;font-size:clamp(18px,1.55vw,22px);font-weight:750;line-height:1.45;letter-spacing:-.03em}
      #ct-live-integrations .ct-int-flow{display:grid;grid-template-columns:minmax(270px,.82fr) 72px minmax(420px,1.18fr) 72px minmax(310px,.92fr);align-items:stretch;margin-top:116px}
      #ct-live-integrations .ct-int-arrow{display:grid;place-items:center;color:#6f7d9f;font-size:54px;font-weight:300;opacity:0;transform:translateX(-18px);transition:opacity .56s ease .36s,transform .72s cubic-bezier(.18,.82,.22,1) .36s}
      #ct-live-integrations .ct-source-slider,#ct-live-integrations .ct-app-alert,#ct-live-integrations .ct-app-manage{min-height:380px;border-radius:30px;opacity:0;transform:translateY(44px) scale(.97);transition:opacity .65s ease,transform .8s cubic-bezier(.18,.82,.22,1),border-color .32s ease,box-shadow .32s ease}
      #ct-live-integrations .ct-source-slider{display:flex;flex-direction:column;justify-content:space-between;padding:42px 38px;border:1px solid rgba(255,255,255,.12);background:linear-gradient(155deg,#171a21,#111319)}
      #ct-live-integrations .ct-source-label{color:#7f8998;font-size:15px;font-weight:900;letter-spacing:.05em}
      #ct-live-integrations .ct-source-copy{margin:auto 0}
      #ct-live-integrations .ct-source-copy strong{display:block;color:#fff;font-size:clamp(34px,3vw,48px);line-height:1.02;letter-spacing:-.065em;transition:opacity .22s ease,transform .28s ease}
      #ct-live-integrations .ct-source-copy span{display:block;margin-top:16px;color:#8f98a7;font-size:18px;font-weight:800;transition:opacity .22s ease,transform .28s ease}
      #ct-live-integrations .ct-source-slider.is-changing .ct-source-copy strong,#ct-live-integrations .ct-source-slider.is-changing .ct-source-copy span{opacity:0;transform:translateY(10px)}
      #ct-live-integrations .ct-source-dots{display:flex;gap:9px}
      #ct-live-integrations .ct-source-dot{width:8px;height:8px;border-radius:999px;background:#343a46;transition:width .28s ease,background .28s ease}
      #ct-live-integrations .ct-source-dot.is-on{width:28px;background:#6f91ff}
      #ct-live-integrations .ct-app-alert{position:relative;display:flex;flex-direction:column;justify-content:center;padding:40px;border:1px solid rgba(59,111,255,.52);background:radial-gradient(circle at 50% 0,rgba(59,111,255,.24),transparent 56%),#121722;box-shadow:0 30px 82px rgba(59,111,255,.13)}
      #ct-live-integrations .ct-app-alert:before{content:"";position:absolute;inset:18px;border:1px solid rgba(124,153,255,.08);border-radius:22px;pointer-events:none}
      #ct-live-integrations .ct-alert-top{display:flex;align-items:center;gap:14px;color:#93a9ff;font-size:15px;font-weight:950;letter-spacing:.08em}
      #ct-live-integrations .ct-bell{width:42px;height:42px;display:grid;place-items:center;border-radius:13px;background:rgba(59,111,255,.18);box-shadow:0 0 0 1px rgba(124,153,255,.16)}
      #ct-live-integrations .ct-bell svg{width:23px;height:23px;stroke:#9bb0ff}
      #ct-live-integrations.is-visible .ct-bell{animation:ctBellRing 3.2s ease-in-out 1.15s infinite}
      #ct-live-integrations .ct-alert-title{margin-top:34px;color:#fff;font-size:clamp(42px,4.2vw,64px);font-weight:950;line-height:.96;letter-spacing:-.075em}
      #ct-live-integrations .ct-notification{display:flex;align-items:center;justify-content:space-between;gap:18px;margin-top:34px;padding:22px 24px;border:1px solid rgba(255,255,255,.11);border-radius:18px;background:#0d1017}
      #ct-live-integrations .ct-notification-copy strong{display:block;font-size:20px;letter-spacing:-.04em}
      #ct-live-integrations .ct-notification-copy span{display:block;margin-top:7px;color:#8b94a2;font-size:14px;font-weight:750}
      #ct-live-integrations .ct-open-app{flex:0 0 auto;padding:11px 14px;border-radius:12px;background:#3b6fff;color:#fff;font-size:13px;font-weight:900}
      #ct-live-integrations .ct-app-manage{display:flex;flex-direction:column;justify-content:center;padding:42px;border:1px solid rgba(255,255,255,.13);background:linear-gradient(155deg,#171a21,#111319)}
      #ct-live-integrations .ct-app-manage>span{color:#7f8998;font-size:15px;font-weight:900;letter-spacing:.04em}
      #ct-live-integrations .ct-app-manage>strong{display:block;margin-top:18px;color:#fff;font-size:clamp(38px,3.5vw,56px);line-height:.98;letter-spacing:-.07em}
      #ct-live-integrations .ct-manage-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:34px}
      #ct-live-integrations .ct-manage-actions b{min-height:66px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.11);border-radius:15px;background:#11151d;color:#dce2ed;font-size:17px}
      #ct-live-integrations .ct-manage-actions b:first-child{border-color:rgba(59,111,255,.48);background:rgba(59,111,255,.13);color:#fff}
      #ct-live-integrations .ct-int-note{margin:70px 0 0;text-align:center;color:#69717e;font-size:14px;font-weight:700}
      #ct-live-integrations.is-visible .ct-int-head,#ct-live-integrations.is-visible .ct-source-slider,#ct-live-integrations.is-visible .ct-int-arrow,#ct-live-integrations.is-visible .ct-app-alert,#ct-live-integrations.is-visible .ct-app-manage{opacity:1;transform:none}
      #ct-live-integrations .ct-source-slider{transition-delay:.08s}#ct-live-integrations .ct-app-alert{transition-delay:.34s}#ct-live-integrations .ct-app-manage{transition-delay:.58s}
      @keyframes ctBellRing{0%,78%,100%{transform:rotate(0)}82%{transform:rotate(13deg)}86%{transform:rotate(-11deg)}90%{transform:rotate(8deg)}94%{transform:rotate(-5deg)}}

      @media(max-width:1050px){
        #ct-live-integrations .ct-int-flow{grid-template-columns:1fr;gap:0;max-width:760px;margin:90px auto 0}
        #ct-live-integrations .ct-int-arrow{height:76px;transform:rotate(90deg) translateX(-14px)}
        #ct-live-integrations.is-visible .ct-int-arrow{transform:rotate(90deg)}
        #ct-live-integrations .ct-source-slider,#ct-live-integrations .ct-app-alert,#ct-live-integrations .ct-app-manage{min-height:280px}
      }
      @media(max-width:700px){
        #ct-live-integrations{padding:156px 0 170px!important}
        #ct-live-integrations .ct-int-wrap{width:min(100% - 40px,720px)}
        #ct-live-integrations h2{font-size:48px}
        #ct-live-integrations .ct-int-sub{margin-top:26px;font-size:18px}
        #ct-live-integrations .ct-int-flow{margin-top:76px}
        #ct-live-integrations .ct-source-slider,#ct-live-integrations .ct-app-alert,#ct-live-integrations .ct-app-manage{min-height:240px;padding:30px 26px;border-radius:22px}
        #ct-live-integrations .ct-source-copy strong{font-size:38px}
        #ct-live-integrations .ct-source-copy span{font-size:16px}
        #ct-live-integrations .ct-alert-title{margin-top:28px;font-size:44px}
        #ct-live-integrations .ct-notification{margin-top:28px;padding:18px;display:block}
        #ct-live-integrations .ct-open-app{display:inline-flex;margin-top:16px}
        #ct-live-integrations .ct-app-manage>strong{font-size:42px}
        #ct-live-integrations .ct-manage-actions{margin-top:26px}
        #ct-live-integrations .ct-manage-actions b{min-height:58px;font-size:16px}
        #ct-live-integrations .ct-int-arrow{height:64px;font-size:44px}
        #ct-live-integrations .ct-int-note{margin-top:52px;font-size:13px;line-height:1.5}
      }
'''
text = text[:css_start] + new_css + text[css_end:]

fn_start = text.find('  const patchIntegrations=()=>{')
fn_end = text.find('\n\n  const apply=()=>{', fn_start)
if fn_start < 0 or fn_end < 0:
    raise SystemExit('section 04 function block not found')

new_fn = r'''  const patchIntegrations=()=>{
    const story=document.querySelector('#how.ct-live-story-v1');
    if(!story)return false;
    let section=document.getElementById('ct-live-integrations');
    if(!section){
      section=document.createElement('section');
      section.id='ct-live-integrations';
      section.setAttribute('aria-label','외부 문의 앱 알림 및 고객관리');
    }
    if(section.dataset.ctV2!=='1'){
      section.dataset.ctV2='1';
      section.innerHTML=`<div class="ct-int-wrap"><div class="ct-int-head"><h2>문의가 들어오면<br><span>콜태그 앱이 바로 알려줍니다.</span></h2><p class="ct-int-sub">어디서 들어온 문의든 앱에서 바로 확인하고 관리하세요.</p></div><div class="ct-int-flow"><div class="ct-source-slider"><span class="ct-source-label">문의 접수</span><div class="ct-source-copy"><strong>Meta Lead Ads</strong><span>광고 리드폼 문의</span></div><div class="ct-source-dots" aria-hidden="true"><i class="ct-source-dot is-on"></i><i class="ct-source-dot"></i><i class="ct-source-dot"></i><i class="ct-source-dot"></i></div></div><div class="ct-int-arrow" aria-hidden="true">→</div><div class="ct-app-alert"><div class="ct-alert-top"><span class="ct-bell"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"></path><path d="M10 21h4"></path></svg></span>CALLTAG APP</div><div class="ct-alert-title">새 문의 도착</div><div class="ct-notification"><div class="ct-notification-copy"><strong>새 고객 문의가 도착했습니다.</strong><span>콜태그 앱에서 바로 확인하세요.</span></div><span class="ct-open-app">앱에서 확인</span></div></div><div class="ct-int-arrow" aria-hidden="true">→</div><div class="ct-app-manage"><span>콜태그 앱</span><strong>바로<br>고객관리</strong><div class="ct-manage-actions"><b>전화</b><b>문자</b><b>일정</b></div></div></div><p class="ct-int-note">연동 방식과 수신 시점은 채널별로 다를 수 있습니다.</p></div>`;
    }
    if(story.nextElementSibling!==section)story.insertAdjacentElement('afterend',section);
    if(!section.dataset.ctObserved){
      section.dataset.ctObserved='1';
      if(reduced||!('IntersectionObserver'in window)){section.classList.add('is-visible');}
      else{
        const io=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');io.unobserve(entry.target);}});},{threshold:.2,rootMargin:'0px 0px -8%'});
        io.observe(section);
      }
    }
    if(!section.dataset.ctSliderStarted){
      section.dataset.ctSliderStarted='1';
      const sources=[
        ['Meta Lead Ads','광고 리드폼 문의'],
        ['Google Forms','폼 응답 문의'],
        ['PageRo','페이지 문의'],
        ['Webhook','외부 서비스 문의']
      ];
      let index=0;
      const slider=section.querySelector('.ct-source-slider');
      const name=section.querySelector('.ct-source-copy strong');
      const desc=section.querySelector('.ct-source-copy span');
      const dots=[...section.querySelectorAll('.ct-source-dot')];
      if(slider&&name&&desc&&!reduced){
        setInterval(()=>{
          slider.classList.add('is-changing');
          setTimeout(()=>{
            index=(index+1)%sources.length;
            name.textContent=sources[index][0];
            desc.textContent=sources[index][1];
            dots.forEach((dot,i)=>dot.classList.toggle('is-on',i===index));
            slider.classList.remove('is-changing');
          },220);
        },2600);
      }
    }
    return true;
  };'''
text = text[:fn_start] + new_fn + text[fn_end:]

js_path.write_text(text, encoding='utf-8')

index = Path('index.html')
html = index.read_text(encoding='utf-8')
html2, count = re.subn(r'calltag-landing-live-0102-v1\.js\?v=[^\"]+', 'calltag-landing-live-0102-v1.js?v=20260916-8', html, count=1)
if count != 1:
    raise SystemExit('live script reference not found')
index.write_text(html2, encoding='utf-8')
