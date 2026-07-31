(()=>{
  if(document.documentElement.dataset.ctRuntimeGuard)return;
  document.documentElement.dataset.ctRuntimeGuard='1';

  const run=()=>{
    const stage=document.querySelector('#app .phone-stage');
    if(stage){
      const items=[...stage.querySelectorAll('.step-item')];
      const screens=[...stage.querySelectorAll('.app-screen')];
      const progress=stage.querySelector('#phoneProgress');
      let hoverIndex=-1;
      let frame=0;

      const apply=index=>{
        if(index<0||index>2)return;
        items.forEach((item,i)=>item.classList.toggle('active',i===index));
        screens.forEach((screen,i)=>screen.classList.toggle('active',i===index));
        if(progress)progress.style.width=`${((index+1)/3)*100}%`;
      };
      const hold=()=>{
        if(hoverIndex<0)return;
        apply(hoverIndex);
        frame=requestAnimationFrame(hold);
      };

      items.slice(0,3).forEach((item,index)=>{
        item.addEventListener('pointerenter',()=>{
          hoverIndex=index;
          cancelAnimationFrame(frame);
          apply(index);
          frame=requestAnimationFrame(hold);
        });
        item.addEventListener('pointerleave',()=>{
          hoverIndex=-1;
          cancelAnimationFrame(frame);
        });
      });
    }

    const marquee=document.querySelector('#targets .ct-marquee-viewport');
    if(marquee){
      marquee.scrollLeft=0;
      marquee.scrollTo=()=>{};
      marquee.scrollBy=()=>{};
    }
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});
  else requestAnimationFrame(run);
})();