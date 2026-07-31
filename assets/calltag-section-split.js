(()=>{
  if(document.documentElement.dataset.ctSplitWhat)return;
  document.documentElement.dataset.ctSplitWhat='1';

  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];
  const make=(html)=>{const t=document.createElement('template');t.innerHTML=html.trim();return t.content.firstElementChild;};

  const run=()=>{
    const original=q('#what');
    if(!original||q('.ct-convert-section'))return;

    const existingUi=q('.ad-value-ui',original);

    const convert=make(`
      <section class="ct-convert-section" id="what">
        <div class="wrap">
          <div class="ct-split-head">
            <p>콜태그가 무엇인가요?</p>
            <h2>전화번호만 남는 통화기록을<br><span>다시 연락할 고객정보로.</span></h2>
          </div>
          <div class="ct-convert-stage" aria-label="통화기록이 고객정보로 변환되는 예시">
            <article class="ct-record-card">
              <div class="ct-card-label"><i></i>최근 통화</div>
              <strong>010-4821-7536</strong>
              <span>오늘 14:32 · 통화 4분 18초</span>
            </article>
            <div class="ct-convert-line" aria-hidden="true"><span></span><b>→</b></div>
            <article class="ct-profile-card">
              <div class="ct-profile-top">
                <div class="ct-profile-avatar">김</div>
                <div><strong>김민수 고객</strong><span>통화 완료</span></div>
                <em>신규 고객</em>
              </div>
              <div class="ct-profile-info">
                <div><small>상담 상태</small><b>견적 전달</b></div>
                <div><small>다음 연락</small><b>8월 3일 10:00</b></div>
                <div><small>오늘 할 일</small><b>견적 자료 보내기</b></div>
              </div>
            </article>
          </div>
        </div>
      </section>
    `);

    const actions=make(`
      <section class="ct-action-section" id="how">
        <div class="wrap ct-action-grid">
          <div class="ct-action-copy">
            <p class="ct-action-kicker">통화 직후 바로 입력</p>
            <h2>통화 다음에 해야 할 일,<br><span>바로 남기세요.</span></h2>
            <div class="ct-action-list">
              <div class="ct-action-item is-active"><b>01</b><strong>전화번호를 고객정보로 저장</strong></div>
              <div class="ct-action-item"><b>02</b><strong>재연락 날짜와 할 일 등록</strong></div>
              <div class="ct-action-item"><b>03</b><strong>안내문자와 후속문자 발송</strong></div>
              <div class="ct-action-item"><b>04</b><strong>PC에서 고객과 일정 확인</strong></div>
            </div>
          </div>
          <div class="ct-action-ui"></div>
        </div>
      </section>
    `);

    const uiHost=q('.ct-action-ui',actions);
    if(existingUi){
      uiHost.append(existingUi);
    }else{
      uiHost.append(make(`
        <div class="ad-value-ui">
          <div class="ad-customer">
            <div class="ad-customer-top"><strong>김민수 고객 · 통화 완료</strong><span>방금 전</span></div>
            <div class="ad-meta"><div><small>고객 구분</small><b>신규 고객</b></div><div><small>상담 상태</small><b>견적 전달</b></div><div><small>다음 연락</small><b>8월 3일 10:00</b></div></div>
          </div>
          <div class="ad-result"><article><span>오늘 할 일</span><strong>견적 자료 보내기</strong><p>기한과 우선순위 표시</p></article><div class="ad-arrow">→</div><article><span>안내문자</span><strong>저장 문구 선택</strong><p>고객명과 날짜 적용</p></article></div>
        </div>
      `));
    }

    original.replaceWith(convert,actions);

    const style=document.createElement('style');
    style.dataset.ctSectionSplit='1';
    style.textContent=`
      .ct-convert-section,.ct-action-section{position:relative;overflow:hidden;border-top:1px solid var(--line)}
      .ct-convert-section{padding:118px 0 124px;background:#0b0d11}
      .ct-action-section{padding:124px 0;background:#0e1015}
      .ct-split-head{max-width:1060px;margin:0 auto;text-align:center}
      .ct-split-head p,.ct-action-kicker{margin:0 0 18px;color:var(--blue-2);font-size:13px;font-weight:900}
      .ct-split-head h2,.ct-action-copy h2{margin:0;font-size:clamp(48px,6vw,82px);line-height:1.03;letter-spacing:-.075em}
      .ct-split-head h2 span,.ct-action-copy h2 span{color:var(--blue-2)}
      .ct-convert-stage{display:grid;grid-template-columns:.85fr 120px 1.15fr;align-items:center;gap:24px;max-width:1120px;margin:64px auto 0}
      .ct-record-card,.ct-profile-card{border:1px solid var(--line-strong);border-radius:24px;background:linear-gradient(145deg,#181b22,#111319);box-shadow:0 28px 80px rgba(0,0,0,.3)}
      .ct-record-card{padding:34px;opacity:.56;transform:translateX(22px);transition:.7s ease}
      .ct-card-label{display:flex;align-items:center;gap:9px;color:var(--muted-2);font-size:11px;font-weight:850}
      .ct-card-label i{width:8px;height:8px;border-radius:50%;background:#666c78}
      .ct-record-card strong{display:block;margin-top:34px;font-size:clamp(28px,3vw,42px);letter-spacing:-.045em}
      .ct-record-card span{display:block;margin-top:13px;color:var(--muted-2);font-size:12px}
      .ct-convert-line{position:relative;height:44px;display:flex;align-items:center;justify-content:flex-end}
      .ct-convert-line:before{content:'';position:absolute;left:0;right:20px;height:2px;background:rgba(255,255,255,.09)}
      .ct-convert-line span{position:absolute;left:0;width:0;height:2px;background:var(--blue);box-shadow:0 0 18px rgba(59,111,255,.7)}
      .ct-convert-line b{position:relative;color:var(--blue-2);font-size:30px}
      .ct-profile-card{padding:28px;opacity:0;transform:translateX(36px) scale(.97);transition:.75s cubic-bezier(.2,.75,.2,1)}
      .ct-profile-top{display:flex;align-items:center;gap:13px;padding-bottom:21px;border-bottom:1px solid var(--line)}
      .ct-profile-avatar{width:48px;height:48px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#bdc7ff;font-size:13px;font-weight:900}
      .ct-profile-top strong{display:block;font-size:17px}.ct-profile-top span{display:block;margin-top:5px;color:var(--muted-2);font-size:10px}
      .ct-profile-top em{margin-left:auto;padding:7px 10px;border-radius:999px;background:var(--blue-soft);color:#b8c5ff;font-size:9px;font-style:normal;font-weight:900}
      .ct-profile-info{display:grid;grid-template-columns:repeat(3,1fr);gap:9px;margin-top:18px}
      .ct-profile-info div{padding:15px;border-radius:13px;background:#101319;opacity:0;transform:translateY(12px)}
      .ct-profile-info small{display:block;color:var(--muted-2);font-size:9px}.ct-profile-info b{display:block;margin-top:8px;font-size:12px}
      .ct-convert-section.is-visible .ct-record-card{opacity:1;transform:none}
      .ct-convert-section.is-visible .ct-convert-line span{animation:ctSweep 1s .35s ease forwards}
      .ct-convert-section.is-visible .ct-profile-card{opacity:1;transform:none;transition-delay:.72s}
      .ct-convert-section.is-visible .ct-profile-info div{animation:ctInfoIn .48s ease forwards}
      .ct-convert-section.is-visible .ct-profile-info div:nth-child(1){animation-delay:1.15s}.ct-convert-section.is-visible .ct-profile-info div:nth-child(2){animation-delay:1.32s}.ct-convert-section.is-visible .ct-profile-info div:nth-child(3){animation-delay:1.49s}
      .ct-convert-section.is-visible .ct-profile-card{animation:ctProfileGlow 3.2s 1.7s ease-in-out infinite}
      @keyframes ctSweep{to{width:calc(100% - 20px)}}@keyframes ctInfoIn{to{opacity:1;transform:none}}@keyframes ctProfileGlow{0%,100%{box-shadow:0 28px 80px rgba(0,0,0,.3)}50%{box-shadow:0 28px 90px rgba(59,111,255,.18)}}
      .ct-action-grid{display:grid;grid-template-columns:.82fr 1.18fr;gap:66px;align-items:center}
      .ct-action-copy h2{font-size:clamp(43px,5vw,70px)}
      .ct-action-list{display:grid;gap:10px;margin-top:38px}
      .ct-action-item{min-height:66px;display:flex;align-items:center;gap:15px;padding:0 17px;border:1px solid var(--line);border-radius:14px;background:#15181e;transition:.4s ease}
      .ct-action-item b{width:34px;height:34px;display:grid;place-items:center;border-radius:10px;background:#1d2028;color:#6f7581;font-size:10px;transition:.4s ease}
      .ct-action-item strong{font-size:14px}.ct-action-item.is-active{border-color:rgba(59,111,255,.5);background:var(--blue-soft);transform:translateX(8px)}
      .ct-action-item.is-active b{background:var(--blue);color:#fff;box-shadow:0 0 0 7px rgba(59,111,255,.1)}
      .ct-action-ui>.ad-value-ui{width:100%;min-height:430px;padding:30px;border:1px solid var(--line-strong);border-radius:26px;background:linear-gradient(145deg,#171a20,#101217);box-shadow:0 34px 90px rgba(0,0,0,.35)}
      .ct-action-ui .ad-customer,.ct-action-ui .ad-result article{transition:.42s ease}
      .ct-action-ui .ct-ui-active{border-color:rgba(59,111,255,.55)!important;background:var(--blue-soft)!important;transform:translateY(-5px);box-shadow:0 18px 44px rgba(59,111,255,.13)}
      @media(max-width:950px){.ct-convert-stage{grid-template-columns:1fr}.ct-convert-line{width:72px;margin:0 auto;transform:rotate(90deg)}.ct-action-grid{grid-template-columns:1fr;gap:42px}.ct-profile-info{grid-template-columns:1fr}}
      @media(max-width:600px){.ct-convert-section,.ct-action-section{padding:86px 0}.ct-split-head h2,.ct-action-copy h2{font-size:43px}.ct-convert-stage{margin-top:44px}.ct-record-card,.ct-profile-card{padding:22px}.ct-profile-top{align-items:flex-start;flex-wrap:wrap}.ct-profile-top em{margin-left:0}.ct-action-ui>.ad-value-ui{min-height:0;padding:18px}.ct-action-item{min-height:60px}.ct-action-item strong{font-size:12px}}
      @media(prefers-reduced-motion:reduce){.ct-record-card,.ct-profile-card,.ct-profile-info div{opacity:1!important;transform:none!important;animation:none!important}.ct-convert-line span{width:calc(100% - 20px)!important;animation:none!important}}
    `;
    document.head.append(style);

    const observer=new IntersectionObserver((entries)=>{
      entries.forEach(entry=>{if(entry.isIntersecting)entry.target.classList.add('is-visible');});
    },{threshold:.28});
    observer.observe(convert);
    observer.observe(actions);

    const list=qa('.ct-action-item',actions);
    const uiTargets=qa('.ad-customer,.ad-result article',actions);
    let index=0;
    const activate=(next)=>{
      index=next%list.length;
      list.forEach((el,i)=>el.classList.toggle('is-active',i===index));
      uiTargets.forEach(el=>el.classList.remove('ct-ui-active'));
      if(uiTargets.length)uiTargets[index%uiTargets.length].classList.add('ct-ui-active');
    };
    activate(0);
    if(!matchMedia('(prefers-reduced-motion: reduce)').matches){
      setInterval(()=>activate(index+1),1900);
    }
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});
  else requestAnimationFrame(run);
})();
