(()=>{
  'use strict';
  if(window.__CALLTAG_SETTLEMENT_POLISH__)return;
  window.__CALLTAG_SETTLEMENT_POLISH__=true;

  const $=id=>document.getElementById(id);
  const text=id=>String($(id)?.textContent||'—').trim()||'—';

  function installMobileFlow(){
    const panel=$('panel-overview');
    const balance=panel?.querySelector('.balance-card');
    if(!panel||!balance||$('settlementMobileFlow'))return;
    const flow=document.createElement('section');
    flow.id='settlementMobileFlow';
    flow.className='settlement-mobile-flow';
    flow.setAttribute('aria-label','모바일 정산 핵심 요약');
    flow.innerHTML=`
      <div class="settlement-mobile-flow__item"><span>이번 달 수익</span><strong id="mobileEstimated">—</strong></div>
      <div class="settlement-mobile-flow__item"><span>지급 가능</span><strong id="mobileAvailable">—</strong></div>
      <div class="settlement-mobile-flow__item"><span>지급 완료</span><strong id="mobilePaid">—</strong></div>
      <button class="settlement-mobile-flow__link" type="button">수익·지급 내역 보기</button>`;
    balance.insertAdjacentElement('beforebegin',flow);
    const sync=()=>{
      $('mobileEstimated').textContent=text('metricEstimated');
      $('mobileAvailable').textContent=text('metricConfirmed');
      $('mobilePaid').textContent=text('metricPaidTotal');
    };
    ['metricEstimated','metricConfirmed','metricPaidTotal'].forEach(id=>{
      const node=$(id);if(node)new MutationObserver(sync).observe(node,{childList:true,subtree:true,characterData:true});
    });
    flow.querySelector('button').addEventListener('click',()=>{
      document.querySelector('.main-nav .nav-btn[data-panel="settlements"]')?.click();
    });
    sync();
  }

  function payoutTypeLabel(value){
    if(value==='SOLE_PROPRIETOR')return'개인사업자';
    if(value==='CORPORATION')return'법인';
    return'개인';
  }

  function installPayoutSummary(){
    const form=$('payoutForm');
    if(!form||$('payoutProfileSummary'))return;
    const summary=document.createElement('section');
    summary.id='payoutProfileSummary';
    summary.className='payout-profile-summary';
    summary.innerHTML=`
      <div class="payout-profile-summary__head"><strong>현재 등록된 정산정보</strong><span class="payout-profile-summary__status" id="payoutProfileStatus">확인 중</span></div>
      <div class="payout-profile-summary__grid">
        <div class="payout-profile-summary__item"><span>은행</span><strong id="payoutSummaryBank">—</strong></div>
        <div class="payout-profile-summary__item"><span>예금주</span><strong id="payoutSummaryHolder">—</strong></div>
        <div class="payout-profile-summary__item"><span>계좌번호</span><strong id="payoutSummaryAccount">—</strong></div>
        <div class="payout-profile-summary__item"><span>정산 유형</span><strong id="payoutSummaryType">—</strong></div>
      </div>
      <p class="payout-profile-summary__note">계좌번호는 저장 후 끝 4자리만 표시됩니다. 정보를 변경하면 Google OTP를 다시 확인합니다.</p>`;
    form.insertAdjacentElement('beforebegin',summary);

    const sync=()=>{
      const bank=String($('bankName')?.value||'').trim();
      const holder=String($('accountHolder')?.value||'').trim();
      const account=String($('accountNumber')?.value||'').trim();
      const type=String($('payoutType')?.value||'INDIVIDUAL');
      $('payoutSummaryBank').textContent=bank||'미등록';
      $('payoutSummaryHolder').textContent=holder||'미등록';
      $('payoutSummaryAccount').textContent=account.includes('*')?account:(account?'변경 입력 중':'미등록');
      $('payoutSummaryType').textContent=payoutTypeLabel(type);
      const configured=!!bank&&!!holder&&account.includes('*');
      $('payoutProfileStatus').textContent=configured?'등록 완료':'등록 필요';
    };

    ['bankName','accountHolder','accountNumber','payoutType','businessNumber'].forEach(id=>{
      const node=$(id);if(!node)return;node.addEventListener('input',sync);node.addEventListener('change',sync);
    });
    const toast=$('toast');if(toast)new MutationObserver(sync).observe(toast,{childList:true,subtree:true,characterData:true});
    [0,250,700,1400,2400].forEach(ms=>setTimeout(sync,ms));
  }

  function enhanceEmptyState(){
    const tryEnhance=()=>{
      const box=$('partnerEmptyState');
      if(!box||box.querySelector('.partner-empty-action'))return false;
      const button=document.createElement('button');
      button.type='button';
      button.className='partner-empty-action';
      button.textContent='추천코드 복사';
      button.addEventListener('click',()=>$('copyCodeBtn')?.click());
      box.append(button);
      return true;
    };
    if(tryEnhance())return;
    let attempts=0;const timer=setInterval(()=>{attempts+=1;if(tryEnhance()||attempts>=20)clearInterval(timer)},100);
  }

  function clarifyPayoutFields(){
    const type=$('payoutType');
    const typeLabel=document.querySelector('label[for="payoutType"]');
    if(typeLabel)typeLabel.textContent='정산 유형 · 사업자 여부';
    const account=$('accountNumber');
    if(account)account.setAttribute('aria-describedby','payoutAccountHelp');
    if(account&&!$('payoutAccountHelp')){
      const help=document.createElement('small');
      help.id='payoutAccountHelp';
      help.textContent='등록 후에는 끝 4자리만 보이며, 변경할 때 새 계좌번호를 입력합니다.';
      help.style.cssText='color:#8a93a1;font-size:10px;line-height:1.45';
      account.insertAdjacentElement('afterend',help);
    }
    type?.addEventListener('change',()=>setTimeout(installPayoutSummary,0));
  }

  function init(){installMobileFlow();installPayoutSummary();enhanceEmptyState();clarifyPayoutFields()}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,0),{once:true});else setTimeout(init,0);
})();
