(()=>{
  'use strict';
  if(window.__CALLTAG_SETTLEMENT_FINALIZE_LOADED__)return;
  window.__CALLTAG_SETTLEMENT_FINALIZE_LOADED__=true;

  const API={
    fresh:'/api/partner/totp/fresh',
    logout:'/api/partner/logout',
    payout:'/api/partner/payout-profile',
    request:'/api/partner/settlements/request',
    policies:'/api/partner/policies',
  };
  const POLICY_VERSION='2026.08.07';
  const STYLE='/web/settlement-finalize.css?v=20260813-final1';

  function $(id){return document.getElementById(id)}
  function addStyle(){if(document.querySelector(`link[href^="/web/settlement-finalize.css"]`))return;const link=document.createElement('link');link.rel='stylesheet';link.href=STYLE;document.head.append(link)}
  function showAlert(message=''){const box=$('alertBox');if(!box)return;box.textContent=message;box.classList.toggle('show',!!message)}
  function toast(message=''){const box=$('toast');if(!box)return;box.textContent=message;box.classList.add('show');clearTimeout(toast.timer);toast.timer=setTimeout(()=>box.classList.remove('show'),1800)}
  function service(){const active=document.querySelector('.service-tab.active[data-service]');const value=String(active?.dataset?.service||'all').toLowerCase();return value==='pagero'?'PAGERO':value==='calltag'?'CALLTAG':'ALL'}

  async function api(url,opt={}){
    const response=await fetch(url,{credentials:'include',cache:'no-store',...opt,headers:{accept:'application/json',...(opt.body?{'content-type':'application/json'}:{}),...(opt.headers||{})}});
    const data=await response.json().catch(()=>({}));
    if(!response.ok){
      const error=new Error(data.error||data.message||`요청 실패 (${response.status})`);
      error.status=response.status;
      error.apiCode=String(data.code||'');
      error.data=data;
      throw error;
    }
    return data;
  }

  async function recordConsent(action){
    return api(API.policies,{method:'POST',body:JSON.stringify({version:POLICY_VERSION,action,service:service(),acceptedAt:new Date().toISOString()})});
  }

  function otpModal(){
    return new Promise((resolve,reject)=>{
      const root=document.createElement('div');root.className='ct-fresh-modal';root.setAttribute('role','dialog');root.setAttribute('aria-modal','true');
      root.innerHTML=`<section class="ct-fresh-card"><p class="eyebrow">SECURITY CHECK</p><h2>구글 OTP 한번 더 확인</h2><p>계좌정보 변경이나 지급 요청은 보안을 위해 최근 5분 이내 인증이 필요합니다. Google Authenticator에 표시된 6자리 숫자를 입력하세요.</p><input class="ct-fresh-code" inputmode="numeric" autocomplete="one-time-code" maxlength="6" pattern="[0-9]*" placeholder="000000" aria-label="Google Authenticator 6자리 숫자"><div class="ct-fresh-error" role="alert"></div><div class="ct-fresh-actions"><button type="button" class="ct-fresh-cancel">취소</button><button type="button" class="ct-fresh-confirm">확인</button></div></section>`;
      document.body.append(root);
      const input=root.querySelector('.ct-fresh-code');const errorBox=root.querySelector('.ct-fresh-error');const cancel=root.querySelector('.ct-fresh-cancel');const confirm=root.querySelector('.ct-fresh-confirm');
      const close=(value)=>{root.remove();resolve(value)};
      cancel.onclick=()=>close(false);
      root.addEventListener('click',event=>{if(event.target===root)close(false)});
      input.addEventListener('input',()=>{input.value=input.value.replace(/\D/g,'').slice(0,6);errorBox.textContent=''});
      const submit=async()=>{
        if(input.value.length!==6){errorBox.textContent='6자리 숫자를 입력해주세요.';return;}
        confirm.disabled=true;cancel.disabled=true;confirm.textContent='확인 중…';
        try{await api(API.fresh,{method:'POST',body:JSON.stringify({code:input.value})});close(true)}
        catch(error){errorBox.textContent=error.message||'인증 코드를 확인해주세요.';confirm.disabled=false;cancel.disabled=false;confirm.textContent='확인';input.select()}
      };
      confirm.onclick=submit;input.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();submit()}if(event.key==='Escape')close(false)});
      setTimeout(()=>input.focus(),20);
    });
  }

  async function sensitive(action){
    try{return await action()}
    catch(error){
      if(error.apiCode!=='PARTNER_TOTP_FRESH_REQUIRED')throw error;
      const verified=await otpModal();
      if(!verified){const cancelled=new Error('보안 확인이 취소되었습니다.');cancelled.cancelled=true;throw cancelled}
      return action();
    }
  }

  function installLogout(){
    const actions=document.querySelector('.top-actions');if(!actions||$('settlementLogoutBtn'))return;
    const button=document.createElement('button');button.id='settlementLogoutBtn';button.type='button';button.className='settlement-logout';button.textContent='로그아웃';
    const home=actions.querySelector('.home-link');actions.insertBefore(button,home||null);
    button.onclick=async()=>{
      button.disabled=true;button.textContent='로그아웃 중…';
      try{await api(API.logout,{method:'POST',body:'{}'})}catch(_error){}
      try{localStorage.removeItem('calltagPartnerPolicyConsent')}catch(_error){}
      location.assign('/web/settlement');
    };
  }

  function installEmptyState(){
    const referral=document.querySelector('.referral-card');if(!referral||$('partnerEmptyState'))return;
    const box=document.createElement('section');box.id='partnerEmptyState';box.className='partner-empty-state';box.innerHTML='<strong>아직 발생한 파트너 수익이 없습니다.</strong><p>추천인 코드를 공유하면 추천 회원의 유료 결제가 확정된 뒤 수익이 자동으로 반영됩니다. 수익이 생기면 이 화면에서 지급 가능 금액과 정산 상태를 바로 확인할 수 있습니다.</p>';
    referral.insertAdjacentElement('afterend',box);
    const update=()=>{
      const referralCount=valueOf($('metricReferrals')?.textContent);
      const estimated=valueOf($('metricEstimated')?.textContent);
      const available=valueOf($('metricConfirmed')?.textContent);
      const paid=valueOf($('metricPaidTotal')?.textContent);
      const loaded=[referralCount,estimated,available,paid].every(v=>v!==null);
      box.classList.toggle('show',loaded&&referralCount===0&&estimated===0&&available===0&&paid===0);
    };
    ['metricReferrals','metricEstimated','metricConfirmed','metricPaidTotal'].forEach(id=>{const node=$(id);if(node)new MutationObserver(update).observe(node,{childList:true,subtree:true,characterData:true})});
    update();
  }

  function valueOf(text=''){
    const raw=String(text||'').trim();if(!raw||raw.includes('—'))return null;
    const digits=raw.replace(/[^0-9-]/g,'');if(!digits)return 0;const value=Number(digits);return Number.isFinite(value)?value:null;
  }

  function enhancePayoutFields(){
    const form=$('payoutForm');if(!form)return;
    const bank=$('bankName'),holder=$('accountHolder'),account=$('accountNumber'),email=$('settlementEmail'),phone=$('phone'),business=$('businessNumber');
    if(holder){holder.placeholder='예: 김도윤';holder.autocomplete='name'}
    if(bank){bank.placeholder='은행 선택 또는 입력';bank.setAttribute('list','partnerBankList')}
    if(account){account.placeholder='숫자만 입력';account.autocomplete='off';account.addEventListener('focus',()=>{if(account.value.includes('*'))account.select()})}
    if(email)email.autocomplete='email';if(phone)phone.autocomplete='tel';if(business)business.addEventListener('focus',()=>{if(business.value.includes('*'))business.select()});
    if(!$('partnerBankList')){const list=document.createElement('datalist');list.id='partnerBankList';['KB국민은행','신한은행','우리은행','하나은행','NH농협은행','IBK기업은행','카카오뱅크','토스뱅크','케이뱅크','SC제일은행','부산은행','iM뱅크','경남은행','광주은행','전북은행','제주은행','새마을금고','신협','우체국'].forEach(name=>{const option=document.createElement('option');option.value=name;list.append(option)});form.append(list)}
    if(!form.querySelector('.payout-security-note')){const note=document.createElement('p');note.className='payout-security-note';note.innerHTML='<strong>보안 안내</strong> · 계좌번호와 사업자번호는 암호화 저장되며 저장 후에는 끝 4자리만 표시됩니다. 변경 또는 지급 요청 시 필요하면 구글 OTP를 한 번 더 확인합니다.';form.querySelector('.consent-row')?.insertAdjacentElement('beforebegin',note)}
  }

  function installPayoutSubmit(){
    const form=$('payoutForm');if(!form)return;
    form.onsubmit=async event=>{
      event.preventDefault();showAlert('');
      if(!form.reportValidity())return;
      if(!$('partnerConsent')?.checked){showAlert('파트너 약관과 개인정보 처리 안내 동의가 필요합니다.');return}
      const button=$('savePayoutBtn');if(button){button.disabled=true;button.textContent='저장 중…'}
      const payload=Object.fromEntries(new FormData(form).entries());delete payload.partnerConsent;
      if(String(payload.accountNumber||'').includes('*'))delete payload.accountNumber;
      if(String(payload.businessNumber||'').includes('*'))delete payload.businessNumber;
      try{
        await recordConsent('payout-save');
        const result=await sensitive(()=>api(API.payout,{method:'PUT',body:JSON.stringify(payload)}));
        if(result.accountNumberMasked)$('accountNumber').value=result.accountNumberMasked;
        if(result.businessNumberMasked&&$('businessNumber'))$('businessNumber').value=result.businessNumberMasked;
        toast('정산 정보를 안전하게 저장했습니다.');
      }catch(error){if(!error.cancelled)showAlert(error.message||'정산 정보를 저장하지 못했습니다.')}
      finally{if(button){button.disabled=false;button.textContent='저장'}}
    };
  }

  function installRequestSubmit(){
    const button=$('requestBtn');if(!button)return;
    button.onclick=async()=>{
      showAlert('');
      if(!$('settlementConsent')?.checked){showAlert('파트너 약관과 정산정책 동의가 필요합니다.');return}
      button.disabled=true;const original=button.textContent;button.textContent='요청 중…';
      try{
        await recordConsent('settlement-request');
        await sensitive(()=>api(API.request,{method:'POST',body:JSON.stringify({service:service()})}));
        toast('지급 요청을 접수했습니다.');
        setTimeout(()=>location.reload(),650);
      }catch(error){
        if(!error.cancelled){
          showAlert(error.message||'지급 요청을 처리하지 못했습니다.');
          if(error.apiCode==='PARTNER_PAYOUT_PROFILE_REQUIRED')document.querySelector('.settings-btn')?.click();
        }
        button.disabled=false;button.textContent=original;
      }
    };
  }

  function init(){addStyle();installLogout();installEmptyState();enhancePayoutFields();installPayoutSubmit();installRequestSubmit()}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,0),{once:true});else setTimeout(init,0);
})();
