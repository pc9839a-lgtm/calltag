(()=>{
  if(document.documentElement.dataset.ctPageroIntroLoaderV11)return;
  document.documentElement.dataset.ctPageroIntroLoaderV11='1';
  const script=document.createElement('script');
  script.src='/assets/calltag-pagero-intro.js?v=20260803-intro11';
  script.defer=true;
  document.head.append(script);
})();
