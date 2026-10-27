const username = requireUsername();
let allContributions = [];

(function init() {
  if (!username) return;
  const u = encodeURIComponent(username);

  bootPage('opensource', username);
  wireFilterTabs();

  // Two independent sections: each one appears the moment ITS data arrives,
  // instead of the whole page waiting for the slower of the two.
  fetchJSON(`${API_BASE}/${u}/open-source-contributions`)
    .then(renderOssSummary)
    .catch((err) => {
      document.getElementById('pr-loading').hidden = true;
      const banner = document.getElementById('oss-error');
      banner.hidden = false;
      banner.textContent = err.message || 'Could not load pull requests.';
    });

  fetchJSON(`${API_BASE}/${u}/open-source-suggestions`)
    .then(renderSuggestions)
    .catch((err) => {
      document.getElementById('oss-suggest-loading').hidden = true;
      const empty = document.getElementById('oss-suggest-empty');
      empty.hidden = false;
      empty.textContent = err.message || 'No suggestions available right now.';
    });
})();

function renderOssSummary(data) {
  document.getElementById('pr-loading').hidden = true;
  document.getElementById('oss-total-value').textContent = data.totalCount;
  document.getElementById('oss-open-value').textContent = data.openCount;
  document.getElementById('oss-merged-value').textContent = data.mergedCount;
  document.getElementById('oss-closed-value').textContent = data.closedCount;

  allContributions = data.contributions || [];
  renderPrList(allContributions);
}

function renderPrList(contributions) {
  const list = document.getElementById('pr-list');
  const emptyMsg = document.getElementById('pr-empty');
  list.innerHTML = '';

  if (!contributions || contributions.length === 0) {
    emptyMsg.hidden = false;
    return;
  }
  emptyMsg.hidden = true;

  contributions.forEach((pr) => {
    const row = document.createElement('div');
    row.className = 'pr-row';
    row.innerHTML = `
      <span class="status-badge ${pr.status}">${pr.status}</span>
      <div class="pr-main">
        <a class="pr-title" href="${pr.prUrl || '#'}" target="_blank" rel="noopener">${escapeHtml(pr.prTitle || 'Untitled pull request')}</a><br>
        <a class="pr-repo" href="${pr.repoUrl || '#'}" target="_blank" rel="noopener">${escapeHtml(pr.repoName || 'unknown repository')}</a>
        ${pr.summary ? `<p class="pr-summary">${escapeHtml(pr.summary)}</p>` : ''}
      </div>
      <span class="pr-date">${formatDate(pr.createdAt)}</span>
    `;
    list.appendChild(row);
  });
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function wireFilterTabs() {
  document.querySelectorAll('.filter-tabs button').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.filter-tabs button').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      const filter = btn.dataset.filter;
      renderPrList(filter === 'ALL' ? allContributions : allContributions.filter((c) => c.status === filter));
    });
  });
}

function renderSuggestions(data) {
  document.getElementById('oss-suggest-loading').hidden = true;
  const grid = document.getElementById('oss-suggest-grid');
  grid.innerHTML = '';
  document.getElementById('oss-suggest-based-on').textContent =
    data.basedOnSkills && data.basedOnSkills.length ? `based on: ${data.basedOnSkills.join(', ')}` : '';

  if (!data.suggestions || data.suggestions.length === 0) {
    document.getElementById('oss-suggest-empty').hidden = false;
    return;
  }

  data.suggestions.forEach((item) => {
    const card = buildRepoCard(item.repo);

    const reasonEl = document.createElement('p');
    reasonEl.className = 'suggestion-reason';
    reasonEl.textContent = item.reason;
    card.appendChild(reasonEl);

    const linkEl = document.createElement('span');
    linkEl.className = 'suggestion-link';
    linkEl.innerHTML = 'Open on GitHub &#8599;';
    card.appendChild(linkEl);

    grid.appendChild(card);
  });
}
