# DevTrack - GitHub Contribution & Career Insights

A Spring Boot backend + a multi-page frontend that turns a GitHub username
into a full analytics dashboard: profile, a real year/month contribution
chart with day-level activity detail, detected skills, AI career guidance,
open-source repo recommendations, and real pull-request contribution
status (open/merged/closed).

```
devtrack-insights/
├── backend/     Spring Boot API (Java 17+, Maven)
└── frontend/    Static multi-page site (plain HTML/CSS/JS, no build step)
     ├── index.html          Landing page - enter a username
     ├── profile.html        Overview (default landing after search)
     ├── contributions.html  Year/month contribution chart + activity feed
     ├── skills.html         Ranked language/skill breakdown
     ├── career.html         AI role suggestions + improvement tips
     ├── repos.html          The person's own repositories (filterable by language)
     └── opensource.html     Real PR status (open/merged/closed) + suggestions
```

Each section is its own real page with its own URL
(`contributions.html?u=octocat`), connected by a persistent left sidebar.

## What's new in this version

- **Fixed a data bug that made the contribution calendar always show 0**,
  and silently broke repo star/fork counts and GitHub links. Root cause:
  a few DTOs are used both to read GitHub's raw JSON (`snake_case`) and to
  send our own API's response (`camelCase`); using `@JsonProperty` affected
  both directions, so our own output was accidentally using GitHub's raw
  field names instead of what the frontend expects. Switched to
  `@JsonAlias`, which only affects reading GitHub's data, not our output.
- **Repository links now actually work** - clicking any repo card (in
  Repositories, Open Source, or the activity feed on Contributions) opens
  the real GitHub page, in a new tab.
- **New "Open Source" page** - every pull request the user has opened
  anywhere on GitHub, with its real, live status (OPEN / MERGED / CLOSED)
  pulled fresh from GitHub every time the page loads - filterable by
  status, each row linking to both the PR and its repository.
- **Repo suggestions now include a real, dynamically-generated reason**
  for each pick (their matching skill's percentage + that repo's actual
  star count and open-issue count) - computed fresh each time, not a
  fixed template.

---

## STEP 1 - Run the backend in IntelliJ

1. **File → Open** → select the `backend` folder (the one with `pom.xml`
   directly inside). Do not open the outer `fullstack-project` folder.
2. Let Maven sync.
3. Fill in `backend/src/main/resources/application.properties`:
   ```
   github.api.token=your_own_token   (required for Contributions and Open Source pages)
   gemini.api.key=your_own_key       (optional, powers Career page)
   ```
   - GitHub token: https://github.com/settings/tokens -> classic token, no scopes.
   - Gemini key: https://aistudio.google.com/app/apikey
4. Run `GithubTrackerApplication.java`.
5. Confirm: `http://localhost:8080/api/github/ping` -> `{"status":"UP",...}`.

Leave it running.

---

## STEP 2 - Run the frontend (must be via a local server, not double-click)

Opening `index.html` directly (`file://`) will silently fail to reach the
backend in most browsers. Use one of these instead:

**VS Code (recommended):**
1. Open the `frontend` folder in VS Code.
2. Install the **Live Server** extension if you don't have it.
3. Right-click `index.html` -> **Open with Live Server**.

**Or, plain Python:**
```
cd frontend
python -m http.server 5500
```
then open `http://localhost:5500`.

---

## STEP 3 - Use it

1. On the landing page, type a GitHub username (or click an example chip).
2. You land on **Overview** - profile summary + 5 quick-link tiles into
   each section.
3. Use the left sidebar to move between **Contributions**, **Skills**,
   **Career**, **Repositories**, and **Open Source** - each a separate page.
4. On **Contributions**: toggle Year/Month, use the arrows to move between
   periods, hover any bar for the exact count - in Month view, hovering
   also shows real recent activity for that day where available (GitHub
   only exposes itemized activity for the last ~90 days; older days show
   count only, which the tooltip explains). Click a bar in Year view to
   jump straight into that month.
5. On **Open Source**: see every real pull request this person has opened
   anywhere on GitHub, with its live status, filterable by Open/Merged/
   Closed. Click any PR title or repo name to open it on GitHub.
6. Click **Change user** (bottom of the sidebar) to look up someone else.

---

## STEP 4 - Verify every feature

### Backend, in Postman

