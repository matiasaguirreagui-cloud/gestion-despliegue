import { apiRequest } from './apiClient.js';

export function obtenerSolicitudes() {
  return apiRequest('/api/solicitudes');
}

export function obtenerSolicitudPorId(id) {
  return apiRequest(`/api/solicitudes/${encodeURIComponent(id)}`);
}

export function crearSolicitud(solicitud) {
  return apiRequest('/api/solicitudes', { method: 'POST', body: JSON.stringify(solicitud) });
}

export function cerrarSolicitud(id, datos) {
  return apiRequest(`/api/solicitudes/${encodeURIComponent(id)}/cerrar`, {
    method: 'POST',
    body: JSON.stringify(datos),
  });
}
