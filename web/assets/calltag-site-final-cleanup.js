(()=>{
  if(document.documentElement.dataset.ctSiteFinalCleanupV4)return;
  document.documentElement.dataset.ctSiteFinalCleanupV4='1';

  const destinations={'이용약관':'/terms/','개인정보처리방침':'/privacy/','환불정책':'/refund/','고객센터':'/support/'};
  const footerMarkup=`
    <div class="wrap ct-wayzi-footer__inner">
      <div class="ct-wayzi-footer__brand">
        <strong>콜태그 <span>CALLTAG</span></strong>
        <p>통화 후 고객관리·자동문자<br>페이지로 문의 연동</p>
        <nav class="ct-wayzi-footer__policy" aria-label="정책 및 고객지원">
          <a href="/terms/">이용약관</a>
          <a class="is-strong" href="/privacy/">개인정보처리방침</a>
          <a href="/refund/">환불정책</a>
          <a href="/support/">고객센터</a>
          <a href="/settlement">파트너 정산</a>
        </nav>
      </div>
      <div class="ct-wayzi-footer__company">
        <strong>사업자 정보</strong>
        <dl>
          <div><dt>상호</dt><dd>웨이지 (WAYZI)</dd></div>
          <div><dt>대표</dt><dd>김도윤</dd></div>
          <div><dt>사업자등록번호</dt><dd>538-42-01450</dd></div>
          <div class="is-wide"><dt>주소</dt><dd>인천광역시 계양구 오조산로89번길 5, 4층 402-37호(용종동)</dd></div>
        </dl>
      </div>
      <div class="ct-wayzi-footer__support">
        <strong>콜태그 고객지원</strong>
        <a href="mailto:roadfor@kakao.com">roadfor@kakao.com</a>
        <p>서비스·결제·환불·개인정보 문의</p>
      </div>
    </div>
    <div class="wrap ct-wayzi-footer__bottom">
      <small>© 2026 콜태그. 운영사 웨이지(WAYZI).</small>
      <span>CALLTAG</span>
    </div>`;

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-site-final-cleanup-v4]'))return;
    const style=document.createElement('style');
    style.dataset.ctSiteFinalCleanupV4='1';
    style.textContent=`
      :root{--ct-display-title:clamp(54px,6.4vw,92px);--ct-section-title:clamp(42px,4.8vw,68px)}
      .hero-heading h1,.ct-v8-head h1{font-size:var(--ct-display-title)!important;line-height:.96!important;letter-spacing:-.078em!important}
      .web-heading-copy h2,.section-title,.feature-copy h3,.ad-title,.ad-message-copy h2,.ct-v8-nocode-copy h2,.ct-horizontal-clean__copy h2,.ct-horizontal-clean__copy h3,.audience h2,#pricing h2,#faq h2{font-size:var(--ct-section-title)!important;line-height:1.02!important;letter-spacing:-.07em!important}
      .ad-final,.ad-sticky,.sticky-offer,.floating-offer,.bottom-offer,.offer-bar,.fixed-offer,.ct-fixed-offer{display:none!important}
      .ct-wayzi-footer{position:relative!important;z-index:80!important;isolation:isolate!important;overflow:visible!important;pointer-events:auto!important;padding:0!important;border-top:1px solid rgba(255,255,255,.11)!important;background:#07080b!important;color:#858b98!important;transform:none!important;filter:none!important;opacity:1!important}
      .ct-wayzi-footer:before,.ct-wayzi-footer:after{pointer-events:none!important}.ct-wayzi-footer .wrap,.ct-wayzi-footer nav,.ct-wayzi-footer a{position:relative!important;z-index:3!important;pointer-events:auto!important}.ct-wayzi-footer a{cursor:pointer!important;touch-action:manipulation!important;-webkit-tap-highlight-color:rgba(120,151,255,.22)!important}
      .ct-wayzi-footer__inner{display:grid!important;grid-template-columns:minmax(230px,.9fr) minmax(390px,1.45fr) minmax(190px,.65fr)!important;gap:56px!important;align-items:start!important;padding-top:52px!important;padding-bottom:38px!important}
      .ct-wayzi-footer__brand>strong{display:block;color:#f4f6fb;font-size:22px;font-weight:950;letter-spacing:-.045em}.ct-wayzi-footer__brand>strong span{margin-left:5px;color:#7897ff;font-size:11px;letter-spacing:.09em;vertical-align:2px}.ct-wayzi-footer__brand>p{margin:12px 0 0;color:#737a87;font-size:12px;line-height:1.65}
      .ct-wayzi-footer__policy{display:flex;flex-wrap:wrap;gap:8px 18px;margin-top:25px}.ct-wayzi-footer__policy a{position:relative;color:#aeb4c0;font-size:12px;font-weight:800;transition:color .2s ease}.ct-wayzi-footer__policy a:hover{color:#fff}.ct-wayzi-footer__policy a.is-strong{color:#dce3ff}.ct-wayzi-footer__policy a+a:before{content:'';position:absolute;left:-10px;top:3px;width:1px;height:10px;background:rgba(255,255,255,.14)}
      .ct-wayzi-footer__company>strong,.ct-wayzi-footer__support>strong{display:block;margin-bottom:16px;color:#d5d8e0;font-size:12px;font-weight:900;letter-spacing:.01em}.ct-wayzi-footer__company dl{display:grid;grid-template-columns:1fr 1fr;gap:9px 26px;margin:0}.ct-wayzi-footer__company dl>div{display:grid;grid-template-columns:88px minmax(0,1fr);gap:10px;align-items:start}.ct-wayzi-footer__company dl>div.is-wide{grid-column:1/-1}.ct-wayzi-footer__company dt{color:#686f7c;font-size:11px;font-weight:750}.ct-wayzi-footer__company dd{margin:0;color:#a6acb7;font-size:11px;line-height:1.55}
      .ct-wayzi-footer__support{display:flex;flex-direction:column;align-items:flex-start}.ct-wayzi-footer__support a{color:#b8bfcc;font-size:12px;font-weight:800;line-height:1.8}.ct-wayzi-footer__support a:hover{color:#fff}.ct-wayzi-footer__support p{margin:10px 0 0;color:#676e7a;font-size:10px;line-height:1.55}
      .ct-wayzi-footer__bottom{min-height:58px;display:flex!important;align-items:center!important;justify-content:space-between!important;gap:20px!important;border-top:1px solid rgba(255,255,255,.075)!important}.ct-wayzi-footer__bottom small{color:#555b66;font-size:10px}.ct-wayzi-footer__bottom span{color:#505765;font-size:9px;font-weight:900;letter-spacing:.12em}
      @media(max-width:980px){.ct-wayzi-footer__inner{grid-template-columns:1fr 1fr!important;gap:38px!important}.ct-wayzi-footer__support{grid-column:2}}
      @media(max-width:700px){:root{--ct-display-title:48px;--ct-section-title:40px}.ct-wayzi-footer__inner{display:flex!important;flex-direction:column!important;gap:30px!important;padding-top:38px!important;padding-bottom:30px!important}.ct-wayzi-footer__brand,.ct-wayzi-footer__company,.ct-wayzi-footer__support{width:100%!important}.ct-wayzi-footer__brand>strong{font-size:20px}.ct-wayzi-footer__brand>p{font-size:11px}.ct-wayzi-footer__policy{display:grid!important;grid-template-columns:1fr 1fr!important;gap:8px!important;margin-top:20px!important}.ct-wayzi-footer__policy a{min-height:42px;display:flex;align-items:center;justify-content:center;padding:0 10px;border:1px solid rgba(255,255,255,.1);border-radius:11px;background:#0f1116;color:#b9bfca;font-size:11px}.ct-wayzi-footer__policy a.is-strong{border-color:rgba(120,151,255,.38);background:rgba(120,151,255,.1);color:#dce3ff}.ct-wayzi-footer__policy a+a:before{display:none}.ct-wayzi-footer__company{padding-top:26px;border-top:1px solid rgba(255,255,255,.075)}.ct-wayzi-footer__company dl{grid-template-columns:1fr!important;gap:8px!important}.ct-wayzi-footer__company dl>div,.ct-wayzi-footer__company dl>div.is-wide{grid-column:auto!important;grid-template-columns:92px minmax(0,1fr)!important}.ct-wayzi-footer__support{padding-top:26px;border-top:1px solid rgba(255,255,255,.075)}.ct-wayzi-footer__bottom{min-height:66px;align-items:flex-start!important;flex-direction:column!important;justify-content:center!important;gap:5px!important;padding-top:14px!important;padding-bottom:14px!important}}
    `;document.head.append(style);
  };

  const apply=()=>{
    installStyle();
    document.querySelectorAll('.ad-final,.ad-sticky,.sticky-offer,.floating-offer,.bottom-offer,.offer-bar,.fixed-offer,.ct-fixed-offer').forEach(element=>element.remove());
    const pageroCopy=document.querySelector('#ct-pagero-intro .ct-v8-head>strong');if(pageroCopy&&pageroCopy.dataset.ctCopyFinal!=='1'){pageroCopy.innerHTML='페이지로에서 문의를 받으면<br>관리는 <em>콜태그 앱에서!</em>';pageroCopy.dataset.ctCopyFinal='1';}
    const tagTitle=document.querySelector('.ct-journey-clean .ct-horizontal-clean__panel:nth-child(3) .ct-horizontal-clean__copy h2');if(tagTitle&&tagTitle.dataset.ctCopyFinal!=='1'){tagTitle.innerHTML='전화가 끝나면<br>태그만 하세요';tagTitle.dataset.ctCopyFinal='1';}
    document.querySelectorAll('.ct-horizontal-industries-clean .ct-horizontal-clean__copy,.ct-industries-static .ct-horizontal-clean__copy,.ct-industry-label,.ct-industry-auto').forEach(element=>element.remove());
    let footer=document.querySelector('footer.footer,footer');if(!footer){footer=document.createElement('footer');document.body.append(footer);}footer.className='footer ct-wayzi-footer';if(footer.dataset.ctWayziFooter!=='4'){footer.innerHTML=footerMarkup;footer.dataset.ctWayziFooter='4';}footer.style.pointerEvents='auto';footer.style.position='relative';footer.style.zIndex='80';
    footer.querySelectorAll('a').forEach(anchor=>{const label=(anchor.textContent||'').replace(/\s+/g,' ').trim();if(destinations[label]){anchor.href=destinations[label];anchor.target='_self';anchor.dataset.ctFooterDestination=destinations[label];}});
  };

  const forceNavigate=event=>{const anchor=event.target.closest?.('.ct-wayzi-footer a');const destination=anchor?.dataset.ctFooterDestination;if(!destination)return;event.preventDefault();event.stopPropagation();event.stopImmediatePropagation?.();window.location.assign(new URL(destination,window.location.origin).href);};
  document.addEventListener('click',forceNavigate,true);
  let queued=false;const queue=()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;apply();});};
  const boot=()=>{apply();const observer=new MutationObserver(queue);observer.observe(document.documentElement,{childList:true,subtree:true});const timers=[400,1200,3000].map(delay=>setTimeout(apply,delay));const stopTimer=setTimeout(()=>{apply();observer.disconnect();},5000);window.addEventListener('pagehide',()=>{timers.forEach(clearTimeout);clearTimeout(stopTimer);observer.disconnect();document.removeEventListener('click',forceNavigate,true);},{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();