import { Icon, type IconName } from '../components/Icon'

export function PlaceholderPage({
  module, description, icon,
}: { module: string; description: string; icon: IconName }) {
  return (
    <section className="placeholder-panel">
      <div className="placeholder-icon"><Icon name={icon} size={30} /></div>
      <span className="eyebrow">Estructura inicial</span>
      <h2>{module}</h2>
      <p>{description}</p>
      <div className="placeholder-status"><span /> Módulo preparado para una siguiente fase</div>
    </section>
  )
}
