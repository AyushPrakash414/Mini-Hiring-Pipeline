const API_BASE = '/api/candidates';

let currentSelectedCandidateId = null;

// DOM Elements
const searchInput = document.getElementById('search-input');
const searchBtn = document.getElementById('search-btn');
const resultsList = document.getElementById('results-list');
const resultsCount = document.getElementById('results-count');
const detailsContainer = document.getElementById('details-container');
const btnSeedData = document.getElementById('btn-seed-data');
const toastEl = document.getElementById('toast');

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    // Initial fetch of candidates
    fetchAllCandidates();

    // Search button click
    searchBtn.addEventListener('click', performSearch);

    // Enter key on search input
    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            performSearch();
        }
    });

    // Chip click for quick suggestions
    document.querySelectorAll('.chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const query = chip.getAttribute('data-query');
            searchInput.value = query;
            performSearch();
        });
    });

    // Seed Demo Candidates
    btnSeedData.addEventListener('click', async () => {
        try {
            btnSeedData.disabled = true;
            btnSeedData.innerText = 'Seeding...';
            const res = await fetch(`${API_BASE}/seed`, { method: 'POST' });
            const data = await res.json();
            showToast(data.message || 'Sample data seeded!');
            fetchAllCandidates();
        } catch (err) {
            showToast('Failed to seed sample data');
        } finally {
            btnSeedData.disabled = false;
            btnSeedData.innerHTML = '<span>⚡</span> Seed Demo Candidates';
        }
    });
});

/**
 * Fetch all candidates (default view when search query is empty)
 */
async function fetchAllCandidates() {
    try {
        const res = await fetch(API_BASE);
        if (!res.ok) throw new Error('API returned ' + res.status);
        const candidates = await res.json();
        renderCandidateList(Array.isArray(candidates) ? candidates : [], false);
    } catch (err) {
        console.error(err);
        showToast('Error connecting to backend API');
    }
}

// Router DOM elements
const routerInspector = document.getElementById('router-inspector');
const routerIntentBadge = document.getElementById('router-intent-badge');
const routerExplanation = document.getElementById('router-explanation');
const routerTagsContainer = document.getElementById('router-tags-container');

/**
 * Perform Search with Apache OpenNLP Query Intent Routing
 */
async function performSearch() {
    const query = searchInput.value.trim();
    if (!query) {
        if (routerInspector) routerInspector.style.display = 'none';
        fetchAllCandidates();
        return;
    }

    resultsList.innerHTML = `
        <div class="empty-placeholder">
            <div class="placeholder-icon">⏳</div>
            <p class="placeholder-title">Classifying &amp; Searching...</p>
        </div>
    `;

    try {
        // Step 1: Query Intent Routing via Apache OpenNLP POS Tagger
        const routeRes = await fetch(`${API_BASE}/route-query?query=${encodeURIComponent(query)}`);
        if (!routeRes.ok) throw new Error('Routing failed with status: ' + routeRes.status);
        const routeData = await routeRes.json();

        // Render OpenNLP Inspector Bar
        renderRouterInspector(routeData);

        // Step 2: Branch based on Intent
        if (routeData.intent === 'NAME_SEARCH') {
            // Execute PostgreSQL pg_trgm fuzzy candidate search
            const searchRes = await fetch(`${API_BASE}/search?query=${encodeURIComponent(query)}&limit=20`);
            if (!searchRes.ok) throw new Error('Search failed with status: ' + searchRes.status);
            const candidates = await searchRes.json();
            renderCandidateList(Array.isArray(candidates) ? candidates : [], true);
        } else {
            // Natural Language Intent Mode
            renderNaturalLanguageMode(routeData);
        }
    } catch (err) {
        console.error(err);
        resultsList.innerHTML = `
            <div class="empty-placeholder">
                <div class="placeholder-icon">⚠️</div>
                <p class="placeholder-title">Search Failed</p>
                <p class="placeholder-text">Could not perform search. Ensure the backend is running.</p>
            </div>
        `;
        showToast('Search query failed');
    }
}

