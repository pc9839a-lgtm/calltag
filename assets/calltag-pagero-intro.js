(()=>{
  if(document.documentElement.dataset.ctPageroCoreV4)return;
  document.documentElement.dataset.ctPageroCoreV4='1';

  const mount=()=>{
    document.querySelector('#ct-pagero-intro')?.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-core-hero">
        <div class="wrap">
          <header class="ct-core-head">
            <p>PAGERO × CALLTAG</p>
            <h1>문의가 들어오는 순간,<br><span>절대 놓치지 않습니다.</span></h1>
            <strong>페이지로에서 문의를 받고, 콜태그가 자동으로 관리합니다.</strong>
          </header>

          <div class="ct-core-stage" aria-label="페이지로 문의가 콜태그에 자동 등록되는 예시">
            <article class="ct-core-page">
              <div class="ct-core-top"><span>PAGERO</span><em>광고 랜딩페이지</em></div>
              <div class="ct-core-page-body">
                <small>무료 상담 신청</small>
                <h2>상담이 필요하면<br>지금 문의하세요.</h2>
                <label><span>이름</span><b>김민수</b></label>
                <label><span>연락처</span><b>010-1234-5678</b></label>
                <button type="button">문의 접수</button>
              </div>
            </article>

            <div class="ct-core-transfer">
              <span></span><b>→</b><small>자동 등록</small>
            </div>

            <article class="ct-core-calltag">
              <div class="ct-core-top"><span>CALLTAG</span><em>신규 고객</em></div>
              <div class="ct-core-person"><i>김</i><span><b>김민수 고객</b><small>유입 · 페이지로</small></span></div>
              <div class="ct-core-result"><span>문의 상태</span><b>신규 문의</b></div>
              <div class="ct-core-result"><span>자동문자</span><b class="green">발송 완료</b></div>
              <div class="ct-core-result"><span>다음 연락</span><b>오늘 오후 3:00</b></div>
            </article>
          </div>
        </div>
      </section>

      <section class="ct-core-section">
        <div class="wrap ct-core-feature">
          <div class="ct-core-copy">
            <p>페이지로</p>
            <h2>누구나 쉽게 만드는<br><span>노코드 랜딩페이지.</span></h2>
            <strong>코드를 몰라도 문구와 이미지만 바꿔 광고용 페이지를 만들고, 고객 문의까지 받을 수 있습니다.</strong>
            <a href="https://pagero.kr/app">페이지로 시작하기 <b>→</b></a>
          </div>

          <div class="ct-core-panel ct-core-nocode">
            <div class="ct-core-nocode-head"><span>페이지 만들기</span><b>3단계</b></div>
            <div class="ct-core-step active"><b>01</b><span><strong>내용 입력</strong><small>문구와 이미지를 바꿉니다.</small></span></div>
            <div class="ct-core-step"><b>02</b><span><strong>문의 폼 추가</strong><small>이름·연락처 등 필요한 항목만 받습니다.</small></span></div>
            <div class="ct-core-step"><b>03</b><span><strong>바로 공개</strong><small>완성된 주소를 광고와 SNS에 사용합니다.</small></span></div>
            <div class="ct-core-publish"><i></i><span>랜딩페이지가 공개되었습니다.</span></div>
          </div>
        </div>
      </section>

      <section class="ct-core-section">
        <div class="wrap ct-core-feature reverse">
          <div class="ct-core-panel ct-core-example">
            <div class="ct-core-example-title"><span>실제 문의 예시</span><b>김민수 고객</b></div>
            <div class="ct-core-timeline">
              <article class="on"><i>1</i><span><strong>페이지로 문의 접수</strong><small>오후 1:42 · 보험 상담 랜딩</small></span></article>
              <article><i>2</i><span><strong>콜태그 고객 자동등록</strong><small>이름·연락처·유입경로 저장</small></span></article>
              <article><i>3</i><span><strong>안내문자 자동발송</strong><small>상담 접수 완료 안내</small></span></article>
              <article><i>4</i><span><strong>재연락 일정 등록</strong><small>오늘 오후 3시 전화</small></span></article>
            </div>
          </div>

          <div class="ct-core-copy">
            <p>페이지로 × 콜태그</p>
            <h2>문의 접수부터<br><span>후속관리까지.</span></h2>
            <strong>고객정보를 다시 옮겨 적지 않습니다. 문의가 들어오면 고객 등록, 문자, 전화와 재연락 일정까지 바로 이어집니다.</strong>
            <a href="#calltag-start">콜태그 기능 이어서 보기 <b>↓</b></a>
          </div>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);
    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());

    const style=document.createElement('style');
    style.dataset.ctPageroIntro='4';
    style.textContent=`
      #ct-pagero-intro{background:var(--bg);color:var(--text);overflow:hidden}
      .ct-core-hero{position:relative;padding:138px 0 120px;border-bottom:1px solid var(--line);overflow:hidden}
      .ct-core-hero:before{content:'';position:absolute;width:900px;height:900px;top:-550px;left:50%;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.22),transparent 68%);pointer-events:none}
      .ct-core-head{position:relative;z-index:1;text-align:center}.ct-core-head>p,.ct-core-copy>p{margin:0 0 20px;color:var(--blue-2);font-size:18px;font-weight:900;letter-spacing:-.03em}
      .ct-core-head h1{margin:0;font-size:clamp(62px,8.2vw,118px);line-height:.94;letter-spacing:-.085em}.ct-core-head h1 span,.ct-core-copy h2 span{color:var(--blue-2)}
      .ct-core-head>strong{display:block;margin:26px auto 0;color:#d5d8df;font-size:clamp(17px,1.75vw,22px);font-weight:800;letter-spacing:-.035em}
      .ct-core-stage{position:relative;z-index:1;display:grid;grid-template-columns:1fr 120px 1fr;align-items:center;gap:0;margin-top:72px;padding:34px;border:1px solid var(--line-strong);border-radius:32px;background:linear-gradient(145deg,rgba(24,27,34,.96),rgba(13,15,19,.98));box-shadow:0 42px 120px rgba(0,0,0,.46)}
      .ct-core-page,.ct-core-calltag{min-height:460px;padding:28px;border:1px solid var(--line);border-radius:22px;background:#101218}
      .ct-core-top{display:flex;align-items:center;justify-content:space-between;padding-bottom:20px;border-bottom:1px solid var(--line)}.ct-core-top span{color:var(--blue-2);font-size:11px;font-weight:900}.ct-core-top em{color:var(--muted-2);font-size:10px;font-style:normal;font-weight:750}
      .ct-core-page-body{padding:38px 10px 0}.ct-core-page-body>small{color:var(--blue-2);font-size:10px;font-weight:850}.ct-core-page-body h2{margin:15px 0 25px;font-size:38px;line-height:1.04;letter-spacing:-.065em}.ct-core-page-body label{height:55px;display:flex;align-items:center;justify-content:space-between;margin-top:10px;padding:0 15px;border:1px solid var(--line);border-radius:12px;background:#151820}.ct-core-page-body label span{color:var(--muted-2);font-size:10px}.ct-core-page-body label b{font-size:12px}.ct-core-page-body button{width:100%;height:56px;margin-top:12px;border:0;border-radius:12px;background:var(--blue);color:#fff;font-size:13px;font-weight:900}
      .ct-core-transfer{display:grid;place-items:center;gap:9px}.ct-core-transfer>span{position:absolute;width:92px;height:2px;background:rgba(59,111,255,.24)}.ct-core-transfer>span:after{content:'';position:absolute;left:0;top:-2px;width:7px;height:7px;border-radius:50%;background:var(--blue-2);animation:ctCoreMove 2.2s linear infinite}.ct-core-transfer>b{position:relative;z-index:1;width:46px;height:46px;display:grid;place-items:center;border:1px solid rgba(59,111,255,.52);border-radius:50%;background:#111725;color:var(--blue-2);font-size:21px}.ct-core-transfer small{color:var(--muted-2);font-size:9px;font-weight:800}
      .ct-core-person{display:flex;align-items:center;gap:14px;margin:30px 0 24px}.ct-core-person>i{width:48px;height:48px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#b5c1ff;font-size:14px;font-style:normal;font-weight:900}.ct-core-person b,.ct-core-person small{display:block}.ct-core-person b{font-size:17px}.ct-core-person small{margin-top:5px;color:var(--muted-2);font-size:10px}.ct-core-result{height:58px;display:flex;align-items:center;justify-content:space-between;margin-top:10px;padding:0 15px;border:1px solid var(--line);border-radius:12px;background:#151820}.ct-core-result span{color:var(--muted-2);font-size:10px}.ct-core-result b{font-size:12px}.ct-core-result b.green{color:#67dda5}
      .ct-core-section{padding:150px 0;border-bottom:1px solid var(--line)}.ct-core-feature{display:grid;grid-template-columns:.78fr 1.22fr;gap:72px;align-items:center}.ct-core-feature.reverse{grid-template-columns:1.22fr .78fr}.ct-core-copy h2{margin:0;font-size:clamp(50px,5.7vw,82px);line-height:1;letter-spacing:-.08em}.ct-core-copy>strong{display:block;margin-top:23px;max-width:540px;color:var(--muted);font-size:17px;font-weight:600;line-height:1.68}.ct-core-copy>a{display:inline-flex;align-items:center;gap:13px;margin-top:30px;padding:15px 19px;border:1px solid var(--blue);border-radius:13px;background:var(--blue);color:#fff;font-size:14px;font-weight:850;text-decoration:none}
      .ct-core-panel{padding:30px;border:1px solid var(--line-strong);border-radius:30px;background:linear-gradient(145deg,#171a20,#101217);box-shadow:0 38px 100px rgba(0,0,0,.34)}
      .ct-core-nocode-head,.ct-core-example-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px}.ct-core-nocode-head span,.ct-core-example-title span{color:var(--muted-2);font-size:11px;font-weight:800}.ct-core-nocode-head b{color:var(--blue-2);font-size:12px}.ct-core-example-title b{font-size:18px}
      .ct-core-step{display:grid;grid-template-columns:48px 1fr;gap:15px;align-items:center;min-height:94px;margin-top:11px;padding:16px;border:1px solid var(--line);border-radius:16px;background:rgba(255,255,255,.02);opacity:.58;transition:.4s ease}.ct-core-step>b{width:48px;height:48px;display:grid;place-items:center;border-radius:13px;background:#1b1e25;color:#6f7683;font-size:13px}.ct-core-step strong,.ct-core-step small{display:block}.ct-core-step strong{font-size:19px}.ct-core-step small{margin-top:6px;color:var(--muted-2);font-size:11px}.ct-core-step.active{border-color:rgba(59,111,255,.52);background:var(--blue-soft);opacity:1;transform:translateX(7px)}.ct-core-step.active>b{background:var(--blue);color:#fff}.ct-core-publish{display:flex;align-items:center;gap:10px;margin-top:18px;padding:15px;border:1px solid rgba(50,200,121,.22);border-radius:13px;background:rgba(50,200,121,.07);color:#82e2ae;font-size:11px;font-weight:850}.ct-core-publish i{width:8px;height:8px;border-radius:50%;background:var(--green)}
      .ct-core-timeline{display:grid;gap:12px}.ct-core-timeline article{position:relative;display:grid;grid-template-columns:48px 1fr;gap:15px;align-items:center;min-height:88px;padding:15px;border:1px solid var(--line);border-radius:16px;background:#13161c;opacity:.42;transform:translateY(8px);transition:.45s ease}.ct-core-timeline article:not(:last-child):after{content:'';position:absolute;left:38px;bottom:-13px;width:2px;height:13px;background:rgba(59,111,255,.26)}.ct-core-timeline article>i{width:48px;height:48px;display:grid;place-items:center;border-radius:14px;background:#1b1e25;color:#69717e;font-size:13px;font-style:normal;font-weight:900}.ct-core-timeline strong,.ct-core-timeline small{display:block}.ct-core-timeline strong{font-size:17px}.ct-core-timeline small{margin-top:6px;color:var(--muted-2);font-size:10px}.ct-core-timeline article.on{border-color:rgba(59,111,255,.5);background:var(--blue-soft);opacity:1;transform:none}.ct-core-timeline article.on>i{background:var(--blue);color:#fff}
      @keyframes ctCoreMove{from{transform:translateX(0)}to{transform:translateX(85px)}}
      @media(max-width:980px){.ct-core-stage{grid-template-columns:1fr;gap:24px}.ct-core-transfer{min-height:64px}.ct-core-transfer>span{width:2px;height:54px}.ct-core-transfer>span:after{left:-2px;top:0;animation:ctCoreMoveY 2.2s linear infinite}.ct-core-transfer>b{transform:rotate(90deg)}.ct-core-feature,.ct-core-feature.reverse{grid-template-columns:1fr}.ct-core-feature.reverse .ct-core-panel{order:2}.ct-core-feature.reverse .ct-core-copy{order:1}}
      @keyframes ctCoreMoveY{from{transform:translateY(0)}to{transform:translateY(48px)}}
      @media(max-width:650px){.ct-core-hero{padding:112px 0 82px}.ct-core-head h1{font-size:clamp(43px,13vw,64px)}.ct-core-head>strong{font-size:15px;line-height:1.55}.ct-core-stage{margin-top:48px;padding:16px;border-radius:23px}.ct-core-page,.ct-core-calltag{min-height:auto;padding:20px;border-radius:17px}.ct-core-page-body{padding:25px 0 0}.ct-core-page-body h2{font-size:30px}.ct-core-section{padding:96px 0}.ct-core-feature{gap:40px}.ct-core-copy h2{font-size:clamp(43px,12vw,60px)}.ct-core-copy>strong{font-size:15px}.ct-core-panel{padding:18px;border-radius:22px}.ct-core-step,.ct-core-timeline article{min-height:80px}.ct-core-step strong{font-size:17px}}
      @media(prefers-reduced-motion:reduce){.ct-core-transfer>span:after{animation:none}}
    `;
    document.head.append(style);

    const steps=[...intro.querySelectorAll('.ct-core-step')];
    let stepIndex=0;
    setInterval(()=>{steps.forEach((el,i)=>el.classList.toggle('active',i===stepIndex));stepIndex=(stepIndex+1)%steps.length},1800);

    const timeline=[...intro.querySelectorAll('.ct-core-timeline article')];
    let timelineIndex=0;
    setInterval(()=>{timeline.forEach((el,i)=>el.classList.toggle('on',i===timelineIndex));timelineIndex=(timelineIndex+1)%timeline.length},1500);
  };

  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',mount,{once:true}):mount();
})();