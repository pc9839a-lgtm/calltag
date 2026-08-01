(()=>{
  if(document.documentElement.dataset.ctPageroSignalV6)return;
  document.documentElement.dataset.ctPageroSignalV6='1';

  const removeLegacy=()=>{
    const direct=[
      '.ct-legacy-pagero','.ct-pagero-bridge','.journey-bridge','.sticky-offer',
      '.floating-offer','.bottom-offer','.offer-bar','.fixed-offer','.ct-fixed-offer'
    ];
    direct.forEach(selector=>document.querySelectorAll(selector).forEach(el=>el.remove()));

    const phrases=[
      '고객을 받는 페이지로','놓치지 않는 콜태그','2026년 가입가 평생 유지','7일 무료체험'
    ];
    [...document.querySelectorAll('section,aside,div')].forEach(el=>{
      if(el.id==='ct-pagero-intro'||el.closest('#ct-pagero-intro'))return;
      const text=(el.textContent||'').replace(/\s+/g,' ').trim();
      if(!text||text.length>260)return;
      if(phrases.some(phrase=>text.includes(phrase))){
        const fixed=getComputedStyle(el).position==='fixed';
        if(fixed||phrases.slice(0,2).some(phrase=>text.includes(phrase)))el.remove();
      }
    });
  };

  const loadGsap=()=>new Promise(resolve=>{
    if(window.gsap)return resolve(window.gsap);
    const existing=document.querySelector('script[data-ct-gsap]');
    if(existing){
      existing.addEventListener('load',()=>resolve(window.gsap||null),{once:true});
      existing.addEventListener('error',()=>resolve(null),{once:true});
      return;
    }
    const script=document.createElement('script');
    script.dataset.ctGsap='1';
    script.src='https://cdn.jsdelivr.net/npm/gsap@3.15.0/dist/gsap.min.js';
    script.async=true;
    script.onload=()=>resolve(window.gsap||null);
    script.onerror=()=>resolve(null);
    document.head.append(script);
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
      <section class="ct-signal-hero" aria-labelledby="ct-signal-title">
        <div class="ct-signal-glow ct-signal-glow-a"></div>
        <div class="ct-signal-glow ct-signal-glow-b"></div>
        <div class="wrap ct-signal-wrap">
          <header class="ct-signal-head">
            <p class="ct-signal-kicker">PAGERO × CALLTAG</p>
            <h1 id="ct-signal-title"><span class="ct-line">문의가 들어오는 순간,</span><span class="ct-line accent">절대 놓치지 않습니다.</span></h1>
            <strong>페이지로에서 문의를 받고, 콜태그가 바로 <em>알림·등록·후속관리</em>합니다.</strong>
          </header>

          <div class="ct-signal-stage" aria-label="페이지로 문의가 콜태그 앱 알림과 고객 등록으로 이어지는 예시">
            <div class="ct-stage-badge left"><b>1</b><span>페이지로 문의 접수</span></div>
            <div class="ct-stage-badge right"><b>2</b><span>콜태그 즉시 알림</span></div>

            <article class="ct-inquiry-card">
              <header><span>PAGERO</span><em>새 문의</em></header>
              <div class="ct-inquiry-title">
                <small>무료 상담 신청</small>
                <h2>김민수 고객</h2>
              </div>
              <dl>
                <div class="ct-field ct-contact"><dt>연락처</dt><dd>010-1234-5678</dd></div>
                <div class="ct-field ct-message"><dt>문의내용</dt><dd>보험 상담 요청드립니다</dd></div>
              </dl>
              <div class="ct-inquiry-done"><i>✓</i><span>문의접수완료</span></div>
            </article>

            <div class="ct-signal-transfer" aria-hidden="true">
              <span class="ct-transfer-line"></span>
              <i class="ct-transfer-dot"></i>
              <b>→</b>
              <small>즉시 전달</small>
            </div>

            <div class="ct-phone-wrap">
              <div class="ct-phone">
                <div class="ct-phone-speaker"></div>
                <div class="ct-phone-screen">
                  <div class="ct-phone-top"><strong>CALLTAG</strong><span class="ct-bell">♢<i>1</i></span></div>
                  <div class="ct-app-title"><small>오늘 할 일</small><b>신규 문의 1</b></div>
                  <article class="ct-app-customer">
                    <div class="ct-app-person"><i>김</i><span><b>김민수 고객</b><small>010-1234-5678 · 페이지로</small></span><em>신규 문의</em></div>
                    <div class="ct-app-message"><small>문의내용</small><strong>보험 상담 요청드립니다</strong></div>
                    <div class="ct-app-actions"><button>전화</button><button>문자</button><button>태그</button></div>
                    <div class="ct-app-registered"><i>✓</i><span><b>고객 등록 완료</b><small>후속관리 준비됨</small></span></div>
                  </article>
                </div>
              </div>

              <div class="ct-push-notification">
                <i class="ct-push-icon">●</i>
                <span><b>새 문의 접수</b><small>김민수 고객 · 010-1234-5678</small></span>
                <em>지금</em>
              </div>
            </div>
          </div>

          <div class="ct-signal-steps" aria-label="문의 처리 단계">
            <span class="active"><b>01</b>연락처·문의내용</span><i>→</i>
            <span><b>02</b>문의접수완료</span><i>→</i>
            <span><b>03</b>앱 즉시 알림</span><i>→</i>
            <span><b>04</b>고객 자동등록</span>
          </div>
        </div>
      </section>

      <section class="ct-nocode-section">
        <div class="wrap ct-nocode-grid">
          <div class="ct-nocode-copy">
            <p>페이지로</p>
            <h2>누구나 만들고,<br><span>문의까지 받습니다.</span></h2>
            <strong>코드를 몰라도 문구와 이미지만 바꾸면 랜딩페이지가 열리고, 접수된 문의는 콜태그에서 바로 확인됩니다.</strong>
          </div>
          <div class="ct-nocode-flow">
            <article class="on"><b>01</b><span><strong>내용 입력</strong><small>문구와 이미지만 바꾸기</small></span></article>
            <i>→</i>
            <article><b>02</b><span><strong>문의 폼 추가</strong><small>연락처와 문의내용 받기</small></span></article>
            <i>→</i>
            <article><b>03</b><span><strong>바로 공개</strong><small>광고와 SNS에 사용</small></span></article>
            <i>→</i>
            <article><b>04</b><span><strong>콜태그 확인</strong><small>알림·등록·후속관리</small></span></article>
          </div>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);
    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());

    const style=document.createElement('style');
    style.dataset.ctPageroIntro='6';
    style.textContent=`
      #ct-pagero-intro{background:var(--bg);color:var(--text);overflow:hidden}
      .ct-signal-hero{position:relative;padding:132px 0 118px;border-bottom:1px solid var(--line);overflow:hidden;background:linear-gradient(180deg,#090b12 0%,var(--bg) 72%)}
      .ct-signal-wrap{position:relative;z-index:2}.ct-signal-glow{position:absolute;border-radius:50%;pointer-events:none;filter:blur(8px)}.ct-signal-glow-a{width:900px;height:900px;top:-650px;left:50%;transform:translateX(-50%);background:radial-gradient(circle,rgba(61,102,255,.27),transparent 68%)}.ct-signal-glow-b{width:620px;height:620px;right:-300px;bottom:-300px;background:radial-gradient(circle,rgba(59,111,255,.13),transparent 69%)}
      .ct-signal-head{text-align:center}.ct-signal-kicker{margin:0 0 18px;color:var(--blue-2);font-size:13px;font-weight:900;letter-spacing:.08em}.ct-signal-head h1{margin:0;font-size:clamp(60px,7.4vw,108px);line-height:.91;letter-spacing:-.087em}.ct-signal-head h1 .ct-line{display:block}.ct-signal-head h1 .accent{color:var(--blue-2)}.ct-signal-head>strong{display:block;margin:27px auto 0;color:#c9ced8;font-size:clamp(16px,1.45vw,21px);font-weight:650;line-height:1.55;letter-spacing:-.03em}.ct-signal-head>strong em{color:#8da6ff;font-style:normal;font-weight:850}
      .ct-signal-stage{position:relative;display:grid;grid-template-columns:minmax(0,.86fr) 110px minmax(360px,.84fr);align-items:center;gap:18px;max-width:1280px;margin:70px auto 0;padding:74px 48px 46px;border:1px solid rgba(124,153,255,.25);border-radius:34px;background:radial-gradient(circle at 75% 20%,rgba(59,111,255,.11),transparent 35%),linear-gradient(145deg,#151922,#0e1117);box-shadow:0 44px 120px rgba(0,0,0,.48),inset 0 1px 0 rgba(255,255,255,.025)}
      .ct-stage-badge{position:absolute;top:21px;display:flex;align-items:center;gap:10px;color:#e6e9f1;font-size:12px;font-weight:800}.ct-stage-badge.left{left:48px}.ct-stage-badge.right{right:48px}.ct-stage-badge b{width:27px;height:27px;display:grid;place-items:center;border-radius:50%;background:var(--blue);color:#fff;font-size:11px;box-shadow:0 0 0 6px rgba(59,111,255,.11)}
      .ct-inquiry-card{min-width:0;padding:27px;border:1px solid var(--line-strong);border-radius:23px;background:rgba(13,16,22,.94);box-shadow:0 24px 70px rgba(0,0,0,.3)}.ct-inquiry-card>header{display:flex;align-items:center;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid var(--line)}.ct-inquiry-card>header span{color:var(--blue-2);font-size:11px;font-weight:950}.ct-inquiry-card>header em{color:var(--muted-2);font-size:10px;font-style:normal;font-weight:750}.ct-inquiry-title{padding:28px 0 17px}.ct-inquiry-title small{color:#8da6ff;font-size:10px;font-weight:850}.ct-inquiry-title h2{margin:10px 0 0;font-size:34px;line-height:1;letter-spacing:-.06em}.ct-inquiry-card dl{display:grid;gap:10px;margin:0}.ct-inquiry-card dl div{min-height:58px;display:flex;align-items:center;justify-content:space-between;gap:18px;padding:0 15px;border:1px solid var(--line);border-radius:12px;background:#151922}.ct-inquiry-card dt{color:var(--muted-2);font-size:10px}.ct-inquiry-card dd{margin:0;color:#fff;font-size:12px;font-weight:820;text-align:right}.ct-inquiry-done{display:flex;align-items:center;gap:11px;margin-top:13px;padding:15px;border:1px solid rgba(50,200,121,.3);border-radius:13px;background:rgba(50,200,121,.1);color:#72e2a8;font-size:12px;font-weight:900}.ct-inquiry-done i{width:28px;height:28px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.16);font-style:normal}
      .ct-signal-transfer{position:relative;min-height:170px;display:grid;place-items:center}.ct-transfer-line{position:absolute;width:110px;height:2px;background:linear-gradient(90deg,rgba(59,111,255,.12),rgba(102,137,255,.7),rgba(59,111,255,.12))}.ct-transfer-dot{position:absolute;left:2px;width:8px;height:8px;border-radius:50%;background:#86a1ff;box-shadow:0 0 18px 7px rgba(59,111,255,.34)}.ct-signal-transfer>b{position:relative;z-index:2;width:54px;height:54px;display:grid;place-items:center;border:1px solid rgba(95,132,255,.55);border-radius:50%;background:#11182b;color:#89a3ff;font-size:27px;box-shadow:0 0 0 8px rgba(59,111,255,.07)}.ct-signal-transfer>small{position:absolute;top:calc(50% + 39px);color:var(--muted-2);font-size:9px;font-weight:800}
      .ct-phone-wrap{position:relative;min-height:490px;display:grid;place-items:center}.ct-phone{position:relative;width:min(330px,100%);padding:10px;border:1px solid rgba(255,255,255,.19);border-radius:38px;background:#030407;box-shadow:0 30px 90px rgba(0,0,0,.57)}.ct-phone-speaker{position:absolute;z-index:3;top:14px;left:50%;width:90px;height:23px;transform:translateX(-50%);border-radius:0 0 13px 13px;background:#030407}.ct-phone-screen{min-height:465px;padding:27px 19px 18px;border-radius:29px;background:radial-gradient(circle at 50% 0,rgba(59,111,255,.13),transparent 35%),#10141c}.ct-phone-top{display:flex;align-items:center;justify-content:space-between}.ct-phone-top>strong{color:#7897ff;font-size:17px;font-weight:950}.ct-bell{position:relative;color:#dfe4f2;font-size:18px}.ct-bell i{position:absolute;right:-4px;top:-4px;width:15px;height:15px;display:grid;place-items:center;border-radius:50%;background:var(--blue);color:#fff;font-size:8px;font-style:normal}.ct-app-title{margin-top:31px}.ct-app-title small,.ct-app-title b{display:block}.ct-app-title small{color:var(--muted-2);font-size:9px}.ct-app-title b{margin-top:7px;font-size:22px;letter-spacing:-.05em}.ct-app-customer{margin-top:17px;padding:17px;border:1px solid rgba(124,153,255,.28);border-radius:17px;background:#151a24}.ct-app-person{display:grid;grid-template-columns:42px 1fr auto;align-items:center;gap:11px}.ct-app-person>i{width:42px;height:42px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#b7c4ff;font-size:12px;font-style:normal;font-weight:900}.ct-app-person span b,.ct-app-person span small{display:block}.ct-app-person span b{font-size:14px}.ct-app-person span small{margin-top:4px;color:var(--muted-2);font-size:9px}.ct-app-person>em{padding:6px 8px;border-radius:999px;background:rgba(50,200,121,.1);color:#70dea5;font-size:8px;font-style:normal;font-weight:850}.ct-app-message{margin-top:15px;padding:13px;border-radius:11px;background:#10141b}.ct-app-message small,.ct-app-message strong{display:block}.ct-app-message small{color:var(--muted-2);font-size:8px}.ct-app-message strong{margin-top:7px;font-size:11px}.ct-app-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:7px;margin-top:12px}.ct-app-actions button{height:38px;border:1px solid var(--line);border-radius:10px;background:#191e28;color:#bdc4d1;font-size:9px;font-weight:800}.ct-app-registered{display:flex;align-items:center;gap:10px;margin-top:12px;padding:12px;border:1px solid rgba(50,200,121,.25);border-radius:11px;background:rgba(50,200,121,.08)}.ct-app-registered>i{width:27px;height:27px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.13);color:#74e1a8;font-style:normal}.ct-app-registered span b,.ct-app-registered span small{display:block}.ct-app-registered span b{color:#79e2ab;font-size:10px}.ct-app-registered span small{margin-top:3px;color:#638678;font-size:8px}
      .ct-push-notification{position:absolute;z-index:5;top:82px;left:50%;width:min(390px,calc(100% + 70px));display:grid;grid-template-columns:43px 1fr auto;align-items:center;gap:11px;padding:13px 15px;border:1px solid rgba(111,146,255,.72);border-radius:15px;background:linear-gradient(135deg,rgba(66,109,255,.97),rgba(49,65,153,.97));box-shadow:0 18px 48px rgba(21,45,128,.5);transform:translateX(-50%)}.ct-push-icon{width:43px;height:43px;display:grid;place-items:center;border-radius:50%;background:rgba(255,255,255,.17);color:#fff;font-size:12px;font-style:normal}.ct-push-notification span b,.ct-push-notification span small{display:block}.ct-push-notification span b{font-size:13px}.ct-push-notification span small{margin-top:5px;color:#dce4ff;font-size:9px}.ct-push-notification>em{align-self:start;color:#ced8ff;font-size:8px;font-style:normal}
      .ct-signal-steps{display:flex;align-items:center;justify-content:center;gap:13px;margin-top:28px;color:#6f7683;font-size:10px;font-weight:800}.ct-signal-steps span{display:flex;align-items:center;gap:7px;transition:.3s ease}.ct-signal-steps span b{color:#728bdc;font-size:9px}.ct-signal-steps span.active{color:#fff}.ct-signal-steps span.active b{color:#86a0ff}.ct-signal-steps>i{color:#36415c;font-style:normal}
      .ct-nocode-section{padding:138px 0;border-bottom:1px solid var(--line);background:#0b0d12}.ct-nocode-grid{display:grid;grid-template-columns:.72fr 1.28fr;align-items:center;gap:70px}.ct-nocode-copy>p{margin:0 0 18px;color:var(--blue-2);font-size:15px;font-weight:900}.ct-nocode-copy h2{margin:0;font-size:clamp(48px,5.7vw,82px);line-height:.98;letter-spacing:-.08em}.ct-nocode-copy h2 span{color:var(--blue-2)}.ct-nocode-copy>strong{display:block;max-width:520px;margin-top:24px;color:var(--muted);font-size:16px;font-weight:580;line-height:1.67}.ct-nocode-flow{display:grid;grid-template-columns:1fr 28px 1fr 28px 1fr 28px 1fr;align-items:center;padding:28px;border:1px solid var(--line-strong);border-radius:28px;background:linear-gradient(145deg,#171a21,#101217);box-shadow:0 34px 90px rgba(0,0,0,.32)}.ct-nocode-flow article{min-height:170px;display:flex;flex-direction:column;justify-content:space-between;padding:20px;border:1px solid var(--line);border-radius:17px;background:#13161c;opacity:.46;transition:.4s ease}.ct-nocode-flow article>b{color:#7f94df;font-size:11px}.ct-nocode-flow article span strong,.ct-nocode-flow article span small{display:block}.ct-nocode-flow article span strong{font-size:17px}.ct-nocode-flow article span small{margin-top:8px;color:var(--muted-2);font-size:10px;line-height:1.5}.ct-nocode-flow article.on{border-color:rgba(59,111,255,.52);background:var(--blue-soft);opacity:1;transform:translateY(-7px);box-shadow:0 18px 44px rgba(27,53,137,.16)}.ct-nocode-flow>i{color:#50618f;font-style:normal;text-align:center}
      @media(max-width:1050px){.ct-signal-stage{grid-template-columns:1fr 76px minmax(320px,.82fr);padding-left:28px;padding-right:28px}.ct-stage-badge.left{left:28px}.ct-stage-badge.right{right:28px}.ct-nocode-grid{grid-template-columns:1fr}.ct-nocode-flow{grid-template-columns:1fr 22px 1fr 22px 1fr 22px 1fr}}
      @media(max-width:800px){.ct-signal-hero{padding:112px 0 82px}.ct-signal-stage{grid-template-columns:1fr;gap:24px;margin-top:48px;padding:66px 18px 24px}.ct-stage-badge.right{top:auto;right:auto;left:18px;bottom:493px}.ct-stage-badge.left{left:18px}.ct-signal-transfer{min-height:66px}.ct-transfer-line{width:2px;height:66px}.ct-transfer-dot{left:auto;top:0}.ct-signal-transfer>b{transform:rotate(90deg)}.ct-signal-transfer>small{top:48px}.ct-phone-wrap{min-height:500px}.ct-push-notification{width:calc(100% - 10px)}.ct-signal-steps{display:grid;grid-template-columns:1fr 1fr;gap:10px}.ct-signal-steps>i{display:none}.ct-signal-steps span{justify-content:center;padding:10px;border:1px solid var(--line);border-radius:10px}.ct-nocode-section{padding:96px 0}.ct-nocode-flow{grid-template-columns:1fr}.ct-nocode-flow>i{transform:rotate(90deg)}}
      @media(max-width:560px){.ct-signal-head h1{font-size:clamp(42px,13vw,60px);line-height:.96}.ct-signal-head>strong{font-size:14px}.ct-signal-stage{border-radius:24px}.ct-inquiry-card{padding:21px}.ct-phone{width:290px}.ct-push-notification{top:88px}.ct-nocode-copy h2{font-size:45px}.ct-nocode-flow{padding:18px}.ct-nocode-flow article{min-height:128px}.ct-stage-badge{font-size:10px}.ct-stage-badge b{width:24px;height:24px}.ct-stage-badge.right{bottom:493px}}
      @media(prefers-reduced-motion:reduce){#ct-pagero-intro *{animation:none!important;transition:none!important}}
    `;
    document.head.append(style);

    const gsap=await loadGsap();
    const reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
    const steps=[...intro.querySelectorAll('.ct-signal-steps span')];
    const flow=[...intro.querySelectorAll('.ct-nocode-flow article')];

    const setStep=index=>{
      steps.forEach((el,i)=>el.classList.toggle('active',i===index));
    };

    if(gsap&&!reduced){
      gsap.set(['.ct-signal-kicker','.ct-signal-head .ct-line','.ct-signal-head>strong'],{opacity:0,y:28});
      gsap.set(['.ct-inquiry-card','.ct-phone-wrap'],{opacity:0,y:34,scale:.97});
      gsap.set(['.ct-contact','.ct-message','.ct-inquiry-done'],{opacity:0,y:13});
      gsap.set('.ct-transfer-dot',{x:0,opacity:0});
      gsap.set('.ct-push-notification',{opacity:0,y:-24,scale:.92});
      gsap.set(['.ct-app-customer','.ct-app-registered'],{opacity:0,y:16});

      gsap.timeline({defaults:{ease:'power3.out'}})
        .to('.ct-signal-kicker',{opacity:1,y:0,duration:.35})
        .to('.ct-signal-head .ct-line',{opacity:1,y:0,duration:.52,stagger:.09},'-=.18')
        .to('.ct-signal-head>strong',{opacity:1,y:0,duration:.4},'-=.26')
        .to(['.ct-inquiry-card','.ct-phone-wrap'],{opacity:1,y:0,scale:1,duration:.55,stagger:.08},'-=.2');

      const story=gsap.timeline({repeat:-1,repeatDelay:1.25,defaults:{ease:'power3.out'}});
      story
        .call(()=>setStep(0))
        .fromTo('.ct-contact',{opacity:0,y:12},{opacity:1,y:0,duration:.28})
        .fromTo('.ct-message',{opacity:0,y:12},{opacity:1,y:0,duration:.28},'-=.08')
        .call(()=>setStep(1))
        .fromTo('.ct-inquiry-done',{opacity:0,y:12,scale:.97},{opacity:1,y:0,scale:1,duration:.3})
        .call(()=>setStep(2))
        .set('.ct-transfer-dot',{opacity:1,x:0})
        .to('.ct-transfer-dot',{x:96,duration:.42,ease:'power1.inOut'})
        .to('.ct-transfer-dot',{opacity:0,duration:.08})
        .fromTo('.ct-push-notification',{opacity:0,y:-25,scale:.92},{opacity:1,y:0,scale:1,duration:.38,ease:'back.out(1.35)'},'-=.09')
        .call(()=>setStep(3))
        .fromTo('.ct-app-customer',{opacity:0,y:18},{opacity:1,y:0,duration:.36},'-=.1')
        .fromTo('.ct-app-registered',{opacity:0,y:12},{opacity:1,y:0,duration:.3},'-=.13')
        .to('.ct-phone',{boxShadow:'0 30px 90px rgba(0,0,0,.57),0 0 0 2px rgba(80,122,255,.28)',duration:.2})
        .to({}, {duration:1.15})
        .to(['.ct-push-notification','.ct-app-customer','.ct-app-registered','.ct-inquiry-done','.ct-message','.ct-contact'],{opacity:0,y:10,duration:.24,stagger:.025,ease:'power2.in'});

      let flowIndex=0;
      const rotateFlow=()=>{
        flow.forEach((el,i)=>el.classList.toggle('on',i===flowIndex));
        flowIndex=(flowIndex+1)%flow.length;
      };
      rotateFlow();
      setInterval(rotateFlow,1150);

      const observer=new IntersectionObserver(entries=>{
        entries.forEach(entry=>{
          if(entry.isIntersecting){
            gsap.fromTo(entry.target,{opacity:0,y:38},{opacity:1,y:0,duration:.7,ease:'power3.out'});
            observer.unobserve(entry.target);
          }
        });
      },{threshold:.18});
      observer.observe(intro.querySelector('.ct-nocode-grid'));
    }else{
      steps[3]?.classList.add('active');
      flow[0]?.classList.add('on');
    }

    let guardCount=0;
    const guard=setInterval(()=>{
      removeLegacy();
      guardCount+=1;
      if(guardCount>20)clearInterval(guard);
    },500);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});
  else mount();
})();