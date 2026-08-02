(()=>{
  if(document.documentElement.dataset.ctHorizontalGuard)return;
  document.documentElement.dataset.ctHorizontalGuard='1';

  const originalAddEventListener=window.addEventListener;

  window.addEventListener=function(type,listener,options){
    const source=document.currentScript?.src||'';
    const isLegacyHorizontalListener=(type==='scroll'||type==='resize')&&source.includes('calltag-horizontal-story.js');
    if(isLegacyHorizontalListener)return;
    return originalAddEventListener.call(this,type,listener,options);
  };
})();
