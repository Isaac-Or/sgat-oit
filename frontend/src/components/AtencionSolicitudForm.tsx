import { useState, type FormEvent } from 'react'
import type { ActivoResponse } from '../api/activos'
import { ApiError } from '../api/client'
import type { AtencionRequest, SolicitudResponse } from '../api/solicitudes'
import { Icon } from './Icon'

export function AtencionSolicitudForm({ solicitud, activos, onSubmit, onCancel }: {
  solicitud: SolicitudResponse
  activos: ActivoResponse[]
  onSubmit: (request: AtencionRequest) => Promise<void>
  onCancel: () => void
}) {
  const [aprobada, setAprobada] = useState(true)
  const [observaciones, setObservaciones] = useState('')
  const [selecciones, setSelecciones] = useState<Record<number, number>>({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!aprobada && !observaciones.trim()) return setError('Indica el motivo del rechazo.')
    if (aprobada && solicitud.detalles.some((detalle) => !selecciones[detalle.id])) {
      return setError('Selecciona un activo disponible para cada detalle.')
    }
    const selectedIds = Object.values(selecciones)
    if (aprobada && new Set(selectedIds).size !== selectedIds.length) return setError('No puedes asignar el mismo activo dos veces.')
    setLoading(true)
    try {
      await onSubmit({
        aprobada,
        observaciones: observaciones.trim() || null,
        activos: aprobada
          ? solicitud.detalles.map((detalle) => ({ detalleId: detalle.id, activoId: selecciones[detalle.id] }))
          : null,
      })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible atender la solicitud.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={submit}>
      {error && <div className="form-alert" role="alert"><Icon name="alert" size={18} />{error}</div>}
      <div className="decision-toggle" role="group" aria-label="Decisión de atención">
        <button type="button" className={aprobada ? 'active approve' : ''} onClick={() => setAprobada(true)}><Icon name="approve" /> Aprobar</button>
        <button type="button" className={!aprobada ? 'active reject' : ''} onClick={() => setAprobada(false)}><Icon name="reject" /> Rechazar</button>
      </div>
      {aprobada && <div className="form-note">Selecciona un equipo disponible que corresponda al tipo solicitado. El servidor verificará la correspondencia definitiva.</div>}
      {aprobada && solicitud.detalles.map((detalle) => {
        const compatibles = activos.filter((activo) =>
          activo.tipoActivoId === detalle.tipoActivoId
          && activo.estado === 'DISPONIBLE'
          && !activo.reservado)
        return (
          <label className="field" key={detalle.id}><span>{detalle.tipoActivo}</span>
            <select value={selecciones[detalle.id] ?? 0} onChange={(event) => setSelecciones((current) => ({ ...current, [detalle.id]: Number(event.target.value) }))}>
              <option value={0}>{compatibles.length ? 'Seleccionar activo' : 'Sin activos compatibles disponibles'}</option>
              {compatibles.map((activo) => <option key={activo.id} value={activo.id}>{activo.codigoPatrimonial} · {activo.marca} {activo.modelo} · {activo.ubicacion}</option>)}
            </select>
            {detalle.observaciones && <small>Observación: {detalle.observaciones}</small>}
          </label>
        )
      })}
      <label className="field"><span>{aprobada ? 'Observaciones de atención' : 'Motivo del rechazo'}</span>
        <textarea value={observaciones} onChange={(event) => setObservaciones(event.target.value)} rows={3} />
      </label>
      <div className="modal-actions form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button>
        <button type="submit" className={`button ${aprobada ? 'primary' : 'danger'}`} disabled={loading}>{loading ? 'Procesando…' : aprobada ? 'Confirmar aprobación' : 'Confirmar rechazo'}</button>
      </div>
    </form>
  )
}
