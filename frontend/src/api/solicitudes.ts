import { apiRequest } from './client'

export const ESTADOS_SOLICITUD = ['PENDIENTE', 'APROBADA', 'RECHAZADA', 'CANCELADA'] as const
export type EstadoSolicitud = (typeof ESTADOS_SOLICITUD)[number]

export interface DetalleRequest {
  tipoActivoId: number
  observaciones: string | null
}

export interface SolicitudRequest {
  motivo: string
  detalles: DetalleRequest[]
}

export interface DetalleResponse {
  id: number
  tipoActivoId: number
  tipoActivo: string
  activo: string | null
  observaciones: string | null
}

export interface SolicitudResponse {
  id: number
  solicitante: string
  fechaSolicitud: string
  fechaAtencion: string | null
  motivo: string
  estado: EstadoSolicitud
  observacionesAtencion: string | null
  detalles: DetalleResponse[]
}

export interface ActivoElegido {
  detalleId: number
  activoId: number
}

export interface AtencionRequest {
  aprobada: boolean
  observaciones: string | null
  activos: ActivoElegido[] | null
}

export interface CancelarSolicitudRequest {
  motivo: string
}

export function crearSolicitud(request: SolicitudRequest) {
  return apiRequest<SolicitudResponse>('/api/solicitudes', { method: 'POST', body: request })
}

export function listarMisSolicitudes() {
  return apiRequest<SolicitudResponse[]>('/api/solicitudes/mis-solicitudes')
}

export function listarSolicitudes(estado?: EstadoSolicitud | '') {
  const query = estado ? `?estado=${encodeURIComponent(estado)}` : ''
  return apiRequest<SolicitudResponse[]>(`/api/solicitudes${query}`)
}

export function atenderSolicitud(id: number, request: AtencionRequest) {
  return apiRequest<SolicitudResponse>(`/api/solicitudes/${id}`, { method: 'PUT', body: request })
}

export function cancelarSolicitud(id: number, request: CancelarSolicitudRequest) {
  return apiRequest<SolicitudResponse>(`/api/solicitudes/${id}/cancelacion`, { method: 'PUT', body: request })
}
