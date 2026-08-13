(()=>{
  'use strict';
  if(window.__CALLTAG_SETTLEMENT_LOADER__)return;
  window.__CALLTAG_SETTLEMENT_LOADER__=true;

  function load(src){
    return new Promise((resolve,reject)=>{
      const script=document.createElement('script');
      script.src=src;
      script.async=false;
      script.onload=resolve;
      script.onerror=()=>reject(new Error(`Failed to load ${src}`));
      document.head.append(script);
    });
  }

  (async()=>{
    await load('/web/settlement-core.js?v=20260813-core1');
    await load('/web/settlement-finalize.js?v=20260813-final1');
  })().catch(error=>console.error('[CallTag settlement loader]',error));
})();
