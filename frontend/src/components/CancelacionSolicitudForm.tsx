import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { CancelarSolicitudRequest } from '../api/solicitudes'
import { Icon } from './Icon'

export function CancelacionSolicitudForm({ onSubmit, onCancel }: {
  onSubmit: (request: CancelarSolicitudRequest) => Promise<void>
  onCancel: () => void
}) {
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!motivo.trim()) {
      setError('Indica el motivo de la cancelación.')
      return
    }
    setLoading(true)
    try {
      await onSubmit({ motivo: motivo.trim() })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible cancelar la solicitud.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={submit}>
      {error && <div className="form-alert" role="alert"><Icon name="alert" size={18} />{error}</div>}
      <div className="form-note">
        La cancelación liberará las reservas. No registra una devolución ni modifica el estado físico de los activos.
      </div>
      <label className="field"><span>Motivo de la cancelación</span>
        <textarea
          value={motivo}
          onChange={(event) => setMotivo(event.target.value)}
          rows={3}
          maxLength={1000}
        />
      </label>
      <div className="modal-actions form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Volver</button>
        <button type="submit" className="button danger" disabled={loading}>
          {loading ? 'Cancelando…' : 'Cancelar solicitud y liberar reservas'}
        </button>
      </div>
    </form>
  )
}
