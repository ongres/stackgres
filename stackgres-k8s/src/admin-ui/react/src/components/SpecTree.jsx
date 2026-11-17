import { HelpTooltip } from './HelpTooltip'

// Recursive read-only renderer for a CRD spec/status object: nested
// definition lists, arrays as indexed entries, primitives as label/value.
// Nested levels draw the cloud console's tree connectors: a vertical rule per
// list, a horizontal tick per item, and a mask under the last item's tick so
// the rule ends in a └ corner (the mask matches the dialog card background).
//
// `tooltipPath` is the dotted sg-tooltips path of this node (e.g.
// 'sgcluster.spec.instances'); when set, every labeled node renders the help
// icon, as the Vue detail views did. Array entries keep their parent's path:
// the tooltips index copies array item properties up one level.
const treeList = 'ml-2 border-l border-[hsl(var(--border))]'
const treeItem = (isLast) =>
  `relative min-w-0 pl-3 pt-0.5 pb-0.5 before:content-[''] before:absolute before:left-0 before:top-3 before:w-2.5 before:border-t before:border-[hsl(var(--border))] ${
    isLast
      ? "after:content-[''] after:absolute after:left-[-1px] after:top-3 after:bottom-0 after:w-[1px] after:bg-[hsl(var(--card))]"
      : ''
  }`

function Label({ label, tooltipPath, trailingColon = false }) {
  return (
    <>
      <strong>{label}</strong>
      {tooltipPath && <HelpTooltip field={tooltipPath} />}
      {trailingColon && <strong>:</strong>}
    </>
  )
}

export default function SpecTree({ value, label, isLast = true, topLevel = false, tooltipPath = null }) {
  if (value === null || value === undefined) {
    return null
  }

  if (Array.isArray(value)) {
    if (!value.length) {
      return null
    }
    return (
      <li className={topLevel ? 'min-w-0 pt-0.5 pb-0.5' : treeItem(isLast)}>
        <Label label={label} tooltipPath={tooltipPath} />
        <ul className={treeList}>
          {value.map((entry, index) => (
            <SpecTree
              key={index}
              value={entry}
              label={`#${index + 1}`}
              isLast={index === value.length - 1}
            />
          ))}
        </ul>
      </li>
    )
  }

  if (typeof value === 'object') {
    const entries = Object.entries(value).filter(
      ([, child]) => child !== null && child !== undefined
    )
    if (!entries.length) {
      return null
    }
    if (!label) {
      return (
        <ul className="flex flex-col gap-1">
          {entries.map(([key, child]) => (
            <SpecTree
              key={key}
              value={child}
              label={key}
              topLevel
              isLast
              tooltipPath={tooltipPath ? `${tooltipPath}.${key}` : null}
            />
          ))}
        </ul>
      )
    }
    return (
      <li className={topLevel ? 'min-w-0 pt-0.5 pb-0.5' : treeItem(isLast)}>
        <Label label={label} tooltipPath={tooltipPath} />
        <ul className={treeList}>
          {entries.map(([key, child], index) => (
            <SpecTree
              key={key}
              value={child}
              label={key}
              isLast={index === entries.length - 1}
              tooltipPath={tooltipPath ? `${tooltipPath}.${key}` : null}
            />
          ))}
        </ul>
      </li>
    )
  }

  return (
    <li className={`${topLevel ? 'min-w-0 pt-0.5 pb-0.5' : treeItem(isLast)} flex items-start gap-2`}>
      <span className="flex shrink-0 items-center gap-1">
        <Label label={label} tooltipPath={tooltipPath} trailingColon />
      </span>
      <span className="break-all">{String(value)}</span>
    </li>
  )
}