| Feature | Request | Success looks like |
|---|---|---|
| Health | `GET /api/github/ping` | `200`, `{"status":"UP"}` |
| Profile | `GET /api/github/octocat` | `200`, profile JSON with `avatarUrl`, `htmlUrl` (camelCase) |
| Repos | `GET /api/github/octocat/repos` | `200`, array with `htmlUrl`, `stars`, `forks` (camelCase, non-zero) |
| Contributions (default) | `GET /api/github/octocat/contributions` | `200`, `daily` array with real non-zero `count` values |
| Contributions (specific year) | `GET /api/github/octocat/contributions?year=2025` | `200`, that year's data |
| Activity feed | `GET /api/github/octocat/activity` | `200`, array of `{date, description, repoName, ...}` |
| Skills | `GET /api/github/octocat/skills` | `200`, ranked languages |
| Career | `GET /api/github/octocat/career-suggestions` | `200`, `eligibleRoles: [{title, reason}]` |
| Open source suggestions | `GET /api/github/octocat/open-source-suggestions` | `200`, `basedOnSkills` + `suggestions: [{repo, reason}]` - differs per user |
| Open source contributions | `GET /api/github/octocat/open-source-contributions` | `200`, `{totalCount, openCount, mergedCount, closedCount, contributions: [...]}` |
| Combined dashboard | `GET /api/github/octocat/dashboard` | `200`, everything except open-source, one call |
| Error handling | `GET /api/github/asdkjhaskdjh123xyz` | `404`, clean JSON error |

### Frontend, page by page

- [ ] **Landing** - invalid username shows a clear inline error; valid one navigates to Overview
- [ ] **Overview** - avatar/name/stats load; all 5 quick-link tiles show real values and link correctly, including the new Pull Requests tile
- [ ] **Contributions** - bars show varying, non-zero heights in both Year and Month view; clicking a bar drills into that month; hovering shows the exact count; hovering a recent day also shows real activity; Recent Activity list entries have clickable repo names that open the real GitHub repo
- [ ] **Skills** - languages ranked, grouped into Primary/Also used
- [ ] **Career** - role cards each show a title + a specific reason; if no Gemini key is set, a clear info banner explains it
- [ ] **Repositories** - shows only the person's own repos, with language filter pills; every card opens the real GitHub page in a new tab
- [ ] **Open Source** (new) - the 4 summary numbers (total/open/merged/closed) are accurate; filter tabs correctly narrow the PR list; every PR title and repo name link to the real GitHub pages; suggestion cards here also show real reasons
- [ ] Sidebar nav (now 6 items) correctly highlights the current page and carries the username between all pages
- [ ] Narrow/mobile browser width still shows usable stacked content

If every box is checked, the full stack is confirmed working end-to-end,
with all data genuinely live from GitHub - nothing hardcoded or cached
across visits, so results change naturally as the person's real GitHub
activity changes.

---

## Notes

- `github.api.token` and `gemini.api.key` are personal credentials - never
  commit real values or share them anywhere outside your local machine.
- CORS is wide open (`*`) in `backend/.../config/CorsConfig.java` for local
  development. Tighten it before any public deployment.
- The activity feed and the "what happened this day" tooltip detail both
  depend on GitHub's public Events API, which only covers roughly the last
  90 days - this is a GitHub platform limitation, not a bug in this app.
- The Open Source page uses GitHub's Search API (`is:pr author:username`),
  which only finds PRs on GitHub.com and only what's publicly visible.

---

## Fixes in this update

- **Skills now reflect the FULL tech stack, not just each repo's single
  "primary" language.** Switched to GitHub's per-repo Languages API
  (byte-weighted across every language actually present in each repo,
  not just the dominant one). A MERN project now correctly contributes
  to JavaScript, HTML, and CSS all at once, instead of only counting as
  "JavaScript". Analyzes the most recently updated 25 non-fork repos.
- **Career suggestions now explicitly reason about the full combination
  of technologies**, not just whichever one has the highest percentage -
  the prompt was updated to require this.
- **Open Source page pagination** - now fetches up to 300 pull requests
  (3 pages of 100) instead of just the first 50, so older contributions
  aren't silently missing from the list.
- **Open Source page now shows a short summary of what each PR actually
  did** (pulled directly from the PR's own description text, cleaned up
  and trimmed - no extra API calls needed).
- **Activity feed pagination** - now fetches up to 300 events (GitHub's
  practical ceiling) instead of just the first 100, so the "hover a day"
  tooltip on the Contributions page correctly finds activity for more of
  the last ~90 days instead of just the most recent handful of days.

---

## Fixes in this update (performance + the reported issues)

**Performance - the app was slow because of repeated, uncached work.**
Skill analysis makes one GitHub API call per repository. Previously that
ran fresh on EVERY page load, and five different pages each triggered it
independently - so opening Overview, Skills, Career, Repositories and
Open Source in one session did that expensive work five separate times.
Two fixes:
  - Those per-repo calls now run **in parallel** instead of one-by-one.
  - Results are **cached in memory per username** (5-10 min TTL), so only
    the first page pays the cost and the rest are near-instant.
  - Also reduced the analyzed-repo cap from 25 to 15, which keeps the
    skill picture accurate while cutting the worst-case call count.

