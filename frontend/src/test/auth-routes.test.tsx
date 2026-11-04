import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { iniciarSesion } from '../api/auth'
import { AppRoutes } from '../App'
import { AuthProvider } from '../auth/AuthContext'

vi.mock('../api/auth', () => ({ iniciarSesion: vi.fn() }))

const mockedLogin = vi.mocked(iniciarSesion)

function renderApp(path = '/') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider><AppRoutes /></AuthProvider>
    </MemoryRouter>,
  )
}

describe('autenticación y rutas protegidas', () => {
  beforeEach(() => mockedLogin.mockReset())

  it('solicita autenticación al acceder a una ruta protegida', () => {
    renderApp('/usuarios')
    expect(screen.getByRole('heading', { name: 'Bienvenido a SGAT' })).toBeInTheDocument()
  })

  it('muestra acceso restringido a una cuenta sin ROLE_ADMIN', async () => {
    mockedLogin.mockResolvedValue({
      token: 'jwt-en-memoria',
      tipo: 'Bearer',
      correo: 'tecnico@cultura.gob.pe',
      roles: ['ROLE_TECNICO'],
    })
    const user = userEvent.setup()
    renderApp('/usuarios')

    await user.type(screen.getByLabelText('Correo institucional'), 'tecnico@cultura.gob.pe')
    await user.type(screen.getByLabelText('Contraseña'), 'una-clave-segura')
    await user.click(screen.getByRole('button', { name: 'Ingresar al sistema' }))

    expect(await screen.findByRole('heading', { level: 2, name: 'Acceso restringido' })).toBeInTheDocument()
  })
})
