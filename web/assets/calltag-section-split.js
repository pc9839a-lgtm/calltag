(()=>{
  if(document.documentElement.dataset.ctStoryV2)return;
  document.documentElement.dataset.ctStoryV2='1';

  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];
  const make=(markup)=>{const t=document.createElement('template');t.innerHTML=markup.trim();return t.content.firstElementChild;};

  const run=()=>{
    const original=q('#what');
    if(!original||q('.ct-convert-section'))return;

    const convert=make(`
      <section class="ct-convert-section" id="what">
        <div class="wrap">
          <div class="ct-convert-head">
            <p>콜태그가 무엇인가요?</p>
            <h2>전화번호만 남는 통화기록을<br><span>다시 연락할 고객정보로.</span></h2>
          </div>
          <div class="ct-convert-stage" aria-label="통화기록이 고객정보로 바뀌는 예시">
            <article class="ct-record-card">
              <span>최근 통화</span>
              <strong>010-4821-7536</strong>
              <small>오늘 14:32 · 통화 4분 18초</small>
            </article>
            <div class="ct-convert-arrow" aria-hidden="true"><i></i><b>→</b></div>
            <article class="ct-customer-card">
              <div class="ct-customer-top">
                <em>김</em>
                <div><strong>김민수 고객</strong><small>통화 완료</small></div>
                <span>신규 고객</span>
              </div>
              <div class="ct-customer-meta">
                <div><small>상담 상태</small><b>견적 전달</b></div>
                <div><small>다음 연락</small><b>8월 3일 10:00</b></div>
                <div><small>오늘 할 일</small><b>견적 자료 보내기</b></div>
              </div>
            </article>
          </div>
        </div>
      </section>
    `);

    const story=make(`
      <section class="ct-story-section" id="how">
        <div class="wrap ct-story-layout">
          <aside class="ct-story-sticky">
            <p>통화 직후</p>
            <h2>통화가 끝나면,<br><span>태그만 하세요.</span></h2>
            <div class="ct-story-status">
              <strong id="ctStoryNumber">01</strong><span>/ 04</span>
            </div>
            <div class="ct-story-current" id="ctStoryCurrent">전화번호를 고객정보로 저장</div>
          </aside>

          <div class="ct-story-steps">
            <section class="ct-story-step is-active" data-number="01" data-title="전화번호를 고객정보로 저장">
              <div class="ct-step-label">01</div>
              <h3>전화번호를<br>고객정보로 저장</h3>
              <div class="ct-screen ct-screen-customer">
                <div class="ct-screen-head"><strong>김민수 고객</strong><span>방금 전</span></div>
                <div class="ct-screen-grid three">
                  <div><small>고객 구분</small><b>신규 고객</b></div>
                  <div><small>상담 상태</small><b>견적 전달</b></div>
                  <div><small>통화 결과</small><b>상담 완료</b></div>
                </div>
                <div class="ct-screen-save">고객정보 저장 완료</div>
              </div>
            </section>

            <section class="ct-story-step" data-number="02" data-title="재연락 날짜와 할 일 등록">
              <div class="ct-step-label">02</div>
              <h3>재연락 날짜와<br>할 일 등록</h3>
              <div class="ct-screen ct-screen-task">
                <div class="ct-date-card"><span>다음 연락</span><strong>8월 3일</strong><b>오전 10:00</b></div>
                <div class="ct-task-list">
                  <article><i></i><div><strong>견적 자료 보내기</strong><span>오늘 · 우선 처리</span></div></article>
                  <article><i></i><div><strong>고객에게 다시 전화</strong><span>8월 3일 오전 10:00</span></div></article>
                </div>
              </div>
            </section>

            <section class="ct-story-step" data-number="03" data-title="안내문자와 후속문자 발송">
              <div class="ct-step-label">03</div>
              <h3>안내문자와<br>후속문자 발송</h3>
              <div class="ct-screen ct-screen-message">
                <div class="ct-message-tabs"><span class="active">수신</span><span>발신</span><span>부재중</span><span>후속</span></div>
                <div class="ct-message-preview">
                  <div><strong>문자 미리보기</strong><span>수신 기본</span></div>
                  <p>김민수 고객님, 요청하신 견적 자료를 보내드립니다. 다음 연락은 8월 3일 오전 10시입니다.</p>
                  <button type="button">지금 보내기</button>
                </div>
              </div>
            </section>

            <section class="ct-story-step" data-number="04" data-title="PC에서 고객과 일정 확인">
              <div class="ct-step-label">04</div>
              <h3>PC에서 고객과<br>일정 확인</h3>
              <div class="ct-screen ct-screen-web">
                <div class="ct-web-top"><i></i><i></i><i></i></div>
                <div class="ct-web-body">
                  <aside><b>콜태그</b><span class="active">오늘 할 일</span><span>고객</span><span>캘린더</span></aside>
                  <main>
                    <div class="ct-web-title">오늘 할 일 <b>4</b></div>
                    <div class="ct-web-cards"><article><span>연락 예정</span><strong>김민수 고객</strong><small>오늘 10:00</small></article><article><span>자료 발송</span><strong>견적서 보내기</strong><small>우선 처리</small></article></div>
                  </main>
                </div>
              </div>
            </section>
          </div>
        </div>
      </section>
    `);

    original.replaceWith(convert,story);

    const style=document.createElement('style');
    style.dataset.ctStoryV2='1';
    style.textContent=`
      .ct-convert-section,.ct-story-section{position:relative;border-top:1px solid var(--line);overflow:clip}
      .ct-convert-section{padding:120px 0 132px;background:#0b0d11}
      .ct-convert-head{text-align:center;max-width:1080px;margin:0 auto}
      .ct-convert-head p,.ct-story-sticky>p{margin:0 0 18px;color:var(--blue-2);font-size:13px;font-weight:900}
      .ct-convert-head h2,.ct-story-sticky h2{margin:0;font-size:clamp(48px,6vw,82px);line-height:1.03;letter-spacing:-.075em}
      .ct-convert-head h2 span,.ct-story-sticky h2 span{color:var(--blue-2)}
      .ct-convert-stage{display:grid;grid-template-columns:.82fr 116px 1.18fr;align-items:center;gap:24px;max-width:1120px;margin:66px auto 0}
      .ct-record-card,.ct-customer-card{border:1px solid var(--line-strong);border-radius:24px;background:linear-gradient(145deg,#181b22,#111319);box-shadow:0 28px 80px rgba(0,0,0,.3)}
      .ct-record-card{padding:34px;opacity:.6;transform:translateX(24px);transition:.7s ease}
      .ct-record-card span{color:var(--muted-2);font-size:11px;font-weight:850}.ct-record-card strong{display:block;margin-top:30px;font-size:clamp(28px,3vw,42px)}.ct-record-card small{display:block;margin-top:12px;color:var(--muted-2)}
      .ct-convert-arrow{position:relative;height:42px;display:flex;align-items:center;justify-content:flex-end}.ct-convert-arrow:before{content:'';position:absolute;left:0;right:18px;height:2px;background:rgba(255,255,255,.09)}.ct-convert-arrow i{position:absolute;left:0;width:0;height:2px;background:var(--blue);box-shadow:0 0 18px rgba(59,111,255,.7)}.ct-convert-arrow b{position:relative;color:var(--blue-2);font-size:29px}
      .ct-customer-card{padding:28px;opacity:0;transform:translateX(40px) scale(.97);transition:.78s cubic-bezier(.2,.75,.2,1)}
      .ct-customer-top{display:flex;align-items:center;gap:13px;padding-bottom:20px;border-bottom:1px solid var(--line)}.ct-customer-top em{width:48px;height:48px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#bdc7ff;font-style:normal;font-weight:900}.ct-customer-top strong{display:block}.ct-customer-top small{display:block;margin-top:5px;color:var(--muted-2)}.ct-customer-top>span{margin-left:auto;padding:7px 10px;border-radius:999px;background:var(--blue-soft);color:#bdc7ff;font-size:9px;font-weight:900}
      .ct-customer-meta{display:grid;grid-template-columns:repeat(3,1fr);gap:9px;margin-top:18px}.ct-customer-meta div{padding:15px;border-radius:13px;background:#101319;opacity:0;transform:translateY(12px)}.ct-customer-meta small{display:block;color:var(--muted-2);font-size:9px}.ct-customer-meta b{display:block;margin-top:8px;font-size:12px}
      .ct-convert-section.is-visible .ct-record-card{opacity:1;transform:none}.ct-convert-section.is-visible .ct-convert-arrow i{animation:ctSweep 1s .3s ease forwards}.ct-convert-section.is-visible .ct-customer-card{opacity:1;transform:none;transition-delay:.72s}.ct-convert-section.is-visible .ct-customer-meta div{animation:ctInfo .45s ease forwards}.ct-convert-section.is-visible .ct-customer-meta div:nth-child(1){animation-delay:1.1s}.ct-convert-section.is-visible .ct-customer-meta div:nth-child(2){animation-delay:1.28s}.ct-convert-section.is-visible .ct-customer-meta div:nth-child(3){animation-delay:1.46s}
      @keyframes ctSweep{to{width:calc(100% - 18px)}}@keyframes ctInfo{to{opacity:1;transform:none}}
      .ct-story-section{background:#0e1015}
      .ct-story-layout{display:grid;grid-template-columns:minmax(340px,.72fr) minmax(0,1.28fr);gap:76px;align-items:start}
      .ct-story-sticky{position:sticky;top:112px;min-height:calc(100vh - 150px);display:flex;flex-direction:column;justify-content:center;padding:50px 0}
      .ct-story-sticky h2{font-size:clamp(48px,5.3vw,76px)}
      .ct-story-status{display:flex;align-items:baseline;gap:7px;margin-top:44px}.ct-story-status strong{color:var(--blue-2);font-size:54px;line-height:1}.ct-story-status span{color:var(--muted-2);font-size:14px;font-weight:800}
      .ct-story-current{margin-top:15px;max-width:330px;color:#dfe2e9;font-size:16px;font-weight:850;line-height:1.45}
      .ct-story-steps{padding:12vh 0}
      .ct-story-step{min-height:78vh;display:flex;flex-direction:column;justify-content:center;padding:70px 0;opacity:.3;transform:scale(.965);transition:.55s ease}
      .ct-story-step.is-active{opacity:1;transform:none}
      .ct-step-label{color:var(--blue-2);font-size:13px;font-weight:900}
      .ct-story-step h3{margin:16px 0 30px;font-size:clamp(37px,4vw,58px);line-height:1.06;letter-spacing:-.06em}
      .ct-screen{min-height:440px;padding:30px;border:1px solid var(--line-strong);border-radius:28px;background:linear-gradient(145deg,#171a20,#101217);box-shadow:0 35px 90px rgba(0,0,0,.4);transform:translateY(30px);transition:.65s cubic-bezier(.2,.75,.2,1)}
      .ct-story-step.is-active .ct-screen{transform:none;box-shadow:0 35px 100px rgba(59,111,255,.13)}
      .ct-screen-head{display:flex;align-items:center;justify-content:space-between;padding:20px;border:1px solid rgba(59,111,255,.45);border-radius:16px;background:var(--blue-soft)}.ct-screen-head span{padding:5px 8px;border-radius:999px;background:rgba(59,111,255,.18);color:#c4cdff;font-size:9px;font-weight:850}
      .ct-screen-grid{display:grid;gap:10px;margin-top:16px}.ct-screen-grid.three{grid-template-columns:repeat(3,1fr)}.ct-screen-grid div{padding:18px;border-radius:14px;background:#11141a}.ct-screen-grid small{display:block;color:var(--muted-2);font-size:9px}.ct-screen-grid b{display:block;margin-top:8px;font-size:13px}.ct-screen-save{margin-top:16px;padding:16px;border-radius:13px;background:rgba(50,200,121,.08);color:#84e2ae;font-size:12px;font-weight:850;text-align:center}
      .ct-screen-task{display:grid;grid-template-columns:.72fr 1.28fr;gap:14px}.ct-date-card{display:flex;flex-direction:column;justify-content:center;padding:28px;border:1px solid rgba(59,111,255,.38);border-radius:18px;background:var(--blue-soft)}.ct-date-card span{color:#c0caff;font-size:11px;font-weight:850}.ct-date-card strong{margin-top:20px;font-size:42px}.ct-date-card b{margin-top:8px;font-size:16px}.ct-task-list{display:grid;gap:12px}.ct-task-list article{display:flex;align-items:center;gap:13px;padding:21px;border:1px solid var(--line);border-radius:17px;background:#15181e}.ct-task-list i{width:10px;height:10px;border-radius:50%;background:var(--blue);box-shadow:0 0 0 7px rgba(59,111,255,.1)}.ct-task-list strong{display:block}.ct-task-list span{display:block;margin-top:7px;color:var(--muted-2);font-size:10px}
      .ct-message-tabs{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.ct-message-tabs span{padding:13px;border:1px solid var(--line);border-radius:11px;color:var(--muted-2);font-size:10px;font-weight:850;text-align:center}.ct-message-tabs .active{border-color:rgba(59,111,255,.5);background:var(--blue-soft);color:#fff}.ct-message-preview{margin-top:16px;padding:24px;border:1px solid rgba(59,111,255,.36);border-radius:18px;background:#15181e}.ct-message-preview>div{display:flex;justify-content:space-between}.ct-message-preview>div span{padding:5px 8px;border-radius:999px;background:var(--blue-soft);color:#c2ccff;font-size:9px}.ct-message-preview p{margin:24px 0 0 auto;max-width:82%;padding:18px;border-radius:16px 16px 4px 16px;background:var(--blue);font-size:13px;line-height:1.65}.ct-message-preview button{width:100%;min-height:48px;margin-top:20px;border:0;border-radius:12px;background:var(--blue);color:#fff;font-weight:900}
      .ct-screen-web{padding:0;overflow:hidden}.ct-web-top{height:45px;display:flex;align-items:center;gap:7px;padding:0 17px;border-bottom:1px solid var(--line);background:#181b22}.ct-web-top i{width:8px;height:8px;border-radius:50%;background:#4b505b}.ct-web-body{display:grid;grid-template-columns:145px 1fr;min-height:395px}.ct-web-body aside{display:flex;flex-direction:column;gap:8px;padding:22px 15px;border-right:1px solid var(--line)}.ct-web-body aside b{margin-bottom:18px}.ct-web-body aside span{padding:11px;border-radius:9px;color:var(--muted-2);font-size:10px}.ct-web-body aside .active{background:var(--blue-soft);color:#b9c5ff}.ct-web-body main{padding:28px}.ct-web-title{font-size:26px;font-weight:900}.ct-web-title b{color:var(--blue-2)}.ct-web-cards{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:24px}.ct-web-cards article{padding:22px;border:1px solid var(--line);border-radius:16px;background:#15181e}.ct-web-cards span{color:var(--blue-2);font-size:9px;font-weight:850}.ct-web-cards strong{display:block;margin-top:13px}.ct-web-cards small{display:block;margin-top:8px;color:var(--muted-2)}
      @media(max-width:960px){.ct-convert-stage{grid-template-columns:1fr}.ct-convert-arrow{width:72px;margin:0 auto;transform:rotate(90deg)}.ct-customer-meta{grid-template-columns:1fr}.ct-story-layout{grid-template-columns:1fr;gap:0}.ct-story-sticky{position:relative;top:auto;min-height:auto;padding:90px 0 20px}.ct-story-steps{padding:0 0 70px}.ct-story-step{min-height:auto;padding:62px 0;opacity:1;transform:none}.ct-story-status,.ct-story-current{display:none}}
      @media(max-width:600px){.ct-convert-section{padding:86px 0 94px}.ct-convert-head h2,.ct-story-sticky h2{font-size:43px}.ct-convert-stage{margin-top:44px}.ct-record-card,.ct-customer-card{padding:22px}.ct-customer-top{flex-wrap:wrap}.ct-customer-top>span{margin-left:0}.ct-screen{min-height:0;padding:18px;border-radius:21px}.ct-screen-grid.three,.ct-screen-task,.ct-web-cards{grid-template-columns:1fr}.ct-message-tabs{grid-template-columns:repeat(2,1fr)}.ct-message-preview p{max-width:100%}.ct-web-body{grid-template-columns:92px 1fr}.ct-web-body main{padding:18px}.ct-story-step h3{font-size:38px}}
      @media(prefers-reduced-motion:reduce){.ct-record-card,.ct-customer-card,.ct-customer-meta div,.ct-story-step,.ct-screen{opacity:1!important;transform:none!important;animation:none!important}.ct-convert-arrow i{width:calc(100% - 18px)!important}}
    `;
    document.head.append(style);

    const number=q('#ctStoryNumber',story);
    const current=q('#ctStoryCurrent',story);
    const steps=qa('.ct-story-step',story);

    const activate=(step)=>{
      steps.forEach(item=>item.classList.toggle('is-active',item===step));
      if(number)number.textContent=step.dataset.number;
      if(current)current.textContent=step.dataset.title;
    };

    const storyObserver=new IntersectionObserver((entries)=>{
      const visible=entries.filter(entry=>entry.isIntersecting).sort((a,b)=>b.intersectionRatio-a.intersectionRatio)[0];
      if(visible)activate(visible.target);
    },{threshold:[.35,.5,.65],rootMargin:'-12% 0px -18% 0px'});
    steps.forEach(step=>storyObserver.observe(step));

    const convertObserver=new IntersectionObserver((entries)=>{
      entries.forEach(entry=>{if(entry.isIntersecting)entry.target.classList.add('is-visible');});
    },{threshold:.3});
    convertObserver.observe(convert);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});
  else requestAnimationFrame(run);
})();