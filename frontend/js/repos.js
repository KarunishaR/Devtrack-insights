const username = requireUsername();
let allRepos = [];

(function init() {
  if (!username) return;

  bootPage('repos', username);

  fetchJSON(`${API_BASE}/${encodeURIComponent(username)}/repos`)
    .then((repos) => {
      document.getElementById('repos-loading').hidden = true;
      document.getElementById('repos-content').hidden = false;
      allRepos = repos;
      document.getElementById('repo-count-tag').textContent = `${repos.length} public`;
      renderFilterBar(allRepos);
      renderRepos(allRepos);
    })
    .catch((err) => {
      document.getElementById('repos-loading').hidden = true;
      showPageBanner(err.message || 'Could not load repositories.');
    });
})();

function renderFilterBar(repos) {
  const bar = document.getElementById('filter-bar');
  const languages = [...new Set(repos.map((r) => r.language).filter(Boolean))];
  if (languages.length === 0) return;

  const allPill = document.createElement('button');
  allPill.className = 'filter-pill active';
  allPill.textContent = 'All';
  allPill.addEventListener('click', () => setFilter(null, allPill));
  bar.appendChild(allPill);

  languages.forEach((lang) => {
    const pill = document.createElement('button');
    pill.className = 'filter-pill';
    pill.textContent = lang;
    pill.addEventListener('click', () => setFilter(lang, pill));
    bar.appendChild(pill);
  });
}

function setFilter(language, activeBtn) {
  document.querySelectorAll('.filter-pill').forEach((el) => el.classList.remove('active'));
  activeBtn.classList.add('active');
  renderRepos(language ? allRepos.filter((r) => r.language === language) : allRepos);
}

function renderRepos(repos) {
  const grid = document.getElementById('recent-grid');
  grid.innerHTML = '';
  const emptyMsg = document.getElementById('recent-empty');

  if (!repos || repos.length === 0) {
    emptyMsg.hidden = false;
    return;
  }
  emptyMsg.hidden = true;
  repos.slice(0, 24).forEach((repo) => grid.appendChild(buildRepoCard(repo)));
}
