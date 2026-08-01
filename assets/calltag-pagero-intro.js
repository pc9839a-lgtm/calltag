(()=>{
  if(document.documentElement.dataset.ctPageroImpactV5)return;
  document.documentElement.dataset.ctPageroImpactV5='1';

  const removeLegacy=()=>{
    document.querySelectorAll('section,article,div').forEach(el=>{
      if(el.id==='ct-pagero-intro'||el.closest('#ct-pagero-intro'))return;
      const text=(el.textContent||'').replace(/\s+/g,' ').trim();
      if(text.includes('고객을 받는 페이지로')&&text.includes('놓치지 않는 콜태그')){
        const target=el.closest('section')||el;
        if(target&&target!==document.body)target.remove();
      }
    });

    document.querySelectorAll('body *').forEach(el=>{
      if(el.closest('#ct-pagero-intro'))return;
      const text=(el.textContent||'').replace(/\s+/g,' ').trim();
      if(!text.includes('2026년 가입가 평생 유지')&&!text.includes('7일 무료체험'))return;
      let target=el;
      for(let i=0;i<7&&target&&target!==document.body;i++,target=target.parentElement){
        const style=getComputedStyle(target);
        const cls=String(target.className||'');
        if(style.position==='fixed'||style.position==='sticky'||/(sticky|floating|fixed|bottom-?cta|offer-?bar)/i.test(cls)){
          target.remove();
          break;
        }
      }
    });
  };

  const mount=()=>{
    removeLegacy();
    document.querySelector('#ct-pagero-intro')?.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-impact-hero">
        <div class="wrap">
          <header class="ct-impact-head">
            <p>PAGERO × CALLTAG</p>
            <h1>문의가 들어오는 순간,<br><span>절대 놓치지 않습니다.</span></h1>
            <strong>페이지로에서 문의를 받고, 콜태그가 자동으로 관리합니다.</strong>
          </header>

          <div class="ct-impact-live" data-step="1" aria-label="문의가 접수되고 자동으로 관리되는 과정">
            <article class="ct-impact-inquiry">
              <div class="ct-impact-top"><span>PAGERO</span><em>새 문의</em></div>
              <small>상담 신청이 들어왔습니다.</small>
              <h2>김민수 고객</h2>
              <div><span>연락처</span><b>010-1234-5678</b></div>
              <div><span>유입 페이지</span><b>보험 상담 랜딩</b></div>
              <strong class="ct-impact-done"><i>✓</i> 문의 접수 완료</strong>
            </article>

            <div class="ct-impact-transfer"><span></span><b>→</b><small>자동 등록</small></div>

            <article class="ct-impact-manage">
              <div class="ct-impact-top"><span>CALLTAG</span><em>신규 고객</em></div>
              <div class="ct-impact-person"><i>김</i><span><b>김민수 고객</b><small>유입 · 페이지로</small></span></div>
              <div class="ct-impact-result result-1"><span>고객 등록</span><b>완료</b></div>
              <div class="ct-impact-result result-2"><span>자동문자</span><b>발송 완료</b></div>
              <div class="ct-impact-result result-3"><span>다음 연락</span><b>오늘 오후 3:00</b></div>
            </article>
          </div>
        </div>
      </section>

      <section class="ct-impact-system">
        <div class="wrap">
          <header class="ct-impact-head compact">
            <p>NO-CODE LANDING × AUTO CRM</p>
            <h2>누구나 만들고,<br><span>문의는 자동으로 관리됩니다.</span></h2>
            <strong>페이지로는 코드를 몰라도 누구나 쉽게 만드는 노코드 랜딩페이지입니다.</strong>
          </header>

          <div class="ct-impact-pair" data-step="1">
            <article class="ct-impact-product pagero">
              <header><span>PAGERO</span><b>랜딩페이지 제작</b></header>
              <div class="ct-impact-action" data-action="1"><i>01</i><span><strong>내용 입력</strong><small>문구와 이미지만 바꾸기</small></span></div>
              <div class="ct-impact-action" data-action="2"><i>02</i><span><strong>문의 폼 추가</strong><small>이름과 연락처 받기</small></span></div>
              <div class="ct-impact-action" data-action="3"><i>03</i><span><strong>바로 공개</strong><small>광고와 SNS에 사용</small></span></div>
            </article>

            <div class="ct-impact-bridge"><small>문의 접수</small><span></span><b>→</b></div>

            <article class="ct-impact-product calltag">
              <header><span>CALLTAG</span><b>자동 고객관리</b></header>
              <div class="ct-impact-action" data-action="1"><i>01</i><span><strong>고객 자동등록</strong><small>이름·연락처·유입경로 저장</small></span></div>
              <div class="ct-impact-action" data-action="2"><i>02</i><span><strong>안내문자 발송</strong><small>접수 즉시 자동 안내</small></span></div>
              <div class="ct-impact-action" data-action="3"><i>03</i><span><strong>재연락 일정</strong><small>오늘 할 일로 자동 연결</small></span></div>
            </article>
          </div>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);
    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());

    const style=document.createElement('style');
    style.dataset.ctPageroIntro='5';
    style.textContent=`
      #ct-pagero-intro{background:var(--bg);color:var(--text);overflow:hidden}
      .ct-impact-hero,.ct-impact-system{position:relative;min-height:100vh;display:flex;align-items:center;padding:132px 0 110px;border-bottom:1px solid var(--line);overflow:hidden}
      .ct-impact-hero:before,.ct-impact-system:before{content:'';position:absolute;width:980px;height:980px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.22),transparent 68%);pointer-events:none}
      .ct-impact-hero:before{top:-620px;left:50%;transform:translateX(-50%)}.ct-impact-system:before{right:-480px;bottom:-650px}
      .ct-impact-hero>.wrap,.ct-impact-system>.wrap{position:relative;z-index:1}
      .ct-impact-head{text-align:center}.ct-impact-head>p{margin:0 0 18px;color:var(--blue-2);font-size:13px;font-weight:900;letter-spacing:.08em}
      .ct-impact-head h1,.ct-impact-head h2{margin:0;line-height:.94;letter-spacing:-.085em}.ct-impact-head h1{font-size:clamp(62px,8.1vw,116px)}.ct-impact-head h2{font-size:clamp(54px,7vw,96px)}
      .ct-impact-head h1 span,.ct-impact-head h2 span{color:var(--blue-2)}.ct-impact-head>strong{display:block;margin:25px auto 0;color:#d5d8df;font-size:clamp(17px,1.7vw,22px);font-weight:800;letter-spacing:-.035em}.ct-impact-head.compact{margin-bottom:58px}
      .ct-impact-live{display:grid;grid-template-columns:1fr 110px 1fr;align-items:center;margin-top:64px;padding:30px;border:1px solid var(--line-strong);border-radius:32px;background:linear-gradient(145deg,rgba(24,27,34,.97),rgba(13,15,19,.99));box-shadow:0 42px 120px rgba(0,0,0,.46)}
      .ct-impact-inquiry,.ct-impact-manage{min-height:350px;padding:27px;border:1px solid var(--line);border-radius:22px;background:#101218;transition:.32s cubic-bezier(.2,.8,.2,1)}
      .ct-impact-top{display:flex;align-items:center;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid var(--line)}.ct-impact-top span{color:var(--blue-2);font-size:11px;font-weight:900}.ct-impact-top em{color:var(--muted-2);font-size:10px;font-style:normal;font-weight:800}
      .ct-impact-inquiry>small{display:block;margin-top:29px;color:var(--blue-2);font-size:10px;font-weight:850}.ct-impact-inquiry h2{margin:11px 0 22px;font-size:37px;letter-spacing:-.06em}.ct-impact-inquiry>div:not(.ct-impact-top){height:52px;display:flex;align-items:center;justify-content:space-between;margin-top:9px;padding:0 14px;border:1px solid var(--line);border-radius:11px;background:#151820}.ct-impact-inquiry>div span{color:var(--muted-2);font-size:10px}.ct-impact-inquiry>div b{font-size:12px}
      .ct-impact-done{display:flex;align-items:center;gap:9px;margin-top:13px;padding:14px;border:1px solid rgba(50,200,121,.25);border-radius:12px;background:rgba(50,200,121,.08);color:#80e2ad;font-size:11px;opacity:0;transform:translateY(12px);transition:.3s}.ct-impact-done i{width:25px;height:25px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.15);font-style:normal}
      .ct-impact-transfer{position:relative;display:grid;place-items:center;gap:8px}.ct-impact-transfer>span{position:absolute;width:90px;height:2px;background:rgba(59,111,255,.18);transform:scaleX(0);transition:.32s}.ct-impact-transfer>b{position:relative;z-index:1;width:46px;height:46px;display:grid;place-items:center;border:1px solid rgba(59,111,255,.22);border-radius:50%;background:#111725;color:#66759d;font-size:21px;transform:scale(.72);transition:.32s}.ct-impact-transfer small{color:var(--muted-2);font-size:9px;font-weight:850;opacity:.35;transition:.32s}
      .ct-impact-person{display:flex;align-items:center;gap:14px;margin:27px 0 20px;opacity:0;transform:translateY(12px);transition:.32s}.ct-impact-person>i{width:48px;height:48px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#b4c0ff;font-size:14px;font-style:normal;font-weight:900}.ct-impact-person b,.ct-impact-person small{display:block}.ct-impact-person b{font-size:17px}.ct-impact-person small{margin-top:5px;color:var(--muted-2);font-size:10px}
      .ct-impact-result{height:57px;display:flex;align-items:center;justify-content:space-between;margin-top:9px;padding:0 15px;border:1px solid var(--line);border-radius:12px;background:#151820;opacity:0;transform:translateY(12px) scale(.98);transition:.3s}.ct-impact-result span{color:var(--muted-2);font-size:10px}.ct-impact-result b{font-size:12px;color:#7fe1ad}
      .ct-impact-live[data-step='1'] .ct-impact-inquiry{border-color:rgba(59,111,255,.48);transform:scale(1.018);box-shadow:0 0 0 5px rgba(59,111,255,.06)}.ct-impact-live[data-step='1'] .ct-impact-done,.ct-impact-live[data-step='2'] .ct-impact-done,.ct-impact-live[data-step='3'] .ct-impact-done,.ct-impact-live[data-step='4'] .ct-impact-done{opacity:1;transform:none}
      .ct-impact-live[data-step='2'] .ct-impact-transfer>span,.ct-impact-live[data-step='3'] .ct-impact-transfer>span,.ct-impact-live[data-step='4'] .ct-impact-transfer>span{transform:scaleX(1)}.ct-impact-live[data-step='2'] .ct-impact-transfer>b,.ct-impact-live[data-step='3'] .ct-impact-transfer>b,.ct-impact-live[data-step='4'] .ct-impact-transfer>b{border-color:rgba(59,111,255,.58);color:var(--blue-2);transform:scale(1);box-shadow:0 0 0 8px rgba(59,111,255,.08)}.ct-impact-live[data-step='2'] .ct-impact-transfer small,.ct-impact-live[data-step='3'] .ct-impact-transfer small,.ct-impact-live[data-step='4'] .ct-impact-transfer small{opacity:1}
      .ct-impact-live[data-step='3'] .ct-impact-manage,.ct-impact-live[data-step='4'] .ct-impact-manage{border-color:rgba(59,111,255,.48);transform:scale(1.018);box-shadow:0 0 0 5px rgba(59,111,255,.06)}.ct-impact-live[data-step='3'] .ct-impact-person,.ct-impact-live[data-step='4'] .ct-impact-person{opacity:1;transform:none}.ct-impact-live[data-step='3'] .result-1,.ct-impact-live[data-step='4'] .ct-impact-result{opacity:1;transform:none}.ct-impact-live[data-step='4'] .result-2{transition-delay:.08s}.ct-impact-live[data-step='4'] .result-3{transition-delay:.16s}
      .ct-impact-system{background:linear-gradient(180deg,var(--bg),#0b0d12)}.ct-impact-pair{display:grid;grid-template-columns:1fr 120px 1fr;align-items:center;padding:30px;border:1px solid var(--line-strong);border-radius:32px;background:linear-gradient(145deg,#171a20,#101217);box-shadow:0 38px 100px rgba(0,0,0,.38)}
      .ct-impact-product{min-height:430px;padding:27px;border:1px solid var(--line);border-radius:22px;background:#101218}.ct-impact-product header{display:flex;align-items:center;justify-content:space-between;padding-bottom:20px;border-bottom:1px solid var(--line)}.ct-impact-product header span{color:var(--blue-2);font-size:11px;font-weight:900}.ct-impact-product header b{font-size:14px}
      .ct-impact-action{display:grid;grid-template-columns:49px 1fr;gap:15px;align-items:center;min-height:92px;margin-top:12px;padding:15px;border:1px solid var(--line);border-radius:15px;background:#151820;opacity:.42;transform:scale(.98);transition:.32s cubic-bezier(.2,.8,.2,1)}.ct-impact-action>i{width:49px;height:49px;display:grid;place-items:center;border-radius:14px;background:#1c2028;color:#68717e;font-size:12px;font-style:normal;font-weight:900}.ct-impact-action strong,.ct-impact-action small{display:block}.ct-impact-action strong{font-size:18px}.ct-impact-action small{margin-top:6px;color:var(--muted-2);font-size:10px}
      .ct-impact-pair[data-step='1'] [data-action='1'],.ct-impact-pair[data-step='2'] [data-action='2'],.ct-impact-pair[data-step='3'] [data-action='3']{opacity:1;transform:scale(1.025);border-color:rgba(59,111,255,.52);background:var(--blue-soft);box-shadow:0 0 0 5px rgba(59,111,255,.05)}.ct-impact-pair[data-step='1'] [data-action='1']>i,.ct-impact-pair[data-step='2'] [data-action='2']>i,.ct-impact-pair[data-step='3'] [data-action='3']>i{background:var(--blue);color:#fff}
      .ct-impact-bridge{position:relative;display:grid;place-items:center;gap:12px}.ct-impact-bridge small{color:var(--blue-2);font-size:9px;font-weight:900}.ct-impact-bridge span{width:88px;height:2px;background:linear-gradient(90deg,rgba(59,111,255,.12),var(--blue),rgba(59,111,255,.12));background-size:200% 100%;animation:ctImpactLine 1.2s linear infinite}.ct-impact-bridge b{width:45px;height:45px;display:grid;place-items:center;border:1px solid rgba(59,111,255,.55);border-radius:50%;background:#111725;color:var(--blue-2);font-size:20px}
      @keyframes ctImpactLine{to{background-position:-200% 0}}
      @media(max-width:980px){.ct-impact-hero,.ct-impact-system{min-height:auto;padding:110px 0 90px}.ct-impact-live,.ct-impact-pair{grid-template-columns:1fr;gap:22px}.ct-impact-transfer,.ct-impact-bridge{min-height:72px}.ct-impact-transfer>span,.ct-impact-bridge span{width:2px;height:58px}.ct-impact-transfer>b,.ct-impact-bridge b{transform:rotate(90deg)}.ct-impact-live[data-step='2'] .ct-impact-transfer>b,.ct-impact-live[data-step='3'] .ct-impact-transfer>b,.ct-impact-live[data-step='4'] .ct-impact-transfer>b{transform:rotate(90deg) scale(1)}.ct-impact-head.compact{margin-bottom:42px}}
      @media(max-width:620px){.ct-impact-hero,.ct-impact-system{padding:94px 0 76px}.ct-impact-head>p{font-size:10px}.ct-impact-head h1{font-size:clamp(46px,14vw,67px);line-height:.96}.ct-impact-head h2{font-size:clamp(41px,12vw,58px);line-height:.98}.ct-impact-head>strong{max-width:330px;font-size:15px;line-height:1.55}.ct-impact-live,.ct-impact-pair{margin-top:42px;padding:14px;border-radius:22px}.ct-impact-pair{margin-top:0}.ct-impact-inquiry,.ct-impact-manage,.ct-impact-product{min-height:auto;padding:20px;border-radius:17px}.ct-impact-inquiry h2{font-size:29px}.ct-impact-action{min-height:80px;grid-template-columns:42px 1fr;padding:12px}.ct-impact-action>i{width:42px;height:42px}.ct-impact-action strong{font-size:16px}.ct-impact-product header b{font-size:12px}}
      @media(prefers-reduced-motion:reduce){.ct-impact-live *,.ct-impact-pair *{transition:none!important;animation:none!important}.ct-impact-live .ct-impact-done,.ct-impact-live .ct-impact-person,.ct-impact-live .ct-impact-result{opacity:1!important;transform:none!important}.ct-impact-action{opacity:1!important;transform:none!important}}
    `;
    document.head.append(style);

    const live=intro.querySelector('.ct-impact-live');
    const pair=intro.querySelector('.ct-impact-pair');
    if(!matchMedia('(prefers-reduced-motion: reduce)').matches){
      let heroStep=1;
      setInterval(()=>{heroStep=heroStep>=4?1:heroStep+1;live.dataset.step=String(heroStep)},650);
      let pairStep=1;
      setInterval(()=>{pairStep=pairStep>=3?1:pairStep+1;pair.dataset.step=String(pairStep)},820);
    }else{
      live.dataset.step='4';
      pair.dataset.step='1';
    }
  };

  const run=()=>{mount();removeLegacy()};
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',run,{once:true}):run();
  [400,1000,2200,4200,7000,10000].forEach(ms=>setTimeout(removeLegacy,ms));
  const observer=new MutationObserver(()=>requestAnimationFrame(removeLegacy));
  if(document.body)observer.observe(document.body,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),12000);
})();