/**
 * Render the OpenNLP Router Inspector Pills and Intent Badge
 */
function renderRouterInspector(routeData) {
    if (!routerInspector) return;
    routerInspector.style.display = 'block';

    const isName = routeData.intent === 'NAME_SEARCH';
    routerIntentBadge.className = isName ? 'badge badge-name-intent' : 'badge badge-nl-intent';
    routerIntentBadge.innerHTML = isName ? '👤 Candidate Name Search' : '🤖 Natural Language Query';
    
    routerExplanation.textContent = routeData.explanation || '';

    // Render POS Tag pills
    if (routeData.tokenTags && routeData.tokenTags.length > 0) {
        routerTagsContainer.innerHTML = routeData.tokenTags.map(t => {
            const pillClass = t.noun ? 'pos-tag-pill is-noun' : 'pos-tag-pill is-non-noun';
            return `
                <div class="${pillClass}" title="${t.tagDescription || t.tag}">
                    <span class="pos-tag-name">${escapeHtml(t.token)}</span>
                    <span class="pos-tag-badge">${escapeHtml(t.tag)}</span>
                </div>
            `;
        }).join('');
    } else {
        routerTagsContainer.innerHTML = '';
    }
}

/**
 * Render UI when query is classified as Natural Language
 */
function renderNaturalLanguageMode(routeData) {
    resultsCount.textContent = `NL Mode`;

    resultsList.innerHTML = `
        <div class="nl-preview-card">
            <h3>🤖 Natural Language Intent Detected</h3>
            <p><strong>Query:</strong> "${escapeHtml(routeData.originalQuery)}"</p>
            <p>${escapeHtml(routeData.explanation)}</p>
            <div style="margin-top: 16px; padding: 12px; background: rgba(0,0,0,0.25); border-radius: 8px; font-size: 13px;">
                <p style="color: #a78bfa; margin-bottom: 6px; font-weight: 600;">OpenNLP POS Grammatical Structure:</p>
                <div style="display: flex; flex-wrap: wrap; gap: 6px;">
                    ${routeData.tokenTags.map(t => `
                        <span style="font-size: 11px; padding: 2px 6px; border-radius: 4px; background: ${t.noun ? 'rgba(59,130,246,0.2)' : 'rgba(236,72,153,0.2)'}; color: ${t.noun ? '#93c5fd' : '#f472b6'};">
                            <strong>${escapeHtml(t.token)}</strong> (${escapeHtml(t.tag)}: ${escapeHtml(t.tagDescription)})
                        </span>
                    `).join('')}
                </div>
            </div>
            <p style="margin-top: 14px; font-size: 12px; color: var(--text-muted);">
                ✨ Ready for LLM processing (NL-to-SQL / Candidate Filtering pipeline).
            </p>
        </div>
    `;
}

/**
 * Render candidate list in the left panel
 */
function renderCandidateList(candidates, isFuzzySearch) {
    resultsCount.textContent = `${candidates.length} found`;

    if (!candidates || candidates.length === 0) {
        resultsList.innerHTML = `
            <div class="empty-placeholder">
                <div class="placeholder-icon">❌</div>
                <p class="placeholder-title">No Candidates Found</p>
                <p class="placeholder-text">Try another query or check spelling with typo tolerance.</p>
            </div>
        `;
        return;
    }

    resultsList.innerHTML = candidates.map(c => {
        const similarityBadge = isFuzzySearch && c.similarityScore !== undefined
            ? `<span class="badge badge-similarity" title="Trigram similarity score">Similarity: ${(c.similarityScore * 100).toFixed(0)}%</span>`
            : '';

        const stage = c.currentStage || 'APPLIED';
        const isSelected = c.id === currentSelectedCandidateId ? 'active' : '';

        return `
            <div class="candidate-item ${isSelected}" data-id="${c.id}" onclick="selectCandidate(${c.id})">
                <div class="candidate-item-main">
                    <div class="candidate-item-name">${escapeHtml(c.name)}</div>
                    <div class="candidate-item-email">${escapeHtml(c.email)}</div>
                </div>
                <div class="candidate-item-meta">
                    <span class="badge stage-${stage}">${stage}</span>
                    ${similarityBadge}
                </div>
            </div>
        `;
    }).join('');
}

