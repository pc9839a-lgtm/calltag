(() => {
  const STATUS_CLASSES = ['status-info','status-success','status-warning','status-danger','status-neutral'];

  function statusClass(text = '') {
    const value = String(text || '').trim();
    if (!value) return 'status-neutral';
    if (/(비활성|해당없음|없음|만료)/.test(value)) return 'status-neutral';
    if (/(체험중|진행중)/.test(value)) return 'status-info';
    if (/(확인필요|대기|미지급|지급요청|취소예정|유예|grace)/i.test(value)) return 'status-warning';
    if (/(실패|오류|정지|환불|차단|취소됨)/.test(value)) return 'status-danger';
    if (/(활성|정상|지급완료|검증됨|이용중)/.test(value)) return 'status-success';
    return 'status-neutral';
  }

  function planClass(text = '') {
    const value = String(text || '').trim();
    if (!value || value === '-') return 'plan-none';
    if (/무료체험/.test(value)) return 'plan-trial';
    if (/(전화관리|문자자동화|통합권|페이지로|SSL)/.test(value)) return 'plan-paid';
    return 'plan-none';
  }

  function themePill(node) {
    if (!(node instanceof HTMLElement) || !node.classList.contains('pill')) return;
    STATUS_CLASSES.forEach((name) => node.classList.remove(name));
    node.classList.add(statusClass(node.textContent));
  }

  function themePlanCell(cell) {
    if (!(cell instanceof HTMLTableCellElement)) return;
    if (cell.dataset.planThemed === '1') return;
    const value = String(cell.textContent || '').trim();
    const chip = document.createElement('span');
    chip.className = `plan-chip ${planClass(value)}`;
    chip.textContent = value || '-';
    cell.replaceChildren(chip);
    cell.dataset.planThemed = '1';
  }

  function themeMemberRows(root = document) {
    const tbody = root.querySelector?.('#memberRows');
    if (!tbody) return;
    for (const row of tbody.querySelectorAll('tr')) {
      const cells = row.querySelectorAll('td');
      if (cells.length >= 7) themePlanCell(cells[4]);
    }
  }

  function apply(root = document) {
    root.querySelectorAll?.('.pill').forEach(themePill);
    themeMemberRows(root);
  }

  const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      for (const node of mutation.addedNodes) {
        if (!(node instanceof HTMLElement)) continue;
        if (node.matches?.('.pill')) themePill(node);
        node.querySelectorAll?.('.pill').forEach(themePill);
      }
    }
    themeMemberRows(document);
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      apply(document);
      observer.observe(document.body, { childList: true, subtree: true });
    }, { once: true });
  } else {
    apply(document);
    observer.observe(document.body, { childList: true, subtree: true });
  }
})();
