(()=>{
  if(document.documentElement.dataset.ctPricingRedesign)return;
  document.documentElement.dataset.ctPricingRedesign='1';

  const run=()=>{
    const strengths=document.querySelector('#strengths');
    if(strengths){
      [...strengths.querySelectorAll('span,em')].forEach(node=>{
        if(node.textContent.includes('연동 출시 예정'))node.remove();
      });
      const future=strengths.querySelector('.ct-strength-grid article.future');
      if(future){
        future.classList.remove('future');
        const copy=future.querySelector('p');
        if(copy)copy.textContent='페이지로에서 접수된 고객을 콜태그에 자동 등록해 전화·문자 후속관리까지 연결합니다.';
      }
    }

    const pricing=document.querySelector('#pricing');
    if(!pricing)return;

    pricing.className='ad-section ct-pricing-redesign';
    pricing.innerHTML=`
      <div class="wrap ct-pricing-wrap">
        <header class="ct-pricing-head">
          <p>요금제</p>
          <h2>필요한 기능만 <span>선택하세요.</span></h2>
          <strong>2026년 가입 가격은 해지 전까지 유지됩니다.</strong>
        </header>
        <div class="ct-pricing-grid">
          <article class="ct-plan-card">
            <small>CALL</small>
            <h3>전화관리</h3>
            <div class="ct-plan-price"><b>1,900원</b><span>/ 월</span></div>
          </article>
          <article class="ct-plan-card">
            <small>MESSAGE</small>
            <h3>문자자동화</h3>
            <div class="ct-plan-price"><b>990원</b><span>/ 월</span></div>
          </article>
          <article class="ct-plan-card pagero">
            <small>PAGERO</small>
            <h3>페이지로</h3>
            <div class="ct-plan-price"><b>3,500원</b><span>/ 월</span></div>
          </article>
          <article class="ct-plan-card all">
            <span class="ct-plan-badge">추천</span>
            <small>ALL IN ONE</small>
            <h3>통합권</h3>
            <div class="ct-plan-price"><b>5,500원</b><span>/ 월</span></div>
          </article>
        </div>
      </div>`;

    if(!document.querySelector('style[data-ct-pricing-redesign]')){
      const style=document.createElement('style');
      style.dataset.ctPricingRedesign='1';
      style.textContent=`
        #pricing.ct-pricing-redesign{padding:110px 0!important;background:#090b10!important}
        .ct-pricing-wrap{max-width:1500px!important}
        .ct-pricing-head{text-align:center;margin-bottom:38px}
        .ct-pricing-head>p{margin:0 0 10px;color:var(--blue-2);font-size:13px;font-weight:900}
        .ct-pricing-head h2{margin:0;font-size:clamp(42px,5vw,66px);line-height:1.04;letter-spacing:-.072em}
        .ct-pricing-head h2 span{color:var(--blue-2)}
        .ct-pricing-head>strong{display:block;margin-top:15px;color:#c7ccd7;font-size:14px}
        .ct-pricing-grid{display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;gap:14px!important}
        .ct-plan-card{position:relative;min-width:0;min-height:250px!important;display:flex;flex-direction:column;justify-content:space-between;padding:29px 27px!important;border:1px solid rgba(255,255,255,.14);border-radius:22px;background:linear-gradient(155deg,#171a21,#111319);box-shadow:0 18px 48px rgba(0,0,0,.2)}
        .ct-plan-card.pagero{border-color:rgba(124,153,255,.42)}
        .ct-plan-card.all{border-color:rgba(59,111,255,.88);background:linear-gradient(155deg,rgba(59,111,255,.2),#121722 54%);box-shadow:0 24px 64px rgba(59,111,255,.16)}
        .ct-plan-card>small{display:block;color:var(--blue-2);font-size:10px;font-weight:900;letter-spacing:.05em}
        .ct-plan-card h3{margin:18px 0 0;color:#fff;font-size:clamp(28px,2.15vw,36px);line-height:1;letter-spacing:-.06em}
        .ct-plan-price{display:flex;align-items:flex-end;gap:6px;margin-top:auto;padding-top:48px;white-space:nowrap}
        .ct-plan-price b{color:#fff;font-size:clamp(38px,3vw,50px);line-height:1;letter-spacing:-.075em}
        .ct-plan-price span{margin-bottom:5px;color:#aeb4c0;font-size:11px;font-weight:750}
        .ct-plan-badge{position:absolute;top:20px;right:20px;padding:7px 10px;border-radius:999px;background:var(--blue);color:#fff;font-size:9px;font-weight:900}
        @media(max-width:1050px){
          .ct-pricing-wrap{max-width:850px!important}
          .ct-pricing-grid{grid-template-columns:repeat(2,minmax(0,1fr))!important}
          .ct-plan-card{min-height:230px!important}
        }
        @media(max-width:650px){
          #pricing.ct-pricing-redesign{padding:80px 0!important}
          .ct-pricing-head{margin-bottom:28px}.ct-pricing-head h2{font-size:38px}.ct-pricing-head>strong{font-size:12px;line-height:1.5}
          .ct-pricing-grid{grid-template-columns:1fr!important}
          .ct-plan-card{min-height:205px!important;padding:23px 21px!important;border-radius:19px}
          .ct-plan-card h3{font-size:31px}.ct-plan-price{padding-top:34px}.ct-plan-price b{font-size:43px}
          .ct-plan-badge{top:17px;right:17px}
        }
      `;
      document.head.append(style);
    }
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(run),{once:true});
  else requestAnimationFrame(run);
})();