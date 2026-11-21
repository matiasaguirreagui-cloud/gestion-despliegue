import { apiRequest } from './apiClient.js';

export function obtenerRevisiones() {
  return apiRequest('/api/revisiones');
}

export function obtenerRevisionPorId(id) {
  return apiRequest(`/api/revisiones/${encodeURIComponent(id)}`);
}

export function crearRevision(revision) {
  return apiRequest('/api/revisiones', { method: 'POST', body: JSON.stringify(revision) });
}

export function reprogramarRevision(id, nuevaFecha) {
  return apiRequest(`/api/revisiones/${encodeURIComponent(id)}/reprogramar`, {
    method: 'PUT',
    body: JSON.stringify({ nuevaFecha }),
  });
}

export function actualizarRevision(id, revision) {
  return apiRequest(`/api/revisiones/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(revision),
  });
}
