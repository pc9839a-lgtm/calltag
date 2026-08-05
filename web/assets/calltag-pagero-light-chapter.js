(()=>{
  if(document.documentElement.dataset.ctPageroLightChapter)return;
  document.documentElement.dataset.ctPageroLightChapter='1';

  const style=document.createElement('style');
  style.dataset.ctPageroLightChapter='1';
  style.textContent=`
    #ct-pagero-intro .ct-v8-hero,
    #ct-pagero-intro .ct-pagero-connect{
      --ct-light-bg:#f7f8fc;
      --ct-light-panel:#ffffff;
      --ct-light-text:#111827;
      --ct-light-muted:#5f6b7d;
      --ct-light-line:#dce3ee;
      color:var(--ct-light-text)!important;
      border-color:var(--ct-light-line)!important;
    }

    #ct-pagero-intro .ct-v8-hero{
      padding-top:142px!important;
      border-top:12px solid #315ddd!important;
      border-bottom:1px solid var(--ct-light-line)!important;
      background:
        radial-gradient(circle at 50% -8%,rgba(70,105,235,.18),transparent 38%),
        linear-gradient(180deg,#ffffff 0%,#f5f7fc 78%,#eef2f8 100%)!important;
      box-shadow:inset 0 1px 0 rgba(255,255,255,.9)!important;
    }
    #ct-pagero-intro .ct-v8-head>p{
      display:inline-flex!important;
      align-items:center!important;
      justify-content:center!important;
      min-height:34px!important;
      padding:0 15px!important;
      border:1px solid #cbd7ff!important;
      border-radius:999px!important;
      background:#edf2ff!important;
      color:#315ddd!important;
      box-shadow:0 8px 24px rgba(49,93,221,.1)!important;
    }
    #ct-pagero-intro .ct-v8-head h1{
      color:var(--ct-light-text)!important;
      text-shadow:none!important;
    }
    #ct-pagero-intro .ct-v8-head h1 span,
    #ct-pagero-intro .ct-v8-head>strong em{
      color:#315ddd!important;
    }
    #ct-pagero-intro .ct-v8-head>strong{
      color:var(--ct-light-muted)!important;
    }
    #ct-pagero-intro .ct-v8-stage{
      border:1px solid #d6deeb!important;
      background:rgba(255,255,255,.88)!important;
      box-shadow:0 34px 90px rgba(31,48,82,.15)!important;
      backdrop-filter:blur(16px)!important;
    }
    #ct-pagero-intro .ct-v8-step{
      color:#263248!important;
    }
    #ct-pagero-intro .ct-v8-transfer>small{
      color:#657187!important;
    }
    #ct-pagero-intro .ct-v8-transfer>b{
      border-color:#b9c9ff!important;
      background:#ffffff!important;
      color:#315ddd!important;
      box-shadow:0 0 0 8px rgba(49,93,221,.08),0 12px 28px rgba(49,93,221,.14)!important;
    }
    #ct-pagero-intro .ct-v8-inquiry,
    #ct-pagero-intro .ct-v8-phone,
    #ct-pagero-intro .ct-v8-inquiry h2,
    #ct-pagero-intro .ct-v8-title b,
    #ct-pagero-intro .ct-v8-appcard,
    #ct-pagero-intro .ct-v8-msg strong{
      color:#ffffff!important;
    }

    #ct-pagero-intro .ct-pagero-connect{
      position:relative!important;
      padding-top:120px!important;
      padding-bottom:132px!important;
      border-top:1px solid #dce3ee!important;
      border-bottom:12px solid #315ddd!important;
      background:
        radial-gradient(circle at 18% 15%,rgba(49,93,221,.11),transparent 27%),
        radial-gradient(circle at 82% 72%,rgba(112,145,255,.12),transparent 30%),
        linear-gradient(180deg,#eef2f8 0%,#ffffff 48%,#f5f7fb 100%)!important;
      color:var(--ct-light-text)!important;
    }
    #ct-pagero-intro .ct-pagero-connect:before{
      content:'PAGERO × CALLTAG';
      display:flex;
      align-items:center;
      justify-content:center;
      width:max-content;
      min-height:34px;
      margin:0 auto 22px;
      padding:0 15px;
      border:1px solid #cbd7ff;
      border-radius:999px;
      background:#ffffff;
      color:#315ddd;
      font-size:12px;
      font-weight:950;
      letter-spacing:.1em;
      box-shadow:0 8px 24px rgba(49,93,221,.09);
    }
    #ct-pagero-intro .ct-pagero-connect h2{
      color:var(--ct-light-text)!important;
      text-shadow:none!important;
    }
    #ct-pagero-intro .ct-pagero-connect h2 span{
      color:#315ddd!important;
    }
    #ct-pagero-intro .ct-pagero-connect p,
    #ct-pagero-intro .ct-pagero-connect>div>strong,
    #ct-pagero-intro .ct-pagero-connect .ct-connect-copy strong{
      color:var(--ct-light-muted)!important;
    }
    #ct-pagero-intro .ct-connect-demo{
      padding:34px 30px 38px!important;
      border:1px solid #d6deeb!important;
      border-radius:30px!important;
      background:rgba(255,255,255,.8)!important;
      box-shadow:0 32px 84px rgba(31,48,82,.14)!important;
      backdrop-filter:blur(14px)!important;
    }
    #ct-pagero-intro .ct-connect-cap{
      color:#263248!important;
    }
    #ct-pagero-intro .ct-connect-device{
      box-shadow:0 26px 64px rgba(28,41,70,.25)!important;
    }
    #ct-pagero-intro .ct-connect-arrow>b{
      border-color:#b9c9ff!important;
      background:#ffffff!important;
      color:#315ddd!important;
      box-shadow:0 0 0 8px rgba(49,93,221,.08),0 12px 28px rgba(49,93,221,.14)!important;
    }
    #ct-pagero-intro .ct-connect-arrow small{
      color:#667287!important;
    }
    #ct-pagero-intro .ct-pagero-bottom-cta>small{
      color:#59667a!important;
    }

    @media(max-width:760px){
      #ct-pagero-intro .ct-v8-hero{
        padding-top:104px!important;
        border-top-width:8px!important;
      }
      #ct-pagero-intro .ct-pagero-connect{
        padding-top:86px!important;
        padding-bottom:96px!important;
        border-bottom-width:8px!important;
      }
      #ct-pagero-intro .ct-connect-demo{
        padding:24px 14px 28px!important;
        border-radius:24px!important;
      }
    }

    @media(prefers-reduced-motion:reduce){
      #ct-pagero-intro .ct-v8-stage,
      #ct-pagero-intro .ct-connect-demo{backdrop-filter:none!important}
    }
  `;
  document.head.append(style);
})();
