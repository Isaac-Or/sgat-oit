export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <div className="brand" aria-label="SGAT-OIT">
      <div className="brand-mark" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      {!compact && (
        <div>
          <strong>SGAT</strong>
          <small>OIT · Cultura</small>
        </div>
      )}
    </div>
  )
}
