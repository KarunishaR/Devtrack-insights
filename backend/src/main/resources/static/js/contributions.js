const username = requireUsername();

const state = {
  view: 'year',            // 'year' | 'month'
  year: new Date().getFullYear(),
  month: new Date().getMonth(), // 0-11, only used in month view
  yearDataCache: {},       // year -> ContributionSummaryDTO
  activityByDate: {},      // 'yyyy-mm-dd' -> [descriptions]
};

const MONTH_NAMES = ['January','February','March','April','May','June','July','August','September','October','November','December'];
const MONTH_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

const els = {
  loading: document.getElementById('contrib-loading'),
  error: document.getElementById('contrib-error'),
  content: document.getElementById('contrib-content'),
  toggleYear: document.getElementById('toggle-year'),
  toggleMonth: document.getElementById('toggle-month'),
  rangePrev: document.getElementById('range-prev'),
  rangeNext: document.getElementById('range-next'),
  rangeLabel: document.getElementById('range-label'),
  totalTag: document.getElementById('contrib-total-tag'),
  barChart: document.getElementById('bar-chart'),
  activityList: document.getElementById('activity-list'),
  activityEmpty: document.getElementById('activity-empty'),
};

(function init() {
  if (!username) return;

  bootPage('contributions', username);

  // The chart and the activity feed are requested at the same time, and each
  // one draws as soon as it arrives (the chart no longer waits for the feed).
  ensureYearData(state.year)
    .then(() => {
      els.loading.hidden = true;
      els.content.hidden = false;
      wireControls();
      render();
    })
    .catch((err) => {
      els.loading.hidden = true;
      els.error.hidden = false;
      els.error.textContent = err.message || 'Could not load contribution data.';
    });

  loadActivityFeed()
    .then(() => render()) // re-draw so the day tooltips can include the activity
    .catch((err) => {
      // The feed is a nice-to-have, so it never blocks the chart - but say so visibly.
      console.warn('[contrib] activity feed failed to load:', err);
      els.activityEmpty.hidden = false;
      els.activityEmpty.textContent =
        `Could not load the activity feed: ${err.message || 'unknown error'}`;
    });
})();

async function loadActivityFeed() {
  const items = await fetchJSON(`${API_BASE}/${encodeURIComponent(username)}/activity`);
  const map = {};
  items.forEach((item) => {
    if (!item.date) return;
    if (!map[item.date]) map[item.date] = [];
    map[item.date].push(item);
  });
  state.activityByDate = map;
  renderActivityList(items);
}

