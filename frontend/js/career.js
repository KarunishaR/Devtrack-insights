const username = requireUsername();

(function init() {
  if (!username) return;

  bootPage('career', username);

  fetchJSON(`${API_BASE}/${encodeURIComponent(username)}/career-suggestions`)
    .then((career) => {
      document.getElementById('career-loading').hidden = true;
      document.getElementById('career-content').hidden = false;
      renderCareer(career);
    })
    .catch((err) => {
      document.getElementById('career-loading').hidden = true;
      const infoBanner = document.getElementById('career-info');
      infoBanner.hidden = false;
      infoBanner.textContent = err.message || 'Career suggestions are not available right now.';
    });
})();

function renderCareer(career) {
  const roleCards = document.getElementById('role-cards');
  const improveList = document.getElementById('improve-list');

  (career.eligibleRoles || []).forEach((role) => {
    const card = document.createElement('div');
    card.className = 'role-card';
    card.innerHTML = `
      <div class="role-card-title">${escapeHtml(role.title)}</div>
      <div class="role-card-reason">${escapeHtml(role.reason)}</div>
    `;
    roleCards.appendChild(card);
  });

  (career.improvementSuggestions || []).forEach((tip, i) => {
    const li = document.createElement('li');
    li.innerHTML = `<span class="improve-num">${i + 1}</span><span>${escapeHtml(tip)}</span>`;
    improveList.appendChild(li);
  });
}
