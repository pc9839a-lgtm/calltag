(()=>{
  if(document.documentElement.dataset.ctPageroSignalV7)return;
  document.documentElement.dataset.ctPageroSignalV7='1';

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

  const loadGsap=()=>new Promise(resolve=>{
    if(window.gsap)return resolve(window.gsap);
    const old=document.querySelector('script[data-ct-gsap]');
    if(old){old.addEventListener('load',()=>resolve(window.gsap||null),{once:true});return;}
    const s=document.createElement('script');
    s.dataset.ctGsap='1';s.src='https://cdn.jsdelivr.net/npm/gsap@3.15.0/dist/gsap.min.js';s.async=true;
    s.onload=()=>resolve(window.gsap||null);s.onerror=()=>resolve(null);document.head.append(s);
  });

  const mount=async()=>{
    removeLegacy();
    document.querySelector('#ct-pagero-intro')?.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-v7-hero">
        <div class="wrap ct-v7-wrap">
          <header class="ct-v7-head">
            <p>PAGERO × CALLTAG</p>
            <h1>문의가 들어오는 순간,<br><span>절대 놓치지 않습니다.</span></h1>
            <strong>페이지로에서 문의를 받고, 콜태그가 바로 <em>알림·등록·후속관리</em>합니다.</strong>
          </header>

          <div class="ct-v7-stage">
            <section class="ct-v7-side">
              <div class="ct-v7-step"><b>1</b><span>페이지로 문의 접수</span></div>
              <article class="ct-v7-inquiry">
                <header><strong>PAGERO</strong><small>새 문의</small></header>
                <div class="ct-v7-customer"><small>무료 상담 신청</small><h2>김민수 고객</h2></div>
                <dl>
                  <div><dt>연락처</dt><dd>010-1234-5678</dd></div>
                  <div><dt>문의내용</dt><dd>보험 상담 요청드립니다</dd></div>
                </dl>
                <div class="ct-v7-complete"><i>✓</i><span>문의접수완료</span></div>
              </article>
            </section>

            <div class="ct-v7-transfer" aria-hidden="true">
              <span></span><i></i><b>→</b><small>즉시 전달</small>
            </div>

            <section class="ct-v7-side ct-v7-phone-side">
              <div class="ct-v7-step"><b>2</b><span>콜태그 즉시 알림</span></div>
              <div class="ct-v7-phone">
                <div class="ct-v7-notch"></div>
                <div class="ct-v7-screen">
                  <header><strong>CALLTAG</strong><span>♢<i>1</i></span></header>
                  <div class="ct-v7-title"><small>오늘 할 일</small><b>신규 문의 1</b></div>
                  <article class="ct-v7-appcard">
                    <div class="ct-v7-person"><i>김</i><span><b>김민수 고객</b><small>010-1234-5678 · 페이지로</small></span><em>신규 문의</em></div>
                    <div class="ct-v7-msg"><small>문의내용</small><strong>보험 상담 요청드립니다</strong></div>
                    <div class="ct-v7-actions"><button>전화</button><button>문자</button><button>태그</button></div>
                    <div class="ct-v7-registered"><i>✓</i><span><b>고객 자동등록 완료</b><small>바로 후속관리할 수 있습니다.</small></span></div>
                  </article>
                </div>
                <div class="ct-v7-push"><i>●</i><span><b>새 문의 접수</b><small>김민수 고객 · 010-1234-5678</small></span><em>지금</em></div>
              </div>
            </section>
          </div>

          <div class="ct-v7-progress">
            <span class="on"><b>01</b>문의 정보 확인</span><i>→</i><span><b>02</b>문의접수완료</span><i>→</i><span><b>03</b>앱 즉시 알림</span><i>→</i><span><b>04</b>고객 자동등록</span>
          </div>
        </div>
      </section>

      <section class="ct-v7-nocode">
        <div class="wrap ct-v7-nocode-grid">
          <div class="ct-v7-nocode-copy">
            <p>페이지로</p>
            <h2>누구나 만들고,<br><span>문의까지 받습니다.</span></h2>
            <strong>코드를 몰라도 문구와 이미지만 바꾸면 랜딩페이지가 열리고, 접수된 문의는 콜태그에서 바로 확인됩니다.</strong>
          </div>
          <div class="ct-v7-flow">
            <article class="on"><b>01</b><span><strong>내용 입력</strong><small>문구와 이미지 수정</small></span></article><i>→</i>
            <article><b>02</b><span><strong>문의 폼 추가</strong><small>연락처와 문의내용 수집</small></span></article><i>→</i>
            <article><b>03</b><span><strong>바로 공개</strong><small>광고와 SNS에 사용</small></span></article><i>→</i>
            <article><b>04</b><span><strong>콜태그 확인</strong><small>알림·등록·후속관리</small></span></article>
          </div>
        </div>
      </section>`;
    calltagHero.parentNode.insertBefore(intro,calltagHero);
    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());

    const style=document.createElement('style');
    style.dataset.ctPageroIntro='7';
    style.textContent=`
      #ct-pagero-intro{background:var(--bg);color:var(--text);overflow:hidden}.ct-v7-hero{position:relative;padding:164px 0 120px;border-bottom:1px solid var(--line);background:radial-gradient(circle at 50% -20%,rgba(59,111,255,.19),transparent 40%),linear-gradient(180deg,#090b12,var(--bg) 78%)}
      .ct-v7-head{text-align:center}.ct-v7-head>p,.ct-v7-nocode-copy>p{margin:0 0 19px;color:var(--blue-2);font-size:13px;font-weight:900;letter-spacing:.08em}.ct-v7-head h1{margin:0;font-size:clamp(58px,7vw,100px);line-height:.93;letter-spacing:-.084em}.ct-v7-head h1 span,.ct-v7-nocode-copy h2 span{color:var(--blue-2)}.ct-v7-head>strong{display:block;margin:31px auto 0;color:#cdd2dc;font-size:clamp(16px,1.35vw,20px);font-weight:650;line-height:1.55}.ct-v7-head>strong em{color:#90a8ff;font-style:normal;font-weight:850}
      .ct-v7-stage{display:grid;grid-template-columns:minmax(0,1fr) 100px minmax(330px,.85fr);gap:24px;align-items:center;max-width:1280px;margin:86px auto 0;padding:42px;border:1px solid rgba(124,153,255,.24);border-radius:32px;background:linear-gradient(145deg,#151922,#0e1117);box-shadow:0 44px 110px rgba(0,0,0,.43)}.ct-v7-side{min-width:0}.ct-v7-step{display:flex;align-items:center;justify-content:flex-start;gap:11px;margin:0 0 22px 4px;color:#edf0f6;font-size:12px;font-weight:800}.ct-v7-step b{width:30px;height:30px;display:grid;place-items:center;border-radius:50%;background:var(--blue);color:#fff;font-size:11px;box-shadow:0 0 0 7px rgba(59,111,255,.1)}
      .ct-v7-inquiry{padding:27px;border:1px solid var(--line-strong);border-radius:23px;background:#0e1117}.ct-v7-inquiry>header{display:flex;align-items:center;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid var(--line)}.ct-v7-inquiry>header strong{color:var(--blue-2);font-size:11px}.ct-v7-inquiry>header small{color:var(--muted-2);font-size:10px}.ct-v7-customer{padding:26px 0 16px}.ct-v7-customer small{color:#8da6ff;font-size:10px;font-weight:850}.ct-v7-customer h2{margin:10px 0 0;font-size:34px;letter-spacing:-.06em}.ct-v7-inquiry dl{display:grid;gap:10px;margin:0}.ct-v7-inquiry dl div{min-height:58px;display:flex;align-items:center;justify-content:space-between;gap:20px;padding:0 15px;border:1px solid var(--line);border-radius:12px;background:#151922}.ct-v7-inquiry dt{color:var(--muted-2);font-size:10px}.ct-v7-inquiry dd{margin:0;color:#fff;font-size:12px;font-weight:820;text-align:right}.ct-v7-complete{display:flex;align-items:center;gap:11px;margin-top:13px;padding:15px;border:1px solid rgba(50,200,121,.3);border-radius:13px;background:rgba(50,200,121,.1);color:#72e2a8;font-size:12px;font-weight:900}.ct-v7-complete i{width:27px;height:27px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.16);font-style:normal}
      .ct-v7-transfer{position:relative;min-height:150px;display:grid;place-items:center}.ct-v7-transfer>span{position:absolute;width:100px;height:2px;background:linear-gradient(90deg,rgba(59,111,255,.1),rgba(112,145,255,.78),rgba(59,111,255,.1))}.ct-v7-transfer>i{position:absolute;left:0;width:8px;height:8px;border-radius:50%;background:#8ca5ff;box-shadow:0 0 18px 7px rgba(59,111,255,.34)}.ct-v7-transfer>b{position:relative;z-index:2;width:52px;height:52px;display:grid;place-items:center;border:1px solid rgba(95,132,255,.55);border-radius:50%;background:#11182b;color:#89a3ff;font-size:26px;box-shadow:0 0 0 8px rgba(59,111,255,.07)}.ct-v7-transfer>small{position:absolute;top:calc(50% + 38px);color:var(--muted-2);font-size:9px;font-weight:800}
      .ct-v7-phone-side{display:flex;flex-direction:column;align-items:center}.ct-v7-phone-side .ct-v7-step{align-self:center;margin-left:0;margin-bottom:24px}.ct-v7-phone{position:relative;width:min(320px,100%);padding:10px;border:1px solid rgba(255,255,255,.2);border-radius:38px;background:#030407;box-shadow:0 30px 90px rgba(0,0,0,.55)}.ct-v7-notch{position:absolute;z-index:3;top:10px;left:50%;width:90px;height:24px;transform:translateX(-50%);border-radius:0 0 13px 13px;background:#030407}.ct-v7-screen{min-height:430px;padding:27px 19px 18px;border-radius:29px;background:radial-gradient(circle at 50% 0,rgba(59,111,255,.13),transparent 35%),#10141c}.ct-v7-screen>header{display:flex;align-items:center;justify-content:space-between}.ct-v7-screen>header strong{color:#7897ff;font-size:17px}.ct-v7-screen>header span{position:relative;font-size:18px}.ct-v7-screen>header span i{position:absolute;right:-5px;top:-5px;width:15px;height:15px;display:grid;place-items:center;border-radius:50%;background:var(--blue);font-size:8px;font-style:normal}.ct-v7-title{margin-top:30px}.ct-v7-title small,.ct-v7-title b{display:block}.ct-v7-title small{color:var(--muted-2);font-size:9px}.ct-v7-title b{margin-top:6px;font-size:22px}.ct-v7-appcard{margin-top:17px;padding:16px;border:1px solid rgba(124,153,255,.18);border-radius:17px;background:#151922}.ct-v7-person{display:grid;grid-template-columns:42px 1fr auto;gap:11px;align-items:center}.ct-v7-person>i{width:42px;height:42px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#b6c2ff;font-style:normal;font-weight:900}.ct-v7-person span b,.ct-v7-person span small{display:block}.ct-v7-person span b{font-size:14px}.ct-v7-person span small{margin-top:4px;color:var(--muted-2);font-size:8px}.ct-v7-person>em{padding:6px 7px;border-radius:999px;background:rgba(50,200,121,.09);color:#72dca6;font-size:8px;font-style:normal;font-weight:850}.ct-v7-msg{margin-top:14px;padding:12px;border:1px solid var(--line);border-radius:11px;background:#101319}.ct-v7-msg small,.ct-v7-msg strong{display:block}.ct-v7-msg small{color:var(--muted-2);font-size:8px}.ct-v7-msg strong{margin-top:6px;font-size:11px}.ct-v7-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:11px}.ct-v7-actions button{height:34px;border:1px solid var(--line);border-radius:9px;background:#12161d;color:#dce1eb;font-size:9px}.ct-v7-registered{display:flex;align-items:center;gap:10px;margin-top:11px;padding:11px;border:1px solid rgba(50,200,121,.25);border-radius:11px;background:rgba(50,200,121,.08);color:#74dfa7}.ct-v7-registered>i{width:25px;height:25px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.14);font-style:normal}.ct-v7-registered b,.ct-v7-registered small{display:block}.ct-v7-registered b{font-size:10px}.ct-v7-registered small{margin-top:3px;color:#6f9d84;font-size:7px}.ct-v7-push{position:absolute;z-index:5;left:50%;top:72px;width:calc(100% + 58px);display:flex;align-items:center;gap:10px;padding:13px 14px;transform:translateX(-50%);border:1px solid rgba(112,146,255,.72);border-radius:14px;background:linear-gradient(135deg,#345ee7,#526fff);box-shadow:0 18px 42px rgba(28,58,160,.43);opacity:0}.ct-v7-push>i{width:29px;height:29px;display:grid;place-items:center;border-radius:50%;background:rgba(255,255,255,.16);font-style:normal}.ct-v7-push b,.ct-v7-push small{display:block}.ct-v7-push b{font-size:11px}.ct-v7-push small{margin-top:4px;font-size:8px;opacity:.88}.ct-v7-push>em{margin-left:auto;font-size:8px;font-style:normal;opacity:.75}
      .ct-v7-progress{display:flex;align-items:center;justify-content:center;gap:14px;margin-top:25px;color:#5f6775;font-size:10px;font-weight:800}.ct-v7-progress span{transition:.25s}.ct-v7-progress span b{margin-right:5px;color:#7897ff}.ct-v7-progress span.on{color:#eef1f7}.ct-v7-progress>i{color:#3b4350;font-style:normal}
      .ct-v7-nocode{padding:158px 0;border-bottom:1px solid var(--line)}.ct-v7-nocode-grid{display:grid;grid-template-columns:.78fr 1.22fr;gap:86px;align-items:center}.ct-v7-nocode-copy h2{margin:0;font-size:clamp(50px,5.4vw,80px);line-height:1;letter-spacing:-.08em}.ct-v7-nocode-copy>strong{display:block;margin-top:25px;max-width:520px;color:var(--muted);font-size:17px;font-weight:600;line-height:1.7}.ct-v7-flow{display:grid;grid-template-columns:1fr 28px 1fr 28px 1fr 28px 1fr;align-items:center;padding:29px;border:1px solid var(--line-strong);border-radius:29px;background:linear-gradient(145deg,#171a20,#101217)}.ct-v7-flow article{min-height:132px;padding:18px;border:1px solid var(--line);border-radius:15px;background:#12151b;opacity:.5;transition:.35s}.ct-v7-flow article>b{color:#7897ff;font-size:11px}.ct-v7-flow article strong,.ct-v7-flow article small{display:block}.ct-v7-flow article strong{margin-top:22px;font-size:17px}.ct-v7-flow article small{margin-top:8px;color:var(--muted-2);font-size:9px;line-height:1.45}.ct-v7-flow article.on{border-color:rgba(89,126,255,.55);background:var(--blue-soft);opacity:1;transform:translateY(-6px)}.ct-v7-flow>i{color:#424a58;font-style:normal;text-align:center}
      @media(max-width:980px){.ct-v7-hero{padding-top:138px}.ct-v7-stage{grid-template-columns:1fr;max-width:720px;padding:32px}.ct-v7-transfer{min-height:76px}.ct-v7-transfer>span{width:2px;height:64px}.ct-v7-transfer>i{left:calc(50% - 4px);top:0}.ct-v7-transfer>b{transform:rotate(90deg)}.ct-v7-transfer>small{top:auto;bottom:-1px}.ct-v7-phone-side .ct-v7-step{margin-top:6px}.ct-v7-nocode-grid{grid-template-columns:1fr}.ct-v7-flow{grid-template-columns:1fr;gap:10px}.ct-v7-flow>i{transform:rotate(90deg)}}
      @media(max-width:640px){.ct-v7-hero{padding:122px 0 88px}.ct-v7-head h1{font-size:clamp(43px,13vw,62px);line-height:.96}.ct-v7-head>strong{margin-top:22px;font-size:15px;padding:0 8px}.ct-v7-stage{margin-top:62px;padding:24px 17px;border-radius:24px}.ct-v7-step{margin-bottom:18px}.ct-v7-inquiry{padding:20px}.ct-v7-customer h2{font-size:29px}.ct-v7-phone{width:min(306px,100%)}.ct-v7-push{width:calc(100% + 24px)}.ct-v7-progress{display:grid;grid-template-columns:1fr 1fr;gap:9px 14px;text-align:center}.ct-v7-progress>i{display:none}.ct-v7-nocode{padding:110px 0}.ct-v7-nocode-grid{gap:48px}.ct-v7-nocode-copy h2{font-size:44px}.ct-v7-flow{padding:20px}.ct-v7-flow article{min-height:auto}}
      @media(prefers-reduced-motion:reduce){#ct-pagero-intro *{animation:none!important;transition:none!important}}
    `;
    document.head.append(style);

    const steps=[...intro.querySelectorAll('.ct-v7-progress span')];
    const setStep=n=>steps.forEach((el,i)=>el.classList.toggle('on',i===n));
    const gsap=await loadGsap();
    const done=intro.querySelector('.ct-v7-complete');
    const dot=intro.querySelector('.ct-v7-transfer>i');
    const push=intro.querySelector('.ct-v7-push');
    const phone=intro.querySelector('.ct-v7-phone');
    const registered=intro.querySelector('.ct-v7-registered');

    if(gsap&&!matchMedia('(prefers-reduced-motion: reduce)').matches){
      gsap.set(push,{opacity:0,y:-18,scale:.96});
      gsap.set(dot,{x:0,opacity:0});
      const tl=gsap.timeline({repeat:-1,repeatDelay:.7});
      tl.call(()=>setStep(0),[],0)
        .fromTo(done,{scale:.985},{scale:1.02,duration:.22,yoyo:true,repeat:1,ease:'power2.out'},.15)
        .call(()=>setStep(1),[],.45)
        .to(dot,{x:92,opacity:1,duration:.48,ease:'power2.inOut'},.55)
        .call(()=>setStep(2),[],1.05)
        .to(push,{opacity:1,y:0,scale:1,duration:.42,ease:'back.out(1.45)'},1.05)
        .to(phone,{boxShadow:'0 30px 95px rgba(39,77,220,.42)',duration:.34,yoyo:true,repeat:1},1.12)
        .call(()=>setStep(3),[],1.55)
        .fromTo(registered,{scale:.98},{scale:1.025,duration:.25,yoyo:true,repeat:1,ease:'power2.out'},1.58)
        .to(push,{opacity:0,y:-14,duration:.3},'+=1.05')
        .set(dot,{x:0,opacity:0});
    }else{
      push.style.opacity='1';setStep(3);
    }

    const flow=[...intro.querySelectorAll('.ct-v7-flow article')];
    let idx=0;setInterval(()=>{flow.forEach((el,i)=>el.classList.toggle('on',i===idx));idx=(idx+1)%flow.length;},1700);

    const observer=new MutationObserver(removeLegacy);observer.observe(document.body,{childList:true,subtree:true});setTimeout(()=>observer.disconnect(),10000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();