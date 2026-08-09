(()=>{
  if(document.documentElement.dataset.ctCtaSystemV3)return;
  document.documentElement.dataset.ctCtaSystemV3='1';
  const APP='https://pagero.kr/app';
  const GOOGLE_PLAY='https://play.google.com/store/search?q=%EC%BD%9C%ED%83%9C%EA%B7%B8&c=apps';
  const PLAY_LOGO='/assets/google-play-mark.svg?v=20260809-1';
  const plans={CALL:{key:'call',store:true},MESSAGE:{key:'message',store:true},PAGERO:{key:'pagero',label:'페이지로 시작하기'},'ALL IN ONE':{key:'all',label:'통합권 3일 무료체험'}};
  const makeUrl=(plan='all',position='site')=>{const url=new URL(APP);url.searchParams.set('source','calltag');url.searchParams.set('plan',plan);url.searchParams.set('utm_source','calltag_site');url.searchParams.set('utm_medium','cta');url.searchParams.set('utm_campaign','2026_launch');url.searchParams.set('utm_content',position);return url.toString();};
  const configureLink=(link,plan,position,label)=>{if(!link)return;link.className='ct-plan-cta';link.href=makeUrl(plan,position);link.target='_blank';link.rel='noopener';link.dataset.ctUnifiedCta='1';link.textContent=label||'시작하기';link.setAttribute('aria-label',`${label||'콜태그 시작'} · 새 창`);};
  const configureGooglePlay=link=>{if(!link)return;link.className='ct-plan-cta ct-google-play-cta';link.href=GOOGLE_PLAY;link.target='_blank';link.rel='noopener';link.dataset.ctUnifiedCta='1';link.innerHTML=`<img src="${PLAY_LOGO}" alt="" aria-hidden="true"><span><small>Google Play</small><strong>앱 다운로드</strong></span>`;link.setAttribute('aria-label','Google Play에서 콜태그 앱 다운로드 · 새 창');};
  const addPricingButtons=()=>{document.querySelectorAll('#pricing .ct-plan-card').forEach(card=>{const code=card.querySelector(':scope>small')?.textContent.trim().toUpperCase()||'';const config=plans[code]||plans['ALL IN ONE'];let link=card.querySelector('.ct-plan-cta');if(!link){link=document.createElement('a');card.append(link);}if(config.store)configureGooglePlay(link);else{configureLink(link,config.key,`pricing_${config.key}`,config.label);if(card.classList.contains('all'))link.classList.add('primary');}});};
  const unifyExistingLinks=()=>{document.querySelectorAll('.hero-heading .ad-btn.primary,.ad-final .ad-btn.primary,.ad-sticky a,.price-button').forEach((link,index)=>{const position=link.closest('.ad-sticky')?'sticky':link.closest('.ad-final')?'final':link.closest('.hero-heading')?'hero':`legacy_${index+1}`;configureLink(link,'all',position,position==='sticky'?'3일 무료체험 시작':'3일 무료로 시작하기');});document.querySelectorAll('#pricing .ad-price a').forEach((link,index)=>{const plan=['call','message','all'][index]||'all';configureLink(link,plan,`legacy_pricing_${plan}`);});};
  const apply=()=>{addPricingButtons();unifyExistingLinks();};
  const boot=()=>{apply();const timers=[250,1000,2500].map(delay=>setTimeout(apply,delay));window.addEventListener('pagehide',()=>timers.forEach(clearTimeout),{once:true});};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();