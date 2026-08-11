(()=>{
  if(document.documentElement.dataset.ctPageroSignalV9)return;
  document.documentElement.dataset.ctPageroSignalV9='1';

  const removeLegacy=()=>{
    document.querySelectorAll('.ct-legacy-pagero,.ct-pagero-bridge,.journey-bridge,.sticky-offer,.floating-offer,.bottom-offer,.offer-bar,.fixed-offer,.ct-fixed-offer').forEach(el=>el.remove());
    const phrases=['고객을 받는 페이지로','놓치지 않는 콜태그','2026년 가입가 평생 유지','7일 무료체험'];
    [...document.querySelectorAll('section,aside,div')].forEach(el=>{
      if(el.id==='ct-pagero-intro'||el.closest('#ct-pagero-intro'))return;
      const text=(el.textContent||'').replace(/\s+/g,' ').trim();
      if(text&&text.length<280&&phrases.some(p=>text.includes(p))){
        const pos=getComputedStyle(el).position;
        if(pos==='fixed'||phrases.slice(0,2).some(p=>text.includes(p)))el.remove();
      }
    });
  };

  const mount=()=>{
    removeLegacy();
    document.querySelector('#ct-pagero-intro')?.remove();
    const calltagHero=document.querySelector('.hero');
    if(!calltagHero)return false;
    calltagHero.id=calltagHero.id||'calltag-start';

    const intro=document.createElement('div');
    intro.id='ct-pagero-intro';
    intro.innerHTML=`
      <section class="ct-v8-hero">
        <div class="wrap ct-v8-wrap">
          <header class="ct-v8-head">
            <p>PAGERO × CALLTAG</p>
            <h1>문의가 들어오는 순간,<br><span>절대 놓치지 않습니다.</span></h1>
            <strong>페이지로에서 문의를 받고, 콜태그가 바로 <em>알림·등록·후속관리</em>합니다.</strong>
          </header>
          <div class="ct-v8-stage">
            <div class="ct-v8-step ct-v8-step-left"><b>1</b><span>페이지로 문의 접수</span></div>
            <div class="ct-v8-step ct-v8-step-right"><b>2</b><span>콜태그 즉시 알림</span></div>
            <article class="ct-v8-inquiry">
              <header><strong>PAGERO</strong><small>새 문의</small></header>
              <div class="ct-v8-customer"><small>무료 상담 신청</small><h2>김민수 고객</h2></div>
              <dl><div><dt>연락처</dt><dd>010-1234-5678</dd></div><div><dt>문의내용</dt><dd>보험 상담 요청드립니다</dd></div></dl>
              <div class="ct-v8-complete"><i>✓</i><span>문의접수완료</span></div>
            </article>
            <div class="ct-v8-transfer" aria-hidden="true"><span class="ct-v8-line"></span><i class="ct-v8-dot"></i><b>→</b><small>즉시 전달</small></div>
            <div class="ct-v8-phone">
              <div class="ct-v8-notch"></div>
              <div class="ct-v8-screen">
                <header><strong>CALLTAG</strong><span>♢<i>1</i></span></header>
                <div class="ct-v8-title"><small>오늘 할 일</small><b>신규 문의 1</b></div>
                <article class="ct-v8-appcard">
                  <div class="ct-v8-person"><i>김</i><span><b>김민수 고객</b><small>010-1234-5678 · 페이지로</small></span><em>신규 문의</em></div>
                  <div class="ct-v8-msg"><small>문의내용</small><strong>보험 상담 요청드립니다</strong></div>
                  <div class="ct-v8-actions"><button type="button">전화</button><button type="button">문자</button><button type="button">태그</button></div>
                  <div class="ct-v8-registered"><i>✓</i><span><b>고객 자동등록 완료</b><small>바로 후속관리할 수 있습니다.</small></span></div>
                </article>
              </div>
              <div class="ct-v8-push"><i>●</i><span><b>새 문의 접수</b><small>김민수 고객 · 010-1234-5678</small></span><em>지금</em></div>
            </div>
          </div>
        </div>
      </section>
      <section class="ct-v8-nocode">
        <div class="wrap ct-v8-nocode-grid">
          <div class="ct-v8-nocode-copy"><p>페이지로</p><h2>누구나 만들고,<br><span>문의까지 받습니다.</span></h2><strong>코드를 몰라도 문구와 이미지만 바꾸면 랜딩페이지가 열리고, 접수된 문의는 콜태그에서 바로 확인됩니다.</strong></div>
          <div class="ct-v8-flow">
            <article class="on"><b>01</b><span><strong>내용 입력</strong><small>문구와 이미지 수정</small></span></article><i>→</i>
            <article><b>02</b><span><strong>문의 폼 추가</strong><small>연락처와 문의내용 수집</small></span></article><i>→</i>
            <article><b>03</b><span><strong>바로 공개</strong><small>광고와 SNS에 사용</small></span></article><i>→</i>
            <article><b>04</b><span><strong>콜태그 확인</strong><small>알림·등록·후속관리</small></span></article>
          </div>
        </div>
      </section>`;
    calltagHero.parentNode.insertBefore(intro,calltagHero);

    const stage=intro.querySelector('.ct-v8-stage');
    const run=()=>{stage.classList.remove('is-running');void stage.offsetWidth;stage.classList.add('is-running');};
    run();
    const animationTimer=setInterval(run,3900);
    const flow=[...intro.querySelectorAll('.ct-v8-flow article')];
    let idx=0;
    const flowTimer=setInterval(()=>{flow.forEach((el,i)=>el.classList.toggle('on',i===idx));idx=(idx+1)%flow.length;},1700);
    const cleanupTimers=[250,900,1800].map(delay=>setTimeout(removeLegacy,delay));
    window.addEventListener('pagehide',()=>{clearInterval(animationTimer);clearInterval(flowTimer);cleanupTimers.forEach(clearTimeout);},{once:true});
    return true;
  };

  const boot=()=>{if(mount())return;const timers=[80,220,500,1000].map(delay=>setTimeout(()=>{if(mount())timers.forEach(clearTimeout);},delay));window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();

(()=>{
  if(document.documentElement.dataset.ctPageroRevealFix)return;
  document.documentElement.dataset.ctPageroRevealFix='1';
  const updateNoCode=()=>{
    const copy=document.querySelector('#ct-pagero-intro .ct-v8-nocode-copy');
    const oldFlow=document.querySelector('#ct-pagero-intro .ct-v8-flow');
    if(!copy||!oldFlow)return false;
    const title=copy.querySelector('h2');
    const description=copy.querySelector(':scope > strong');
    if(title)title.innerHTML='코드를 몰라도 누구나<br><span>랜딩페이지 제작이 가능한 페이지로</span>';
    if(description)description.remove();
    if(!oldFlow.classList.contains('ct-pagero-builder')){
      oldFlow.className='ct-pagero-builder';
      oldFlow.innerHTML=`
        <aside class="ct-pagero-tools">
          <strong><i>P</i>페이지로</strong>
          <span class="ct-pagero-tool-label">페이지 구성</span>
          <div class="ct-pagero-tool on"><i>H</i>히어로 영역</div>
          <div class="ct-pagero-tool"><i>T</i>텍스트</div>
          <div class="ct-pagero-tool"><i>I</i>이미지</div>
          <div class="ct-pagero-tool"><i>F</i>문의 폼</div>
          <span class="ct-pagero-tool-label">설정</span>
          <div class="ct-pagero-tool"><i>C</i>색상·스타일</div>
          <div class="ct-pagero-tool"><i>M</i>모바일 화면</div>
          <div class="ct-pagero-save">저장하고 공개하기</div>
        </aside>
        <div class="ct-pagero-canvas">
          <div class="ct-pagero-browser"><span class="ct-pagero-dots"><i></i><i></i><i></i></span><span class="ct-pagero-url">pagero.kr/p/무료상담</span><b class="ct-pagero-live">● 실시간 미리보기</b></div>
          <div class="ct-pagero-page">
            <div class="ct-page-nav"><b class="ct-page-logo">BRAND<span>+</span></b><small>무료 상담 신청</small></div>
            <section class="ct-page-hero"><div class="ct-page-copy"><small>전문가 무료 상담</small><h3>고민되는 순간,<br>바로 상담받으세요.</h3><p>복잡한 절차 없이 연락처만 남기면 담당자가 빠르게 안내해 드립니다.</p><span class="ct-page-cta">무료 상담 신청하기</span></div><div class="ct-page-visual"><div class="ct-page-photo"></div><span class="ct-page-badge">✓ 상담 신청 완료</span></div></section>
            <div class="ct-page-features"><div class="ct-page-feature"><i>01</i><b>빠른 상담</b><small>신청 즉시 담당자가 확인합니다.</small></div><div class="ct-page-feature"><i>02</i><b>간편한 신청</b><small>연락처와 문의내용만 입력하세요.</small></div><div class="ct-page-feature"><i>03</i><b>맞춤 안내</b><small>고객 상황에 맞춰 안내합니다.</small></div></div>
            <section class="ct-page-form"><div class="ct-page-form-copy"><b>지금 무료 상담을 신청하세요</b><small>접수된 문의는 콜태그에 자동 등록됩니다.</small></div><div class="ct-page-fields"><div class="ct-page-field">이름을 입력해 주세요</div><div class="ct-page-field">연락처를 입력해 주세요</div><div class="ct-page-submit">상담 신청 완료</div></div></section>
          </div>
        </div>
        <aside class="ct-pagero-mobile-preview"><header><span>모바일 미리보기</span><small>390 × 844</small></header><div class="ct-pagero-phone"><div class="ct-pagero-phone-screen"><div class="ct-phone-nav">BRAND<span>상담 신청</span></div><section class="ct-phone-hero"><small>전문가 무료 상담</small><h4>고민되는 순간,<br>바로 상담받으세요.</h4><p>연락처만 남기면 담당자가 빠르게 안내해 드립니다.</p><div class="ct-phone-image"></div></section><div class="ct-phone-features"><i></i><i></i><i></i></div><section class="ct-phone-form"><b>무료 상담 신청</b><div class="ct-phone-field"></div><div class="ct-phone-field"></div><div class="ct-phone-submit">상담 신청 완료</div></section></div></div></aside>`;
    }
    return true;
  };
  const updateIntegration=()=>{
    const intro=document.querySelector('#ct-pagero-intro');
    const hero=intro?.querySelector('.ct-v8-hero');
    const nocode=intro?.querySelector('.ct-v8-nocode');
    if(!intro||!hero||!nocode)return false;
    hero.querySelector('.ct-v8-head>strong')?.remove();
    if(!intro.querySelector('.ct-pagero-connect')){
      const section=document.createElement('section');section.className='ct-pagero-connect';section.innerHTML='<div class="wrap"><p>PAGERO × CALLTAG</p><h2>페이지로에서 문의를 받고,<br><span>콜태그가 바로 알림·등록·후속관리합니다.</span></h2></div>';nocode.before(section);
    }
    return true;
  };
  const updateInquiry=()=>{
    const inquiry=document.querySelector('#ct-pagero-intro .ct-v8-inquiry');if(!inquiry)return false;
    if(inquiry.querySelector('.ct-v8-reveal-group'))return true;
    const details=inquiry.querySelector(':scope > dl');const complete=inquiry.querySelector(':scope > .ct-v8-complete');if(!details||!complete)return false;
    const group=document.createElement('div');group.className='ct-v8-reveal-group';details.before(group);group.append(details,complete);
    const stage=inquiry.closest('.ct-v8-stage');if(stage){stage.classList.remove('is-running');requestAnimationFrame(()=>{void stage.offsetWidth;stage.classList.add('is-running');});}
    return true;
  };
  const apply=()=>updateInquiry()&&updateIntegration()&&updateNoCode();
  if(apply())return;
  const timers=[80,220,500,1000,1800].map(delay=>setTimeout(()=>{if(apply())timers.forEach(clearTimeout);},delay));
  window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});
})();

(()=>{
  if(document.documentElement.dataset.ctConnectVisual)return;
  document.documentElement.dataset.ctConnectVisual='1';
  const html=`<div class="ct-connect-demo"><div><div class="ct-connect-cap"><b>1</b><span>모바일 랜딩페이지</span></div><article class="ct-connect-device"><div class="ct-connect-mobile"><div class="ct-connect-nav"><strong>PAGERO<span>.</span></strong><small>무료 상담</small></div><section class="ct-connect-hero"><small>보험료 무료 비교</small><h3>내 보험료,<br>얼마나 줄일 수 있을까요?</h3><p>연락처와 문의내용만 남기면 담당자가 빠르게 안내해 드립니다.</p><div class="ct-connect-image"></div></section><div class="ct-connect-benefits"><span><i>01</i>간편 신청</span><span><i>02</i>빠른 상담</span><span><i>03</i>맞춤 안내</span></div><section class="ct-connect-form"><strong>무료 상담 신청</strong><small>입력한 문의는 콜태그로 바로 전달됩니다.</small><div class="ct-connect-field">김민수</div><div class="ct-connect-field">010-1234-5678</div><div class="ct-connect-field">보험 상담을 받고 싶어요</div><div class="ct-connect-submit">문의 접수 완료</div></section><div class="ct-connect-sticky">무료 상담 신청하기</div></div></article></div><div class="ct-connect-arrow" aria-hidden="true"><i></i><b>→</b><small>문의 즉시 전달</small></div><div><div class="ct-connect-cap"><b>2</b><span>콜태그 자동등록</span></div><article class="ct-connect-device"><div class="ct-connect-app"><div class="ct-connect-apphead"><strong>CALLTAG</strong><span>◇</span></div><div class="ct-connect-title"><small>오늘 할 일</small><b>신규 문의 1</b></div><article class="ct-connect-card"><div class="ct-connect-person"><i>김</i><span><b>김민수 고객</b><small>010-1234-5678 · 페이지로</small></span><em>신규 문의</em></div><div class="ct-connect-msg"><small>문의내용</small><strong>보험 상담을 받고 싶어요</strong></div><div class="ct-connect-actions"><span>전화</span><span>문자</span><span>태그</span></div><div class="ct-connect-done"><i>✓</i><span><b>고객 자동등록 완료</b><small>바로 후속관리할 수 있습니다.</small></span></div></article></div><div class="ct-connect-push"><i>●</i><span><b>페이지로 신규 문의</b><small>김민수 고객 · 010-1234-5678</small></span><em>지금</em></div></article></div></div>`;
  const mount=()=>{const section=document.querySelector('#ct-pagero-intro .ct-pagero-connect');if(!section)return false;const wrap=section.querySelector('.wrap');if(wrap&&!wrap.querySelector('.ct-connect-demo'))wrap.insertAdjacentHTML('beforeend',html);return !!wrap;};
  if(mount())return;
  const timers=[80,220,500,1000,1800].map(delay=>setTimeout(()=>{if(mount())timers.forEach(clearTimeout);},delay));
  window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});
})();

