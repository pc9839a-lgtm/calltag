(()=>{
  if(document.documentElement.dataset.ctPageroClarity)return;
  document.documentElement.dataset.ctPageroClarity='1';

  const APP_URL='https://pagero.kr/app';

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-pagero-clarity]'))return;
    const style=document.createElement('style');
    style.dataset.ctPageroClarity='1';
    style.textContent=`
      .ct-pagero-connect-copy{display:block;max-width:720px;margin:22px auto 0;color:#aeb5c2;font-size:16px;font-weight:600;line-height:1.7}
      .ct-connect-data-chip{position:absolute;z-index:3;left:50%;top:12px;transform:translateX(-50%);width:max-content;max-width:260px;padding:8px 11px;border:1px solid rgba(124,153,255,.34);border-radius:999px;background:#12192a;color:#aebcff;font-size:8px;font-weight:850;line-height:1.2;white-space:nowrap;box-shadow:0 10px 24px rgba(0,0,0,.22)}
      .ct-v8-nocode-copy>strong.ct-pagero-nocode-description{display:block;max-width:700px;margin:24px auto 0!important;color:#aeb5c2!important;font-size:16px!important;font-weight:600!important;line-height:1.7!important}
      .ct-pagero-nocode-benefits{display:flex;align-items:center;justify-content:center;flex-wrap:wrap;gap:9px;margin:24px auto 0}
      .ct-pagero-nocode-benefits span{display:inline-flex;align-items:center;min-height:38px;padding:0 13px;border:1px solid rgba(124,153,255,.24);border-radius:999px;background:rgba(59,111,255,.07);color:#dce3ff;font-size:12px;font-weight:800}
      .ct-pagero-nocode-actions{display:flex;flex-direction:column;align-items:center;gap:10px;margin:28px auto 0}
      .ct-pagero-nocode-cta{display:inline-flex;align-items:center;justify-content:center;min-width:286px;min-height:58px;padding:0 27px;border:1px solid rgba(151,174,255,.58);border-radius:999px;background:#3b6fff;color:#fff!important;font-size:17px;font-weight:900;letter-spacing:-.03em;text-decoration:none!important;box-shadow:0 17px 42px rgba(41,77,196,.32);transition:transform .2s ease,background .2s ease,box-shadow .2s ease}
      .ct-pagero-nocode-cta:hover,.ct-pagero-nocode-cta:focus-visible{transform:translateY(-2px);background:#527dff;box-shadow:0 21px 48px rgba(41,77,196,.42);outline:none}
      .ct-pagero-nocode-actions small{color:#7f8795;font-size:11px;font-weight:700}
      @media(max-width:760px){.ct-pagero-connect-copy{font-size:14px;padding:0 10px}.ct-connect-data-chip{top:4px;max-width:220px;padding:6px 9px;font-size:7px}.ct-v8-nocode-copy>strong.ct-pagero-nocode-description{font-size:14px!important;padding:0 10px}.ct-pagero-nocode-benefits{gap:7px;margin-top:20px}.ct-pagero-nocode-benefits span{min-height:34px;padding:0 11px;font-size:11px}.ct-pagero-nocode-actions{margin-top:24px}.ct-pagero-nocode-cta{width:min(100%,280px);min-width:0;min-height:54px;padding:0 20px;font-size:16px}}
      @media(prefers-reduced-motion:reduce){.ct-pagero-nocode-cta{transition:none!important}}
    `;
    document.head.append(style);
  };

  const updateIntegrationCopy=()=>{
    const section=document.querySelector('#ct-pagero-intro .ct-pagero-connect');
    const wrap=section?.querySelector(':scope > .wrap');
    const heading=wrap?.querySelector('h2');
    if(!section||!wrap||!heading)return false;

    const desired='랜딩페이지에서 받은 문의,<br><span>콜태그에서 바로 관리하세요.</span>';
    if(heading.innerHTML!==desired)heading.innerHTML=desired;

    let copy=wrap.querySelector('.ct-pagero-connect-copy');
    if(!copy){
      copy=document.createElement('strong');
      copy.className='ct-pagero-connect-copy';
      heading.insertAdjacentElement('afterend',copy);
    }
    copy.innerHTML='페이지로에 접수된 고객정보가 콜태그에 등록되고,<br>전화·문자·태그·후속관리까지 바로 이어집니다.';

    wrap.querySelectorAll(':scope > .ct-pagero-bottom-cta,:scope > .ct-pagero-start-cta').forEach(el=>el.remove());
    return true;
  };

  const updateFlowVisual=()=>{
    const demo=document.querySelector('#ct-pagero-intro .ct-connect-demo');
    if(!demo)return false;

    const captions=[...demo.querySelectorAll('.ct-connect-cap span')];
    if(captions[0])captions[0].textContent='페이지로 문의 접수';
    if(captions[1])captions[1].textContent='콜태그 고객관리';

    const arrow=demo.querySelector('.ct-connect-arrow');
    const arrowCopy=arrow?.querySelector('small');
    if(arrowCopy)arrowCopy.textContent='고객정보 자동 전달';
    if(arrow&&!arrow.querySelector('.ct-connect-data-chip')){
      const chip=document.createElement('span');
      chip.className='ct-connect-data-chip';
      chip.textContent='김민수 · 010-1234-5678 · 보험 상담';
      arrow.append(chip);
    }

    demo.querySelector('.ct-connect-sticky')?.remove();
    const submit=demo.querySelector('.ct-connect-submit');
    if(submit)submit.textContent='무료 상담 신청하기';

    const appHead=demo.querySelector('.ct-connect-apphead span');
    if(appHead)appHead.textContent='알림';
    const appSource=demo.querySelector('.ct-connect-title small');
    if(appSource)appSource.textContent='페이지로 유입';
    const doneCopy=demo.querySelector('.ct-connect-done small');
    if(doneCopy)doneCopy.textContent='바로 연락하고 후속관리할 수 있습니다.';
    return true;
  };

  const updatePageroSection=()=>{
    const section=document.querySelector('#ct-pagero-intro .ct-v8-nocode');
    const copy=section?.querySelector('.ct-v8-nocode-copy');
    if(!section||!copy)return false;

    const kicker=copy.querySelector(':scope > p');
    const heading=copy.querySelector(':scope > h2');
    if(kicker)kicker.textContent='노코드 랜딩페이지';
    if(heading)heading.innerHTML='고객 문의를 받을 페이지가 필요하다면,<br><span>페이지로에서 바로 만드세요.</span>';

    let description=copy.querySelector('.ct-pagero-nocode-description');
    if(!description){
      description=document.createElement('strong');
      description.className='ct-pagero-nocode-description';
      heading?.insertAdjacentElement('afterend',description);
    }
    description.innerHTML='문구와 이미지만 바꾸면<br>모바일 랜딩페이지와 고객 문의 폼이 완성됩니다.';

    let benefits=copy.querySelector('.ct-pagero-nocode-benefits');
    if(!benefits){
      benefits=document.createElement('div');
      benefits.className='ct-pagero-nocode-benefits';
      benefits.innerHTML='<span>코드 없이 제작</span><span>모바일 자동 최적화</span><span>고객 문의 바로 수집</span>';
      description.insertAdjacentElement('afterend',benefits);
    }

    let actions=copy.querySelector('.ct-pagero-nocode-actions');
    if(!actions){
      actions=document.createElement('div');
      actions.className='ct-pagero-nocode-actions';
      actions.innerHTML=`<a class="ct-pagero-nocode-cta" href="${APP_URL}">페이지로 무료로 시작하기 →</a><small>설치 없이 웹에서 바로 시작</small>`;
      benefits.insertAdjacentElement('afterend',actions);
    }
    return true;
  };

  const apply=()=>{
    installStyle();
    const a=updateIntegrationCopy();
    const b=updateFlowVisual();
    const c=updatePageroSection();
    return a&&b&&c;
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(()=>requestAnimationFrame(apply));
    observer.observe(document.documentElement,{childList:true,subtree:true,characterData:true});
    [0,80,180,400,800,1500,3000,6000,10000].forEach(delay=>setTimeout(apply,delay));
    setTimeout(()=>observer.disconnect(),15000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
