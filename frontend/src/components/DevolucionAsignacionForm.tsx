import { useState, type FormEvent } from 'react'
import type { AsignacionResponse, DevolucionAsignacionRequest } from '../api/asignaciones'
import type { CondicionActivo } from '../api/activos'
import { ApiError } from '../api/client'
import { Icon } from './Icon'

export function DevolucionAsignacionForm({ asignacion, onSubmit, onCancel }: {
  asignacion: AsignacionResponse
  onSubmit: (request: DevolucionAsignacionRequest) => Promise<void>
  onCancel: () => void
}) {
  const [condicion, setCondicion] = useState<CondicionActivo>(asignacion.condicionFisica)
  const [estado, setEstado] = useState<'DISPONIBLE' | 'EN_MANTENIMIENTO'>('DISPONIBLE')
  const [observaciones, setObservaciones] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const changeCondition = (value: CondicionActivo) => {
    setCondicion(value)
    if (value === 'MALO') setEstado('EN_MANTENIMIENTO')
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (condicion === 'MALO' && estado === 'DISPONIBLE') {
      return setError('Un activo en mala condición debe pasar a mantenimiento.')
    }
    setLoading(true)
    try {
      await onSubmit({ condicionFisica: condicion, estadoActivo: estado, observaciones: observaciones.trim() || null })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible registrar la devolución.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={submit}>
      {error && <div className="form-alert" role="alert"><Icon name="alert" size={18} />{error}</div>}
      <div className="form-note">Revisa físicamente el activo <strong>{asignacion.codigoPatrimonial}</strong> antes de confirmar.</div>
      <div className="form-grid two-columns">
        <label className="field"><span>Condición física</span><select aria-label="Condición física" value={condicion} onChange={(event) => changeCondition(event.target.value as CondicionActivo)}><option value="BUENO">Bueno</option><option value="REGULAR">Regular</option><option value="MALO">Malo</option></select></label>
        <label className="field"><span>Estado posterior</span><select aria-label="Estado posterior" value={estado} onChange={(event) => setEstado(event.target.value as 'DISPONIBLE' | 'EN_MANTENIMIENTO')}><option value="DISPONIBLE">Disponible</option><option value="EN_MANTENIMIENTO">En mantenimiento</option></select></label>
      </div>
      <label className="field"><span>Observaciones de devolución</span><textarea value={observaciones} onChange={(event) => setObservaciones(event.target.value)} rows={3} maxLength={2000} /></label>
      <div className="modal-actions form-actions"><button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button><button type="submit" className="button primary" disabled={loading}>{loading ? 'Registrando…' : 'Confirmar devolución'}</button></div>
    </form>
  )
}
