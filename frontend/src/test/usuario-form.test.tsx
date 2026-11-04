import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { UsuarioForm } from '../components/UsuarioForm'

describe('formulario de usuarios', () => {
  it('valida campos, contraseña y selección de roles antes de crear', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    render(<UsuarioForm onSubmit={onSubmit} onCancel={vi.fn()} />)

    await user.click(screen.getByRole('button', { name: 'Crear usuario' }))

    expect(screen.getByText('Ingresa los nombres.')).toBeInTheDocument()
    expect(screen.getByText('La contraseña debe tener entre 12 y 72 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('Selecciona al menos un rol.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('envía los nombres exactos del contrato y no persiste la contraseña', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<UsuarioForm onSubmit={onSubmit} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Nombres'), 'Ana')
    await user.type(screen.getByLabelText('Apellidos'), 'Torres')
    await user.type(screen.getByLabelText('Correo institucional'), 'ana@cultura.gob.pe')
    await user.type(screen.getByLabelText(/^Contraseña temporal/), 'clave-temporal-2026')
    await user.click(screen.getByText('Administrador'))
    await user.click(screen.getByRole('button', { name: 'Crear usuario' }))

    expect(onSubmit).toHaveBeenCalledWith({
      nombres: 'Ana',
      apellidos: 'Torres',
      correoInstitucional: 'ana@cultura.gob.pe',
      password: 'clave-temporal-2026',
      roles: ['ROLE_ADMIN'],
    })
    expect(window.localStorage).toHaveLength(0)
    expect(window.sessionStorage).toHaveLength(0)
  })
})
