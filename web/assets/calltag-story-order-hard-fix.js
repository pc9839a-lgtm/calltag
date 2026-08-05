(()=>{
  if(document.documentElement.dataset.ctStoryOrderHardFix)return;
  document.documentElement.dataset.ctStoryOrderHardFix='1';

  const findStory=()=>{
    const direct=document.querySelector('#how.ct-story-section');
    if(direct)return direct;
    return [...document.querySelectorAll('.ct-story-section')].find(section=>{
      const text=(section.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('통화가 끝나면')&&text.includes('태그만 하세요')&&text.includes('안내문자와 후속문자 발송');
    })||null;
  };

  const pin=()=>{
    const main=document.querySelector('main#top');
    const app=document.querySelector('#app');
    const story=findStory();
    if(!main||!app||!story||app.parentElement!==main)return false;

    if(story.parentElement!==main||app.nextElementSibling!==story){
      app.insertAdjacentElement('afterend',story);
    }
    story.dataset.ctPinnedAfterApp='1';
    return true;
  };

  const boot=()=>{
    pin();
    const observer=new MutationObserver(()=>requestAnimationFrame(pin));
    observer.observe(document.documentElement,{childList:true,subtree:true});

    const timer=setInterval(pin,250);
    setTimeout(()=>clearInterval(timer),30000);
    [0,50,100,200,400,800,1200,2000,3500,5000,8000,12000,18000,25000].forEach(delay=>setTimeout(pin,delay));
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
