(()=>{
  if(document.documentElement.dataset.ctHeroRestoreV1)return;
  document.documentElement.dataset.ctHeroRestoreV1='1';

  let applying=false;

  const installStyle=()=>{
    if(document.getElementById('ct-hero-restore-v1-style'))return;
    const style=document.createElement('style');
    style.id='ct-hero-restore-v1-style';
    style.textContent=`
      #app.hero-app{padding-bottom:120px!important}
      #app .ct-live-hero-actions{display:none!important}
      #app .hero-heading.ct-live-hero-enter h1,
      #app .hero-heading.ct-live-hero-enter>p:last-of-type,
      #app .hero-heading.ct-live-hero-enter .ct-live-hero-actions{animation:none!important}
      @media(max-width:560px){#app.hero-app{padding-bottom:72px!important}}
    `;
    document.head.append(style);
  };

  const patch=()=>{
    const app=document.getElementById('app');
    const heading=app?.querySelector('.hero-heading');
    if(!heading)return false;

    heading.querySelectorAll('.hero-kicker,.ct-live-hero-actions,.ad-actions,.ad-offer').forEach(node=>node.remove());

    const title=heading.querySelector('h1');
    const description=heading.querySelector(':scope > p:last-of-type');
    const originalTitle='통화 후 <span>고객관리.</span>';
    const originalDescription='고객·일정·문자를 통화 직후 남기세요.';

    if(title&&title.innerHTML!==originalTitle)title.innerHTML=originalTitle;
    if(description&&description.textContent.trim()!==originalDescription)description.textContent=originalDescription;

    heading.classList.remove('ct-live-hero-enter');
    delete heading.dataset.ctHeroAnimated;
    return true;
  };

  const apply=()=>{
    if(applying)return;
    applying=true;
    try{installStyle();patch();}
    finally{applying=false;}
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  [80,220,500,900,1500,2400,3600,5200,7000,8200].forEach(delay=>setTimeout(apply,delay));
  const observer=new MutationObserver(()=>requestAnimationFrame(apply));
  observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true});
  setTimeout(()=>observer.disconnect(),9000);
})();
