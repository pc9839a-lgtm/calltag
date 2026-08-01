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
        if(copy)copy.textContent='페이지로 랜딩에서 접수된 고객을 콜태그에 자동 등록해 전화·문자 후속관리까지 연결합니다.';
      }
    }

    const pricing=document.querySelector('#pricing');
    if(!pricing)return;

    pricing.className='ad-section ct-pricing-redesign';
    pricing.innerHTML=`
      <div class="wrap ct-pricing-wrap">
        <header class="ct-pricing-head">
          <p>요금제</p>
          <h2>필요한 기능만<br><span>선택하세요.</span></h2>
          <strong>2026년 가입 가격은 해지 전까지 유지됩니다.</strong>
        </header>
        <div class="ct-pricing-grid">
          <article class="ct-plan-card">
            <div class="ct-plan-top"><div><small>CALL</small><h3>전화관리</h3></div><div class="ct-plan-price"><b>1,900원</b><span>/ 월</span></div></div>
            <p>통화 직후 고객 상태와 다음 연락을 바로 남깁니다.</p>
            <ul><li>통화 종료 후 고객 태그</li><li>상담 상태·메모 저장</li><li>오늘 할 일·재연락 관리</li></ul>
          </article>
          <article class="ct-plan-card">
            <div class="ct-plan-top"><div><small>MESSAGE</small><h3>문자자동화</h3></div><div class="ct-plan-price"><b>990원</b><span>/ 월</span></div></div>
            <p>통화 상황에 맞는 문자를 즉시 또는 예약 발송합니다.</p>
            <ul><li>수신·발신·부재중 문자</li><li>예약·후속문자 발송</li><li>템플릿·중복발송 방지</li></ul>
          </article>
          <article class="ct-plan-card pagero">
            <div class="ct-plan-top"><div><small>PAGERO</small><h3>페이지로</h3></div><div class="ct-plan-price"><b>3,500원</b><span>/ 월</span></div></div>
            <p>광고용 랜딩페이지를 만들고 고객 접수를 받습니다.</p>
            <ul><li>랜딩페이지 제작</li><li>모바일 고객 접수 폼</li><li>접수 고객 자동 저장</li></ul>
          </article>
          <article class="ct-plan-card all">
            <span class="ct-plan-badge">가장 많이 선택</span>
            <div class="ct-plan-top"><div><small>ALL IN ONE</small><h3>통합권</h3></div><div class="ct-plan-price"><b>5,500원</b><span>/ 월</span></div></div>
            <p>페이지로 접수부터 콜태그 전화·문자 후속관리까지 한 번에 사용합니다.</p>
            <ul><li>전화관리·문자자동화 전체 기능</li><li>페이지로 랜딩페이지 전체 기능</li><li>접수 고객 콜태그 자동등록</li></ul>
          </article>
        </div>
      </div>`;

    if(!document.querySelector('style[data-ct-pricing-redesign]')){
      const style=document.createElement('style');
      style.dataset.ctPricingRedesign='1';
      style.textContent=`
        #pricing.ct-pricing-redesign{padding:128px 0!important;background:#090b10!important}
        .ct-pricing-wrap{max-width:1180px!important}
        .ct-pricing-head{text-align:center;margin-bottom:48px}
        .ct-pricing-head>p{margin:0 0 12px;color:var(--blue-2);font-size:13px;font-weight:900}
        .ct-pricing-head h2{margin:0;font-size:clamp(44px,5.4vw,72px);line-height:1.02;letter-spacing:-.075em}
        .ct-pricing-head h2 span{color:var(--blue-2)}
        .ct-pricing-head>strong{display:block;margin-top:18px;color:#c7ccd7;font-size:15px}
        .ct-pricing-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}
        .ct-plan-card{position:relative;min-height:330px;padding:34px;border:1px solid rgba(255,255,255,.14);border-radius:24px;background:linear-gradient(145deg,#171a21,#111319);box-shadow:0 20px 60px rgba(0,0,0,.2)}
        .ct-plan-card.pagero{border-color:rgba(124,153,255,.42)}
        .ct-plan-card.all{border-color:rgba(59,111,255,.86);background:linear-gradient(145deg,rgba(59,111,255,.2),#121722 52%);box-shadow:0 26px 70px rgba(59,111,255,.16)}
        .ct-plan-badge{position:absolute;top:22px;right:22px;padding:7px 11px;border-radius:999px;background:var(--blue);color:#fff;font-size:10px;font-weight:900}
        .ct-plan-top{display:flex;align-items:flex-start;justify-content:space-between;gap:24px}
        .ct-plan-top small{display:block;color:var(--blue-2);font-size:11px;font-weight:900;letter-spacing:.04em}
        .ct-plan-top h3{margin:12px 0 0;color:#fff;font-size:35px;line-height:1;letter-spacing:-.06em}
        .ct-plan-price{text-align:right;white-space:nowrap}
        .ct-plan-price b{display:block;color:#fff;font-size:clamp(42px,4vw,58px);line-height:1;letter-spacing:-.075em}
        .ct-plan-price span{display:block;margin-top:7px;color:#b8becb;font-size:12px;font-weight:700}
        .ct-plan-card>p{max-width:510px;margin:31px 0 0;color:#d3d7e0;font-size:16px;line-height:1.6}
        .ct-plan-card ul{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin:30px 0 0;padding:0;list-style:none}
        .ct-plan-card li{min-height:55px;display:flex;align-items:center;padding:12px 13px;border:1px solid rgba(255,255,255,.1);border-radius:12px;background:rgba(255,255,255,.035);color:#eef0f5;font-size:12px;font-weight:750;line-height:1.4}
        .ct-plan-card li:before{content:'✓';margin-right:8px;color:#86a0ff;font-weight:900}
        @media(max-width:900px){#pricing.ct-pricing-redesign{padding:88px 0!important}.ct-pricing-grid{grid-template-columns:1fr}.ct-plan-card{min-height:0;padding:27px}.ct-plan-price b{font-size:46px}}
        @media(max-width:600px){.ct-pricing-head{margin-bottom:32px}.ct-pricing-head h2{font-size:39px}.ct-pricing-head>strong{font-size:13px;line-height:1.5}.ct-plan-card{padding:24px 20px;border-radius:20px}.ct-plan-top{display:block}.ct-plan-price{margin-top:25px;text-align:left}.ct-plan-price b{font-size:43px}.ct-plan-top h3{font-size:31px}.ct-plan-card>p{margin-top:21px;font-size:14px}.ct-plan-card ul{grid-template-columns:1fr;margin-top:22px}.ct-plan-card li{min-height:48px;font-size:12px}.ct-plan-badge{top:18px;right:18px;font-size:9px}}
      `;
      document.head.append(style);
    }
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(run),{once:true});
  else requestAnimationFrame(run);
})();