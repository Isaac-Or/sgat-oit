import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { iniciarSesion, type LoginRequest } from '../api/auth'
import { setApiSession } from '../api/client'

export interface AuthSession {
  token: string
  tipo: string
  correo: string
  roles: string[]
}

interface AuthContextValue {
  session: AuthSession | null
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
  hasRole: (role: string) => boolean
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null)

  const logout = useCallback(() => {
    setApiSession(null)
    setSession(null)
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await iniciarSesion(credentials)
    const nextSession: AuthSession = {
      token: response.token,
      tipo: response.tipo,
      correo: response.correo,
      roles: response.roles,
    }
    setApiSession(response.token, logout)
    setSession(nextSession)
  }, [logout])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    login,
    logout,
    hasRole: (role) => session?.roles.includes(role) ?? false,
  }), [session, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe utilizarse dentro de AuthProvider')
  return context
}
