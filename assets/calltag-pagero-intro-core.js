(()=>{
  if(document.documentElement.dataset.ctPageroSignalV8)return;
  document.documentElement.dataset.ctPageroSignalV8='1';

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
    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());

    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
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
              <dl>
                <div><dt>연락처</dt><dd>010-1234-5678</dd></div>
                <div><dt>문의내용</dt><dd>보험 상담 요청드립니다</dd></div>
              </dl>
              <div class="ct-v8-complete"><i>✓</i><span>문의접수완료</span></div>
            </article>

            <div class="ct-v8-transfer" aria-hidden="true">
              <span class="ct-v8-line"></span><i class="ct-v8-dot"></i><b>→</b><small>즉시 전달</small>
            </div>

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
          <div class="ct-v8-nocode-copy">
            <p>페이지로</p>
            <h2>누구나 만들고,<br><span>문의까지 받습니다.</span></h2>
            <strong>코드를 몰라도 문구와 이미지만 바꾸면 랜딩페이지가 열리고, 접수된 문의는 콜태그에서 바로 확인됩니다.</strong>
          </div>
          <div class="ct-v8-flow">
            <article class="on"><b>01</b><span><strong>내용 입력</strong><small>문구와 이미지 수정</small></span></article><i>→</i>
            <article><b>02</b><span><strong>문의 폼 추가</strong><small>연락처와 문의내용 수집</small></span></article><i>→</i>
            <article><b>03</b><span><strong>바로 공개</strong><small>광고와 SNS에 사용</small></span></article><i>→</i>
            <article><b>04</b><span><strong>콜태그 확인</strong><small>알림·등록·후속관리</small></span></article>
          </div>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);

    const style=document.createElement('style');
    style.dataset.ctPageroIntro='8';
    style.textContent=`
      #ct-pagero-intro{background:var(--bg);color:var(--text);overflow:hidden}
      .ct-v8-hero{position:relative;padding:150px 0 112px;border-bottom:1px solid var(--line);background:radial-gradient(circle at 50% -18%,rgba(59,111,255,.18),transparent 40%),linear-gradient(180deg,#090b12,var(--bg) 78%)}
      .ct-v8-head{text-align:center}.ct-v8-head>p,.ct-v8-nocode-copy>p{margin:0 0 18px;color:var(--blue-2);font-size:13px;font-weight:900;letter-spacing:.08em}.ct-v8-head h1{max-width:1120px;margin:0 auto;font-size:clamp(58px,6.4vw,92px);line-height:.94;letter-spacing:-.078em}.ct-v8-head h1 span,.ct-v8-nocode-copy h2 span{color:var(--blue-2)}.ct-v8-head>strong{display:block;margin:28px auto 0;color:#cdd2dc;font-size:clamp(16px,1.25vw,19px);font-weight:650;line-height:1.55}.ct-v8-head>strong em{color:#90a8ff;font-style:normal;font-weight:850}

      .ct-v8-stage{display:grid;grid-template-columns:minmax(0,1fr) 116px 340px;grid-template-rows:auto auto;column-gap:28px;row-gap:18px;align-items:start;max-width:1280px;margin:76px auto 0;padding:36px 40px 42px;border:1px solid rgba(124,153,255,.24);border-radius:30px;background:linear-gradient(145deg,#151922,#0e1117);box-shadow:0 42px 104px rgba(0,0,0,.42)}
      .ct-v8-step{display:flex;align-items:center;gap:12px;min-height:34px;color:#edf0f6;font-size:12px;font-weight:850}.ct-v8-step-left{grid-column:1;grid-row:1}.ct-v8-step-right{grid-column:3;grid-row:1;justify-content:center}.ct-v8-step b{width:30px;height:30px;display:grid;place-items:center;flex:0 0 30px;border-radius:50%;background:var(--blue);color:#fff;font-size:11px;box-shadow:0 0 0 7px rgba(59,111,255,.1)}

      .ct-v8-inquiry{grid-column:1;grid-row:2;min-width:0;min-height:414px;padding:27px;border:1px solid var(--line-strong);border-radius:22px;background:#0e1117}.ct-v8-inquiry>header{display:flex;align-items:center;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid var(--line)}.ct-v8-inquiry>header strong{color:var(--blue-2);font-size:11px}.ct-v8-inquiry>header small{color:var(--muted-2);font-size:10px}.ct-v8-customer{padding:26px 0 16px}.ct-v8-customer small{color:#8da6ff;font-size:10px;font-weight:850}.ct-v8-customer h2{margin:10px 0 0;font-size:34px;letter-spacing:-.06em}.ct-v8-inquiry dl{display:grid;gap:10px;margin:0}.ct-v8-inquiry dl div{min-height:58px;display:flex;align-items:center;justify-content:space-between;gap:20px;padding:0 15px;border:1px solid var(--line);border-radius:12px;background:#151922}.ct-v8-inquiry dt{color:var(--muted-2);font-size:10px}.ct-v8-inquiry dd{margin:0;color:#fff;font-size:12px;font-weight:820;text-align:right}.ct-v8-complete{display:flex;align-items:center;gap:11px;margin-top:13px;padding:15px;border:1px solid rgba(50,200,121,.3);border-radius:13px;background:rgba(50,200,121,.1);color:#72e2a8;font-size:12px;font-weight:900}.ct-v8-complete i{width:27px;height:27px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.16);font-style:normal}

      .ct-v8-transfer{grid-column:2;grid-row:2;position:relative;align-self:center;justify-self:stretch;height:150px;display:grid;place-items:center}.ct-v8-line{position:absolute;left:0;right:0;top:50%;height:2px;transform:translateY(-50%);background:linear-gradient(90deg,rgba(59,111,255,.08),rgba(112,145,255,.82),rgba(59,111,255,.08))}.ct-v8-dot{position:absolute;left:4px;top:calc(50% - 4px);width:8px;height:8px;border-radius:50%;background:#8ca5ff;box-shadow:0 0 18px 7px rgba(59,111,255,.34)}.ct-v8-transfer>b{position:relative;z-index:2;width:54px;height:54px;display:grid;place-items:center;border:1px solid rgba(95,132,255,.58);border-radius:50%;background:#11182b;color:#89a3ff;font-size:27px;box-shadow:0 0 0 8px rgba(59,111,255,.07)}.ct-v8-transfer>small{position:absolute;top:calc(50% + 39px);color:var(--muted-2);font-size:9px;font-weight:800}

      .ct-v8-phone{grid-column:3;grid-row:2;position:relative;justify-self:center;width:320px;padding:10px;border:1px solid rgba(255,255,255,.2);border-radius:38px;background:#030407;box-shadow:0 30px 90px rgba(0,0,0,.55)}.ct-v8-notch{position:absolute;z-index:3;top:10px;left:50%;width:90px;height:24px;transform:translateX(-50%);border-radius:0 0 13px 13px;background:#030407}.ct-v8-screen{min-height:430px;padding:27px 19px 18px;border-radius:29px;background:radial-gradient(circle at 50% 0,rgba(59,111,255,.13),transparent 35%),#10141c}.ct-v8-screen>header{display:flex;align-items:center;justify-content:space-between}.ct-v8-screen>header strong{color:#7897ff;font-size:17px}.ct-v8-screen>header span{position:relative;font-size:18px}.ct-v8-screen>header span i{position:absolute;right:-5px;top:-5px;width:15px;height:15px;display:grid;place-items:center;border-radius:50%;background:var(--blue);font-size:8px;font-style:normal}.ct-v8-title{margin-top:30px}.ct-v8-title small,.ct-v8-title b{display:block}.ct-v8-title small{color:var(--muted-2);font-size:9px}.ct-v8-title b{margin-top:6px;font-size:22px}.ct-v8-appcard{margin-top:17px;padding:16px;border:1px solid rgba(124,153,255,.18);border-radius:17px;background:#151922}.ct-v8-person{display:grid;grid-template-columns:42px 1fr auto;gap:11px;align-items:center}.ct-v8-person>i{width:42px;height:42px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#b6c2ff;font-style:normal;font-weight:900}.ct-v8-person span b,.ct-v8-person span small{display:block}.ct-v8-person span b{font-size:14px}.ct-v8-person span small{margin-top:4px;color:var(--muted-2);font-size:8px}.ct-v8-person>em{padding:6px 7px;border-radius:999px;background:rgba(50,200,121,.09);color:#72dca6;font-size:8px;font-style:normal;font-weight:850}.ct-v8-msg{margin-top:14px;padding:12px;border:1px solid var(--line);border-radius:11px;background:#101319}.ct-v8-msg small,.ct-v8-msg strong{display:block}.ct-v8-msg small{color:var(--muted-2);font-size:8px}.ct-v8-msg strong{margin-top:6px;font-size:11px}.ct-v8-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:11px}.ct-v8-actions button{height:34px;border:1px solid var(--line);border-radius:9px;background:#12161d;color:#dce1eb;font-size:9px}.ct-v8-registered{display:flex;align-items:center;gap:10px;margin-top:11px;padding:11px;border:1px solid rgba(50,200,121,.25);border-radius:11px;background:rgba(50,200,121,.08);color:#74dfa7}.ct-v8-registered>i{width:25px;height:25px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.14);font-style:normal}.ct-v8-registered b,.ct-v8-registered small{display:block}.ct-v8-registered b{font-size:10px}.ct-v8-registered small{margin-top:3px;color:#6f9d84;font-size:7px}.ct-v8-push{position:absolute;z-index:5;left:50%;top:72px;width:calc(100% + 32px);display:flex;align-items:center;gap:10px;padding:13px 14px;transform:translate(-50%,-16px) scale(.97);border:1px solid rgba(112,146,255,.72);border-radius:14px;background:linear-gradient(135deg,#345ee7,#526fff);box-shadow:0 18px 42px rgba(28,58,160,.43);opacity:0}.ct-v8-push>i{width:29px;height:29px;display:grid;place-items:center;border-radius:50%;background:rgba(255,255,255,.16);font-style:normal}.ct-v8-push b,.ct-v8-push small{display:block}.ct-v8-push b{font-size:11px}.ct-v8-push small{margin-top:4px;font-size:8px;opacity:.88}.ct-v8-push>em{margin-left:auto;font-size:8px;font-style:normal;opacity:.75}

      .ct-v8-nocode{padding:150px 0;border-bottom:1px solid var(--line)}.ct-v8-nocode-grid{display:grid;grid-template-columns:.78fr 1.22fr;gap:86px;align-items:center}.ct-v8-nocode-copy h2{margin:0;font-size:clamp(50px,5.4vw,80px);line-height:1;letter-spacing:-.08em}.ct-v8-nocode-copy>strong{display:block;margin-top:25px;max-width:520px;color:var(--muted);font-size:17px;font-weight:600;line-height:1.7}.ct-v8-flow{display:grid;grid-template-columns:1fr 28px 1fr 28px 1fr 28px 1fr;align-items:center;padding:29px;border:1px solid var(--line-strong);border-radius:29px;background:linear-gradient(145deg,#171a20,#101217)}.ct-v8-flow article{min-height:132px;padding:18px;border:1px solid var(--line);border-radius:15px;background:#12151b;opacity:.5;transition:.35s}.ct-v8-flow article>b{color:#7897ff;font-size:11px}.ct-v8-flow article strong,.ct-v8-flow article small{display:block}.ct-v8-flow article strong{margin-top:22px;font-size:17px}.ct-v8-flow article small{margin-top:8px;color:var(--muted-2);font-size:9px;line-height:1.45}.ct-v8-flow article.on{border-color:rgba(89,126,255,.55);background:var(--blue-soft);opacity:1;transform:translateY(-6px)}.ct-v8-flow>i{color:#424a58;font-style:normal;text-align:center}

      .ct-v8-stage.is-running .ct-v8-complete{animation:ctComplete .48s ease .05s 2 alternate}.ct-v8-stage.is-running .ct-v8-dot{animation:ctTransfer .62s ease-in-out .35s both}.ct-v8-stage.is-running .ct-v8-push{animation:ctPush 2.25s cubic-bezier(.2,.8,.2,1) .92s both}.ct-v8-stage.is-running .ct-v8-phone{animation:ctPhone 1.1s ease 1s}.ct-v8-stage.is-running .ct-v8-registered{animation:ctComplete .42s ease 1.48s 2 alternate}
      @keyframes ctComplete{to{transform:scale(1.025);filter:brightness(1.12)}}@keyframes ctTransfer{0%{left:4px;opacity:0}12%{opacity:1}100%{left:calc(100% - 12px);opacity:1}}@keyframes ctPush{0%{opacity:0;transform:translate(-50%,-16px) scale(.97)}15%,72%{opacity:1;transform:translate(-50%,0) scale(1)}100%{opacity:0;transform:translate(-50%,-10px) scale(.98)}}@keyframes ctPhone{45%{box-shadow:0 30px 95px rgba(39,77,220,.42)}}

      @media(max-width:980px){.ct-v8-hero{padding-top:132px}.ct-v8-stage{grid-template-columns:1fr;grid-template-rows:auto auto 74px auto auto;max-width:720px;padding:30px}.ct-v8-step-left{grid-column:1;grid-row:1}.ct-v8-inquiry{grid-column:1;grid-row:2;min-height:auto}.ct-v8-transfer{grid-column:1;grid-row:3;width:100%;height:74px}.ct-v8-line{left:50%;right:auto;top:0;width:2px;height:74px;transform:translateX(-50%)}.ct-v8-dot{left:calc(50% - 4px);top:2px}.ct-v8-transfer>b{transform:rotate(90deg)}.ct-v8-transfer>small{top:auto;bottom:0}.ct-v8-step-right{grid-column:1;grid-row:4;justify-content:flex-start;margin-top:8px}.ct-v8-phone{grid-column:1;grid-row:5}.ct-v8-stage.is-running .ct-v8-dot{animation:ctTransferMobile .62s ease-in-out .35s both}.ct-v8-nocode-grid{grid-template-columns:1fr}.ct-v8-flow{grid-template-columns:1fr;gap:10px}.ct-v8-flow>i{transform:rotate(90deg)}}
      @keyframes ctTransferMobile{0%{top:2px;opacity:0}12%{opacity:1}100%{top:64px;opacity:1}}
      @media(max-width:640px){.ct-v8-hero{padding:112px 0 82px}.ct-v8-head h1{font-size:clamp(42px,12.5vw,58px);line-height:.97;letter-spacing:-.07em}.ct-v8-head>strong{max-width:330px;margin-top:21px;padding:0 8px;font-size:14px}.ct-v8-stage{margin-top:54px;padding:22px 16px;border-radius:23px;row-gap:15px}.ct-v8-step{padding-left:4px;font-size:12px}.ct-v8-inquiry{padding:19px}.ct-v8-customer{padding:22px 0 14px}.ct-v8-customer h2{font-size:28px}.ct-v8-inquiry dl div{min-height:54px;padding:0 13px}.ct-v8-inquiry dd{max-width:68%;font-size:11px}.ct-v8-phone{width:min(304px,100%)}.ct-v8-push{width:calc(100% + 18px)}.ct-v8-nocode{padding:104px 0}.ct-v8-nocode-grid{gap:46px}.ct-v8-nocode-copy h2{font-size:42px}.ct-v8-nocode-copy>strong{font-size:15px}.ct-v8-flow{padding:18px}.ct-v8-flow article{min-height:auto}}
      @media(prefers-reduced-motion:reduce){#ct-pagero-intro *{animation:none!important;transition:none!important}.ct-v8-push{opacity:1;transform:translate(-50%,0)}}
    `;
    document.head.append(style);

    const stage=intro.querySelector('.ct-v8-stage');
    const run=()=>{
      stage.classList.remove('is-running');
      void stage.offsetWidth;
      stage.classList.add('is-running');
    };
    run();
    const animationTimer=setInterval(run,3900);

    const flow=[...intro.querySelectorAll('.ct-v8-flow article')];
    let idx=0;
    const flowTimer=setInterval(()=>{
      flow.forEach((el,i)=>el.classList.toggle('on',i===idx));
      idx=(idx+1)%flow.length;
    },1700);

    const observer=new MutationObserver(removeLegacy);
    observer.observe(document.body,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),10000);
    window.addEventListener('pagehide',()=>{clearInterval(animationTimer);clearInterval(flowTimer);},{once:true});
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();