const playFinanceEl = {
  customer: document.getElementById('playActualCustomer'),
  fee: document.getElementById('playActualFee'),
  net: document.getElementById('playActualNet'),
  partner: document.getElementById('playActualPartner'),
  final: document.getElementById('playActualFinal'),
  meta: document.getElementById('playFinanceMeta'),
  refresh: document.getElementById('refreshButton'),
};

loadPlayFinance();
playFinanceEl.refresh?.addEventListener('click', () => setTimeout(loadPlayFinance, 50));

async function loadPlayFinance() {
  if (!playFinanceEl.customer) return;
  try {
    const response = await fetch('/admin/api/play-finance', {
      method: 'GET',
      cache: 'no-store',
      credentials: 'same-origin',
      headers: { accept: 'application/json' },
    });
    let data = {};
    try { data = await response.json(); } catch {}
    if (!response.ok || data?.ok === false) {
      renderUnavailable('실정산 데이터를 불러오지 못했습니다.');
      return;
    }
    if (!data?.available || !data?.report) {
      const message = data?.status === 'permission_required'
        ? 'Google Play 서비스계정에 재무 데이터 보기 권한 필요'
        : data?.code === 'PLAY_REPORT_COLUMNS_CHANGED'
          ? 'Google Play 수익보고서 형식 확인 필요'
          : '아직 확정 수익보고서가 없습니다.';
      renderUnavailable(message);
      return;
    }

    const report = data.report;
    playFinanceEl.customer.textContent = `${won(report.customerNetKrw)}원`;
    playFinanceEl.fee.textContent = `${won(report.googleFeeKrw)}원`;
    playFinanceEl.net.textContent = `${won(report.playNetKrw)}원`;
    playFinanceEl.partner.textContent = `${won(report.partnerConfirmedKrw)}원`;
    playFinanceEl.final.textContent = `${won(report.finalAfterPartnerKrw)}원`;
    const payout = `지급완료 ${won(report.partnerPaidKrw)}원 · 미지급 ${won(report.partnerUnpaidKrw)}원`;
    const sync = report.syncedAt ? ` · 동기화 ${dateTime(report.syncedAt)}` : '';
    playFinanceEl.meta.textContent = `${report.month || '-'} Play 수익보고서 · ${payout}${sync} · 은행 최종 입금 조정은 Payments Center 기준`;
  } catch {
    renderUnavailable('실정산 데이터 연결을 확인해주세요.');
  }
}

function renderUnavailable(message) {
  for (const element of [playFinanceEl.customer, playFinanceEl.fee, playFinanceEl.net, playFinanceEl.partner, playFinanceEl.final]) {
    if (element) element.textContent = '—';
  }
  if (playFinanceEl.meta) playFinanceEl.meta.textContent = message;
}

function won(value) {
  const number = Number(value || 0);
  return (Number.isFinite(number) ? Math.max(0, Math.trunc(number)) : 0).toLocaleString('ko-KR');
}

function dateTime(value) {
  const date = new Date(String(value || ''));
  return Number.isFinite(date.getTime())
    ? new Intl.DateTimeFormat('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
    : '-';
}
