const CANONICAL='https://calltag.pagero.kr/';
const SEO_TITLE='콜태그 | 통화 후 고객관리·자동문자·페이지로 문의 연동';
const SEO_DESCRIPTION='통화가 끝나면 고객을 태그하고 상담 상태·다음 할 일·재연락 일정을 관리하세요. 페이지로 랜딩페이지 문의 자동등록과 안내·후속문자까지 연결하는 Android 고객관리 서비스입니다.';
const OG_IMAGE=`${CANONICAL}assets/calltag-og-20260805.png`;
const WORKER_VERSION='v118-runtime32';
const RUNTIME_SRC='/assets/calltag-runtime-loader.js?v=20260809-runtime32';

const SEO_SCHEMA={
  '@context':'https://schema.org',
  '@graph':[
    {'@type':'Organization','@id':`${CANONICAL}#organization`,name:'웨이지',alternateName:'WAYZI',url:'https://pagero.kr/',brand:[{'@type':'Brand',name:'콜태그'},{'@type':'Brand',name:'페이지로'}]},
    {'@type':'WebSite','@id':`${CANONICAL}#website`,url:CANONICAL,name:'콜태그',alternateName:'CALLTAG',inLanguage:'ko-KR',publisher:{'@id':`${CANONICAL}#organization`}},
    {'@type':'SoftwareApplication','@id':`${CANONICAL}#software`,name:'콜태그',alternateName:'CALLTAG',url:CANONICAL,applicationCategory:'BusinessApplication',applicationSubCategory:'Customer Relationship Management',operatingSystem:'Android',description:'통화 후 고객 태그, 상담 상태, 다음 할 일, 재연락 일정, 자동문자와 페이지로 문의 연동을 제공하는 고객관리 서비스',provider:{'@id':`${CANONICAL}#organization`},featureList:['통화 후 고객 태그','고객 상태와 상담 이력 관리','다음 할 일과 재연락 일정','안내문자와 후속문자 자동화','페이지로 문의 고객 자동등록','PC 고객관리와 캘린더'],offers:[{'@type':'Offer',name:'전화관리',price:'1900',priceCurrency:'KRW',availability:'https://schema.org/InStock',url:`${CANONICAL}#pricing`},{'@type':'Offer',name:'문자자동화',price:'990',priceCurrency:'KRW',availability:'https://schema.org/InStock',url:`${CANONICAL}#pricing`},{'@type':'Offer',name:'페이지로',price:'3500',priceCurrency:'KRW',availability:'https://schema.org/InStock',url:`${CANONICAL}#pricing`},{'@type':'Offer',name:'통합권',price:'6000',priceCurrency:'KRW',availability:'https://schema.org/InStock',url:`${CANONICAL}#pricing`}]},
    {'@type':'Service','@id':`${CANONICAL}#pagero-service`,name:'페이지로',serviceType:'노코드 랜딩페이지 제작 및 고객 문의 수집',url:'https://pagero.kr/',provider:{'@id':`${CANONICAL}#organization`},offers:{'@type':'Offer',price:'3500',priceCurrency:'KRW',availability:'https://schema.org/InStock'}},
    {'@type':'FAQPage','@id':`${CANONICAL}#faq`,url:`${CANONICAL}#faq`,mainEntity:[
      {'@type':'Question',name:'통화 내용이 녹음되나요?',acceptedAnswer:{'@type':'Answer',text:'아닙니다. 콜태그는 통화 음성을 녹음하거나 대화 내용을 자동 수집하지 않습니다. 통화가 끝난 뒤 사용자가 고객 상태와 다음 할 일을 직접 선택합니다.'}},
      {'@type':'Question',name:'개인 전화도 고객으로 등록되나요?',acceptedAnswer:{'@type':'Answer',text:'개인통화, 거래처, 제외번호를 따로 선택할 수 있습니다. 연락처에 저장된 번호라고 해서 자동으로 고객으로 확정하지 않습니다.'}},
      {'@type':'Question',name:'아이폰에서도 사용할 수 있나요?',acceptedAnswer:{'@type':'Answer',text:'현재 콜태그 앱은 Android 전용으로 개발하고 있습니다. 웹 화면은 PC와 모바일 브라우저에서 확인할 수 있습니다.'}},
      {'@type':'Question',name:'웹에서는 무엇을 볼 수 있나요?',acceptedAnswer:{'@type':'Answer',text:'오늘 해야 할 업무, 기한이 지난 업무, 고객별 상담 이력, 재연락·자료 발송·방문 일정을 확인할 수 있습니다.'}},
      {'@type':'Question',name:'요금제는 어떻게 구성되나요?',acceptedAnswer:{'@type':'Answer',text:'전화관리 월 1,900원, 문자자동화 월 990원, 페이지로 월 3,500원, 통합권 월 6,000원이며 모든 요금은 부가세 별도입니다.'}}
    ]}
  ]
};

