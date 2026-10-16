(() => {
  const customer = document.getElementById('playActualCustomer');
  const fee = document.getElementById('playActualFee');
  const net = document.getElementById('playActualNet');
  const partner = document.getElementById('playActualPartner');
  const final = document.getElementById('playActualFinal');
  const meta = document.getElementById('playFinanceMeta');
  const select = document.getElementById('playFinanceMonth');
  const refresh = document.getElementById('refreshButton');
  if (!customer || !select) return;

  let currentMonth = '';
  let warming = false;
  let warmCount = 0;

  select.addEventListener('change', () => {
    currentMonth = String(select.value || '');
    load(currentMonth, false);
  });
  refresh?.addEventListener('click', () => setTimeout(() => load(currentMonth, false), 100));
  setTimeout(() => load('', false), 80);

  async function load(month, warmOnly) {
    try {
      const query = month ? `?month=${encodeURIComponent(month)}` : '';
      const response = await fetch(`/admin/api/play-finance-monthly${query}`, {
        method: 'GET', cache: 'no-store', credentials: 'same-origin', headers: { accept: 'application/json' },
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok || data?.ok === false) throw new Error(data?.error || '실정산 데이터를 불러오지 못했습니다.');
      populateMonths(Array.isArray(data.months) ? data.months : [], data.month || month || '');
      currentMonth = data.month || month || currentMonth;
      if (!warmOnly) render(data);
      const remaining = Number(data.backfillRemaining || 0);
      if (remaining > 0 && warmCount < 8 && !warming) {
        warming = true;
        warmCount += 1;
        setTimeout(async () => {
          warming = false;
          await load(currentMonth, true);
        }, 900);
      }
    } catch (error) {
      if (!warmOnly) unavailable(String(error?.message || '실정산 데이터 연결을 확인해주세요.'));
    }
  }

  function populateMonths(months, selected) {
    const previous = selected || select.value;
    select.replaceChildren();
    if (!months.length) {
      const option = document.createElement('option');
      option.value = '';
      option.textContent = '보고서 없음';
      select.append(option);
      select.disabled = true;
      return;
    }
    select.disabled = false;
    for (const month of months) {
      const option = document.createElement('option');
      option.value = month;
      option.textContent = month;
      option.selected = month === previous;
      select.append(option);
    }
    if (previous && months.includes(previous)) select.value = previous;
    else select.value = months[0];
  }

  function render(data) {
    if (!data?.available || !data?.report) {
      const message = data?.status === 'permission_required'
        ? 'Google Play 서비스계정에 재무 데이터 보기 권한 필요'
        : '아직 확정 수익보고서가 없습니다.';
      unavailable(message);
      return;
    }
    const report = data.report;
    customer.textContent = `${won(report.customerNetKrw)}원`;
    fee.textContent = `${won(report.googleFeeKrw)}원`;
    net.textContent = `${won(report.playNetKrw)}원`;
    partner.textContent = `${won(report.partnerConfirmedKrw)}원`;
    final.textContent = `${won(report.finalAfterPartnerKrw)}원`;
    const payout = `지급완료 ${won(report.partnerPaidKrw)}원 · 미지급 ${won(report.partnerUnpaidKrw)}원`;
    const sync = report.syncedAt ? ` · 동기화 ${dateTime(report.syncedAt)}` : '';
    const backfill = Number(data.backfillRemaining || 0) > 0 ? ` · 과거월 ${Number(data.backfillRemaining)}개 동기화 중` : '';
    meta.textContent = `${report.month || '-'} Play 수익보고서 · ${payout}${sync}${backfill}`;
  }

  function unavailable(message) {
    for (const element of [customer, fee, net, partner, final]) if (element) element.textContent = '—';
    if (meta) meta.textContent = message;
  }
  function won(value) { const n = Number(value || 0); return (Number.isFinite(n) ? Math.max(0, Math.trunc(n)) : 0).toLocaleString('ko-KR'); }
  function dateTime(value) {
    const d = new Date(String(value || ''));
    return Number.isFinite(d.getTime()) ? new Intl.DateTimeFormat('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(d) : '-';
  }
})();
