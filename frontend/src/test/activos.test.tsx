import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  crearActivo, editarActivo, listarActivos, listarTiposActivo, listarUbicaciones,
  type ActivoResponse,
} from '../api/activos'
import { InventarioPage } from '../pages/InventarioPage'

const auth = vi.hoisted(() => ({ roles: ['ROLE_ADMIN'] as string[] }))

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ hasRole: (role: string) => auth.roles.includes(role) }),
}))

vi.mock('../api/activos', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/activos')>()
  return {
    ...actual,
    listarActivos: vi.fn(),
    crearActivo: vi.fn(),
    editarActivo: vi.fn(),
    listarTiposActivo: vi.fn(),
    listarUbicaciones: vi.fn(),
    obtenerHistorialActivo: vi.fn(),
  }
})

const asset: ActivoResponse = {
  id: 12,
  tipoActivoId: 3,
  ubicacionId: 7,
  codigoPatrimonial: 'PAT-0012',
  numeroSerie: 'SER-12',
  marca: 'Dell',
  modelo: 'Latitude',
  descripcion: 'Equipo asignable',
  categoria: 'Cómputo',
  tipoActivo: 'Laptop',
  ubicacion: 'Sede central',
  estado: 'DISPONIBLE',
  condicionFisica: 'BUENO',
  reservado: false,
}

describe('inventario tecnológico', () => {
  beforeEach(() => {
    auth.roles = ['ROLE_ADMIN']
    vi.mocked(listarActivos).mockReset().mockResolvedValue([asset])
    vi.mocked(crearActivo).mockReset().mockResolvedValue(asset)
    vi.mocked(editarActivo).mockReset().mockResolvedValue(asset)
    vi.mocked(listarTiposActivo).mockReset().mockResolvedValue([{ id: 3, nombre: 'Laptop', categoria: 'Cómputo' }])
    vi.mocked(listarUbicaciones).mockReset().mockResolvedValue([{ id: 7, codigo: 'SC', nombre: 'Sede central' }])
  })

  it('lista activos y aplica el filtro de estado para administradores', async () => {
    const user = userEvent.setup()
    render(<InventarioPage />)

    expect(await screen.findByText('PAT-0012')).toBeInTheDocument()
    expect(listarActivos).toHaveBeenCalledWith('')
    await user.selectOptions(screen.getByLabelText('Filtrar por estado'), 'ASIGNADO')
    await waitFor(() => expect(listarActivos).toHaveBeenLastCalledWith('ASIGNADO'))
  })

  it('restringe registro y edición a ROLE_ADMIN y fuerza disponibles al solicitante', async () => {
    auth.roles = ['ROLE_SOLICITANTE']
    render(<InventarioPage />)

    expect(await screen.findByText('PAT-0012')).toBeInTheDocument()
    expect(listarActivos).toHaveBeenCalledWith('DISPONIBLE')
    expect(screen.queryByRole('button', { name: /Registrar activo/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Editar PAT-0012/ })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Filtrar por estado')).not.toBeInTheDocument()
  })

  it('registra un activo con los campos exactos de ActivoRequest', async () => {
    const user = userEvent.setup()
    render(<InventarioPage />)
    await screen.findByText('PAT-0012')
    await user.click(screen.getByRole('button', { name: /Registrar activo/ }))

    await user.selectOptions(await screen.findByLabelText('Tipo de activo'), '3')
    await user.selectOptions(screen.getByLabelText('Ubicación'), '7')
    await user.type(screen.getByLabelText('Código patrimonial'), 'PAT-0099')
    await user.type(screen.getByLabelText('Número de serie'), 'SER-99')
    await user.type(screen.getByLabelText('Marca'), 'Lenovo')
    await user.type(screen.getByLabelText('Modelo'), 'ThinkPad')
    await user.type(screen.getByLabelText('Descripción'), 'Equipo de prueba')
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Registrar activo' }))

    expect(crearActivo).toHaveBeenCalledWith({
      tipoActivoId: 3,
      ubicacionId: 7,
      codigoPatrimonial: 'PAT-0099',
      numeroSerie: 'SER-99',
      marca: 'Lenovo',
      modelo: 'ThinkPad',
      descripcion: 'Equipo de prueba',
      estado: 'DISPONIBLE',
      condicionFisica: 'BUENO',
    })
  })

  it('edita un activo existente sin inventar un endpoint de consulta individual', async () => {
    const user = userEvent.setup()
    render(<InventarioPage />)
    await screen.findByText('PAT-0012')
    await user.click(screen.getByRole('button', { name: 'Editar PAT-0012' }))
    const model = await screen.findByLabelText('Modelo')
    await user.clear(model)
    await user.type(model, 'Latitude 7450')
    await user.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(editarActivo).toHaveBeenCalledWith(12, expect.objectContaining({
      tipoActivoId: 3,
      ubicacionId: 7,
      codigoPatrimonial: 'PAT-0012',
      modelo: 'Latitude 7450',
      descripcion: 'Equipo asignable',
    }))
  })

  it('muestra la reserva sin alterar el estado físico del activo', async () => {
    vi.mocked(listarActivos).mockResolvedValue([{ ...asset, reservado: true }])
    render(<InventarioPage />)

    expect(await screen.findByText('Reservado')).toBeInTheDocument()
    expect(screen.getAllByText('Disponible')).toHaveLength(2)
  })
})
