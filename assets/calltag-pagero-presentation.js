(()=>{
  if(document.documentElement.dataset.ctPageroRevealFix)return;
  document.documentElement.dataset.ctPageroRevealFix='1';
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
            <section class="ct-page-hero"><div class="ct-page-copy"><small>전문가 무료 상담</small><h3>고민되는 순간,<br>바로 상담받으세요.</h3><p>복잡한 절차 없이 연락처만 남기면 담당자가 빠르게 안내해 드립니다.</p><span class="ct-page-cta">무료 상담 신청하기</span></div><div class="ct-page-visual"><div class="ct-page-photo"></div><span class="ct-page-badge">✓ 상담 신청 완료</span></div></section>
            <div class="ct-page-features"><div class="ct-page-feature"><i>01</i><b>빠른 상담</b><small>신청 즉시 담당자가 확인합니다.</small></div><div class="ct-page-feature"><i>02</i><b>간편한 신청</b><small>연락처와 문의내용만 입력하세요.</small></div><div class="ct-page-feature"><i>03</i><b>맞춤 안내</b><small>고객 상황에 맞춰 안내합니다.</small></div></div>
            <section class="ct-page-form"><div class="ct-page-form-copy"><b>지금 무료 상담을 신청하세요</b><small>접수된 문의는 콜태그에 자동 등록됩니다.</small></div><div class="ct-page-fields"><div class="ct-page-field">이름을 입력해 주세요</div><div class="ct-page-field">연락처를 입력해 주세요</div><div class="ct-page-submit">상담 신청 완료</div></div></section>
          </div>
        </div>
        <aside class="ct-pagero-mobile-preview"><header><span>모바일 미리보기</span><small>390 × 844</small></header><div class="ct-pagero-phone"><div class="ct-pagero-phone-screen"><div class="ct-phone-nav">BRAND<span>상담 신청</span></div><section class="ct-phone-hero"><small>전문가 무료 상담</small><h4>고민되는 순간,<br>바로 상담받으세요.</h4><p>연락처만 남기면 담당자가 빠르게 안내해 드립니다.</p><div class="ct-phone-image"></div></section><div class="ct-phone-features"><i></i><i></i><i></i></div><section class="ct-phone-form"><b>무료 상담 신청</b><div class="ct-phone-field"></div><div class="ct-phone-field"></div><div class="ct-phone-submit">상담 신청 완료</div></section></div></div></aside>`;
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
      const section=document.createElement('section');section.className='ct-pagero-connect';section.innerHTML='<div class="wrap"><p>PAGERO × CALLTAG</p><h2>페이지로에서 문의를 받고,<br><span>콜태그가 바로 알림·등록·후속관리합니다.</span></h2></div>';nocode.before(section);
    }
    return true;
  };
  const updateInquiry=()=>{
    const inquiry=document.querySelector('#ct-pagero-intro .ct-v8-inquiry');if(!inquiry)return false;
    if(inquiry.querySelector('.ct-v8-reveal-group'))return true;
    const details=inquiry.querySelector(':scope > dl');const complete=inquiry.querySelector(':scope > .ct-v8-complete');if(!details||!complete)return false;
    const group=document.createElement('div');group.className='ct-v8-reveal-group';details.before(group);group.append(details,complete);
    const stage=inquiry.closest('.ct-v8-stage');if(stage){stage.classList.remove('is-running');requestAnimationFrame(()=>{void stage.offsetWidth;stage.classList.add('is-running');});}
    return true;
  };
  const apply=()=>updateInquiry()&&updateIntegration()&&updateNoCode();
  if(apply())return;
  const timers=[80,220,500,1000,1800].map(delay=>setTimeout(()=>{if(apply())timers.forEach(clearTimeout);},delay));
  window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});
})();

(()=>{
  if(document.documentElement.dataset.ctConnectVisual)return;
  document.documentElement.dataset.ctConnectVisual='1';
  const html=`<div class="ct-connect-demo"><div><div class="ct-connect-cap"><b>1</b><span>모바일 랜딩페이지</span></div><article class="ct-connect-device"><div class="ct-connect-mobile"><div class="ct-connect-nav"><strong>PAGERO<span>.</span></strong><small>무료 상담</small></div><section class="ct-connect-hero"><small>보험료 무료 비교</small><h3>내 보험료,<br>얼마나 줄일 수 있을까요?</h3><p>연락처와 문의내용만 남기면 담당자가 빠르게 안내해 드립니다.</p><div class="ct-connect-image"></div></section><div class="ct-connect-benefits"><span><i>01</i>간편 신청</span><span><i>02</i>빠른 상담</span><span><i>03</i>맞춤 안내</span></div><section class="ct-connect-form"><strong>무료 상담 신청</strong><small>입력한 문의는 콜태그로 바로 전달됩니다.</small><div class="ct-connect-field">김민수</div><div class="ct-connect-field">010-1234-5678</div><div class="ct-connect-field">보험 상담을 받고 싶어요</div><div class="ct-connect-submit">문의 접수 완료</div></section><div class="ct-connect-sticky">무료 상담 신청하기</div></div></article></div><div class="ct-connect-arrow" aria-hidden="true"><i></i><b>→</b><small>문의 즉시 전달</small></div><div><div class="ct-connect-cap"><b>2</b><span>콜태그 자동등록</span></div><article class="ct-connect-device"><div class="ct-connect-app"><div class="ct-connect-apphead"><strong>CALLTAG</strong><span>◇</span></div><div class="ct-connect-title"><small>오늘 할 일</small><b>신규 문의 1</b></div><article class="ct-connect-card"><div class="ct-connect-person"><i>김</i><span><b>김민수 고객</b><small>010-1234-5678 · 페이지로</small></span><em>신규 문의</em></div><div class="ct-connect-msg"><small>문의내용</small><strong>보험 상담을 받고 싶어요</strong></div><div class="ct-connect-actions"><span>전화</span><span>문자</span><span>태그</span></div><div class="ct-connect-done"><i>✓</i><span><b>고객 자동등록 완료</b><small>바로 후속관리할 수 있습니다.</small></span></div></article></div><div class="ct-connect-push"><i>●</i><span><b>페이지로 신규 문의</b><small>김민수 고객 · 010-1234-5678</small></span><em>지금</em></div></article></div></div>`;
  const mount=()=>{const section=document.querySelector('#ct-pagero-intro .ct-pagero-connect');if(!section)return false;const wrap=section.querySelector('.wrap');if(wrap&&!wrap.querySelector('.ct-connect-demo'))wrap.insertAdjacentHTML('beforeend',html);return !!wrap;};
  if(mount())return;
  const timers=[80,220,500,1000,1800].map(delay=>setTimeout(()=>{if(mount())timers.forEach(clearTimeout);},delay));
  window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});
})();