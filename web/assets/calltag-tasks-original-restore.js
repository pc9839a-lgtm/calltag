(()=>{
  if(document.documentElement.dataset.ctTasksOriginalRestore)return;
  document.documentElement.dataset.ctTasksOriginalRestore='1';

  const original=`
    <div class="feature-copy"><h3>놓친 일부터<br>먼저 보입니다.</h3><p>기한이 지난 재연락, 오늘 연락할 고객, 보내지 않은 자료를 우선순위대로 보여줍니다.</p><div class="feature-points" id="taskPoints"><div class="point-card active"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 4h4l2 5-3 2c1.5 3 3 4.5 6 6l2-3 5 2v4c0 1-1 2-2 2C9 22 2 15 2 6c0-1 1-2 3-2Z"/></svg></div><div><strong>전화·문자·미루기·완료</strong><span>업무마다 바로 처리</span></div></div><div class="point-card"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 8v5l3 2M4 4l3 3M20 4l-3 3"/><circle cx="12" cy="13" r="8"/></svg></div><div><strong>기한 초과는 따로 표시</strong><span>늦은 업무가 위로 올라옴</span></div></div><div class="point-card"><div class="point-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5v14"/></svg></div><div><strong>끝내기 전에는 사라지지 않음</strong><span>미처리 업무를 계속 유지</span></div></div></div></div>
    <div class="product-panel"><div class="today-content"><div class="today-header"><h4>오늘 할 일</h4><span>7월 30일 목요일 · 5건</span></div><div class="today-card overdue" data-task="0"><div class="today-time">어제</div><div class="today-main"><strong>박지훈 고객 방문 확인</strong><p>약속 시간을 다시 확인해야 합니다.</p></div><div class="today-actions"><span class="ui-action" data-action="0">전화</span><span class="ui-action" data-action="1">미루기</span><span class="ui-action done" data-action="2">완료</span></div></div><div class="today-card" data-task="1"><div class="today-time">10:30</div><div class="today-main"><strong>김민수 고객 자료 발송</strong><p>견적서와 소개자료 이메일 발송</p></div><div class="today-actions"><span class="ui-action" data-action="0">문자</span><span class="ui-action" data-action="1">미루기</span><span class="ui-action done" data-action="2">완료</span></div></div><div class="today-card" data-task="2"><div class="today-time">14:00</div><div class="today-main"><strong>이서연 고객 재연락</strong><p>계약 검토 결과 확인</p></div><div class="today-actions"><span class="ui-action" data-action="0">전화</span><span class="ui-action" data-action="1">미루기</span><span class="ui-action done" data-action="2">완료</span></div></div><div class="today-card" data-task="3"><div class="today-time">16:20</div><div class="today-main"><strong>최준호 고객 상담 정리</strong><p>통화 후 상담 결과 미입력</p></div><div class="today-actions"><span class="ui-action" data-action="0">정리</span><span class="ui-action done" data-action="1">완료</span></div></div></div><div class="task-toast" id="taskToast">박지훈 고객에게 전화 연결</div></div>
  `;

  const style=document.createElement('style');
  style.dataset.ctTasksOriginalRestore='1';
  style.textContent=`
    #tasks.ct-tasks-original-restored,
    #tasks.ct-tasks-original-restored .feature-block,
    #tasks.ct-tasks-original-restored .feature-copy,
    #tasks.ct-tasks-original-restored .product-panel,
    #tasks.ct-tasks-original-restored .today-content{
      opacity:1!important;
      visibility:visible!important;
      filter:none!important;
      transform:none!important;
      translate:none!important;
      scale:none!important;
      mix-blend-mode:normal!important;
      clip-path:none!important;
      animation:none!important;
    }
    #tasks.ct-tasks-original-restored:before,
    #tasks.ct-tasks-original-restored:after,
    #tasks.ct-tasks-original-restored .feature-block:before,
    #tasks.ct-tasks-original-restored .feature-block:after,
    #tasks.ct-tasks-original-restored .product-panel:after{
      display:none!important;
      content:none!important;
      opacity:0!important;
    }
    #tasks.ct-tasks-original-restored{
      background:#090a0d!important;
      background-image:none!important;
      color:var(--text)!important;
    }
    #tasks.ct-tasks-original-restored .feature-block{
      display:grid!important;
      grid-template-columns:.72fr 1.28fr!important;
      gap:70px!important;
      align-items:center!important;
    }
    #tasks.ct-tasks-original-restored .product-panel{
      position:relative!important;
      min-width:0!important;
      padding:28px!important;
      border:1px solid var(--line-strong)!important;
      border-radius:28px!important;
      background:linear-gradient(145deg,#171a20,#101217)!important;
      box-shadow:0 30px 84px rgba(0,0,0,.34)!important;
      overflow:hidden!important;
    }
    @media(max-width:1120px){
      #tasks.ct-tasks-original-restored .feature-block{grid-template-columns:1fr!important;gap:46px!important}
    }
  `;
  document.head.append(style);

  const restore=()=>{
    const section=document.querySelector('#tasks');
    const block=section?.querySelector(':scope > .wrap > .feature-block:first-of-type');
    if(!section||!block)return false;

    section.classList.add('ct-tasks-original-restored');
    section.classList.remove('ct-motion-section','is-motion-visible');
    block.className='feature-block reveal';
    block.removeAttribute('data-ct-motion');
    block.removeAttribute('style');

    const normalized=(block.textContent||'').replace(/\s+/g,'');
    if(!normalized.includes('놓친일부터먼저보입니다')||!block.querySelector('.today-card')){
      block.innerHTML=original;
    }

    block.querySelectorAll('[data-ct-motion],.ct-motion-panel,.ct-motion-card').forEach(node=>{
      node.removeAttribute('data-ct-motion');
      node.classList.remove('ct-motion-panel','ct-motion-card','is-visible','visible');
      node.style.removeProperty('--ct-delay');
      node.style.removeProperty('opacity');
      node.style.removeProperty('filter');
      node.style.removeProperty('transform');
      node.style.removeProperty('translate');
      node.style.removeProperty('scale');
      node.style.removeProperty('clip-path');
    });

    return true;
  };

  const boot=()=>{
    restore();
    const observer=new MutationObserver(restore);
    observer.observe(document.documentElement,{childList:true,subtree:true,characterData:true});
    [100,300,700,1500,3000,6000].forEach(delay=>setTimeout(restore,delay));
    setTimeout(()=>observer.disconnect(),10000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
