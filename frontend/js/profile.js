const username = requireUsername();

(function init() {
  if (!username) return;
  const u = encodeURIComponent(username);
  const year = new Date().getFullYear();

  wireQuickLinks(u);

  // 1) The profile card appears as soon as the profile arrives.
  bootPage('profile', username)
    .then((profile) => {
      renderProfile(profile);
      document.getElementById('profile-loading').hidden = true;
      document.getElementById('profile-content').hidden = false;
    })
    .catch(() => {
      document.getElementById('profile-loading').hidden = true;
    });

  // 2) Every tile loads independently and fills in the moment ITS data
  //    arrives - the page never waits for the slowest one (the AI career
  //    call). Requesting them all here also warms the server cache, so the
  //    other pages open instantly afterwards.
  loadTile('quick-contrib-value', `${API_BASE}/${u}/contributions?year=${year}`,
    (d) => String(d.totalContributions));

  loadTile('quick-skill-value', `${API_BASE}/${u}/skills`,
    (d) => d.topLanguage || 'None detected');

  loadTile('quick-career-value', `${API_BASE}/${u}/career-suggestions`,
    (d) => {
      const n = d.eligibleRoles ? d.eligibleRoles.length : 0;
      return `${n} role${n === 1 ? '' : 's'}`;
    });

  loadTile('quick-repos-value', `${API_BASE}/${u}/open-source-suggestions`,
    (d) => {
      const n = d.suggestions ? d.suggestions.length : 0;
      return `${n} project${n === 1 ? '' : 's'}`;
    });

  loadTile('quick-oss-value', `${API_BASE}/${u}/open-source-contributions`,
    (d) => `${d.mergedCount} merged / ${d.totalCount} total`);
})();

function loadTile(elementId, url, format) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = 'Loading...';
  el.classList.add('is-loading');
  fetchJSON(url)
    .then((data) => {
      el.textContent = format(data);
    })
    .catch(() => {
      el.textContent = 'Unavailable';
    })
    .finally(() => el.classList.remove('is-loading'));
}

function wireQuickLinks(u) {
  document.getElementById('link-contributions').href = `contributions.html?u=${u}`;
  document.getElementById('link-skills').href = `skills.html?u=${u}`;
  document.getElementById('link-career').href = `career.html?u=${u}`;
  document.getElementById('link-repos').href = `opensource.html?u=${u}`;
  document.getElementById('link-opensource').href = `opensource.html?u=${u}`;
}

function renderProfile(profile) {
  document.getElementById('hero-avatar').src = profile.avatarUrl || '';
  document.getElementById('hero-name').textContent = profile.name || profile.login || '-';
  document.getElementById('hero-username').textContent = `@${profile.login || '-'}`;
  document.getElementById('hero-bio').textContent = profile.bio || '';
  document.getElementById('stat-repos').textContent = profile.publicRepos ?? 0;
  document.getElementById('stat-followers').textContent = profile.followers ?? 0;
  document.getElementById('stat-following').textContent = profile.following ?? 0;
  document.getElementById('hero-link').href = profile.htmlUrl || `https://github.com/${profile.login}`;

  if (profile.location) {
    const el = document.getElementById('meta-location');
    el.hidden = false;
    el.textContent = profile.location;
  }
  if (profile.company) {
    const el = document.getElementById('meta-company');
    el.hidden = false;
    el.textContent = profile.company;
  }
}
