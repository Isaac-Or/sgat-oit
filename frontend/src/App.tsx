import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAdmin, RequireAuth } from './auth/RouteGuards'
import { AppShell } from './components/AppShell'
import { AccessDeniedPage } from './pages/AccessDeniedPage'
import { DashboardPage } from './pages/DashboardPage'
import { InventarioPage } from './pages/InventarioPage'
import { LoginPage } from './pages/LoginPage'
import { SolicitudesPage } from './pages/SolicitudesPage'
import { UsuariosPage } from './pages/UsuariosPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="inventario" element={<InventarioPage />} />
          <Route path="solicitudes" element={<SolicitudesPage />} />
          <Route path="acceso-denegado" element={<AccessDeniedPage />} />
          <Route element={<RequireAdmin />}>
            <Route path="usuarios" element={<UsuariosPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Route>
    </Routes>
  )
}
