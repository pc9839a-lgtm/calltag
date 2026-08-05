(()=>{
  if(document.documentElement.dataset.ctSeoRuntime)return;
  document.documentElement.dataset.ctSeoRuntime='1';

  const apply=()=>{
    const faq=document.querySelector('#faq');
    if(faq){
      const items=[...faq.querySelectorAll('.faq-item')];
      const last=items.at(-1);
      const question=last?.querySelector('.faq-question');
      const answer=last?.querySelector('.faq-answer p');
      if(question){
        const svg=question.querySelector('svg');
        question.childNodes.forEach(node=>{if(node.nodeType===Node.TEXT_NODE)node.remove();});
        question.insertBefore(document.createTextNode('요금제는 어떻게 구성되나요?'),svg||null);
      }
      if(answer)answer.textContent='전화관리 월 1,900원, 문자자동화 월 990원, 페이지로 월 3,500원, 통합권 월 6,000원이며 모든 요금은 부가세 별도입니다.';
    }

    document.querySelectorAll('a[href="#faq"]').forEach(link=>{
      link.setAttribute('aria-label','콜태그 자주 묻는 질문으로 이동');
    });
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',apply,{once:true});
  else apply();
})();
