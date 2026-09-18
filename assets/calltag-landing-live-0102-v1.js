(()=>{
  if(document.documentElement.dataset.ctLanding0104V2)return;
  document.documentElement.dataset.ctLanding0104V2='1';

  const APP='https://pagero.kr/app';
  const TITLE='전화가 끝난 뒤,<br><span>고객관리는 시작됩니다.</span>';
  const DESCRIPTION='고객 · 상담 · 다음 할 일.';
  const reduced=window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
  let applying=false,queued=false;

  const installStyle=()=>{
    let style=document.getElementById('ct-live-0102-style');
    if(!style){style=document.createElement('style');style.id='ct-live-0102-style';document.head.append(style);}
    style.textContent=`
      #app.hero-app{padding-bottom:220px!important}
      .ct-live-hero-actions{display:flex;justify-content:center;align-items:center;margin-top:38px}
      .ct-live-hero-primary{min-height:62px;display:inline-flex;align-items:center;justify-content:center;padding:0 34px;border-radius:15px;background:#3b6fff;color:#fff!important;font-size:17px;font-weight:900;text-decoration:none;box-shadow:0 18px 44px rgba(59,111,255,.24);transition:transform .28s cubic-bezier(.2,.8,.2,1),box-shadow .28s ease,background .28s ease}
      .ct-live-hero-primary:hover,.ct-live-hero-primary:focus-visible{transform:translateY(-3px);background:#527dff;box-shadow:0 24px 58px rgba(59,111,255,.31);outline:none}
      .ct-live-hero-link{display:none!important}
      #app .hero-heading.ct-live-hero-enter h1{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) both}
      #app .hero-heading.ct-live-hero-enter>p:last-of-type{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) .12s both}
      #app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:ctLiveHeroUp .88s cubic-bezier(.18,.82,.22,1) .24s both}

      #ct-live-pain{position:relative;overflow:hidden;padding:230px 0 250px!important;min-height:980px;display:flex!important;align-items:center;border-top:1px solid var(--line);border-bottom:1px solid var(--line);background:#0d0f13}
      #ct-live-pain:before{content:"";position:absolute;width:780px;height:780px;left:50%;top:-520px;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.13),rgba(59,111,255,0) 68%);pointer-events:none}
      #ct-live-pain .wrap{position:relative;z-index:1;width:min(1360px,calc(100% - 96px))}
      #ct-live-pain .ct-live-pain-head{text-align:center;opacity:0;transform:translateY(34px);transition:opacity .78s ease,transform .86s cubic-bezier(.18,.82,.22,1)}
      #ct-live-pain h2{margin:0;font-size:clamp(58px,6.45vw,94px);line-height:.98;letter-spacing:-.082em}
      #ct-live-pain h2 span{color:var(--blue-2)}
      #ct-live-pain .ct-live-pain-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;margin-top:104px}
      #ct-live-pain .ct-live-pain-card{min-height:220px;display:grid;place-items:center;padding:32px;border:1px solid var(--line);border-radius:24px;background:linear-gradient(155deg,#171a21,#111319);text-align:center;opacity:0;transform:translateY(70px) scale(.965);filter:blur(6px);transition:opacity .72s ease,transform .86s cubic-bezier(.18,.82,.22,1),filter .72s ease,border-color .32s ease,box-shadow .32s ease,background .32s ease}
      #ct-live-pain .ct-live-pain-card:nth-child(1){transition-delay:.10s}
      #ct-live-pain .ct-live-pain-card:nth-child(2){transition-delay:.20s}
      #ct-live-pain .ct-live-pain-card:nth-child(3){transition-delay:.30s}
      #ct-live-pain .ct-live-pain-card b{font-size:clamp(32px,3.15vw,48px);line-height:1.05;letter-spacing:-.065em}
      #ct-live-pain.is-visible .ct-live-pain-head,#ct-live-pain.is-visible .ct-live-pain-card{opacity:1;transform:none;filter:none}
      #ct-live-pain .ct-live-pain-card:hover{transform:translateY(-7px);border-color:rgba(124,153,255,.45);background:linear-gradient(155deg,rgba(59,111,255,.13),#12151c 58%);box-shadow:0 24px 58px rgba(0,0,0,.28)}

      /* SECTION 03 — 통화 종료 후 10초 시연 */
      #how.ct-live-story-v1{position:relative!important;overflow:visible!important;padding:230px 0 190px!important;background:#090a0d!important;border-top:1px solid var(--line)!important;border-bottom:1px solid var(--line)!important}
      #how.ct-live-story-v1:before{content:"";position:absolute;width:980px;height:980px;right:-460px;top:120px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.14),rgba(59,111,255,0) 67%);pointer-events:none}
      #how.ct-live-story-v1 .ct-story-layout{position:relative;z-index:1;width:min(1460px,calc(100% - 96px))!important;max-width:none!important;display:grid!important;grid-template-columns:minmax(360px,.72fr) minmax(620px,1.28fr)!important;gap:110px!important;align-items:start!important}
      #how.ct-live-story-v1 .ct-story-sticky{position:sticky!important;top:132px!important;align-self:start!important;padding:0!important}
      #how.ct-live-story-v1 .ct-story-sticky>p,#how.ct-live-story-v1 .ct-story-current{display:none!important}
      #how.ct-live-story-v1 .ct-story-sticky h2{margin:0!important;font-size:clamp(64px,6vw,92px)!important;line-height:.96!important;letter-spacing:-.085em!important}
      #how.ct-live-story-v1 .ct-story-sticky h2 span{color:var(--blue-2)!important}
      #how.ct-live-story-v1 .ct-story-status{display:flex!important;align-items:baseline!important;gap:10px!important;margin-top:56px!important;color:#69707d!important}
      #how.ct-live-story-v1 .ct-story-status strong{color:#8ea6ff!important;font-size:56px!important;line-height:1!important;letter-spacing:-.07em!important}
      #how.ct-live-story-v1 .ct-story-status span{font-size:20px!important;font-weight:850!important}
      #how.ct-live-story-v1 .ct-story-steps{display:grid!important;gap:132px!important;min-width:0!important}
      #how.ct-live-story-v1 .ct-story-step{min-height:760px!important;display:flex!important;flex-direction:column!important;justify-content:center!important;padding:68px!important;border:1px solid rgba(255,255,255,.12)!important;border-radius:32px!important;background:linear-gradient(155deg,#151820,#0f1117)!important;opacity:.40!important;transform:translateY(62px) scale(.972)!important;filter:saturate(.72) blur(2px)!important;transition:opacity .64s ease,transform .82s cubic-bezier(.18,.82,.22,1),filter .64s ease,border-color .38s ease,box-shadow .38s ease,background .38s ease!important}
      #how.ct-live-story-v1 .ct-story-step.is-active{opacity:1!important;transform:none!important;filter:none!important;border-color:rgba(94,130,255,.58)!important;background:linear-gradient(155deg,rgba(59,111,255,.12),#12151c 54%,#0f1117)!important;box-shadow:0 38px 90px rgba(0,0,0,.32),0 0 0 1px rgba(59,111,255,.08)!important}
      #how.ct-live-story-v1 .ct-story-step[data-number="04"]{display:none!important}
      #how.ct-live-story-v1 .ct-step-label{margin:0 0 28px!important;color:#7595ff!important;font-size:64px!important;font-weight:950!important;line-height:1!important;letter-spacing:-.08em!important}
      #how.ct-live-story-v1 .ct-story-step h3{margin:0!important;color:#f7f8fb!important;font-size:clamp(46px,4.1vw,66px)!important;line-height:1.02!important;letter-spacing:-.07em!important}
      #how.ct-live-story-v1 .ct-story-step h3 span{color:var(--blue-2)!important}
      #how.ct-live-story-v1 .ct-screen{min-height:380px!important;margin-top:60px!important;padding:36px!important;border:1px solid rgba(255,255,255,.11)!important;border-radius:24px!important;background:#0d1016!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.03)!important;overflow:hidden!important}
      #how.ct-live-story-v1 .ct-live-callend-top{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding-bottom:28px;border-bottom:1px solid rgba(255,255,255,.1)}
      #how.ct-live-story-v1 .ct-live-callend-top span{color:#87909e;font-size:16px;font-weight:800}
      #how.ct-live-story-v1 .ct-live-callend-top strong{font-size:30px;letter-spacing:-.045em}
      #how.ct-live-story-v1 .ct-live-callend-row{display:flex;align-items:center;justify-content:space-between;margin-top:30px;padding:28px;border-radius:18px;background:#171b24}
      #how.ct-live-story-v1 .ct-live-callend-row b{font-size:28px;letter-spacing:-.05em}
      #how.ct-live-story-v1 .ct-live-callend-row span{color:#7c99ff;font-size:16px;font-weight:850}
      #how.ct-live-story-v1 .ct-live-callend-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:18px}
      #how.ct-live-story-v1 .ct-live-callend-actions b{min-height:74px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.1);border-radius:16px;background:#161a22;color:#b6bdca;font-size:17px}
      #how.ct-live-story-v1 .ct-live-callend-actions b:first-child{border-color:rgba(59,111,255,.55);background:rgba(59,111,255,.15);color:#fff}
      #how.ct-live-story-v1 .ct-live-choice-grid{display:grid;gap:16px}
      #how.ct-live-story-v1 .ct-live-choice-row{min-height:88px;display:flex;align-items:center;justify-content:space-between;gap:20px;padding:0 24px;border:1px solid rgba(255,255,255,.1);border-radius:17px;background:#171b24}
      #how.ct-live-story-v1 .ct-live-choice-row span{color:#8e96a3;font-size:16px;font-weight:750}
      #how.ct-live-story-v1 .ct-live-choice-row b{font-size:21px;letter-spacing:-.035em}
      #how.ct-live-story-v1 .ct-live-choice-row.is-on{border-color:rgba(59,111,255,.48);background:rgba(59,111,255,.12)}
      #how.ct-live-story-v1 .ct-live-next-date{display:grid;grid-template-columns:1fr auto;gap:24px;align-items:end;padding:30px;border-radius:20px;background:#171b24}
      #how.ct-live-story-v1 .ct-live-next-date span{display:block;color:#8c95a3;font-size:16px;font-weight:800}
      #how.ct-live-story-v1 .ct-live-next-date strong{display:block;margin-top:10px;font-size:34px;letter-spacing:-.055em}
      #how.ct-live-story-v1 .ct-live-next-date b{color:#8ea6ff;font-size:22px}
      #how.ct-live-story-v1 .ct-live-next-task{margin-top:18px;padding:26px 28px;border:1px solid rgba(255,255,255,.1);border-radius:18px;background:#151922}
      #how.ct-live-story-v1 .ct-live-next-task span{display:block;color:#7f8896;font-size:15px;font-weight:800}
      #how.ct-live-story-v1 .ct-live-next-task b{display:block;margin-top:9px;font-size:24px;letter-spacing:-.04em}
      #how.ct-live-story-v1 .ct-live-save{min-height:68px;display:grid;place-items:center;margin-top:18px;border-radius:16px;background:#3b6fff;color:#fff;font-size:18px;font-weight:950;box-shadow:0 16px 36px rgba(59,111,255,.23)}
      #how.ct-live-story-v1 .ct-live-step-finish{display:flex;align-items:center;justify-content:space-between;gap:24px;margin-top:26px;padding:24px 28px;border-top:1px solid rgba(255,255,255,.1);color:#dce2ef}
      #how.ct-live-story-v1 .ct-live-step-finish strong{color:var(--blue-2);font-size:30px;line-height:1;letter-spacing:-.055em}
      #how.ct-live-story-v1 .ct-live-step-finish span{font-size:18px;font-weight:900;letter-spacing:-.035em}
      #how.ct-live-story-v1 .ct-live-story-result{display:none!important}

      /* SECTION 04 — 문의 유입 → 앱 알림 → 앱 관리 */
      #ct-live-integrations{position:relative;overflow:hidden;padding:230px 0 250px!important;border-top:1px solid var(--line);border-bottom:1px solid var(--line);background:#0d0f13}
      #ct-live-integrations:before{content:"";position:absolute;width:920px;height:920px;left:50%;top:-520px;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.16),rgba(59,111,255,0) 69%);pointer-events:none}
      #ct-live-integrations .ct-int-wrap{position:relative;z-index:1;width:min(1460px,calc(100% - 96px));margin:0 auto}
      #ct-live-integrations .ct-int-head{text-align:center;opacity:0;transform:translateY(38px);transition:opacity .72s ease,transform .82s cubic-bezier(.18,.82,.22,1)}
      #ct-live-integrations h2{margin:0;font-size:clamp(58px,6.2vw,92px);line-height:.98;letter-spacing:-.082em}
      #ct-live-integrations h2 span{color:var(--blue-2)}
      #ct-live-integrations .ct-int-sub{margin:34px auto 0;color:#a2a9b6;font-size:clamp(18px,1.55vw,22px);font-weight:750;line-height:1.45;letter-spacing:-.03em}
      #ct-live-integrations .ct-int-flow{display:grid;grid-template-columns:minmax(270px,.82fr) 72px minmax(420px,1.18fr) 72px minmax(310px,.92fr);align-items:stretch;margin-top:116px}
      #ct-live-integrations .ct-int-arrow{display:grid;place-items:center;color:#6f7d9f;font-size:54px;font-weight:300;opacity:0;transform:translateX(-18px);transition:opacity .56s ease .36s,transform .72s cubic-bezier(.18,.82,.22,1) .36s}
      #ct-live-integrations .ct-source-slider,#ct-live-integrations .ct-app-alert,#ct-live-integrations .ct-app-manage{min-height:380px;border-radius:30px;opacity:0;transform:translateY(44px) scale(.97);transition:opacity .65s ease,transform .8s cubic-bezier(.18,.82,.22,1),border-color .32s ease,box-shadow .32s ease}
      #ct-live-integrations .ct-source-slider{display:flex;flex-direction:column;justify-content:space-between;padding:42px 38px;border:1px solid rgba(255,255,255,.12);background:linear-gradient(155deg,#171a21,#111319)}
      #ct-live-integrations .ct-source-label{color:#7f8998;font-size:15px;font-weight:900;letter-spacing:.05em}
      #ct-live-integrations .ct-source-copy{margin:auto 0}
      #ct-live-integrations .ct-source-copy strong{display:block;color:#fff;font-size:clamp(34px,3vw,48px);line-height:1.02;letter-spacing:-.065em;transition:opacity .22s ease,transform .28s ease}
      #ct-live-integrations .ct-source-copy span{display:block;margin-top:16px;color:#8f98a7;font-size:18px;font-weight:800;transition:opacity .22s ease,transform .28s ease}
      #ct-live-integrations .ct-source-slider.is-changing .ct-source-copy strong,#ct-live-integrations .ct-source-slider.is-changing .ct-source-copy span{opacity:0;transform:translateY(10px)}
      #ct-live-integrations .ct-source-dots{display:flex;gap:9px}
      #ct-live-integrations .ct-source-dot{width:8px;height:8px;border-radius:999px;background:#343a46;transition:width .28s ease,background .28s ease}
      #ct-live-integrations .ct-source-dot.is-on{width:28px;background:#6f91ff}
      #ct-live-integrations .ct-app-alert{position:relative;display:flex;flex-direction:column;justify-content:center;padding:40px;border:1px solid rgba(59,111,255,.52);background:radial-gradient(circle at 50% 0,rgba(59,111,255,.24),transparent 56%),#121722;box-shadow:0 30px 82px rgba(59,111,255,.13)}
      #ct-live-integrations .ct-app-alert:before{content:"";position:absolute;inset:18px;border:1px solid rgba(124,153,255,.08);border-radius:22px;pointer-events:none}
      #ct-live-integrations .ct-alert-top{display:flex;align-items:center;gap:14px;color:#93a9ff;font-size:15px;font-weight:950;letter-spacing:.08em}
      #ct-live-integrations .ct-bell{width:42px;height:42px;display:grid;place-items:center;border-radius:13px;background:rgba(59,111,255,.18);box-shadow:0 0 0 1px rgba(124,153,255,.16)}
      #ct-live-integrations .ct-bell svg{width:23px;height:23px;stroke:#9bb0ff}
      #ct-live-integrations.is-visible .ct-bell{animation:ctBellRing 3.2s ease-in-out 1.15s infinite}
      #ct-live-integrations .ct-alert-title{margin-top:34px;color:#fff;font-size:clamp(42px,4.2vw,64px);font-weight:950;line-height:.96;letter-spacing:-.075em}
      #ct-live-integrations .ct-notification{display:flex;align-items:center;justify-content:space-between;gap:18px;margin-top:34px;padding:22px 24px;border:1px solid rgba(255,255,255,.11);border-radius:18px;background:#0d1017}
      #ct-live-integrations .ct-notification-copy strong{display:block;font-size:20px;letter-spacing:-.04em}
      #ct-live-integrations .ct-notification-copy span{display:block;margin-top:7px;color:#8b94a2;font-size:14px;font-weight:750}
      #ct-live-integrations .ct-open-app{flex:0 0 auto;padding:11px 14px;border-radius:12px;background:#3b6fff;color:#fff;font-size:13px;font-weight:900}
      #ct-live-integrations .ct-app-manage{display:flex;flex-direction:column;justify-content:center;padding:42px;border:1px solid rgba(255,255,255,.13);background:linear-gradient(155deg,#171a21,#111319)}
      #ct-live-integrations .ct-app-manage>span{color:#7f8998;font-size:15px;font-weight:900;letter-spacing:.04em}
      #ct-live-integrations .ct-app-manage>strong{display:block;margin-top:18px;color:#fff;font-size:clamp(38px,3.5vw,56px);line-height:.98;letter-spacing:-.07em}
      #ct-live-integrations .ct-manage-actions{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:34px}
      #ct-live-integrations .ct-manage-actions b{min-height:66px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.11);border-radius:15px;background:#11151d;color:#dce2ed;font-size:17px}
      #ct-live-integrations .ct-manage-actions b:first-child{border-color:rgba(59,111,255,.48);background:rgba(59,111,255,.13);color:#fff}
      #ct-live-integrations .ct-int-note{margin:70px 0 0;text-align:center;color:#69717e;font-size:14px;font-weight:700}
      #ct-live-integrations.is-visible .ct-int-head,#ct-live-integrations.is-visible .ct-source-slider,#ct-live-integrations.is-visible .ct-int-arrow,#ct-live-integrations.is-visible .ct-app-alert,#ct-live-integrations.is-visible .ct-app-manage{opacity:1;transform:none}
      #ct-live-integrations .ct-source-slider{transition-delay:.08s}#ct-live-integrations .ct-app-alert{transition-delay:.34s}#ct-live-integrations .ct-app-manage{transition-delay:.58s}
      @keyframes ctBellRing{0%,78%,100%{transform:rotate(0)}82%{transform:rotate(13deg)}86%{transform:rotate(-11deg)}90%{transform:rotate(8deg)}94%{transform:rotate(-5deg)}}

      @media(max-width:1050px){
        #ct-live-integrations .ct-int-flow{grid-template-columns:1fr;gap:0;max-width:760px;margin:90px auto 0}
        #ct-live-integrations .ct-int-arrow{height:76px;transform:rotate(90deg) translateX(-14px)}
        #ct-live-integrations.is-visible .ct-int-arrow{transform:rotate(90deg)}
        #ct-live-integrations .ct-source-slider,#ct-live-integrations .ct-app-alert,#ct-live-integrations .ct-app-manage{min-height:280px}
      }
      @media(max-width:700px){
        #ct-live-integrations{padding:156px 0 170px!important}
        #ct-live-integrations .ct-int-wrap{width:min(100% - 40px,720px)}
        #ct-live-integrations h2{font-size:48px}
        #ct-live-integrations .ct-int-sub{margin-top:26px;font-size:18px}
        #ct-live-integrations .ct-int-flow{margin-top:76px}
        #ct-live-integrations .ct-source-slider,#ct-live-integrations .ct-app-alert,#ct-live-integrations .ct-app-manage{min-height:240px;padding:30px 26px;border-radius:22px}
        #ct-live-integrations .ct-source-copy strong{font-size:38px}
        #ct-live-integrations .ct-source-copy span{font-size:16px}
        #ct-live-integrations .ct-alert-title{margin-top:28px;font-size:44px}
        #ct-live-integrations .ct-notification{margin-top:28px;padding:18px;display:block}
        #ct-live-integrations .ct-open-app{display:inline-flex;margin-top:16px}
        #ct-live-integrations .ct-app-manage>strong{font-size:42px}
        #ct-live-integrations .ct-manage-actions{margin-top:26px}
        #ct-live-integrations .ct-manage-actions b{min-height:58px;font-size:16px}
        #ct-live-integrations .ct-int-arrow{height:64px;font-size:44px}
        #ct-live-integrations .ct-int-note{margin-top:52px;font-size:13px;line-height:1.5}
      }
      @keyframes ctLiveHeroUp{from{opacity:0;transform:translateY(34px)}to{opacity:1;transform:none}}

      @media(max-width:900px){
        #app.hero-app{padding-bottom:150px!important}
        .ct-live-hero-actions{margin-top:30px}
        .ct-live-hero-primary{width:min(100%,340px);min-height:58px;font-size:16px}
        #ct-live-pain{padding:156px 0 172px!important;min-height:0;display:block!important}
        #ct-live-pain .wrap{width:min(100% - 40px,720px)}
        #ct-live-pain h2{font-size:48px}
        #ct-live-pain .ct-live-pain-grid{grid-template-columns:1fr;gap:16px;margin-top:72px}
        #ct-live-pain .ct-live-pain-card{min-height:138px;border-radius:20px}
        #ct-live-pain .ct-live-pain-card b{font-size:32px}

        #how.ct-live-story-v1{padding:156px 0 148px!important}
        #how.ct-live-story-v1 .ct-story-layout{width:min(100% - 40px,720px)!important;display:block!important}
        #how.ct-live-story-v1 .ct-story-sticky{position:relative!important;top:auto!important;margin-bottom:92px!important;text-align:center!important}
        #how.ct-live-story-v1 .ct-story-sticky h2{font-size:52px!important}
        #how.ct-live-story-v1 .ct-story-status{justify-content:center!important;margin-top:40px!important}
        #how.ct-live-story-v1 .ct-story-steps{gap:70px!important}
        #how.ct-live-story-v1 .ct-story-step{min-height:0!important;padding:38px 26px!important;border-radius:24px!important;opacity:1!important;transform:none!important;filter:none!important}
        #how.ct-live-story-v1 .ct-step-label{margin-bottom:22px!important;font-size:48px!important}
        #how.ct-live-story-v1 .ct-story-step h3{font-size:40px!important}
        #how.ct-live-story-v1 .ct-screen{min-height:0!important;margin-top:42px!important;padding:24px!important;border-radius:20px!important}
        #how.ct-live-story-v1 .ct-live-callend-top{display:block!important}
        #how.ct-live-story-v1 .ct-live-callend-top strong{display:block;margin-top:9px;font-size:25px!important}
        #how.ct-live-story-v1 .ct-live-callend-actions{grid-template-columns:1fr!important}
        #how.ct-live-story-v1 .ct-live-callend-actions b{min-height:64px!important;font-size:16px!important}
        #how.ct-live-story-v1 .ct-live-choice-row{min-height:74px!important;padding:0 18px!important}
        #how.ct-live-story-v1 .ct-live-choice-row span{font-size:15px!important}
        #how.ct-live-story-v1 .ct-live-choice-row b{font-size:18px!important}
        #how.ct-live-story-v1 .ct-live-next-date{grid-template-columns:1fr!important;gap:12px!important}
        #how.ct-live-story-v1 .ct-live-next-date strong{font-size:29px!important}
        #how.ct-live-story-v1 .ct-live-step-finish{margin-top:20px;padding:20px 4px 0;gap:14px}
        #how.ct-live-story-v1 .ct-live-step-finish strong{font-size:24px}
        #how.ct-live-story-v1 .ct-live-step-finish span{font-size:16px}
      }

      @media(prefers-reduced-motion:reduce){
        #app .hero-heading.ct-live-hero-enter h1,#app .hero-heading.ct-live-hero-enter>p:last-of-type,#app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:none!important}
        #ct-live-pain .ct-live-pain-head,#ct-live-pain .ct-live-pain-card{opacity:1!important;transform:none!important;filter:none!important;transition:none!important}
        #how.ct-live-story-v1 .ct-story-step{opacity:1!important;transform:none!important;filter:none!important;transition:none!important}
      }
    `;
  };

  const makePain=()=>{const section=document.createElement('section');section.id='ct-live-pain';section.className='ad-section alt ct-live-pain';return section;};
  const fillPain=section=>{const wanted='<div class="wrap"><div class="ct-live-pain-head"><h2>기억으로 관리하면<br><span>놓칩니다.</span></h2></div><div class="ct-live-pain-grid"><div class="ct-live-pain-card"><b>누구였지?</b></div><div class="ct-live-pain-card"><b>언제 다시?</b></div><div class="ct-live-pain-card"><b>보냈나?</b></div></div></div>';if(section.innerHTML!==wanted)section.innerHTML=wanted;};

  const revealPain=section=>{
    if(section.dataset.ctObserved)return;
    section.dataset.ctObserved='1';
    if(reduced||!('IntersectionObserver'in window)){section.classList.add('is-visible');return;}
    const observer=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');observer.unobserve(entry.target);}});},{threshold:.24,rootMargin:'0px 0px -10%'});
    observer.observe(section);
  };

  const findStory=()=>document.querySelector('#how.ct-story-section')||[...document.querySelectorAll('.ct-story-section')].find(section=>{const t=(section.textContent||'').replace(/\s+/g,' ').trim();return t.includes('통화가 끝나면')&&t.includes('태그만 하세요');})||null;

  const patchStory=()=>{
    const pain=document.getElementById('ct-live-pain');
    const story=findStory();
    if(!story)return false;

    story.classList.add('ct-live-story-v1');
    story.setAttribute('aria-label','통화 종료 후 3단계 고객관리 시연');
    if(pain&&pain.nextElementSibling!==story)pain.insertAdjacentElement('afterend',story);

    const sticky=story.querySelector('.ct-story-sticky');
    const stickyTitle=sticky?.querySelector('h2');
    if(stickyTitle&&stickyTitle.dataset.ctPlan03!=='1'){
      stickyTitle.innerHTML='통화가 끝나면,<br><span>3단계면 끝.</span>';
      stickyTitle.dataset.ctPlan03='1';
    }
    const status=sticky?.querySelector('.ct-story-status');
    const total=status?.querySelector('span');
    if(total)total.textContent='/ 03';

    const steps=[...story.querySelectorAll('.ct-story-step')];
    if(steps.length<3)return false;

    const definitions=[
      {
        title:'통화 종료',
        html:`<div class="ct-step-label">01</div><h3>전화를 끊으면<br><span>바로 나타납니다.</span></h3><div class="ct-screen ct-live-callend-screen"><div class="ct-live-callend-top"><span>통화 종료</span><strong>010-4821-7536</strong></div><div class="ct-live-callend-row"><b>김민수</b><span>방금 통화</span></div><div class="ct-live-callend-actions"><b>신규 문의</b><b>기존 고객</b><b>제외</b></div></div>`
      },
      {
        title:'고객 + 상담',
        html:`<div class="ct-step-label">02</div><h3>누구인지,<br><span>상담만 선택.</span></h3><div class="ct-screen"><div class="ct-live-choice-grid"><div class="ct-live-choice-row is-on"><span>고객 구분</span><b>신규 문의</b></div><div class="ct-live-choice-row is-on"><span>상담 유형</span><b>상품 상담</b></div><div class="ct-live-choice-row"><span>상담 상태</span><b>진행 중</b></div></div></div>`
      },
      {
        title:'다음 할 일',
        html:`<div class="ct-step-label">03</div><h3>다시 연락할 날짜까지<br><span>남기면 끝.</span></h3><div class="ct-screen"><div class="ct-live-next-date"><div><span>다음 연락</span><strong>내일 · 오전 10:30</strong></div><b>재연락</b></div><div class="ct-live-next-task"><span>다음 할 일</span><b>견적서 발송</b></div><div class="ct-live-save">저장 완료</div></div><div class="ct-live-step-finish"><strong>약 10초</strong><span>고객관리 완료</span></div>`
      }
    ];

    definitions.forEach((definition,index)=>{
      const step=steps[index];
      step.dataset.number=String(index+1).padStart(2,'0');
      step.dataset.title=definition.title;
      if(step.dataset.ctPlan03!=='1'){
        step.innerHTML=definition.html;
        step.dataset.ctPlan03='1';
      }
    });
    steps.slice(3).forEach(step=>{step.style.setProperty('display','none','important');step.setAttribute('aria-hidden','true');});

    story.querySelectorAll('.ct-live-story-result').forEach(result=>result.remove());
    return true;
  };

  const patchIntegrations=()=>{
    const story=document.querySelector('#how.ct-live-story-v1');
    if(!story)return false;
    let section=document.getElementById('ct-live-integrations');
    if(!section){
      section=document.createElement('section');
      section.id='ct-live-integrations';
      section.setAttribute('aria-label','외부 문의 앱 알림 및 고객관리');
    }
    if(section.dataset.ctV2!=='1'){
      section.dataset.ctV2='1';
      section.innerHTML=`<div class="ct-int-wrap"><div class="ct-int-head"><h2>문의가 들어오면<br><span>콜태그 앱이 바로 알려줍니다.</span></h2><p class="ct-int-sub">어디서 들어온 문의든 앱에서 바로 확인하고 관리하세요.</p></div><div class="ct-int-flow"><div class="ct-source-slider"><span class="ct-source-label">문의 접수</span><div class="ct-source-copy"><strong>Meta Lead Ads</strong><span>광고 리드폼 문의</span></div><div class="ct-source-dots" aria-hidden="true"><i class="ct-source-dot is-on"></i><i class="ct-source-dot"></i><i class="ct-source-dot"></i><i class="ct-source-dot"></i></div></div><div class="ct-int-arrow" aria-hidden="true">→</div><div class="ct-app-alert"><div class="ct-alert-top"><span class="ct-bell"><svg viewBox="0 0 24 24" fill="none" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"></path><path d="M10 21h4"></path></svg></span>CALLTAG APP</div><div class="ct-alert-title">새 문의 도착</div><div class="ct-notification"><div class="ct-notification-copy"><strong>새 고객 문의가 도착했습니다.</strong><span>콜태그 앱에서 바로 확인하세요.</span></div><span class="ct-open-app">앱에서 확인</span></div></div><div class="ct-int-arrow" aria-hidden="true">→</div><div class="ct-app-manage"><span>콜태그 앱</span><strong>바로<br>고객관리</strong><div class="ct-manage-actions"><b>전화</b><b>문자</b><b>일정</b></div></div></div><p class="ct-int-note">연동 방식과 수신 시점은 채널별로 다를 수 있습니다.</p></div>`;
    }
    if(story.nextElementSibling!==section)story.insertAdjacentElement('afterend',section);
    if(!section.dataset.ctObserved){
      section.dataset.ctObserved='1';
      if(reduced||!('IntersectionObserver'in window)){section.classList.add('is-visible');}
      else{
        const io=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(entry.isIntersecting){entry.target.classList.add('is-visible');io.unobserve(entry.target);}});},{threshold:.2,rootMargin:'0px 0px -8%'});
        io.observe(section);
      }
    }
    if(!section.dataset.ctSliderStarted){
      section.dataset.ctSliderStarted='1';
      const sources=[
        ['Meta Lead Ads','광고 리드폼 문의'],
        ['Google Forms','폼 응답 문의'],
        ['PageRo','페이지 문의'],
        ['Webhook','외부 서비스 문의']
      ];
      let index=0;
      const slider=section.querySelector('.ct-source-slider');
      const name=section.querySelector('.ct-source-copy strong');
      const desc=section.querySelector('.ct-source-copy span');
      const dots=[...section.querySelectorAll('.ct-source-dot')];
      if(slider&&name&&desc&&!reduced){
        setInterval(()=>{
          slider.classList.add('is-changing');
          setTimeout(()=>{
            index=(index+1)%sources.length;
            name.textContent=sources[index][0];
            desc.textContent=sources[index][1];
            dots.forEach((dot,i)=>dot.classList.toggle('is-on',i===index));
            slider.classList.remove('is-changing');
          },220);
        },2600);
      }
    }
    return true;
  };

  const apply=()=>{
    queued=false;
    if(applying)return;
    applying=true;
    try{
      installStyle();
      const app=document.querySelector('#app');
      const heading=app?.querySelector('.hero-heading');
      const title=heading?.querySelector('h1');
      const description=heading?.querySelector(':scope > p:last-of-type');
      if(title&&title.innerHTML!==TITLE)title.innerHTML=TITLE;
      if(description&&description.textContent.trim()!==DESCRIPTION)description.textContent=DESCRIPTION;
      if(heading){
        let actions=heading.querySelector('.ct-live-hero-actions');
        if(!actions){actions=document.createElement('div');actions.className='ct-live-hero-actions';heading.append(actions);}
        if(!actions.querySelector('.ct-live-hero-primary'))actions.innerHTML=`<a class="ct-live-hero-primary" href="${APP}" target="_blank" rel="noopener">7일 무료로 시작하기</a>`;
        if(!heading.dataset.ctHeroAnimated){heading.dataset.ctHeroAnimated='1';requestAnimationFrame(()=>heading.classList.add('ct-live-hero-enter'));}
      }
      if(app){
        let pain=document.getElementById('ct-live-pain');
        if(!pain)pain=makePain();
        fillPain(pain);
        if(app.nextElementSibling!==pain)app.insertAdjacentElement('afterend',pain);
        revealPain(pain);
      }
      patchStory();
      patchIntegrations();
    }finally{applying=false;}
  };

  const run=()=>apply();
  let runtimeSeen=false;
  const onReady=()=>{runtimeSeen=true;run();};
  const arm=()=>{
    if(document.documentElement.dataset.ctLayoutCoordinatorV12)onReady();
    else document.addEventListener('calltag:runtime-ready',onReady,{once:true});
    document.addEventListener('calltag:runtime-settled',run,{once:true});
    setTimeout(()=>{if(!runtimeSeen)onReady();},2600);
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',arm,{once:true});else arm();
})();
