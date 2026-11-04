import { useCallback, useEffect, useState } from 'react'
import { devolverAsignacion, listarAsignaciones, type AsignacionResponse, type DevolucionAsignacionRequest } from '../api/asignaciones'
import { ApiError } from '../api/client'
import { DevolucionAsignacionForm } from '../components/DevolucionAsignacionForm'
import { EmptyState, InlineError, LoadingState, Toast } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { etiquetaCodigo, fechaHora } from '../utils/format'

export function PrestamosPage() {
  const [soloAbiertas, setSoloAbiertas] = useState(true)
  const [asignaciones, setAsignaciones] = useState<AsignacionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [devolucion, setDevolucion] = useState<AsignacionResponse | null>(null)
  const [toast, setToast] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setAsignaciones(await listarAsignaciones(soloAbiertas))
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible consultar los préstamos.')
    } finally {
      setLoading(false)
    }
  }, [soloAbiertas])

  useEffect(() => { void load() }, [load])

  const saveReturn = async (request: DevolucionAsignacionRequest) => {
    if (!devolucion) return
    await devolverAsignacion(devolucion.id, request)
    setDevolucion(null)
    setToast('La devolución fue registrada correctamente.')
    await load()
  }

  return (
    <div className="page-stack loans-page">
      <section className="page-intro"><div><span className="eyebrow">Control operativo</span><h2>Préstamos de activos</h2><p>Consulta entregas, vencimientos y devoluciones registradas.</p></div></section>
      <section className="data-panel">
        <div className="data-toolbar responsive-toolbar">
          <div><strong>{soloAbiertas ? 'Préstamos activos' : 'Historial de préstamos'}</strong><span>{asignaciones.length} registro{asignaciones.length === 1 ? '' : 's'}</span></div>
          <label className="compact-filter"><span>Vista</span><select aria-label="Vista de préstamos" value={soloAbiertas ? 'abiertas' : 'todas'} onChange={(event) => setSoloAbiertas(event.target.value === 'abiertas')}><option value="abiertas">Préstamos activos</option><option value="todas">Historial completo</option></select></label>
        </div>
        {error && <InlineError message={error} onRetry={() => void load()} />}
        {loading && <LoadingState label="Consultando préstamos…" />}
        {!loading && !error && asignaciones.length === 0 && <EmptyState title="No hay préstamos para mostrar" description={soloAbiertas ? 'No existen activos pendientes de devolución.' : 'Todavía no se registraron entregas.'} />}
        {!loading && !error && asignaciones.length > 0 && <div className="table-scroll"><table className="users-table loans-table">
          <thead><tr><th>Activo</th><th>Receptor</th><th>Entrega</th><th>Devolución</th><th>Estado</th><th><span className="sr-only">Acciones</span></th></tr></thead>
          <tbody>{asignaciones.map((item) => <tr key={item.id}>
            <td><strong className="cell-main">{item.codigoPatrimonial}</strong><span className="cell-subtle">{item.tipoActivo} · SOL-{String(item.solicitudId).padStart(5, '0')}</span></td>
            <td><strong className="cell-main">{item.usuarioReceptor}</strong><span className="cell-subtle">Entregó: {item.entregadoPor}</span></td>
            <td><strong className="cell-main">{fechaHora(item.fechaEntrega)}</strong><span className="cell-subtle">Prevista: {fechaHora(item.fechaDevolucionEstimada)}</span></td>
            <td>{item.fechaDevolucionReal ? <><strong className="cell-main">{fechaHora(item.fechaDevolucionReal)}</strong><span className="cell-subtle">Recibió: {item.recibidoDevolucionPor}</span></> : <span className="cell-subtle">Pendiente</span>}</td>
            <td><span className={`status-badge ${item.abierta ? 'request-pendiente' : 'enabled'}`}><span />{item.abierta ? 'Activo' : 'Devuelto'}</span><span className="cell-subtle">{etiquetaCodigo(item.estadoActivo)} · {etiquetaCodigo(item.condicionFisica)}</span></td>
            <td><div className="row-actions">{item.abierta && <button className="button primary small" onClick={() => setDevolucion(item)}><Icon name="history" size={16} /> Registrar devolución</button>}</div></td>
          </tr>)}</tbody>
        </table></div>}
      </section>
      {devolucion && <Modal title={`Devolver ${devolucion.codigoPatrimonial}`} eyebrow="Recepción de activo" onClose={() => setDevolucion(null)}><DevolucionAsignacionForm asignacion={devolucion} onSubmit={saveReturn} onCancel={() => setDevolucion(null)} /></Modal>}
      {toast && <Toast message={toast} onClose={() => setToast('')} />}
    </div>
  )
}
