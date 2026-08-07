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
            <div class="ct-plan-pricebox">
              <div class="ct-plan-sale"><del>정가 월 6,900원</del><em>5,000원 할인</em></div>
              <div class="ct-plan-price"><b>1,900원</b><span>/ 월</span></div>
            </div>
            <div class="ct-plan-divider"></div>
            <span class="ct-plan-label">포함 기능</span>
            <ul class="ct-plan-points">
              <li><i>✓</i>통화 후 태그</li><li><i>✓</i>고객 상태 기록</li><li><i>✓</i>재연락 일정</li><li><i>✓</i>오늘 할 일</li><li><i>✓</i>상담 이력 관리</li>
            </ul>
          </article>
          <article class="ct-plan-card">
            <small>MESSAGE</small>
            <h3>문자자동화</h3>
            <div class="ct-plan-pricebox">
              <div class="ct-plan-sale"><del>정가 월 4,900원</del><em>3,910원 할인</em></div>
              <div class="ct-plan-price"><b>990원</b><span>/ 월</span></div>
            </div>
            <div class="ct-plan-divider"></div>
            <span class="ct-plan-label">포함 기능</span>
            <ul class="ct-plan-points">
              <li><i>✓</i>자동문자 발송</li><li><i>✓</i>수신·발신 대응</li><li><i>✓</i>부재중 안내</li><li><i>✓</i>후속문자 예약</li><li><i>✓</i>중복발송 방지</li>
            </ul>
          </article>
          <article class="ct-plan-card pagero">
            <small>PAGERO</small>
            <h3>페이지로</h3>
            <div class="ct-plan-pricebox">
              <div class="ct-plan-sale"><del>정가 월 9,900원</del><em>6,400원 할인</em></div>
              <div class="ct-plan-price"><b>3,500원</b><span>/ 월</span></div>
            </div>
            <div class="ct-plan-divider"></div>
            <span class="ct-plan-label">포함 기능</span>
            <ul class="ct-plan-points">
              <li><i>✓</i>랜딩페이지 제작</li><li><i>✓</i>고객 접수 수집</li><li><i>✓</i>광고용 페이지</li><li><i>✓</i>모바일 최적화</li><li><i>✓</i>빠른 배포</li>
            </ul>
          </article>
          <article class="ct-plan-card all">
            <span class="ct-plan-badge">추천</span>
            <small>ALL IN ONE</small>
            <h3>통합권</h3>
            <div class="ct-plan-pricebox">
              <div class="ct-plan-sale"><del>정가 월 19,900원</del><em>13,900원 할인</em></div>
              <div class="ct-plan-price"><b>6,000원</b><span>/ 월</span></div>
            </div>
            <div class="ct-plan-divider"></div>
            <span class="ct-plan-label">포함 기능</span>
            <ul class="ct-plan-points">
              <li><i>✓</i>고객 접수부터</li><li><i>✓</i>전화관리까지</li><li><i>✓</i>문자 후속관리</li><li><i>✓</i>자동 고객 등록</li><li><i>✓</i>한 번에 통합</li>
            </ul>
          </article>
        </div>
        <p class="ct-vat-note">모든 요금제는 부가세 별도입니다.</p>
      </div>`;

    if(!document.querySelector('style[data-ct-pricing-redesign]')){
      const style=document.createElement('style');
      style.dataset.ctPricingRedesign='1';
      style.textContent=`
        #pricing.ct-pricing-redesign{padding:110px 0!important;background:#090b10!important}
        .ct-pricing-wrap{max-width:1500px!important}
        .ct-pricing-head{text-align:center;margin-bottom:42px}
        .ct-pricing-head>p{margin:0 0 10px;color:var(--blue-2);font-size:13px;font-weight:850}
        .ct-pricing-head h2{margin:0;font-size:clamp(42px,5vw,66px);line-height:1.04;letter-spacing:-.072em}
        .ct-pricing-head h2 span{color:var(--blue-2)}
        .ct-pricing-head>strong{display:block;margin-top:16px;color:#c7ccd7;font-size:14px;font-weight:650}
        .ct-pricing-grid{display:grid!important;grid-template-columns:repeat(4,minmax(0,1fr))!important;gap:14px!important}
        .ct-plan-card{position:relative;min-width:0;min-height:470px!important;padding:31px 29px!important;border:1px solid rgba(255,255,255,.14);border-radius:22px;background:linear-gradient(155deg,#171a21,#111319);box-shadow:0 18px 48px rgba(0,0,0,.2)}
        .ct-plan-card.pagero{border-color:rgba(124,153,255,.42)}
        .ct-plan-card.all{border-color:rgba(59,111,255,.9);background:linear-gradient(155deg,rgba(59,111,255,.2),#121722 54%);box-shadow:0 24px 64px rgba(59,111,255,.16)}
        .ct-plan-card>small{display:block;color:#8ca4ff;font-size:10px;font-weight:850;letter-spacing:.06em}
        .ct-plan-card h3{margin:17px 0 0;color:#fff;font-size:clamp(29px,2.15vw,36px);line-height:1;letter-spacing:-.06em}
        .ct-plan-pricebox{margin-top:30px}
        .ct-plan-sale{display:flex;align-items:center;gap:9px;flex-wrap:wrap}
        .ct-plan-sale del{color:#727986;font-size:12px;font-weight:600}
        .ct-plan-sale em{padding:5px 9px;border-radius:999px;background:rgba(59,111,255,.14);color:#9fb1ff;font-size:10px;font-style:normal;font-weight:750;white-space:nowrap}
        .ct-plan-price{display:flex;align-items:flex-end;gap:7px;margin-top:11px;white-space:nowrap}
        .ct-plan-price b{color:#fff;font-size:clamp(40px,3vw,52px);line-height:1;letter-spacing:-.075em}
        .ct-plan-price span{margin-bottom:6px;color:#aeb4c0;font-size:11px;font-weight:650}
        .ct-plan-divider{height:1px;margin:27px 0 21px;background:rgba(255,255,255,.11)}
        .ct-plan-label{display:block;color:#89909d;font-size:10px;font-weight:750;letter-spacing:.03em}
        .ct-plan-points{display:grid;gap:12px;margin:17px 0 0;padding:0;list-style:none}
        .ct-plan-points li{display:flex;align-items:flex-start;gap:10px;color:#d9dee8;font-size:clamp(14px,1.02vw,17px);font-weight:600;line-height:1.42;letter-spacing:-.025em}
        .ct-plan-points li i{flex:0 0 auto;color:#7595ff;font-size:13px;font-style:normal;font-weight:900;line-height:1.55}
        .ct-plan-badge{position:absolute;top:21px;right:21px;padding:7px 11px;border-radius:999px;background:var(--blue);color:#fff;font-size:9px;font-weight:850}
        .ct-vat-note{margin:19px 0 0;color:#9da4b2;font-size:12px;font-weight:650;text-align:right}
        .ct-vat-note:before{content:'* ';color:var(--blue-2)}
        @media(max-width:1180px){
          .ct-pricing-wrap{max-width:900px!important}.ct-pricing-grid{grid-template-columns:repeat(2,minmax(0,1fr))!important}.ct-plan-card{min-height:455px!important}.ct-plan-points li{font-size:16px}
        }
        @media(max-width:650px){
          #pricing.ct-pricing-redesign{padding:80px 0!important}.ct-pricing-head{margin-bottom:30px}.ct-pricing-head h2{font-size:38px}.ct-pricing-head>strong{font-size:12px;line-height:1.5}
          .ct-pricing-grid{grid-template-columns:1fr!important}.ct-plan-card{min-height:0!important;padding:25px 22px 27px!important;border-radius:19px}.ct-plan-card h3{font-size:31px}.ct-plan-pricebox{margin-top:27px}.ct-plan-price b{font-size:44px}.ct-plan-divider{margin:24px 0 19px}.ct-plan-points{gap:11px}.ct-plan-points li{font-size:16px}.ct-plan-badge{top:18px;right:18px}.ct-vat-note{text-align:center;font-size:11px}
        }
      `;
      document.head.append(style);
    }
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(run),{once:true});
  else requestAnimationFrame(run);
})();