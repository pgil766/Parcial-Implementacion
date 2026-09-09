const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export class ApiError extends Error {
  constructor(message, status = 0) { super(message); this.status = status; }
}

async function request(path, options = {}) {
  const token = localStorage.getItem('racing_access_token');
  const headers = { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) };
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new ApiError(body.message || `Request failed (${response.status})`, response.status);
  return body;
}
const json = (method, payload) => ({ method, body: JSON.stringify(payload) });

export const api = {
  login: (payload) => request('/auth/login', json('POST', payload)),
  profile: () => request('/auth/profile'),
  races: () => request('/races'),
  race: (id) => request(`/races/${id}`),
  createRace: (payload) => request('/races', json('POST', payload)),
  updateRace: (id, payload) => request(`/races/${id}`, json('PUT', payload)),
  updateRaceStatus: (id, status) => request(`/races/${id}/status`, json('PATCH', { status })),
  competitors: (params = '') => request(`/competitors${params}`),
  competitor: (id) => request(`/competitors/${id}`),
  createCompetitor: (payload) => request('/competitors', json('POST', payload)),
  updateCompetitor: (id, payload) => request(`/competitors/${id}`, json('PUT', payload)),
  teams: () => request('/teams'),
  team: (id) => request(`/teams/${id}`),
  createTeam: (payload) => request('/teams', json('POST', payload)),
  addMember: (teamId, competitorId) => request(`/teams/${teamId}/members/${competitorId}`, json('POST', {})),
  removeMember: (teamId, competitorId) => request(`/teams/${teamId}/members/${competitorId}`, { method: 'DELETE' }),
  standings: () => request('/standings'),
  competitorStandings: () => request('/standings/competitors'),
  teamStandings: () => request('/standings/teams'),
  registrations: (raceId) => request(`/races/${raceId}/registrations`),
  createRegistration: (raceId, payload) => request(`/races/${raceId}/registrations`, json('POST', payload)),
  approveRegistration: (id, startingPosition) => request(`/registrations/${id}/approve`, json('PATCH', startingPosition ? { startingPosition } : {})),
  rejectRegistration: (id, reason) => request(`/registrations/${id}/reject`, json('PATCH', { reason })),
  results: (raceId) => request(`/races/${raceId}/results`),
  createResult: (raceId, payload) => request(`/races/${raceId}/results`, json('POST', payload)),
  updateResult: (id, payload) => request(`/results/${id}`, json('PUT', payload)),
};

export function saveSession(auth) {
  localStorage.setItem('racing_access_token', auth.accessToken);
  localStorage.setItem('racing_refresh_token', auth.refreshToken);
  localStorage.setItem('racing_user', JSON.stringify({ username: auth.username, role: auth.role }));
}
export function clearSession() { ['racing_access_token', 'racing_refresh_token', 'racing_user'].forEach((key) => localStorage.removeItem(key)); }
export function storedUser() { try { return JSON.parse(localStorage.getItem('racing_user')) || null; } catch { return null; } }
