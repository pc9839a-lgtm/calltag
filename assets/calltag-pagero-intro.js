(()=>{
  if(document.documentElement.dataset.ctPageroIntroV3)return;
  document.documentElement.dataset.ctPageroIntroV3='1';

  const mount=()=>{
    const previous=document.querySelector('#ct-pagero-intro');
    if(previous)previous.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-pg3-hero">
        <div class="wrap">
          <header class="ct-pg3-heading">
            <p>페이지로</p>
            <h1>랜딩페이지 만들고,<br><span>고객 받으세요.</span></h1>
          </header>

          <div class="ct-pg3-stage">
            <div class="ct-pg3-stage-copy">
              <span class="ct-pg3-label"><i></i>광고 랜딩페이지</span>
              <h2>설명하고,<br><span>바로 접수받습니다.</span></h2>
              <p>서비스 소개와 고객 접수 폼을 한 페이지에 담아<br>광고·SNS에 바로 사용합니다.</p>
              <a href="https://pagero.kr/app">페이지로 시작하기 <b>→</b></a>
            </div>

            <div class="ct-pg3-landing">
              <div class="ct-pg3-browser"><i></i><i></i><i></i><span>내 광고 랜딩페이지</span><em>공개 중</em></div>
              <div class="ct-pg3-page">
                <div class="ct-pg3-page-copy">
                  <small>무료 상담 신청</small>
                  <h3>고객이 보고,<br><span>바로 신청합니다.</span></h3>
                  <p>필요한 설명과 신청 폼을 한 화면에.</p>
                </div>
                <div class="ct-pg3-form">
                  <strong>상담 신청</strong>
                  <label><span>이름</span><b>김민수</b></label>
                  <label><span>연락처</span><b>010-1234-5678</b></label>
                  <button type="button">상담 신청</button>
                </div>
              </div>
              <div class="ct-pg3-toast"><i>✓</i><span><b>새 고객이 접수됐습니다.</b><small>김민수 · 페이지로</small></span></div>
            </div>
          </div>
        </div>
      </section>

      <section class="ct-pg3-section">
        <div class="wrap ct-pg3-feature">
          <div class="ct-pg3-copy">
            <p>페이지 제작</p>
            <h2>세 단계면<br><span>페이지가 열립니다.</span></h2>
            <strong>템플릿을 고르고, 내용만 바꾸고, 바로 공개합니다.</strong>
          </div>
          <div class="ct-pg3-panel ct-pg3-build" data-step="1">
            <button data-step="1"><b>01</b><span><strong>템플릿 선택</strong><small>업종과 목적에 맞는 화면 선택</small></span></button>
            <button data-step="2"><b>02</b><span><strong>내용 수정</strong><small>문구·이미지·접수 항목 변경</small></span></button>
            <button data-step="3"><b>03</b><span><strong>즉시 공개</strong><small>완성 주소를 광고와 SNS에 사용</small></span></button>
            <div class="ct-pg3-progress"><span></span></div>
          </div>
        </div>
      </section>

      <section class="ct-pg3-section">
        <div class="wrap ct-pg3-feature reverse">
          <div class="ct-pg3-panel ct-pg3-sync">
            <article>
              <small>PAGERO</small>
              <h3>고객 접수</h3>
              <div><span>고객명</span><b>김민수</b></div>
              <div><span>연락처</span><b>010-1234-5678</b></div>
              <em>접수 완료</em>
            </article>
            <i class="ct-pg3-arrow"><span></span><b>→</b></i>
            <article>
              <small>CALLTAG</small>
              <h3>신규 고객</h3>
              <div class="ct-pg3-person"><i>김</i><span><b>김민수 고객</b><small>유입 · 페이지로</small></span></div>
              <div><span>자동문자</span><b class="green">발송 완료</b></div>
              <div><span>다음 연락</span><b>오늘 오후 3:00</b></div>
            </article>
          </div>
          <div class="ct-pg3-copy">
            <p>페이지로 × 콜태그</p>
            <h2>접수된 고객은<br><span>바로 관리합니다.</span></h2>
            <strong>페이지로에서 받은 고객이 콜태그에 등록되고 전화·문자·재연락 관리로 이어집니다.</strong>
            <a href="#calltag-start">콜태그 기능 이어서 보기 <b>↓</b></a>
          </div>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);
    document.querySelectorAll('style[data-ct-pagero-intro]').forEach(el=>el.remove());

    const style=document.createElement('style');
    style.dataset.ctPageroIntro='3';
    style.textContent=`
      #ct-pagero-intro{background:var(--bg);color:var(--text);overflow:hidden}
      .ct-pg3-hero{position:relative;padding:138px 0 120px;border-bottom:1px solid var(--line);overflow:hidden}
      .ct-pg3-hero:before{content:'';position:absolute;width:900px;height:900px;top:-540px;left:50%;transform:translateX(-50%);border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.22),rgba(59,111,255,0) 68%);pointer-events:none}
      .ct-pg3-heading{position:relative;z-index:1;text-align:center}.ct-pg3-heading p,.ct-pg3-copy>p{margin:0 0 20px;color:var(--blue-2);font-size:18px;font-weight:900;letter-spacing:-.03em}
      .ct-pg3-heading h1{margin:0;font-size:clamp(62px,8.2vw,118px);line-height:.94;letter-spacing:-.085em}.ct-pg3-heading h1 span,.ct-pg3-stage-copy h2 span,.ct-pg3-copy h2 span{color:var(--blue-2)}
      .ct-pg3-stage{position:relative;z-index:1;display:grid;grid-template-columns:minmax(0,.84fr) minmax(620px,1.16fr);gap:56px;align-items:center;margin-top:72px;padding:32px;border:1px solid var(--line-strong);border-radius:32px;background:linear-gradient(145deg,rgba(24,27,34,.95),rgba(13,15,19,.97));box-shadow:0 42px 120px rgba(0,0,0,.46);overflow:hidden}
      .ct-pg3-stage:before{content:'';position:absolute;width:520px;height:520px;left:-240px;bottom:-300px;border-radius:50%;background:radial-gradient(circle,rgba(59,111,255,.18),rgba(59,111,255,0) 67%)}
      .ct-pg3-stage-copy{position:relative;z-index:1;padding:18px 12px 18px 20px}.ct-pg3-label{display:flex;align-items:center;gap:10px;color:#b8c5ff;font-size:15px;font-weight:850}.ct-pg3-label i{width:9px;height:9px;border-radius:50%;background:var(--blue-2);box-shadow:0 0 0 7px rgba(124,153,255,.11)}
      .ct-pg3-stage-copy h2{margin:25px 0 0;font-size:clamp(44px,4.8vw,70px);line-height:1.03;letter-spacing:-.07em}.ct-pg3-stage-copy p{margin:21px 0 0;color:var(--muted);font-size:17px;line-height:1.68}.ct-pg3-stage-copy a,.ct-pg3-copy a{display:inline-flex;align-items:center;gap:13px;margin-top:30px;padding:15px 19px;border:1px solid var(--blue);border-radius:13px;background:var(--blue);color:#fff;font-size:14px;font-weight:850;text-decoration:none}
      .ct-pg3-landing{position:relative;overflow:visible;border:1px solid var(--line);border-radius:24px;background:#0f1116}.ct-pg3-browser{height:50px;display:flex;align-items:center;gap:7px;padding:0 18px;border-bottom:1px solid var(--line);background:#17191f;border-radius:24px 24px 0 0}.ct-pg3-browser>i{width:8px;height:8px;border-radius:50%;background:#444954}.ct-pg3-browser>span{margin-left:10px;color:var(--muted-2);font-size:10px}.ct-pg3-browser>em{margin-left:auto;padding:6px 9px;border-radius:999px;background:rgba(50,200,121,.1);color:#74dfa7;font-size:9px;font-style:normal;font-weight:850}
      .ct-pg3-page{display:grid;grid-template-columns:1.08fr .92fr;min-height:520px;background:radial-gradient(circle at 15% 0,rgba(59,111,255,.12),transparent 42%),#101218}.ct-pg3-page-copy{display:flex;flex-direction:column;justify-content:center;padding:58px 38px}.ct-pg3-page-copy small{color:var(--blue-2);font-size:10px;font-weight:850}.ct-pg3-page-copy h3{margin:17px 0 0;font-size:42px;line-height:1;letter-spacing:-.07em}.ct-pg3-page-copy h3 span{color:var(--blue-2)}.ct-pg3-page-copy p{margin:18px 0 0;color:var(--muted-2);font-size:12px;line-height:1.6}
      .ct-pg3-form{align-self:center;margin:30px 30px 30px 0;padding:22px;border:1px solid var(--line);border-radius:17px;background:#0c0f14}.ct-pg3-form>strong{font-size:17px}.ct-pg3-form label{height:52px;display:flex;align-items:center;justify-content:space-between;margin-top:11px;padding:0 14px;border:1px solid var(--line);border-radius:11px;background:#13161c}.ct-pg3-form label span{color:var(--muted-2);font-size:10px}.ct-pg3-form label b{font-size:11px}.ct-pg3-form button{width:100%;height:54px;margin-top:13px;border:0;border-radius:11px;background:var(--blue);color:#fff;font-size:12px;font-weight:850}
      .ct-pg3-toast{position:absolute;right:25px;bottom:-29px;display:flex;align-items:center;gap:11px;min-width:235px;padding:14px 16px;border:1px solid rgba(50,200,121,.25);border-radius:13px;background:#122018;box-shadow:0 18px 44px rgba(0,0,0,.4);animation:ctPg3Toast 3s ease-in-out infinite}.ct-pg3-toast>i{width:30px;height:30px;display:grid;place-items:center;border-radius:50%;background:rgba(50,200,121,.14);color:#76e0a8;font-style:normal}.ct-pg3-toast b,.ct-pg3-toast small{display:block}.ct-pg3-toast b{font-size:11px}.ct-pg3-toast small{margin-top:4px;color:#6f8c7d;font-size:9px}
      .ct-pg3-section{padding:150px 0;border-bottom:1px solid var(--line)}.ct-pg3-feature{display:grid;grid-template-columns:.78fr 1.22fr;gap:72px;align-items:center}.ct-pg3-feature.reverse{grid-template-columns:1.22fr .78fr}.ct-pg3-copy h2{margin:0;font-size:clamp(50px,5.7vw,82px);line-height:1;letter-spacing:-.08em}.ct-pg3-copy>strong{display:block;margin-top:23px;max-width:510px;color:var(--muted);font-size:17px;font-weight:600;line-height:1.65}
      .ct-pg3-panel{padding:28px;border:1px solid var(--line-strong);border-radius:30px;background:linear-gradient(145deg,#171a20,#101217);box-shadow:0 38px 100px rgba(0,0,0,.34)}
      .ct-pg3-build{display:grid;gap:12px}.ct-pg3-build button{display:grid;grid-template-columns:48px 1fr;gap:15px;align-items:center;min-height:96px;padding:16px;border:1px solid var(--line);border-radius:16px;background:rgba(255,255,255,.02);color:#747b88;text-align:left;transition:.4s ease}.ct-pg3-build button>b{width:48px;height:48px;display:grid;place-items:center;border-radius:13px;background:#1b1e25;color:#6f7683;font-size:13px}.ct-pg3-build button strong,.ct-pg3-build button small{display:block}.ct-pg3-build button strong{color:#b7bcc6;font-size:19px}.ct-pg3-build button small{margin-top:6px;color:#6f7682;font-size:11px}.ct-pg3-build[data-step='1'] button[data-step='1'],.ct-pg3-build[data-step='2'] button[data-step='2'],.ct-pg3-build[data-step='3'] button[data-step='3']{border-color:rgba(59,111,255,.52);background:var(--blue-soft);transform:translateX(7px)}.ct-pg3-build[data-step='1'] button[data-step='1']>b,.ct-pg3-build[data-step='2'] button[data-step='2']>b,.ct-pg3-build[data-step='3'] button[data-step='3']>b{background:var(--blue);color:#fff}.ct-pg3-build[data-step='1'] button[data-step='1'] strong,.ct-pg3-build[data-step='2'] button[data-step='2'] strong,.ct-pg3-build[data-step='3'] button[data-step='3'] strong{color:#fff}.ct-pg3-progress{height:4px;margin-top:6px;overflow:hidden;border-radius:999px;background:rgba(255,255,255,.08)}.ct-pg3-progress span{display:block;width:33.33%;height:100%;background:var(--blue-2);transition:width .45s ease}
      .ct-pg3-sync{display:grid;grid-template-columns:1fr 100px 1fr;align-items:center}.ct-pg3-sync article{min-height:350px;padding:27px;border:1px solid var(--line);border-radius:20px;background:#12151b}.ct-pg3-sync article>small{color:var(--blue-2);font-size:10px;font-weight:900}.ct-pg3-sync article h3{margin:15px 0 25px;font-size:32px;letter-spacing:-.06em}.ct-pg3-sync article>div:not(.ct-pg3-person){height:56px;display:flex;align-items:center;justify-content:space-between;margin-top:10px;padding:0 14px;border:1px solid var(--line);border-radius:11px;background:#0d1015}.ct-pg3-sync article>div span{color:var(--muted-2);font-size:10px}.ct-pg3-sync article>div b{font-size:11px}.ct-pg3-sync article em{display:grid;place-items:center;height:52px;margin-top:14px;border-radius:11px;background:var(--blue);font-size:11px;font-style:normal;font-weight:850}.ct-pg3-person{display:flex;align-items:center;gap:12px;margin-bottom:17px}.ct-pg3-person>i{width:45px;height:45px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#b7c4ff;font-style:normal;font-weight:850}.ct-pg3-person b,.ct-pg3-person small{display:block}.ct-pg3-person b{font-size:14px}.ct-pg3-person small{margin-top:5px;color:var(--muted-2);font-size:9px}.ct-pg3-sync .green{color:#6ddca6}
      .ct-pg3-arrow{position:relative;height:2px;background:rgba(59,111,255,.2);font-style:normal}.ct-pg3-arrow span:after{content:'';position:absolute;left:-38%;top:0;width:38%;height:2px;background:var(--blue-2);box-shadow:0 0 14px var(--blue-2);animation:ctPg3Move 1.5s linear infinite}.ct-pg3-arrow b{position:absolute;left:50%;top:50%;width:40px;height:40px;display:grid;place-items:center;transform:translate(-50%,-50%);border:1px solid rgba(124,153,255,.45);border-radius:50%;background:#141924;color:var(--blue-2);font-size:19px}
      @keyframes ctPg3Toast{0%,100%{transform:translateY(0)}50%{transform:translateY(-7px)}}@keyframes ctPg3Move{to{left:100%}}
      @media(max-width:1050px){.ct-pg3-stage,.ct-pg3-feature,.ct-pg3-feature.reverse{grid-template-columns:1fr}.ct-pg3-stage-copy,.ct-pg3-copy{text-align:center}.ct-pg3-stage-copy p,.ct-pg3-copy>strong{margin-left:auto;margin-right:auto}.ct-pg3-sync{grid-template-columns:1fr}.ct-pg3-arrow{height:90px;width:2px;margin:auto}.ct-pg3-arrow span:after{left:0;top:-38%;width:2px;height:38%;animation:ctPg3MoveY 1.5s linear infinite}.ct-pg3-arrow b{transform:translate(-50%,-50%) rotate(90deg)}@keyframes ctPg3MoveY{to{top:100%}}}
      @media(max-width:700px){.ct-pg3-hero{padding:112px 0 76px}.ct-pg3-heading h1{font-size:54px}.ct-pg3-heading p,.ct-pg3-copy>p{font-size:13px}.ct-pg3-stage{margin-top:48px;padding:18px;border-radius:23px}.ct-pg3-stage-copy{padding:18px 5px 8px}.ct-pg3-stage-copy h2{font-size:43px}.ct-pg3-stage-copy p{font-size:15px}.ct-pg3-page{grid-template-columns:1fr}.ct-pg3-page-copy{text-align:center;padding:42px 22px 24px}.ct-pg3-page-copy h3{font-size:39px}.ct-pg3-form{margin:0 17px 42px}.ct-pg3-toast{position:relative;right:auto;bottom:auto;min-width:0;margin:0 17px 18px}.ct-pg3-section{padding:94px 0}.ct-pg3-copy h2{font-size:47px}.ct-pg3-panel{padding:17px;border-radius:22px}.ct-pg3-build button{min-height:86px;padding:13px}.ct-pg3-build button strong{font-size:17px}.ct-pg3-sync article{min-height:0;padding:23px}}
    `;
    document.head.append(style);

    const build=intro.querySelector('.ct-pg3-build');
    const progress=intro.querySelector('.ct-pg3-progress span');
    let step=1;
    const setStep=n=>{step=n;build.dataset.step=String(n);progress.style.width=`${n*33.333}%`;};
    build.querySelectorAll('button').forEach(button=>button.addEventListener('mouseenter',()=>setStep(Number(button.dataset.step))));
    setInterval(()=>setStep(step>=3?1:step+1),2100);
  };

  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',mount,{once:true}):mount();
})();