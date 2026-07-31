(()=>{
  if(document.documentElement.dataset.ctFeatureCopyExact)return;
  document.documentElement.dataset.ctFeatureCopyExact='1';
  const titles={tasks:'오늘 할 일을<br><span>바로 확인하세요.</span>',history:'한눈에 보이는<br><span>상담이력</span>',calendar:'모든 일정 정리는<br><span>콜태그에서!</span>'};
  const apply=()=>{Object.entries(titles).forEach(([id,title])=>{const copyBox=document.querySelector(`#${id} .feature-copy`);if(copyBox)copyBox.innerHTML=`<h3 class="ct-feature-only-title">${title}</h3>`;});if(!document.querySelector('style[data-ct-feature-copy-exact]')){const style=document.createElement('style');style.dataset.ctFeatureCopyExact='1';style.textContent='.ct-feature-only-title span{color:var(--blue-2)}@media(max-width:900px){.ct-feature-only-title{text-align:center}}';document.head.append(style);}};
  document.readyState==='loading'?document.addEventListener('DOMContentLoaded',apply,{once:true}):requestAnimationFrame(apply);
})();