const username = requireUsername();

(function init() {
  if (!username) return;

  // Sidebar + top bar load alongside the page's own data (not before it).
  bootPage('skills', username);

  fetchJSON(`${API_BASE}/${encodeURIComponent(username)}/skills`)
    .then((skillSummary) => {
      document.getElementById('skills-loading').hidden = true;
      document.getElementById('skills-content').hidden = false;
      renderSkills(skillSummary);
    })
    .catch((err) => {
      document.getElementById('skills-loading').hidden = true;
      document.getElementById('skills-error').hidden = false;
      document.getElementById('skills-error').textContent = err.message || 'Could not load skill data.';
    });
})();

function renderSkills(summary) {
  document.getElementById('skills-repo-count').textContent = `${summary.totalReposAnalyzed} repos analyzed`;

  const skills = summary.skills || [];
  if (skills.length === 0) {
    document.getElementById('skills-empty').hidden = false;
    return;
  }

  const primary = skills.filter((s) => s.percentage >= 15);
  const familiar = skills.filter((s) => s.percentage < 15);

  const primaryList = document.getElementById('primary-skills-list');
  const familiarList = document.getElementById('familiar-skills-list');

  if (primary.length > 0) {
    document.getElementById('primary-skills-label').hidden = false;
    primary.forEach((s) => primaryList.appendChild(buildSkillRow(s)));
  }
  if (familiar.length > 0) {
    document.getElementById('familiar-skills-label').hidden = false;
    familiar.forEach((s) => familiarList.appendChild(buildSkillRow(s)));
  }
}

function buildSkillRow(skill) {
  const row = document.createElement('div');
  row.className = 'skill-row-lg';
  row.innerHTML = `
    <span class="skill-name-lg">${escapeHtml(skill.language)}<span class="skill-count-lg">${skill.repoCount} repo${skill.repoCount === 1 ? '' : 's'}</span></span>
    <span class="skill-bar-track-lg"><span class="skill-bar-fill-lg" style="width:${skill.percentage}%"></span></span>
    <span class="skill-pct-lg">${skill.percentage.toFixed(0)}%</span>
  `;
  return row;
}
