import type { CondicionActivo, EstadoActivo } from './activos'
import { apiRequest } from './client'

export interface AsignacionResponse {
  id: number
  solicitudId: number
  solicitudDetalleId: number
  activoId: number
  codigoPatrimonial: string
  tipoActivo: string
  usuarioReceptorId: number
  usuarioReceptor: string
  entregadoPor: string
  recibidoDevolucionPor: string | null
  fechaEntrega: string
  fechaDevolucionEstimada: string
  fechaDevolucionReal: string | null
  observacionesEntrega: string | null
  observacionesRetorno: string | null
  estadoActivo: EstadoActivo
  condicionFisica: CondicionActivo
  abierta: boolean
}

export interface DevolucionAsignacionRequest {
  condicionFisica: CondicionActivo
  estadoActivo: 'DISPONIBLE' | 'EN_MANTENIMIENTO'
  observaciones: string | null
}

export function listarAsignaciones(abiertas = true) {
  return apiRequest<AsignacionResponse[]>(`/api/asignaciones?abiertas=${abiertas}`)
}

export function devolverAsignacion(id: number, request: DevolucionAsignacionRequest) {
  return apiRequest<AsignacionResponse>(`/api/asignaciones/${id}/devolucion`, {
    method: 'PUT', body: request,
  })
}
