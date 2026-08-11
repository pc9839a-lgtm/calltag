(()=>{
  if(document.documentElement.dataset.ctPageroSignalV9)return;
  document.documentElement.dataset.ctPageroSignalV9='1';

  const removeLegacy=()=>{
    document.querySelectorAll('.ct-legacy-pagero,.ct-pagero-bridge,.journey-bridge,.sticky-offer,.floating-offer,.bottom-offer,.offer-bar,.fixed-offer,.ct-fixed-offer').forEach(el=>el.remove());
    const phrases=['고객을 받는 페이지로','놓치지 않는 콜태그','2026년 가입가 평생 유지','7일 무료체험'];
    [...document.querySelectorAll('section,aside,div')].forEach(el=>{
      if(el.id==='ct-pagero-intro'||el.closest('#ct-pagero-intro'))return;
      const text=(el.textContent||'').replace(/\s+/g,' ').trim();
      if(text&&text.length<280&&phrases.some(p=>text.includes(p))){
        const pos=getComputedStyle(el).position;
        if(pos==='fixed'||phrases.slice(0,2).some(p=>text.includes(p)))el.remove();
      }
    });
  };

  const mount=()=>{
    removeLegacy();
    document.querySelector('#ct-pagero-intro')?.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return false;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-v8-hero">
        <div class="wrap ct-v8-wrap">
          <header class="ct-v8-head">
            <p>PAGERO × CALLTAG</p>
            <h1>문의가 들어오는 순간,<br><span>절대 놓치지 않습니다.</span></h1>
            <strong>페이지로에서 문의를 받고, 콜태그가 바로 <em>알림·등록·후속관리</em>합니다.</strong>
          </header>
          <div class="ct-v8-stage">
            <div class="ct-v8-step ct-v8-step-left"><b>1</b><span>페이지로 문의 접수</span></div>
            <div class="ct-v8-step ct-v8-step-right"><b>2</b><span>콜태그 즉시 알림</span></div>
            <article class="ct-v8-inquiry">
              <header><strong>PAGERO</strong><small>새 문의</small></header>
              <div class="ct-v8-customer"><small>무료 상담 신청</small><h2>김민수 고객</h2></div>
              <dl><div><dt>연락처</dt><dd>010-1234-5678</dd></div><div><dt>문의내용</dt><dd>보험 상담 요청드립니다</dd></div></dl>
              <div class="ct-v8-complete"><i>✓</i><span>문의접수완료</span></div>
            </article>
            <div class="ct-v8-transfer" aria-hidden="true"><span class="ct-v8-line"></span><i class="ct-v8-dot"></i><b>→</b><small>즉시 전달</small></div>
            <div class="ct-v8-phone">
              <div class="ct-v8-notch"></div>
              <div class="ct-v8-screen">
                <header><strong>CALLTAG</strong><span>♢<i>1</i></span></header>
                <div class="ct-v8-title"><small>오늘 할 일</small><b>신규 문의 1</b></div>
                <article class="ct-v8-appcard">
                  <div class="ct-v8-person"><i>김</i><span><b>김민수 고객</b><small>010-1234-5678 · 페이지로</small></span><em>신규 문의</em></div>
                  <div class="ct-v8-msg"><small>문의내용</small><strong>보험 상담 요청드립니다</strong></div>
                  <div class="ct-v8-actions"><button type="button">전화</button><button type="button">문자</button><button type="button">태그</button></div>
                  <div class="ct-v8-registered"><i>✓</i><span><b>고객 자동등록 완료</b><small>바로 후속관리할 수 있습니다.</small></span></div>
                </article>
              </div>
              <div class="ct-v8-push"><i>●</i><span><b>새 문의 접수</b><small>김민수 고객 · 010-1234-5678</small></span><em>지금</em></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ct-v8-nocode">
        <div class="wrap ct-v8-nocode-grid">
          <div class="ct-v8-nocode-copy"><p>페이지로</p><h2>누구나 만들고,<br><span>문의까지 받습니다.</span></h2><strong>코드를 몰라도 문구와 이미지만 바꾸면 랜딩페이지가 열리고, 접수된 문의는 콜태그에서 바로 확인됩니다.</strong></div>
          <div class="ct-v8-flow">
            <article class="on"><b>01</b><span><strong>내용 입력</strong><small>문구와 이미지 수정</small></span></article><i>→</i>
            <article><b>02</b><span><strong>문의 폼 추가</strong><small>연락처와 문의내용 수집</small></span></article><i>→</i>
            <article><b>03</b><span><strong>바로 공개</strong><small>광고와 SNS에 사용</small></span></article><i>→</i>
            <article><b>04</b><span><strong>콜태그 확인</strong><small>알림·등록·후속관리</small></span></article>
          </div>
        </div>
      </section>`;
    calltagHero.parentNode.insertBefore(intro,calltagHero);

    const stage=intro.querySelector('.ct-v8-stage');
    const run=()=>{stage.classList.remove('is-running');void stage.offsetWidth;stage.classList.add('is-running');};
    run();
    const animationTimer=setInterval(run,3900);
    const flow=[...intro.querySelectorAll('.ct-v8-flow article')];
    let idx=0;
    const flowTimer=setInterval(()=>{flow.forEach((el,i)=>el.classList.toggle('on',i===idx));idx=(idx+1)%flow.length;},1700);
    const cleanupTimers=[250,900,1800].map(delay=>setTimeout(removeLegacy,delay));
    window.addEventListener('pagehide',()=>{clearInterval(animationTimer);clearInterval(flowTimer);cleanupTimers.forEach(clearTimeout);},{once:true});
    return true;
  };

  const boot=()=>{if(mount())return;const timers=[80,220,500,1000].map(delay=>setTimeout(()=>{if(mount())timers.forEach(clearTimeout);},delay));window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();