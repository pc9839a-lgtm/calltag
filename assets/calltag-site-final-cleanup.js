(()=>{
  if(document.documentElement.dataset.ctSiteFinalCleanup)return;
  document.documentElement.dataset.ctSiteFinalCleanup='1';

  const footerMarkup=`
    <div class="wrap ct-wayzi-footer__inner">
      <div class="ct-wayzi-footer__brand">
        <strong>웨이지 <span>WAYZI</span></strong>
        <p>콜태그 · 페이지로 운영사</p>
      </div>
      <div class="ct-wayzi-footer__info">
        <p><b>대표</b> 김도윤 <i></i> <b>사업자등록번호</b> 538-42-01450</p>
        <p><b>주소</b> 인천광역시 계양구 오조산로89번길 5, 4층 402-37호(용종동)</p>
        <p><b>문의</b> <a href="mailto:roadfor@kakao.com">roadfor@kakao.com</a> <i></i> <a href="tel:01057669839">010-5766-9839</a></p>
        <small>© 2026 WAYZI. All rights reserved.</small>
      </div>
    </div>`;

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-site-final-cleanup]'))return;
    const style=document.createElement('style');
    style.dataset.ctSiteFinalCleanup='1';
    style.textContent=`
      :root{
        --ct-display-title:clamp(54px,6.4vw,92px);
        --ct-section-title:clamp(42px,4.8vw,68px);
      }

      .hero-heading h1,
      .ct-v8-head h1{
        font-size:var(--ct-display-title)!important;
        line-height:.96!important;
        letter-spacing:-.078em!important;
      }

      .web-heading-copy h2,
      .section-title,
      .feature-copy h3,
      .ad-title,
      .ad-message-copy h2,
      .ct-v8-nocode-copy h2,
      .ct-horizontal-clean__copy h2,
      .ct-horizontal-clean__copy h3,
      .audience h2,
      #pricing h2,
      #faq h2{
        font-size:var(--ct-section-title)!important;
        line-height:1.02!important;
        letter-spacing:-.07em!important;
      }

      .ad-final,
      .ad-sticky,
      .sticky-offer,
      .floating-offer,
      .bottom-offer,
      .offer-bar,
      .fixed-offer,
      .ct-fixed-offer{
        display:none!important;
      }

      .ct-wayzi-footer{
        padding:46px 0 50px!important;
        border-top:1px solid var(--line)!important;
        background:#090a0d!important;
        color:#7f8591!important;
      }
      .ct-wayzi-footer__inner{
        display:grid!important;
        grid-template-columns:minmax(190px,.6fr) minmax(0,1.4fr)!important;
        gap:48px!important;
        align-items:start!important;
      }
      .ct-wayzi-footer__brand strong{
        display:block;
        color:#f4f6fb;
        font-size:20px;
        font-weight:950;
        letter-spacing:-.04em;
      }
      .ct-wayzi-footer__brand strong span{color:#7897ff;font-size:12px;letter-spacing:.08em}
      .ct-wayzi-footer__brand p{margin:9px 0 0;color:#777e8c;font-size:12px}
      .ct-wayzi-footer__info{display:grid;gap:8px}
      .ct-wayzi-footer__info p{margin:0;color:#8c929e;font-size:12px;line-height:1.65}
      .ct-wayzi-footer__info b{color:#c7cbd4;font-weight:850}
      .ct-wayzi-footer__info i{display:inline-block;width:1px;height:10px;margin:0 9px;background:rgba(255,255,255,.16);vertical-align:-1px}
      .ct-wayzi-footer__info a{color:#aeb8d8}
      .ct-wayzi-footer__info small{display:block;margin-top:7px;color:#606672;font-size:11px}

      @media(max-width:700px){
        :root{--ct-display-title:48px;--ct-section-title:40px}
        .ct-wayzi-footer{padding:38px 0 42px!important}
        .ct-wayzi-footer__inner{grid-template-columns:1fr!important;gap:24px!important}
        .ct-wayzi-footer__info p{font-size:11px}
        .ct-wayzi-footer__info i{display:none}
      }
    `;
    document.head.append(style);
  };

  const apply=()=>{
    installStyle();

    document.querySelectorAll(
      '.ad-final,.ad-sticky,.sticky-offer,.floating-offer,.bottom-offer,.offer-bar,.fixed-offer,.ct-fixed-offer'
    ).forEach(element=>element.remove());

    const pageroCopy=document.querySelector('#ct-pagero-intro .ct-v8-head>strong');
    if(pageroCopy&&pageroCopy.dataset.ctCopyFinal!=='1'){
      pageroCopy.innerHTML='페이지로에서 문의를 받으면<br>관리는 <em>콜태그 앱에서!</em>';
      pageroCopy.dataset.ctCopyFinal='1';
    }

    const tagTitle=document.querySelector('.ct-journey-clean .ct-horizontal-clean__panel:nth-child(3) .ct-horizontal-clean__copy h2');
    if(tagTitle&&tagTitle.dataset.ctCopyFinal!=='1'){
      tagTitle.innerHTML='전화가 끝나면<br>태그만 하세요';
      tagTitle.dataset.ctCopyFinal='1';
    }

    document.querySelectorAll(
      '.ct-horizontal-industries-clean .ct-horizontal-clean__copy,'+
      '.ct-industries-static .ct-horizontal-clean__copy,'+
      '.ct-industry-label,'+
      '.ct-industry-auto'
    ).forEach(element=>element.remove());

    let footer=document.querySelector('footer.footer,footer');
    if(!footer){
      footer=document.createElement('footer');
      document.body.append(footer);
    }
    footer.className='footer ct-wayzi-footer';
    if(footer.dataset.ctWayziFooter!=='1'){
      footer.innerHTML=footerMarkup;
      footer.dataset.ctWayziFooter='1';
    }
  };

  let queued=false;
  const queue=()=>{
    if(queued)return;
    queued=true;
    requestAnimationFrame(()=>{queued=false;apply()});
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(queue);
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(apply,400);
    setTimeout(apply,1200);
    setTimeout(apply,3000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();