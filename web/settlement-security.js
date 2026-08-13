(()=>{
  'use strict';

  const API={
    security:'/api/partner/security',
    login:'/api/partner/login',
    setup:'/api/partner/totp/setup',
    enable:'/api/partner/totp/enable',
    verify:'/api/partner/totp/verify',
    recoveryEmail:'/api/partner/totp/recovery-email',
    recover:'/api/partner/totp/recover',
  };

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
.ct-sec-brand{font-weight:900;font-size:13px;letter-spacing:.08em;color:#2563eb;margin-bottom:14px}.ct-sec-card h1{margin:0;font-size:27px;line-height:1.25}.ct-sec-desc{margin:10px 0 24px;color:#64748b;font-size:14px;line-height:1.65}.ct-sec-field{display:grid;gap:7px;margin:14px 0}.ct-sec-field label{font-size:13px;font-weight:800;color:#374151}.ct-sec-field input{width:100%;height:50px;border:1px solid #d1d5db;border-radius:13px;padding:0 14px;font-size:16px;outline:none;background:#fff}.ct-sec-field input:focus{border-color:#2563eb;box-shadow:0 0 0 3px rgba(37,99,235,.12)}
.ct-sec-code{font-size:25px!important;letter-spacing:.18em;text-align:center;font-weight:900}.ct-sec-btn{width:100%;height:50px;border:0;border-radius:13px;background:#111827;color:#fff;font-size:15px;font-weight:900;cursor:pointer;margin-top:8px}.ct-sec-btn:disabled{opacity:.5;cursor:wait}.ct-sec-btn.secondary{background:#fff;color:#111827;border:1px solid #d1d5db}.ct-sec-btn.link{height:auto;background:transparent;color:#2563eb;padding:10px 0;margin-top:8px;font-size:14px}
.ct-sec-alert{display:none;margin:14px 0 0;padding:12px 14px;border-radius:12px;background:#fff1f2;color:#be123c;font-size:13px;line-height:1.5}.ct-sec-alert.show{display:block}.ct-sec-key{margin:14px 0;padding:14px;border-radius:13px;background:#f8fafc;border:1px solid #e2e8f0;word-break:break-all;font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-weight:800;font-size:14px;letter-spacing:.06em}.ct-sec-help{font-size:13px;color:#64748b;line-height:1.65;margin:10px 0}.ct-sec-app-link{display:flex;align-items:center;justify-content:center;text-decoration:none;height:46px;border-radius:12px;border:1px solid #cbd5e1;color:#111827;font-weight:850;margin:10px 0}.ct-sec-divider{height:1px;background:#e5e7eb;margin:22px 0}.ct-sec-email{font-weight:800;color:#111827}.ct-sec-badge{display:inline-flex;padding:6px 10px;border-radius:999px;background:#eef2ff;color:#3730a3;font-size:12px;font-weight:900;margin-bottom:12px}
@media(max-width:520px){#ctSecurityGate{padding:0;background:#fff;align-items:stretch}.ct-sec-card{width:100%;min-height:100vh;border:0;border-radius:0;box-shadow:none;padding:32px 22px;display:flex;flex-direction:column;justify-content:center}.ct-sec-card h1{font-size:25px}}
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
    const card=cardBase('파트너 정산 로그인','페이지로와 콜태그에서 사용하는 동일한 계정으로 로그인하세요. 정산정보는 로그인 후 인증 앱 2차 인증까지 완료해야 열립니다.');
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
    const card=cardBase('인증 앱 등록','정산페이지 보호를 위해 Google Authenticator 등 TOTP 인증 앱을 한 번 등록해야 합니다. 이후 정산 로그인에는 이메일 발송 비용 없이 6자리 코드만 사용합니다.');
    card.append(el('span',{class:'ct-sec-badge',text:'최초 1회 설정'}));
    if(security.email)card.append(el('p',{class:'ct-sec-help',html:`로그인 계정: <span class="ct-sec-email">${escapeHtml(security.email)}</span>`}));
    const start=el('button',{class:'ct-sec-btn',type:'button',text:'인증 앱 등록 시작'});
    const errorBox=alertBox();
    start.addEventListener('click',async()=>{
      if(state.busy)return;setBusy(start,true,'보안키 생성 중…');errorBox.classList.remove('show');
      try{state.setup=await api(API.setup,{method:'POST',body:'{}'});renderEnrollSetup();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(start,false);}
    });
    card.append(start,errorBox);root.append(card);
  }

  function renderEnrollSetup(){
    const setup=state.setup||{};const root=gate();root.replaceChildren();
    const card=cardBase('인증 앱에 등록하세요','Google Authenticator에서 “설정 키 입력”을 선택해 아래 키를 등록하세요. 휴대폰에서는 인증 앱으로 바로 열 수도 있습니다.');
    card.append(el('div',{class:'ct-sec-key',text:setup.manualSecret||''}));
    if(setup.otpauthUri)card.append(el('a',{class:'ct-sec-app-link',href:setup.otpauthUri,text:'인증 앱으로 열기'}));
    card.append(el('p',{class:'ct-sec-help',text:'계정 이름은 CallTag, 키 유형은 시간 기반(TOTP)으로 설정하세요. 이 키는 현재 등록 과정에서만 표시됩니다.'}));
    const code=el('input',{class:'ct-sec-code',inputmode:'numeric',autocomplete:'one-time-code',maxlength:'6',placeholder:'000000'});
    const confirm=el('button',{class:'ct-sec-btn',type:'button',text:'6자리 코드 확인'});
    const errorBox=alertBox();
    confirm.addEventListener('click',async()=>{
      if(state.busy)return;setBusy(confirm,true,'확인 중…');errorBox.classList.remove('show');
      try{await api(API.enable,{method:'POST',body:JSON.stringify({code:code.value})});location.reload();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(confirm,false);}
    });
    card.append(field('인증 앱에 표시된 코드',code),confirm,errorBox);root.append(card);setTimeout(()=>code.focus(),30);
  }

  function renderVerify(security={}){
    const root=gate();root.replaceChildren();
    const card=cardBase('2차 인증','인증 앱에 표시되는 현재 6자리 코드를 입력하면 정산정보가 열립니다.');
    if(security.email)card.append(el('p',{class:'ct-sec-help',html:`로그인 계정: <span class="ct-sec-email">${escapeHtml(security.email)}</span>`}));
    const code=el('input',{class:'ct-sec-code',inputmode:'numeric',autocomplete:'one-time-code',maxlength:'6',placeholder:'000000'});
    const verify=el('button',{class:'ct-sec-btn',type:'button',text:'정산페이지 열기'});
    const recovery=el('button',{class:'ct-sec-btn link',type:'button',text:'인증 앱을 사용할 수 없나요? 이메일로 복구'});
    const errorBox=alertBox();
    const submit=async()=>{
      if(state.busy)return;setBusy(verify,true,'인증 중…');errorBox.classList.remove('show');
      try{await api(API.verify,{method:'POST',body:JSON.stringify({code:code.value})});location.reload();}
      catch(error){showError(errorBox,error);}
      finally{setBusy(verify,false);}
    };
    verify.addEventListener('click',submit);code.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();submit();}});
    recovery.addEventListener('click',()=>renderRecoveryStart(security));
    card.append(field('인증 코드',code),verify,recovery,errorBox);root.append(card);setTimeout(()=>code.focus(),30);
  }

  function renderRecoveryStart(security={}){
    const root=gate();root.replaceChildren();
    const card=cardBase('이메일로 인증 앱 복구','로그인 계정 이메일로 복구 코드를 보냅니다. 코드 확인이 끝나면 기존 인증 앱 등록이 폐기되고 새 인증 앱을 등록해야 합니다.');
    const send=el('button',{class:'ct-sec-btn',type:'button',text:'복구 코드 이메일 받기'});
    const back=el('button',{class:'ct-sec-btn secondary',type:'button',text:'인증 코드 입력으로 돌아가기'});
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
    const code=el('input',{class:'ct-sec-code',inputmode:'numeric',autocomplete:'one-time-code',maxlength:'6',placeholder:'000000'});
    const confirm=el('button',{class:'ct-sec-btn',type:'button',text:'기존 인증 앱 초기화'});
    const errorBox=alertBox();
    confirm.addEventListener('click',async()=>{
      if(state.busy)return;setBusy(confirm,true,'확인 중…');errorBox.classList.remove('show');
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
