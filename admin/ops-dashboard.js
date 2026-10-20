/* CallTag production operations summary.
   Presentation only: receives aggregate events from existing authenticated APIs.
   Never computes paid buyers from subscription counts or revenue estimates. */
(() => {
  'use strict';
  const $ = (id) => document.getElementById(id);
  const panel = $('opsHealthPanel');
  if (!panel) return;

  const parts = {
    subscriptions: $('opsVerifiedSubscribers'),
    subscriptionsNote: $('opsVerifiedSubscribersNote'),
    ledger: $('opsLedgerAccounts'),
    ledgerNote: $('opsLedgerAccountsNote'),
    finance: $('opsFinanceStatus'),
    financeNote: $('opsFinanceStatusNote'),
    attention: $('opsAttention'),
    synced: $('opsCheckedAt'),
    memberSearch: $('memberSearchInput'),
    payerAuditButton: $('payerAuditRefresh'),
  };
  const state = { overview: null, audit: null, finance: null, lastChecked: null };

  const safeCount = (value) => {
    const n = Number(value);
    return Number.isSafeInteger(n) && n >= 0 ? n : null;
  };
  const countText = (value, suffix = '명') => {
    const count = safeCount(value);
    return count === null ? '확인 전' : count.toLocaleString('ko-KR') + suffix;
  };
  const status = (node, value, condition) => {
    if (!node) return;
    node.textContent = value;
    node.dataset.state = condition || 'neutral';
  };
  const checked = () => {
    state.lastChecked = new Date();
    if (parts.synced) {
      parts.synced.textContent = '최근 확인 ' + state.lastChecked.toLocaleTimeString('ko-KR', {
        hour: '2-digit', minute: '2-digit',
      });
    }
  };
  function render() {
    const metrics = state.overview?.metrics;
    if (metrics) {
      status(parts.subscriptions, countText(metrics.activePaid), 'ok');
      parts.subscriptionsNote.textContent = '서버에서 인식한 유료 이용 회원 · 상품 건수와 다름';
    } else {
      status(parts.subscriptions, '확인 전', 'neutral');
      parts.subscriptionsNote.textContent = '회원 현황을 확인할 수 없습니다.';
    }

    if (state.audit?.available === true) {
      const summary = state.audit.summary || {};
      const ledger = safeCount(summary.chargeRecorded);
      const review = safeCount(summary.awaitingProviderProof);
      status(parts.ledger, countText(ledger), review > 0 || state.audit.truncated ? 'warn' : 'ok');
      const statusParts = [
        review === null ? '구독·원장 대조 불가' : '원장 미확인 구독 계정 ' + review.toLocaleString('ko-KR') + '명',
      ];
      if (state.audit.truncated) statusParts.push('일부 기록만 검사됨');
      parts.ledgerNote.textContent = statusParts.join(' · ');
    } else {
      status(parts.ledger, '확인 전', 'neutral');
      parts.ledgerNote.textContent = '결제 기록 조회를 기다리거나 실패했습니다.';
    }

    const finance = state.finance;
    if (!finance) {
      status(parts.finance, '확인 전', 'neutral');
      parts.financeNote.textContent = 'Google Play 수익보고서 연결 확인 중';
    } else if (finance.status === 'ready') {
      status(parts.finance, '연결됨', 'ok');
      parts.financeNote.textContent = (finance.month || '선택 월') + ' · Google Play 수익보고서 기준';
    } else if (finance.status === 'permission_required') {
      status(parts.finance, '권한 확인 필요', 'warn');
      parts.financeNote.textContent = 'Google Play 수익보고서 접근 권한을 확인하세요.';
    } else if (finance.status === 'no_report') {
      status(parts.finance, '보고서 없음', 'neutral');
      parts.financeNote.textContent = '실매출 0원이 아닌, 확정 자료가 없는 상태';
    } else {
      status(parts.finance, '확인 실패', 'warn');
      parts.financeNote.textContent = '연결 실패 · 실제 정산액을 판단할 수 없습니다.';
    }

    const warnings = [];
    const a = state.audit?.summary;
    if (state.audit?.available && a) {
      const unseen = safeCount(a.notInMemberList);
      const unverified = safeCount(a.awaitingProviderProof);
      if (unseen > 0) warnings.push({
        label: '회원 목록에 연결되지 않은 구독·결제 계정 ' + countText(unseen),
        note: '서버 기록 기준 · 실제 신규 결제자라고 단정하지 않음',
        action: 'audit',
      });
      if (unverified > 0) warnings.push({
        label: '결제 원장 추가확인 필요 ' + countText(unverified),
        note: '구독 상태가 검증됐어도 주문 매출이 자동으로 기록되는 것은 아님',
        action: 'audit',
      });
      if (state.audit.truncated) warnings.push({
        label: '전체 결제 점검 결과 일부 누락 가능',
        note: '조회 상한에 도달했습니다. 이 결과로 전체 구매자를 확정하면 안 됩니다.',
        action: 'audit',
      });
    }
    if (finance?.status === 'permission_required') warnings.push({
      label: 'Google Play 수익보고서 접근 권한 필요',
      note: '실제 정산액과 추가 유료 구매자 확인을 위한 보고서 연결 필요',
      action: 'finance',
    });
    if (finance?.status === 'error') warnings.push({
      label: 'Google Play 실정산 확인 실패',
      note: '아래 정산 영역에서 오류 상태를 확인하세요.',
      action: 'finance',
    });

    parts.attention.replaceChildren();
    if (!state.overview && !state.audit && !state.finance) {
      const item = document.createElement('li');
      item.textContent = '운영 지표를 불러오는 중입니다.';
      parts.attention.append(item);
    } else if (!warnings.length) {
      const item = document.createElement('li');
      item.textContent = state.audit?.available && finance?.status === 'ready'
        ? '현재 연결된 서버 데이터에서 별도 점검 항목이 표시되지 않았습니다. 전체 Google Play 판매 내역의 완전성을 보증하지 않습니다.'
        : '조회가 완료된 범위에서 추가 경고가 없습니다. 미조회 자료는 정상으로 판정하지 않습니다.';
      parts.attention.append(item);
    } else {
      for (const issue of warnings.slice(0, 5)) {
        const li = document.createElement('li');
        li.className = 'ops-issue';
        const details = document.createElement('div');
        const strong = document.createElement('strong');
        strong.textContent = issue.label;
        const small = document.createElement('small');
        small.textContent = issue.note;
        details.append(strong, small);
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = '확인하기';
        button.addEventListener('click', () => navigate(issue.action));
        li.append(details, button);
        parts.attention.append(li);
      }
    }
  }

  function navigate(action) {
    if (action === 'audit') {
      $('payerAuditPanel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      parts.payerAuditButton?.click();
      return;
    }
    if (action === 'finance') {
      $('playFinancePanel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    $('membersPanel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    parts.memberSearch?.focus({ preventScroll: true });
  }
  panel.querySelectorAll('[data-ops-nav]').forEach((button) => {
    button.addEventListener('click', () => navigate(button.dataset.opsNav));
  });

  document.addEventListener('calltag:ops-overview', (event) => {
    state.overview = event.detail?.ok ? event.detail : null;
    checked(); render();
  });
  document.addEventListener('calltag:ops-payer-audit', (event) => {
    state.audit = event.detail?.available ? event.detail : null;
    checked(); render();
  });
  document.addEventListener('calltag:ops-finance', (event) => {
    state.finance = event.detail || { status: 'error' };
    checked(); render();
  });
  render();
})();
