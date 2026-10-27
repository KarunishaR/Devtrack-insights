/* ==========================================================================
   LANDING PAGE LOGIC
   ========================================================================== */
const searchForm = document.getElementById('search-form');
const usernameInput = document.getElementById('username-input');
const searchError = document.getElementById('search-error');
const backendStatus = document.getElementById('backend-status');

(async function checkBackendOnLoad() {
  try {
    const res = await fetch(`${API_BASE}/ping`);
    if (!res.ok) throw new Error('unhealthy');
    backendStatus.hidden = true;
  } catch (err) {
    backendStatus.hidden = false;
    backendStatus.innerHTML =
      `Cannot reach the backend API at <span class="mono">${API_BASE}</span>. ` +
      'Make sure the Spring Boot app is running.';
  }
})();

searchForm.addEventListener('submit', (e) => {
  e.preventDefault();
  goToProfile(usernameInput.value.trim());
});

document.querySelectorAll('.example-chip').forEach((chip) => {
  chip.addEventListener('click', () => goToProfile(chip.dataset.username));
});

async function goToProfile(username) {
  searchError.hidden = true;
  if (!username) {
    searchError.textContent = 'Type a GitHub username first.';
    searchError.hidden = false;
    return;
  }

  // Kick off all the heavy server-side work right now, in parallel with the
  // username check below - so by the time the next page opens, its data is
  // already being prepared instead of starting from zero.
  fetch(`${API_BASE}/${encodeURIComponent(username)}/warmup`, { keepalive: true }).catch(() => {});

  const original = document.querySelector('.search-btn').textContent;
  document.querySelector('.search-btn').textContent = 'Checking...';

  try {
    const res = await fetch(`${API_BASE}/${encodeURIComponent(username)}`);
    if (res.status === 404) {
      throw new Error(`"${username}" isn't a real GitHub username.`);
    }
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.message || 'Something went wrong. Try again.');
    }
    window.location.href = `profile.html?u=${encodeURIComponent(username)}`;
  } catch (err) {
    document.querySelector('.search-btn').textContent = original;
    searchError.textContent = err.message;
    searchError.hidden = false;
  }
}
