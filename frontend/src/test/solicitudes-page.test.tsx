import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cancelarSolicitud, listarMisSolicitudes, listarSolicitudes, type SolicitudResponse } from '../api/solicitudes'
import { SolicitudesPage } from '../pages/SolicitudesPage'

const auth = vi.hoisted(() => ({ roles: ['ROLE_SOLICITANTE'] as string[] }))
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ hasRole: (role: string) => auth.roles.includes(role) }) }))
vi.mock('../api/solicitudes', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/solicitudes')>()
  return {
    ...actual,
    listarMisSolicitudes: vi.fn(),
    listarSolicitudes: vi.fn(),
    crearSolicitud: vi.fn(),
    atenderSolicitud: vi.fn(),
    cancelarSolicitud: vi.fn(),
  }
})
vi.mock('../api/activos', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/activos')>()
  return { ...actual, listarActivos: vi.fn(), listarTiposActivo: vi.fn() }
})

const request: SolicitudResponse = {
  id: 5,
  solicitante: 'Persona Solicitante',
  fechaSolicitud: '2026-11-04T10:00:00-05:00',
  fechaAtencion: null,
  motivo: 'Equipo para trabajo remoto',
  estado: 'PENDIENTE',
  observacionesAtencion: null,
  detalles: [{ id: 21, tipoActivoId: 3, tipoActivo: 'Laptop', activo: null, observaciones: null }],
}

describe('bandejas de solicitudes por rol', () => {
  beforeEach(() => {
    vi.mocked(listarMisSolicitudes).mockReset().mockResolvedValue([request])
    vi.mocked(listarSolicitudes).mockReset().mockResolvedValue([request])
    vi.mocked(cancelarSolicitud).mockReset().mockResolvedValue({ ...request, estado: 'CANCELADA' })
  })

  it('consulta solicitudes propias y permite crear para ROLE_SOLICITANTE', async () => {
    auth.roles = ['ROLE_SOLICITANTE']
    render(<MemoryRouter><SolicitudesPage /></MemoryRouter>)
    expect(await screen.findByText('Equipo para trabajo remoto')).toBeInTheDocument()
    expect(listarMisSolicitudes).toHaveBeenCalledOnce()
    expect(listarSolicitudes).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: /Nueva solicitud/ })).toBeInTheDocument()
  })

  it('consulta la bandeja general, filtra y muestra atención solo al administrador', async () => {
    auth.roles = ['ROLE_ADMIN']
    const user = userEvent.setup()
    render(<MemoryRouter><SolicitudesPage /></MemoryRouter>)
    expect(await screen.findByText('Equipo para trabajo remoto')).toBeInTheDocument()
    expect(listarSolicitudes).toHaveBeenCalledWith('')
    await user.selectOptions(screen.getByLabelText('Filtrar por estado'), 'APROBADA')
    await waitFor(() => expect(listarSolicitudes).toHaveBeenLastCalledWith('APROBADA'))
    expect(screen.getByRole('button', { name: /Atender/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Nueva solicitud/ })).not.toBeInTheDocument()
  })

  it('no ejecuta operaciones de solicitudes para ROLE_TECNICO', () => {
    auth.roles = ['ROLE_TECNICO']
    render(<MemoryRouter><SolicitudesPage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'Acceso restringido' })).toBeInTheDocument()
    expect(listarSolicitudes).not.toHaveBeenCalled()
    expect(listarMisSolicitudes).not.toHaveBeenCalled()
  })

  it('permite al administrador cancelar una aprobada y liberar sus reservas', async () => {
    auth.roles = ['ROLE_ADMIN']
    vi.mocked(listarSolicitudes).mockResolvedValueOnce([{
      ...request,
      estado: 'APROBADA',
      detalles: [{ ...request.detalles[0], activo: 'PAT-001' }],
    }]).mockResolvedValueOnce([])
    const user = userEvent.setup()
    render(<MemoryRouter><SolicitudesPage /></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: 'Cancelar' }))
    await user.type(screen.getByLabelText('Motivo de la cancelación'), 'Necesidad retirada')
    await user.click(screen.getByRole('button', { name: 'Cancelar solicitud y liberar reservas' }))

    expect(cancelarSolicitud).toHaveBeenCalledWith(5, { motivo: 'Necesidad retirada' })
    expect(await screen.findByText('La solicitud fue cancelada y sus reservas quedaron liberadas.')).toBeInTheDocument()
  })
})