function renderActivityList(items) {
  els.activityList.innerHTML = '';
  if (!items || items.length === 0) {
    els.activityEmpty.hidden = false;
    return;
  }
  els.activityEmpty.hidden = true;

  items.slice(0, 15).forEach((item) => {
    const row = document.createElement('div');
    row.className = 'activity-item';
    const repoUrl = item.repoName ? `https://github.com/${item.repoName}` : null;
    row.innerHTML = `
      <div class="activity-icon">${iconFor(item.type)}</div>
      <div>
        <div class="activity-desc">${escapeHtml(item.description)}</div>
        <div class="activity-time">
          ${formatRelativeTime(item.timestamp)}
          ${repoUrl ? ` &middot; <a href="${repoUrl}" target="_blank" rel="noopener">${escapeHtml(item.repoName)} &#8599;</a>` : ''}
        </div>
      </div>
    `;
    els.activityList.appendChild(row);
  });
}

function iconFor(type) {
  const map = {
    PushEvent: '&#8593;', PullRequestEvent: '&#8646;', IssuesEvent: '&#9679;',
    IssueCommentEvent: '&#128172;', WatchEvent: '&#9733;', ForkEvent: '&#8994;',
    CreateEvent: '+', DeleteEvent: '&#8722;', ReleaseEvent: '&#127991;',
  };
  return map[type] || '&#8226;';
}

function formatRelativeTime(iso) {
  if (!iso) return '';
  const then = new Date(iso).getTime();
  const diffMs = Date.now() - then;
  const days = Math.floor(diffMs / 86400000);
  if (days === 0) return 'today';
  if (days === 1) return 'yesterday';
  if (days < 30) return `${days} days ago`;
  const months = Math.floor(days / 30);
  return `${months} month${months === 1 ? '' : 's'} ago`;
}

async function ensureYearData(year) {
  if (state.yearDataCache[year]) return state.yearDataCache[year];
  const data = await fetchJSON(`${API_BASE}/${encodeURIComponent(username)}/contributions?year=${year}`);
  state.yearDataCache[year] = data;
  return data;
}

function wireControls() {
  els.toggleYear.addEventListener('click', () => switchView('year'));
  els.toggleMonth.addEventListener('click', () => switchView('month'));
  els.rangePrev.addEventListener('click', () => navigate(-1));
  els.rangeNext.addEventListener('click', () => navigate(1));
}

function switchView(view) {
  state.view = view;
  els.toggleYear.classList.toggle('active', view === 'year');
  els.toggleMonth.classList.toggle('active', view === 'month');
  render();
}

async function navigate(direction) {
  if (state.view === 'year') {
    state.year += direction;
    try {
      await ensureYearData(state.year);
    } catch (err) {
      state.year -= direction;
      return;
    }
  } else {
    state.month += direction;
    if (state.month > 11) { state.month = 0; state.year += 1; }
    if (state.month < 0) { state.month = 11; state.year -= 1; }
    try {
      await ensureYearData(state.year);
    } catch (err) {
      // revert
      state.month -= direction;
      if (state.month > 11) { state.month = 0; state.year -= 1; }
      if (state.month < 0) { state.month = 11; state.year += 1; }
      return;
    }
  }
  render();
}

function render() {
  const data = state.yearDataCache[state.year];
  if (!data) return;

  const currentYear = new Date().getFullYear();
  els.rangeNext.disabled = state.view === 'year'
    ? state.year >= currentYear
    : (state.year === currentYear && state.month === new Date().getMonth());

  if (state.view === 'year') {
    els.rangeLabel.textContent = String(state.year);
    els.totalTag.textContent = `${data.totalContributions} total`;
    renderYearChart(data);
  } else {
    els.rangeLabel.textContent = `${MONTH_NAMES[state.month]} ${state.year}`;
    const monthDays = data.daily.filter((d) => d.date.startsWith(monthKey(state.year, state.month)));
    const monthTotal = monthDays.reduce((sum, d) => sum + (d.count || 0), 0);
    els.totalTag.textContent = `${monthTotal} this month`;
    renderMonthChart(monthDays);
  }
}

function monthKey(year, monthIndex) {
  return `${year}-${String(monthIndex + 1).padStart(2, '0')}`;
}

function renderYearChart(data) {
  els.barChart.innerHTML = '';
  hideDayDetail();
  const monthly = [...data.monthly].sort((a, b) => a.month.localeCompare(b.month));
  const max = Math.max(1, ...monthly.map((m) => m.totalContributions));

  monthly.forEach((m) => {
    const monthIndex = parseInt(m.month.split('-')[1], 10) - 1;
    const col = document.createElement('div');
    col.className = 'bar-col';
    const heightPct = Math.max(2, (m.totalContributions / max) * 100);
    col.innerHTML = `
      <div class="bar-tooltip">
        <div class="tt-title">${MONTH_NAMES[monthIndex]} ${state.year}</div>
        <div>${m.totalContributions} contribution${m.totalContributions === 1 ? '' : 's'}</div>
        <div class="tt-activity">Click to view this month in detail</div>
      </div>
      <div class="bar" style="height:${heightPct}%"></div>
      <div class="bar-label">${MONTH_SHORT[monthIndex]}</div>
    `;
    col.querySelector('.bar').addEventListener('click', () => {
      state.month = monthIndex;
      switchView('month');
    });
    els.barChart.appendChild(col);
  });
}

function renderMonthChart(monthDays) {
  els.barChart.innerHTML = '';
  hideDayDetail();
  const sorted = [...monthDays].sort((a, b) => a.date.localeCompare(b.date));
  const max = Math.max(1, ...sorted.map((d) => d.count || 0));

  sorted.forEach((d) => {
    const dayNum = parseInt(d.date.split('-')[2], 10);
    const heightPct = Math.max(2, ((d.count || 0) / max) * 100);
    const activities = state.activityByDate[d.date];

    const col = document.createElement('div');
    col.className = 'bar-col';
    col.dataset.date = d.date;
    col.innerHTML = `
      <div class="bar-tooltip">
        <div class="tt-title">${d.date}</div>
        <div>${d.count || 0} contribution${d.count === 1 ? '' : 's'}</div>
        ${activities
          ? `<div class="tt-activity">${activities.slice(0, 3).map((a) => escapeHtml(a.description)).join('<br>')}</div>`
          : `<div class="tt-activity">${activityUnavailableMessage(d.date)}</div>`}
        <div class="tt-activity" style="margin-top:6px; opacity:0.8;">Click for full details</div>
      </div>
      <div class="bar" style="height:${heightPct}%"></div>
      <div class="bar-label">${dayNum}</div>
    `;
    col.addEventListener('click', () => showDayDetail(d, activities));
    els.barChart.appendChild(col);
  });
}

/**
 * Instead of a fixed "older than 90 days" message, this tells the person
 * exactly how many days ago the date was, which is much more useful and
 * transparent than a vague threshold statement.
 */
function activityUnavailableMessage(dateStr) {
  const days = daysAgo(dateStr);
  if (days <= 90) {
    return 'No public activity recorded for this day.';
  }
  return `Detailed activity unavailable - this day was ${days} days ago (GitHub only keeps itemized activity for about 90 days).`;
}

function daysAgo(dateStr) {
  const then = new Date(dateStr + 'T00:00:00Z').getTime();
  const now = new Date();
  const todayUtc = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());
  return Math.round((todayUtc - then) / 86400000);
}

/* ==========================================================================
   DAY DETAIL PANEL - shown when a day bar (Month view) is clicked
   ========================================================================== */
function showDayDetail(day, activities) {
  document.querySelectorAll('.bar-col').forEach((el) => el.classList.remove('selected'));
  const col = els.barChart.querySelector(`[data-date="${day.date}"]`);
  if (col) col.classList.add('selected');

  const panel = document.getElementById('day-detail-panel');
  const dateEl = document.getElementById('day-detail-date');
  const countEl = document.getElementById('day-detail-count');
  const activityEl = document.getElementById('day-detail-activity');

  panel.hidden = false;
  dateEl.textContent = formatFullDate(day.date);
  countEl.textContent = `${day.count || 0} contribution${day.count === 1 ? '' : 's'}`;

  activityEl.innerHTML = '';
  if (activities && activities.length > 0) {
    activities.forEach((item) => {
      const repoUrl = item.repoName ? `https://github.com/${item.repoName}` : null;
      const row = document.createElement('div');
      row.className = 'day-detail-activity-item';
      row.innerHTML = repoUrl
        ? `${escapeHtml(item.description)} &mdash; <a href="${repoUrl}" target="_blank" rel="noopener">${escapeHtml(item.repoName)} &#8599;</a>`
        : escapeHtml(item.description);
      activityEl.appendChild(row);
    });
  } else {
    const empty = document.createElement('div');
    empty.className = 'day-detail-empty';
    empty.textContent = activityUnavailableMessage(day.date);
    activityEl.appendChild(empty);
  }

  panel.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function hideDayDetail() {
  const panel = document.getElementById('day-detail-panel');
  if (panel) panel.hidden = true;
}

function formatFullDate(dateStr) {
  const d = new Date(dateStr + 'T00:00:00Z');
  return d.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
}

document.getElementById('day-detail-close')?.addEventListener('click', hideDayDetail);
