import { apiRequest } from './client'

export const ESTADOS_ACTIVO = ['DISPONIBLE', 'ASIGNADO', 'EN_MANTENIMIENTO', 'DE_BAJA'] as const
export const CONDICIONES_ACTIVO = ['BUENO', 'REGULAR', 'MALO'] as const

export type EstadoActivo = (typeof ESTADOS_ACTIVO)[number]
export type CondicionActivo = (typeof CONDICIONES_ACTIVO)[number]

export interface ActivoRequest {
  tipoActivoId: number
  ubicacionId: number
  codigoPatrimonial: string
  numeroSerie: string | null
  marca: string
  modelo: string
  descripcion: string | null
  estado: EstadoActivo
  condicionFisica: CondicionActivo
}

export interface ActivoResponse {
  id: number
  tipoActivoId: number
  ubicacionId: number
  codigoPatrimonial: string
  numeroSerie: string | null
  marca: string
  modelo: string
  descripcion: string | null
  categoria: string
  tipoActivo: string
  ubicacion: string
  estado: EstadoActivo
  condicionFisica: CondicionActivo
  reservado: boolean
}

export interface HistorialResponse {
  id: number
  estadoAnterior: EstadoActivo | null
  estadoNuevo: EstadoActivo
  fechaCambio: string
  motivo: string | null
  usuarioResponsable: string
}

export interface TipoActivoResponse {
  id: number
  nombre: string
  categoria: string
}

export interface UbicacionResponse {
  id: number
  codigo: string
  nombre: string
}

export function listarActivos(estado?: EstadoActivo | '') {
  const query = estado ? `?estado=${encodeURIComponent(estado)}` : ''
  return apiRequest<ActivoResponse[]>(`/api/activos${query}`)
}

export function crearActivo(request: ActivoRequest) {
  return apiRequest<ActivoResponse>('/api/activos', { method: 'POST', body: request })
}

export function editarActivo(id: number, request: ActivoRequest) {
  return apiRequest<ActivoResponse>(`/api/activos/${id}`, { method: 'PUT', body: request })
}

export function obtenerHistorialActivo(id: number) {
  return apiRequest<HistorialResponse[]>(`/api/activos/${id}/historial`)
}

export function listarTiposActivo() {
  return apiRequest<TipoActivoResponse[]>('/api/catalogo/tipos-activo')
}

export function listarUbicaciones() {
  return apiRequest<UbicacionResponse[]>('/api/catalogo/ubicaciones')
}
