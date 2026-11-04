import { Icon } from './Icon'

export function LoadingState({ label = 'Cargando información…' }: { label?: string }) {
  return (
    <div className="loading-state" role="status">
      <span className="spinner" />
      <span>{label}</span>
    </div>
  )
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="empty-state">
      <div className="empty-icon"><Icon name="search" size={24} /></div>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  )
}

export function InlineError({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="inline-error" role="alert">
      <Icon name="alert" />
      <div><strong>No pudimos cargar la información</strong><span>{message}</span></div>
      {onRetry && <button className="button secondary small" onClick={onRetry}>Reintentar</button>}
    </div>
  )
}

export function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  return (
    <div className="toast" role="status">
      <span className="toast-icon"><Icon name="check" size={16} /></span>
      <span>{message}</span>
      <button onClick={onClose} aria-label="Cerrar mensaje"><Icon name="close" size={16} /></button>
    </div>
  )
}
