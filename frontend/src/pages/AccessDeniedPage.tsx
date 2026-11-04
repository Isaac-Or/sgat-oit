import { Link } from 'react-router-dom'
import { Icon } from '../components/Icon'

export function AccessDeniedPage() {
  return (
    <section className="access-denied">
      <div className="denied-icon"><Icon name="shield" size={32} /></div>
      <span className="eyebrow">HTTP 403</span>
      <h2>Acceso restringido</h2>
      <p>Tu cuenta no tiene el rol necesario para acceder a esta sección.</p>
      <Link className="button primary" to="/">Volver al inicio</Link>
    </section>
  )
}
