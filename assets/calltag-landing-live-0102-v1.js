(()=>{
  if(document.documentElement.dataset.ctLanding0102V1)return;
  document.documentElement.dataset.ctLanding0102V1='1';
  const APP='https://pagero.kr/app';
  const TITLE='전화가 끝난 뒤,<br><span>고객관리는 시작됩니다.</span>';
  const DESCRIPTION='고객 · 상담 · 다음 할 일. 통화 직후 바로 남기세요.';
  let applying=false;
  let queued=false;

  const installStyle=()=>{
    if(document.getElementById('ct-live-0102-style'))return;
    const style=document.createElement('style');
    style.id='ct-live-0102-style';
    style.textContent=`
      .ct-live-hero-actions{display:flex;justify-content:center;align-items:center;gap:12px;margin-top:26px;flex-wrap:wrap}
      .ct-live-hero-primary{min-height:50px;display:inline-flex;align-items:center;justify-content:center;padding:0 20px;border-radius:12px;background:#3b6fff;color:#fff!important;font-size:14px;font-weight:900;text-decoration:none}
      .ct-live-hero-link{min-height:50px;display:inline-flex;align-items:center;justify-content:center;padding:0 8px;color:#c8ccd5!important;font-size:13px;font-weight:800;text-decoration:none}
      #ct-live-pain{padding:104px 0;border-top:1px solid rgba(255,255,255,.08);border-bottom:1px solid rgba(255,255,255,.08)}
      #ct-live-pain .ct-live-pain-head{text-align:center}
      #ct-live-pain .ct-live-pain-kicker{margin:0 0 12px;color:#7c99ff;font-size:13px;font-weight:900}
      #ct-live-pain h2{margin:0;font-size:clamp(42px,4.7vw,66px);line-height:1.05;letter-spacing:-.07em}
      #ct-live-pain .ct-live-pain-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:1px;margin-top:38px;overflow:hidden;border:1px solid rgba(255,255,255,.105);border-radius:18px;background:rgba(255,255,255,.105)}
      #ct-live-pain .ct-live-pain-card{min-height:126px;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:22px;background:#111319;text-align:center}
      #ct-live-pain .ct-live-pain-card b{font-size:24px;letter-spacing:-.045em}
      #ct-live-pain .ct-live-pain-card span{margin-top:8px;color:#737986;font-size:13px}
      #ct-live-pain .ct-live-pain-end{margin:24px 0 0;text-align:center;color:#d9dde5;font-size:17px;font-weight:850}
      #ct-live-pain .ct-live-pain-end strong{color:#7c99ff}
      @media(max-width:640px){
        .ct-live-hero-actions{display:grid;grid-template-columns:1fr;gap:4px;margin-top:22px}
        .ct-live-hero-primary,.ct-live-hero-link{width:min(100%,330px);margin:0 auto}
        #ct-live-pain{padding:78px 0}
        #ct-live-pain h2{font-size:40px}
        #ct-live-pain .ct-live-pain-grid{grid-template-columns:1fr;gap:1px;margin-top:30px}
        #ct-live-pain .ct-live-pain-card{min-height:92px}
        #ct-live-pain .ct-live-pain-card b{font-size:21px}
      }
    `;
    document.head.append(style);
  };

  const makePain=()=>{
    const section=document.createElement('section');
    section.id='ct-live-pain';
    section.className='ad-section alt ct-live-pain';
    section.innerHTML=`<div class="wrap"><div class="ct-live-pain-head"><p class="ct-live-pain-kicker">통화 뒤에 남는 일</p><h2>기억으로 관리하면<br>하나씩 놓칩니다.</h2></div><div class="ct-live-pain-grid"><div class="ct-live-pain-card"><b>누구였지?</b><span>상담 내용</span></div><div class="ct-live-pain-card"><b>언제 다시?</b><span>재연락 일정</span></div><div class="ct-live-pain-card"><b>보냈나?</b><span>자료 · 견적</span></div></div><p class="ct-live-pain-end">기억 말고, <strong>콜태그에 남기세요.</strong></p></div>`;
    return section;
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
      if(heading&&!heading.querySelector('.ct-live-hero-actions')){
        const actions=document.createElement('div');
        actions.className='ct-live-hero-actions';
        actions.innerHTML=`<a class="ct-live-hero-primary" href="${APP}" target="_blank" rel="noopener">7일 무료로 시작하기</a><a class="ct-live-hero-link" href="#ct-live-pain">왜 필요한지 보기 ↓</a>`;
        heading.append(actions);
      }
      if(app){
        let pain=document.getElementById('ct-live-pain');
        if(!pain)pain=makePain();
        if(app.nextElementSibling!==pain)app.insertAdjacentElement('afterend',pain);
      }
    }finally{
      applying=false;
    }
  };

  const queueApply=()=>{
    if(queued||applying)return;
    queued=true;
    requestAnimationFrame(apply);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});
  else apply();
  [80,220,500,900,1500,2400,3600,5200].forEach(delay=>setTimeout(apply,delay));
  const observer=new MutationObserver(queueApply);
  observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true});
  setTimeout(()=>observer.disconnect(),7000);
})();
