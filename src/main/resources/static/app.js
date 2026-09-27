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

/**
 * Perform Typo-Tolerant Trigram Search
 */
async function performSearch() {
    const query = searchInput.value.trim();
    if (!query) {
        fetchAllCandidates();
        return;
    }

    resultsList.innerHTML = `
        <div class="empty-placeholder">
            <div class="placeholder-icon">⏳</div>
            <p class="placeholder-title">Searching...</p>
        </div>
    `;

    try {
        const res = await fetch(`${API_BASE}/search?query=${encodeURIComponent(query)}&limit=20`);
        if (!res.ok) throw new Error('Search failed with status: ' + res.status);
        const results = await res.json();
        renderCandidateList(Array.isArray(results) ? results : [], true);
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
