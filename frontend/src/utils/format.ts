export function etiquetaCodigo(value: string | null | undefined) {
  if (!value) return 'Sin información'
  return value.toLocaleLowerCase('es').split('_')
    .map((part) => part.replace(/^./, (letter) => letter.toLocaleUpperCase('es')))
    .join(' ')
}

export function fechaHora(value: string | null | undefined) {
  if (!value) return 'Pendiente'
  return new Intl.DateTimeFormat('es-PE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}
