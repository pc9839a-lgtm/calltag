(()=>{
  if(document.documentElement.dataset.ctStrengthAnimation)return;
  document.documentElement.dataset.ctStrengthAnimation='1';

  const run=()=>{
    const section=document.querySelector('#strengths');
    if(!section)return;

    const title=section.querySelector('.ad-title');
    if(title)title.innerHTML='놓치지 않고,<br><span>다시 연락합니다.</span>';

    const cards=[...section.querySelectorAll('.ct-strength-grid article')];
    const flowSteps=[...section.querySelectorAll('.ct-suite-flow div')];
    const flowArrows=[...section.querySelectorAll('.ct-suite-flow i')];
    if(!cards.length)return;

    section.classList.add('ct-strength-motion');
    cards.forEach((card,index)=>{
      card.style.setProperty('--ct-order',String(index));
      card.tabIndex=0;
    });
    flowSteps.forEach((step,index)=>step.style.setProperty('--ct-order',String(index)));

    let active=0;
    let timer=0;
    let visible=false;
    const reduced=matchMedia('(prefers-reduced-motion: reduce)');

    const activate=index=>{
      active=(index+cards.length)%cards.length;
      cards.forEach((card,i)=>card.classList.toggle('is-active',i===active));
      flowSteps.forEach((step,i)=>step.classList.toggle('is-active',i===Math.min(active,flowSteps.length-1)));
      flowArrows.forEach((arrow,i)=>arrow.classList.toggle('is-active',i<Math.min(active,flowArrows.length)));
    };
    const stop=()=>{clearInterval(timer);timer=0;};
    const start=()=>{
      stop();
      if(!visible||reduced.matches)return;
      timer=setInterval(()=>activate(active+1),1700);
    };

    cards.forEach((card,index)=>{
      card.addEventListener('mouseenter',()=>{stop();activate(index);});
      card.addEventListener('focus',()=>{stop();activate(index);});
    });
    section.addEventListener('mouseleave',start);
    section.addEventListener('focusout',event=>{if(!section.contains(event.relatedTarget))start();});

    const observer=new IntersectionObserver(entries=>{
      const entry=entries[0];
      visible=entry.isIntersecting;
      section.classList.toggle('is-visible',visible);
      if(visible){activate(0);start();}else stop();
    },{threshold:.24});
    observer.observe(section);

    if(!document.querySelector('style[data-ct-strength-animation]')){
      const style=document.createElement('style');
      style.dataset.ctStrengthAnimation='1';
      style.textContent=`
        #strengths.ct-strength-motion .ad-title span{color:var(--blue-2)}
        #strengths.ct-strength-motion .ad-head{opacity:0;transform:translateY(24px);transition:opacity .65s ease,transform .75s cubic-bezier(.2,.75,.2,1)}
        #strengths.ct-strength-motion .ct-strength-grid article{opacity:0;transform:translateY(38px) scale(.965);transition:opacity .6s ease calc(var(--ct-order)*110ms),transform .68s cubic-bezier(.2,.78,.2,1) calc(var(--ct-order)*110ms),border-color .35s ease,background .35s ease,box-shadow .35s ease}
        #strengths.ct-strength-motion .ct-suite-flow{opacity:0;transform:translateY(28px);transition:opacity .65s ease .42s,transform .72s cubic-bezier(.2,.78,.2,1) .42s}
        #strengths.ct-strength-motion.is-visible .ad-head,#strengths.ct-strength-motion.is-visible .ct-suite-flow{opacity:1;transform:none}
        #strengths.ct-strength-motion.is-visible .ct-strength-grid article{opacity:.58;transform:none}
        #strengths.ct-strength-motion.is-visible .ct-strength-grid article.is-active{opacity:1;transform:translateY(-10px) scale(1.025);z-index:2;border-color:rgba(59,111,255,.7);background:linear-gradient(155deg,rgba(59,111,255,.2),#11141a 52%);box-shadow:0 24px 58px rgba(59,111,255,.17)}
        #strengths.ct-strength-motion .ct-strength-grid article>b{transition:color .35s ease,text-shadow .35s ease}
        #strengths.ct-strength-motion .ct-strength-grid article.is-active>b{color:#9eb1ff;text-shadow:0 0 24px rgba(59,111,255,.52)}
        #strengths.ct-strength-motion .ct-suite-flow div{transition:background .35s ease,border-color .35s ease,transform .35s ease,box-shadow .35s ease;border:1px solid transparent}
        #strengths.ct-strength-motion .ct-suite-flow div.is-active{transform:translateY(-4px);border-color:rgba(59,111,255,.5);background:rgba(59,111,255,.16);box-shadow:0 12px 30px rgba(59,111,255,.12)}
        #strengths.ct-strength-motion .ct-suite-flow i{transition:color .35s ease,transform .35s ease,text-shadow .35s ease}
        #strengths.ct-strength-motion .ct-suite-flow i.is-active{color:#aebcff;transform:translateX(5px);text-shadow:0 0 20px rgba(59,111,255,.7)}
        @media(max-width:760px){
          #strengths.ct-strength-motion.is-visible .ct-strength-grid article{opacity:.66}
          #strengths.ct-strength-motion.is-visible .ct-strength-grid article.is-active{transform:translateY(-5px) scale(1.012)}
          #strengths.ct-strength-motion .ct-suite-flow i.is-active{transform:rotate(90deg) translateX(4px)}
        }
        @media(prefers-reduced-motion:reduce){
          #strengths.ct-strength-motion .ad-head,#strengths.ct-strength-motion .ct-strength-grid article,#strengths.ct-strength-motion .ct-suite-flow{opacity:1!important;transform:none!important;transition:none!important}
          #strengths.ct-strength-motion .ct-strength-grid article{opacity:1!important}
        }
      `;
      document.head.append(style);
    }

    activate(0);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(run),{once:true});
  else requestAnimationFrame(run);
})();