This caching also indirectly fixes cases where Skills / Career / Open
Source came back empty: the old version could exhaust GitHub rate limits
or time out partway through, leaving sections blank.

**Calendar day details.** Clicking a day bar in Month view now opens a
detail panel below the chart showing that day's contribution count and
every recorded activity, each linking to the relevant repository.
Clicking a month bar in Year view still drills into that month. Instead
of a vague "older than 90 days" message, days without itemized data now
say exactly how many days ago they were and why the detail isn't there.

**Open Source suggestions.** New dedicated endpoint
(`/open-source-suggestions`) that picks repositories using BOTH detected
skills AND real prior contribution history - it also excludes repos the
person has already contributed to. Each card shows a direct GitHub link
plus a specific reason referencing their actual skill percentages and the
repo's real star/issue counts.


---

## Performance + personalised suggestions (latest update)

**Why it was slow - and what changed**

| Cause | Fix |
|---|---|
| Profile and repo list re-fetched from GitHub on every page | Cached 5 min on the server |
| Parallel GitHub calls ran on Java's shared pool (often only 2-3 threads) | Dedicated 16-thread pool for GitHub calls |
| Two pages asking for the same data at once both hit GitHub | "Single-flight" cache: one request does the work, the rest wait for it |
| Every GitHub response body was read and logged | Bodies are only logged on errors; each call now logs how long it took |
| No timeouts - one stalled call could hang a page | 5s connect / 20s read timeouts |
| Event-feed pages fetched one after another | Fetched in parallel |
| AI (Gemini) result regenerated every visit | Cached 30 min per skill set |
| Frontend: profile -> data -> render in a chain; Overview waited for the slowest of 6 requests | All requests start together; every section draws as soon as its own data arrives |

Tip: the backend console now prints lines like `GET https://api.github.com/... -> 200 in 312ms`,
so if anything is ever slow you can see exactly which call it is.

**Open source suggestions are now personalised** (and live only on the Open Source page)

Each person's list is built from four signals, run as parallel GitHub searches:
1. **Repos they have already opened PRs on** -> similar projects (same topics / language)
2. **Topics on their own repositories** (e.g. `react`)
3. **Their language mix**, weighted by real code percentages
4. **Their experience** - 0 merged PRs gets smaller projects with `good first issue` work;
   5+ merged PRs gets larger projects with `help wanted` work

Repos they already contributed to, and repos they own, are never suggested. Each card has the
repo's own description, a direct GitHub link, and a reason naming which signal produced it.

**Other fixes in this round**
- Search queries now encode `+` correctly, so `C++` is searched as C++ (it used to become "C").
- The Open Source page now excludes PRs on the person's own repositories, since those are not
  open-source contributions.

**About the GitHub token:** it is read from `application.properties`, but an environment
variable named `GITHUB_TOKEN` overrides it. If you push this project to a public GitHub repo
with a token inside, GitHub's secret scanning automatically revokes it - which may be why the
previous token expired. Keep tokens out of anything you push.


---

## Speed update (latest)

Where time was still going on a cold load, and what was changed:

| Cause | Fix |
|---|---|
| GitHub responses (repo list, PR search, events feed) arrived uncompressed - hundreds of KB each | The backend now requests gzip and decompresses transparently (JSON shrinks roughly 5-10x) |
| Suggestions waited for skills, THEN pull requests, THEN repos, one after another | All three are now fetched at the same time |
| Nothing started on the server until the Overview page had loaded | Pressing **Analyze** now calls `/warmup`, which starts skills, contributions, activity, PRs, suggestions and the AI career advice in the background while the browser is still navigating |
| Google Fonts stylesheet blocked page rendering | Loaded non-blocking (pages paint immediately with a fallback font, then switch) |
| Only 5 idle HTTPS connections kept for reuse | Raised to 32, so bursts of parallel calls reuse connections instead of redoing the TLS handshake |
| Gemini retry waited 1.5s / 3s between attempts | Now 0.8s / 1.6s |

**Honest note on first load:** the very first load of a username after the backend starts
still has to talk to GitHub (and, for the Career page, Gemini), so it will always be slower
than later visits, which come from cache. If something is still slow, the backend console
prints one line per GitHub call, e.g. `GET https://api.github.com/... -> 200 in 412ms`.
The slowest lines show exactly where the time goes.

New endpoint: `GET /api/github/{username}/warmup` -> `202 {"status":"warming"}` (fire-and-forget).
