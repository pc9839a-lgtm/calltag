(()=>{
  if(document.documentElement.dataset.ctPageroBridge)return;
  document.documentElement.dataset.ctPageroBridge='1';

  const mount=()=>{
    if(document.querySelector('#pagero-bridge'))return;
    const anchor=document.querySelector('#strengths')||document.querySelector('#targets')||document.querySelector('#pricing');
    if(!anchor)return;

    const section=document.createElement('section');
    section.id='pagero-bridge';
    section.className='ct-pagero-bridge';
    section.dataset.step='1';
    section.innerHTML=`
      <div class="ct-bridge-wrap">
        <div class="ct-bridge-copy">
          <p class="ct-bridge-kicker">PAGERO × CALLTAG</p>
          <h2>고객을 받는 페이지로.<br><span>놓치지 않는 콜태그.</span></h2>
          <p class="ct-bridge-desc">페이지로에서 접수된 고객이 콜태그에 등록되고,<br>전화·문자·재연락까지 바로 이어집니다.</p>
          <div class="ct-bridge-flow" aria-label="페이지로와 콜태그 연결 과정">
            <button type="button" data-step="1"><b>01</b><span>랜딩 제작</span></button>
            <i>→</i>
            <button type="button" data-step="2"><b>02</b><span>고객 접수</span></button>
            <i>→</i>
            <button type="button" data-step="3"><b>03</b><span>자동 등록</span></button>
            <i>→</i>
            <button type="button" data-step="4"><b>04</b><span>후속관리</span></button>
          </div>
          <a class="ct-bridge-link" href="https://pagero.kr/app">페이지로 알아보기 <span>→</span></a>
        </div>

        <div class="ct-bridge-demo" aria-label="페이지로 고객 접수와 콜태그 연동 예시">
          <div class="ct-bridge-windowbar"><i></i><i></i><i></i><strong>고객 접수부터 후속관리까지</strong></div>
          <div class="ct-bridge-stage">
            <article class="ct-bridge-pagero">
              <header><span>PAGERO</span><b>광고 랜딩</b></header>
              <div class="ct-landing-hero"><small>무료 상담 신청</small><strong>고객관리를<br>더 간단하게.</strong></div>
              <div class="ct-form-field"><span>이름</span><b>김민수</b></div>
              <div class="ct-form-field"><span>연락처</span><b>010-1234-5678</b></div>
              <div class="ct-form-submit"><span>상담 신청</span><b>접수 완료 ✓</b></div>
            </article>

            <div class="ct-bridge-transfer" aria-hidden="true"><span></span><b>→</b></div>

            <article class="ct-bridge-calltag">
              <header><span>CALLTAG</span><b>신규 고객</b></header>
              <div class="ct-customer-head"><i>김</i><div><strong>김민수 고객</strong><span>유입 · 페이지로</span></div></div>
              <div class="ct-customer-row"><span>상담 상태</span><b>신규 문의</b></div>
              <div class="ct-customer-row"><span>다음 연락</span><b>오늘 오후 3:00</b></div>
              <div class="ct-bridge-result message"><i>✓</i><span><b>자동문자 발송 완료</b><small>접수 안내 템플릿 적용</small></span></div>
              <div class="ct-bridge-result task"><i>✓</i><span><b>재연락 일정 저장</b><small>오늘 할 일에 자동 표시</small></span></div>
            </article>
          </div>
          <div class="ct-bridge-progress"><span></span></div>
        </div>
      </div>`;

    anchor.parentNode.insertBefore(section,anchor);

    if(!document.querySelector('style[data-ct-pagero-bridge]')){
      const style=document.createElement('style');
      style.dataset.ctPageroBridge='1';
      style.textContent=`
        .ct-pagero-bridge{position:relative;overflow:hidden;padding:140px 0;background:#090b10;color:#f7f8fc}
        .ct-pagero-bridge:before{content:'';position:absolute;inset:8% -10% auto 36%;height:520px;background:radial-gradient(circle,rgba(71,104,255,.17),transparent 66%);pointer-events:none}
        .ct-bridge-wrap{position:relative;z-index:1;display:grid;grid-template-columns:minmax(360px,.82fr) minmax(620px,1.38fr);align-items:center;gap:70px;width:min(1420px,calc(100% - 72px));margin:0 auto}
        .ct-bridge-kicker{margin:0 0 20px;color:#7897ff;font-size:12px;font-weight:850;letter-spacing:.08em}
        .ct-bridge-copy h2{margin:0;color:#f7f8fc;font-size:clamp(52px,5.2vw,82px);line-height:.98;letter-spacing:-.078em}
        .ct-bridge-copy h2 span{color:#7594ff}
        .ct-bridge-desc{margin:28px 0 0;color:#b8bfcc;font-size:17px;line-height:1.72;letter-spacing:-.025em}
        .ct-bridge-flow{display:flex;align-items:center;gap:9px;margin-top:34px}
        .ct-bridge-flow button{appearance:none;min-width:84px;padding:0;border:0;background:none;color:#697180;text-align:left;cursor:pointer;transition:color .3s ease,transform .3s ease}
        .ct-bridge-flow button b{display:block;margin-bottom:6px;color:#637ecc;font-size:10px;font-weight:850}
        .ct-bridge-flow button span{font-size:13px;font-weight:720;white-space:nowrap}
        .ct-bridge-flow i{color:#3d4552;font-size:13px;font-style:normal}
        .ct-pagero-bridge[data-step='1'] .ct-bridge-flow [data-step='1'],.ct-pagero-bridge[data-step='2'] .ct-bridge-flow [data-step='2'],.ct-pagero-bridge[data-step='3'] .ct-bridge-flow [data-step='3'],.ct-pagero-bridge[data-step='4'] .ct-bridge-flow [data-step='4']{color:#fff;transform:translateY(-3px)}
        .ct-bridge-link{display:inline-flex;align-items:center;gap:13px;margin-top:38px;padding:15px 19px;border:1px solid rgba(117,148,255,.48);border-radius:13px;color:#f7f8fc;font-size:14px;font-weight:780;text-decoration:none;transition:background .25s ease,border-color .25s ease}
        .ct-bridge-link:hover{border-color:#7594ff;background:rgba(117,148,255,.1)}
        .ct-bridge-link span{color:#7594ff;font-size:18px}

        .ct-bridge-demo{position:relative;overflow:hidden;border:1px solid rgba(117,148,255,.4);border-radius:30px;background:linear-gradient(145deg,#151922,#0f1218);box-shadow:0 36px 100px rgba(0,0,0,.35),0 0 80px rgba(67,100,255,.09)}
        .ct-bridge-windowbar{height:52px;display:flex;align-items:center;gap:8px;padding:0 22px;border-bottom:1px solid rgba(255,255,255,.09);color:#747d8d;font-size:11px}
        .ct-bridge-windowbar i{width:8px;height:8px;border-radius:50%;background:#525a67}
        .ct-bridge-windowbar strong{margin-left:auto;color:#7f8898;font-size:10px;font-weight:700}
        .ct-bridge-stage{position:relative;display:grid;grid-template-columns:1fr 64px 1fr;align-items:center;gap:10px;min-height:520px;padding:38px}
        .ct-bridge-pagero,.ct-bridge-calltag{position:relative;min-height:420px;padding:25px;border:1px solid rgba(255,255,255,.11);border-radius:21px;background:#12161e;transition:opacity .45s ease,transform .55s cubic-bezier(.2,.8,.2,1),border-color .4s ease,box-shadow .4s ease}
        .ct-bridge-pagero header,.ct-bridge-calltag header{display:flex;align-items:center;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid rgba(255,255,255,.08)}
        .ct-bridge-pagero header span,.ct-bridge-calltag header span{color:#7897ff;font-size:10px;font-weight:900;letter-spacing:.07em}
        .ct-bridge-pagero header b,.ct-bridge-calltag header b{color:#8e96a5;font-size:10px;font-weight:700}
        .ct-landing-hero{padding:26px 0 22px}
        .ct-landing-hero small{display:block;color:#7897ff;font-size:10px;font-weight:850}
        .ct-landing-hero strong{display:block;margin-top:10px;color:#f4f6fb;font-size:27px;line-height:1.08;letter-spacing:-.055em}
        .ct-form-field{display:flex;align-items:center;justify-content:space-between;height:48px;margin-top:10px;padding:0 14px;border:1px solid rgba(255,255,255,.1);border-radius:11px;background:#0e1218}
        .ct-form-field span{color:#707988;font-size:10px}.ct-form-field b{color:#e9ecf3;font-size:12px;font-weight:700;opacity:.18;transform:translateY(3px);transition:opacity .45s ease,transform .45s ease}
        .ct-form-submit{position:relative;display:flex;align-items:center;justify-content:center;height:48px;margin-top:13px;border-radius:11px;background:#416fff;color:#fff;font-size:12px;font-weight:800;overflow:hidden}
        .ct-form-submit span,.ct-form-submit b{position:absolute;transition:opacity .35s ease,transform .35s ease}.ct-form-submit b{opacity:0;transform:translateY(12px)}
        .ct-bridge-transfer{position:relative;height:80px;display:flex;align-items:center;justify-content:center;color:#7191ff}
        .ct-bridge-transfer span{position:absolute;width:100%;height:2px;background:rgba(113,145,255,.18);overflow:hidden}
        .ct-bridge-transfer span:after{content:'';position:absolute;top:0;left:-40%;width:40%;height:100%;background:#7191ff;box-shadow:0 0 14px #7191ff}
        .ct-bridge-transfer b{position:relative;z-index:1;width:34px;height:34px;display:grid;place-items:center;border:1px solid rgba(113,145,255,.45);border-radius:50%;background:#121721;font-size:16px}
        .ct-bridge-calltag{opacity:.36;transform:translateX(18px)}
        .ct-customer-head{display:flex;align-items:center;gap:13px;padding:25px 0 21px}
        .ct-customer-head>i{width:45px;height:45px;display:grid;place-items:center;border-radius:50%;background:#1c315f;color:#dce5ff;font-size:15px;font-style:normal;font-weight:850}
        .ct-customer-head strong,.ct-customer-head span{display:block}.ct-customer-head strong{font-size:16px}.ct-customer-head span{margin-top:5px;color:#7c8594;font-size:10px}
        .ct-customer-row{display:flex;align-items:center;justify-content:space-between;min-height:47px;margin-top:9px;padding:0 13px;border:1px solid rgba(255,255,255,.08);border-radius:10px;background:#0e1218}
        .ct-customer-row span{color:#737d8d;font-size:10px}.ct-customer-row b{color:#dfe4ed;font-size:11px;font-weight:700}
        .ct-bridge-result{display:flex;align-items:center;gap:11px;margin-top:11px;padding:12px;border:1px solid rgba(56,201,145,.26);border-radius:11px;background:rgba(30,126,91,.11);opacity:0;transform:translateY(12px);transition:opacity .45s ease,transform .45s ease}
        .ct-bridge-result>i{width:25px;height:25px;display:grid;place-items:center;border-radius:50%;background:rgba(50,210,146,.18);color:#55dda9;font-size:11px;font-style:normal;font-weight:900}
        .ct-bridge-result span b,.ct-bridge-result span small{display:block}.ct-bridge-result span b{color:#dff8ed;font-size:10px}.ct-bridge-result span small{margin-top:4px;color:#769b8b;font-size:9px}
        .ct-bridge-progress{height:3px;background:rgba(255,255,255,.06)}
        .ct-bridge-progress span{display:block;width:25%;height:100%;background:#7191ff;box-shadow:0 0 16px rgba(113,145,255,.65);transition:width .55s ease}

        .ct-pagero-bridge[data-step='1'] .ct-bridge-pagero{border-color:rgba(117,148,255,.42);box-shadow:0 0 0 1px rgba(117,148,255,.05)}
        .ct-pagero-bridge[data-step='1'] .ct-form-field:nth-of-type(1) b{opacity:1;transform:none}
        .ct-pagero-bridge[data-step='2'] .ct-form-field b,.ct-pagero-bridge[data-step='3'] .ct-form-field b,.ct-pagero-bridge[data-step='4'] .ct-form-field b{opacity:1;transform:none}
        .ct-pagero-bridge[data-step='2'] .ct-form-submit span,.ct-pagero-bridge[data-step='3'] .ct-form-submit span,.ct-pagero-bridge[data-step='4'] .ct-form-submit span{opacity:0;transform:translateY(-12px)}
        .ct-pagero-bridge[data-step='2'] .ct-form-submit b,.ct-pagero-bridge[data-step='3'] .ct-form-submit b,.ct-pagero-bridge[data-step='4'] .ct-form-submit b{opacity:1;transform:none}
        .ct-pagero-bridge[data-step='2'] .ct-bridge-transfer span:after,.ct-pagero-bridge[data-step='3'] .ct-bridge-transfer span:after,.ct-pagero-bridge[data-step='4'] .ct-bridge-transfer span:after{animation:ctBridgeMove 1s linear infinite}
        .ct-pagero-bridge[data-step='2'] .ct-bridge-calltag,.ct-pagero-bridge[data-step='3'] .ct-bridge-calltag,.ct-pagero-bridge[data-step='4'] .ct-bridge-calltag{opacity:1;transform:none;border-color:rgba(117,148,255,.42)}
        .ct-pagero-bridge[data-step='3'] .ct-bridge-result.message,.ct-pagero-bridge[data-step='4'] .ct-bridge-result.message{opacity:1;transform:none}
        .ct-pagero-bridge[data-step='4'] .ct-bridge-result.task{opacity:1;transform:none}
        .ct-pagero-bridge[data-step='1'] .ct-bridge-progress span{width:25%}.ct-pagero-bridge[data-step='2'] .ct-bridge-progress span{width:50%}.ct-pagero-bridge[data-step='3'] .ct-bridge-progress span{width:75%}.ct-pagero-bridge[data-step='4'] .ct-bridge-progress span{width:100%}
        @keyframes ctBridgeMove{to{left:100%}}

        @media(max-width:1180px){
          .ct-bridge-wrap{grid-template-columns:1fr;gap:58px;width:min(900px,calc(100% - 48px))}.ct-bridge-copy{text-align:center}.ct-bridge-desc br{display:none}.ct-bridge-flow{justify-content:center}.ct-bridge-stage{min-height:500px}
        }
        @media(max-width:720px){
          .ct-pagero-bridge{padding:94px 0}.ct-pagero-bridge:before{left:0;right:-70%;top:20%;height:380px}.ct-bridge-wrap{width:calc(100% - 32px);gap:40px}.ct-bridge-copy h2{font-size:clamp(40px,11.5vw,58px);line-height:1.02}.ct-bridge-desc{margin-top:22px;font-size:14px;line-height:1.65}.ct-bridge-flow{display:grid;grid-template-columns:1fr 14px 1fr;gap:11px 5px;margin-top:28px}.ct-bridge-flow button{text-align:center;min-width:0}.ct-bridge-flow i:nth-of-type(2){display:none}.ct-bridge-link{margin-top:29px}
          .ct-bridge-demo{border-radius:22px}.ct-bridge-windowbar{height:45px;padding:0 16px}.ct-bridge-windowbar strong{display:none}.ct-bridge-stage{display:flex;flex-direction:column;min-height:0;padding:20px;gap:15px}.ct-bridge-pagero,.ct-bridge-calltag{width:100%;min-height:0;box-sizing:border-box;padding:21px}.ct-bridge-transfer{width:60px;height:38px;transform:rotate(90deg)}.ct-landing-hero{padding:21px 0 17px}.ct-landing-hero strong{font-size:24px}.ct-bridge-calltag{transform:translateY(14px)}.ct-bridge-result{padding:11px}.ct-bridge-progress{margin-top:2px}
        }
        @media(prefers-reduced-motion:reduce){.ct-bridge-transfer span:after{animation:none!important}.ct-bridge-pagero,.ct-bridge-calltag,.ct-bridge-result,.ct-form-field b,.ct-form-submit span,.ct-form-submit b,.ct-bridge-progress span{transition:none!important}}
      `;
      document.head.append(style);
    }

    const reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    let step=reduced?4:1;
    let timer=null;
    const setStep=(next)=>{step=next;section.dataset.step=String(next)};
    const stop=()=>{if(timer){clearInterval(timer);timer=null}};
    const start=()=>{
      if(reduced||timer)return;
      timer=setInterval(()=>setStep(step===4?1:step+1),1750);
    };
    section.querySelectorAll('.ct-bridge-flow button').forEach(button=>{
      button.addEventListener('pointerenter',()=>{stop();setStep(Number(button.dataset.step))});
      button.addEventListener('focus',()=>{stop();setStep(Number(button.dataset.step))});
      button.addEventListener('click',()=>setStep(Number(button.dataset.step)));
    });
    section.addEventListener('pointerleave',start);
    section.addEventListener('focusout',e=>{if(!section.contains(e.relatedTarget))start()});
    if('IntersectionObserver'in window){
      const observer=new IntersectionObserver(entries=>entries.forEach(entry=>entry.isIntersecting?start():stop()),{threshold:.2});
      observer.observe(section);
    }else start();
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(mount),{once:true});
  else requestAnimationFrame(mount);
})();