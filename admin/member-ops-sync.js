(() => {
  const searchButton = document.getElementById('memberSearchButton');
  const membersTab = document.getElementById('membersTab');
  const refreshButton = document.getElementById('refreshButton');
  if (!searchButton) return;
  const reload = () => setTimeout(() => searchButton.click(), 700);
  membersTab?.addEventListener('click', reload);
  refreshButton?.addEventListener('click', reload);
  reload();
})();