const SEO_HEAD=`
<meta name="description" content="${SEO_DESCRIPTION}" />
<meta name="robots" content="index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1" />
<meta name="googlebot" content="index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1" />
<meta name="bingbot" content="index,follow,max-image-preview:large,max-snippet:-1,max-video-preview:-1" />
<meta name="author" content="웨이지" />
<meta name="application-name" content="콜태그" />
<link rel="canonical" href="${CANONICAL}" />
<link rel="alternate" hreflang="ko-KR" href="${CANONICAL}" />
<link rel="alternate" hreflang="x-default" href="${CANONICAL}" />
<meta property="og:type" content="website" />
<meta property="og:locale" content="ko_KR" />
<meta property="og:site_name" content="콜태그" />
<meta property="og:title" content="${SEO_TITLE}" />
<meta property="og:description" content="${SEO_DESCRIPTION}" />
<meta property="og:url" content="${CANONICAL}" />
<meta property="og:image" content="${OG_IMAGE}" />
<meta property="og:image:secure_url" content="${OG_IMAGE}" />
<meta property="og:image:type" content="image/png" />
<meta property="og:image:width" content="1200" />
<meta property="og:image:height" content="630" />
<meta property="og:image:alt" content="콜태그 통화 후 고객관리와 페이지로 문의 연동" />
<meta name="twitter:card" content="summary_large_image" />
<meta name="twitter:title" content="${SEO_TITLE}" />
<meta name="twitter:description" content="${SEO_DESCRIPTION}" />
<meta name="twitter:image" content="${OG_IMAGE}" />
<script type="application/ld+json" id="ct-seo-schema">${JSON.stringify(SEO_SCHEMA).replace(/</g,'\\u003c')}</script>
<link rel="stylesheet" href="/assets/calltag-enhance.css?v=20260801-37" />
<link rel="stylesheet" href="/assets/calltag-pagero-light.css?v=20260809-light1" />
<link rel="stylesheet" href="/assets/calltag-final.css?v=20260809-final1" />
<link rel="stylesheet" href="/assets/calltag-mobile.css?v=20260809-mobile1" />
<link rel="stylesheet" href="/assets/calltag-strength.css?v=20260809-strength1" />
<style id="ct-initial-layout-guard">html:not(.ct-layout-ready) body>main#top{visibility:hidden!important}</style>
<script>setTimeout(()=>document.documentElement.classList.add('ct-layout-ready'),3000)</script>`;

const stripSeo=body=>body
  .replace(/<meta\s+(?:name|property)=["'](?:description|robots|googlebot|bingbot|author|application-name|og:[^"']+|twitter:[^"']+)["'][^>]*>\s*/gi,'')
  .replace(/<link\s+rel=["']canonical["'][^>]*>\s*/gi,'')
  .replace(/<link\s+rel=["']alternate["'][^>]*hreflang=[^>]*>\s*/gi,'')
  .replace(/<script\s+type=["']application\/ld\+json["'][^>]*id=["']ct-seo-schema["'][^>]*>[\s\S]*?<\/script>\s*/gi,'');

const normalizeCopy=body=>body
  .replace('무료체험 뒤 요금은 얼마인가요?','요금제는 어떻게 구성되나요?')
  .replace('처음 7일은 무료로 체험할 수 있으며, 무료체험 종료 후 이용 요금은 월 1,900원입니다.','전화관리 월 1,900원, 문자자동화 월 990원, 페이지로 월 3,500원, 통합권 월 6,000원이며 모든 요금은 부가세 별도입니다.')
  .replaceAll('7일 무료체험','3일 무료체험')
  .replaceAll('7일 무료 체험','3일 무료 체험')
  .replaceAll('7일 동안 써보고','3일 동안 써보고')
  .replaceAll('7일 동안 무료','3일 동안 무료')
  .replaceAll('7일 무료로','3일 무료로');

export default {
  async fetch(request,env){
    const url=new URL(request.url);
    const legacyLegal=url.pathname.match(/^\/(terms|privacy|refund|support)(?:\.html|\/)$/);
    if(url.pathname==='/index.html')return Response.redirect(new URL('/',url).toString(),301);
    if(legacyLegal)return Response.redirect(new URL(`/${legacyLegal[1]}`,url).toString(),301);

    const response=await env.ASSETS.fetch(request);
    const type=response.headers.get('content-type')||'';
    if(!type.includes('text/html')){
      if(url.pathname==='/robots.txt'||url.pathname==='/sitemap.xml'){
        const headers=new Headers(response.headers);
        headers.set('content-type',url.pathname==='/robots.txt'?'text/plain; charset=UTF-8':'application/xml; charset=UTF-8');
        headers.set('cache-control','public, max-age=3600, must-revalidate');
        return new Response(response.body,{status:response.status,statusText:response.statusText,headers});
      }
      return response;
    }

    const headers=new Headers(response.headers);
    ['content-encoding','content-length','etag','last-modified','content-md5','digest'].forEach(name=>headers.delete(name));
    headers.set('content-type','text/html; charset=UTF-8');
    headers.set('cache-control','no-cache, no-store, must-revalidate');
    headers.set('x-calltag-worker',WORKER_VERSION);

    const isLegal=/^\/(terms|privacy|refund|support)(?:\.html)?(?:\/|$)/.test(url.pathname);
    if(isLegal)return new Response(await response.text(),{status:response.status,statusText:response.statusText,headers,encodeBody:'automatic'});

    let body=await response.text();
    body=normalizeCopy(stripSeo(body))
      .replace(/<title>[\s\S]*?<\/title>/i,`<title>${SEO_TITLE}</title>`)
      .replace('</head>',`${SEO_HEAD}</head>`)
      .replace('</body>',`<script src="${RUNTIME_SRC}"></script></body>`);

    return new Response(body,{status:response.status,statusText:response.statusText,headers,encodeBody:'automatic'});
  }
};
