(()=>{
  if(document.documentElement.dataset.ctPageroIntroLoaderV9)return;
  document.documentElement.dataset.ctPageroIntroLoaderV9='1';

  const applyCopy=()=>{
    const copy=document.querySelector('#ct-pagero-intro .ct-v8-head>strong');
    if(copy)copy.innerHTML='페이지로에서 문의를 받으면<br>관리는 콜태그 앱에서!';
  };

  const script=document.createElement('script');
  script.src='https://calltag.pagero.kr/assets/calltag-pagero-intro.js?v=20260803-9';
  script.defer=true;
  script.addEventListener('load',()=>{
    requestAnimationFrame(applyCopy);
    setTimeout(applyCopy,250);
  });
  document.head.append(script);
})();