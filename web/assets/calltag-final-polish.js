(()=>{
  if(document.documentElement.dataset.ctFinalPolish)return;
  document.documentElement.dataset.ctFinalPolish='1';
  const q=(s,r=document)=>r.querySelector(s);
  const qa=(s,r=document)=>[...r.querySelectorAll(s)];
  const make=(markup)=>{const t=document.createElement('template');t.innerHTML=markup.trim();return t.content.firstElementChild;};

  const run=()=>{
    const hero=q('.hero-heading');
    if(hero){
      const title=q('h1',hero);
      if(title)title.innerHTML='통화 후 <span>고객관리.</span>';
      qa('.ad-actions,.ad-offer',hero).forEach(el=>el.remove());
    }

    const story=q('.ct-story-section');
    const sticky=q('.ct-story-sticky',story||document);
    const steps=qa('.ct-story-step',story||document);
    if(story&&sticky&&steps.length&&!q('.ct-hover-menu',sticky)){
      const labels=steps.map(step=>step.dataset.title||q('h3',step)?.textContent.trim()||'');
      const menu=make(`<nav class="ct-hover-menu" aria-label="콜태그 기능 화면 선택">${labels.map((label,i)=>`<button type="button" data-index="${i}"${i===0?' class="is-active"':''}><b>${String(i+1).padStart(2,'0')}</b><span>${label}</span></button>`).join('')}</nav>`);
      const current=q('.ct-story-current',sticky);
      (current||sticky).after(menu);
      const buttons=qa('button',menu);
      const number=q('#ctStoryNumber',sticky);
      const currentText=q('#ctStoryCurrent',sticky);
      const activate=(index,move=false)=>{
        const step=steps[index];
        if(!step)return;
        steps.forEach((item,i)=>item.classList.toggle('is-active',i===index));
        buttons.forEach((button,i)=>button.classList.toggle('is-active',i===index));
        if(number)number.textContent=step.dataset.number||String(index+1).padStart(2,'0');
        if(currentText)currentText.textContent=step.dataset.title||labels[index];
        if(move)step.scrollIntoView({behavior:'auto',block:'center'});
      };
      buttons.forEach((button,index)=>{
        button.addEventListener('mouseenter',()=>activate(index,true));
        button.addEventListener('focus',()=>activate(index,true));
        button.addEventListener('click',()=>activate(index,true));
      });
      const observer=new IntersectionObserver(entries=>{
        const visible=entries.filter(entry=>entry.isIntersecting).sort((a,b)=>b.intersectionRatio-a.intersectionRatio)[0];
        if(visible)activate(steps.indexOf(visible.target),false);
      },{threshold:[.35,.5,.7],rootMargin:'-12% 0px -18% 0px'});
      steps.forEach(step=>observer.observe(step));
    }

    const targets=q('#targets');
    const targetTitle=q('.ad-title',targets||document);
    const track=q('.ad-targets',targets||document);
    if(targetTitle)targetTitle.textContent='이런 업종에 필요합니다.';
    if(targets&&track&&!track.classList.contains('ct-industry-carousel')){
      track.classList.add('ct-industry-carousel');
      const cards=qa('.ad-target',track);
      const controls=make(`<div class="ct-carousel-controls"><button type="button" aria-label="이전 업종">←</button><span><b>01</b> / ${String(cards.length).padStart(2,'0')}</span><button type="button" aria-label="다음 업종">→</button></div>`);
      track.after(controls);
      const [prev,,next]=controls.children;
      const counter=q('b',controls);
      let index=0;
      let timer=null;
      const go=(nextIndex,behavior='smooth')=>{
        if(!cards.length)return;
        index=(nextIndex+cards.length)%cards.length;
        track.scrollTo({left:cards[index].offsetLeft-track.offsetLeft,behavior});
        if(counter)counter.textContent=String(index+1).padStart(2,'0');
      };
      const start=()=>{if(cards.length>1&&!matchMedia('(prefers-reduced-motion: reduce)').matches){clearInterval(timer);timer=setInterval(()=>go(index+1),3000);}};
      const stop=()=>clearInterval(timer);
      prev.addEventListener('click',()=>{go(index-1);start();});
      next.addEventListener('click',()=>{go(index+1);start();});
      track.addEventListener('mouseenter',stop);
      track.addEventListener('mouseleave',start);
      track.addEventListener('focusin',stop);
      track.addEventListener('focusout',start);
      let scrollTimer;
      track.addEventListener('scroll',()=>{
        clearTimeout(scrollTimer);
        scrollTimer=setTimeout(()=>{
          let nearest=0,distance=Infinity;
          cards.forEach((card,i)=>{const d=Math.abs(card.offsetLeft-track.scrollLeft);if(d<distance){distance=d;nearest=i;}});
          index=nearest;if(counter)counter.textContent=String(index+1).padStart(2,'0');
        },90);
      },{passive:true});
      start();
    }

    if(!q('style[data-ct-final-polish]')){
      const style=document.createElement('style');
      style.dataset.ctFinalPolish='1';
      style.textContent=`
        .hero-heading h1{white-space:nowrap!important;font-size:clamp(40px,7.2vw,102px)!important;line-height:1!important}
        .hero-heading .ad-actions,.hero-heading .ad-offer{display:none!important}
        .ct-hover-menu{display:grid;gap:8px;margin-top:25px;max-width:390px}
        .ct-hover-menu button{min-height:58px;display:grid;grid-template-columns:42px 1fr;align-items:center;gap:12px;padding:8px 14px;border:1px solid var(--line);border-radius:13px;background:#15181e;color:#a6abb5;text-align:left;cursor:pointer;transition:.15s ease}
        .ct-hover-menu button b{width:34px;height:34px;display:grid;place-items:center;border-radius:9px;background:#1d2028;color:#737985;font-size:10px}
        .ct-hover-menu button span{font-size:13px;font-weight:850;line-height:1.3}
        .ct-hover-menu button:hover,.ct-hover-menu button:focus-visible,.ct-hover-menu button.is-active{border-color:rgba(59,111,255,.55);background:var(--blue-soft);color:#fff;transform:translateX(6px);outline:none}
        .ct-hover-menu button:hover b,.ct-hover-menu button:focus-visible b,.ct-hover-menu button.is-active b{background:var(--blue);color:#fff}
        #targets .ad-head{max-width:none!important}
        #targets .ad-title{white-space:nowrap;font-size:clamp(42px,5.2vw,76px)!important}
        #targets .ct-industry-carousel{display:flex!important;grid-template-columns:none!important;gap:18px!important;width:min(960px,100%);margin:0 auto;overflow-x:auto;scroll-snap-type:x mandatory;scrollbar-width:none;overscroll-behavior-x:contain}
        #targets .ct-industry-carousel::-webkit-scrollbar{display:none}
        #targets .ct-industry-carousel .ad-target{flex:0 0 100%;min-height:320px;padding:48px 52px;border-radius:27px;scroll-snap-align:start;display:flex;flex-direction:column;justify-content:center}
        #targets .ct-industry-carousel .ad-target span{font-size:13px}
        #targets .ct-industry-carousel .ad-target h3{margin-top:24px;font-size:clamp(31px,4vw,47px);line-height:1.08}
        #targets .ct-industry-carousel .ad-target b{margin-top:36px;font-size:15px}
        .ct-carousel-controls{width:min(960px,100%);display:flex;align-items:center;justify-content:center;gap:18px;margin:22px auto 0}
        .ct-carousel-controls button{width:46px;height:46px;border:1px solid var(--line-strong);border-radius:50%;background:#171a20;color:#fff;font-size:18px;cursor:pointer}
        .ct-carousel-controls button:hover{border-color:var(--blue);background:var(--blue)}
        .ct-carousel-controls span{min-width:76px;color:var(--muted-2);font-size:11px;font-weight:850;text-align:center}.ct-carousel-controls b{color:var(--blue-2);font-size:16px}
        @media(max-width:960px){.ct-hover-menu{display:none}}
        @media(max-width:700px){
          .hero-heading h1{font-size:clamp(32px,10.4vw,46px)!important;letter-spacing:-.065em!important}
          #targets .ad-title{font-size:clamp(29px,9vw,43px)!important;letter-spacing:-.065em!important}
          #targets .ct-industry-carousel .ad-target{min-height:300px;padding:34px 27px}
          #targets .ct-industry-carousel .ad-target h3{font-size:34px}
        }
      `;
      document.head.append(style);
    }
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});
  else requestAnimationFrame(run);
})();