/**
 * Select a candidate and load details + audit trail
 */
async function selectCandidate(candidateId) {
    currentSelectedCandidateId = candidateId;

    // Highlight selected item in list
    document.querySelectorAll('.candidate-item').forEach(el => {
        el.classList.toggle('active', el.getAttribute('data-id') == candidateId);
    });

    detailsContainer.innerHTML = `
        <div class="empty-placeholder">
            <div class="placeholder-icon">⏳</div>
            <p class="placeholder-title">Loading Profile...</p>
        </div>
    `;

    try {
        const [candidateRes, historyRes] = await Promise.all([
            fetch(`${API_BASE}/${candidateId}`),
            fetch(`${API_BASE}/${candidateId}/history`)
        ]);

        const candidate = await candidateRes.json();
        const history = await historyRes.json();

        renderCandidateDetails(candidate, history);
    } catch (err) {
        console.error(err);
        detailsContainer.innerHTML = `
            <div class="empty-placeholder">
                <div class="placeholder-icon">⚠️</div>
                <p class="placeholder-title">Error Loading Details</p>
            </div>
        `;
        showToast('Failed to load candidate details');
    }
}

/**
 * Render candidate profile card & history timeline
 */
function renderCandidateDetails(candidate, history) {
    const stage = candidate.currentStage;

    const timelineItems = history.map((h, index) => {
        const fromBadge = h.fromStage 
            ? `<span class="badge stage-${h.fromStage}">${h.fromStage}</span>` 
            : `<span class="badge badge-neutral">CREATED</span>`;
        const toBadge = `<span class="badge stage-${h.toStage}">${h.toStage}</span>`;
        const dateStr = new Date(h.changedAt).toLocaleString();

        return `
            <div class="timeline-node">
                <div class="timeline-box">
                    <div class="timeline-header">
                        <div class="timeline-transition">${fromBadge} &rarr; ${toBadge}</div>
                        <span class="timeline-time">${dateStr}</span>
                    </div>
                    <p class="timeline-reason">${escapeHtml(h.reason || 'No notes provided')}</p>
                </div>
            </div>
        `;
    }).join('');

    detailsContainer.innerHTML = `
        <div class="profile-card">
            <div class="profile-header">
                <div>
                    <h3 class="profile-title-name">${escapeHtml(candidate.name)}</h3>
                    <p class="subtitle">Candidate ID: #${candidate.id}</p>
                </div>
                <span class="badge stage-${stage}">${stage}</span>
            </div>

            <div class="profile-grid">
                <div class="info-item">
                    <div class="info-label">Email</div>
                    <div class="info-value">${escapeHtml(candidate.email)}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Phone</div>
                    <div class="info-value">${escapeHtml(candidate.phone || 'N/A')}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Current Stage</div>
                    <div class="info-value">${stage}</div>
                </div>
                <div class="info-item">
                    <div class="info-label">Applied Date</div>
                    <div class="info-value">${new Date(candidate.createdAt).toLocaleDateString()}</div>
                </div>
            </div>

            <div class="timeline-section">
                <div class="timeline-section-title">Stage Progression Audit Trail (${history.length} events)</div>
                <div class="timeline-list">
                    ${timelineItems || '<p class="text-muted">No history records found.</p>'}
                </div>
            </div>
        </div>
    `;
}

/**
 * Toast notification helper
 */
function showToast(message) {
    toastEl.textContent = message;
    toastEl.classList.add('show');
    setTimeout(() => {
        toastEl.classList.remove('show');
    }, 3000);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;')
              .replace(/'/g, '&#039;');
}
