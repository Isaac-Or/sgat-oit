import { useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import {
  cambiarHabilitacion, crearUsuario, editarUsuario, listarUsuarios, obtenerUsuario,
  type CrearUsuarioRequest, type EditarUsuarioRequest, type Pagina, type Usuario,
} from '../api/usuarios'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState, InlineError, LoadingState, Toast } from '../components/Feedback'
import { Icon } from '../components/Icon'
import { Modal } from '../components/Modal'
import { UsuarioForm } from '../components/UsuarioForm'
import { etiquetaRol } from '../utils/roles'

const PAGE_SIZE = 20

export function UsuariosPage() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Pagina<Usuario> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<Usuario | undefined>()
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [statusTarget, setStatusTarget] = useState<Usuario | null>(null)
  const [changingStatus, setChangingStatus] = useState(false)
  const [toast, setToast] = useState('')

  const load = useCallback(async (targetPage = page) => {
    setLoading(true)
    setError('')
    try {
      const response = await listarUsuarios(targetPage, PAGE_SIZE)
      setData(response)
      setPage(response.pagina)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible consultar los usuarios.')
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => { void load(page) }, [page, load])
  useEffect(() => {
    if (!toast) return
    const timeout = window.setTimeout(() => setToast(''), 4000)
    return () => window.clearTimeout(timeout)
  }, [toast])

  const openCreate = () => {
    setSelected(undefined)
    setModalMode('create')
  }

  const openEdit = async (id: number) => {
    setLoadingDetail(true)
    setError('')
    try {
      const user = await obtenerUsuario(id)
      setSelected(user)
      setModalMode('edit')
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible obtener el usuario.')
    } finally {
      setLoadingDetail(false)
    }
  }

  const save = async (request: CrearUsuarioRequest | EditarUsuarioRequest) => {
    if (modalMode === 'create') {
      await crearUsuario(request as CrearUsuarioRequest)
      setToast('El usuario fue creado correctamente.')
      setModalMode(null)
      if (page !== 0) setPage(0)
      else await load(0)
      return
    }
    if (!selected) return
    await editarUsuario(selected.id, request as EditarUsuarioRequest)
    setToast('Los datos del usuario fueron actualizados.')
    setModalMode(null)
    await load(page)
  }

  const changeStatus = async () => {
    if (!statusTarget) return
    setChangingStatus(true)
    try {
      await cambiarHabilitacion(statusTarget.id, !statusTarget.habilitado)
      setToast(statusTarget.habilitado ? 'La cuenta fue deshabilitada.' : 'La cuenta fue habilitada.')
      setStatusTarget(null)
      await load(page)
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : 'No fue posible cambiar el estado de la cuenta.')
      setStatusTarget(null)
    } finally {
      setChangingStatus(false)
    }
  }

  return (
    <div className="page-stack users-page">
      <section className="page-intro">
        <div>
          <span className="eyebrow">Control de acceso</span>
          <h2>Usuarios del sistema</h2>
          <p>Administra cuentas institucionales, roles y disponibilidad de acceso.</p>
        </div>
        <button className="button primary" onClick={openCreate}>
          <Icon name="plus" size={18} /> Nuevo usuario
        </button>
      </section>

      <section className="data-panel">
        <div className="data-toolbar">
          <div>
            <strong>Directorio institucional</strong>
            <span>{data ? `${data.totalElementos} usuario${data.totalElementos === 1 ? '' : 's'}` : 'Consultando usuarios'}</span>
          </div>
          <div className="page-size-indicator">{PAGE_SIZE} por página</div>
        </div>

        {error && <InlineError message={error} onRetry={() => void load(page)} />}
        {(loading || loadingDetail) && <LoadingState label={loadingDetail ? 'Consultando usuario…' : undefined} />}

        {!loading && !error && data?.contenido.length === 0 && (
          <EmptyState title="No hay usuarios para mostrar" description="Crea la primera cuenta institucional para comenzar." />
        )}

        {!loading && !error && data && data.contenido.length > 0 && (
          <>
            <div className="table-scroll">
              <table className="users-table">
                <thead><tr><th>Usuario</th><th>Roles</th><th>Estado</th><th><span className="sr-only">Acciones</span></th></tr></thead>
                <tbody>
                  {data.contenido.map((user) => (
                    <tr key={user.id}>
                      <td>
                        <div className="user-cell">
                          <div className="table-avatar">{user.nombres.slice(0, 1)}{user.apellidos.slice(0, 1)}</div>
                          <div><strong>{user.nombres} {user.apellidos}</strong><span>{user.correoInstitucional}</span></div>
                        </div>
                      </td>
                      <td><div className="role-row">{user.roles.map((role) => <span className="role-chip" key={role}>{etiquetaRol(role)}</span>)}</div></td>
                      <td><span className={`status-badge ${user.habilitado ? 'enabled' : 'disabled'}`}><span />{user.habilitado ? 'Habilitado' : 'Deshabilitado'}</span></td>
                      <td>
                        <div className="row-actions">
                          <button className="icon-button bordered" onClick={() => void openEdit(user.id)} aria-label={`Editar a ${user.nombres}`} title="Editar"><Icon name="edit" size={17} /></button>
                          <button
                            className={`icon-button bordered ${user.habilitado ? 'danger-text' : 'success-text'}`}
                            onClick={() => setStatusTarget(user)}
                            aria-label={`${user.habilitado ? 'Deshabilitar' : 'Habilitar'} a ${user.nombres}`}
                            title={user.habilitado ? 'Deshabilitar' : 'Habilitar'}
                          ><Icon name="power" size={17} /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <footer className="pagination">
              <span>Página {data.pagina + 1} de {Math.max(data.totalPaginas, 1)}</span>
              <div>
                <button className="icon-button bordered" disabled={data.primera} onClick={() => setPage((value) => value - 1)} aria-label="Página anterior"><Icon name="chevronLeft" /></button>
                <button className="icon-button bordered" disabled={data.ultima} onClick={() => setPage((value) => value + 1)} aria-label="Página siguiente"><Icon name="chevronRight" /></button>
              </div>
            </footer>
          </>
        )}
      </section>

      {modalMode && (
        <Modal
          title={modalMode === 'create' ? 'Crear usuario' : 'Editar usuario'}
          eyebrow={modalMode === 'create' ? 'Nueva cuenta institucional' : 'Actualización de cuenta'}
          onClose={() => setModalMode(null)}
          wide
        >
          <UsuarioForm usuario={selected} onSubmit={save} onCancel={() => setModalMode(null)} />
        </Modal>
      )}

      {statusTarget && (
        <ConfirmDialog
          title={statusTarget.habilitado ? 'Deshabilitar cuenta' : 'Habilitar cuenta'}
          message={statusTarget.habilitado
            ? `La cuenta de ${statusTarget.nombres} perderá acceso inmediatamente, incluso con tokens anteriores.`
            : `La cuenta de ${statusTarget.nombres} podrá volver a iniciar sesión.`}
          confirmLabel={statusTarget.habilitado ? 'Deshabilitar' : 'Habilitar'}
          tone={statusTarget.habilitado ? 'danger' : 'primary'}
          loading={changingStatus}
          onConfirm={() => void changeStatus()}
          onCancel={() => setStatusTarget(null)}
        />
      )}
      {toast && <Toast message={toast} onClose={() => setToast('')} />}
    </div>
  )
}
