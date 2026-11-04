import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { devolverAsignacion, listarAsignaciones, type AsignacionResponse } from '../api/asignaciones'
import { PrestamosPage } from '../pages/PrestamosPage'

vi.mock('../api/asignaciones', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/asignaciones')>()
  return { ...actual, listarAsignaciones: vi.fn(), devolverAsignacion: vi.fn() }
})

const prestamo: AsignacionResponse = {
  id: 8,
  solicitudId: 4,
  solicitudDetalleId: 11,
  activoId: 3,
  codigoPatrimonial: 'PAT-001',
  tipoActivo: 'Laptop',
  usuarioReceptorId: 15,
  usuarioReceptor: 'Persona Solicitante',
  entregadoPor: 'Administrador SGAT',
  recibidoDevolucionPor: null,
  fechaEntrega: '2026-11-04T10:00:00-05:00',
  fechaDevolucionEstimada: '2026-11-20T10:00:00-05:00',
  fechaDevolucionReal: null,
  observacionesEntrega: 'Equipo verificado',
  observacionesRetorno: null,
  estadoActivo: 'ASIGNADO',
  condicionFisica: 'BUENO',
  abierta: true,
}

describe('préstamos y devoluciones', () => {
  beforeEach(() => {
    vi.mocked(listarAsignaciones).mockReset().mockResolvedValue([prestamo])
    vi.mocked(devolverAsignacion).mockReset().mockResolvedValue({
      ...prestamo,
      abierta: false,
      estadoActivo: 'DISPONIBLE',
      fechaDevolucionReal: '2026-11-05T10:00:00-05:00',
    })
  })

  it('lista únicamente préstamos abiertos de forma predeterminada', async () => {
    render(<MemoryRouter><PrestamosPage /></MemoryRouter>)
    expect(await screen.findByText('PAT-001')).toBeInTheDocument()
    expect(listarAsignaciones).toHaveBeenCalledWith(true)
    expect(screen.getByText('Persona Solicitante')).toBeInTheDocument()
    expect(screen.getByText('Entregó: Administrador SGAT')).toBeInTheDocument()
  })

  it('muestra por separado al receptor, a quien entregó y a quien recibió la devolución', async () => {
    vi.mocked(listarAsignaciones).mockResolvedValue([{
      ...prestamo,
      abierta: false,
      fechaDevolucionReal: '2026-11-05T10:00:00-05:00',
      recibidoDevolucionPor: 'Administradora Receptora',
      estadoActivo: 'DISPONIBLE',
    }])

    render(<MemoryRouter><PrestamosPage /></MemoryRouter>)

    expect(await screen.findByText('Persona Solicitante')).toBeInTheDocument()
    expect(screen.getByText('Entregó: Administrador SGAT')).toBeInTheDocument()
    expect(screen.getByText('Recibió: Administradora Receptora')).toBeInTheDocument()
  })

  it('registra la devolución y refresca el listado', async () => {
    vi.mocked(listarAsignaciones).mockResolvedValueOnce([prestamo]).mockResolvedValueOnce([])
    const user = userEvent.setup()
    render(<MemoryRouter><PrestamosPage /></MemoryRouter>)

    await user.click(await screen.findByRole('button', { name: 'Registrar devolución' }))
    await user.selectOptions(screen.getByLabelText('Condición física'), 'REGULAR')
    await user.type(screen.getByLabelText('Observaciones de devolución'), 'Sin daños nuevos')
    await user.click(screen.getByRole('button', { name: 'Confirmar devolución' }))

    expect(devolverAsignacion).toHaveBeenCalledWith(8, {
      condicionFisica: 'REGULAR', estadoActivo: 'DISPONIBLE', observaciones: 'Sin daños nuevos',
    })
    expect(await screen.findByText('La devolución fue registrada correctamente.')).toBeInTheDocument()
    await waitFor(() => expect(listarAsignaciones).toHaveBeenCalledTimes(2))
  })

  it('consulta el historial completo cuando cambia el filtro', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><PrestamosPage /></MemoryRouter>)
    await screen.findByText('PAT-001')
    await user.selectOptions(screen.getByLabelText('Vista de préstamos'), 'todas')
    await waitFor(() => expect(listarAsignaciones).toHaveBeenLastCalledWith(false))
  })
})
