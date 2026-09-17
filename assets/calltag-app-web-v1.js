(()=>{
  if(document.documentElement.dataset.ctAppWebV1)return;
  document.documentElement.dataset.ctAppWebV1='1';

  const reduced=window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  let applying=false;

  const installStyle=()=>{
    if(document.getElementById('ct-app-web-v1-style'))return;
    const style=document.createElement('style');
    style.id='ct-app-web-v1-style';
    style.textContent=`
      #web.ct-app-web-v1{display:block!important;position:relative!important;overflow:hidden!important;padding:210px 0 220px!important;background:#090a0d!important;border-top:1px solid rgba(255,255,255,.105)!important;border-bottom:1px solid rgba(255,255,255,.105)!important}
      #web.ct-app-web-v1[hidden]{display:block!important}
      #web.ct-app-web-v1:before{content:"";position:absolute;width:900px;height:900px;right:-420px;top:-430px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.14),rgba(59,111,255,0) 68%);pointer-events:none}
      #web.ct-app-web-v1>.wrap{position:relative;z-index:1;width:min(1360px,calc(100% - 72px));margin:0 auto}
      #web.ct-app-web-v1 .web-heading-copy{text-align:center!important;max-width:1000px;margin:0 auto}
      #web.ct-app-web-v1 .ct-app-web-kicker{margin:0 0 20px;color:#7c99ff;font-size:15px;font-weight:950;letter-spacing:.05em}
      #web.ct-app-web-v1 .web-heading-copy h2{margin:0!important;font-size:clamp(56px,6.4vw,88px)!important;line-height:.98!important;letter-spacing:-.08em!important}
      #web.ct-app-web-v1 .web-heading-copy h2 span{color:#7c99ff!important}
      #web.ct-app-web-v1 .web-heading-copy>p:last-child{max-width:760px!important;margin:28px auto 0!important;color:#a4a9b4!important;font-size:clamp(18px,1.55vw,21px)!important;line-height:1.55!important}
      #web.ct-app-web-v1 .ct-app-web-roles{display:grid;grid-template-columns:1fr 1fr;gap:22px;margin:82px 0 34px}
      #web.ct-app-web-v1 .ct-app-web-role{position:relative;min-height:280px;padding:38px;border:1px solid rgba(255,255,255,.11);border-radius:26px;background:linear-gradient(155deg,#171a21,#101217);overflow:hidden}
      #web.ct-app-web-v1 .ct-app-web-role:after{content:"";position:absolute;width:260px;height:260px;right:-120px;bottom:-145px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.17),rgba(59,111,255,0) 68%);pointer-events:none}
      #web.ct-app-web-v1 .ct-app-web-role small{display:block;color:#7c99ff;font-size:14px;font-weight:950;letter-spacing:.06em}
      #web.ct-app-web-v1 .ct-app-web-role h3{margin:18px 0 0;color:#f7f8fb;font-size:clamp(30px,3vw,42px);line-height:1.05;letter-spacing:-.06em}
      #web.ct-app-web-v1 .ct-app-web-role>p{margin:18px 0 0;color:#929aa8;font-size:16px;font-weight:750;line-height:1.55}
      #web.ct-app-web-v1 .ct-app-web-tags{display:flex;flex-wrap:wrap;gap:9px;margin-top:28px}
      #web.ct-app-web-v1 .ct-app-web-tags span{min-height:38px;display:inline-flex;align-items:center;padding:0 13px;border:1px solid rgba(124,153,255,.2);border-radius:999px;background:rgba(59,111,255,.08);color:#bdc9f6;font-size:13px;font-weight:850}
      #web.ct-app-web-v1 .web-demo{margin-top:34px!important}
      #web.ct-app-web-v1 .fact-strip{margin-top:28px!important}
      #web.ct-app-web-v1 .ct-app-web-roles,#web.ct-app-web-v1 .web-demo{opacity:0;transform:translateY(34px);transition:opacity .7s ease,transform .82s cubic-bezier(.18,.82,.22,1)}
      #web.ct-app-web-v1.is-visible .ct-app-web-roles,#web.ct-app-web-v1.is-visible .web-demo{opacity:1;transform:none}
      #web.ct-app-web-v1.is-visible .web-demo{transition-delay:.12s}
      @media(max-width:900px){
        #web.ct-app-web-v1{padding:150px 0 160px!important}
        #web.ct-app-web-v1>.wrap{width:min(100% - 40px,900px)}
        #web.ct-app-web-v1 .ct-app-web-roles{grid-template-columns:1fr;gap:16px;margin-top:56px}
        #web.ct-app-web-v1 .ct-app-web-role{min-height:0;padding:30px 26px}
        #web.ct-app-web-v1 .web-demo{padding:16px!important;border-radius:24px!important;overflow:hidden!important}
        #web.ct-app-web-v1 .web-shell{overflow:auto!important;-webkit-overflow-scrolling:touch}
        #web.ct-app-web-v1 .web-body{min-width:820px}
        #web.ct-app-web-v1 .fact-strip{grid-template-columns:repeat(2,1fr)!important}
      }
      @media(max-width:560px){
        #web.ct-app-web-v1{padding:124px 0 136px!important}
        #web.ct-app-web-v1>.wrap{width:min(100% - 32px,560px)}
        #web.ct-app-web-v1 .web-heading-copy h2{font-size:clamp(42px,12vw,58px)!important}
        #web.ct-app-web-v1 .web-heading-copy>p:last-child{font-size:16px!important}
        #web.ct-app-web-v1 .ct-app-web-role{padding:27px 22px;border-radius:22px}
        #web.ct-app-web-v1 .ct-app-web-role h3{font-size:32px}
        #web.ct-app-web-v1 .fact-strip{grid-template-columns:1fr!important}
      }
      @media(prefers-reduced-motion:reduce){
        #web.ct-app-web-v1 .ct-app-web-roles,#web.ct-app-web-v1 .web-demo{opacity:1!important;transform:none!important;transition:none!important}
      }
    `;
    document.head.append(style);
  };

  const rolesMarkup=`<div class="ct-app-web-roles"><article class="ct-app-web-role"><small>CALLTAG APP</small><h3>통화 직후<br>10초 안에 기록.</h3><p>밖에서는 휴대폰으로 고객 상태와 다음 행동만 빠르게 남깁니다.</p><div class="ct-app-web-tags"><span>고객분류</span><span>메모</span><span>다음 할 일</span><span>문자</span></div></article><article class="ct-app-web-role"><small>CALLTAG WEB</small><h3>사무실에서는<br>전체 업무 관리.</h3><p>PC에서는 오늘 할 일과 고객별 상담 흐름, 후속 일정을 한눈에 확인합니다.</p><div class="ct-app-web-tags"><span>오늘 할 일</span><span>고객</span><span>상담이력</span><span>일정</span></div></article></div>`;

  const reveal=section=>{
    if(section.dataset.ctAppWebObserved)return;
    section.dataset.ctAppWebObserved='1';
    if(reduced||!('IntersectionObserver'in window)){section.classList.add('is-visible');return;}
    const observer=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');observer.unobserve(entry.target);}});},{threshold:.16,rootMargin:'0px 0px -8%'});
    observer.observe(section);
  };

  const patch=()=>{
    const section=document.getElementById('web');
    if(!section)return false;
    section.hidden=false;
    section.removeAttribute('hidden');
    section.removeAttribute('aria-hidden');
    section.classList.add('ct-app-web-v1');
    section.setAttribute('aria-label','콜태그 앱과 PC 웹 연동');

    const heading=section.querySelector('.web-heading-copy');
    if(heading){
      if(heading.dataset.ctAppWebCopy!=='1'){
        heading.innerHTML='<p class="ct-app-web-kicker">APP + WEB</p><h2>밖에서는 <span>10초 기록.</span><br>사무실에서는 전체 관리.</h2><p>통화 직후에는 필요한 것만 빠르게 남기고, PC에서는 고객과 상담·일정을 전체 흐름으로 확인하세요.</p>';
        heading.dataset.ctAppWebCopy='1';
      }
      let roles=section.querySelector('.ct-app-web-roles');
      if(!roles){
        const holder=document.createElement('div');
        holder.innerHTML=rolesMarkup;
        roles=holder.firstElementChild;
        const demo=section.querySelector('.web-demo');
        if(demo)demo.insertAdjacentElement('beforebegin',roles);else heading.insertAdjacentElement('afterend',roles);
      }
    }

    const integration=document.getElementById('ct-live-integrations');
    if(integration&&integration.nextElementSibling!==section)integration.insertAdjacentElement('afterend',section);
    reveal(section);
    return true;
  };

  const apply=()=>{
    if(applying)return;
    applying=true;
    try{installStyle();patch();}
    finally{applying=false;}
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  [100,260,520,900,1500,2400,3600,5200,7000].forEach(delay=>setTimeout(apply,delay));
  const observer=new MutationObserver(()=>requestAnimationFrame(apply));
  observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['hidden','aria-hidden','class']});
  setTimeout(()=>observer.disconnect(),8200);
})();
