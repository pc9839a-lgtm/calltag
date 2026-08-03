(()=>{
  if(document.documentElement.dataset.ctRecontactRestore)return;
  document.documentElement.dataset.ctRecontactRestore='1';

  const installStyle=()=>{
    if(document.querySelector('style[data-ct-recontact-restore]'))return;
    const style=document.createElement('style');
    style.dataset.ctRecontactRestore='1';
    style.textContent=`
      .ct-recontact-restored,
      .ct-recontact-restored *{
        filter:none!important;
        visibility:visible!important;
        clip-path:none!important;
      }
      .ct-recontact-restored [data-ct-motion],
      .ct-recontact-restored [data-ct-reveal],
      .ct-recontact-restored .feature-copy,
      .ct-recontact-restored .product-panel,
      .ct-recontact-restored .ct-motion-panel,
      .ct-recontact-restored .ct-motion-card{
        opacity:1!important;
        translate:none!important;
        scale:1!important;
        transform:none!important;
        filter:none!important;
        clip-path:none!important;
        animation:none!important;
        transition:none!important;
      }
      .ct-recontact-restored.ct-motion-section:before,
      .ct-recontact-restored .ct-motion-panel:after{
        display:none!important;
        content:none!important;
      }
    `;
    document.head.append(style);
  };

  const findTarget=()=>{
    const nodes=[...document.querySelectorAll('.feature-copy h3,.feature-copy,.feature-block h3,.feature-block')];
    const marker=nodes.find(node=>{
      const text=(node.textContent||'').replace(/\s+/g,' ').trim();
      return text.includes('다시 연락할')&&text.includes('고객만')&&text.includes('보입니다');
    });
    return marker?.closest('.feature-block,section,.section')||null;
  };

  const restore=()=>{
    installStyle();
    const section=findTarget();
    if(!section)return false;

    section.classList.add('ct-recontact-restored','is-visible','visible','is-motion-visible');
    section.classList.remove('ct-motion-section');
    section.removeAttribute('data-ct-motion');
    section.removeAttribute('data-ct-reveal');

    section.querySelectorAll('[data-ct-motion],[data-ct-reveal],.ct-motion-panel,.ct-motion-card').forEach(node=>{
      node.removeAttribute('data-ct-motion');
      node.removeAttribute('data-ct-reveal');
      node.classList.remove('ct-motion-panel','ct-motion-card');
      node.classList.add('is-visible','visible');
      node.style.removeProperty('--ct-delay');
      node.style.opacity='1';
      node.style.filter='none';
      node.style.translate='none';
      node.style.scale='1';
      node.style.transform='none';
      node.style.clipPath='none';
      node.style.animation='none';
      node.style.transition='none';
    });
    return true;
  };

  const boot=()=>{
    restore();
    const observer=new MutationObserver(restore);
    observer.observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['class','data-ct-motion','data-ct-reveal','style']});
    [0,100,300,700,1500,3000,6000,10000].forEach(delay=>setTimeout(restore,delay));
    setTimeout(()=>observer.disconnect(),14000);
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();
