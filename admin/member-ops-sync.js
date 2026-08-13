(() => {
  const searchButton = document.getElementById('memberSearchButton');
  const membersTab = document.getElementById('membersTab');
  if (!searchButton) return;
  const reload = () => setTimeout(() => searchButton.click(), 700);
  membersTab?.addEventListener('click', reload);
  reload();
})();
