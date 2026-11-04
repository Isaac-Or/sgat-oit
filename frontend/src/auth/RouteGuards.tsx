import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function RequireAuth() {
  const { session } = useAuth()
  const location = useLocation()

  if (!session) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

export function RequireAdmin() {
  const { hasRole } = useAuth()
  if (!hasRole('ROLE_ADMIN')) return <Navigate to="/acceso-denegado" replace />
  return <Outlet />
}
