(()=>{
  if(document.documentElement.dataset.ctPageroIndustries)return;
  document.documentElement.dataset.ctPageroIndustries='1';

  const css=`
    .ct-industry-showcase{display:grid!important;grid-template-columns:repeat(3,minmax(0,1fr))!important;gap:26px!important;width:100%!important;max-width:1080px!important;min-height:0!important;margin:0 auto!important;padding:0!important;overflow:visible!important;border:0!important;border-radius:0!important;background:transparent!important;box-shadow:none!important;text-align:left!important}
    .ct-industry-card{min-width:0}
    .ct-industry-label{display:flex;align-items:center;justify-content:center;gap:8px;margin-bottom:14px;color:#eef2ff;font-size:12px;font-weight:900}
    .ct-industry-label i{width:28px;height:28px;display:grid;place-items:center;border-radius:9px;background:#3b6fff;color:#fff;font-size:11px;font-style:normal;box-shadow:0 0 0 7px rgba(59,111,255,.09)}
    .ct-industry-phone{position:relative;padding:9px;border:1px solid rgba(255,255,255,.19);border-radius:36px;background:#030407;box-shadow:0 28px 72px rgba(0,0,0,.46);transition:transform .25s ease,border-color .25s ease,box-shadow .25s ease}
    .ct-industry-phone:hover{transform:translateY(-8px);border-color:rgba(113,145,255,.58);box-shadow:0 36px 90px rgba(15,31,83,.42)}
    .ct-industry-phone:before{content:'';position:absolute;z-index:5;top:9px;left:50%;width:82px;height:22px;transform:translateX(-50%);border-radius:0 0 12px 12px;background:#030407}
    .ct-industry-screen{position:relative;min-height:560px;overflow:hidden;border-radius:27px;background:#f8f9fc;color:#162039}
    .ct-industry-nav{height:43px;display:flex;align-items:center;justify-content:space-between;padding:0 16px;background:#fff;font-size:10px;font-weight:950}
    .ct-industry-nav small{padding:6px 8px;border-radius:999px;font-size:7px;font-weight:900}
    .ct-industry-hero{padding:27px 18px 20px}
    .ct-industry-hero>small{display:inline-flex;padding:6px 9px;border-radius:999px;font-size:7px;font-weight:950}
    .ct-industry-hero h3{margin:13px 0 8px;font-size:24px;line-height:1.03;letter-spacing:-.065em}
    .ct-industry-hero p{margin:0;font-size:8px;line-height:1.55}
    .ct-industry-visual{position:relative;height:145px;margin-top:17px;overflow:hidden;border-radius:16px}
    .ct-industry-visual:before,.ct-industry-visual:after{content:'';position:absolute}
    .ct-industry-features{display:grid;grid-template-columns:repeat(3,1fr);gap:6px;padding:12px;background:#fff}
    .ct-industry-features span{padding:9px 4px;border:1px solid #e5e9f1;border-radius:8px;font-size:7px;font-weight:900;text-align:center}
    .ct-industry-features i{display:grid;place-items:center;width:20px;height:20px;margin:0 auto 6px;border-radius:6px;font-size:7px;font-style:normal}
    .ct-industry-form{padding:16px 16px 64px;background:#f4f6fa}
    .ct-industry-form strong{display:block;font-size:12px}
    .ct-industry-form small{display:block;margin-top:4px;color:#7d8798;font-size:7px}
    .ct-industry-field{height:31px;display:flex;align-items:center;margin-top:7px;padding:0 10px;border:1px solid #dde3ec;border-radius:8px;background:#fff;color:#9ca5b5;font-size:7px}
    .ct-industry-submit{height:35px;display:grid;place-items:center;margin-top:8px;border-radius:8px;color:#fff;font-size:8px;font-weight:950}
    .ct-industry-sticky{position:absolute;z-index:3;left:12px;right:12px;bottom:11px;height:41px;display:grid;place-items:center;border-radius:11px;color:#fff;font-size:9px;font-weight:950;box-shadow:0 12px 28px rgba(17,28,55,.23)}
    .ct-industry-auto{display:flex;align-items:center;justify-content:center;gap:7px;margin-top:13px;color:#7d8aa4;font-size:9px;font-weight:800}.ct-industry-auto i{color:#62d89a;font-style:normal}

    .ct-industry-card.insurance .ct-industry-nav span,.ct-industry-card.insurance .ct-industry-hero>small{color:#315ddd}.ct-industry-card.insurance .ct-industry-nav small,.ct-industry-card.insurance .ct-industry-hero>small{background:#e1eaff}.ct-industry-card.insurance .ct-industry-hero{background:linear-gradient(145deg,#edf3ff,#e2eaff)}.ct-industry-card.insurance .ct-industry-hero p{color:#6d7890}.ct-industry-card.insurance .ct-industry-visual{background:radial-gradient(circle at 78% 20%,rgba(255,255,255,.28) 0 52px,transparent 53px),linear-gradient(145deg,#315ddd,#8399ff)}.ct-industry-card.insurance .ct-industry-visual:before{width:120px;height:75px;left:50%;bottom:15px;transform:translateX(-50%);border-radius:46px 46px 12px 12px;background:rgba(255,255,255,.92);box-shadow:0 0 0 11px rgba(255,255,255,.14)}.ct-industry-card.insurance .ct-industry-features i{background:#eef3ff;color:#3b6fff}.ct-industry-card.insurance .ct-industry-submit,.ct-industry-card.insurance .ct-industry-sticky{background:#3265ee}

    .ct-industry-card.hospital .ct-industry-nav span,.ct-industry-card.hospital .ct-industry-hero>small{color:#11856e}.ct-industry-card.hospital .ct-industry-nav small,.ct-industry-card.hospital .ct-industry-hero>small{background:#daf7ef}.ct-industry-card.hospital .ct-industry-hero{background:linear-gradient(145deg,#eafbf6,#ddf4ee)}.ct-industry-card.hospital .ct-industry-hero p{color:#627b76}.ct-industry-card.hospital .ct-industry-visual{background:linear-gradient(145deg,#49bca3,#9ce0d0)}.ct-industry-card.hospital .ct-industry-visual:before{width:72px;height:72px;left:50%;top:31px;transform:translateX(-50%);border-radius:50%;background:#fff;box-shadow:0 0 0 12px rgba(255,255,255,.18)}.ct-industry-card.hospital .ct-industry-visual:after{width:34px;height:8px;left:50%;top:63px;transform:translateX(-50%);border-radius:5px;background:#35aa91;box-shadow:0 -13px 0 -9px #35aa91}.ct-industry-card.hospital .ct-industry-features i{background:#e5f8f3;color:#159a7f}.ct-industry-card.hospital .ct-industry-submit,.ct-industry-card.hospital .ct-industry-sticky{background:#14987e}

    .ct-industry-card.realestate .ct-industry-nav span,.ct-industry-card.realestate .ct-industry-hero>small{color:#b56b23}.ct-industry-card.realestate .ct-industry-nav small,.ct-industry-card.realestate .ct-industry-hero>small{background:#fff0dd}.ct-industry-card.realestate .ct-industry-hero{background:linear-gradient(145deg,#fff7ec,#f5eadb)}.ct-industry-card.realestate .ct-industry-hero p{color:#7d7061}.ct-industry-card.realestate .ct-industry-visual{background:linear-gradient(160deg,#d39b61,#f0c58f)}.ct-industry-card.realestate .ct-industry-visual:before{left:32px;right:32px;bottom:17px;height:83px;background:#fff1df;clip-path:polygon(50% 0,100% 35%,92% 35%,92% 100%,8% 100%,8% 35%,0 35%)}.ct-industry-card.realestate .ct-industry-visual:after{width:38px;height:49px;left:50%;bottom:17px;transform:translateX(-50%);border-radius:5px 5px 0 0;background:#c8894d;box-shadow:-49px -10px 0 -12px rgba(197,134,76,.75),49px -10px 0 -12px rgba(197,134,76,.75)}.ct-industry-card.realestate .ct-industry-features i{background:#fff2e2;color:#c57a32}.ct-industry-card.realestate .ct-industry-submit,.ct-industry-card.realestate .ct-industry-sticky{background:#b96f2a}

    @media(max-width:980px){.ct-industry-showcase{gap:16px!important}.ct-industry-screen{min-height:535px}.ct-industry-hero h3{font-size:21px}.ct-industry-hero{padding:24px 15px 18px}.ct-industry-visual{height:126px}.ct-industry-features{padding:9px;gap:4px}.ct-industry-features span{padding:8px 2px;font-size:6px}}
    @media(max-width:760px){.ct-industry-showcase{display:flex!important;max-width:none!important;gap:18px!important;overflow-x:auto!important;scroll-snap-type:x mandatory;padding:6px max(18px,calc((100vw - 330px)/2)) 22px!important;margin-left:calc(50% - 50vw)!important;margin-right:calc(50% - 50vw)!important;scrollbar-width:none}.ct-industry-showcase::-webkit-scrollbar{display:none}.ct-industry-card{flex:0 0 310px;scroll-snap-align:center}.ct-industry-phone:hover{transform:none}}
    @media(max-width:380px){.ct-industry-card{flex-basis:286px}.ct-industry-screen{min-height:525px}.ct-industry-hero h3{font-size:21px}}
  `;

  const html=`
    <article class="ct-industry-card insurance">
      <div class="ct-industry-label"><i>01</i><span>보험 상담</span></div>
      <div class="ct-industry-phone"><div class="ct-industry-screen">
        <div class="ct-industry-nav"><strong>보험톡<span>.</span></strong><small>무료 비교</small></div>
        <section class="ct-industry-hero"><small>보험료 무료 비교</small><h3>내 보험료,<br>얼마나 줄일 수 있을까요?</h3><p>연락처만 남기면 보장 분석부터 절감 가능한 보험료까지 안내해드립니다.</p><div class="ct-industry-visual"></div></section>
        <div class="ct-industry-features"><span><i>01</i>보장 분석</span><span><i>02</i>보험료 비교</span><span><i>03</i>맞춤 상담</span></div>
        <section class="ct-industry-form"><strong>무료 보험 상담</strong><small>간단한 정보를 남겨주세요.</small><div class="ct-industry-field">이름</div><div class="ct-industry-field">연락처</div><div class="ct-industry-submit">무료 상담 신청</div></section><div class="ct-industry-sticky">보험료 무료 비교하기</div>
      </div></div><div class="ct-industry-auto"><i>●</i> 문의는 콜태그로 자동 등록</div>
    </article>
    <article class="ct-industry-card hospital">
      <div class="ct-industry-label"><i>02</i><span>병원 예약</span></div>
      <div class="ct-industry-phone"><div class="ct-industry-screen">
        <div class="ct-industry-nav"><strong>온유의원<span>.</span></strong><small>진료 예약</small></div>
        <section class="ct-industry-hero"><small>빠른 진료 예약</small><h3>기다리지 말고,<br>원하는 시간에 예약하세요.</h3><p>진료과목과 희망 시간을 남기면 병원에서 빠르게 예약을 확인합니다.</p><div class="ct-industry-visual"></div></section>
        <div class="ct-industry-features"><span><i>01</i>간편 예약</span><span><i>02</i>빠른 확인</span><span><i>03</i>일정 안내</span></div>
        <section class="ct-industry-form"><strong>진료 예약 신청</strong><small>희망 진료와 시간을 입력해주세요.</small><div class="ct-industry-field">진료과목</div><div class="ct-industry-field">연락처</div><div class="ct-industry-submit">예약 신청 완료</div></section><div class="ct-industry-sticky">빠른 진료 예약하기</div>
      </div></div><div class="ct-industry-auto"><i>●</i> 예약 문의는 콜태그로 자동 등록</div>
    </article>
    <article class="ct-industry-card realestate">
      <div class="ct-industry-label"><i>03</i><span>부동산 문의</span></div>
      <div class="ct-industry-phone"><div class="ct-industry-screen">
        <div class="ct-industry-nav"><strong>하우스픽<span>.</span></strong><small>매물 문의</small></div>
        <section class="ct-industry-hero"><small>지역 맞춤 매물</small><h3>원하는 지역 매물,<br>빠르게 안내받으세요.</h3><p>희망 지역과 조건을 남기면 담당자가 맞춤 매물을 찾아 연락드립니다.</p><div class="ct-industry-visual"></div></section>
        <div class="ct-industry-features"><span><i>01</i>조건 입력</span><span><i>02</i>매물 추천</span><span><i>03</i>방문 예약</span></div>
        <section class="ct-industry-form"><strong>맞춤 매물 문의</strong><small>희망 지역과 조건을 남겨주세요.</small><div class="ct-industry-field">희망 지역</div><div class="ct-industry-field">연락처</div><div class="ct-industry-submit">매물 문의 완료</div></section><div class="ct-industry-sticky">맞춤 매물 안내받기</div>
      </div></div><div class="ct-industry-auto"><i>●</i> 매물 문의는 콜태그로 자동 등록</div>
    </article>`;

  const mount=()=>{
    const target=document.querySelector('#ct-pagero-intro .ct-v8-nocode .ct-pagero-builder, #ct-pagero-intro .ct-v8-nocode .ct-v8-flow');
    if(!target)return false;
    if(!document.querySelector('style[data-ct-pagero-industries]')){
      const style=document.createElement('style');
      style.dataset.ctPageroIndustries='1';
      style.textContent=css;
      document.head.append(style);
    }
    target.className='ct-industry-showcase';
    target.innerHTML=html;
    return true;
  };

  if(!mount()){
    const observer=new MutationObserver(()=>{if(mount())observer.disconnect()});
    observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),10000);
  }
})();