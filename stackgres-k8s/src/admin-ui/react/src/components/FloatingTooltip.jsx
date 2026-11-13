import { useEffect, useRef, useState } from 'react'
import MarkdownIt from 'markdown-it'

// The global #helpTooltip floating div (Vue NavBar.vue) plus the delegated
// [data-tooltip] handlers (Vue main.js): clicking a help icon shows the
// markdown-rendered tooltip text in a fixed div next to the cursor; clicking
// the same icon, clicking outside, or scrolling hides it. Elements with the
// onHover class (alert badges) also show it on hover and hide on mouseleave.
const md = new MarkdownIt({ html: true, breaks: false, linkify: true, typographer: true })

const DEFAULT_TEXT = 'Click on a question mark to get help and tips about that field.'

export function FloatingTooltip() {
  const [content, setContent] = useState(DEFAULT_TEXT)
  const [visible, setVisible] = useState(false)
  const [style, setStyle] = useState({})
  const triggerRef = useRef(null)
  const divRef = useRef(null)

  useEffect(() => {
    const positionFor = (event, trigger) => {
      const offsetTop = trigger.getBoundingClientRect().top
      const next = {}
      if (window.innerWidth - event.clientX > 420) {
        next.right = 'auto'
        next.left = event.clientX + 10
      } else {
        next.left = 'auto'
        next.right = window.innerWidth - event.clientX + 10
      }
      if (window.innerHeight - event.clientY > 240) {
        next.bottom = 'auto'
        next.top = event.clientY + 10
        next.maxHeight = window.innerHeight - offsetTop - 50
      } else {
        next.top = 'auto'
        next.bottom = window.innerHeight - event.clientY + 10
        next.maxHeight = 'none'
      }
      return next
    }

    const show = (event, trigger) => {
      triggerRef.current?.classList.remove('show')
      trigger.classList.add('show')
      triggerRef.current = trigger
      setContent(trigger.dataset.tooltip || '')
      setStyle(positionFor(event, trigger))
      setVisible(true)
    }

    const hide = () => {
      triggerRef.current?.classList.remove('show')
      triggerRef.current = null
      setContent(DEFAULT_TEXT)
      setVisible(false)
    }

    const onClick = (event) => {
      const trigger = event.target.closest('[data-tooltip]')
      if (trigger) {
        // The icons sit inside <label>s: stop the click from focusing or
        // toggling the labelled control.
        event.preventDefault()
        if (trigger === triggerRef.current) {
          hide()
        } else {
          show(event, trigger)
        }
        return
      }
      if (triggerRef.current && !event.target.closest('#helpTooltip')) {
        hide()
      }
    }

    const onMouseOver = (event) => {
      const trigger = event.target.closest('[data-tooltip].onHover')
      if (trigger && trigger !== triggerRef.current) {
        show(event, trigger)
      }
    }

    const onMouseOut = (event) => {
      const trigger = event.target.closest('[data-tooltip].onHover')
      if (trigger && trigger === triggerRef.current && !trigger.contains(event.relatedTarget)) {
        hide()
      }
    }

    const onScroll = (event) => {
      // Capture-phase so inner scroll containers count too; scrolling the
      // tooltip's own overflow keeps it open.
      if (triggerRef.current && !(event.target instanceof Element && event.target.closest('#helpTooltip'))) {
        hide()
      }
    }

    document.addEventListener('click', onClick)
    document.addEventListener('mouseover', onMouseOver)
    document.addEventListener('mouseout', onMouseOut)
    document.addEventListener('scroll', onScroll, true)
    return () => {
      document.removeEventListener('click', onClick)
      document.removeEventListener('mouseover', onMouseOver)
      document.removeEventListener('mouseout', onMouseOut)
      document.removeEventListener('scroll', onScroll, true)
    }
  }, [])

  // Links inside the tooltip open in a new window, as in Vue.
  const onTooltipClick = (event) => {
    const anchor = event.target.closest('a')
    if (anchor) {
      event.preventDefault()
      window.open(anchor.href)
    }
  }

  return (
    <div
      id="helpTooltip"
      ref={divRef}
      className={visible ? 'show' : undefined}
      hidden={!visible}
      role="tooltip"
      onClick={onTooltipClick}
      style={{
        position: 'fixed',
        zIndex: 1000,
        overflow: 'auto',
        maxWidth: 400,
        padding: '10px 15px',
        ...style,
      }}
      dangerouslySetInnerHTML={{ __html: md.render(content) }}
    />
  )
}
