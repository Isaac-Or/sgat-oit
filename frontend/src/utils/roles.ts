const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_TECNICO: 'Técnico',
  ROLE_SOLICITANTE: 'Solicitante',
}

export function etiquetaRol(role: string) {
  return ROLE_LABELS[role] ?? role.replace('ROLE_', '')
}
