import { Navigate, Route, Routes } from 'react-router-dom'
import { RequireAdmin, RequireAuth } from './auth/RouteGuards'
import { AppShell } from './components/AppShell'
import { AccessDeniedPage } from './pages/AccessDeniedPage'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { PlaceholderPage } from './pages/PlaceholderPage'
import { UsuariosPage } from './pages/UsuariosPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="inventario" element={<PlaceholderPage module="Inventario tecnológico" description="La interfaz de inventario se incorporará en una siguiente fase, conectada exclusivamente a los endpoints reales del backend." icon="inventory" />} />
          <Route path="solicitudes" element={<PlaceholderPage module="Solicitudes" description="La gestión de solicitudes queda preparada para su integración funcional en una siguiente fase." icon="requests" />} />
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
