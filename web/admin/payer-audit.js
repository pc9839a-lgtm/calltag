(() => {
  const button = document.getElementById('payerAuditRefresh');
  const panel = document.getElementById('payerAuditPanel');
  const summary = document.getElementById('payerAuditSummary');
  const status = document.getElementById('payerAuditStatus');
  const wrap = document.getElementById('payerAuditTableWrap');
  const body = document.getElementById('payerAuditRows');
  if (!button || !panel || !summary || !status || !wrap || !body) return;
  let busy = false;
  const span = (value, className='') => {
    const el = document.createElement('span');el.textContent=String(value||'-');if(className)el.className=className;return el;
  };
  const cell = (...children) => {const el=document.createElement('td');el.append(...children);return el;};
  const statusBadge = (value, type) => span(value, 'payer-audit-badge '+type);
  const total = (value) => Number.isFinite(Number(value)) ? String(Math.max(0, Math.trunc(Number(value)))) : '-';
  const summaryMetric = (name, value) => {
    const box=document.createElement('div'); box.className='payer-audit-metric';
    const label=document.createElement('span');label.textContent=name;
    const count=document.createElement('strong');count.textContent=total(value);
    box.append(label,count);return box;
  };
  function describe(row) {
    if (row.recordedRefund) return ['환불 기록 포함','warning'];
    if (row.recordedCharge) return ['확인된 결제 기록','success'];
    if (row.hasVerifiedSubscription) return ['구독 검증 · 결제 추가확인','review'];
    if (row.legacyPaymentMarker) return ['이전 결제 표기 · 미검증','warning'];
    return ['확인 필요','muted'];
  }
  function subscription(row) {
    if (row.autoRenewing && row.hasCurrentSubscription) return '자동갱신 구독 활성';
    if (row.hasCurrentSubscription) return '유효한 구독 기간';
    if (row.hasVerifiedSubscription) return '과거 구독 또는 만료';
    return '구독 정보 없음';
  }
  async function verify(row, badge, btn) {
    btn.disabled=true;btn.textContent='확인 중';
    try {
      const response=await fetch('/admin/api/payment-proof?ownerId='+encodeURIComponent(row.ownerId),{credentials:'same-origin',cache:'no-store'});
      const data=await response.json().catch(()=>({}));
      if (!response.ok || data.ok===false) throw new Error('조회 실패');
      if (data.confirmed===true && data.result==='play_order_paid') {
        badge.textContent='Google Play 결제 확인';badge.className='payer-audit-badge success';
      } else if (data.result==='play_reference_missing') {
        badge.textContent='주문 식별정보 누락';badge.className='payer-audit-badge warning';
      } else if ([401,403].includes(Number(data.providerHttpStatus))) {
        badge.textContent='Play 조회 권한 부족';badge.className='payer-audit-badge warning';
      } else {
        badge.textContent='Play 결제 확인 불가';badge.className='payer-audit-badge review';
      }
    } catch {badge.textContent='서버 연결 실패';badge.className='payer-audit-badge warning';}
    finally {btn.disabled=false;btn.textContent='재확인';}
  }
  function show(data) {
    summary.replaceChildren(
      summaryMetric('전체 결제 관련 계정', data.summary?.candidates),
      summaryMetric('결제 원장 확인', data.summary?.chargeRecorded),
      summaryMetric('구독 검증 기록', data.summary?.verifiedSubscription),
      summaryMetric('회원목록 누락 의심', data.summary?.notInMemberList),
      summaryMetric('결제 추가확인', data.summary?.awaitingProviderProof),
      summaryMetric('이전 결제 표기만', data.summary?.legacyOnly)
    );
    body.replaceChildren();
    const rows=Array.isArray(data.candidates)?data.candidates:[];
    for (const row of rows) {
      const tr=document.createElement('tr');
      if (row.suspectedOmission) tr.className='payer-audit-missing';
      const [title,level]=describe(row);
      const proof=statusBadge(title,level);
      const last=cell();
      if (row.hasVerifiedSubscription && row.hasPurchaseReference) {
        const btn=document.createElement('button');btn.type='button';btn.className='row-action';
        btn.textContent='Play 실결제 확인';btn.addEventListener('click',()=>verify(row,proof,btn));
        last.append(btn);
      } else last.append(span(row.hasVerifiedSubscription?'주문 조회 불가':'-'));
      tr.append(
        cell(span(row.ownerId)),cell(span(row.email||'-')),
        cell(statusBadge(row.memberListed?'목록 포함':'목록 누락',row.memberListed?'muted':'danger')),
        cell(proof),cell(span(subscription(row))),last,
      );
      body.append(tr);
    }
    wrap.hidden=rows.length===0;
    document.dispatchEvent(new CustomEvent('calltag:ops-payer-audit', {
      detail: { available: true, truncated: data.truncated === true,
        summary: {
          chargeRecorded: data.summary?.chargeRecorded,
          verifiedSubscription: data.summary?.verifiedSubscription,
          notInMemberList: data.summary?.notInMemberList,
          awaitingProviderProof: data.summary?.awaitingProviderProof,
        }, sources: { subscriptions: data.sources?.subscriptions, payments: data.sources?.payments },
      },
    }));
    if (data.truncated) status.textContent='일부 기록 미확인 (500건 한도)';
    else status.textContent=rows.length ? '결제 기록 확인 완료 · '+new Date().toLocaleString('ko-KR') : '결제 기록 없음';
  }
  async function run() {
    if (busy) return;busy=true;button.disabled=true;button.textContent='검사 중';
    status.textContent='결제 기록 확인 중';
    try {
      const response=await fetch('/admin/api/payer-audit',{credentials:'same-origin',cache:'no-store',headers:{accept:'application/json'}});
      const data=await response.json().catch(()=>({}));
      if (!response.ok||data.ok===false||!data.available) throw new Error(data.error||'구독 원장에 접근할 수 없습니다.');
      show(data);
    } catch(e) {
      status.textContent='점검 실패: '+String(e.message||'요청 실패');
      document.dispatchEvent(new CustomEvent('calltag:ops-payer-audit', { detail: { available: false } }));
    }
    finally {busy=false;button.disabled=false;button.textContent='전체 결제 누락 점검';}
  }
  button.addEventListener('click',run);
  setTimeout(run,900);
})();
