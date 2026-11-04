import { Info } from 'lucide-react'
import { useTooltips } from '../contexts/TooltipsContext'

// The Vue helpTooltip span: an icon whose data-tooltip carries the resolved
// help text. `field` is a dotted tooltip key; `text` is a literal used as-is;
// `transform` applies the per-field .replace() chains the Vue templates used.
// When the tooltips file failed to load the icons disappear entirely (the Vue
// noTooltips body class).
export function HelpTooltip({ field, text, transform }) {
  const { error, getTooltip } = useTooltips()
  if (error) {
    return null
  }
  let content = text ?? getTooltip(field)
  if (transform) {
    content = transform(content)
  }
  // aria-hidden keeps the icon out of the fields' accessible names, matching
  // the Vue console's empty styled span. The FloatingTooltip component renders
  // data-tooltip as markdown in the global #helpTooltip div on click.
  return (
    <span className="helpTooltip" data-tooltip={content} aria-hidden="true">
      <Info size={14} />
    </span>
  )
}
