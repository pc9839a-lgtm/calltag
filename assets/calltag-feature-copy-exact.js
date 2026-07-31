(()=>{
  if(document.documentElement.dataset.ctFeatureCopyExact)return;
  document.documentElement.dataset.ctFeatureCopyExact='1';
  const sections={history:{title:'고객마다<br><span>기억할 필요 없습니다.</span>',copy:'누구와 언제 통화했고, 상담이 어디까지 진행됐고, 다음에 무엇을 해야 하는지 고객별로 확인합니다.',callout:'한눈에 보이는 상담이력'},calendar:{title:'일정은<br><span>달력에 모입니다.</span>',copy:'재연락, 자료 발송, 방문 약속을 날짜별로 확인하고 고객 상담 이력까지 바로 연결합니다.',callout:'모든 일정 정리는 콜태그에서!'}};
  const apply=()=>{
    const taskCopy=document.querySelector('#tasks .feature-copy');
    if(taskCopy)taskCopy.innerHTML='<h3 class="ct-feature-only-title">오늘 할 일을<br><span>바로 확인하세요.</span></h3>';
    Object.entries(sections).forEach(([id,data])=>{const section=document.getElementById(id),copyBox=section&&section.querySelector('.feature-copy');if(copyBox)copyBox.innerHTML=`<h3>${data.title}</h3><p>${data.copy}</p><div class="ct-feature-exact-callout">${data.callout}</div>`;});
    if(!document.querySelector('style[data-ct-feature-copy-exact]')){const style=document.createElement('style');style.dataset.ctFeatureCopyExact='1';style.textContent='.ct-feature-only-title span{color:var(--blue-2)}.ct-feature-exact-callout{display:inline-flex;align-items:center;gap:10px;margin-top:28px;padding:14px 18px;border:1px solid rgba(59,111,255,.38);border-radius:12px;background:rgba(59,111,255,.12);color:#eef1ff;font-size:15px;font-weight:900}.ct-feature-exact-callout:before{content:"→";color:var(--blue-2);font-size:19px}@media(max-width:900px){.ct-feature-only-title{text-align:center}.ct-feature-exact-callout{justify-content:center;margin-top:20px;padding:12px 14px;font-size:13px}}';document.head.append(style);}
  };
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',apply,{once:true}):requestAnimationFrame(apply);
})();