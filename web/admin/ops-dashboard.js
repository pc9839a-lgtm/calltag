/* CallTag admin member overview. Display server-provided counts only. */
(() => {
  'use strict';
  const $ = (id) => document.getElementById(id);
  if (!$('opsHealthPanel')) return;
  const synced = $('opsCheckedAt');
  const cards = [
    { node: $('opsTotalMembers'), key: 'totalMembers' },
    { node: $('opsNewMembers'), key: 'newMembers7d' },
    { node: $('opsTrialMembers'), key: 'trialMembers' },
    { node: $('opsVerifiedSubscribers'), key: 'activePaid' },
  ];

  document.addEventListener('calltag:ops-overview', (event) => {
    const ready = event.detail?.ok === true;
    const metrics = ready ? event.detail.metrics : null;
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
