import { Modal } from './Modal'

interface ConfirmDialogProps {
  title: string
  message: string
  confirmLabel: string
  tone?: 'danger' | 'primary'
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  title, message, confirmLabel, tone = 'primary', loading, onConfirm, onCancel,
}: ConfirmDialogProps) {
  return (
    <Modal title={title} eyebrow="Confirmación" onClose={onCancel}>
      <div className="confirm-body">
        <p>{message}</p>
        <div className="modal-actions">
          <button className="button secondary" onClick={onCancel} disabled={loading}>Cancelar</button>
          <button className={`button ${tone}`} onClick={onConfirm} disabled={loading}>
            {loading ? 'Procesando…' : confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  )
}
