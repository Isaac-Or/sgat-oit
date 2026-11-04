import { afterEach, describe, expect, it, vi } from 'vitest'
import { devolverAsignacion, listarAsignaciones } from '../api/asignaciones'
import { crearActivo, listarActivos, type ActivoRequest } from '../api/activos'
import { setApiSession } from '../api/client'
import { atenderSolicitud, cancelarSolicitud, crearSolicitud, entregarSolicitud } from '../api/solicitudes'

afterEach(() => {
  setApiSession(null)
  vi.unstubAllGlobals()
})

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } })
}

describe('contratos HTTP de inventario y solicitudes', () => {
  it('consulta activos con el filtro exacto y adjunta el JWT en memoria', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse([]))
    vi.stubGlobal('fetch', fetchMock)
    setApiSession('jwt-prueba')
    await listarActivos('EN_MANTENIMIENTO')
    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/activos?estado=EN_MANTENIMIENTO')
    expect((options.headers as Headers).get('Authorization')).toBe('Bearer jwt-prueba')
  })

  it('envía ActivoRequest sin campos adicionales', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 1 }, 201))
    vi.stubGlobal('fetch', fetchMock)
    const request: ActivoRequest = {
      tipoActivoId: 3, ubicacionId: 7, codigoPatrimonial: 'PAT-1', numeroSerie: null,
      marca: 'Dell', modelo: 'Latitude', descripcion: null, estado: 'DISPONIBLE', condicionFisica: 'BUENO',
    }
    await crearActivo(request)
    const [, options] = fetchMock.mock.calls[0]
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body as string)).toEqual(request)
  })

  it('respeta SolicitudRequest y AtencionRequest con ActivoElegido', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse({ id: 4 }, 201))
      .mockResolvedValueOnce(jsonResponse({ id: 4 }))
    vi.stubGlobal('fetch', fetchMock)
    await crearSolicitud({ motivo: 'Trabajo remoto', detalles: [{ tipoActivoId: 3, observaciones: null }] })
    await atenderSolicitud(4, { aprobada: true, observaciones: 'Atendida', activos: [{ detalleId: 9, activoId: 2 }] })
    expect(fetchMock.mock.calls[0][0]).toBe('/api/solicitudes')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body as string)).toEqual({ motivo: 'Trabajo remoto', detalles: [{ tipoActivoId: 3, observaciones: null }] })
    expect(fetchMock.mock.calls[1][0]).toBe('/api/solicitudes/4')
    expect(JSON.parse(fetchMock.mock.calls[1][1].body as string)).toEqual({ aprobada: true, observaciones: 'Atendida', activos: [{ detalleId: 9, activoId: 2 }] })
  })

  it('cancela una solicitud mediante el endpoint administrativo explícito', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ id: 4, estado: 'CANCELADA' }))
    vi.stubGlobal('fetch', fetchMock)
    await cancelarSolicitud(4, { motivo: 'La necesidad institucional fue retirada' })
    expect(fetchMock.mock.calls[0][0]).toBe('/api/solicitudes/4/cancelacion')
    expect(fetchMock.mock.calls[0][1].method).toBe('PUT')
    expect(JSON.parse(fetchMock.mock.calls[0][1].body as string)).toEqual({
      motivo: 'La necesidad institucional fue retirada',
    })
  })

  it('usa los contratos administrativos de entrega, listado y devolución', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse([], 201))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse({ id: 8 }))
    vi.stubGlobal('fetch', fetchMock)
    await entregarSolicitud(4, {
      fechaDevolucionEstimada: '2027-01-15T12:00:00-05:00', observaciones: 'Entrega verificada',
    })
    await listarAsignaciones(true)
    await devolverAsignacion(8, {
      condicionFisica: 'REGULAR', estadoActivo: 'DISPONIBLE', observaciones: 'Sin daños nuevos',
    })
    expect(fetchMock.mock.calls[0][0]).toBe('/api/solicitudes/4/entrega')
    expect(fetchMock.mock.calls[0][1].method).toBe('POST')
    expect(fetchMock.mock.calls[1][0]).toBe('/api/asignaciones?abiertas=true')
    expect(fetchMock.mock.calls[2][0]).toBe('/api/asignaciones/8/devolucion')
    expect(fetchMock.mock.calls[2][1].method).toBe('PUT')
  })
})
