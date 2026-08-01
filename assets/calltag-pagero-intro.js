(()=>{
  if(document.documentElement.dataset.ctPageroIntroV2)return;
  document.documentElement.dataset.ctPageroIntroV2='1';

  const mount=()=>{
    const old=document.querySelector('#ct-pagero-intro');
    if(old)old.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-p2-hero">
        <div class="ct-p2-glow"></div>
        <div class="ct-p2-wrap">
          <div class="ct-p2-hero-copy">
            <p>페이지로</p>
            <h1>랜딩 만들고,<br><span>고객 받으세요.</span></h1>
            <strong>광고에 쓸 한 페이지를 만들고, 문의와 상담 신청을 바로 받습니다.</strong>
            <a href="https://pagero.kr/app">페이지로 시작하기 <b>→</b></a>
          </div>

          <div class="ct-p2-result" aria-label="페이지로 랜딩페이지와 고객 접수 예시">
            <div class="ct-p2-bar"><i></i><i></i><i></i><span>내 광고 랜딩페이지</span><em>공개 중</em></div>
            <div class="ct-p2-page">
              <div class="ct-p2-page-copy">
                <small>무료 상담 신청</small>
                <h2>고객이 보고,<br><span>바로 신청합니다.</span></h2>
                <p>서비스를 설명하고 필요한 고객정보를 한 화면에서 받습니다.</p>
              </div>
              <div class="ct-p2-form">
                <header><b>상담 신청</b><span>1분이면 끝</span></header>
                <label><span>이름</span><strong>김민수</strong></label>
                <label><span>연락처</span><strong>010-1234-5678</strong></label>
                <button type="button">상담 신청</button>
              </div>
            </div>
            <div class="ct-p2-newlead"><i>✓</i><span><b>새 고객이 접수됐습니다.</b><small>김민수 · 페이지로</small></span></div>
          </div>
        </div>
      </section>

      <section class="ct-p2-build" id="ct-pg-build">
        <div class="ct-p2-wrap">
          <header class="ct-p2-head">
            <p>페이지 제작</p>
            <h2>고르고 <i>→</i> 바꾸고 <i>→</i> <span>바로 공개</span></h2>
          </header>
          <div class="ct-p2-flow" data-step="1">
            <article data-step="1"><b>01</b><strong>템플릿 선택</strong><span>목적에 맞는 화면을 고릅니다.</span></article>
            <i>→</i>
            <article data-step="2"><b>02</b><strong>내용 수정</strong><span>문구와 이미지만 바꿉니다.</span></article>
            <i>→</i>
            <article data-step="3"><b>03</b><strong>즉시 공개</strong><span>완성된 주소를 바로 광고합니다.</span></article>
          </div>
          <div class="ct-p2-buildline"><span></span><b>템플릿 선택</b><i>완성까지 세 단계</i></div>
        </div>
      </section>

      <section class="ct-p2-sync">
        <div class="ct-p2-wrap">
          <header class="ct-p2-head">
            <p>페이지로 × 콜태그</p>
            <h2>받은 고객은<br><span>바로 관리합니다.</span></h2>
          </header>
          <div class="ct-p2-syncstage">
            <article class="ct-p2-receive">
              <small>PAGERO</small>
              <h3>고객 접수</h3>
              <div><span>고객명</span><b>김민수</b></div>
              <div><span>연락처</span><b>010-1234-5678</b></div>
              <em>접수 완료</em>
            </article>
            <div class="ct-p2-transfer"><span></span><b>→</b><small>자동 등록</small></div>
            <article class="ct-p2-manage">
              <small>CALLTAG</small>
              <h3>신규 고객</h3>
              <div class="ct-p2-person"><i>김</i><span><b>김민수 고객</b><small>유입 · 페이지로</small></span></div>
              <div class="ct-p2-task"><span>자동문자</span><b>발송 완료</b></div>
              <div class="ct-p2-task"><span>다음 연락</span><b>오늘 오후 3:00</b></div>
            </article>
          </div>
          <a class="ct-p2-continue" href="#calltag-start">이어서 콜태그 보기 <b>↓</b></a>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);

    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());
    const style=document.createElement('style');
    style.dataset.ctPageroIntro='2';
    style.textContent=`
      #ct-pagero-intro{background:#080a0f;color:#f7f8fb;overflow:hidden}
      .ct-p2-wrap{width:min(1460px,calc(100% - 72px));margin:0 auto}
      .ct-p2-hero{position:relative;min-height:100vh;padding:132px 0 88px;border-bottom:1px solid rgba(255,255,255,.08);background:linear-gradient(180deg,#090c15,#080a0f)}
      .ct-p2-glow{position:absolute;left:50%;top:-630px;width:1300px;height:1100px;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(65,105,255,.25),transparent 67%);pointer-events:none}
      .ct-p2-hero-copy{position:relative;z-index:1;text-align:center}
      .ct-p2-hero-copy>p,.ct-p2-head>p{margin:0 0 18px;color:#7897ff;font-size:12px;font-weight:850;letter-spacing:.08em}
      .ct-p2-hero-copy h1{margin:0;font-size:clamp(72px,8.1vw,126px);line-height:.9;letter-spacing:-.09em}
      .ct-p2-hero-copy h1 span,.ct-p2-head h2 span{color:#7594ff}
      .ct-p2-hero-copy>strong{display:block;margin:28px auto 0;color:#b8bfcb;font-size:17px;font-weight:550;line-height:1.6}
      .ct-p2-hero-copy>a{display:inline-flex;align-items:center;gap:15px;margin-top:31px;padding:16px 21px;border-radius:13px;background:#416fff;color:#fff;font-size:14px;font-weight:780;text-decoration:none;box-shadow:0 16px 38px rgba(65,111,255,.24)}
      .ct-p2-hero-copy>a b{font-size:18px}
      .ct-p2-result{position:relative;z-index:1;max-width:1180px;margin:70px auto 0;overflow:visible;border:1px solid rgba(117,148,255,.44);border-radius:29px;background:#11151d;box-shadow:0 42px 110px rgba(0,0,0,.42),0 0 100px rgba(65,111,255,.08)}
      .ct-p2-bar{height:52px;display:flex;align-items:center;gap:7px;padding:0 20px;border-bottom:1px solid rgba(255,255,255,.09);background:#171b24;border-radius:29px 29px 0 0}
      .ct-p2-bar i{width:8px;height:8px;border-radius:50%;background:#555e6d}.ct-p2-bar span{margin-left:9px;color:#707988;font-size:10px}.ct-p2-bar em{margin-left:auto;padding:6px 9px;border-radius:999px;background:rgba(53,207,140,.1);color:#58dda3;font-size:9px;font-style:normal;font-weight:800}
      .ct-p2-page{display:grid;grid-template-columns:1.25fr .75fr;min-height:500px;background:radial-gradient(circle at 20% 0,rgba(65,111,255,.13),transparent 38%),#0d1118}
      .ct-p2-page-copy{display:flex;flex-direction:column;justify-content:center;padding:70px 76px}.ct-p2-page-copy small{color:#7897ff;font-size:11px;font-weight:850}.ct-p2-page-copy h2{margin:17px 0 0;font-size:clamp(47px,5vw,76px);line-height:.96;letter-spacing:-.075em}.ct-p2-page-copy h2 span{color:#7897ff}.ct-p2-page-copy p{max-width:480px;margin:23px 0 0;color:#858e9d;font-size:14px;line-height:1.65}
      .ct-p2-form{align-self:center;margin:48px 58px 48px 0;padding:28px;border:1px solid rgba(255,255,255,.12);border-radius:19px;background:#111620}.ct-p2-form header{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.ct-p2-form header b{font-size:18px}.ct-p2-form header span{color:#758091;font-size:10px}.ct-p2-form label{height:56px;display:flex;align-items:center;justify-content:space-between;margin-top:10px;padding:0 15px;border:1px solid rgba(255,255,255,.1);border-radius:11px;background:#0d1117}.ct-p2-form label span{color:#697282;font-size:10px}.ct-p2-form label strong{font-size:12px}.ct-p2-form button{width:100%;height:57px;margin-top:13px;border:0;border-radius:11px;background:#416fff;color:#fff;font-size:12px;font-weight:850}
      .ct-p2-newlead{position:absolute;right:36px;bottom:-31px;display:flex;align-items:center;gap:12px;min-width:245px;padding:15px 18px;border:1px solid rgba(53,207,140,.28);border-radius:14px;background:#102019;box-shadow:0 20px 50px rgba(0,0,0,.38);animation:ctP2Lead 3s ease-in-out infinite}.ct-p2-newlead>i{width:31px;height:31px;display:grid;place-items:center;border-radius:50%;background:rgba(53,207,140,.16);color:#58dda3;font-style:normal;font-weight:900}.ct-p2-newlead b,.ct-p2-newlead small{display:block}.ct-p2-newlead b{font-size:11px}.ct-p2-newlead small{margin-top:4px;color:#6f8d7f;font-size:9px}
      .ct-p2-build,.ct-p2-sync{padding:145px 0;border-bottom:1px solid rgba(255,255,255,.08)}
      .ct-p2-head{text-align:center}.ct-p2-head h2{margin:0;font-size:clamp(54px,6.3vw,92px);line-height:.96;letter-spacing:-.082em}.ct-p2-head h2 i{color:#454d5b;font-style:normal;font-weight:500}
      .ct-p2-flow{display:grid;grid-template-columns:1fr 62px 1fr 62px 1fr;align-items:center;max-width:1220px;margin:68px auto 0}.ct-p2-flow>i{color:#3d4655;font-size:23px;font-style:normal;text-align:center}.ct-p2-flow article{min-height:210px;padding:30px;border:1px solid rgba(255,255,255,.1);border-radius:20px;background:#11151c;opacity:.42;transform:scale(.96);transition:.45s ease}.ct-p2-flow article b{color:#6e87dd;font-size:11px}.ct-p2-flow article strong,.ct-p2-flow article span{display:block}.ct-p2-flow article strong{margin-top:55px;font-size:29px;letter-spacing:-.05em}.ct-p2-flow article span{margin-top:10px;color:#818a99;font-size:12px}.ct-p2-flow[data-step='1'] article[data-step='1'],.ct-p2-flow[data-step='2'] article[data-step='2'],.ct-p2-flow[data-step='3'] article[data-step='3']{opacity:1;transform:scale(1);border-color:rgba(83,123,255,.75);background:rgba(65,111,255,.12);box-shadow:0 18px 55px rgba(65,111,255,.1)}
      .ct-p2-buildline{position:relative;display:flex;align-items:center;justify-content:center;gap:14px;max-width:1220px;height:4px;margin:34px auto 0;background:rgba(255,255,255,.06)}.ct-p2-buildline span{position:absolute;left:0;top:0;height:100%;width:33.33%;background:#7594ff;transition:width .5s ease;box-shadow:0 0 16px rgba(117,148,255,.55)}.ct-p2-buildline b,.ct-p2-buildline i{position:relative;top:28px;font-size:10px}.ct-p2-buildline b{color:#aab8ef}.ct-p2-buildline i{color:#666f7e;font-style:normal}
      .ct-p2-syncstage{display:grid;grid-template-columns:1fr 150px 1fr;align-items:center;max-width:1120px;margin:68px auto 0;padding:38px;border:1px solid rgba(117,148,255,.35);border-radius:28px;background:radial-gradient(circle at 50% 0,rgba(65,111,255,.12),transparent 48%),#10141b}
      .ct-p2-receive,.ct-p2-manage{min-height:370px;padding:30px;border:1px solid rgba(255,255,255,.11);border-radius:20px;background:#121720}.ct-p2-receive>small,.ct-p2-manage>small{color:#7897ff;font-size:10px;font-weight:900;letter-spacing:.08em}.ct-p2-receive h3,.ct-p2-manage h3{margin:16px 0 29px;font-size:34px;letter-spacing:-.06em}.ct-p2-receive>div,.ct-p2-task{height:58px;display:flex;align-items:center;justify-content:space-between;margin-top:10px;padding:0 15px;border:1px solid rgba(255,255,255,.09);border-radius:11px;background:#0d1117}.ct-p2-receive div span,.ct-p2-task span{color:#6d7685;font-size:10px}.ct-p2-receive div b,.ct-p2-task b{font-size:12px}.ct-p2-receive em{display:grid;place-items:center;height:53px;margin-top:14px;border-radius:11px;background:#416fff;color:#fff;font-size:11px;font-style:normal;font-weight:850}
      .ct-p2-transfer{position:relative;text-align:center;color:#7897ff}.ct-p2-transfer>span{position:absolute;left:0;right:0;top:50%;height:2px;background:rgba(117,148,255,.2);overflow:hidden}.ct-p2-transfer>span:after{content:'';position:absolute;left:-42%;width:42%;height:100%;background:#7897ff;box-shadow:0 0 14px #7897ff;animation:ctP2Transfer 1.5s linear infinite}.ct-p2-transfer>b{position:relative;width:42px;height:42px;display:grid;place-items:center;margin:auto;border:1px solid rgba(117,148,255,.5);border-radius:50%;background:#131924;font-size:20px}.ct-p2-transfer small{display:block;margin-top:12px;color:#73809a;font-size:9px}
      .ct-p2-person{display:flex;align-items:center;gap:13px;margin-bottom:17px}.ct-p2-person>i{width:47px;height:47px;display:grid;place-items:center;border-radius:50%;background:rgba(65,111,255,.2);color:#b8c5ff;font-style:normal;font-weight:850}.ct-p2-person b,.ct-p2-person small{display:block}.ct-p2-person b{font-size:15px}.ct-p2-person small{margin-top:5px;color:#707989;font-size:9px}.ct-p2-task{border-color:rgba(53,207,140,.2)}.ct-p2-task b{color:#62dda8}
      .ct-p2-continue{display:flex;align-items:center;justify-content:center;gap:11px;width:max-content;margin:35px auto 0;color:#c9cfda;font-size:13px;font-weight:750;text-decoration:none}.ct-p2-continue b{color:#7897ff;font-size:17px}
      @keyframes ctP2Lead{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}@keyframes ctP2Transfer{to{left:100%}}
      @media(max-width:1050px){.ct-p2-page{grid-template-columns:1fr}.ct-p2-page-copy{text-align:center;padding:60px 34px 30px}.ct-p2-page-copy p{margin-left:auto;margin-right:auto}.ct-p2-form{margin:0 34px 55px}.ct-p2-flow{grid-template-columns:1fr}.ct-p2-flow>i{transform:rotate(90deg);padding:12px}.ct-p2-syncstage{grid-template-columns:1fr}.ct-p2-transfer{height:100px;display:grid;place-items:center}.ct-p2-transfer>span{left:50%;right:auto;top:0;bottom:0;width:2px;height:auto}.ct-p2-transfer>span:after{top:-42%;left:0;width:100%;height:42%;animation:ctP2TransferY 1.5s linear infinite}.ct-p2-transfer>b{transform:rotate(90deg)}@keyframes ctP2TransferY{to{top:100%}}}
      @media(max-width:700px){.ct-p2-wrap{width:calc(100% - 32px)}.ct-p2-hero{min-height:auto;padding:112px 0 72px}.ct-p2-hero-copy h1{font-size:55px}.ct-p2-hero-copy>strong{font-size:15px}.ct-p2-result{margin-top:48px;border-radius:21px}.ct-p2-page-copy{padding:44px 21px 25px}.ct-p2-page-copy h2{font-size:41px}.ct-p2-form{margin:0 17px 42px;padding:21px}.ct-p2-newlead{position:relative;right:auto;bottom:auto;min-width:0;margin:0 17px 18px}.ct-p2-build,.ct-p2-sync{padding:92px 0}.ct-p2-head h2{font-size:47px}.ct-p2-head h2 i{display:none}.ct-p2-flow{margin-top:43px}.ct-p2-flow article{min-height:170px;padding:24px}.ct-p2-flow article strong{margin-top:35px;font-size:26px}.ct-p2-buildline{margin-top:25px}.ct-p2-syncstage{margin-top:45px;padding:16px;border-radius:21px}.ct-p2-receive,.ct-p2-manage{min-height:0;padding:23px}.ct-p2-receive h3,.ct-p2-manage h3{font-size:30px}}
    `;
    document.head.append(style);

    const flow=intro.querySelector('.ct-p2-flow');
    const line=intro.querySelector('.ct-p2-buildline');
    let step=1;
    const setStep=n=>{
      step=n;flow.dataset.step=String(n);
      line.querySelector('span').style.width=`${n*33.333}%`;
      const names=['템플릿 선택','내용 수정','즉시 공개'];
      line.querySelector('b').textContent=names[n-1];
    };
    flow.querySelectorAll('article').forEach(card=>{
      card.addEventListener('mouseenter',()=>setStep(Number(card.dataset.step)));
    });
    setInterval(()=>setStep(step>=3?1:step+1),1900);
  };

  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',mount,{once:true}):mount();
})();