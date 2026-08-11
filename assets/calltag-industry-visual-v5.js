(()=>{
  if(document.documentElement.dataset.ctIndustryVisualV6)return;
  document.documentElement.dataset.ctIndustryVisualV6='1';

  const data=[
    ['보험','상담 신청','insurance','xl',38,54,1,0,-170,120,-8,-120,-145,-8],
    ['병원','진료 예약','clinic','xl',63,53,.98,.06,175,110,8,130,-155,9],
    ['부동산','매물 문의','estate','md',15,31,.84,.14,-165,-75,-9,-145,-90,-10],
    ['학원','상담 예약','academy','sm',84,28,.68,.22,160,-85,10,150,-105,11],
    ['미용실','시술 예약','salon','sm',10,72,.62,.28,-165,110,-11,-160,-70,-12],
    ['자동차','정비 문의','auto','md',86,70,.82,.18,180,100,9,155,-85,10],
    ['쇼핑몰','상품 문의','shop','md',49,83,.88,.11,0,180,2,18,-175,4],
    ['인테리어','견적 신청','interior','sm',73,83,.7,.3,115,155,7,110,-130,8]
  ];

  const ui={
    insurance:'<div class="v5-ui insurance"><div class="v5-row"><span>월 예상 보험료</span><b>84,000원</b></div><div class="v5-bars"><i></i><i></i><i></i><i></i></div><em>보장 분석 완료</em></div>',
    clinic:'<div class="v5-ui clinic"><div class="v5-profile"><i></i><span><b>김온유 원장</b><small>진료 예약</small></span></div><div class="v5-days"><i>2</i><i class="on">3</i><i>4</i><i>5</i><i>6</i></div><em>오전 10:30 예약 가능</em></div>',
    estate:'<div class="v5-ui estate"><div class="v5-house"><i></i><i></i><i></i></div><div class="v5-row"><span>시티뷰 리버파크</span><b>8억 4,000</b></div></div>',
    academy:'<div class="v5-ui academy"><div class="v5-avatars"><i></i><i></i><i></i></div><div class="v5-row"><span>수학 상담</span><b>18:30</b></div><em>상담 일정 확정</em></div>',
    salon:'<div class="v5-ui salon"><div class="v5-tiles"><i></i><i></i><i></i></div><div class="v5-row"><span>커트 · 컬러</span><b>예약</b></div></div>',
    auto:'<div class="v5-ui auto"><div class="v5-car"><i></i><span></span></div><div class="v5-checks"><span>● 엔진오일</span><span>● 타이어</span></div><em>정비 접수 완료</em></div>',
    shop:'<div class="v5-ui shop"><div class="v5-tiles"><i></i><i></i><i></i></div><div class="v5-row"><span>상품 문의 3건</span><b>확인</b></div></div>',
    interior:'<div class="v5-ui interior"><div class="v5-room"><i></i><i></i><i></i></div><div class="v5-row"><span>32평 견적</span><b>상담 요청</b></div></div>'
  };

  const markup=`<div class="ct-industry-v5__sticky"><div class="ct-industry-v5__head"><h2>업종별 문의 화면</h2></div><div class="ct-industry-v5__stage">${data.map((item,index)=>`<article class="ct-industry-v5__card ${item[3]}" style="--x:${item[4]}%;--y:${item[5]}%;--alpha:${item[6]};--float-duration:${5.5+(index%4)*.55}s;--float-delay:${-((index%5)*.72)}s" data-alpha="${item[6]}" data-delay="${item[7]}" data-dx="${item[8]}" data-dy="${item[9]}" data-rot="${item[10]}" data-ex="${item[11]}" data-ey="${item[12]}" data-er="${item[13]}"><div class="ct-industry-v5__inner"><div class="ct-industry-v5__thumb">${ui[item[2]]}</div><div class="ct-industry-v5__meta"><span>${item[0]}</span><strong>${item[1]}</strong></div></div></article>`).join('')}</div></div>`;

  let section=null;
  let sticky=null;
  let head=null;
  let cards=[];
  let raf=0;
  const mobile=matchMedia('(max-width:900px)');
  const reduce=matchMedia('(prefers-reduced-motion:reduce)').matches;
  const clamp=value=>Math.max(0,Math.min(1,value));
  const easeOut=value=>1-Math.pow(1-value,3);
  const easeIn=value=>value*value*value;

  const mount=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-industry-v4,#ct-pagero-intro .ct-industry-visual-section,#ct-pagero-intro .ct-industry-float-section,#ct-pagero-intro .ct-industries-static,#ct-pagero-intro .ct-horizontal-industries-clean');
    if(!target)return false;
    if(target.dataset.ctIndustryV5Mounted!=='1'){
      target.className='ct-industry-v5';
      target.removeAttribute('style');
      target.innerHTML=markup;
      target.dataset.ctIndustryV5Mounted='1';
    }
    section=target;
    sticky=target.querySelector('.ct-industry-v5__sticky');
    head=target.querySelector('.ct-industry-v5__head');
    cards=[...target.querySelectorAll('.ct-industry-v5__card')];
    if(!sticky||!head||!cards.length)return false;
    if(reduce){
      cards.forEach(card=>{card.style.opacity=card.dataset.alpha||'1';card.classList.add('settled');});
      head.style.opacity='1';
      head.style.transform='none';
    }else requestRender();
    dispatchEvent(new Event('resize'));
    return true;
  };

  const getProgress=()=>{
    if(!section)return 0;
    const rect=section.getBoundingClientRect();
    const vh=innerHeight||document.documentElement.clientHeight;
    if(mobile.matches)return clamp((vh*.92-rect.top)/(vh*.92+rect.height*.72));
    const top=rect.top+scrollY;
    const range=Math.max(1,section.offsetHeight-(sticky?.offsetHeight||innerHeight));
    return clamp((scrollY-top)/range);
  };

  const render=()=>{
    raf=0;
    if(!section||!cards.length||!head)return;
    const p=getProgress();
    const isMobile=mobile.matches;
    const titleIn=easeOut(clamp(p/.16));
    const titleOut=easeIn(clamp((p-.8)/.18));
    head.style.opacity=(titleIn*(1-titleOut)).toFixed(3);
    head.style.transform=`translate3d(0,${((1-titleIn)*28-titleOut*38).toFixed(2)}px,0) scale(${(.96+titleIn*.04-titleOut*.03).toFixed(4)})`;

    cards.forEach(card=>{
      const alpha=parseFloat(card.dataset.alpha||'.8');
      const delay=parseFloat(card.dataset.delay||'0');
      const dx=parseFloat(card.dataset.dx||'0');
      const dy=parseFloat(card.dataset.dy||'80');
      const rot=parseFloat(card.dataset.rot||'0');
      const ex=parseFloat(card.dataset.ex||'0');
      const ey=parseFloat(card.dataset.ey||'-120');
      const er=parseFloat(card.dataset.er||'0');
      const enter=easeOut(clamp((p-delay)/.34));
      const leave=easeIn(clamp((p-.72)/.28));
      const visibility=enter*(1-leave);
      const lift=clamp((p-.2)/.48)*(isMobile?12:34);
      const x=isMobile?0:dx*(1-enter)+ex*leave;
      const y=isMobile?(1-enter)*36-leave*42:dy*(1-enter)-lift+ey*leave;
      const rotation=isMobile?0:rot*(1-enter)+er*leave;
      const startScale=isMobile ? .97 : .86;
      const scale=startScale+(1-startScale)*enter-.1*leave;
      const blur=(1-enter)*2.4+leave*3.2;
      card.style.opacity=(alpha*visibility).toFixed(3);
      card.style.filter=`blur(${blur.toFixed(2)}px) brightness(${(.8+enter*.2-leave*.14).toFixed(3)})`;
      card.style.transform=isMobile
        ?`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`
        :`translate(-50%,-50%) translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) rotate(${rotation.toFixed(2)}deg) scale(${scale.toFixed(4)})`;
      card.classList.toggle('settled',enter>.96&&leave<.04);
    });
  };

  const requestRender=()=>{if(!raf&&!reduce)raf=requestAnimationFrame(render);};
  const boot=()=>{
    const onScroll=requestRender;
    const onResize=requestRender;
    const onPageShow=()=>{mount();requestRender();};
    const onMedia=()=>{mount();requestRender();};
    addEventListener('scroll',onScroll,{passive:true});
    addEventListener('resize',onResize,{passive:true});
    addEventListener('pageshow',onPageShow,{passive:true});
    mobile.addEventListener?.('change',onMedia);
    mount();
    const timers=[100,350,800,1600,3000].map(delay=>setTimeout(()=>{mount();requestRender();},delay));
    window.addEventListener('pagehide',()=>{
      timers.forEach(clearTimeout);
      removeEventListener('scroll',onScroll);
      removeEventListener('resize',onResize);
      removeEventListener('pageshow',onPageShow);
      mobile.removeEventListener?.('change',onMedia);
      if(raf)cancelAnimationFrame(raf);
    },{once:true});
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});
  else boot();
})();