import { useState, type FormEvent } from 'react'
import { ApiError, type ErrorFields } from '../api/client'
import {
  CONDICIONES_ACTIVO, ESTADOS_ACTIVO, type ActivoRequest, type ActivoResponse,
  type CondicionActivo, type EstadoActivo, type TipoActivoResponse, type UbicacionResponse,
} from '../api/activos'
import { etiquetaCodigo } from '../utils/format'

interface ActivoFormProps {
  activo?: ActivoResponse
  tipos: TipoActivoResponse[]
  ubicaciones: UbicacionResponse[]
  onSubmit: (request: ActivoRequest) => Promise<void>
  onCancel: () => void
}

export function ActivoForm({ activo, tipos, ubicaciones, onSubmit, onCancel }: ActivoFormProps) {
  const [tipoActivoId, setTipoActivoId] = useState(activo?.tipoActivoId ?? 0)
  const [ubicacionId, setUbicacionId] = useState(activo?.ubicacionId ?? 0)
  const [codigo, setCodigo] = useState(activo?.codigoPatrimonial ?? '')
  const [serie, setSerie] = useState(activo?.numeroSerie ?? '')
  const [marca, setMarca] = useState(activo?.marca ?? '')
  const [modelo, setModelo] = useState(activo?.modelo ?? '')
  const [descripcion, setDescripcion] = useState(activo?.descripcion ?? '')
  const [estado, setEstado] = useState<EstadoActivo>(activo?.estado ?? 'DISPONIBLE')
  const [condicion, setCondicion] = useState<CondicionActivo>(activo?.condicionFisica ?? 'BUENO')
  const [errors, setErrors] = useState<ErrorFields>({})
  const [generalError, setGeneralError] = useState('')
  const [loading, setLoading] = useState(false)

  const validate = () => {
    const next: ErrorFields = {}
    if (!tipoActivoId) next.tipoActivoId = 'Selecciona un tipo de activo.'
    if (!ubicacionId) next.ubicacionId = 'Selecciona una ubicación.'
    if (!codigo.trim()) next.codigoPatrimonial = 'Ingresa el código patrimonial.'
    if (!marca.trim()) next.marca = 'Ingresa la marca.'
    if (!modelo.trim()) next.modelo = 'Ingresa el modelo.'
    setErrors(next)
    return Object.keys(next).length === 0
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setGeneralError('')
    if (!validate()) return
    setLoading(true)
    try {
      await onSubmit({
        tipoActivoId,
        ubicacionId,
        codigoPatrimonial: codigo.trim(),
        numeroSerie: serie.trim() || null,
        marca: marca.trim(),
        modelo: modelo.trim(),
        descripcion: activo ? descripcion.trim() : descripcion.trim() || null,
        estado,
        condicionFisica: condicion,
      })
    } catch (reason) {
      if (reason instanceof ApiError) {
        setGeneralError(reason.message)
        setErrors(reason.fields)
      } else setGeneralError('No fue posible guardar el activo.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="entity-form" onSubmit={submit}>
      {generalError && <div className="form-alert" role="alert">{generalError}</div>}
      <div className="form-grid">
        <label className="field"><span>Tipo de activo</span>
          <select value={tipoActivoId} onChange={(event) => setTipoActivoId(Number(event.target.value))}>
            <option value={0}>Seleccionar tipo</option>
            {tipos.map((tipo) => <option key={tipo.id} value={tipo.id}>{tipo.categoria} · {tipo.nombre}</option>)}
          </select>
          {errors.tipoActivoId && <small className="field-error">{errors.tipoActivoId}</small>}
        </label>
        <label className="field"><span>Ubicación</span>
          <select value={ubicacionId} onChange={(event) => setUbicacionId(Number(event.target.value))}>
            <option value={0}>Seleccionar ubicación</option>
            {ubicaciones.map((ubicacion) => <option key={ubicacion.id} value={ubicacion.id}>{ubicacion.codigo} · {ubicacion.nombre}</option>)}
          </select>
          {errors.ubicacionId && <small className="field-error">{errors.ubicacionId}</small>}
        </label>
        <label className="field"><span>Código patrimonial</span>
          <input value={codigo} onChange={(event) => setCodigo(event.target.value)} maxLength={50} />
          {errors.codigoPatrimonial && <small className="field-error">{errors.codigoPatrimonial}</small>}
        </label>
        <label className="field"><span>Número de serie</span>
          <input value={serie} onChange={(event) => setSerie(event.target.value)} maxLength={100} />
        </label>
        <label className="field"><span>Marca</span>
          <input value={marca} onChange={(event) => setMarca(event.target.value)} />
          {errors.marca && <small className="field-error">{errors.marca}</small>}
        </label>
        <label className="field"><span>Modelo</span>
          <input value={modelo} onChange={(event) => setModelo(event.target.value)} />
          {errors.modelo && <small className="field-error">{errors.modelo}</small>}
        </label>
        <label className="field"><span>Estado</span>
          <select value={estado} onChange={(event) => setEstado(event.target.value as EstadoActivo)}>
            {ESTADOS_ACTIVO.map((item) => <option key={item} value={item}>{etiquetaCodigo(item)}</option>)}
          </select>
        </label>
        <label className="field"><span>Condición física</span>
          <select value={condicion} onChange={(event) => setCondicion(event.target.value as CondicionActivo)}>
            {CONDICIONES_ACTIVO.map((item) => <option key={item} value={item}>{etiquetaCodigo(item)}</option>)}
          </select>
        </label>
      </div>
      <label className="field"><span>Descripción</span>
        <textarea value={descripcion} onChange={(event) => setDescripcion(event.target.value)} rows={3} />
      </label>
      <div className="modal-actions form-actions">
        <button type="button" className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button>
        <button type="submit" className="button primary" disabled={loading}>{loading ? 'Guardando…' : activo ? 'Guardar cambios' : 'Registrar activo'}</button>
      </div>
    </form>
  )
}