(()=>{
  if(document.documentElement.dataset.ctIndustryVisualV6)return;
  document.documentElement.dataset.ctIndustryVisualV6='1';
  const data=[['보험','상담 신청','insurance','xl',38,54,1,0,-170,120,-8,-120,-145,-8],['병원','진료 예약','clinic','xl',63,53,.98,.06,175,110,8,130,-155,9],['부동산','매물 문의','estate','md',15,31,.84,.14,-165,-75,-9,-145,-90,-10],['학원','상담 예약','academy','sm',84,28,.68,.22,160,-85,10,150,-105,11],['미용실','시술 예약','salon','sm',10,72,.62,.28,-165,110,-11,-160,-70,-12],['자동차','정비 문의','auto','md',86,70,.82,.18,180,100,9,155,-85,10],['쇼핑몰','상품 문의','shop','md',49,83,.88,.11,0,180,2,18,-175,4],['인테리어','견적 신청','interior','sm',73,83,.7,.3,115,155,7,110,-130,8]];
  const ui={insurance:'<div class="v5-ui insurance"><div class="v5-row"><span>월 예상 보험료</span><b>84,000원</b></div><div class="v5-bars"><i></i><i></i><i></i><i></i></div><em>보장 분석 완료</em></div>',clinic:'<div class="v5-ui clinic"><div class="v5-profile"><i></i><span><b>김온유 원장</b><small>진료 예약</small></span></div><div class="v5-days"><i>2</i><i class="on">3</i><i>4</i><i>5</i><i>6</i></div><em>오전 10:30 예약 가능</em></div>',estate:'<div class="v5-ui estate"><div class="v5-house"><i></i><i></i><i></i></div><div class="v5-row"><span>시티뷰 리버파크</span><b>8억 4,000</b></div></div>',academy:'<div class="v5-ui academy"><div class="v5-avatars"><i></i><i></i><i></i></div><div class="v5-row"><span>수학 상담</span><b>18:30</b></div><em>상담 일정 확정</em></div>',salon:'<div class="v5-ui salon"><div class="v5-tiles"><i></i><i></i><i></i></div><div class="v5-row"><span>커트 · 컬러</span><b>예약</b></div></div>',auto:'<div class="v5-ui auto"><div class="v5-car"><i></i><span></span></div><div class="v5-checks"><span>● 엔진오일</span><span>● 타이어</span></div><em>정비 접수 완료</em></div>',shop:'<div class="v5-ui shop"><div class="v5-tiles"><i></i><i></i><i></i></div><div class="v5-row"><span>상품 문의 3건</span><b>확인</b></div></div>',interior:'<div class="v5-ui interior"><div class="v5-room"><i></i><i></i><i></i></div><div class="v5-row"><span>32평 견적</span><b>상담 요청</b></div></div>'};
  const markup=`<div class="ct-industry-v5__sticky"><div class="ct-industry-v5__head"><h2>업종별 문의 화면</h2></div><div class="ct-industry-v5__stage">${data.map((item,index)=>`<article class="ct-industry-v5__card ${item[3]}" style="--x:${item[4]}%;--y:${item[5]}%;--alpha:${item[6]};--float-duration:${5.5+(index%4)*.55}s;--float-delay:${-((index%5)*.72)}s" data-alpha="${item[6]}" data-delay="${item[7]}" data-dx="${item[8]}" data-dy="${item[9]}" data-rot="${item[10]}" data-ex="${item[11]}" data-ey="${item[12]}" data-er="${item[13]}"><div class="ct-industry-v5__inner"><div class="ct-industry-v5__thumb">${ui[item[2]]}</div><div class="ct-industry-v5__meta"><span>${item[0]}</span><strong>${item[1]}</strong></div></div></article>`).join('')}</div></div>`;
  let section=null,sticky=null,head=null,cards=[],raf=0;const mobile=matchMedia('(max-width:900px)'),reduce=matchMedia('(prefers-reduced-motion:reduce)').matches,clamp=value=>Math.max(0,Math.min(1,value)),easeOut=value=>1-Math.pow(1-value,3),easeIn=value=>value*value*value;
  const mount=()=>{const target=document.querySelector('#ct-pagero-intro .ct-industry-v4,#ct-pagero-intro .ct-industry-visual-section,#ct-pagero-intro .ct-industry-float-section,#ct-pagero-intro .ct-industries-static,#ct-pagero-intro .ct-horizontal-industries-clean');if(!target)return false;if(target.dataset.ctIndustryV5Mounted!=='1'){target.className='ct-industry-v5';target.removeAttribute('style');target.innerHTML=markup;target.dataset.ctIndustryV5Mounted='1';}section=target;sticky=target.querySelector('.ct-industry-v5__sticky');head=target.querySelector('.ct-industry-v5__head');cards=[...target.querySelectorAll('.ct-industry-v5__card')];if(!sticky||!head||!cards.length)return false;if(reduce){cards.forEach(card=>{card.style.opacity=card.dataset.alpha||'1';card.classList.add('settled');});head.style.opacity='1';head.style.transform='none';}else requestRender();dispatchEvent(new Event('resize'));return true;};
  const getProgress=()=>{if(!section)return 0;const rect=section.getBoundingClientRect(),vh=innerHeight||document.documentElement.clientHeight;if(mobile.matches)return clamp((vh*.92-rect.top)/(vh*.92+rect.height*.72));const top=rect.top+scrollY,range=Math.max(1,section.offsetHeight-(sticky?.offsetHeight||innerHeight));return clamp((scrollY-top)/range);};
  const render=()=>{raf=0;if(!section||!cards.length||!head)return;const p=getProgress(),isMobile=mobile.matches,titleIn=easeOut(clamp(p/.16)),titleOut=easeIn(clamp((p-.8)/.18));head.style.opacity=(titleIn*(1-titleOut)).toFixed(3);head.style.transform=`translate3d(0,${((1-titleIn)*28-titleOut*38).toFixed(2)}px,0) scale(${(.96+titleIn*.04-titleOut*.03).toFixed(4)})`;cards.forEach(card=>{const alpha=parseFloat(card.dataset.alpha||'.8'),delay=parseFloat(card.dataset.delay||'0'),dx=parseFloat(card.dataset.dx||'0'),dy=parseFloat(card.dataset.dy||'80'),rot=parseFloat(card.dataset.rot||'0'),ex=parseFloat(card.dataset.ex||'0'),ey=parseFloat(card.dataset.ey||'-120'),er=parseFloat(card.dataset.er||'0'),enter=easeOut(clamp((p-delay)/.34)),leave=easeIn(clamp((p-.72)/.28)),visibility=enter*(1-leave),lift=clamp((p-.2)/.48)*(isMobile?12:34),x=isMobile?0:dx*(1-enter)+ex*leave,y=isMobile?(1-enter)*36-leave*42:dy*(1-enter)-lift+ey*leave,rotation=isMobile?0:rot*(1-enter)+er*leave,startScale=isMobile?.97:.86,scale=startScale+(1-startScale)*enter-.1*leave,blur=(1-enter)*2.4+leave*3.2;card.style.opacity=(alpha*visibility).toFixed(3);card.style.filter=`blur(${blur.toFixed(2)}px) brightness(${(.8+enter*.2-leave*.14).toFixed(3)})`;card.style.transform=isMobile?`translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${scale.toFixed(4)})`:`translate(-50%,-50%) translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) rotate(${rotation.toFixed(2)}deg) scale(${scale.toFixed(4)})`;card.classList.toggle('settled',enter>.96&&leave<.04);});};
  const requestRender=()=>{if(!raf&&!reduce)raf=requestAnimationFrame(render);};
  const boot=()=>{const onScroll=requestRender,onResize=requestRender,onPageShow=()=>{mount();requestRender();},onMedia=()=>{mount();requestRender();};addEventListener('scroll',onScroll,{passive:true});addEventListener('resize',onResize,{passive:true});addEventListener('pageshow',onPageShow,{passive:true});mobile.addEventListener?.('change',onMedia);mount();const timers=[100,350,800,1600,3000].map(delay=>setTimeout(()=>{mount();requestRender();},delay));window.addEventListener('pagehide',()=>{timers.forEach(clearTimeout);removeEventListener('scroll',onScroll);removeEventListener('resize',onResize);removeEventListener('pageshow',onPageShow);mobile.removeEventListener?.('change',onMedia);if(raf)cancelAnimationFrame(raf);},{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();

(()=>{
  if(document.documentElement.dataset.ctHorizontalCoordinatorV5)return;
  document.documentElement.dataset.ctHorizontalCoordinatorV5='1';
  const desktop=matchMedia('(min-width:901px)');
  const impactDesktop=matchMedia('(min-width:901px) and (prefers-reduced-motion:no-preference)');
  const clamp=(value,min=0,max=1)=>Math.min(max,Math.max(min,value));
  const impactProperties=['--ct-glow','--ct-copy-x','--ct-copy-y','--ct-copy-scale','--ct-copy-opacity','--ct-copy-blur','--ct-visual-x','--ct-visual-rotate','--ct-visual-scale','--ct-visual-opacity','--ct-visual-brightness','--ct-visual-saturation','--ct-visual-blur'];
  const industryTitles=['상담 신청부터 고객 등록까지','날짜와 시간까지 그대로','매물 문의가 바로 다음 할 일로'];
  const industryMarkup=`<article class="ct-industry-card ct-insurance"><div class="ct-industry-label"><i>01</i><span>보험료 진단형</span></div><div class="ct-industry-phone"><div class="ct-industry-screen"><div class="ct-ins-head"><strong>보험톡.</strong><span>1분 진단</span></div><section class="ct-ins-copy"><small>내 보험료 셀프 체크</small><h3>매달 내는 보험료,<br>적정한지 확인해보세요.</h3><p>현재 보험료를 선택하면 절감 가능 금액을 바로 보여드립니다.</p></section><div class="ct-ins-calc"><small>월 납입 보험료</small><b>320,000원</b><div class="ct-ins-range"><i></i></div><div class="ct-ins-options"><span>10만원 이하</span><span>10~30만원</span><span>30만원 이상</span></div></div><div class="ct-ins-result"><small>예상 절감 가능 금액</small><strong>월 84,000원</strong><p>전문가가 보장 중복과 불필요한 특약을 확인합니다.</p><div class="ct-ins-form"><div class="ct-ins-field">이름</div><div class="ct-ins-field">연락처</div><div class="ct-ins-submit">무료 진단 결과 받기</div></div></div></div></div><div class="ct-industry-auto"><i>●</i> 진단 신청은 콜태그로 자동 등록</div></article><article class="ct-industry-card ct-hospital"><div class="ct-industry-label"><i>02</i><span>진료 예약형</span></div><div class="ct-industry-phone"><div class="ct-industry-screen"><div class="ct-hos-head"><strong>온유의원</strong><span>＋</span></div><div class="ct-hos-doctor"><div class="ct-hos-photo"></div><div class="ct-hos-info"><small>내과 전문의</small><h3>김온유 원장</h3><p>생활습관 질환·건강검진<br>평일 야간진료</p></div></div><h4 class="ct-hos-title">예약 날짜를 선택하세요</h4><div class="ct-hos-days"><div class="ct-hos-day"><b>2</b><small>월</small></div><div class="ct-hos-day on"><b>3</b><small>화</small></div><div class="ct-hos-day"><b>4</b><small>수</small></div><div class="ct-hos-day"><b>5</b><small>목</small></div><div class="ct-hos-day"><b>6</b><small>금</small></div></div><h4 class="ct-hos-title">예약 가능 시간</h4><div class="ct-hos-times"><span>10:00</span><span class="on">10:30</span><span>11:00</span><span>14:00</span><span>15:30</span><span>17:00</span></div><div class="ct-hos-summary"><span>8월 3일 화요일</span><b>오전 10:30</b></div><div class="ct-hos-submit">진료 예약 신청</div></div></div><div class="ct-industry-auto"><i>●</i> 예약 정보는 콜태그로 자동 등록</div></article><article class="ct-industry-card ct-estate"><div class="ct-industry-label"><i>03</i><span>매물 탐색형</span></div><div class="ct-industry-phone"><div class="ct-industry-screen"><div class="ct-est-head"><strong>하우스픽.</strong><span>♡</span></div><div class="ct-est-photo"><span class="ct-est-badge">추천 매물</span><span class="ct-est-dots">1 / 8</span></div><section class="ct-est-body"><div class="ct-est-meta"><small>아파트 · 매매</small><span>신축 3년</span></div><h3>시티뷰 리버파크 84㎡</h3><div class="ct-est-price">매매 <em>8억 4,000</em></div><div class="ct-est-address">서울 강동구 · 역 도보 4분</div><div class="ct-est-chips"><span>방 3</span><span>욕실 2</span><span>남향</span><span>주차 1.4대</span></div><div class="ct-est-map"></div><div class="ct-est-agent"><i>박</i><span><b>박현우 공인중개사</b><small>응답률 98% · 평균 3분</small></span><em>상담 가능</em></div><div class="ct-est-submit">이 매물 문의하기</div></section></div></div><div class="ct-industry-auto"><i>●</i> 매물 문의는 콜태그로 자동 등록</div></article>`;
  const makeProgress=count=>`<div class="ct-horizontal-clean__progress" style="grid-template-columns:repeat(${count},1fr)">${Array.from({length:count},()=>'<i></i>').join('')}</div>`;
  const journeyMarkup=`<section class="ct-horizontal-clean ct-journey-clean"><div class="ct-horizontal-clean__sticky"><div class="ct-horizontal-clean__track"><article class="ct-horizontal-clean__panel"><div class="ct-horizontal-clean__copy"><h2>문의가<br>들어옵니다.</h2></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>새 상담 신청</small><strong>김민수 고객</strong></div><em>접수 완료</em></div><div class="ct-j-form"><div><span>연락처</span><b>010-1234-5678</b></div><div><span>문의 내용</span><b>보험 상담 요청</b></div><div><span>유입 페이지</span><b>보험료 진단 랜딩</b></div></div><div class="ct-j-complete">✓ 콜태그로 전달됐습니다.</div></div></div></div></article><article class="ct-horizontal-clean__panel"><div class="ct-horizontal-clean__copy"><h2>앱에서<br>바로 뜹니다.</h2></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>콜태그 고객함</small><strong>신규 문의 1</strong></div><em>지금</em></div><div class="ct-j-phone"><div class="ct-j-phone-screen"><div class="ct-j-phone-head"><b>CALLTAG</b><span>● 1</span></div><div class="ct-j-alert"><small>페이지로 신규 문의</small><b>김민수 고객</b><small>보험 상담 요청 · 지금</small></div><div class="ct-j-actions"><span>전화</span><span>문자</span><span class="on">고객카드</span></div><div class="ct-j-timeline"><div><i>✓</i><span><b>자동 등록 완료</b><small>연락처·문의 저장</small></span></div><div><i>!</i><span><b>오늘 할 일</b><small>신규 문의 확인</small></span></div></div></div></div></div></div></div></article><article class="ct-horizontal-clean__panel"><div class="ct-horizontal-clean__copy"><h2>전화가 끝나면<br>정리도 끝.</h2></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>통화 종료</small><strong>김민수 고객</strong></div><em>02:18</em></div><div class="ct-j-form"><div><span>고객 구분</span><b>신규 문의</b></div><div><span>상담 상태</span><b>견적 전달</b></div><div><span>다음 할 일</span><b>3일 뒤 연락</b></div></div><div class="ct-j-actions"><span>문자</span><span>일정</span><span class="on">저장</span></div></div></div></div></article><article class="ct-horizontal-clean__panel"><div class="ct-horizontal-clean__copy"><h2>다시 연락할<br>고객만 보입니다.</h2></div><div class="ct-horizontal-clean__visual"><div class="ct-j-scene-clean"><div class="ct-j-windowbar"><i></i><i></i><i></i><span></span></div><div class="ct-j-body"><div class="ct-j-bigstatus"><div><small>오늘의 고객관리</small><strong>재연락 8건</strong></div><em>진행 중</em></div><div class="ct-j-stats"><article><small>신규</small><b>24</b></article><article><small>완료</small><b>17</b></article><article><small>예정</small><b>6</b></article></div><div class="ct-j-bars"><i></i><i></i><i></i><i></i><i></i><i></i></div><div class="ct-j-complete">✓ 후속관리까지 연결됐습니다.</div></div></div></div></article></div>${makeProgress(4)}</div></section>`;
  let section=null,track=null,panels=[],bars=[],raf=0,activeImpactIndex=-1;const impactTimers=new Set();
  const ensureIndustryCards=()=>{const nocode=document.querySelector('#ct-pagero-intro .ct-v8-nocode');if(!nocode)return false;let target=nocode.querySelector('.ct-industry-showcase,.ct-pagero-builder,.ct-v8-flow');if(!target)return false;if(!target.classList.contains('ct-industry-showcase')){target.className='ct-industry-showcase';target.innerHTML=industryMarkup;}return target.querySelectorAll('.ct-industry-card').length===3;};
  const buildIndustries=()=>{const nocode=document.querySelector('#ct-pagero-intro .ct-v8-nocode'),cards=nocode?[...nocode.querySelectorAll('.ct-industry-showcase .ct-industry-card')]:[];if(!nocode||cards.length!==3)return null;nocode.className='ct-horizontal-clean ct-horizontal-industries-clean';nocode.style.height='230svh';nocode.innerHTML=`<div class="ct-horizontal-clean__sticky"><div class="ct-horizontal-clean__track"></div>${makeProgress(3)}</div>`;const industryTrack=nocode.querySelector('.ct-horizontal-clean__track');cards.forEach((card,index)=>{card.querySelectorAll('.ct-industry-auto').forEach(node=>node.remove());const panel=document.createElement('article');panel.className='ct-horizontal-clean__panel';panel.innerHTML=`<div class="ct-horizontal-clean__copy"><h3>${industryTitles[index]}</h3></div><div class="ct-horizontal-clean__visual"></div>`;panel.querySelector('.ct-horizontal-clean__visual').append(card);industryTrack.append(panel);});return nocode;};
  const buildJourney=industry=>{let journey=document.querySelector('.ct-journey-clean');if(journey)return journey;industry.insertAdjacentHTML('afterend',journeyMarkup);journey=industry.nextElementSibling;journey.style.height='300svh';return journey;};
  const ensureHorizontalDom=()=>{if(document.querySelector('.ct-journey-clean'))return true;if(!ensureIndustryCards())return false;const industry=buildIndustries();if(!industry)return false;return !!buildJourney(industry);};
  const cleanWrongRestore=()=>{document.querySelectorAll('style[data-ct-recontact-restore]').forEach(node=>node.remove());document.querySelectorAll('.ct-recontact-restored').forEach(node=>{node.classList.remove('ct-recontact-restored');['transform','translate','scale','filter','opacity','animation','transition'].forEach(name=>node.style.removeProperty(name));});};
  const clearStickyBlockers=target=>{let node=target.parentElement;while(node&&node!==document.body){node.classList.remove('ct-motion-section','ct-motion-enter','is-inview');['transform','filter','perspective','contain'].forEach(name=>node.style.setProperty(name,'none','important'));node.style.setProperty('overflow','visible','important');node.style.setProperty('will-change','auto','important');node=node.parentElement;}};
  const addImpactShell=panel=>{const visual=panel.querySelector('.ct-horizontal-clean__visual');if(!visual||visual.firstElementChild?.classList.contains('ct-impact-shell'))return;const shell=document.createElement('div');shell.className='ct-impact-shell';while(visual.firstChild)shell.append(visual.firstChild);visual.append(shell);};
  const resetImpact=panel=>{impactProperties.forEach(name=>panel.style.removeProperty(name));panel.classList.remove('ct-impact-hit');};
  const triggerImpact=index=>{const panel=panels[index];if(!panel)return;panels.forEach(node=>node.classList.remove('ct-impact-hit'));void panel.offsetWidth;panel.classList.add('ct-impact-hit');const timer=setTimeout(()=>{panel.classList.remove('ct-impact-hit');impactTimers.delete(timer);},620);impactTimers.add(timer);};
  const mount=()=>{cleanWrongRestore();if(!ensureHorizontalDom())return false;const journey=document.querySelector('.ct-journey-clean');if(!journey)return false;const industry=document.querySelector('.ct-horizontal-industries-clean,.ct-industries-static');if(industry){if(journey.previousElementSibling!==industry)industry.insertAdjacentElement('afterend',journey);industry.classList.add('ct-industries-static');industry.classList.remove('ct-horizontal-clean');industry.style.setProperty('height','auto','important');industry.querySelector('.ct-horizontal-clean__track')?.style.setProperty('transform','none','important');industry.querySelectorAll('.ct-horizontal-clean__panel').forEach(panel=>{panel.classList.remove('is-active','ct-impact-hit');panel.style.removeProperty('--ct-focus');panel.style.removeProperty('--ct-side');resetImpact(panel);});}clearStickyBlockers(journey);const header=document.querySelector('.header,.site-header,body>header,header'),headerHeight=Math.max(0,Math.min(96,Math.ceil(header?.getBoundingClientRect().height||68)));document.documentElement.style.setProperty('--ct-horizontal-header',`${headerHeight}px`);journey.style.height=`calc(300svh - ${headerHeight}px)`;journey.classList.remove('ct-motion-section','ct-motion-enter','is-inview');journey.style.setProperty('transform','none','important');journey.style.setProperty('filter','none','important');journey.style.setProperty('opacity','1','important');section=journey;track=journey.querySelector('.ct-horizontal-clean__track');panels=[...journey.querySelectorAll('.ct-horizontal-clean__panel')];bars=[...journey.querySelectorAll('.ct-horizontal-clean__progress i')];if(!track||panels.length!==4)return false;panels.forEach(addImpactShell);activeImpactIndex=-1;requestRender();return true;};
  const render=()=>{raf=0;if(!section||!track||!panels.length)return;if(!desktop.matches){track.style.setProperty('transform','none','important');panels.forEach(panel=>{panel.classList.add('is-active');resetImpact(panel);});bars.forEach(bar=>bar.classList.add('on'));activeImpactIndex=-1;return;}const sticky=section.querySelector('.ct-horizontal-clean__sticky'),absoluteTop=section.getBoundingClientRect().top+scrollY,stickyHeight=sticky?.offsetHeight||innerHeight,range=Math.max(1,section.offsetHeight-stickyHeight),progress=clamp((scrollY-absoluteTop)/range),position=progress*(panels.length-1),x=-progress*(panels.length-1)*innerWidth,index=Math.min(panels.length-1,Math.max(0,Math.round(position)));track.style.setProperty('transform',`translate3d(${x}px,0,0)`,'important');panels.forEach((panel,i)=>panel.classList.toggle('is-active',i===index));bars.forEach((bar,i)=>bar.classList.toggle('on',i<=index));if(!impactDesktop.matches){panels.forEach(resetImpact);activeImpactIndex=-1;return;}panels.forEach((panel,panelIndex)=>{const distance=panelIndex-position,focus=clamp(1-Math.abs(distance)*.92),side=clamp(distance,-1.15,1.15);panel.style.setProperty('--ct-glow',(focus*.72).toFixed(4));panel.style.setProperty('--ct-copy-x',`${(side*-76).toFixed(2)}px`);panel.style.setProperty('--ct-copy-y',`${((1-focus)*24).toFixed(2)}px`);panel.style.setProperty('--ct-copy-scale',(.9+focus*.1).toFixed(4));panel.style.setProperty('--ct-copy-opacity',(.12+focus*.88).toFixed(4));panel.style.setProperty('--ct-copy-blur',`${((1-focus)*2).toFixed(2)}px`);panel.style.setProperty('--ct-visual-x',`${(side*108).toFixed(2)}px`);panel.style.setProperty('--ct-visual-rotate',`${(side*-9).toFixed(2)}deg`);panel.style.setProperty('--ct-visual-scale',(.84+focus*.16).toFixed(4));panel.style.setProperty('--ct-visual-opacity',(.22+focus*.78).toFixed(4));panel.style.setProperty('--ct-visual-brightness',(.54+focus*.46).toFixed(4));panel.style.setProperty('--ct-visual-saturation',(.72+focus*.28).toFixed(4));panel.style.setProperty('--ct-visual-blur',`${((1-focus)*1.5).toFixed(2)}px`);});const inView=scrollY>=absoluteTop-innerHeight*.12&&scrollY<=absoluteTop+range+innerHeight*.12;if(!inView){activeImpactIndex=-1;return;}if(index!==activeImpactIndex){activeImpactIndex=index;triggerImpact(index);}};
  const requestRender=()=>{if(!raf)raf=requestAnimationFrame(render);};
  const boot=()=>{mount();const onScroll=requestRender,onResize=()=>{mount();requestRender();},onPageShow=()=>{mount();requestRender();},onMedia=requestRender;addEventListener('scroll',onScroll,{passive:true});addEventListener('resize',onResize,{passive:true});addEventListener('pageshow',onPageShow,{passive:true});desktop.addEventListener?.('change',onMedia);impactDesktop.addEventListener?.('change',onMedia);const resizeObserver=new ResizeObserver(requestRender);resizeObserver.observe(document.documentElement);const timers=[80,220,500,1000,1800,3000].map(delay=>setTimeout(()=>{mount();requestRender();},delay));window.addEventListener('pagehide',()=>{timers.forEach(clearTimeout);impactTimers.forEach(clearTimeout);impactTimers.clear();resizeObserver.disconnect();removeEventListener('scroll',onScroll);removeEventListener('resize',onResize);removeEventListener('pageshow',onPageShow);desktop.removeEventListener?.('change',onMedia);impactDesktop.removeEventListener?.('change',onMedia);if(raf)cancelAnimationFrame(raf);panels.forEach(resetImpact);},{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();