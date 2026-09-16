(()=>{
  if(document.documentElement.dataset.ctLanding0102V3)return;
  document.documentElement.dataset.ctLanding0102V3='1';
  const APP='https://pagero.kr/app';
  const TITLE='전화가 끝난 뒤,<br><span>고객관리는 시작됩니다.</span>';
  const DESCRIPTION='고객 · 상담 · 다음 할 일.';
  const reduced=window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  let applying=false,queued=false;

  const installStyle=()=>{
    let style=document.getElementById('ct-live-0102-style');
    if(!style){style=document.createElement('style');style.id='ct-live-0102-style';document.head.append(style);}
    style.textContent=`
      #app.hero-app{padding-bottom:172px!important}
      .ct-live-hero-actions{display:flex;justify-content:center;align-items:center;margin-top:38px}
      .ct-live-hero-primary{min-height:62px;display:inline-flex;align-items:center;justify-content:center;padding:0 34px;border-radius:15px;background:#3b6fff;color:#fff!important;font-size:17px;font-weight:900;text-decoration:none;box-shadow:0 18px 44px rgba(59,111,255,.24);transition:transform .28s cubic-bezier(.2,.8,.2,1),box-shadow .28s ease,background .28s ease}
      .ct-live-hero-primary:hover,.ct-live-hero-primary:focus-visible{transform:translateY(-3px);background:#527dff;box-shadow:0 24px 58px rgba(59,111,255,.31);outline:none}
      .ct-live-hero-link{display:none!important}
      #app .hero-heading.ct-live-hero-enter h1{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) both}
      #app .hero-heading.ct-live-hero-enter>p:last-of-type{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) .12s both}
      #app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) .24s both}
      #ct-live-pain{position:relative;overflow:hidden;padding:184px 0 192px;border-top:1px solid var(--line);border-bottom:1px solid var(--line);background:#0d0f13}
      #ct-live-pain:before{content:"";position:absolute;width:780px;height:780px;left:50%;top:-520px;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.13),rgba(59,111,255,0) 68%);pointer-events:none}
      #ct-live-pain .wrap{position:relative;z-index:1}
      #ct-live-pain .ct-live-pain-head{text-align:center;opacity:0;transform:translateY(34px);transition:opacity .78s ease,transform .86s cubic-bezier(.18,.82,.22,1)}
      #ct-live-pain h2{margin:0;font-size:clamp(58px,6.45vw,94px);line-height:.98;letter-spacing:-.082em}
      #ct-live-pain h2 span{color:var(--blue-2)}
      #ct-live-pain .ct-live-pain-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;margin-top:78px}
      #ct-live-pain .ct-live-pain-card{min-height:220px;display:grid;place-items:center;padding:32px;border:1px solid var(--line);border-radius:24px;background:linear-gradient(155deg,#171a21,#111319);text-align:center;opacity:0;transform:translateY(46px) scale(.975);transition:opacity .68s ease,transform .78s cubic-bezier(.18,.82,.22,1),border-color .32s ease,box-shadow .32s ease,background .32s ease}
      #ct-live-pain .ct-live-pain-card:nth-child(1){transition-delay:.10s}
      #ct-live-pain .ct-live-pain-card:nth-child(2){transition-delay:.20s}
      #ct-live-pain .ct-live-pain-card:nth-child(3){transition-delay:.30s}
      #ct-live-pain .ct-live-pain-card b{font-size:clamp(32px,3.15vw,48px);line-height:1.05;letter-spacing:-.065em}
      #ct-live-pain.is-visible .ct-live-pain-head,#ct-live-pain.is-visible .ct-live-pain-card{opacity:1;transform:none}
      #ct-live-pain .ct-live-pain-card:hover{transform:translateY(-7px);border-color:rgba(124,153,255,.45);background:linear-gradient(155deg,rgba(59,111,255,.13),#12151c 58%);box-shadow:0 24px 58px rgba(0,0,0,.28)}
      @keyframes ctLiveHeroUp{from{opacity:0;transform:translateY(34px)}to{opacity:1;transform:none}}
      @media(max-width:760px){
        #app.hero-app{padding-bottom:118px!important}
        .ct-live-hero-actions{margin-top:30px}
        .ct-live-hero-primary{width:min(100%,340px);min-height:58px;font-size:16px}
        #ct-live-pain{padding:124px 0 132px}
        #ct-live-pain h2{font-size:48px}
        #ct-live-pain .ct-live-pain-grid{grid-template-columns:1fr;gap:14px;margin-top:56px}
        #ct-live-pain .ct-live-pain-card{min-height:126px;border-radius:20px}
        #ct-live-pain .ct-live-pain-card b{font-size:32px}
      }
      @media(prefers-reduced-motion:reduce){
        #app .hero-heading.ct-live-hero-enter h1,#app .hero-heading.ct-live-hero-enter>p:last-of-type,#app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:none!important}
        #ct-live-pain .ct-live-pain-head,#ct-live-pain .ct-live-pain-card{opacity:1!important;transform:none!important;transition:none!important}
      }
    `;
  };

  const makePain=()=>{const section=document.createElement('section');section.id='ct-live-pain';section.className='ad-section alt ct-live-pain';return section;};
  const fillPain=section=>{const wanted='<div class="wrap"><div class="ct-live-pain-head"><h2>기억으로 관리하면<br><span>놓칩니다.</span></h2></div><div class="ct-live-pain-grid"><div class="ct-live-pain-card"><b>누구였지?</b></div><div class="ct-live-pain-card"><b>언제 다시?</b></div><div class="ct-live-pain-card"><b>보냈나?</b></div></div></div>';if(section.innerHTML!==wanted)section.innerHTML=wanted;};

  const revealPain=section=>{
    if(section.dataset.ctObserved)return;
    section.dataset.ctObserved='1';
    if(reduced||!('IntersectionObserver'in window)){section.classList.add('is-visible');return;}
    const observer=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');observer.unobserve(entry.target);}});},{threshold:.24,rootMargin:'0px 0px -10%'});
    observer.observe(section);
  };

  const apply=()=>{
    queued=false;if(applying)return;applying=true;
    try{
      installStyle();
      const app=document.querySelector('#app');
      const heading=app?.querySelector('.hero-heading');
      const title=heading?.querySelector('h1');
      const description=heading?.querySelector(':scope > p:last-of-type');
      if(title&&title.innerHTML!==TITLE)title.innerHTML=TITLE;
      if(description&&description.textContent.trim()!==DESCRIPTION)description.textContent=DESCRIPTION;
      if(heading){
        let actions=heading.querySelector('.ct-live-hero-actions');
        if(!actions){actions=document.createElement('div');actions.className='ct-live-hero-actions';heading.append(actions);}
        if(!actions.querySelector('.ct-live-hero-primary'))actions.innerHTML=`<a class="ct-live-hero-primary" href="${APP}" target="_blank" rel="noopener">7일 무료로 시작하기</a>`;
        if(!heading.dataset.ctHeroAnimated){heading.dataset.ctHeroAnimated='1';requestAnimationFrame(()=>heading.classList.add('ct-live-hero-enter'));}
      }
      if(app){
        let pain=document.getElementById('ct-live-pain');
        if(!pain)pain=makePain();
        fillPain(pain);
        if(app.nextElementSibling!==pain)app.insertAdjacentElement('afterend',pain);
        revealPain(pain);
      }
    }finally{applying=false;}
  };

  const queueApply=()=>{if(queued||applying)return;queued=true;requestAnimationFrame(apply);};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  [80,220,500,900,1500,2400,3600].forEach(delay=>setTimeout(apply,delay));
  const observer=new MutationObserver(queueApply);observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true});setTimeout(()=>observer.disconnect(),6200);
})();
