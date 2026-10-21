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
      parts.subscriptionsNote.textContent = '';
    } else {
      status(parts.subscriptions, '확인 전', 'neutral');
      parts.subscriptionsNote.textContent = '';
    }

    if (state.audit?.available === true) {
      const summary = state.audit.summary || {};
      const ledger = safeCount(summary.chargeRecorded);
      const review = safeCount(summary.awaitingProviderProof);
      status(parts.ledger, countText(ledger), review > 0 || state.audit.truncated ? 'warn' : 'ok');
      const statusParts = [
        review === null ? '구독·원장 대조 불가' : '결제 미확인 ' + review.toLocaleString('ko-KR') + '명',
      ];
      if (state.audit.truncated) statusParts.push('일부 기록만 검사됨');
      parts.ledgerNote.textContent = statusParts.join(' · ');
    } else {
      status(parts.ledger, '확인 전', 'neutral');
      parts.ledgerNote.textContent = '조회 대기';
    }

    const finance = state.finance;
    if (!finance) {
      status(parts.finance, '확인 전', 'neutral');
      parts.financeNote.textContent = '';
    } else if (finance.status === 'ready') {
      status(parts.finance, '연결됨', 'ok');
      parts.financeNote.textContent = finance.month || '';
    } else if (finance.status === 'permission_required') {
      status(parts.finance, '권한 확인 필요', 'warn');
      parts.financeNote.textContent = '';
    } else if (finance.status === 'no_report') {
      status(parts.finance, '보고서 없음', 'neutral');
      parts.financeNote.textContent = '';
    } else {
      status(parts.finance, '확인 실패', 'warn');
      parts.financeNote.textContent = '';
    }

    const warnings = [];
    const a = state.audit?.summary;
    if (state.audit?.available && a) {
      const unseen = safeCount(a.notInMemberList);
      const unverified = safeCount(a.awaitingProviderProof);
      if (unseen > 0) warnings.push({
        label: '회원 목록 누락 ' + countText(unseen),
        note: '',
        action: 'audit',
      });
      if (unverified > 0) warnings.push({
        label: '결제 확인 필요 ' + countText(unverified),
        note: '',
        action: 'audit',
      });
      if (state.audit.truncated) warnings.push({
        label: '일부 결제 기록 미확인',
        note: '',
        action: 'audit',
      });
    }
    if (finance?.status === 'permission_required') warnings.push({
      label: 'Google Play 권한 확인 필요',
      note: '',
      action: 'finance',
    });
    if (finance?.status === 'error') warnings.push({
      label: 'Google Play 정산 조회 실패',
      note: '',
      action: 'finance',
    });

    panel.classList.toggle('ops-has-alerts', warnings.length > 0);
    parts.attention.replaceChildren();
    if (!state.overview && !state.audit && !state.finance) {
      const item = document.createElement('li');
      item.textContent = '운영 지표를 불러오는 중입니다.';
      parts.attention.append(item);
    } else if (!warnings.length) {
      const item = document.createElement('li');
      item.textContent = '추가 확인 항목 없음';
      parts.attention.append(item);
    } else {
      for (const issue of warnings.slice(0, 5)) {
        const li = document.createElement('li');
        li.className = 'ops-issue';
        const details = document.createElement('div');
        const strong = document.createElement('strong');
        strong.textContent = issue.label;
        details.append(strong);
        if (issue.note) {
          const small = document.createElement('small');
          small.textContent = issue.note;
          details.append(small);
        }
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
      const details = $('payerAuditDetails');
      if (details) details.open = true;
      $('payerAuditPanel')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      parts.payerAuditButton?.click();
      return;
    }
    if (action === 'finance') {
      const details = $('financeDetails');
      if (details) details.open = true;
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
