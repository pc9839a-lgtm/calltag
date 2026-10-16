(() => {
  const button = document.getElementById('playRenewalReconcile');
  const status = document.getElementById('playRenewalStatus');
  if (!button || !status) return;
  let working = false;
  button.addEventListener('click', async () => {
    if (working) return;
    working = true;
    button.disabled = true;
    button.textContent = 'Google Play 확인 중';
    status.textContent = '기존 구독 주문을 Google Play에서 재검증하고 있습니다.';
    try {
      const response = await fetch('/admin/api/subscription-reconcile', {
        method: 'POST',
        credentials: 'same-origin',
        cache: 'no-store',
        headers: { accept: 'application/json', 'content-type': 'application/json' },
        body: '{}',
      });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok || payload.ok !== true) {
        throw new Error(payload.error || '구독 갱신 확인 요청이 실패했습니다.');
      }
      if (payload.examined === 0) {
        status.textContent = '갱신 확인이 필요한 저장된 구독이 없습니다.';
      } else if (payload.results.some(x => x.providerHttpStatus === 401 || x.providerHttpStatus === 403)) {
        status.textContent = 'Google Play 주문 조회 권한이 부족합니다. 기존 구독 상태를 변경하지 않았습니다.';
      } else {
        status.textContent = 'Play 확인 ' + payload.providerVerified + '건 · 현재 이용가능 '
          + payload.activeNow + '건 · 추가 확인 ' + payload.needsReview + '건';
      }
      document.getElementById('memberSearchButton')?.click();
      document.getElementById('payerAuditRefresh')?.click();
    } catch (error) {
      status.textContent = String(error?.message || 'Google Play 검증에 실패했습니다.');
    } finally {
      working = false;
      button.disabled = false;
      button.textContent = 'Google Play 구독 갱신 확인';
    }
  });
})();
