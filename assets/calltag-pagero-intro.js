(()=>{
  if(document.documentElement.dataset.ctPageroIntro)return;
  document.documentElement.dataset.ctPageroIntro='1';

  const mount=()=>{
    if(document.querySelector('#ct-pagero-intro'))return;
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-pg-hero">
        <div class="ct-pg-glow"></div>
        <div class="ct-pg-wrap ct-pg-hero-grid">
          <div class="ct-pg-copy">
            <p class="ct-pg-kicker">PAGERO · LANDING PAGE BUILDER</p>
            <h1>광고할 페이지,<br><span>직접 만들고</span><br>고객까지 받으세요.</h1>
            <p class="ct-pg-desc">페이지로는 랜딩페이지 제작과 고객 접수를 한 번에 해결합니다.<br>만들고, 공개하고, 들어온 고객까지 바로 확인하세요.</p>
            <div class="ct-pg-actions">
              <a class="ct-pg-primary" href="https://pagero.kr/app">페이지로 시작하기 <b>→</b></a>
              <a class="ct-pg-secondary" href="#ct-pg-build">어떻게 만드는지 보기 <b>↓</b></a>
            </div>
            <div class="ct-pg-proof"><span>코드 없이</span><i></i><span>모바일 최적화</span><i></i><span>고객 접수 폼</span></div>
          </div>

          <div class="ct-pg-editor" aria-label="페이지로 랜딩페이지 편집 화면 예시">
            <div class="ct-pg-browser"><i></i><i></i><i></i><span>pagero.kr/app</span><em>게시 중</em></div>
            <div class="ct-pg-editor-body">
              <aside class="ct-pg-blocks">
                <strong>블록</strong>
                <button class="active"><i></i><span>히어로</span></button>
                <button><i></i><span>혜택</span></button>
                <button><i></i><span>접수 폼</span></button>
                <button><i></i><span>후기</span></button>
              </aside>
              <div class="ct-pg-canvas">
                <div class="ct-pg-live"><i></i>실시간 편집</div>
                <div class="ct-pg-landing">
                  <small>무료 상담 신청</small>
                  <h3>고객이 바로<br><span>반응하는 페이지.</span></h3>
                  <p>필요한 내용만 담아 빠르게 공개하세요.</p>
                  <div class="ct-pg-form-mini"><span>이름</span><b>김민수</b></div>
                  <div class="ct-pg-form-mini"><span>연락처</span><b>010-1234-5678</b></div>
                  <button>상담 신청</button>
                </div>
                <span class="ct-pg-cursor">↖</span>
              </div>
              <aside class="ct-pg-settings">
                <strong>스타일</strong>
                <label>메인 문구</label><div class="ct-pg-setting-line wide"></div>
                <label>강조 색상</label><div class="ct-pg-color-row"><i></i><i></i><i></i></div>
                <label>버튼</label><div class="ct-pg-setting-line"></div>
                <div class="ct-pg-publish">바로 공개</div>
              </aside>
            </div>
          </div>
        </div>
      </section>

      <section class="ct-pg-build" id="ct-pg-build">
        <div class="ct-pg-wrap">
          <header class="ct-pg-section-head">
            <p>페이지 제작</p>
            <h2>세 단계면<br><span>페이지가 열립니다.</span></h2>
          </header>
          <div class="ct-pg-steps" data-step="1">
            <button data-step="1"><b>01</b><strong>템플릿 선택</strong><span>업종과 목적에 맞는 화면부터 고릅니다.</span></button>
            <i>→</i>
            <button data-step="2"><b>02</b><strong>내용 수정</strong><span>문구·이미지·접수 항목을 바로 바꿉니다.</span></button>
            <i>→</i>
            <button data-step="3"><b>03</b><strong>즉시 공개</strong><span>완성된 주소를 광고와 SNS에 사용합니다.</span></button>
          </div>
          <div class="ct-pg-build-screen" data-step="1">
            <div class="ct-pg-template-list">
              <article class="active"><span>상담 신청</span><b>서비스 소개형</b></article>
              <article><span>예약 접수</span><b>매장 예약형</b></article>
              <article><span>이벤트</span><b>광고 캠페인형</b></article>
            </div>
            <div class="ct-pg-build-preview">
              <small>페이지로 템플릿</small><h3>서비스를 설명하고<br>고객을 받는 한 페이지.</h3><button>상담 신청하기</button>
            </div>
            <div class="ct-pg-build-status"><i></i><span>템플릿을 선택했습니다.</span></div>
          </div>
        </div>
      </section>

      <section class="ct-pg-connect">
        <div class="ct-pg-wrap ct-pg-connect-grid">
          <div class="ct-pg-connect-copy">
            <p class="ct-pg-kicker">PAGERO → CALLTAG</p>
            <h2>접수된 고객은<br><span>콜태그로 이어집니다.</span></h2>
            <p>페이지로에서 받은 이름과 연락처가 콜태그 고객으로 등록되고,<br>전화·문자·재연락 관리까지 한 흐름으로 이어집니다.</p>
            <div class="ct-pg-flow-line"><b>고객 접수</b><i>→</i><b>자동 등록</b><i>→</i><b>후속관리</b></div>
            <a href="#calltag-start">콜태그 기능 이어서 보기 <b>↓</b></a>
          </div>
          <div class="ct-pg-connect-demo">
            <article class="ct-pg-lead-card">
              <header><span>PAGERO</span><b>새로운 접수</b></header>
              <div><small>고객명</small><strong>김민수</strong></div>
              <div><small>연락처</small><strong>010-1234-5678</strong></div>
              <em>접수 완료</em>
            </article>
            <div class="ct-pg-transfer"><span></span><b>→</b></div>
            <article class="ct-pg-call-card">
              <header><span>CALLTAG</span><b>신규 고객</b></header>
              <div class="ct-pg-person"><i>김</i><span><strong>김민수 고객</strong><small>유입 · 페이지로</small></span></div>
              <div class="ct-pg-task"><span>자동문자</span><b>발송 완료</b></div>
              <div class="ct-pg-task"><span>다음 연락</span><b>오늘 오후 3:00</b></div>
            </article>
          </div>
        </div>
      </section>`;

    calltagHero.parentNode.insertBefore(intro,calltagHero);

    if(!document.querySelector('style[data-ct-pagero-intro]')){
      const style=document.createElement('style');
      style.dataset.ctPageroIntro='1';
      style.textContent=`
        #ct-pagero-intro{background:#080a0f;color:#f7f8fb;overflow:hidden}
        .ct-pg-wrap{width:min(1460px,calc(100% - 72px));margin:0 auto}.ct-pg-kicker{margin:0 0 19px;color:#7897ff;font-size:12px;font-weight:850;letter-spacing:.09em}
        .ct-pg-hero{position:relative;min-height:100vh;display:flex;align-items:center;padding:132px 0 90px;border-bottom:1px solid rgba(255,255,255,.08);background:linear-gradient(180deg,#090c15,#080a0f)}
        .ct-pg-glow{position:absolute;top:-340px;right:-180px;width:980px;height:980px;border-radius:50%;background:radial-gradient(circle,rgba(63,101,255,.24),transparent 68%);pointer-events:none}
        .ct-pg-hero-grid{position:relative;z-index:1;display:grid;grid-template-columns:minmax(430px,.82fr) minmax(700px,1.35fr);align-items:center;gap:72px}
        .ct-pg-copy h1{margin:0;font-size:clamp(64px,6.5vw,104px);line-height:.93;letter-spacing:-.086em}.ct-pg-copy h1 span{color:#7594ff}.ct-pg-desc{margin:29px 0 0;color:#b5bdcb;font-size:17px;line-height:1.72;letter-spacing:-.026em}
        .ct-pg-actions{display:flex;align-items:center;gap:12px;margin-top:35px}.ct-pg-actions a{display:inline-flex;align-items:center;gap:15px;padding:16px 20px;border-radius:13px;font-size:14px;font-weight:780}.ct-pg-primary{background:#416fff;color:#fff;box-shadow:0 14px 34px rgba(65,111,255,.24)}.ct-pg-secondary{border:1px solid rgba(255,255,255,.13);color:#d7dce6}.ct-pg-actions b{font-size:18px}.ct-pg-proof{display:flex;align-items:center;gap:11px;margin-top:27px;color:#7e8796;font-size:11px;font-weight:700}.ct-pg-proof i{width:3px;height:3px;border-radius:50%;background:#4f5867}
        .ct-pg-editor{overflow:hidden;border:1px solid rgba(117,148,255,.42);border-radius:28px;background:#11151d;box-shadow:0 40px 100px rgba(0,0,0,.4),0 0 100px rgba(65,111,255,.08)}.ct-pg-browser{height:50px;display:flex;align-items:center;gap:7px;padding:0 18px;border-bottom:1px solid rgba(255,255,255,.09);background:#171b24}.ct-pg-browser>i{width:8px;height:8px;border-radius:50%;background:#555e6e}.ct-pg-browser>span{margin-left:9px;color:#687181;font-size:10px}.ct-pg-browser>em{margin-left:auto;padding:6px 9px;border-radius:999px;background:rgba(53,207,140,.11);color:#55dca1;font-size:9px;font-style:normal;font-weight:800}.ct-pg-editor-body{display:grid;grid-template-columns:115px minmax(0,1fr) 150px;min-height:590px}.ct-pg-blocks,.ct-pg-settings{padding:22px 14px;background:#0e1218}.ct-pg-blocks{border-right:1px solid rgba(255,255,255,.08)}.ct-pg-settings{border-left:1px solid rgba(255,255,255,.08)}.ct-pg-blocks>strong,.ct-pg-settings>strong{display:block;margin:0 7px 19px;color:#7c8595;font-size:10px}.ct-pg-blocks button{width:100%;display:flex;align-items:center;gap:9px;margin:0 0 7px;padding:11px 10px;border:0;border-radius:9px;background:transparent;color:#626b79;font-size:10px;text-align:left}.ct-pg-blocks button i{width:13px;height:13px;border:1px solid #444d5c;border-radius:3px}.ct-pg-blocks button.active{background:rgba(65,111,255,.15);color:#a9b8ff}.ct-pg-canvas{position:relative;display:grid;place-items:center;padding:38px;background:radial-gradient(circle at center,rgba(65,111,255,.08),transparent 60%),#12161e}.ct-pg-live{position:absolute;top:18px;right:18px;display:flex;align-items:center;gap:7px;color:#7f8998;font-size:9px}.ct-pg-live i{width:6px;height:6px;border-radius:50%;background:#45d99b;box-shadow:0 0 0 5px rgba(69,217,155,.08)}.ct-pg-landing{width:min(390px,100%);padding:38px 32px;border:1px solid rgba(255,255,255,.12);border-radius:20px;background:#0d1118;box-shadow:0 24px 70px rgba(0,0,0,.35)}.ct-pg-landing>small{color:#7594ff;font-size:10px;font-weight:800}.ct-pg-landing h3{margin:13px 0 0;font-size:34px;line-height:1.05;letter-spacing:-.06em}.ct-pg-landing h3 span{color:#7897ff}.ct-pg-landing>p{margin:14px 0 22px;color:#818a99;font-size:11px}.ct-pg-form-mini{display:flex;align-items:center;justify-content:space-between;height:45px;margin-top:8px;padding:0 13px;border:1px solid rgba(255,255,255,.09);border-radius:10px;background:#111620}.ct-pg-form-mini span{color:#687181;font-size:9px}.ct-pg-form-mini b{font-size:10px;font-weight:700}.ct-pg-landing>button{width:100%;height:47px;margin-top:11px;border:0;border-radius:10px;background:#416fff;color:#fff;font-size:11px;font-weight:800}.ct-pg-cursor{position:absolute;right:24%;bottom:27%;color:#fff;font-size:26px;filter:drop-shadow(0 4px 8px rgba(0,0,0,.45));animation:ctPgCursor 3.4s ease-in-out infinite}.ct-pg-settings label{display:block;margin:17px 0 8px;color:#626b79;font-size:9px}.ct-pg-setting-line{height:29px;border:1px solid rgba(255,255,255,.08);border-radius:7px;background:#141922}.ct-pg-setting-line.wide{height:52px}.ct-pg-color-row{display:flex;gap:7px}.ct-pg-color-row i{width:23px;height:23px;border-radius:7px;background:#416fff}.ct-pg-color-row i:nth-child(2){background:#111827}.ct-pg-color-row i:nth-child(3){background:#f5f7fb}.ct-pg-publish{display:grid;place-items:center;height:38px;margin-top:26px;border-radius:8px;background:#416fff;color:#fff;font-size:10px;font-weight:800}
        .ct-pg-build{padding:145px 0;background:#0b0e14;border-bottom:1px solid rgba(255,255,255,.08)}.ct-pg-section-head{text-align:center}.ct-pg-section-head>p{margin:0 0 13px;color:#7897ff;font-size:12px;font-weight:850}.ct-pg-section-head h2{margin:0;font-size:clamp(52px,5.6vw,82px);line-height:.98;letter-spacing:-.075em}.ct-pg-section-head h2 span{color:#7897ff}.ct-pg-steps{display:grid;grid-template-columns:1fr 30px 1fr 30px 1fr;align-items:center;gap:8px;margin:60px auto 0;max-width:1120px}.ct-pg-steps>button{min-height:150px;padding:25px;border:1px solid rgba(255,255,255,.1);border-radius:19px;background:#11151c;color:#77808f;text-align:left;transition:.35s ease}.ct-pg-steps>button b,.ct-pg-steps>button strong,.ct-pg-steps>button span{display:block}.ct-pg-steps>button b{color:#647fd4;font-size:11px}.ct-pg-steps>button strong{margin-top:17px;color:#e6e9ef;font-size:22px}.ct-pg-steps>button span{margin-top:10px;font-size:12px;line-height:1.55}.ct-pg-steps>i{color:#3f4754;font-style:normal;text-align:center}.ct-pg-steps[data-step='1'] [data-step='1'],.ct-pg-steps[data-step='2'] [data-step='2'],.ct-pg-steps[data-step='3'] [data-step='3']{border-color:rgba(117,148,255,.65);background:rgba(65,111,255,.13);transform:translateY(-7px);box-shadow:0 18px 45px rgba(0,0,0,.22)}.ct-pg-build-screen{position:relative;display:grid;grid-template-columns:260px 1fr;min-height:460px;max-width:1120px;margin:28px auto 0;overflow:hidden;border:1px solid rgba(255,255,255,.11);border-radius:24px;background:#10141b}.ct-pg-template-list{padding:25px;border-right:1px solid rgba(255,255,255,.08)}.ct-pg-template-list article{padding:18px;margin-bottom:10px;border:1px solid rgba(255,255,255,.08);border-radius:13px;color:#6f7887;background:#0d1117}.ct-pg-template-list article span,.ct-pg-template-list article b{display:block}.ct-pg-template-list article span{font-size:9px}.ct-pg-template-list article b{margin-top:8px;font-size:12px}.ct-pg-template-list article.active{border-color:rgba(117,148,255,.5);background:rgba(65,111,255,.11);color:#dfe5ff}.ct-pg-build-preview{display:flex;flex-direction:column;align-items:center;justify-content:center;padding:50px;text-align:center;background:radial-gradient(circle,rgba(65,111,255,.1),transparent 60%)}.ct-pg-build-preview small{color:#7897ff;font-size:10px}.ct-pg-build-preview h3{margin:15px 0 25px;font-size:37px;line-height:1.08;letter-spacing:-.055em}.ct-pg-build-preview button{padding:13px 18px;border:0;border-radius:10px;background:#416fff;color:#fff;font-size:11px;font-weight:800}.ct-pg-build-status{position:absolute;right:24px;bottom:22px;display:flex;align-items:center;gap:8px;padding:11px 13px;border:1px solid rgba(53,207,140,.22);border-radius:10px;background:#112019;color:#66dba8;font-size:10px}.ct-pg-build-status i{width:6px;height:6px;border-radius:50%;background:#45d99b}
        .ct-pg-connect{padding:150px 0;background:#080a0f;border-bottom:1px solid rgba(255,255,255,.08)}.ct-pg-connect-grid{display:grid;grid-template-columns:minmax(410px,.8fr) minmax(620px,1.2fr);align-items:center;gap:80px}.ct-pg-connect-copy h2{margin:0;font-size:clamp(52px,5.5vw,82px);line-height:.98;letter-spacing:-.076em}.ct-pg-connect-copy h2 span{color:#7897ff}.ct-pg-connect-copy>p:not(.ct-pg-kicker){margin:27px 0 0;color:#aeb6c4;font-size:16px;line-height:1.72}.ct-pg-flow-line{display:flex;align-items:center;gap:12px;margin-top:31px;color:#d9deea;font-size:12px}.ct-pg-flow-line i{color:#4a5361;font-style:normal}.ct-pg-connect-copy>a{display:inline-flex;align-items:center;gap:12px;margin-top:35px;padding:14px 17px;border:1px solid rgba(117,148,255,.42);border-radius:12px;color:#f3f5fb;font-size:13px;font-weight:760}.ct-pg-connect-demo{display:grid;grid-template-columns:1fr 70px 1fr;align-items:center;padding:34px;border:1px solid rgba(117,148,255,.38);border-radius:27px;background:linear-gradient(145deg,#141923,#0f1218);box-shadow:0 30px 80px rgba(0,0,0,.32)}.ct-pg-lead-card,.ct-pg-call-card{min-height:340px;padding:25px;border:1px solid rgba(255,255,255,.11);border-radius:19px;background:#0e1218}.ct-pg-lead-card header,.ct-pg-call-card header{display:flex;justify-content:space-between;padding-bottom:18px;border-bottom:1px solid rgba(255,255,255,.08)}.ct-pg-lead-card header span,.ct-pg-call-card header span{color:#7897ff;font-size:10px;font-weight:850}.ct-pg-lead-card header b,.ct-pg-call-card header b{color:#707988;font-size:9px}.ct-pg-lead-card>div{display:flex;align-items:center;justify-content:space-between;height:50px;margin-top:12px;padding:0 13px;border:1px solid rgba(255,255,255,.08);border-radius:10px}.ct-pg-lead-card small{color:#707988;font-size:9px}.ct-pg-lead-card strong{font-size:11px}.ct-pg-lead-card>em{display:grid;place-items:center;height:46px;margin-top:16px;border-radius:10px;background:#416fff;color:#fff;font-size:11px;font-style:normal;font-weight:800}.ct-pg-transfer{position:relative;display:grid;place-items:center}.ct-pg-transfer>span{position:absolute;width:100%;height:2px;background:rgba(117,148,255,.2);overflow:hidden}.ct-pg-transfer>span:after{content:'';position:absolute;width:42%;height:100%;left:-42%;background:#7897ff;box-shadow:0 0 12px #7897ff;animation:ctPgTransfer 1.5s linear infinite}.ct-pg-transfer>b{position:relative;z-index:1;width:34px;height:34px;display:grid;place-items:center;border:1px solid rgba(117,148,255,.45);border-radius:50%;background:#131925;color:#7897ff}.ct-pg-person{display:flex;align-items:center;gap:12px;padding:24px 0}.ct-pg-person>i{width:43px;height:43px;display:grid;place-items:center;border-radius:50%;background:#1a315f;color:#dfe6ff;font-size:14px;font-style:normal;font-weight:800}.ct-pg-person span strong,.ct-pg-person span small{display:block}.ct-pg-person span strong{font-size:14px}.ct-pg-person span small{margin-top:5px;color:#717a89;font-size:9px}.ct-pg-task{display:flex;align-items:center;justify-content:space-between;height:48px;margin-top:10px;padding:0 13px;border:1px solid rgba(255,255,255,.08);border-radius:10px}.ct-pg-task span{color:#717a89;font-size:9px}.ct-pg-task b{font-size:10px}.ct-pg-task:first-of-type{border-color:rgba(53,207,140,.25);background:rgba(53,207,140,.07)}.ct-pg-task:first-of-type b{color:#5bdba6}
        @keyframes ctPgCursor{0%,18%{transform:translate(0,0)}45%,62%{transform:translate(-58px,-42px)}85%,100%{transform:translate(0,0)}}@keyframes ctPgTransfer{to{left:100%}}
        @media(max-width:1180px){.ct-pg-hero-grid,.ct-pg-connect-grid{grid-template-columns:1fr;gap:55px}.ct-pg-copy{text-align:center}.ct-pg-actions,.ct-pg-proof{justify-content:center}.ct-pg-desc br,.ct-pg-connect-copy>p br{display:none}.ct-pg-connect-copy{text-align:center}.ct-pg-flow-line{justify-content:center}.ct-pg-editor{max-width:900px;margin:0 auto}.ct-pg-connect-demo{max-width:900px;margin:0 auto}.ct-pg-hero{padding-top:150px}}
        @media(max-width:760px){.ct-pg-wrap{width:calc(100% - 32px)}.ct-pg-hero{min-height:auto;padding:122px 0 75px}.ct-pg-copy h1{font-size:53px}.ct-pg-desc{font-size:14px}.ct-pg-actions{flex-direction:column}.ct-pg-actions a{width:100%;justify-content:center}.ct-pg-proof{flex-wrap:wrap}.ct-pg-editor-body{grid-template-columns:1fr;min-height:0}.ct-pg-blocks,.ct-pg-settings{display:none}.ct-pg-canvas{padding:45px 18px 28px}.ct-pg-landing{padding:29px 22px}.ct-pg-landing h3{font-size:29px}.ct-pg-build,.ct-pg-connect{padding:95px 0}.ct-pg-section-head h2,.ct-pg-connect-copy h2{font-size:45px}.ct-pg-steps{display:flex;overflow-x:auto;gap:10px;margin-top:39px;padding:8px 2px 16px;scroll-snap-type:x mandatory}.ct-pg-steps>button{flex:0 0 82%;min-height:145px;scroll-snap-align:center}.ct-pg-steps>i{display:none}.ct-pg-build-screen{grid-template-columns:1fr;min-height:0}.ct-pg-template-list{display:flex;overflow-x:auto;gap:8px;padding:17px;border-right:0;border-bottom:1px solid rgba(255,255,255,.08)}.ct-pg-template-list article{flex:0 0 72%;margin:0}.ct-pg-build-preview{padding:58px 20px 80px}.ct-pg-build-preview h3{font-size:29px}.ct-pg-connect-demo{grid-template-columns:1fr;padding:18px}.ct-pg-transfer{height:58px;transform:rotate(90deg)}.ct-pg-lead-card,.ct-pg-call-card{min-height:300px}.ct-pg-connect-copy>a{justify-content:center}.ct-pg-connect-copy{text-align:center}}
        @media(prefers-reduced-motion:reduce){.ct-pg-cursor,.ct-pg-transfer>span:after{animation:none!important}}
      `;
      document.head.append(style);
    }

    const steps=intro.querySelector('.ct-pg-steps');
    const build=intro.querySelector('.ct-pg-build-screen');
    const templates=[...intro.querySelectorAll('.ct-pg-template-list article')];
    const status=intro.querySelector('.ct-pg-build-status span');
    const labels=['템플릿을 선택했습니다.','문구와 접수 항목을 수정했습니다.','페이지를 공개했습니다.'];
    let index=1;
    const activate=step=>{
      index=Number(step);
      steps.dataset.step=String(index);build.dataset.step=String(index);
      templates.forEach((item,i)=>item.classList.toggle('active',i===Math.min(index-1,templates.length-1)));
      if(status)status.textContent=labels[index-1];
    };
    steps.querySelectorAll('button').forEach(btn=>{
      btn.addEventListener('pointerenter',()=>activate(btn.dataset.step));
      btn.addEventListener('focus',()=>activate(btn.dataset.step));
      btn.addEventListener('click',()=>activate(btn.dataset.step));
    });
    if(!matchMedia('(prefers-reduced-motion: reduce)').matches)setInterval(()=>activate(index%3+1),2400);
  };

  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(mount),{once:true}):requestAnimationFrame(mount);
})();