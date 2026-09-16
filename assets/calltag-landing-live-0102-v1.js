(()=>{
  if(document.documentElement.dataset.ctLanding0103V2)return;
  document.documentElement.dataset.ctLanding0103V2='1';

  const APP='https://pagero.kr/app';
  const TITLE='전화가 끝난 뒤,<br><span>고객관리는 시작됩니다.</span>';
  const DESCRIPTION='고객 · 상담 · 다음 할 일.';
  const reduced=window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  let applying=false,queued=false;

  const installStyle=()=>{
    let style=document.getElementById('ct-live-0102-style');
    if(!style){style=document.createElement('style');style.id='ct-live-0102-style';document.head.append(style);}
    style.textContent=`
      #app.hero-app{padding-bottom:220px!important}
      .ct-live-hero-actions{display:flex;justify-content:center;align-items:center;margin-top:38px}
      .ct-live-hero-primary{min-height:62px;display:inline-flex;align-items:center;justify-content:center;padding:0 34px;border-radius:15px;background:#3b6fff;color:#fff!important;font-size:17px;font-weight:900;text-decoration:none;box-shadow:0 18px 44px rgba(59,111,255,.24);transition:transform .28s cubic-bezier(.2,.8,.2,1),box-shadow .28s ease,background .28s ease}
      .ct-live-hero-primary:hover,.ct-live-hero-primary:focus-visible{transform:translateY(-3px);background:#527dff;box-shadow:0 24px 58px rgba(59,111,255,.31);outline:none}
      .ct-live-hero-link{display:none!important}
      #app .hero-heading.ct-live-hero-enter h1{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) both}
      #app .hero-heading.ct-live-hero-enter>p:last-of-type{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) .12s both}
      #app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) .24s both}

      #ct-live-pain{position:relative;overflow:hidden;padding:230px 0 250px!important;min-height:980px;display:flex!important;align-items:center;border-top:1px solid var(--line);border-bottom:1px solid var(--line);background:#0d0f13}
      #ct-live-pain:before{content:"";position:absolute;width:780px;height:780px;left:50%;top:-520px;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.13),rgba(59,111,255,0) 68%);pointer-events:none}
      #ct-live-pain .wrap{position:relative;z-index:1;width:min(1360px,calc(100% - 96px))}
      #ct-live-pain .ct-live-pain-head{text-align:center;opacity:0;transform:translateY(34px);transition:opacity .78s ease,transform .86s cubic-bezier(.18,.82,.22,1)}
      #ct-live-pain h2{margin:0;font-size:clamp(58px,6.45vw,94px);line-height:.98;letter-spacing:-.082em}
      #ct-live-pain h2 span{color:var(--blue-2)}
      #ct-live-pain .ct-live-pain-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;margin-top:104px}
      #ct-live-pain .ct-live-pain-card{min-height:220px;display:grid;place-items:center;padding:32px;border:1px solid var(--line);border-radius:24px;background:linear-gradient(155deg,#171a21,#111319);text-align:center;opacity:0;transform:translateY(70px) scale(.965);filter:blur(6px);transition:opacity .72s ease,transform .86s cubic-bezier(.18,.82,.22,1),filter .72s ease,border-color .32s ease,box-shadow .32s ease,background .32s ease}
      #ct-live-pain .ct-live-pain-card:nth-child(1){transition-delay:.10s}
      #ct-live-pain .ct-live-pain-card:nth-child(2){transition-delay:.20s}
      #ct-live-pain .ct-live-pain-card:nth-child(3){transition-delay:.30s}
      #ct-live-pain .ct-live-pain-card b{font-size:clamp(32px,3.15vw,48px);line-height:1.05;letter-spacing:-.065em}
      #ct-live-pain.is-visible .ct-live-pain-head,#ct-live-pain.is-visible .ct-live-pain-card{opacity:1;transform:none;filter:none}
      #ct-live-pain .ct-live-pain-card:hover{transform:translateY(-7px);border-color:rgba(124,153,255,.45);background:linear-gradient(155deg,rgba(59,111,255,.13),#12151c 58%);box-shadow:0 24px 58px rgba(0,0,0,.28)}

      /* SECTION 03 — 통화 종료 후 10초 시연 */
      #how.ct-live-story-v1{position:relative!important;overflow:visible!important;padding:230px 0 190px!important;background:#090a0d!important;border-top:1px solid var(--line)!important;border-bottom:1px solid var(--line)!important}
      #how.ct-live-story-v1:before{content:"";position:absolute;width:980px;height:980px;right:-460px;top:120px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.14),rgba(59,111,255,0) 67%);pointer-events:none}
      #how.ct-live-story-v1 .ct-story-layout{position:relative;z-index:1;width:min(1460px,calc(100% - 96px))!important;max-width:none!important;display:grid!important;grid-template-columns:minmax(360px,.72fr) minmax(620px,1.28fr)!important;gap:110px!important;align-items:start!important}
      #how.ct-live-story-v1 .ct-story-sticky{position:sticky!important;top:132px!important;align-self:start!important;padding:0!important}
      #how.ct-live-story-v1 .ct-story-sticky>p,#how.ct-live-story-v1 .ct-story-current{display:none!important}
      #how.ct-live-story-v1 .ct-story-sticky h2{margin:0!important;font-size:clamp(64px,6vw,92px)!important;line-height:.96!important;letter-spacing:-.085em!important}
      #how.ct-live-story-v1 .ct-story-sticky h2 span{color:var(--blue-2)!important}
      #how.ct-live-story-v1 .ct-story-status{display:flex!important;align-items:baseline!important;gap:10px!important;margin-top:56px!important;color:#69707d!important}
      #how.ct-live-story-v1 .ct-story-status strong{color:#8ea6ff!important;font-size:56px!important;line-height:1!important;letter-spacing:-.07em!important}
      #how.ct-live-story-v1 .ct-story-status span{font-size:20px!important;font-weight:850!important}
      #how.ct-live-story-v1 .ct-story-steps{display:grid!important;gap:132px!important;min-width:0!important}
      #how.ct-live-story-v1 .ct-story-step{min-height:760px!important;display:flex!important;flex-direction:column!important;justify-content:center!important;padding:68px!important;border:1px solid rgba(255,255,255,.12)!important;border-radius:32px!important;background:linear-gradient(155deg,#151820,#0f1117)!important;opacity:.40!important;transform:translateY(62px) scale(.972)!important;filter:saturate(.72) blur(2px)!important;transition:opacity .64s ease,transform .82s cubic-bezier(.18,.82,.22,1),filter .64s ease,border-color .38s ease,box-shadow .38s ease,background .38s ease!important}
      #how.ct-live-story-v1 .ct-story-step.is-active{opacity:1!important;transform:none!important;filter:none!important;border-color:rgba(94,130,255,.58)!important;background:linear-gradient(155deg,rgba(59,111,255,.12),#12151c 54%,#0f1117)!important;box-shadow:0 38px 90px rgba(0,0,0,.32),0 0 0 1px rgba(59,111,255,.08)!important}
      #how.ct-live-story-v1 .ct-story-step[data-number="04"]{display:none!important}
      #how.ct-live-story-v1 .ct-step-label{margin:0 0 28px!important;color:#7595ff!important;font-size:64px!important;font-weight:950!important;line-height:1!important;letter-spacing:-.08em!important}
      #how.ct-live-story-v1 .ct-story-step h3{margin:0!important;color:#f7f8fb!important;font-size:clamp(46px,4.1vw,66px)!important;line-height:1.02!important;letter-spacing:-.07em!important}
      #how.ct-live-story-v1 .ct-story-step h3 span{color:var(--blue-2)!important}
      #how.ct-live-story-v1 .ct-screen{min-height:380px!important;margin-top:60px!important;padding:36px!important;border:1px solid rgba(255,255,255,.11)!important;border-radius:24px!important;background:#0d1016!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.03)!important;overflow:hidden!important}
      #how.ct-live-story-v1 .ct-live-callend-top{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding-bottom:28px;border-bottom:1px solid rgba(255,255,255,.1)}
      #how.ct-live-story-v1 .ct-live-callend-top span{color:#87909e;font-size:16px;font-weight:800}
      #how.ct-live-story-v1 .ct-live-callend-top strong{font-size:30px;letter-spacing:-.045em}
      #how.ct-live-story-v1 .ct-live-callend-row{display:flex;align-items:center;justify-content:space-between;margin-top:30px;padding:28px;border-radius:18px;background:#171b24}
      #how.ct-live-story-v1 .ct-live-callend-row b{font-size:28px;letter-spacing:-.05em}
      #how.ct-live-story-v1 .ct-live-callend-row span{color:#7c99ff;font-size:16px;font-weight:850}
      #how.ct-live-story-v1 .ct-live-callend-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:18px}
      #how.ct-live-story-v1 .ct-live-callend-actions b{min-height:74px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.1);border-radius:16px;background:#161a22;color:#b6bdca;font-size:17px}
      #how.ct-live-story-v1 .ct-live-callend-actions b:first-child{border-color:rgba(59,111,255,.55);background:rgba(59,111,255,.15);color:#fff}
      #how.ct-live-story-v1 .ct-live-choice-grid{display:grid;gap:16px}
      #how.ct-live-story-v1 .ct-live-choice-row{min-height:88px;display:flex;align-items:center;justify-content:space-between;gap:20px;padding:0 24px;border:1px solid rgba(255,255,255,.1);border-radius:17px;background:#171b24}
      #how.ct-live-story-v1 .ct-live-choice-row span{color:#8e96a3;font-size:16px;font-weight:750}
      #how.ct-live-story-v1 .ct-live-choice-row b{font-size:21px;letter-spacing:-.035em}
      #how.ct-live-story-v1 .ct-live-choice-row.is-on{border-color:rgba(59,111,255,.48);background:rgba(59,111,255,.12)}
      #how.ct-live-story-v1 .ct-live-next-date{display:grid;grid-template-columns:1fr auto;gap:24px;align-items:end;padding:30px;border-radius:20px;background:#171b24}
      #how.ct-live-story-v1 .ct-live-next-date span{display:block;color:#8c95a3;font-size:16px;font-weight:800}
      #how.ct-live-story-v1 .ct-live-next-date strong{display:block;margin-top:10px;font-size:34px;letter-spacing:-.055em}
      #how.ct-live-story-v1 .ct-live-next-date b{color:#8ea6ff;font-size:22px}
      #how.ct-live-story-v1 .ct-live-next-task{margin-top:18px;padding:26px 28px;border:1px solid rgba(255,255,255,.1);border-radius:18px;background:#151922}
      #how.ct-live-story-v1 .ct-live-next-task span{display:block;color:#7f8896;font-size:15px;font-weight:800}
      #how.ct-live-story-v1 .ct-live-next-task b{display:block;margin-top:9px;font-size:24px;letter-spacing:-.04em}
      #how.ct-live-story-v1 .ct-live-save{min-height:68px;display:grid;place-items:center;margin-top:18px;border-radius:16px;background:#3b6fff;color:#fff;font-size:18px;font-weight:950;box-shadow:0 16px 36px rgba(59,111,255,.23)}
      #how.ct-live-story-v1 .ct-live-step-finish{display:flex;align-items:center;justify-content:space-between;gap:24px;margin-top:26px;padding:24px 28px;border-top:1px solid rgba(255,255,255,.1);color:#dce2ef}
      #how.ct-live-story-v1 .ct-live-step-finish strong{color:var(--blue-2);font-size:30px;line-height:1;letter-spacing:-.055em}
      #how.ct-live-story-v1 .ct-live-step-finish span{font-size:18px;font-weight:900;letter-spacing:-.035em}
      #how.ct-live-story-v1 .ct-live-story-result{display:none!important}

      @keyframes ctLiveHeroUp{from{opacity:0;transform:translateY(34px)}to{opacity:1;transform:none}}

      @media(max-width:900px){
        #app.hero-app{padding-bottom:150px!important}
        .ct-live-hero-actions{margin-top:30px}
        .ct-live-hero-primary{width:min(100%,340px);min-height:58px;font-size:16px}
        #ct-live-pain{padding:156px 0 172px!important;min-height:0;display:block!important}
        #ct-live-pain .wrap{width:min(100% - 40px,720px)}
        #ct-live-pain h2{font-size:48px}
        #ct-live-pain .ct-live-pain-grid{grid-template-columns:1fr;gap:16px;margin-top:72px}
        #ct-live-pain .ct-live-pain-card{min-height:138px;border-radius:20px}
        #ct-live-pain .ct-live-pain-card b{font-size:32px}

        #how.ct-live-story-v1{padding:156px 0 148px!important}
        #how.ct-live-story-v1 .ct-story-layout{width:min(100% - 40px,720px)!important;display:block!important}
        #how.ct-live-story-v1 .ct-story-sticky{position:relative!important;top:auto!important;margin-bottom:92px!important;text-align:center!important}
        #how.ct-live-story-v1 .ct-story-sticky h2{font-size:52px!important}
        #how.ct-live-story-v1 .ct-story-status{justify-content:center!important;margin-top:40px!important}
        #how.ct-live-story-v1 .ct-story-steps{gap:70px!important}
        #how.ct-live-story-v1 .ct-story-step{min-height:0!important;padding:38px 26px!important;border-radius:24px!important;opacity:1!important;transform:none!important;filter:none!important}
        #how.ct-live-story-v1 .ct-step-label{margin-bottom:22px!important;font-size:48px!important}
        #how.ct-live-story-v1 .ct-story-step h3{font-size:40px!important}
        #how.ct-live-story-v1 .ct-screen{min-height:0!important;margin-top:42px!important;padding:24px!important;border-radius:20px!important}
        #how.ct-live-story-v1 .ct-live-callend-top{display:block!important}
        #how.ct-live-story-v1 .ct-live-callend-top strong{display:block;margin-top:9px;font-size:25px!important}
        #how.ct-live-story-v1 .ct-live-callend-actions{grid-template-columns:1fr!important}
        #how.ct-live-story-v1 .ct-live-callend-actions b{min-height:64px!important;font-size:16px!important}
        #how.ct-live-story-v1 .ct-live-choice-row{min-height:74px!important;padding:0 18px!important}
        #how.ct-live-story-v1 .ct-live-choice-row span{font-size:15px!important}
        #how.ct-live-story-v1 .ct-live-choice-row b{font-size:18px!important}
        #how.ct-live-story-v1 .ct-live-next-date{grid-template-columns:1fr!important;gap:12px!important}
        #how.ct-live-story-v1 .ct-live-next-date strong{font-size:29px!important}
        #how.ct-live-story-v1 .ct-live-step-finish{margin-top:20px;padding:20px 4px 0;gap:14px}
        #how.ct-live-story-v1 .ct-live-step-finish strong{font-size:24px}
        #how.ct-live-story-v1 .ct-live-step-finish span{font-size:16px}
      }

      @media(prefers-reduced-motion:reduce){
        #app .hero-heading.ct-live-hero-enter h1,#app .hero-heading.ct-live-hero-enter>p:last-of-type,#app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:none!important}
        #ct-live-pain .ct-live-pain-head,#ct-live-pain .ct-live-pain-card{opacity:1!important;transform:none!important;filter:none!important;transition:none!important}
        #how.ct-live-story-v1 .ct-story-step{opacity:1!important;transform:none!important;filter:none!important;transition:none!important}
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

  const findStory=()=>document.querySelector('#how.ct-story-section')||[...document.querySelectorAll('.ct-story-section')].find(section=>{const t=(section.textContent||'').replace(/\s+/g,' ').trim();return t.includes('통화가 끝나면')&&t.includes('태그만 하세요');})||null;

  const patchStory=()=>{
    const pain=document.getElementById('ct-live-pain');
    const story=findStory();
    if(!story)return false;

    story.classList.add('ct-live-story-v1');
    story.setAttribute('aria-label','통화 종료 후 3단계 고객관리 시연');
    if(pain&&pain.nextElementSibling!==story)pain.insertAdjacentElement('afterend',story);

    const sticky=story.querySelector('.ct-story-sticky');
    const stickyTitle=sticky?.querySelector('h2');
    if(stickyTitle&&stickyTitle.dataset.ctPlan03!=='1'){
      stickyTitle.innerHTML='통화가 끝나면,<br><span>3단계면 끝.</span>';
      stickyTitle.dataset.ctPlan03='1';
    }
    const status=sticky?.querySelector('.ct-story-status');
    const total=status?.querySelector('span');
    if(total)total.textContent='/ 03';

    const steps=[...story.querySelectorAll('.ct-story-step')];
    if(steps.length<3)return false;

    const definitions=[
      {
        title:'통화 종료',
        html:`<div class="ct-step-label">01</div><h3>전화를 끊으면<br><span>바로 나타납니다.</span></h3><div class="ct-screen ct-live-callend-screen"><div class="ct-live-callend-top"><span>통화 종료</span><strong>010-4821-7536</strong></div><div class="ct-live-callend-row"><b>김민수</b><span>방금 통화</span></div><div class="ct-live-callend-actions"><b>신규 문의</b><b>기존 고객</b><b>제외</b></div></div>`
      },
      {
        title:'고객 + 상담',
        html:`<div class="ct-step-label">02</div><h3>누구인지,<br><span>상담만 선택.</span></h3><div class="ct-screen"><div class="ct-live-choice-grid"><div class="ct-live-choice-row is-on"><span>고객 구분</span><b>신규 문의</b></div><div class="ct-live-choice-row is-on"><span>상담 유형</span><b>상품 상담</b></div><div class="ct-live-choice-row"><span>상담 상태</span><b>진행 중</b></div></div></div>`
      },
      {
        title:'다음 할 일',
        html:`<div class="ct-step-label">03</div><h3>다시 연락할 날짜까지<br><span>남기면 끝.</span></h3><div class="ct-screen"><div class="ct-live-next-date"><div><span>다음 연락</span><strong>내일 · 오전 10:30</strong></div><b>재연락</b></div><div class="ct-live-next-task"><span>다음 할 일</span><b>견적서 발송</b></div><div class="ct-live-save">저장 완료</div></div><div class="ct-live-step-finish"><strong>약 10초</strong><span>고객관리 완료</span></div>`
      }
    ];

    definitions.forEach((definition,index)=>{
      const step=steps[index];
      step.dataset.number=String(index+1).padStart(2,'0');
      step.dataset.title=definition.title;
      if(step.dataset.ctPlan03!=='1'){
        step.innerHTML=definition.html;
        step.dataset.ctPlan03='1';
      }
    });
    steps.slice(3).forEach(step=>{step.style.setProperty('display','none','important');step.setAttribute('aria-hidden','true');});

    story.querySelectorAll('.ct-live-story-result').forEach(result=>result.remove());
    return true;
  };

  const apply=()=>{
    queued=false;
    if(applying)return;
    applying=true;
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
      patchStory();
    }finally{applying=false;}
  };

  const queueApply=()=>{if(queued||applying)return;queued=true;requestAnimationFrame(apply);};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  [80,220,500,900,1500,2400,3600,5200].forEach(delay=>setTimeout(apply,delay));
  const observer=new MutationObserver(queueApply);
  observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true});
  setTimeout(()=>observer.disconnect(),7200);
})();
