import { useState, type FormEvent } from 'react'
import { ApiError, type ErrorFields } from '../api/client'
import {
  ROLES, type CrearUsuarioRequest, type EditarUsuarioRequest,
  type RolCodigo, type Usuario,
} from '../api/usuarios'

const roleLabels: Record<RolCodigo, { title: string; description: string }> = {
  ROLE_ADMIN: { title: 'Administrador', description: 'Gestiona usuarios y operaciones administrativas.' },
  ROLE_TECNICO: { title: 'Técnico', description: 'Gestiona activos y atención técnica.' },
  ROLE_SOLICITANTE: { title: 'Solicitante', description: 'Registra y consulta sus solicitudes.' },
}

interface UsuarioFormProps {
  usuario?: Usuario
  onSubmit: (request: CrearUsuarioRequest | EditarUsuarioRequest) => Promise<void>
  onCancel: () => void
}

export function UsuarioForm({ usuario, onSubmit, onCancel }: UsuarioFormProps) {
  const editing = Boolean(usuario)
  const [nombres, setNombres] = useState(usuario?.nombres ?? '')
  const [apellidos, setApellidos] = useState(usuario?.apellidos ?? '')
  const [correo, setCorreo] = useState(usuario?.correoInstitucional ?? '')
  const [password, setPassword] = useState('')
  const [roles, setRoles] = useState<RolCodigo[]>(
    (usuario?.roles.filter((role): role is RolCodigo => ROLES.includes(role as RolCodigo)) ?? []),
  )
  const [errors, setErrors] = useState<ErrorFields>({})
  const [generalError, setGeneralError] = useState('')
  const [loading, setLoading] = useState(false)

  const toggleRole = (role: RolCodigo) => {
    setRoles((current) => current.includes(role)
      ? current.filter((item) => item !== role)
      : [...current, role])
  }

  const validate = () => {
    const next: ErrorFields = {}
    if (!nombres.trim()) next.nombres = 'Ingresa los nombres.'
    if (!apellidos.trim()) next.apellidos = 'Ingresa los apellidos.'
    if (!/^\S+@\S+\.\S+$/.test(correo.trim())) next.correoInstitucional = 'Ingresa un correo válido.'
    if (!editing && (password.length < 12 || password.length > 72)) {
      next.password = 'La contraseña debe tener entre 12 y 72 caracteres.'
    }
    if (roles.length === 0) next.roles = 'Selecciona al menos un rol.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setGeneralError('')
    if (!validate()) return
    setLoading(true)
    try {
      const common: EditarUsuarioRequest = {
        nombres: nombres.trim(),
        apellidos: apellidos.trim(),
        correoInstitucional: correo.trim(),
        roles,
      }
      await onSubmit(editing ? common : { ...common, password })
    } catch (reason) {
      if (reason instanceof ApiError) {
        setGeneralError(reason.message)
        setErrors(reason.fields)
      } else {
        setGeneralError('No fue posible guardar el usuario.')
      }
    } finally {
      setLoading(false)
    }
  }

  const fieldError = (name: string) => errors[name] || errors[`${name}[]`]

  return (
    <form onSubmit={submit} className="user-form">
      {generalError && <div className="form-alert" role="alert">{generalError}</div>}
      <div className="form-grid">
        <label className="field">
          <span>Nombres</span>
          <input value={nombres} onChange={(e) => setNombres(e.target.value)} maxLength={100} autoFocus />
          {fieldError('nombres') && <small className="field-error">{fieldError('nombres')}</small>}
        </label>
        <label className="field">
          <span>Apellidos</span>
          <input value={apellidos} onChange={(e) => setApellidos(e.target.value)} maxLength={150} />
          {fieldError('apellidos') && <small className="field-error">{fieldError('apellidos')}</small>}
        </label>
      </div>
      <label className="field">
        <span>Correo institucional</span>
        <input type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} maxLength={254} />
        {fieldError('correoInstitucional') && <small className="field-error">{fieldError('correoInstitucional')}</small>}
      </label>
      {!editing && (
        <label className="field">
          <span>Contraseña temporal</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={12}
            maxLength={72}
            autoComplete="new-password"
          />
          <small>Entre 12 y 72 caracteres. No se mostrará ni almacenará en el navegador.</small>
          {fieldError('password') && <small className="field-error">{fieldError('password')}</small>}
        </label>
      )}
      <fieldset className="roles-fieldset">
        <legend>Roles asignados</legend>
        <div className="role-options">
          {ROLES.map((role) => (
            <label className={`role-option ${roles.includes(role) ? 'selected' : ''}`} key={role}>
              <input type="checkbox" checked={roles.includes(role)} onChange={() => toggleRole(role)} />
              <span className="role-check" aria-hidden="true" />
              <span><strong>{roleLabels[role].title}</strong><small>{roleLabels[role].description}</small></span>
            </label>
          ))}
        </div>
        {fieldError('roles') && <small className="field-error">{fieldError('roles')}</small>}
      </fieldset>
      <div className="modal-actions form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button>
        <button type="submit" className="button primary" disabled={loading}>
          {loading ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear usuario'}
        </button>
      </div>
    </form>
  )
}
