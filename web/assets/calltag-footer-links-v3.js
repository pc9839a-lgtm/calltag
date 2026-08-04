(()=>{
  if(document.documentElement.dataset.ctFooterLinksV4)return;
  document.documentElement.dataset.ctFooterLinksV4='1';

  const destinations={
    '이용약관':'/terms/',
    '개인정보처리방침':'/privacy/',
    '환불정책':'/refund/',
    '고객센터':'/support/'
  };

  const apply=()=>{
    const footer=document.querySelector('.ct-wayzi-footer,footer');
    if(!footer)return;

    footer.querySelectorAll('a').forEach(anchor=>{
      const label=(anchor.textContent||'').replace(/\s+/g,' ').trim();
      if(destinations[label])anchor.setAttribute('href',destinations[label]);
      if((anchor.getAttribute('href')||'').startsWith('tel:'))anchor.remove();
    });

    footer.innerHTML=footer.innerHTML
      .replace(/<a[^>]*href="tel:[^"]*"[^>]*>[\s\S]*?<\/a>/gi,'')
      .replace(/010[-\s]?5766[-\s]?9839/g,'');
  };

  let queued=false;
  const queue=()=>{
    if(queued)return;
    queued=true;
    requestAnimationFrame(()=>{queued=false;apply()});
  };

  const boot=()=>{
    apply();
    const observer=new MutationObserver(queue);
    observer.observe(document.documentElement,{childList:true,subtree:true});
    [100,400,1000,2500,5000].forEach(delay=>setTimeout(apply,delay));
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
