/* CallTag admin member overview. Display server-provided counts only. */
(() => {
  'use strict';
  const $ = (id) => document.getElementById(id);
  if (!$('opsHealthPanel')) return;
  const synced = $('opsCheckedAt');
  const nonPaid = $('opsNonPaidMembers');
  const paidShare = $('opsPaidShare');
  const cards = [
    { node: $('opsTotalMembers'), key: 'totalMembers' },
    { node: $('opsNewMembers'), key: 'newMembers7d' },
    { node: $('opsTrialMembers'), key: 'trialMembers' },
    { node: $('opsVerifiedSubscribers'), key: 'activePaid' },
  ];

  document.addEventListener('calltag:ops-overview', (event) => {
    const ready = event.detail?.ok === true;
    const metrics = ready ? event.detail.metrics : null;
    const validCount = value => Number.isSafeInteger(value) && value >= 0;
    const total = metrics?.totalMembers;
    const paid = metrics?.activePaid;
    const balanced = ready && validCount(total) && validCount(paid) && paid <= total;
    if (nonPaid) nonPaid.textContent = balanced
      ? (total - paid).toLocaleString('ko-KR') + '명' : '—';
    if (paidShare) paidShare.textContent = balanced && total > 0
      ? (paid * 100 / total).toFixed(1) + '%' : '—';
    for (const { node, key } of cards) {
      if (!node) continue;
      const value = metrics?.[key];
      const count = Number(value);
      node.textContent = ready && value !== undefined && value !== null
        && Number.isSafeInteger(count) && count >= 0
        ? count.toLocaleString('ko-KR') + '명' : '—';
    }
    if (synced) {
      synced.textContent = ready
        ? '갱신 ' + new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
        : '회원 현황 조회 실패';
    }
  });
})();
