const API_BASE = '/api/candidates';

export async function fetchPipeline() {
  const res = await fetch(`${API_BASE}/pipeline`);
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function fetchAllCandidates() {
  const res = await fetch(API_BASE);
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function fetchCandidate(id) {
  const res = await fetch(`${API_BASE}/${id}`);
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function fetchCandidateHistory(id) {
  const res = await fetch(`${API_BASE}/${id}/history`);
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function createCandidate({ name, email, phone }) {
  const res = await fetch(API_BASE, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, phone }),
  });
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function transitionStage(id, targetStage, reason) {
  const res = await fetch(`${API_BASE}/${id}/transition`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ targetStage, reason }),
  });
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function searchCandidates(query, limit = 20) {
  const res = await fetch(`${API_BASE}/search?query=${encodeURIComponent(query)}&limit=${limit}`);
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function routeQuery(query) {
  const res = await fetch(`${API_BASE}/route-query?query=${encodeURIComponent(query)}`);
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function nlQuery(query) {
  const res = await fetch(`${API_BASE}/nl-query`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw await parseError(res);
  return res.json();
}

export async function seedData() {
  const res = await fetch(`${API_BASE}/seed`, { method: 'POST' });
  if (!res.ok) throw await parseError(res);
  return res.json();
}

async function parseError(res) {
  try {
    const body = await res.json();
    return new ApiError(body.message || body.error || 'Request failed', res.status, body);
  } catch {
    return new ApiError(`Request failed with status ${res.status}`, res.status);
  }
}

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}
