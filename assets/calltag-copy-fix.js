(()=>{
  const q=(selector)=>document.querySelector(selector);
  const apply=()=>{
    const kicker=q('.hero-kicker');
    const title=q('.hero-heading h1');
    const description=q('.hero-heading > p:last-of-type');
    const targetTitle=q('#targets .ad-title');

    if(kicker) kicker.textContent='통화 후 고객관리 Android 앱';
    if(title) title.innerHTML='1명의 고객도<br><span>놓치지 마세요.</span>';
    if(description) description.textContent='통화가 끝나면 고객 상태, 다음 연락 날짜, 보낼 문자를 바로 남기는 Android 앱입니다.';
    if(targetTitle) targetTitle.innerHTML='전화 한 통이 매출·예약·상담을 만드는<br>업종에 필요합니다.';

    const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
    let textNode;
    while((textNode=walker.nextNode())){
      textNode.nodeValue=textNode.nodeValue
        .replaceAll('이어집니다.','확인하세요.')
        .replaceAll('흐름입니다.','사용 방식입니다.')
        .replaceAll('정리됩니다.','한눈에 볼 수 있습니다.');
    }
  };

  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',apply,{once:true});
  else requestAnimationFrame(apply);
})();