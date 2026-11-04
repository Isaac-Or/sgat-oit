import { apiRequest } from './client'

export const ROLES = ['ROLE_ADMIN', 'ROLE_TECNICO', 'ROLE_SOLICITANTE'] as const
export type RolCodigo = (typeof ROLES)[number]

export interface Usuario {
  id: number
  nombres: string
  apellidos: string
  correoInstitucional: string
  habilitado: boolean
  roles: string[]
}

export interface Pagina<T> {
  contenido: T[]
  pagina: number
  tamanio: number
  totalElementos: number
  totalPaginas: number
  primera: boolean
  ultima: boolean
}

export interface CrearUsuarioRequest {
  nombres: string
  apellidos: string
  correoInstitucional: string
  password: string
  roles: RolCodigo[]
}

export interface EditarUsuarioRequest {
  nombres: string
  apellidos: string
  correoInstitucional: string
  roles: RolCodigo[]
}

export function listarUsuarios(page = 0, size = 20) {
  return apiRequest<Pagina<Usuario>>(`/api/usuarios?page=${page}&size=${size}`)
}

export function obtenerUsuario(id: number) {
  return apiRequest<Usuario>(`/api/usuarios/${id}`)
}

export function crearUsuario(request: CrearUsuarioRequest) {
  return apiRequest<Usuario>('/api/usuarios', { method: 'POST', body: request })
}

export function editarUsuario(id: number, request: EditarUsuarioRequest) {
  return apiRequest<Usuario>(`/api/usuarios/${id}`, { method: 'PUT', body: request })
}

export function cambiarHabilitacion(id: number, habilitado: boolean) {
  return apiRequest<Usuario>(`/api/usuarios/${id}/habilitacion`, {
    method: 'PUT',
    body: { habilitado },
  })
}
