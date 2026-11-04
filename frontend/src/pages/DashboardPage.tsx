import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Icon, type IconName } from '../components/Icon'
import { etiquetaRol } from '../utils/roles'

const modules: Array<{ title: string; description: string; to: string; icon: IconName; admin?: boolean }> = [
  { title: 'Inventario', description: 'Consulta y gestión de activos tecnológicos.', to: '/inventario', icon: 'inventory' },
  { title: 'Solicitudes', description: 'Seguimiento de requerimientos institucionales.', to: '/solicitudes', icon: 'requests' },
  { title: 'Usuarios', description: 'Cuentas, roles y estados de acceso.', to: '/usuarios', icon: 'users', admin: true },
]

export function DashboardPage() {
  const { session, hasRole } = useAuth()
  const visibleModules = modules.filter((module) => !module.admin || hasRole('ROLE_ADMIN'))

  return (
    <div className="page-stack">
      <section className="welcome-panel">
        <div>
          <span className="eyebrow light">Sesión institucional activa</span>
          <h2>Bienvenido al centro de operaciones tecnológicas.</h2>
          <p>
            Accede a los módulos habilitados para tu cuenta y continúa con la gestión diaria.
          </p>
        </div>
        <div className="welcome-account">
          <span>Cuenta actual</span>
          <strong>{session?.correo}</strong>
          <div className="role-row">
            {session?.roles.map((role) => <span className="role-chip inverted" key={role}>{etiquetaRol(role)}</span>)}
          </div>
        </div>
      </section>

      <section>
        <div className="section-heading">
          <div><span className="eyebrow">Módulos</span><h2>Áreas de trabajo</h2></div>
          <p>Selecciona una opción para comenzar.</p>
        </div>
        <div className="module-grid">
          {visibleModules.map((module, index) => (
            <Link to={module.to} className="module-card" key={module.to}>
              <div className="module-index">0{index + 1}</div>
              <div className="module-icon"><Icon name={module.icon} size={24} /></div>
              <h3>{module.title}</h3>
              <p>{module.description}</p>
              <span className="module-link">Abrir módulo <Icon name="chevronRight" size={16} /></span>
            </Link>
          ))}
        </div>
      </section>

      <section className="phase-note">
        <div className="phase-line" />
        <div><span>Fase inicial</span><strong>Base operativa preparada</strong></div>
        <p>Los módulos de inventario y solicitudes se completarán en las siguientes fases.</p>
      </section>
    </div>
  )
}
