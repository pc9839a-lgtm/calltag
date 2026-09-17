(()=>{
  if(document.documentElement.dataset.ctHeroRestoreV2)return;
  document.documentElement.dataset.ctHeroRestoreV2='1';

  const installStyle=()=>{
    let style=document.getElementById('ct-hero-restore-v2-style');
    if(!style){
      style=document.createElement('style');
      style.id='ct-hero-restore-v2-style';
      document.head.append(style);
    }
    style.textContent=`
      #app.hero-app{padding-bottom:120px!important}
      #app .ct-original-hero-heading{text-align:center;position:relative;z-index:1}
      #app .ct-original-hero-heading .hero-kicker{display:block!important;margin:0 0 20px!important;color:var(--blue-2)!important;font-size:18px!important;font-weight:900!important;letter-spacing:-.03em!important}
      #app .ct-original-hero-heading h1{margin:0!important;font-size:clamp(62px,8.2vw,118px)!important;line-height:.94!important;letter-spacing:-.085em!important}
      #app .ct-original-hero-heading h1 span{color:var(--blue-2)!important}
      #app .ct-original-hero-heading>p:last-child{margin:26px auto 0!important;max-width:720px!important;color:var(--muted)!important;font-size:clamp(17px,1.75vw,22px)!important;line-height:1.55!important;letter-spacing:-.025em!important}
      #app .ct-live-hero-actions{display:none!important}
      @media(max-width:820px){#app .ct-original-hero-heading h1{font-size:clamp(56px,15vw,86px)!important}#app .ct-original-hero-heading>p:last-child{font-size:17px!important}}
      @media(max-width:560px){#app.hero-app{padding-bottom:72px!important}#app .ct-original-hero-heading .hero-kicker{font-size:15px!important}#app .ct-original-hero-heading h1{font-size:53px!important}#app .ct-original-hero-heading>p:last-child{max-width:330px!important;font-size:16px!important}}
    `;
  };

  const patch=()=>{
    const app=document.getElementById('app');
    if(!app)return false;

    let heading=app.querySelector('.ct-original-hero-heading')||app.querySelector('.hero-heading');
    if(!heading)return false;

    heading.classList.remove('hero-heading','ct-live-hero-enter');
    heading.classList.add('ct-original-hero-heading');
    delete heading.dataset.ctHeroAnimated;

    const markup='<p class="hero-kicker">통화 후 고객관리</p><h1>1명의 고객도<br><span>놓치지 않습니다.</span></h1><p>통화가 끝난 뒤 고객을 태그하면 상담 상태와 다음 할 일이 바로 정리됩니다.</p>';
    if(heading.innerHTML!==markup)heading.innerHTML=markup;

    app.querySelectorAll('.ct-live-hero-actions').forEach(node=>node.remove());
    return true;
  };

  const apply=()=>{installStyle();patch();};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});else apply();
  [80,220,500,900,1500,2400,3600,5200,7200,9000].forEach(delay=>setTimeout(apply,delay));
})();
