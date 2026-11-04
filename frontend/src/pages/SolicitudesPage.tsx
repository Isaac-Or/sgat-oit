import { useCallback, useEffect, useState } from 'react'
import { listarActivos, listarTiposActivo, type ActivoResponse, type TipoActivoResponse } from '../api/activos'
import { ApiError } from '../api/client'
import {
  crearSolicitud, ESTADOS_SOLICITUD, listarMisSolicitudes, listarSolicitudes,
  atenderSolicitud, cancelarSolicitud, type AtencionRequest, type CancelarSolicitudRequest,
  type EstadoSolicitud, type SolicitudRequest, type SolicitudResponse,
} from '../api/solicitudes'
import { useAuth } from '../auth/AuthContext'
import { AtencionSolicitudForm } from '../components/AtencionSolicitudForm'
import { CancelacionSolicitudForm } from '../components/CancelacionSolicitudForm'
import { EmptyState, InlineError, LoadingState, Toast } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { SolicitudForm } from '../components/SolicitudForm'
import { etiquetaCodigo, fechaHora } from '../utils/format'
import { AccessDeniedPage } from './AccessDeniedPage'

export function SolicitudesPage() {
  const { hasRole } = useAuth()
  const admin = hasRole('ROLE_ADMIN')
  const solicitante = hasRole('ROLE_SOLICITANTE')
  const [estado, setEstado] = useState<EstadoSolicitud | ''>('')
  const [solicitudes, setSolicitudes] = useState<SolicitudResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const [tipos, setTipos] = useState<TipoActivoResponse[]>([])
  const [catalogLoading, setCatalogLoading] = useState(false)
  const [detail, setDetail] = useState<SolicitudResponse | null>(null)
  const [attention, setAttention] = useState<SolicitudResponse | null>(null)
  const [cancellation, setCancellation] = useState<SolicitudResponse | null>(null)
  const [availableAssets, setAvailableAssets] = useState<ActivoResponse[]>([])
  const [attentionLoading, setAttentionLoading] = useState(false)
  const [toast, setToast] = useState('')

  const load = useCallback(async () => {
    if (!admin && !solicitante) return
    setLoading(true)
    setError('')
    try {
      setSolicitudes(admin ? await listarSolicitudes(estado) : await listarMisSolicitudes())
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible consultar las solicitudes.')
    } finally {
      setLoading(false)
    }
  }, [admin, solicitante, estado])

  useEffect(() => { void load() }, [load])

  if (!admin && !solicitante) return <AccessDeniedPage />

  const openCreate = async () => {
    setCatalogLoading(true)
    setError('')
    try {
      setTipos(await listarTiposActivo())
      setCreateOpen(true)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible cargar los tipos de activo.')
    } finally {
      setCatalogLoading(false)
    }
  }

  const saveRequest = async (request: SolicitudRequest) => {
    await crearSolicitud(request)
    setCreateOpen(false)
    setToast('La solicitud fue registrada correctamente.')
    await load()
  }

  const openAttention = async (request: SolicitudResponse) => {
    setAttentionLoading(true)
    setError('')
    try {
      setAvailableAssets(await listarActivos('DISPONIBLE'))
      setAttention(request)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible consultar los activos disponibles.')
    } finally {
      setAttentionLoading(false)
    }
  }

  const saveAttention = async (request: AtencionRequest) => {
    if (!attention) return
    const response = await atenderSolicitud(attention.id, request)
    setAttention(null)
    setToast(response.estado === 'APROBADA' ? 'La solicitud fue aprobada.' : 'La solicitud fue rechazada.')
    await load()
  }

  const saveCancellation = async (request: CancelarSolicitudRequest) => {
    if (!cancellation) return
    await cancelarSolicitud(cancellation.id, request)
    setCancellation(null)
    setToast('La solicitud fue cancelada y sus reservas quedaron liberadas.')
    await load()
  }

  return (
    <div className="page-stack requests-page">
      <section className="page-intro">
        <div><span className="eyebrow">Gestión de requerimientos</span><h2>{admin ? 'Bandeja de solicitudes' : 'Mis solicitudes'}</h2><p>{admin ? 'Revisa, aprueba o rechaza requerimientos pendientes.' : 'Solicita equipos y consulta el estado de tus requerimientos.'}</p></div>
        {!admin && solicitante && <button className="button primary" onClick={() => void openCreate()} disabled={catalogLoading}><Icon name="plus" size={18} /> Nueva solicitud</button>}
      </section>

      <section className="data-panel">
        <div className="data-toolbar responsive-toolbar">
          <div><strong>{admin ? 'Solicitudes institucionales' : 'Historial personal'}</strong><span>{solicitudes.length} solicitud{solicitudes.length === 1 ? '' : 'es'}</span></div>
          {admin && <label className="compact-filter"><span>Filtrar por estado</span><select aria-label="Filtrar solicitudes por estado" value={estado} onChange={(event) => setEstado(event.target.value as EstadoSolicitud | '')}><option value="">Todos los estados</option>{ESTADOS_SOLICITUD.map((item) => <option key={item} value={item}>{etiquetaCodigo(item)}</option>)}</select></label>}
        </div>
        {error && <InlineError message={error} onRetry={() => void load()} />}
        {(loading || catalogLoading || attentionLoading) && <LoadingState label={catalogLoading ? 'Cargando catálogo…' : attentionLoading ? 'Consultando activos disponibles…' : 'Consultando solicitudes…'} />}
        {!loading && !error && solicitudes.length === 0 && <EmptyState title="No hay solicitudes para mostrar" description={admin ? 'No existen requerimientos que coincidan con el filtro.' : 'Registra una solicitud para comenzar.'} />}
        {!loading && !error && solicitudes.length > 0 && <div className="request-list">
          {solicitudes.map((request) => (
            <article className="request-card" key={request.id}>
              <div className="request-card-main">
                <div className="request-number">SOL-{String(request.id).padStart(5, '0')}</div>
                <div><strong>{request.motivo}</strong><span>{admin ? `${request.solicitante} · ` : ''}{fechaHora(request.fechaSolicitud)}</span></div>
              </div>
              <div className="request-card-meta"><span className={`status-badge request-${request.estado.toLocaleLowerCase()}`}><span />{etiquetaCodigo(request.estado)}</span><small>{request.detalles.length} detalle{request.detalles.length === 1 ? '' : 's'}</small></div>
              <div className="row-actions">
                <button className="button secondary small" onClick={() => setDetail(request)}><Icon name="eye" size={16} /> Ver detalle</button>
                {admin && request.estado === 'PENDIENTE' && <button className="button primary small" onClick={() => void openAttention(request)}><Icon name="approve" size={16} /> Atender</button>}
                {admin && request.estado === 'APROBADA' && <button className="button danger small" onClick={() => setCancellation(request)}><Icon name="reject" size={16} /> Cancelar</button>}
              </div>
            </article>
          ))}
        </div>}
      </section>

      {createOpen && <Modal wide title="Nueva solicitud" eyebrow="Requerimiento de activos" onClose={() => setCreateOpen(false)}><SolicitudForm tipos={tipos} onSubmit={saveRequest} onCancel={() => setCreateOpen(false)} /></Modal>}

      {detail && <Modal title={`Solicitud SOL-${String(detail.id).padStart(5, '0')}`} eyebrow="Detalle del requerimiento" onClose={() => setDetail(null)}>
        <div className="request-detail-body">
          <div className="detail-summary"><span className={`status-badge request-${detail.estado.toLocaleLowerCase()}`}><span />{etiquetaCodigo(detail.estado)}</span><small>Registrada {fechaHora(detail.fechaSolicitud)}</small></div>
          {admin && <div className="detail-row"><span>Solicitante</span><strong>{detail.solicitante}</strong></div>}
          <div className="detail-row"><span>Motivo</span><strong>{detail.motivo}</strong></div>
          <div className="detail-list"><h3>Equipos solicitados</h3>{detail.detalles.map((item) => <article key={item.id}><div><strong>{item.tipoActivo}</strong>{item.observaciones && <span>{item.observaciones}</span>}</div><small>{item.activo ? detail.estado === 'APROBADA' ? `Reservado: ${item.activo}` : detail.estado === 'CANCELADA' ? `Reserva liberada: ${item.activo}` : `Activo seleccionado: ${item.activo}` : 'Sin activo seleccionado'}</small></article>)}</div>
          {detail.observacionesAtencion && <div className="attention-note"><span>Observaciones de atención</span><p>{detail.observacionesAtencion}</p><small>{fechaHora(detail.fechaAtencion)}</small></div>}
        </div>
      </Modal>}

      {attention && <Modal wide title={`Atender SOL-${String(attention.id).padStart(5, '0')}`} eyebrow="Decisión administrativa" onClose={() => setAttention(null)}><AtencionSolicitudForm solicitud={attention} activos={availableAssets} onSubmit={saveAttention} onCancel={() => setAttention(null)} /></Modal>}
      {cancellation && <Modal title={`Cancelar SOL-${String(cancellation.id).padStart(5, '0')}`} eyebrow="Liberación de reservas" onClose={() => setCancellation(null)}><CancelacionSolicitudForm onSubmit={saveCancellation} onCancel={() => setCancellation(null)} /></Modal>}
      {toast && <Toast message={toast} onClose={() => setToast('')} />}
    </div>
  )
}
