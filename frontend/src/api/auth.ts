import { apiRequest } from './client'

export interface LoginRequest {
  correo: string
  password: string
}

export interface LoginResponse {
  token: string
  tipo: string
  correo: string
  roles: string[]
}

export function iniciarSesion(request: LoginRequest) {
  return apiRequest<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: request,
    authenticated: false,
  })
}
