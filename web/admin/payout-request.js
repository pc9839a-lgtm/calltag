(()=>{
  'use strict';
  if(window.__CALLTAG_ADMIN_PAYOUT_REQUEST_LOADED__)return;
  window.__CALLTAG_ADMIN_PAYOUT_REQUEST_LOADED__=true;

  const baseRenderPartners=renderPartners;
  const baseRenderPartnerDetail=renderPartnerDetail;
  const baseSettlementStatusLabel=settlementStatusLabel;
  const basePillClass=pillClass;

  settlementStatusLabel=function(value){
    if(value==='requested')return'지급요청';
    return baseSettlementStatusLabel(value);
  };

  pillClass=function(value){
    if(['지급요청','처리중','검토필요'].includes(value))return'warn';
    return basePillClass(value);
  };

  renderPartners=function(data){
    baseRenderPartners(data);
    const totals=data?.totals||{};
    const count=document.getElementById('partnerRequested');
    const amount=document.getElementById('partnerRequestedAmount');
    if(count)count.textContent=`${number(totals.payoutRequestCount)}건`;
    if(amount)amount.textContent=`${money(totals.requestedAmountKrw)}원 요청`;

    const rows=Array.isArray(data?.partners)?data.partners:[];
    rows.forEach((item,index)=>{
      const month=item?.month||{};
      if(numberRaw(month.payoutRequestCount)<=0)return;
      const tr=el.partnerRows.children[index];
      const payableCell=tr?.children?.[7];
      if(!payableCell)return;
      const note=document.createElement('div');
      note.className='rate-note payout-request-row-note';
      note.textContent=`지급요청 ${money(month.requestedAmountKrw)}원 · ${dateTime(month.lastRequestedAt)}`;
      payableCell.append(note);
    });
  };

  renderPartnerDetail=function(data){
    baseRenderPartnerDetail(data);
    el.detailBody.querySelectorAll('.paybox').forEach(node=>node.remove());

    const partner=data?.partner||{};
    const requests=Array.isArray(data?.payoutRequests)?data.payoutRequests:[];
    const profile=data?.payoutProfile||{configured:false};
    const financeEnabled=data?.financeWriteEnabled===true;
    const requestSection=buildRequestSection(partner.ownerId,requests,profile,financeEnabled);
    const rateBox=el.detailBody.querySelector('.financebox');
    if(rateBox)rateBox.insertAdjacentElement('afterend',requestSection);
    else el.detailBody.prepend(requestSection);
  };

  function buildRequestSection(ownerId,requests,profile,financeEnabled){
    const wrapper=document.createElement('section');
    wrapper.className='section payout-request-section';
    wrapper.append(heading('지급요청'));
    if(!requests.length){
      const empty=document.createElement('div');
      empty.className='payout-request-empty';
      empty.textContent='사용자가 지급요청하면 이곳에 요청금액과 계좌정보가 표시됩니다.';
      wrapper.append(empty);
      return wrapper;
    }

    for(const request of requests){
      const card=document.createElement('div');
      card.className=`payout-request-card status-${String(request.status||'review')}`;
      card.append(
        detailRow('상태',requestStatusLabel(request.status)),
        detailRow('서비스',serviceLabel(request.service)),
        detailRow('요청 금액',`${money(request.amountKrw)}원`),
        detailRow('요청 일시',dateTime(request.requestedAt)),
        detailRow('요청 번호',shortId(request.requestId)),
      );
      if(request.processedAt)card.append(detailRow('처리 일시',dateTime(request.processedAt)));

      if(request.status==='requested'){
        if(profile?.configured===true&&financeEnabled){
          card.append(
            detailRow('은행',profile.bankName||'-'),
            detailRow('예금주',profile.accountHolder||'-'),
            detailRow('계좌번호',profile.accountNumberMasked||'-'),
            detailRow('정산 유형',payoutTypeLabel(profile.payoutType)),
          );
        }
        card.append(requestPayControl(ownerId,request,profile,financeEnabled));
      }
      wrapper.append(card);
    }
    return wrapper;
  }

  function requestPayControl(ownerId,request,profile,financeEnabled){
    const box=document.createElement('div');
    box.className='paybox payout-request-paybox';
    const note=document.createElement('p');
    if(!financeEnabled)note.textContent='정산 변경 권한이 있는 관리자만 지급완료 처리할 수 있습니다.';
    else if(!profile?.configured)note.textContent='정산 계좌정보를 확인할 수 없어 지급할 수 없습니다.';
    else note.textContent=`${profile.bankName||'은행'} · ${profile.accountNumberMasked||'계좌 확인 필요'} · ${profile.accountHolder||'예금주 확인 필요'}로 실제 송금한 뒤 지급완료 처리하세요.`;
    const button=document.createElement('button');
    button.type='button';
    button.className='paybtn';
    button.disabled=!financeEnabled||profile?.configured!==true;
    button.textContent=`지급완료 · ${money(request.amountKrw)}원`;
    button.addEventListener('click',()=>payRequestedSettlement(ownerId,request,profile));
    box.append(note,button);
    return box;
  }

  async function payRequestedSettlement(ownerId,request,profile){
    const amount=numberRaw(request?.amountKrw);
    if(!ownerId||!request?.requestId||!amount)return;
    const bank=profile?.bankName||'은행 확인 필요';
    const account=profile?.accountNumberMasked||'계좌 확인 필요';
    const holder=profile?.accountHolder||'예금주 확인 필요';
    const confirmed=window.confirm(
      `${serviceLabel(request.service)} 지급요청 ${money(amount)}원을 지급완료 처리할까요?\n\n${bank} ${account}\n예금주 ${holder}\n\n실제 계좌 송금을 완료한 뒤에만 확인을 누르세요.`
    );
    if(!confirmed)return;
    setStatus('지급요청과 정산금 재검증 중…',true);
    const result=await post('/admin/api/settlement-pay',{
      ownerId,
      requestId:request.requestId,
      month:request.month||state.month,
      expectedAmountKrw:amount,
    });
    if(!result.ok){
      const current=result.data?.currentAmountKrw?` 현재 정산 가능 ${money(result.data.currentAmountKrw)}원`:'';
      const requested=result.data?.requestedAmountKrw?` 요청금액 ${money(result.data.requestedAmountKrw)}원`:'';
      setStatus(`${result.error||'지급완료 처리에 실패했습니다.'}${requested}${current}`,false);
      return;
    }
    setStatus(`${serviceLabel(request.service)} 지급요청 ${money(result.data?.settlement?.paidAmountKrw||amount)}원 지급완료 처리했습니다.`,true);
    await loadPartners();
    await openPartner(ownerId);
  }

  function serviceLabel(value){
    const service=String(value||'ALL').toUpperCase();
    if(service==='CALLTAG')return'콜태그';
    if(service==='PAGERO')return'페이지로';
    return'전체';
  }

  function requestStatusLabel(value){
    const status=String(value||'').toLowerCase();
    if(status==='requested')return'지급대기';
    if(status==='processing')return'처리중';
    if(status==='paid')return'지급완료';
    if(status==='cancelled')return'취소';
    return'검토필요';
  }

  function payoutTypeLabel(value){
    const type=String(value||'').toUpperCase();
    if(type==='SOLE_PROPRIETOR')return'개인사업자';
    if(type==='CORPORATION')return'법인사업자';
    return'개인';
  }
})();
