export type ErrorFields = Record<string, string>

interface ErrorPayload {
  mensaje?: string
  campos?: ErrorFields
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown
  authenticated?: boolean
}

const configuredBaseUrl = import.meta.env.VITE_API_URL?.trim() ?? ''
const apiBaseUrl = configuredBaseUrl.replace(/\/$/, '')

let sessionToken: string | null = null
let unauthorizedHandler: (() => void) | null = null

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly fields: ErrorFields = {},
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export function setApiSession(token: string | null, onUnauthorized?: () => void) {
  sessionToken = token
  unauthorizedHandler = onUnauthorized ?? null
}

async function readPayload(response: Response): Promise<ErrorPayload | undefined> {
  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) return undefined

  try {
    return (await response.json()) as ErrorPayload
  } catch {
    return undefined
  }
}

function defaultMessage(status: number) {
  if (status === 400) return 'Revisa los datos ingresados.'
  if (status === 401) return 'La sesión no es válida. Ingresa nuevamente.'
  if (status === 403) return 'No tienes permisos para realizar esta operación.'
  if (status === 404) return 'No se encontró el recurso solicitado.'
  if (status === 409) return 'La operación entra en conflicto con los datos actuales.'
  return 'No fue posible completar la operación.'
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, authenticated = true, headers: customHeaders, ...requestInit } = options
  const headers = new Headers(customHeaders)
  headers.set('Accept', 'application/json')

  if (body !== undefined) headers.set('Content-Type', 'application/json')
  if (authenticated && sessionToken) {
    headers.set('Authorization', `Bearer ${sessionToken}`)
  }

  let response: Response
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...requestInit,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Verifica que el backend esté disponible.')
  }

  if (!response.ok) {
    const payload = await readPayload(response)
    if (response.status === 401 && authenticated) unauthorizedHandler?.()
    throw new ApiError(
      response.status,
      payload?.mensaje || defaultMessage(response.status),
      payload?.campos,
    )
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
