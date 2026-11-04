import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarUsuarios } from '../api/usuarios'
import { UsuariosPage } from '../pages/UsuariosPage'

vi.mock('../api/usuarios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/usuarios')>()
  return {
    ...actual,
    listarUsuarios: vi.fn(),
    obtenerUsuario: vi.fn(),
    crearUsuario: vi.fn(),
    editarUsuario: vi.fn(),
    cambiarHabilitacion: vi.fn(),
  }
})

const mockedList = vi.mocked(listarUsuarios)

const userPage = (pagina: number, name: string) => ({
  contenido: [{
    id: pagina + 1,
    nombres: name,
    apellidos: 'Quispe',
    correoInstitucional: `${name.toLocaleLowerCase()}@cultura.gob.pe`,
    habilitado: true,
    roles: ['ROLE_TECNICO'],
  }],
  pagina,
  tamanio: 20,
  totalElementos: 21,
  totalPaginas: 2,
  primera: pagina === 0,
  ultima: pagina === 1,
})

describe('administración de usuarios', () => {
  beforeEach(() => {
    mockedList.mockReset()
    mockedList.mockImplementation(async (page = 0) => userPage(page, page === 0 ? 'María' : 'Carlos'))
  })

  it('muestra la tabla sin información sensible y usa paginación base cero', async () => {
    const user = userEvent.setup()
    render(<UsuariosPage />)

    expect(await screen.findByText('María Quispe')).toBeInTheDocument()
    expect(mockedList).toHaveBeenCalledWith(0, 20)
    expect(screen.queryByText(/password|hash|bcrypt/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Página siguiente' }))
    expect(await screen.findByText('Carlos Quispe')).toBeInTheDocument()
    await waitFor(() => expect(mockedList).toHaveBeenCalledWith(1, 20))
  })
})
