import { useCallback, useEffect, useState } from 'react'
import {
  crearActivo, editarActivo, ESTADOS_ACTIVO, listarActivos, listarTiposActivo,
  listarUbicaciones, obtenerHistorialActivo, type ActivoRequest, type ActivoResponse,
  type EstadoActivo, type HistorialResponse, type TipoActivoResponse, type UbicacionResponse,
} from '../api/activos'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { ActivoForm } from '../components/ActivoForm'
import { EmptyState, InlineError, LoadingState, Toast } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { etiquetaCodigo, fechaHora } from '../utils/format'

export function InventarioPage() {
  const { hasRole } = useAuth()
  const admin = hasRole('ROLE_ADMIN')
  const veTodosLosEstados = admin || hasRole('ROLE_TECNICO')
  const [estado, setEstado] = useState<EstadoActivo | ''>('')
  const [activos, setActivos] = useState<ActivoResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [formActivo, setFormActivo] = useState<ActivoResponse | null | undefined>()
  const [tipos, setTipos] = useState<TipoActivoResponse[]>([])
  const [ubicaciones, setUbicaciones] = useState<UbicacionResponse[]>([])
  const [catalogLoading, setCatalogLoading] = useState(false)
  const [historialActivo, setHistorialActivo] = useState<ActivoResponse | null>(null)
  const [historial, setHistorial] = useState<HistorialResponse[]>([])
  const [historialLoading, setHistorialLoading] = useState(false)
  const [historialError, setHistorialError] = useState('')
  const [toast, setToast] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setActivos(await listarActivos(veTodosLosEstados ? estado : 'DISPONIBLE'))
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible consultar el inventario.')
    } finally {
      setLoading(false)
    }
  }, [estado, veTodosLosEstados])

  useEffect(() => { void load() }, [load])
  useEffect(() => {
    if (!toast) return
    const timeout = window.setTimeout(() => setToast(''), 3500)
    return () => window.clearTimeout(timeout)
  }, [toast])

  const openForm = async (activo: ActivoResponse | null) => {
    setCatalogLoading(true)
    setError('')
    try {
      const [nextTipos, nextUbicaciones] = await Promise.all([listarTiposActivo(), listarUbicaciones()])
      setTipos(nextTipos)
      setUbicaciones(nextUbicaciones)
      setFormActivo(activo)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible cargar los catálogos.')
    } finally {
      setCatalogLoading(false)
    }
  }

  const save = async (request: ActivoRequest) => {
    if (formActivo) {
      await editarActivo(formActivo.id, request)
      setToast('El activo fue actualizado correctamente.')
    } else {
      await crearActivo(request)
      setToast('El activo fue registrado correctamente.')
    }
    setFormActivo(undefined)
    await load()
  }

  const openHistory = async (activo: ActivoResponse) => {
    setHistorialActivo(activo)
    setHistorialLoading(true)
    setHistorialError('')
    try {
      setHistorial(await obtenerHistorialActivo(activo.id))
    } catch (reason) {
      setHistorialError(reason instanceof ApiError ? reason.message : 'No fue posible consultar el historial.')
    } finally {
      setHistorialLoading(false)
    }
  }

  return (
    <div className="page-stack inventory-page">
      <section className="page-intro">
        <div><span className="eyebrow">Inventario institucional</span><h2>Activos tecnológicos</h2><p>Consulta la disponibilidad, ubicación y condición de los equipos.</p></div>
        {admin && <button className="button primary" onClick={() => void openForm(null)} disabled={catalogLoading}><Icon name="plus" size={18} /> Registrar activo</button>}
      </section>

      <section className="data-panel">
        <div className="data-toolbar responsive-toolbar">
          <div><strong>Inventario vigente</strong><span>{activos.length} activo{activos.length === 1 ? '' : 's'}</span></div>
          {veTodosLosEstados && (
            <label className="compact-filter"><span>Filtrar por estado</span>
              <select aria-label="Filtrar por estado" value={estado} onChange={(event) => setEstado(event.target.value as EstadoActivo | '')}>
                <option value="">Todos los estados</option>
                {ESTADOS_ACTIVO.map((item) => <option key={item} value={item}>{etiquetaCodigo(item)}</option>)}
              </select>
            </label>
          )}
        </div>
        {error && <InlineError message={error} onRetry={() => void load()} />}
        {(loading || catalogLoading) && <LoadingState label={catalogLoading ? 'Cargando catálogos…' : 'Consultando inventario…'} />}
        {!loading && !error && activos.length === 0 && <EmptyState title="No hay activos para mostrar" description="No existen equipos que coincidan con el estado seleccionado." />}
        {!loading && !error && activos.length > 0 && (
          <div className="table-scroll"><table className="users-table assets-table">
            <thead><tr><th>Activo</th><th>Tipo</th><th>Ubicación</th><th>Estado</th><th>Condición</th><th><span className="sr-only">Acciones</span></th></tr></thead>
            <tbody>{activos.map((activo) => <tr key={activo.id}>
              <td><div className="asset-main"><strong>{activo.codigoPatrimonial}</strong><span>{activo.marca} {activo.modelo}</span><small>Serie: {activo.numeroSerie || 'No registrada'}</small></div></td>
              <td><strong className="cell-primary">Tipo: {activo.tipoActivo}</strong><span className="cell-secondary">Categoría: {activo.categoria}</span></td>
              <td>{activo.ubicacion}</td>
              <td><div className="asset-status-stack">
                <span className={`status-badge status-${activo.estado.toLocaleLowerCase().replaceAll('_', '-')}`}><span />{etiquetaCodigo(activo.estado)}</span>
                {activo.reservado && <span className="reservation-badge">Reservado</span>}
              </div></td>
              <td>{etiquetaCodigo(activo.condicionFisica)}</td>
              <td><div className="row-actions">
                <button className="icon-button bordered" aria-label={`Ver historial de ${activo.codigoPatrimonial}`} title="Ver historial" onClick={() => void openHistory(activo)}><Icon name="history" /></button>
                {admin && <button className="icon-button bordered" aria-label={`Editar ${activo.codigoPatrimonial}`} title="Editar activo" onClick={() => void openForm(activo)}><Icon name="edit" /></button>}
              </div></td>
            </tr>)}</tbody>
          </table></div>
        )}
      </section>

      {formActivo !== undefined && (
        <Modal wide title={formActivo ? 'Editar activo' : 'Registrar activo'} eyebrow="Inventario tecnológico" onClose={() => setFormActivo(undefined)}>
          <ActivoForm activo={formActivo ?? undefined} tipos={tipos} ubicaciones={ubicaciones} onSubmit={save} onCancel={() => setFormActivo(undefined)} />
        </Modal>
      )}

      {historialActivo && (
        <Modal title={`Historial · ${historialActivo.codigoPatrimonial}`} eyebrow="Trazabilidad de estados" onClose={() => setHistorialActivo(null)}>
          <div className="history-body">
            {historialLoading && <LoadingState label="Consultando historial…" />}
            {historialError && <InlineError message={historialError} onRetry={() => void openHistory(historialActivo)} />}
            {!historialLoading && !historialError && historial.length === 0 && <EmptyState title="Sin cambios registrados" description="Este activo todavía no cuenta con eventos de estado." />}
            {!historialLoading && !historialError && historial.map((item) => (
              <article className="timeline-item" key={item.id}><div className="timeline-dot" /><div>
                <strong>{item.estadoAnterior ? `${etiquetaCodigo(item.estadoAnterior)} → ` : ''}{etiquetaCodigo(item.estadoNuevo)}</strong>
                <span>{fechaHora(item.fechaCambio)} · {item.usuarioResponsable}</span>
                {item.motivo && <p>{item.motivo}</p>}
              </div></article>
            ))}
          </div>
        </Modal>
      )}
      {toast && <Toast message={toast} onClose={() => setToast('')} />}
    </div>
  )
}
