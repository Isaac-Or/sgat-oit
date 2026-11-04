import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import { AtencionSolicitudForm } from '../components/AtencionSolicitudForm'
import { SolicitudForm } from '../components/SolicitudForm'

const request = {
  id: 5,
  solicitante: 'Persona Solicitante',
  fechaSolicitud: '2026-11-04T10:00:00-05:00',
  fechaAtencion: null,
  motivo: 'Trabajo remoto',
  estado: 'PENDIENTE' as const,
  observacionesAtencion: null,
  detalles: [{ id: 21, tipoActivoId: 3, tipoActivo: 'Laptop', activo: null, observaciones: 'Con cámara' }],
}

describe('formularios de solicitudes', () => {
  it('crea una solicitud usando SolicitudRequest y DetalleRequest', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<SolicitudForm tipos={[{ id: 3, nombre: 'Laptop', categoria: 'Cómputo' }]} onSubmit={onSubmit} onCancel={vi.fn()} />)

    await user.type(screen.getByLabelText('Motivo'), 'Necesito equipo para comisión')
    await user.selectOptions(screen.getByLabelText('Tipo de activo'), '3')
    await user.type(screen.getByLabelText('Observaciones'), 'Con cámara')
    await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))

    expect(onSubmit).toHaveBeenCalledWith({
      motivo: 'Necesito equipo para comisión',
      detalles: [{ tipoActivoId: 3, observaciones: 'Con cámara' }],
    })
  })

  it('presenta el mensaje del backend ante un conflicto 409', async () => {
    const onSubmit = vi.fn().mockRejectedValue(new ApiError(409, 'No hay equipos disponibles del tipo: Laptop'))
    const user = userEvent.setup()
    render(<SolicitudForm tipos={[{ id: 3, nombre: 'Laptop', categoria: 'Cómputo' }]} onSubmit={onSubmit} onCancel={vi.fn()} />)
    await user.type(screen.getByLabelText('Motivo'), 'Necesidad de trabajo')
    await user.selectOptions(screen.getByLabelText('Tipo de activo'), '3')
    await user.click(screen.getByRole('button', { name: 'Registrar solicitud' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No hay equipos disponibles')
  })

  it('aprueba seleccionando un ActivoElegido por detalle', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<AtencionSolicitudForm solicitud={request} activos={[{
      id: 8, tipoActivoId: 3, ubicacionId: 7, codigoPatrimonial: 'PAT-8', numeroSerie: null,
      marca: 'Dell', modelo: 'Latitude', descripcion: null,
      categoria: 'Cómputo', tipoActivo: 'Laptop', ubicacion: 'Sede', estado: 'DISPONIBLE', condicionFisica: 'BUENO', reservado: false,
    }]} onSubmit={onSubmit} onCancel={vi.fn()} />)

    await user.selectOptions(screen.getByLabelText(/^Laptop/), '8')
    await user.type(screen.getByLabelText('Observaciones de atención'), 'Entrega coordinada')
    await user.click(screen.getByRole('button', { name: 'Confirmar aprobación' }))
    expect(onSubmit).toHaveBeenCalledWith({
      aprobada: true,
      observaciones: 'Entrega coordinada',
      activos: [{ detalleId: 21, activoId: 8 }],
    })
  })

  it('filtra activos candidatos por tipoActivoId y no por nombres coincidentes', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<AtencionSolicitudForm solicitud={request} activos={[
      {
        id: 8, tipoActivoId: 99, ubicacionId: 7, codigoPatrimonial: 'PAT-NOMBRE', numeroSerie: null,
        marca: 'Dell', modelo: 'Nombre igual', descripcion: null, categoria: 'Cómputo', tipoActivo: 'Laptop',
        ubicacion: 'Sede', estado: 'DISPONIBLE', condicionFisica: 'BUENO', reservado: false,
      },
      {
        id: 9, tipoActivoId: 3, ubicacionId: 7, codigoPatrimonial: 'PAT-ID', numeroSerie: null,
        marca: 'Lenovo', modelo: 'ID correcto', descripcion: null, categoria: 'Cómputo', tipoActivo: 'Portátil',
        ubicacion: 'Sede', estado: 'DISPONIBLE', condicionFisica: 'BUENO', reservado: false,
      },
    ]} onSubmit={onSubmit} onCancel={vi.fn()} />)

    expect(screen.queryByRole('option', { name: /PAT-NOMBRE/ })).not.toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText(/^Laptop/), '9')
    await user.click(screen.getByRole('button', { name: 'Confirmar aprobación' }))
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({
      activos: [{ detalleId: 21, activoId: 9 }],
    }))
  })

  it('excluye activos con reserva activa aunque estén físicamente disponibles', () => {
    render(<AtencionSolicitudForm solicitud={request} activos={[{
      id: 8, tipoActivoId: 3, ubicacionId: 7, codigoPatrimonial: 'PAT-RESERVADO', numeroSerie: null,
      marca: 'Dell', modelo: 'Latitude', descripcion: null, categoria: 'Cómputo', tipoActivo: 'Laptop',
      ubicacion: 'Sede', estado: 'DISPONIBLE', condicionFisica: 'BUENO', reservado: true,
    }]} onSubmit={vi.fn()} onCancel={vi.fn()} />)

    expect(screen.queryByRole('option', { name: /PAT-RESERVADO/ })).not.toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Sin activos compatibles disponibles' })).toBeInTheDocument()
  })

  it('rechaza sin enviar activos y exige observación', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const user = userEvent.setup()
    render(<AtencionSolicitudForm solicitud={request} activos={[]} onSubmit={onSubmit} onCancel={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: 'Rechazar' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar rechazo' }))
    expect(screen.getByRole('alert')).toHaveTextContent('Indica el motivo del rechazo')
    await user.type(screen.getByLabelText('Motivo del rechazo'), 'No corresponde al alcance')
    await user.click(screen.getByRole('button', { name: 'Confirmar rechazo' }))
    expect(onSubmit).toHaveBeenCalledWith({ aprobada: false, observaciones: 'No corresponde al alcance', activos: null })
  })
})
