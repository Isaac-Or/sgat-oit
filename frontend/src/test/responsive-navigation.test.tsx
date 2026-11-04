import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AppShell } from '../components/AppShell'

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    session: { correo: 'tecnico@example.test', roles: ['ROLE_TECNICO'], token: 'memoria', tipo: 'Bearer' },
    logout: vi.fn(),
    hasRole: (role: string) => role === 'ROLE_TECNICO',
  }),
}))

describe('navegación adaptable', () => {
  it('abre y cierra el menú móvil y oculta módulos no autorizados', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><Routes><Route element={<AppShell />}><Route index element={<div>Contenido</div>} /></Route></Routes></MemoryRouter>)
    const sidebar = screen.getByRole('complementary')
    expect(sidebar).not.toHaveClass('open')
    await user.click(screen.getByRole('button', { name: 'Abrir menú' }))
    expect(sidebar).toHaveClass('open')
    expect(screen.queryByRole('link', { name: /Solicitudes/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Usuarios/ })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cerrar menú' }))
    expect(sidebar).not.toHaveClass('open')
  })
})
