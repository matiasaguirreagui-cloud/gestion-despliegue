import { apiRequest } from './apiClient.js';

export function obtenerEquipos() {
  return apiRequest('/api/equipos');
}

export function obtenerEquipoPorId(idEquipo) {
  return apiRequest(`/api/equipos/${encodeURIComponent(idEquipo)}`);
}

export function crearEquipo(equipo) {
  return apiRequest('/api/equipos', {
    method: 'POST',
    body: JSON.stringify(equipo),
  });
}

export function actualizarEquipo(idEquipo, equipo) {
  return apiRequest(`/api/equipos/${encodeURIComponent(idEquipo)}`, {
    method: 'PUT',
    body: JSON.stringify(equipo),
  });
}

export function marcarFueraDeServicio(idEquipo) {
  return apiRequest(`/api/equipos/${encodeURIComponent(idEquipo)}/fuera-de-servicio`, {
    method: 'POST',
  });
}
