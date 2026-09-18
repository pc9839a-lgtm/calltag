(()=>{
  if(document.documentElement.dataset.ctLandingStabilizerV1)return;
  document.documentElement.dataset.ctLandingStabilizerV1='1';

  const APP='https://pagero.kr/app?source=calltag&plan=all&utm_source=calltag_site&utm_medium=cta&utm_campaign=2026_launch&utm_content=pricing_all';
  const installStyle=()=>{
    if(document.getElementById('ct-landing-stabilizer-v1-style'))return;
    const style=document.createElement('style');
    style.id='ct-landing-stabilizer-v1-style';
    style.textContent=`
      #app.hero-app{padding-bottom:120px!important}
      #app .ct-original-hero-heading{text-align:center;position:relative;z-index:1}
      #app .ct-original-hero-heading .hero-kicker{display:block!important;margin:0 0 20px!important;color:var(--blue-2)!important;font-size:18px!important;font-weight:900!important;letter-spacing:-.03em!important}
      #app .ct-original-hero-heading h1{margin:0!important;font-size:clamp(62px,8.2vw,118px)!important;line-height:.94!important;letter-spacing:-.085em!important}
      #app .ct-original-hero-heading h1 span{color:var(--blue-2)!important}
      #app .ct-original-hero-heading>p:last-child{margin:26px auto 0!important;max-width:720px!important;color:var(--muted)!important;font-size:clamp(17px,1.75vw,22px)!important;line-height:1.55!important;letter-spacing:-.025em!important}
      #app .ct-live-hero-actions{display:none!important}
      #web{display:none!important}
      #pricing.ct-pricing-policy-v1{position:relative;overflow:hidden;padding:190px 0 210px!important;background:#0d0f13;border-top:1px solid rgba(255,255,255,.105);border-bottom:1px solid rgba(255,255,255,.105)}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-wrap{position:relative;z-index:1;width:min(1120px,calc(100% - 48px));margin:0 auto}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-head{text-align:center}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-head>p{margin:0 0 18px;color:#7c99ff;font-size:15px;font-weight:900;letter-spacing:.04em}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-head h2{margin:0;color:#f7f8fb;font-size:clamp(48px,6.2vw,78px);line-height:.98;letter-spacing:-.075em}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-head h2 span{color:#7c99ff}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-head strong{display:block;margin-top:24px;color:#a4a9b4;font-size:18px;line-height:1.55}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-card{max-width:760px;margin:72px auto 0;padding:48px;border:1px solid rgba(124,153,255,.34);border-radius:30px;background:linear-gradient(155deg,rgba(59,111,255,.12),#151820 42%,#101217);box-shadow:0 34px 90px rgba(0,0,0,.28)}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-top{display:flex;align-items:flex-start;justify-content:space-between;gap:24px}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-badge{display:inline-flex;align-items:center;min-height:34px;padding:0 13px;border-radius:999px;background:rgba(59,111,255,.16);color:#9eb1ff;font-size:13px;font-weight:950}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-card h3{margin:18px 0 0;color:#fff;font-size:clamp(27px,3vw,36px);letter-spacing:-.05em}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-price{text-align:right;white-space:nowrap}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-price strong{display:block;color:#fff;font-size:clamp(42px,5vw,62px);line-height:1;letter-spacing:-.065em}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-price span{display:block;margin-top:8px;color:#8d95a3;font-size:15px;font-weight:800}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-divider{height:1px;margin:36px 0;background:rgba(255,255,255,.1)}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px 22px;margin:0;padding:0;list-style:none}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-list li{display:flex;align-items:center;gap:10px;color:#d7dbe4;font-size:16px;font-weight:760}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-list i{width:22px;height:22px;display:grid;place-items:center;flex:0 0 auto;border-radius:50%;background:rgba(59,111,255,.16);color:#8fa6ff;font-size:12px;font-style:normal;font-weight:950}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-cta{min-height:64px;display:flex;align-items:center;justify-content:center;margin-top:38px;border-radius:16px;background:#3b6fff;color:#fff!important;font-size:17px;font-weight:950;text-decoration:none;box-shadow:0 18px 44px rgba(59,111,255,.24)}
      #pricing.ct-pricing-policy-v1 .ct-price-policy-note{margin:16px 0 0;text-align:center;color:#777f8d;font-size:13px;font-weight:750}
      @media(max-width:820px){#app .ct-original-hero-heading h1{font-size:clamp(56px,15vw,86px)!important}#app .ct-original-hero-heading>p:last-child{font-size:17px!important}}
      @media(max-width:700px){#pricing.ct-pricing-policy-v1{padding:126px 0 140px!important}#pricing.ct-pricing-policy-v1 .ct-price-policy-wrap{width:min(100% - 36px,720px)}#pricing.ct-pricing-policy-v1 .ct-price-policy-card{margin-top:48px;padding:30px 24px;border-radius:24px}#pricing.ct-pricing-policy-v1 .ct-price-policy-top{display:block}#pricing.ct-pricing-policy-v1 .ct-price-policy-price{margin-top:30px;text-align:left}#pricing.ct-pricing-policy-v1 .ct-price-policy-list{grid-template-columns:1fr}}
      @media(max-width:560px){#app.hero-app{padding-bottom:72px!important}#app .ct-original-hero-heading .hero-kicker{font-size:15px!important}#app .ct-original-hero-heading h1{font-size:53px!important}#app .ct-original-hero-heading>p:last-child{max-width:330px!important;font-size:16px!important}}
    `;
    document.head.append(style);
  };

  const patchHero=()=>{
    const app=document.getElementById('app');
    if(!app)return;
    let heading=app.querySelector('.ct-original-hero-heading')||app.querySelector('.hero-heading');
    if(!heading)return;
    heading.classList.remove('hero-heading','ct-live-hero-enter');
    heading.classList.add('ct-original-hero-heading');
    delete heading.dataset.ctHeroAnimated;
    const markup='<p class="hero-kicker">통화 후 고객관리</p><h1>1명의 고객도<br><span>놓치지 않습니다.</span></h1><p>통화가 끝난 뒤 고객을 태그하면 상담 상태와 다음 할 일이 바로 정리됩니다.</p>';
    if(heading.innerHTML!==markup)heading.innerHTML=markup;
    app.querySelectorAll('.ct-live-hero-actions').forEach(node=>node.remove());
  };

  const patchAppOnly=()=>{
    const web=document.getElementById('web');
    if(web){web.hidden=true;web.setAttribute('aria-hidden','true');}
    document.querySelectorAll('a[href="#web"]').forEach(link=>link.remove());
    document.querySelectorAll('.ct-app-web-roles').forEach(node=>node.remove());
    const replacements=[
      ['오늘 할 일과 웹 캘린더에 추가됨','오늘 할 일과 일정에 추가됨'],
      ['앱과 PC에서 고객과 일정 확인','앱에서 고객과 일정 확인'],
      ['고객별 상담 이력과 웹 캘린더','고객별 상담 이력과 일정 관리'],
      ['Android 앱 · PC 웹 연동','Android 앱 지원'],
      ['휴대폰에서 기록, 큰 화면에서 확인','앱에서 바로 기록하고 확인']
    ];
    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    let node;
    while((node=walker.nextNode())){
      const parent=node.parentElement;
      if(!parent||['SCRIPT','STYLE','NOSCRIPT','TEXTAREA'].includes(parent.tagName))continue;
      let next=node.nodeValue;
      replacements.forEach(([from,to])=>{if(next.includes(from))next=next.replaceAll(from,to);});
      if(next!==node.nodeValue)node.nodeValue=next;
    }
    document.querySelectorAll('.faq-item').forEach(item=>{
      const question=item.querySelector('.faq-question');
      const answer=item.querySelector('.faq-answer p');
      const q=(question?.textContent||'').trim();
      if(q.includes('웹에서는')||q.includes('PC에서')){item.remove();return;}
      if(q.includes('아이폰')&&answer)answer.textContent='현재 콜태그 앱은 Android 전용입니다.';
    });
  };

  const pricingMarkup=`<div class="ct-price-policy-wrap"><header class="ct-price-policy-head"><p>요금 안내</p><h2><span>7일</span> 써보고 결정하세요.</h2><strong>페이지로 + 콜태그를 하나의 통합권으로 이용합니다.</strong></header><article class="ct-price-policy-card"><div class="ct-price-policy-top"><div><span class="ct-price-policy-badge">7일 무료체험</span><h3>페이지로 + 콜태그 통합권</h3></div><div class="ct-price-policy-price"><strong>6,000원</strong><span>/ 월</span></div></div><div class="ct-price-policy-divider"></div><ul class="ct-price-policy-list"><li><i>✓</i>페이지로 고객 문의 접수</li><li><i>✓</i>콜태그 통화 후 고객관리</li><li><i>✓</i>통화 후 업무관리</li><li><i>✓</i>안내·후속문자 자동화</li><li><i>✓</i>외부 문의 연동</li></ul><a class="ct-price-policy-cta" href="${APP}" target="_blank" rel="noopener">7일 무료로 시작하기</a><p class="ct-price-policy-note">7일 무료체험 종료 후 월 6,000원</p></article></div>`;

  const patchPricing=()=>{
    const pricing=document.getElementById('pricing');
    if(pricing){pricing.className='ad-section ct-pricing-policy-v1';pricing.innerHTML=pricingMarkup;}
    const faq=document.getElementById('faq');
    if(!faq)return;
    const item=[...faq.querySelectorAll('.faq-item')].find(node=>(node.querySelector('.faq-question')?.textContent||'').includes('요금'));
    if(!item)return;
    const question=item.querySelector('.faq-question');
    const answer=item.querySelector('.faq-answer p');
    if(question){const svg=question.querySelector('svg');[...question.childNodes].filter(node=>node.nodeType===Node.TEXT_NODE).forEach(node=>node.remove());question.insertBefore(document.createTextNode('요금은 얼마인가요?'),svg||null);}
    if(answer)answer.textContent='기본 7일 무료체험 후 페이지로 + 콜태그 통합권은 월 6,000원입니다.';
  };

  const applyStable=()=>{
    installStyle();
    patchHero();
    patchAppOnly();
    patchPricing();
  };

  let runtimeSeen=false;
  const onReady=()=>{runtimeSeen=true;applyStable();};
  const arm=()=>{
    if(document.documentElement.dataset.ctLayoutCoordinatorV12)onReady();
    else document.addEventListener('calltag:runtime-ready',onReady,{once:true});
    document.addEventListener('calltag:runtime-settled',applyStable,{once:true});
    setTimeout(()=>{if(!runtimeSeen)onReady();},2600);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',arm,{once:true});else arm();
})();
