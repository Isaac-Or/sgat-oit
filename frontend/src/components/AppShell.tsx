import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Brand } from './Brand'
import { Icon, type IconName } from './Icon'

interface NavItem {
  label: string
  to: string
  icon: IconName
  adminOnly?: boolean
  roles?: string[]
}

const navItems: NavItem[] = [
  { label: 'Inicio', to: '/', icon: 'dashboard' },
  { label: 'Inventario', to: '/inventario', icon: 'inventory' },
  { label: 'Solicitudes', to: '/solicitudes', icon: 'requests', roles: ['ROLE_ADMIN', 'ROLE_SOLICITANTE'] },
  { label: 'Usuarios', to: '/usuarios', icon: 'users', adminOnly: true },
]

const routeTitles: Record<string, string> = {
  '/': 'Panel de inicio',
  '/inventario': 'Inventario tecnológico',
  '/solicitudes': 'Solicitudes',
  '/usuarios': 'Administración de usuarios',
  '/acceso-denegado': 'Acceso restringido',
}

export function AppShell() {
  const { session, logout, hasRole } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const visibleItems = navItems.filter((item) => {
    if (item.adminOnly && !hasRole('ROLE_ADMIN')) return false
    return !item.roles || item.roles.some(hasRole)
  })

  return (
    <div className="app-layout">
      <button
        className={`sidebar-backdrop ${menuOpen ? 'visible' : ''}`}
        aria-label="Cerrar navegación"
        onClick={() => setMenuOpen(false)}
      />
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="sidebar-head">
          <Brand />
          <button className="icon-button sidebar-close" onClick={() => setMenuOpen(false)} aria-label="Cerrar menú">
            <Icon name="close" />
          </button>
        </div>
        <div className="sidebar-section-label">Navegación</div>
        <nav className="main-nav" aria-label="Navegación principal">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) => isActive ? 'active' : undefined}
            >
              <Icon name={item.icon} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="environment-pill"><span /> Entorno local</div>
          <small>SGAT-OIT · Versión inicial</small>
        </div>
      </aside>

      <div className="app-main">
        <header className="topbar">
          <div className="topbar-title">
            <button className="icon-button menu-button" onClick={() => setMenuOpen(true)} aria-label="Abrir menú">
              <Icon name="menu" />
            </button>
            <div>
              <span>Sistema de Gestión de Activos Tecnológicos</span>
              <h1>{routeTitles[location.pathname] ?? 'SGAT-OIT'}</h1>
            </div>
          </div>
          <div className="account-area">
            <div className="account-avatar" aria-hidden="true">
              {session?.correo.slice(0, 1).toUpperCase()}
            </div>
            <div className="account-copy">
              <strong>{session?.correo}</strong>
              <span>{hasRole('ROLE_ADMIN') ? 'Administrador' : 'Usuario'}</span>
            </div>
            <button className="logout-button" onClick={logout} title="Cerrar sesión">
              <Icon name="logout" />
              <span>Cerrar sesión</span>
            </button>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
