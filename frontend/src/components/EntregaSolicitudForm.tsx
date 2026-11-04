import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { EntregaSolicitudRequest, SolicitudResponse } from '../api/solicitudes'
import { Icon } from './Icon'

export function EntregaSolicitudForm({ solicitud, onSubmit, onCancel }: {
  solicitud: SolicitudResponse
  onSubmit: (request: EntregaSolicitudRequest) => Promise<void>
  onCancel: () => void
}) {
  const [fechaEstimada, setFechaEstimada] = useState('')
  const [observaciones, setObservaciones] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!fechaEstimada) return setError('Indica la fecha estimada de devolución.')
    const fecha = new Date(fechaEstimada)
    if (Number.isNaN(fecha.getTime()) || fecha.getTime() <= Date.now()) {
      return setError('La fecha estimada debe ser posterior al momento de la entrega.')
    }
    setLoading(true)
    try {
      await onSubmit({
        fechaDevolucionEstimada: fecha.toISOString(),
        observaciones: observaciones.trim() || null,
      })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible registrar la entrega.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={submit}>
      {error && <div className="form-alert" role="alert"><Icon name="alert" size={18} />{error}</div>}
      <div className="form-note">
        Se entregarán de forma atómica los {solicitud.detalles.length} activo{solicitud.detalles.length === 1 ? '' : 's'} reservado{solicitud.detalles.length === 1 ? '' : 's'} a <strong>{solicitud.solicitante}</strong>.
      </div>
      <label className="field"><span>Fecha estimada de devolución</span>
        <input aria-label="Fecha estimada de devolución" type="datetime-local" value={fechaEstimada} onChange={(event) => setFechaEstimada(event.target.value)} required />
      </label>
      <label className="field"><span>Observaciones de entrega</span>
        <textarea value={observaciones} onChange={(event) => setObservaciones(event.target.value)} rows={3} maxLength={2000} />
      </label>
      <div className="modal-actions form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button>
        <button type="submit" className="button primary" disabled={loading}>{loading ? 'Registrando…' : 'Confirmar entrega'}</button>
      </div>
    </form>
  )
}
