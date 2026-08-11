(()=>{
  if(document.documentElement.dataset.ctAdBootstrapV5)return;
  document.documentElement.dataset.ctAdBootstrapV5='1';
  const q=(s,r=document)=>r.querySelector(s);
  const make=html=>{const t=document.createElement('template');t.innerHTML=html.trim();return t.content.firstElementChild;};
  if(!document.head.querySelector('style[data-ct-ad-bootstrap-v5]')){
    const style=document.createElement('style');
    style.dataset.ctAdBootstrapV5='1';
    style.textContent=`
      .ad-section{padding:116px 0;border-top:1px solid var(--line)}.ad-section.alt{background:#0d0f13}.ad-head{max-width:780px;margin:0 auto 46px;text-align:center}.ad-kicker{margin:0 0 14px;color:var(--blue-2);font-size:14px;font-weight:900}.ad-title{margin:0;font-size:clamp(42px,5vw,72px);line-height:1.04;letter-spacing:-.07em}.ad-targets{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.ad-target{min-height:210px;padding:25px;border:1px solid var(--line);border-radius:20px;background:var(--surface)}.ad-target span{color:var(--blue-2);font-size:11px;font-weight:900}.ad-target h3{margin:19px 0 0;font-size:23px;letter-spacing:-.05em}.ad-target b{display:block;margin-top:24px;color:#e8eaf0;font-size:12px}.ad-benefits{display:grid;grid-template-columns:repeat(4,1fr);gap:1px;overflow:hidden;border:1px solid var(--line);border-radius:22px;background:var(--line)}.ad-benefit{min-height:190px;padding:26px;background:#11141a}@media(max-width:1000px){.ad-targets,.ad-benefits{grid-template-columns:repeat(2,1fr)}}@media(max-width:700px){.ad-section{padding:86px 0}.ad-targets,.ad-benefits{grid-template-columns:1fr}}
    `;
    document.head.append(style);
  }
  const nav=q('.nav');
  if(nav)nav.innerHTML="<a href='#app'>기능</a><a href='#targets'>추천 업종</a><a href='#messages'>문자</a><a href='#strengths'>강점</a><a href='#pricing'>요금</a><a href='#faq'>FAQ</a>";
  const step=q('.step-title');if(step)step.innerHTML='통화가 끝난 직후<br><span>고객 정보를 남기세요.</span>';
  const sub=q('.step-sub');if(sub)sub.textContent='고객 구분, 상담 상태, 다음 할 일, 재연락 날짜를 한 번에 입력합니다.';
  const app=q('#app'),web=q('#web');
  let what=q('#what');
  if(!what&&app){what=make('<section class="ad-section alt" id="what"></section>');app.insertAdjacentElement('afterend',what);}
  let targets=q('#targets');
  if(!targets&&what){
    targets=make(`<section class="ad-section" id="targets"><div class="wrap"><div class="ad-head"><p class="ad-kicker">누가 써야 하나요?</p><h2 class="ad-title">이런 업종에 필요합니다.</h2></div><div class="ad-targets"><article class="ad-target"><span>보험·영업</span><h3>견적 후 재연락</h3><b>상담 → 견적 → 3일 후 전화</b></article><article class="ad-target"><span>부동산·자동차</span><h3>문의 후 방문 안내</h3><b>문의 → 방문 예약 → 위치 문자</b></article><article class="ad-target"><span>병원·미용·예약매장</span><h3>예약 확정과 방문 알림</h3><b>예약 → 확정문자 → 방문 알림</b></article><article class="ad-target"><span>학원·교육</span><h3>상담 후 등록 안내</h3><b>상담 → 자료 발송 → 재상담</b></article><article class="ad-target"><span>법무·세무</span><h3>상담과 서류 요청</h3><b>상담 → 서류 요청 → 진행 안내</b></article><article class="ad-target"><span>쇼핑몰·고객지원</span><h3>문의와 처리 결과</h3><b>문의 → 처리 → 결과 문자</b></article></div></div></section>`);
    what.insertAdjacentElement('afterend',targets);
  }
  if(targets&&!q('.ad-benefits'))targets.insertAdjacentElement('afterend',make('<section class="ad-section alt"><div class="wrap"><div class="ad-head"><p class="ad-kicker">쓰면 무엇이 좋아지나요?</p><h2 class="ad-title">반복 업무 그만하세요</h2></div><div class="ad-benefits"><article class="ad-benefit"></article><article class="ad-benefit"></article><article class="ad-benefit"></article><article class="ad-benefit"></article></div></div></section>'));
  if(web&&!q('#messages'))web.insertAdjacentElement('beforebegin',make('<section class="ad-section" id="messages"></section>'));
  if(!q('#strengths')){
    const strength=make('<section class="ad-section alt" id="strengths"></section>');
    const reviews=q('#reviews');
    if(reviews)reviews.replaceWith(strength);else (q('.ad-benefits')?.closest('.ad-section')||targets||what)?.insertAdjacentElement('afterend',strength);
  }
  q('.audience')?.remove();
})();