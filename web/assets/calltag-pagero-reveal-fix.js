(()=>{
  if(document.documentElement.dataset.ctPageroRevealFix)return;
  document.documentElement.dataset.ctPageroRevealFix='1';

  const style=document.createElement('style');
  style.dataset.ctPageroRevealFix='1';
  style.textContent=`
    .ct-v8-reveal-group{opacity:0;visibility:hidden;transform:translateY(12px) scale(.985);filter:blur(3px);will-change:opacity,transform,filter}
    .ct-v8-stage.is-running .ct-v8-reveal-group{animation:ctInquiryReveal 3.55s cubic-bezier(.22,1,.36,1) .18s both}
    .ct-v8-stage.is-running .ct-v8-complete{animation:ctComplete .44s ease .72s 2 alternate}
    .ct-v8-stage.is-running .ct-v8-dot{animation-delay:1.02s}
    .ct-v8-stage.is-running .ct-v8-push{animation-delay:1.58s}
    .ct-v8-stage.is-running .ct-v8-phone{animation-delay:1.62s}
    .ct-v8-stage.is-running .ct-v8-registered{animation-delay:2.08s}

    .ct-pagero-connect{padding:132px 0;border-bottom:1px solid var(--line);background:radial-gradient(circle at 50% 0,rgba(59,111,255,.13),transparent 48%),#0b0e14;text-align:center}
    .ct-pagero-connect p{margin:0 0 20px;color:var(--blue-2);font-size:13px;font-weight:900;letter-spacing:.09em}
    .ct-pagero-connect h2{max-width:1080px;margin:0 auto;font-size:clamp(45px,5.6vw,78px);line-height:1.02;letter-spacing:-.075em}
    .ct-pagero-connect h2 span{color:var(--blue-2)}

    .ct-v8-nocode-grid{grid-template-columns:1fr!important;gap:60px!important;text-align:center}
    .ct-v8-nocode-copy{text-align:center}
    .ct-v8-nocode-copy h2{max-width:1080px;margin:0 auto!important}
    .ct-v8-nocode-copy>p{margin-bottom:20px!important}

    .ct-pagero-builder{display:grid;grid-template-columns:178px minmax(0,1fr) 254px;width:100%;max-width:1180px;min-height:610px;margin:0 auto;overflow:hidden;border:1px solid rgba(124,153,255,.26);border-radius:28px;background:#11151d;box-shadow:0 36px 100px rgba(0,0,0,.42);text-align:left}
    .ct-pagero-tools{padding:20px 14px;border-right:1px solid rgba(255,255,255,.07);background:#0d1016}
    .ct-pagero-tools>strong{display:flex;align-items:center;gap:9px;padding:7px 8px 18px;color:#eef2ff;font-size:14px}
    .ct-pagero-tools>strong i{width:24px;height:24px;display:grid;place-items:center;border-radius:7px;background:#416dff;color:#fff;font-size:10px;font-style:normal}
    .ct-pagero-tool-label{display:block;margin:14px 8px 8px;color:#657087;font-size:9px;font-weight:850;letter-spacing:.08em}
    .ct-pagero-tool{display:flex;align-items:center;gap:9px;width:100%;margin:5px 0;padding:11px 10px;border:1px solid transparent;border-radius:10px;background:transparent;color:#8d96a9;font-size:11px;font-weight:750}
    .ct-pagero-tool i{width:23px;height:23px;display:grid;place-items:center;border-radius:7px;background:#171c26;color:#7f9aff;font-size:10px;font-style:normal}
    .ct-pagero-tool.on{border-color:rgba(84,122,255,.32);background:rgba(59,111,255,.12);color:#eef2ff}
    .ct-pagero-tool.on i{background:#315be0;color:#fff}
    .ct-pagero-save{display:flex;align-items:center;justify-content:center;margin:25px 7px 0;padding:11px;border-radius:10px;background:#3b6fff;color:#fff;font-size:10px;font-weight:850}

    .ct-pagero-canvas{min-width:0;background:#171b24}
    .ct-pagero-browser{height:44px;display:flex;align-items:center;gap:12px;padding:0 15px;border-bottom:1px solid rgba(255,255,255,.08);background:#11151c}
    .ct-pagero-dots{display:flex;gap:5px}.ct-pagero-dots i{width:7px;height:7px;border-radius:50%;background:#394151}.ct-pagero-url{flex:1;max-width:360px;padding:7px 12px;border-radius:7px;background:#1a202b;color:#7d879b;font-size:9px}.ct-pagero-live{margin-left:auto;color:#67d99c;font-size:9px;font-weight:850}
    .ct-pagero-page{margin:18px;overflow:hidden;border-radius:15px;background:#f6f8fc;box-shadow:0 18px 48px rgba(0,0,0,.24)}
    .ct-page-nav{height:42px;display:flex;align-items:center;justify-content:space-between;padding:0 24px;background:#fff;color:#172033}
    .ct-page-logo{font-size:13px;font-weight:950;letter-spacing:-.03em}.ct-page-logo span{color:#3b6fff}.ct-page-nav small{padding:7px 11px;border-radius:999px;background:#edf2ff;color:#3b63d9;font-size:8px;font-weight:850}
    .ct-page-hero{display:grid;grid-template-columns:1.08fr .92fr;min-height:230px;padding:34px;background:linear-gradient(135deg,#edf3ff,#f8faff 48%,#e8eeff);color:#172033}
    .ct-page-copy{align-self:center}.ct-page-copy>small{display:inline-flex;padding:6px 9px;border-radius:999px;background:#dfe9ff;color:#315ddd;font-size:8px;font-weight:900}.ct-page-copy h3{margin:14px 0 9px;font-size:29px;line-height:1.02;letter-spacing:-.065em}.ct-page-copy p{max-width:330px;margin:0;color:#687287;font-size:9px;line-height:1.65}.ct-page-cta{display:inline-flex;margin-top:17px;padding:10px 15px;border-radius:9px;background:#3265ee;color:#fff;font-size:9px;font-weight:900;box-shadow:0 9px 23px rgba(50,101,238,.25)}
    .ct-page-visual{position:relative;display:grid;place-items:center}.ct-page-photo{position:relative;width:86%;height:172px;overflow:hidden;border-radius:18px;background:linear-gradient(145deg,#416dff,#7b91ff 55%,#becaff);box-shadow:0 20px 45px rgba(51,91,205,.28)}.ct-page-photo:before{content:'';position:absolute;width:115px;height:115px;right:-16px;top:-15px;border-radius:50%;background:rgba(255,255,255,.22)}.ct-page-photo:after{content:'';position:absolute;width:120px;height:78px;left:22px;bottom:19px;border-radius:42px 42px 12px 12px;background:rgba(255,255,255,.9);box-shadow:0 0 0 13px rgba(255,255,255,.16)}.ct-page-badge{position:absolute;right:3px;bottom:11px;padding:9px 11px;border-radius:10px;background:#fff;color:#2c3d67;font-size:8px;font-weight:900;box-shadow:0 12px 28px rgba(39,55,99,.18)}
    .ct-page-features{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:19px 24px;background:#fff}.ct-page-feature{padding:13px;border:1px solid #e6eaf2;border-radius:11px}.ct-page-feature i{width:24px;height:24px;display:grid;place-items:center;border-radius:8px;background:#eef3ff;color:#416dff;font-size:9px;font-style:normal}.ct-page-feature b{display:block;margin-top:9px;color:#253047;font-size:9px}.ct-page-feature small{display:block;margin-top:5px;color:#8992a3;font-size:7px;line-height:1.45}
    .ct-page-form{display:grid;grid-template-columns:1fr 1.05fr;gap:18px;padding:21px 24px 25px;background:#f6f8fc;color:#172033}.ct-page-form-copy{align-self:center}.ct-page-form-copy b{display:block;font-size:16px;letter-spacing:-.04em}.ct-page-form-copy small{display:block;margin-top:7px;color:#7d8798;font-size:8px;line-height:1.5}.ct-page-fields{display:grid;gap:7px}.ct-page-field{height:28px;display:flex;align-items:center;padding:0 10px;border:1px solid #dfe4ee;border-radius:7px;background:#fff;color:#a0a7b5;font-size:7px}.ct-page-submit{height:30px;display:grid;place-items:center;border-radius:7px;background:#3265ee;color:#fff;font-size:8px;font-weight:900}

    .ct-pagero-mobile-preview{padding:18px;border-left:1px solid rgba(255,255,255,.07);background:#0d1016}
    .ct-pagero-mobile-preview>header{display:flex;align-items:center;justify-content:space-between;margin-bottom:17px;color:#eef2ff;font-size:11px;font-weight:850}.ct-pagero-mobile-preview>header small{color:#687287;font-size:8px}
    .ct-pagero-phone{width:188px;margin:0 auto;padding:7px;border:1px solid #31394a;border-radius:27px;background:#020306;box-shadow:0 22px 55px rgba(0,0,0,.52)}
    .ct-pagero-phone-screen{min-height:455px;overflow:hidden;border-radius:21px;background:#f7f9fd}.ct-phone-nav{height:31px;display:flex;align-items:center;justify-content:space-between;padding:0 11px;background:#fff;color:#172033;font-size:7px;font-weight:900}.ct-phone-nav span{color:#3265ee}.ct-phone-hero{padding:18px 14px 14px;background:linear-gradient(145deg,#edf3ff,#e8eeff);color:#172033}.ct-phone-hero small{color:#3265ee;font-size:6px;font-weight:900}.ct-phone-hero h4{margin:8px 0 6px;font-size:17px;line-height:1.04;letter-spacing:-.06em}.ct-phone-hero p{margin:0;color:#788296;font-size:6px;line-height:1.45}.ct-phone-image{height:92px;margin-top:12px;border-radius:11px;background:linear-gradient(145deg,#416dff,#92a7ff);position:relative;overflow:hidden}.ct-phone-image:after{content:'';position:absolute;width:70px;height:46px;left:50%;bottom:10px;transform:translateX(-50%);border-radius:30px 30px 8px 8px;background:rgba(255,255,255,.9)}.ct-phone-features{display:grid;grid-template-columns:repeat(3,1fr);gap:5px;padding:11px;background:#fff}.ct-phone-features i{height:44px;border:1px solid #e6eaf2;border-radius:7px;background:#fbfcff}.ct-phone-form{padding:12px;background:#f4f6fa}.ct-phone-form b{display:block;color:#263148;font-size:9px}.ct-phone-field{height:24px;margin-top:6px;border:1px solid #e0e5ee;border-radius:6px;background:#fff}.ct-phone-submit{height:26px;display:grid;place-items:center;margin-top:7px;border-radius:6px;background:#3265ee;color:#fff;font-size:7px;font-weight:900}

    @keyframes ctInquiryReveal{
      0%,10%{opacity:0;visibility:hidden;transform:translateY(12px) scale(.985);filter:blur(3px)}
      17%,89%{opacity:1;visibility:visible;transform:translateY(0) scale(1);filter:blur(0)}
      100%{opacity:0;visibility:hidden;transform:translateY(-5px) scale(.99);filter:blur(1px)}
    }
    @media(max-width:980px){.ct-pagero-builder{grid-template-columns:150px minmax(0,1fr)}.ct-pagero-mobile-preview{display:none}.ct-page-hero{grid-template-columns:1fr .8fr}.ct-pagero-connect{padding:110px 0}}
    @media(max-width:700px){.ct-pagero-builder{display:block;min-height:0}.ct-pagero-tools{display:none}.ct-pagero-page{margin:10px}.ct-page-hero{grid-template-columns:1fr;padding:24px}.ct-page-visual{margin-top:18px}.ct-page-photo{width:100%;height:150px}.ct-page-features{padding:14px;gap:7px}.ct-page-form{grid-template-columns:1fr;padding:17px}.ct-pagero-connect h2{font-size:40px}}
    @media(max-width:640px){.ct-v8-nocode-grid{gap:42px!important}.ct-v8-nocode-copy h2{font-size:39px!important;line-height:1.06!important}.ct-pagero-connect{padding:92px 0}.ct-pagero-connect h2{font-size:36px;line-height:1.07}.ct-page-copy h3{font-size:24px}.ct-page-features{grid-template-columns:1fr}.ct-page-feature{display:none}.ct-page-feature:first-child{display:block}.ct-pagero-browser{padding:0 10px}.ct-pagero-url{font-size:7px}}
    @media(prefers-reduced-motion:reduce){.ct-v8-reveal-group{opacity:1!important;visibility:visible!important;transform:none!important;filter:none!important;animation:none!important}}
  `;
  document.head.append(style);

  const updateNoCode=()=>{
    const copy=document.querySelector('#ct-pagero-intro .ct-v8-nocode-copy');
    const oldFlow=document.querySelector('#ct-pagero-intro .ct-v8-flow');
    if(!copy||!oldFlow)return false;

    const title=copy.querySelector('h2');
    const description=copy.querySelector(':scope > strong');
    if(title)title.innerHTML='코드를 몰라도 누구나<br><span>랜딩페이지 제작이 가능한 페이지로</span>';
    if(description)description.remove();

    if(!oldFlow.classList.contains('ct-pagero-builder')){
      oldFlow.className='ct-pagero-builder';
      oldFlow.innerHTML=`
        <aside class="ct-pagero-tools">
          <strong><i>P</i>페이지로</strong>
          <span class="ct-pagero-tool-label">페이지 구성</span>
          <div class="ct-pagero-tool on"><i>H</i>히어로 영역</div>
          <div class="ct-pagero-tool"><i>T</i>텍스트</div>
          <div class="ct-pagero-tool"><i>I</i>이미지</div>
          <div class="ct-pagero-tool"><i>F</i>문의 폼</div>
          <span class="ct-pagero-tool-label">설정</span>
          <div class="ct-pagero-tool"><i>C</i>색상·스타일</div>
          <div class="ct-pagero-tool"><i>M</i>모바일 화면</div>
          <div class="ct-pagero-save">저장하고 공개하기</div>
        </aside>
        <div class="ct-pagero-canvas">
          <div class="ct-pagero-browser"><span class="ct-pagero-dots"><i></i><i></i><i></i></span><span class="ct-pagero-url">pagero.kr/p/무료상담</span><b class="ct-pagero-live">● 실시간 미리보기</b></div>
          <div class="ct-pagero-page">
            <div class="ct-page-nav"><b class="ct-page-logo">BRAND<span>+</span></b><small>무료 상담 신청</small></div>
            <section class="ct-page-hero">
              <div class="ct-page-copy"><small>전문가 무료 상담</small><h3>고민되는 순간,<br>바로 상담받으세요.</h3><p>복잡한 절차 없이 연락처만 남기면 담당자가 빠르게 안내해 드립니다.</p><span class="ct-page-cta">무료 상담 신청하기</span></div>
              <div class="ct-page-visual"><div class="ct-page-photo"></div><span class="ct-page-badge">✓ 상담 신청 완료</span></div>
            </section>
            <div class="ct-page-features"><div class="ct-page-feature"><i>01</i><b>빠른 상담</b><small>신청 즉시 담당자가 확인합니다.</small></div><div class="ct-page-feature"><i>02</i><b>간편한 신청</b><small>연락처와 문의내용만 입력하세요.</small></div><div class="ct-page-feature"><i>03</i><b>맞춤 안내</b><small>고객 상황에 맞춰 안내합니다.</small></div></div>
            <section class="ct-page-form"><div class="ct-page-form-copy"><b>지금 무료 상담을 신청하세요</b><small>접수된 문의는 콜태그에 자동 등록됩니다.</small></div><div class="ct-page-fields"><div class="ct-page-field">이름을 입력해 주세요</div><div class="ct-page-field">연락처를 입력해 주세요</div><div class="ct-page-submit">상담 신청 완료</div></div></section>
          </div>
        </div>
        <aside class="ct-pagero-mobile-preview">
          <header><span>모바일 미리보기</span><small>390 × 844</small></header>
          <div class="ct-pagero-phone"><div class="ct-pagero-phone-screen"><div class="ct-phone-nav">BRAND<span>상담 신청</span></div><section class="ct-phone-hero"><small>전문가 무료 상담</small><h4>고민되는 순간,<br>바로 상담받으세요.</h4><p>연락처만 남기면 담당자가 빠르게 안내해 드립니다.</p><div class="ct-phone-image"></div></section><div class="ct-phone-features"><i></i><i></i><i></i></div><section class="ct-phone-form"><b>무료 상담 신청</b><div class="ct-phone-field"></div><div class="ct-phone-field"></div><div class="ct-phone-submit">상담 신청 완료</div></section></div></div>
        </aside>`;
    }
    return true;
  };

  const updateIntegration=()=>{
    const intro=document.querySelector('#ct-pagero-intro');
    const hero=intro?.querySelector('.ct-v8-hero');
    const nocode=intro?.querySelector('.ct-v8-nocode');
    if(!intro||!hero||!nocode)return false;

    hero.querySelector('.ct-v8-head>strong')?.remove();
    if(!intro.querySelector('.ct-pagero-connect')){
      const section=document.createElement('section');
      section.className='ct-pagero-connect';
      section.innerHTML='<div class="wrap"><p>PAGERO × CALLTAG</p><h2>페이지로에서 문의를 받고,<br><span>콜태그가 바로 알림·등록·후속관리합니다.</span></h2></div>';
      nocode.before(section);
    }
    return true;
  };

  const updateInquiry=()=>{
    const inquiry=document.querySelector('#ct-pagero-intro .ct-v8-inquiry');
    if(!inquiry)return false;
    if(inquiry.querySelector('.ct-v8-reveal-group'))return true;

    const details=inquiry.querySelector(':scope > dl');
    const complete=inquiry.querySelector(':scope > .ct-v8-complete');
    if(!details||!complete)return false;

    const group=document.createElement('div');
    group.className='ct-v8-reveal-group';
    details.before(group);
    group.append(details,complete);

    const stage=inquiry.closest('.ct-v8-stage');
    if(stage){
      stage.classList.remove('is-running');
      requestAnimationFrame(()=>{
        void stage.offsetWidth;
        stage.classList.add('is-running');
      });
    }
    return true;
  };

  const apply=()=>updateInquiry()&&updateIntegration()&&updateNoCode();

  if(!apply()){
    const observer=new MutationObserver(()=>{
      if(apply())observer.disconnect();
    });
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),10000);
  }
})();