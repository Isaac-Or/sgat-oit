import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Brand } from '../components/Brand'
import { Icon } from '../components/Icon'

export function LoginPage() {
  const { session, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [correo, setCorreo] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const requestedPath = (location.state as { from?: string } | null)?.from

  if (session) return <Navigate to={requestedPath && requestedPath !== '/login' ? requestedPath : '/'} replace />

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login({ correo: correo.trim(), password })
      navigate(requestedPath && requestedPath !== '/login' ? requestedPath : '/', { replace: true })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible iniciar sesión.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="login-context" aria-label="Presentación de SGAT-OIT">
        <Brand />
        <div className="context-content">
          <span className="eyebrow light">Gestión tecnológica institucional</span>
          <h1>Control claro para cada activo tecnológico.</h1>
          <p>
            Un espacio seguro para organizar inventario, atender solicitudes y mantener
            la trazabilidad operativa de la OIT.
          </p>
        </div>
        <div className="context-grid" aria-hidden="true">
          <span /><span /><span /><span /><span /><span />
        </div>
        <small>Ministerio de Cultura · Oficina de Infraestructura Tecnológica</small>
      </section>

      <section className="login-panel">
        <div className="mobile-brand"><Brand /></div>
        <form className="login-card" onSubmit={submit}>
          <div className="login-heading">
            <span className="eyebrow">Acceso al sistema</span>
            <h2>Bienvenido a SGAT</h2>
            <p>Ingresa con tu cuenta institucional para continuar.</p>
          </div>

          {error && (
            <div className="form-alert" role="alert">
              <Icon name="alert" size={18} />
              <span>{error}</span>
            </div>
          )}

          <label className="field">
            <span>Correo institucional</span>
            <input
              type="email"
              autoComplete="username"
              value={correo}
              onChange={(event) => setCorreo(event.target.value)}
              placeholder="nombre@cultura.gob.pe"
              required
              autoFocus
            />
          </label>

          <label className="field">
            <span>Contraseña</span>
            <input
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Ingresa tu contraseña"
              required
            />
          </label>

          <button className="button primary login-submit" disabled={loading} type="submit">
            {loading ? <><span className="spinner small-spinner" /> Verificando…</> : 'Ingresar al sistema'}
          </button>

          <p className="security-note">
            <Icon name="shield" size={17} />
            La sesión se mantiene solo mientras esta ventana permanezca abierta.
          </p>
        </form>
        <small className="login-version">SGAT-OIT · Acceso institucional</small>
      </section>
    </main>
  )
}
