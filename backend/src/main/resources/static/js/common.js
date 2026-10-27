/* ==========================================================================
   SHARED CONFIG + UTILITIES - used by every inner page
   ========================================================================== */
const API_BASE = (function() {
  if (typeof window === 'undefined' || !window.location || !window.location.origin || window.location.origin === 'null') {
    return 'http://localhost:8080/api/github';
  }
  const isLocalDevServer = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') &&
    window.location.port && window.location.port !== '8080';
  if (isLocalDevServer) {
    return 'http://localhost:8080/api/github';
  }
  return `${window.location.origin}/api/github`;
})();

function getUsernameFromUrl() {
  const params = new URLSearchParams(window.location.search);
  return params.get('u');
}

function requireUsername() {
  const username = getUsernameFromUrl();
  if (!username) {
    window.location.href = 'index.html';
    return null;
  }
  return username;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str == null ? '' : String(str);
  return div.innerHTML;
}

async function fetchJSON(url) {
  const res = await fetch(url);
  let data;
  try {
    data = await res.json();
  } catch (e) {
    throw new Error('Backend returned a response that was not valid JSON.');
  }
  if (!res.ok) {
    throw new Error(data.message || `Request failed with status ${res.status}`);
  }
  return data;
}

/* ==========================================================================
   SIDEBAR NAV - wires up hrefs with the current username and marks the
   active page. Call this once per page, passing the current page's key.
   ========================================================================== */
function initSidebar(activePage, username) {
  document.querySelectorAll('.sidebar-link[data-page]').forEach((link) => {
    const page = link.dataset.page;
    link.href = `${page}.html?u=${encodeURIComponent(username)}`;
    link.classList.toggle('active', page === activePage);
  });

  const changeUserBtn = document.getElementById('change-user-btn');
  if (changeUserBtn) {
    changeUserBtn.addEventListener('click', () => {
      window.location.href = 'index.html';
    });
  }
}

/* ==========================================================================
   TOPBAR PROFILE - fills in avatar/name/username once profile data is known
   ========================================================================== */
function renderTopbarProfile(profile) {
  const avatar = document.getElementById('topbar-avatar');
  const name = document.getElementById('topbar-name');
  const username = document.getElementById('topbar-username');
  if (avatar) { avatar.src = profile.avatarUrl || ''; avatar.alt = profile.login || ''; }
  if (name) name.textContent = profile.name || profile.login || '-';
  if (username) username.textContent = `@${profile.login || '-'}`;
}

/* ==========================================================================
   SHARED: repo card builder (used on profile, repos pages)
   ========================================================================== */
function buildRepoCard(repo) {
  const a = document.createElement('a');
  a.className = 'repo-card';
  a.href = repo.htmlUrl || '#';
  a.target = '_blank';
  a.rel = 'noopener';
  a.innerHTML = `
    <div class="repo-card-name">${escapeHtml(repo.name || '')}</div>
    <p class="repo-card-desc">${escapeHtml(repo.description || 'No description provided.')}</p>
    <div class="repo-card-meta">
      ${repo.language ? `<span><span class="lang-dot"></span>${escapeHtml(repo.language)}</span>` : ''}
      <span>&#9733; ${repo.stars ?? 0}</span>
      <span>&#8991; ${repo.forks ?? 0}</span>
    </div>
  `;
  return a;
}

/* ==========================================================================
   SHARED: show a fatal page-level error (e.g. user not found)
   ========================================================================== */
function showPageError(message) {
  const content = document.getElementById('page-content');
  if (!content) return;
  content.innerHTML = `<div class="status-banner">${escapeHtml(message)}</div>`;
}

/* ==========================================================================
   PAGE BOOT - the key to pages feeling fast.

   Previously every page did: wait for the profile -> THEN wire the sidebar
   -> THEN start loading its own data. Those steps ran one after another,
   so a page could not even begin its real work until the profile call had
   finished.

   bootPage() instead:
     1. wires the sidebar immediately (it only needs the username),
     2. starts the profile request right away,
     3. fills the top bar whenever the profile arrives.
   The page then starts its OWN data request at the same moment, so the
   two requests run side by side instead of end to end.
   ========================================================================== */
function bootPage(activePage, username) {
  initSidebar(activePage, username);
  const profilePromise = fetchJSON(`${API_BASE}/${encodeURIComponent(username)}`);
  profilePromise
    .then(renderTopbarProfile)
    .catch((err) => showPageBanner(err.message || 'Could not load this profile.'));
  return profilePromise;
}

/** Non-destructive error banner - adds a message without wiping the page's own content. */
function showPageBanner(message) {
  const content = document.getElementById('page-content');
  if (!content) return;
  const banner = document.createElement('div');
  banner.className = 'status-banner';
  banner.textContent = message;
  content.prepend(banner);
}
