(()=>{
  if(document.documentElement.dataset.ctCtaSystem)return;
  document.documentElement.dataset.ctCtaSystem='1';

  const APP='https://pagero.kr/app';
  const GOOGLE_PLAY='https://play.google.com/store/search?q=%EC%BD%9C%ED%83%9C%EA%B7%B8&c=apps';
  const PLAY_LOGO='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAYAAABXAvmHAAAJdklEQVR42sVabYyU1RV+zj33nZndndlFBSMsWmkN/rE22qYf1nSWaFotLiJ2NjFtraaJlJpqG5OmWtvJIEL9qNHW4ldS07QaM5OICNGmC27GAnHBFWNjaqwCGokKuwuz8/m+77339MfsLLuysIvMwM38mEzmffM89zzPuefcewnjIy1plaGMu+b5Gy5XXu0OcbhcBHNI0ahSeNU698jmZS9sA4DkQFLne/IWBMFpHgQAqWyKc305e0X2F3cmOvasFtWhXRBCnIAUgaMMG9qQgGfCUNa9vOKFdwEgmU7qfCZvgdNHhJAVRh/Z89ZXb23v8h/t9jJhR/Q/ytouJrIAICIiRKS8eATWNxUR+UstwIP91284MHkCTlsEFj8xNjd09L6otjhM1Z5/5h+oq31QG9cFwhFcImKVUqzjHkzVfCwiDxRHRh/L35yvQUCpXEqdaiIKAAJDV6i2eCeZkoDY2zt6txqtXBFqdRgCPsKWiMWJhGOBIcF8r817qHPuWUO9G5ffAILk+nI2lU1xOp1WpzQCi9aP3cXxxBpTKlqCaIDhJOq65zxm58U3auu6qC7zSVIXiECcjmomreACmxcnqzddu+GVCVmlcq7VRlcAIA7cIFP/yUFRTX106Fb9ydiPDNOYTOLb+EpExMY3LqwEVnkqqbTaumzziuzVG679cq4vZ0GQ5EBSQyY/2AIC0wdGoFWZPi7cpPcXbjGKygKSqSTqslIE4rAaWuMb4QinPOZdvZtWPLJ009Lu/JK8AUFS2RSfQgJ1EgJAqzE6UEx5Hx76pSH4jshBpnmMQExEFJQC66yL6pi+jRF9s3fTijtT2VR83NzUbCJqJosIFLQqYLR8tbdv9DdWxDlF4bQkGkYHIGExMCIyV8f0Wr/N7O7duOImAMj15Wxa0iotzTE6AcD5j479TncmVptS0RBIT/9HC+O6kIgOmS/MXUtMPjuJTUmzRw2BALAqojRHGNa3rzlxqzcve+HlCaO/nRNk4FoUgclYGFoVUPQv1XsOrhHjOi2r6pQ0O52uQNA2tC4oBZa0+iZ7+qXezSs2Lt2w/Ku5vpxFBu5kjH5CYayTGEM1vFC/f3CtBOHZhlX5+CTqPBQRsamGzlRDxx4vU5oGl22+7onebO+ihtFlAFpwYkROWIcCBlMRvlmo3zu4lqr+IqNVcUYSExmLSIWVwIoR5qi+BW1693Wbrs10PbPqDFoCQ4BIFtwyAkdIlGHcWfz+8L1Uql1ktBqbFYlGxgKAoBgadtJViXX8/sY5/3vD7IiuHBhIauqDlTSUyMz4PncmEDAUVeHQxntGMqpQ+YbRqjBrEgCgSXTRsswvjfh3x18/n9vk8e9EX9sp2yPLKQNHBCdZsKSPjfOkUpmAQQgAsNo7ercaLV95VP10rMEQlOHhXFVy98/Zoc9GUWxBWcW4BBHaIDsi/5R87DLqg6UMnAxAyzRGV81ZSkIoMurDQ3fwweLyUKvDUv+djg1ePCykor2/awfmcZUdPGISRhUOZThE6HvwZLtsj/5NXo0spiUwREf7o0lV46T66fDP9SeFHxum4tH102TwqmQmwIuGwsTfFQgKJVgEELThRjC9Iduj98kA5lIf7GQSTSx7G/VTiT4e+4neX1h5VP3UkM1CVbT3d22neVxlOxn81NcxCIQxWFh0oB2/RiyyW/oj11AfbMPgTa7bCQIar5+u9z489CtD8B2oXu6WRU+VjWjwTNU2geEgKMAAtBCdtFEGolc2DN6CxmNy/XSVt2/kLsdiXAWCbqpML5tZ5F0QNHwYAAokT8oAYkjBtaxzEjCYCyhXv6XfGlkn50QoePCsf9M8VTu2bGYmolGFRZwWAdEeIkhLWz9tBeWY4JwDXViXPSRzSyFcVIHdSQZYk4Dk4hZ4YBJ4Z1CIduJLhz8w2W03Y/67Y9HikxcJPvUM2g1gqWn5r2XgLzi8xz63dSXNr4xwKRGFGvV09fELye7rMIh/ThICghGC0FstITAV/Cp0V4e5pNvB1gFRC1Q1155aTOadToOEAdwJkTBoA6Mk+xD18yIg1RLwhb3mua2rsKB2gEu6HVrGmx5HgOdAVnHtrxcos/uMEPFwZhICgcDAg643SXQLXYYqclC6NTP/M1pQO8gl3XEEfGM4grCDcqRqf/8iohUOvW8Pa5Q0Qcl04C0UGAlo+LIfNayinqBfBIoIVrVENrXh6cFPgKrnPxURVcst0sHWcww6jHxm68lCIEiAwaigjD+iEFxKPcEmyYKJ6m2obrJsqC6b44A/MrMQAlSbpeDFc7VU2USv3q/hc10wHWCEAGp4FqGsoZ7gvwAgWTD1HWnE9SmRzXFIAIDqMBT8awFLRfuxH3wUAwMI5BWEKkPJ2qsAIAPQ6IGlz+wi6GbKpngi4CcFQhxsLGG1N7ggZhTe0j/ct4a+ZnONGcfbEFoC07R1YMZsM8vhRCwTKO552gn2+2212/fed9vX6WKbEwE15ELH2XbRp1w249v0IOKE57FvbSkw9s9BaB/qzA8Ng3ZBHHhcKjO+VLU02xwN3DkRF/c0ayIJnXnaBy6Nbhm8qzM/NDyQTGoREBFmPRu65dmmrnMnIhLXzAAhcPYlR7S6rX/nIABIMqmRz1vK501LaqEpM79l5QR4ngG8ACIQE1OkEp7HgcjrgciyaP/OpR39g4OSSrEAivJ5Q5/znE23KNuIQKwmpWNaa9/Yvc66tS+eMfh0X66+5wMAlDn54yjdbNmIiCUijnueDowd9a19uALzpzO3DBUAQFIpbgbwGSV0orJxIk4EDYOGgTHryzCXxPoH7zlzy1BBUvVzAco19xBQn6xsROAAkYTWbCEInDxvyN0T79/15hSD5lpzelnXosLEYfVsFykBxInYNlYq7mn2xW6rGvlutP+16+P9u94cNyidjEFnHQEH2qMcSDs74yIl9Y/1SOmopzkw9p0glHvbX9n5DwCQdFoBGTRT5zOe0FzwyNg80dhTaE+0Lx5+zz63dZWqg5868/Wln7jd0/CN+RTAgyMxu75781BFAEIqpVollWMTGL9q0P2Euf0rwUcPP/XST8OzKwe47MVJiyUZv2qgiFSHp+EbWxXQYza0D8Tzuz6ZyCy503jVoAHgg+9f9dvzap9mrNfOoQ1hBWAixJSCL9ZA6NkgNOs680PvAMBAMql78nlLp/WyR0PbaSjKwB248vJkF4I7HOgyC+li4LACbbOwD7dveT0/JbPg9F+3+T8MxMTwxu63gwAAAABJRU5ErkJggg==';
  const plans={
    CALL:{key:'call',store:true},
    MESSAGE:{key:'message',store:true},
    PAGERO:{key:'pagero',label:'페이지로 시작하기'},
    'ALL IN ONE':{key:'all',label:'통합권 7일 무료체험'}
  };

  const makeUrl=(plan='all',position='site')=>{
    const url=new URL(APP);
    url.searchParams.set('source','calltag');
    url.searchParams.set('plan',plan);
    url.searchParams.set('utm_source','calltag_site');
    url.searchParams.set('utm_medium','cta');
    url.searchParams.set('utm_campaign','2026_launch');
    url.searchParams.set('utm_content',position);
    return url.toString();
  };

  const configureLink=(link,plan,position,label)=>{
    if(!link)return;
    link.className='ct-plan-cta';
    link.href=makeUrl(plan,position);
    link.target='_blank';
    link.rel='noopener';
    link.dataset.ctUnifiedCta='1';
    link.textContent=label||'시작하기';
    link.setAttribute('aria-label',`${label||'콜태그 시작'} · 새 창`);
  };

  const configureGooglePlay=(link)=>{
    if(!link)return;
    link.className='ct-plan-cta ct-google-play-cta';
    link.href=GOOGLE_PLAY;
    link.target='_blank';
    link.rel='noopener';
    link.dataset.ctUnifiedCta='1';
    link.innerHTML=`<img src="${PLAY_LOGO}" alt="" aria-hidden="true"><span><small>Google Play</small><strong>앱 다운로드</strong></span>`;
    link.setAttribute('aria-label','Google Play에서 콜태그 앱 다운로드 · 새 창');
  };

  const addPricingButtons=()=>{
    document.querySelectorAll('#pricing .ct-plan-card').forEach(card=>{
      const code=card.querySelector(':scope>small')?.textContent.trim().toUpperCase()||'';
      const config=plans[code]||plans['ALL IN ONE'];
      let link=card.querySelector('.ct-plan-cta');
      if(!link){
        link=document.createElement('a');
        card.append(link);
      }
      if(config.store){
        configureGooglePlay(link);
      }else{
        configureLink(link,config.key,`pricing_${config.key}`,config.label);
        if(card.classList.contains('all'))link.classList.add('primary');
      }
    });
  };

  const unifyExistingLinks=()=>{
    document.querySelectorAll('.hero-heading .ad-btn.primary,.ad-final .ad-btn.primary,.ad-sticky a,.price-button').forEach((link,index)=>{
      const position=link.closest('.ad-sticky')?'sticky':link.closest('.ad-final')?'final':link.closest('.hero-heading')?'hero':`legacy_${index+1}`;
      configureLink(link,'all',position,position==='sticky'?'7일 무료체험 시작':'7일 무료로 시작하기');
    });
    document.querySelectorAll('#pricing .ad-price a').forEach((link,index)=>{
      const plan=['call','message','all'][index]||'all';
      configureLink(link,plan,`legacy_pricing_${plan}`);
    });
  };

  const installStyle=()=>{
    let style=document.querySelector('style[data-ct-cta-system]');
    if(!style){
      style=document.createElement('style');
      style.dataset.ctCtaSystem='1';
      document.head.append(style);
    }
    style.textContent=`
      #pricing .ct-plan-card{display:flex!important;flex-direction:column!important}
      #pricing .ct-plan-points{margin-bottom:24px!important}
      .ct-plan-cta{min-height:50px;display:flex;align-items:center;justify-content:center;margin-top:auto;padding:0 15px;border:1px solid rgba(124,153,255,.42);border-radius:12px;background:rgba(59,111,255,.09);color:#eef1ff;font-size:13px;font-weight:900;text-align:center;text-decoration:none;transition:transform .18s ease,border-color .18s ease,background .18s ease}
      .ct-plan-cta:hover,.ct-plan-cta:focus-visible{border-color:#7595ff;background:rgba(59,111,255,.2);transform:translateY(-2px);outline:none}
      .ct-plan-cta.primary{border-color:#3b6fff;background:#3b6fff;color:#fff;box-shadow:0 15px 36px rgba(59,111,255,.24)}
      .ct-plan-cta.primary:hover,.ct-plan-cta.primary:focus-visible{background:#527dff}
      .ct-google-play-cta{min-height:58px!important;justify-content:center!important;gap:12px!important;padding:8px 18px!important;border-color:rgba(255,255,255,.18)!important;background:#07090d!important;color:#fff!important;box-shadow:0 12px 30px rgba(0,0,0,.24)}
      .ct-google-play-cta:hover,.ct-google-play-cta:focus-visible{border-color:rgba(255,255,255,.42)!important;background:#0c0f15!important}
      .ct-google-play-cta img{width:30px;height:30px;display:block;flex:0 0 30px;object-fit:contain}
      .ct-google-play-cta span{display:flex;flex-direction:column;align-items:flex-start;gap:2px;line-height:1}
      .ct-google-play-cta small{color:#aeb5c2;font-size:9px;font-weight:700;letter-spacing:.02em}
      .ct-google-play-cta strong{color:#fff;font-size:15px;font-weight:900;letter-spacing:-.025em}
      @media(max-width:650px){.ct-plan-cta{min-height:48px;font-size:13px}.ct-plan-points{margin-bottom:20px!important}.ct-google-play-cta{min-height:56px!important}}
    `;
  };

  const apply=()=>{
    installStyle();
    addPricingButtons();
    unifyExistingLinks();
  };

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(apply),{once:true});
  else requestAnimationFrame(apply);

  const observer=new MutationObserver(()=>requestAnimationFrame(apply));
  observer.observe(document.documentElement,{childList:true,subtree:true});
  setTimeout(()=>observer.disconnect(),12000);
})();
