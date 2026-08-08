(()=>{
  if(document.documentElement.dataset.ctMessageSimpleV2)return;
  document.documentElement.dataset.ctMessageSimpleV2='1';
  const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];

  const normalizeStory=()=>{
    qa('.ct-story-step[data-number="04"]').forEach(el=>el.remove());
    qa('.ct-story-status span').forEach(el=>el.textContent='/ 03');
    const number=q('#ctStoryNumber'),current=q('#ctStoryCurrent');
    if(number&&number.textContent.trim()==='04')number.textContent='03';
    if(current&&current.textContent.includes('PC에서'))current.textContent='안내문자와 후속문자 발송';
  };

  const run=()=>{
    const web=q('#web');if(web){web.hidden=true;web.setAttribute('aria-hidden','true');}
    qa('a[href="#web"]').forEach(link=>link.remove());
    normalizeStory();

    const section=q('#messages');if(!section||section.dataset.ctMessageSimple)return;
    section.dataset.ctMessageSimple='2';section.className='ad-section ct-auto-message-section';
    section.innerHTML=`
      <div class="wrap ct-auto-message-layout">
        <div class="ct-auto-message-copy"><p>자동문자</p><h2>통화가 끝나면,<br><span>자동문자 발송.</span></h2><div class="ct-auto-message-point"><i></i>저장한 문구를 통화 직후 자동으로 보냅니다.</div></div>
        <div class="ct-auto-message-preview" aria-label="자동문자 발송 예시">
          <div class="ct-auto-preview-top"><span><i></i>자동 발송 ON</span><em>방금 전</em></div>
          <div class="ct-auto-customer"><div class="ct-auto-avatar">김</div><div><strong>김민수 고객</strong><small>발신 통화 종료</small></div></div>
          <div class="ct-auto-bubble">김민수 고객님,<br>방금 안내드린 자료와 신청 방법을 보내드립니다.</div>
          <div class="ct-auto-result"><strong>자동문자 발송 완료</strong><span>템플릿 적용 · 중복 발송 방지</span></div>
        </div>
      </div>`;

    if(!q('style[data-ct-message-simple]')){const style=document.createElement('style');style.dataset.ctMessageSimple='2';style.textContent=`
      #web[hidden]{display:none!important}.ct-story-step[data-number="04"]{display:none!important}
      .ct-auto-message-section{padding:128px 0!important;background:#0b0d11!important;border-top:1px solid var(--line)!important;text-align:center!important}
      .ct-auto-message-layout{display:grid;grid-template-columns:1fr!important;max-width:980px!important;margin:auto!important;gap:54px!important;align-items:center}
      .ct-auto-message-copy{text-align:center!important}.ct-auto-message-copy>p{margin:0 0 18px;color:var(--blue-2);font-size:13px;font-weight:900}.ct-auto-message-copy h2{margin:0;font-size:clamp(52px,5.8vw,82px);line-height:1.02;letter-spacing:-.075em}.ct-auto-message-copy h2 span{color:var(--blue-2)}.ct-auto-message-point{display:flex;align-items:center;justify-content:center!important;gap:11px;margin-top:32px;color:#d9dde6;font-size:15px;font-weight:800}.ct-auto-message-point i{width:10px;height:10px;border-radius:50%;background:var(--blue);box-shadow:0 0 0 8px rgba(59,111,255,.12)}
      .ct-auto-message-preview{width:min(760px,100%)!important;margin:auto!important;padding:34px;border:1px solid rgba(59,111,255,.42);border-radius:28px;background:linear-gradient(145deg,rgba(59,111,255,.12),#12151b 42%);box-shadow:0 36px 100px rgba(0,0,0,.4),0 20px 70px rgba(59,111,255,.11);text-align:left!important}.ct-auto-preview-top{display:flex;align-items:center;justify-content:space-between;padding-bottom:22px;border-bottom:1px solid var(--line)}.ct-auto-preview-top span{display:flex;align-items:center;gap:9px;color:#c6d0ff;font-size:11px;font-weight:900}.ct-auto-preview-top span i{width:9px;height:9px;border-radius:50%;background:#32c879;box-shadow:0 0 0 7px rgba(50,200,121,.1)}.ct-auto-preview-top em{color:var(--muted-2);font-size:10px;font-style:normal;font-weight:800}.ct-auto-customer{display:flex;align-items:center;gap:14px;margin-top:24px}.ct-auto-avatar{width:50px;height:50px;display:grid;place-items:center;border-radius:50%;background:var(--blue-soft);color:#bdc8ff;font-weight:900}.ct-auto-customer strong{display:block;font-size:17px}.ct-auto-customer small{display:block;margin-top:6px;color:var(--muted-2);font-size:11px}
      .ct-auto-bubble{margin:30px 0 0 auto;max-width:86%;padding:22px 24px;border-radius:20px 20px 5px 20px;background:var(--blue);font-size:16px;font-weight:760;line-height:1.72;box-shadow:0 18px 44px rgba(59,111,255,.24);opacity:0;transform:translateY(18px)}.ct-auto-message-preview.sending .ct-auto-bubble{animation:ctMsgIn .6s ease forwards}.ct-auto-result{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-top:24px;padding:18px;border:1px solid rgba(50,200,121,.2);border-radius:15px;background:rgba(50,200,121,.07);opacity:.4}.ct-auto-result strong{color:#84e2ae;font-size:13px}.ct-auto-result span{color:var(--muted-2);font-size:10px;font-weight:800}.ct-auto-message-preview.sent .ct-auto-result{opacity:1!important}.ct-auto-message-preview.sent .ct-auto-result strong{color:#84e2ae!important}@keyframes ctMsgIn{to{opacity:1;transform:none}}
      @media(max-width:600px){.ct-auto-message-section{padding:88px 0!important}.ct-auto-message-copy h2{font-size:44px}.ct-auto-message-point{font-size:13px}.ct-auto-message-preview{padding:22px;border-radius:22px}.ct-auto-bubble{max-width:100%;padding:18px;font-size:14px}.ct-auto-result{align-items:flex-start;flex-direction:column}}
    `;document.head.append(style);}

    const preview=q('.ct-auto-message-preview',section),top=q('.ct-auto-preview-top span',preview),result=q('.ct-auto-result strong',preview);let timer=0,active=true;
    const play=()=>{if(!active)return;clearTimeout(timer);preview.classList.remove('sending','sent');if(top)top.lastChild.textContent=' 자동문자 전송 중';if(result)result.textContent='자동문자 전송 중';void preview.offsetWidth;preview.classList.add('sending');timer=setTimeout(()=>{if(!active)return;preview.classList.add('sent');if(top)top.lastChild.textContent=' 자동문자 발송 완료';if(result)result.textContent='자동문자 발송 완료';timer=setTimeout(play,2200);},1200);};
    play();window.addEventListener('pagehide',()=>{active=false;clearTimeout(timer);},{once:true});
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});else requestAnimationFrame(run);
})();