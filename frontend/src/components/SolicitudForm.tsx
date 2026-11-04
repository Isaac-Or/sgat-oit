import { useState, type FormEvent } from 'react'
import type { TipoActivoResponse } from '../api/activos'
import { ApiError } from '../api/client'
import type { SolicitudRequest } from '../api/solicitudes'
import { Icon } from './Icon'

interface DetailDraft {
  key: number
  tipoActivoId: number
  observaciones: string
}

export function SolicitudForm({ tipos, onSubmit, onCancel }: {
  tipos: TipoActivoResponse[]
  onSubmit: (request: SolicitudRequest) => Promise<void>
  onCancel: () => void
}) {
  const [motivo, setMotivo] = useState('')
  const [detalles, setDetalles] = useState<DetailDraft[]>([{ key: 1, tipoActivoId: 0, observaciones: '' }])
  const [nextKey, setNextKey] = useState(2)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const updateDetail = (key: number, field: 'tipoActivoId' | 'observaciones', value: number | string) => {
    setDetalles((current) => current.map((item) => item.key === key ? { ...item, [field]: value } : item))
  }

  const addDetail = () => {
    setDetalles((current) => [...current, { key: nextKey, tipoActivoId: 0, observaciones: '' }])
    setNextKey((value) => value + 1)
  }

  const removeDetail = (key: number) => setDetalles((current) => current.filter((item) => item.key !== key))

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    if (!motivo.trim()) return setError('Indica el motivo de la solicitud.')
    if (detalles.some((item) => !item.tipoActivoId)) return setError('Selecciona el tipo de activo en cada detalle.')
    const typeIds = detalles.map((item) => item.tipoActivoId)
    if (new Set(typeIds).size !== typeIds.length) return setError('No repitas el mismo tipo de activo.')
    setLoading(true)
    try {
      await onSubmit({
        motivo: motivo.trim(),
        detalles: detalles.map((item) => ({
          tipoActivoId: item.tipoActivoId,
          observaciones: item.observaciones.trim() || null,
        })),
      })
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible registrar la solicitud.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={submit}>
      {error && <div className="form-alert" role="alert"><Icon name="alert" size={18} />{error}</div>}
      <label className="field"><span>Motivo</span>
        <textarea value={motivo} onChange={(event) => setMotivo(event.target.value)} rows={3} placeholder="Describe la necesidad institucional" />
      </label>
      <div className="detail-heading"><div><strong>Equipos solicitados</strong><span>Agrega uno o más tipos de activo.</span></div><button className="button secondary small" type="button" onClick={addDetail}><Icon name="plus" size={15} /> Agregar detalle</button></div>
      <div className="request-detail-list">
        {detalles.map((detalle, index) => (
          <div className="request-detail-editor" key={detalle.key}>
            <div className="detail-number">{index + 1}</div>
            <label className="field"><span>Tipo de activo</span>
              <select value={detalle.tipoActivoId} onChange={(event) => updateDetail(detalle.key, 'tipoActivoId', Number(event.target.value))}>
                <option value={0}>Seleccionar tipo</option>
                {tipos.map((tipo) => <option key={tipo.id} value={tipo.id}>{tipo.categoria} · {tipo.nombre}</option>)}
              </select>
            </label>
            <label className="field"><span>Observaciones</span>
              <input value={detalle.observaciones} onChange={(event) => updateDetail(detalle.key, 'observaciones', event.target.value)} placeholder="Opcional" />
            </label>
            <button type="button" className="icon-button danger-text" aria-label={`Quitar detalle ${index + 1}`} onClick={() => removeDetail(detalle.key)} disabled={detalles.length === 1}><Icon name="trash" /></button>
          </div>
        ))}
      </div>
      <div className="modal-actions form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button>
        <button type="submit" className="button primary" disabled={loading}>{loading ? 'Registrando…' : 'Registrar solicitud'}</button>
      </div>
    </form>
  )
}
