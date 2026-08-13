(()=>{
  'use strict';

  if(window.__CALLTAG_SETTLEMENT_SECURITY_LOADED__)return;
  window.__CALLTAG_SETTLEMENT_SECURITY_LOADED__=true;

  const API={
    security:'/api/partner/security',
    login:'/api/partner/login',
    setup:'/api/partner/totp/setup',
    enable:'/api/partner/totp/enable',
    verify:'/api/partner/totp/verify',
    recoveryEmail:'/api/partner/totp/recovery-email',
    recover:'/api/partner/totp/recover',
  };

  const QR_LIB_URL='https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js';
  const QR_LIB_INTEGRITY='sha512-CNgIRecGo7nphbeZ04Sc13ka07paqdeTu0WR1IM4kNcpmBAUSHSQX0FslNhTDadL4O5SAGapGt4FodqL8My0mA==';
  const state={setup:null,recoveryEmail:'',busy:false};

  function el(tag,attrs={},children=[]){
    const node=document.createElement(tag);
    Object.entries(attrs).forEach(([key,value])=>{
      if(key==='class')node.className=value;
      else if(key==='text')node.textContent=value;
      else if(key==='html')node.innerHTML=value;
      else if(key.startsWith('on')&&typeof value==='function')node.addEventListener(key.slice(2).toLowerCase(),value);
      else if(value!==false&&value!=null)node.setAttribute(key,String(value));
    });
    [].concat(children||[]).filter(Boolean).forEach(child=>node.append(child));
    return node;
  }

  function style(){
    if(document.getElementById('ct-security-style'))return;
    const css=`
#ctSecurityGate{position:fixed;inset:0;z-index:2147483000;background:#f4f6f9;display:grid;place-items:center;padding:24px;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#111827;overflow:auto}
#ctSecurityGate *{box-sizing:border-box}.ct-sec-card{width:min(460px,100%);background:#fff;border:1px solid #e5e7eb;border-radius:24px;padding:30px;box-shadow:0 24px 70px rgba(15,23,42,.12)}
.ct-sec-brand{font-weight:900;font-size:13px;letter-spacing:.08em;color:#2563eb;margin-bottom:14px}.ct-sec-card h1{margin:0;font-size:27px;line-height:1.25}.ct-sec-desc{margin:10px 0 22px;color:#64748b;font-size:14px;line-height:1.65}.ct-sec-field{display:grid;gap:7px;margin:14px 0}.ct-sec-field label{font-size:13px;font-weight:800;color:#374151}.ct-sec-field input{width:100%;height:50px;border:1px solid #d1d5db;border-radius:13px;padding:0 14px;font-size:16px;outline:none;background:#fff}.ct-sec-field input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.12)}
.ct-sec-code{font-size:25px!important;letter-spacing:.18em;text-align:center;font-weight:900}.ct-sec-btn{width:100%;height:50px;border:0;border-radius:13px;background:#111827;color:#fff;font-size:15px;font-weight:900;cursor:pointer;margin-top:8px}.ct-sec-btn:disabled{opacity:.5;cursor:wait}.ct-sec-btn.secondary{background:#fff;color:#111827;border:1px solid #d1d5db}.ct-sec-btn.link{height:auto;background:transparent;color:#2563eb;padding:10px 0;margin-top:8px;font-size:14px}
.ct-sec-alert{display:none;margin:14px 0 0;padding:12px 14px;border-radius:12px;background:#fff1f2;color:#be123c;font-size:13px;line-height:1.5}.ct-sec-alert.show{display:block}.ct-sec-help{font-size:13px;color:#64748b;line-height:1.65;margin:10px 0}.ct-sec-email{font-weight:800;color:#111827}.ct-sec-badge{display:inline-flex;padding:6px 10px;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:12px;font-weight:900;margin-bottom:12px}
.ct-sec-steps{margin:0 0 18px;padding:0;display:grid;gap:8px;list-style:none}.ct-sec-step{display:flex;align-items:center;gap:9px;color:#475569;font-size:13px;line-height:1.45}.ct-sec-step b{width:23px;height:23px;border-radius:999px;background:#eef2ff;color:#2563eb;display:inline-grid;place-items:center;font-size:12px;flex:0 0 auto}
.ct-sec-qr-wrap{display:grid;place-items:center;margin:4px auto 14px;padding:14px;width:232px;max-width:100%;min-height:232px;border:1px solid #e2e8f0;border-radius:18px;background:#fff}.ct-sec-qr{width:204px;height:204px;max-width:100%;display:grid;place-items:center}.ct-sec-qr canvas,.ct-sec-qr img{display:block!important;width:204px!important;height:204px!important;max-width:100%;object-fit:contain}.ct-sec-qr-fallback{font-size:13px;line-height:1.55;text-align:center;color:#64748b;padding:18px}
.ct-sec-app-link{display:flex;align-items:center;justify-content:center;text-decoration:none;height:48px;border-radius:13px;border:1px solid #2563eb;color:#1d4ed8;background:#eff6ff;font-size:14px;font-weight:900;margin:10px 0 6px}.ct-sec-mobile-note{text-align:center;color:#64748b;font-size:12px;line-height:1.5;margin:6px 0 14px}
.ct-sec-manual{margin:8px 0 18px;border-top:1px solid #e5e7eb;border-bottom:1px solid #e5e7eb}.ct-sec-manual summary{cursor:pointer;list-style:none;padding:13px 2px;font-size:13px;font-weight:800;color:#64748b;text-align:center}.ct-sec-manual summary::-webkit-details-marker{display:none}.ct-sec-manual[open] summary{color:#111827}.ct-sec-manual-body{padding:0 0 14px}.ct-sec-key{margin:4px 0 8px;padding:13px;border-radius:12px;background:#f8fafc;border:1px solid #e2e8f0;word-break:break-all;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-weight:800;font-size:13px;letter-spacing:.04em}.ct-sec-copy{height:40px;margin-top:0;font-size:13px}.ct-sec-divider{height:1px;background:#e5e7eb;margin:22px 0}
@media(max-width:520px){#ctSecurityGate{padding:0;background:#fff;align-items:stretch}.ct-sec-card{width:100%;min-height:100vh;border:0;border-radius:0;box-shadow:none;padding:28px 22px;display:flex;flex-direction:column;justify-content:center}.ct-sec-card h1{font-size:25px}.ct-sec-qr-wrap{width:216px;min-height:216px;padding:10px}.ct-sec-qr,.ct-sec-qr canvas,.ct-sec-qr img{width:194px!important;height:194px!important}}
`;
    document.head.append(el('style',{id:'ct-security-style',text:css}));
  }

  function gate(){
    style();
    let root=document.getElementById('ctSecurityGate');
    if(!root){root=el('div',{id:'ctSecurityGate','aria-live':'polite'});document.body.append(root);}
    return root;
  }

  function alertBox(){return el('div',{class:'ct-sec-alert',role:'alert'});}
  function showError(box,error){
    const text=String(error?.message||error||'요청을 처리하지 못했습니다.');
    box.textContent=text;box.classList.add('show');
  }
  function setBusy(button,busy,label='처리 중…'){
    state.busy=busy;
    if(button){if(!button.dataset.label)button.dataset.label=button.textContent;button.disabled=busy;button.textContent=busy?label:button.dataset.label;}
  }

  function otpInput(){
    const input=el('input',{class:'ct-sec-code',inputmode:'numeric',autocomplete:'one-time-code',maxlength:'6',pattern:'[0-9]*',placeholder:'000000'});
    input.addEventListener('input',()=>{input.value=input.value.replace(/\D/g,'').slice(0,6);});
    return input;
  }

  function loadQrLibrary(){
    if(window.QRCode)return Promise.resolve(window.QRCode);
    return new Promise((resolve,reject)=>{
      const previous=document.getElementById('ct-qrcode-lib');
      if(previous){
        previous.addEventListener('load',()=>window.QRCode?resolve(window.QRCode):reject(new Error('QR 라이브러리를 불러오지 못했습니다.')),{once:true});
        previous.addEventListener('error',()=>reject(new Error('QR 라이브러리를 불러오지 못했습니다.')),{once:true});
        return;
      }
      const script=document.createElement('script');
      script.id='ct-qrcode-lib';
      script.src=QR_LIB_URL;
      script.integrity=QR_LIB_INTEGRITY;
      script.crossOrigin='anonymous';
      script.referrerPolicy='no-referrer';
      script.onload=()=>window.QRCode?resolve(window.QRCode):reject(new Error('QR 라이브러리를 불러오지 못했습니다.'));
      script.onerror=()=>reject(new Error('QR 라이브러리를 불러오지 못했습니다.'));
      document.head.append(script);
    });
  }

  async function renderQr(container,uri){
    if(!uri){container.replaceChildren(el('div',{class:'ct-sec-qr-fallback',text:'QR 정보를 만들지 못했습니다. 아래 직접 등록을 이용해주세요.'}));return;}
    try{
      const QR=await loadQrLibrary();
      container.replaceChildren();
      new QR(container,{text:uri,width:204,height:204,colorDark:'#111827',colorLight:'#ffffff',correctLevel:QR.CorrectLevel.M});
      container.removeAttribute('title');
    }catch(_error){
      container.replaceChildren(el('div',{class:'ct-sec-qr-fallback',text:'QR 표시가 안 되면 아래 “인증 앱으로 바로 등록” 또는 “직접 입력하기”를 이용하세요.'}));
    }
  }

  async function api(path,options={}){
    const response=await fetch(path,{credentials:'include',cache:'no-store',...options,headers:{'Content-Type':'application/json',...(options.headers||{})}});
    const data=await response.json().catch(()=>({}));
    if(!response.ok){const error=new Error(data.error||data.message||`요청 실패 (${response.status})`);error.code=data.code||'';error.status=response.status;throw error;}
    return data;
  }

  async function refresh(){
    try{
      const security=await api(API.security,{method:'GET',headers:{}});
      if(security.settlementVerified){unlock();return;}
      if(!security.totpEnrolled){renderEnrollIntro(security);return;}
      renderVerify(security);
    }catch(error){
      if(error.status===401||error.code==='PARTNER_LOGIN_REQUIRED'||error.code==='AUTH_SESSION_INVALID'){renderLogin();return;}
      renderFatal(error);
    }
  }

  function unlock(){
    const root=document.getElementById('ctSecurityGate');
    if(root)root.remove();
    document.documentElement.classList.add('ct-settlement-secure');
  }

  function cardBase(title,desc){
    const card=el('section',{class:'ct-sec-card'});
    card.append(el('div',{class:'ct-sec-brand',text:'CALLTAG × PAGERO'}),el('h1',{text:title}),el('p',{class:'ct-sec-desc',text:desc}));
    return card;
  }

  function renderLogin(){
    const root=gate();root.replaceChildren();
    const card=cardBase('파트너 정산 로그인','페이지로와 콜태그에서 사용하는 동일한 계정으로 로그인하세요.');
    const form=el('form');
    const email=el('input',{type:'email',autocomplete:'username',required:true,placeholder:'name@example.com'});
    const password=el('input',{type:'password',autocomplete:'current-password',required:true,placeholder:'비밀번호'});
    const submit=el('button',{class:'ct-sec-btn',type:'submit',text:'로그인'});
    const errorBox=alertBox();
    form.append(field('이메일',email),field('비밀번호',password),submit,errorBox);
    form.addEventListener('submit',async event=>{
      event.preventDefault();if(state.busy)return;errorBox.classList.remove('show');setBusy(submit,true,'로그인 중…');
      try{await api(API.login,{method:'POST',body:JSON.stringify({email:email.value,password:password.value})});await refresh();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(submit,false);}
    });
    card.append(form);root.append(card);setTimeout(()=>email.focus(),30);
  }

  function field(label,input){const wrap=el('div',{class:'ct-sec-field'});wrap.append(el('label',{text:label}),input);return wrap;}

  function renderEnrollIntro(security={}){
    const root=gate();root.replaceChildren();
    const card=cardBase('구글 OTP 등록','정산정보를 안전하게 보호하기 위해 Google Authenticator를 한 번만 등록합니다.');
    card.append(el('span',{class:'ct-sec-badge',text:'최초 1회만'}));
    if(security.email)card.append(el('p',{class:'ct-sec-help',html:`로그인 계정: <span class="ct-sec-email">${escapeHtml(security.email)}</span>`}));
    const start=el('button',{class:'ct-sec-btn',type:'button',text:'QR 코드 만들기'});
    const errorBox=alertBox();
    start.addEventListener('click',async()=>{
      if(state.busy)return;setBusy(start,true,'QR 코드 만드는 중…');errorBox.classList.remove('show');
      try{state.setup=await api(API.setup,{method:'POST',body:'{}'});renderEnrollSetup();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(start,false);}
    });
    card.append(start,errorBox);root.append(card);
  }

  function renderEnrollSetup(){
    const setup=state.setup||{};const root=gate();root.replaceChildren();
    const card=cardBase('Google Authenticator 등록','휴대폰의 Google Authenticator(구글 OTP) 앱으로 아래 QR 코드를 스캔하세요.');

    const steps=el('ol',{class:'ct-sec-steps'});
    ['Google Authenticator 앱을 엽니다.','오른쪽 아래 + 버튼 → QR 코드 스캔을 누릅니다.','아래 QR을 찍고 앱에 뜬 6자리 숫자를 입력합니다.'].forEach((text,index)=>{
      steps.append(el('li',{class:'ct-sec-step'},[el('b',{text:String(index+1)}),el('span',{text})]));
    });
    card.append(steps);

    const qr=el('div',{class:'ct-sec-qr','aria-label':'Google Authenticator 등록 QR 코드'});
    const qrWrap=el('div',{class:'ct-sec-qr-wrap'},qr);
    card.append(qrWrap);
    renderQr(qr,setup.otpauthUri||'');

    if(setup.otpauthUri){
      card.append(
        el('a',{class:'ct-sec-app-link',href:setup.otpauthUri,text:'휴대폰에서 인증 앱으로 바로 등록'}),
        el('p',{class:'ct-sec-mobile-note',text:'지금 이 페이지를 휴대폰에서 보고 있다면 이 버튼을 누르는 게 가장 빠릅니다.'})
      );
    }

    const manual=el('details',{class:'ct-sec-manual'});
    const manualBody=el('div',{class:'ct-sec-manual-body'});
    const secret=el('div',{class:'ct-sec-key',text:setup.manualSecret||''});
    const copy=el('button',{class:'ct-sec-btn secondary ct-sec-copy',type:'button',text:'설정 키 복사'});
    copy.addEventListener('click',async()=>{
      try{await navigator.clipboard.writeText(setup.manualSecret||'');copy.textContent='복사됨';setTimeout(()=>{copy.textContent='설정 키 복사';},1200);}catch(_error){copy.textContent='직접 길게 눌러 복사하세요';}
    });
    manualBody.append(el('p',{class:'ct-sec-help',text:'QR 스캔이 안 될 때만 아래 키를 Google Authenticator의 “설정 키 입력”에 붙여넣으세요.'}),secret,copy);
    manual.append(el('summary',{text:'QR을 사용할 수 없나요? 직접 입력하기'}),manualBody);
    card.append(manual);

    const code=otpInput();
    const confirm=el('button',{class:'ct-sec-btn',type:'button',text:'인증 완료'});
    const errorBox=alertBox();
    const submit=async()=>{
      if(state.busy)return;
      if(code.value.length!==6){showError(errorBox,'Google Authenticator에 표시된 6자리 숫자를 입력해주세요.');return;}
      setBusy(confirm,true,'확인 중…');errorBox.classList.remove('show');
      try{await api(API.enable,{method:'POST',body:JSON.stringify({code:code.value})});location.reload();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(confirm,false);}
    };
    confirm.addEventListener('click',submit);code.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();submit();}});
    card.append(field('Google Authenticator의 6자리 숫자',code),confirm,errorBox);root.append(card);
  }

  function renderVerify(security={}){
    const root=gate();root.replaceChildren();
    const card=cardBase('구글 OTP 인증','Google Authenticator에 표시된 현재 6자리 숫자를 입력하세요.');
    if(security.email)card.append(el('p',{class:'ct-sec-help',html:`로그인 계정: <span class="ct-sec-email">${escapeHtml(security.email)}</span>`}));
    const code=otpInput();
    const verify=el('button',{class:'ct-sec-btn',type:'button',text:'정산페이지 열기'});
    const recovery=el('button',{class:'ct-sec-btn link',type:'button',text:'휴대폰을 바꿨거나 인증 앱을 사용할 수 없나요?'});
    const errorBox=alertBox();
    const submit=async()=>{
      if(state.busy)return;
      if(code.value.length!==6){showError(errorBox,'6자리 숫자를 입력해주세요.');return;}
      setBusy(verify,true,'인증 중…');errorBox.classList.remove('show');
      try{await api(API.verify,{method:'POST',body:JSON.stringify({code:code.value})});location.reload();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(verify,false);}
    };
    verify.addEventListener('click',submit);code.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();submit();}});
    recovery.addEventListener('click',()=>renderRecoveryStart(security));
    card.append(field('6자리 숫자',code),verify,recovery,errorBox);root.append(card);setTimeout(()=>code.focus(),30);
  }

  function renderRecoveryStart(security={}){
    const root=gate();root.replaceChildren();
    const card=cardBase('이메일로 인증 앱 복구','로그인 계정 이메일로 복구 코드를 보냅니다. 확인 후 새 Google Authenticator를 다시 등록할 수 있습니다.');
    const send=el('button',{class:'ct-sec-btn',type:'button',text:'복구 코드 이메일 받기'});
    const back=el('button',{class:'ct-sec-btn secondary',type:'button',text:'구글 OTP 입력으로 돌아가기'});
    const errorBox=alertBox();
    send.addEventListener('click',async()=>{
      if(state.busy)return;setBusy(send,true,'메일 발송 중…');errorBox.classList.remove('show');
      try{const result=await api(API.recoveryEmail,{method:'POST',body:'{}'});state.recoveryEmail=result.email||security.email||'';renderRecoveryCode();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(send,false);}
    });
    back.addEventListener('click',()=>renderVerify(security));card.append(send,back,errorBox);root.append(card);
  }

  function renderRecoveryCode(){
    const root=gate();root.replaceChildren();
    const card=cardBase('복구 코드 확인',`${state.recoveryEmail||'로그인 이메일'}로 보낸 6자리 코드를 입력하세요.`);
    const code=otpInput();
    const confirm=el('button',{class:'ct-sec-btn',type:'button',text:'기존 인증 앱 초기화'});
    const errorBox=alertBox();
    confirm.addEventListener('click',async()=>{
      if(state.busy)return;
      if(code.value.length!==6){showError(errorBox,'이메일로 받은 6자리 숫자를 입력해주세요.');return;}
      setBusy(confirm,true,'확인 중…');errorBox.classList.remove('show');
      try{await api(API.recover,{method:'POST',body:JSON.stringify({code:code.value})});state.setup=null;await refresh();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(confirm,false);}
    });
    card.append(field('이메일 복구 코드',code),confirm,errorBox);root.append(card);setTimeout(()=>code.focus(),30);
  }

  function renderFatal(error){
    const root=gate();root.replaceChildren();const card=cardBase('정산 보안 설정 확인 필요','정산페이지 보안 기능을 준비하지 못했습니다. 관리자 설정을 확인해주세요.');
    const box=alertBox();showError(box,error);const retry=el('button',{class:'ct-sec-btn secondary',type:'button',text:'다시 확인',onclick:refresh});card.append(box,retry);root.append(card);
  }

  function escapeHtml(value=''){return String(value).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');}

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',refresh,{once:true});else refresh();
})();