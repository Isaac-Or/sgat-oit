import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest, setApiSession } from '../api/client'

afterEach(() => {
  setApiSession(null)
  vi.unstubAllGlobals()
})

describe('cliente HTTP', () => {
  it('envía el JWT en memoria y cierra la sesión ante un 401', async () => {
    const onUnauthorized = vi.fn()
    const fetchMock = vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ mensaje: 'Token no válido' }),
      { status: 401, headers: { 'content-type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetchMock)
    setApiSession('token-prueba', onUnauthorized)

    await expect(apiRequest('/api/usuarios')).rejects.toMatchObject({
      status: 401,
      message: 'Token no válido',
    })

    const [, options] = fetchMock.mock.calls[0]
    expect((options.headers as Headers).get('Authorization')).toBe('Bearer token-prueba')
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('conserva la sesión y traduce un 403 sin filtrar la respuesta', async () => {
    const onUnauthorized = vi.fn()
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 403 })))
    setApiSession('token-prueba', onUnauthorized)

    await expect(apiRequest('/api/usuarios')).rejects.toMatchObject({
      status: 403,
      message: 'No tienes permisos para realizar esta operación.',
    })
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it.each([
    [400, 'Revisa los datos ingresados.'],
    [404, 'No se encontró el recurso solicitado.'],
  ])('presenta un mensaje seguro para HTTP %s sin cuerpo de error', async (status, message) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status })))

    await expect(apiRequest('/api/recurso')).rejects.toMatchObject({ status, message })
  })
})
