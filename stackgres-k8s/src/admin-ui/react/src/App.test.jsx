import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

describe('App', () => {
  it('renders the placeholder page', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({ ok: true, json: () => Promise.resolve(['default', 'demo']) })
      )
    )

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>
    )

    expect(screen.getByRole('heading', { name: 'StackGres Web Console' })).toBeInTheDocument()
    expect(await screen.findByTestId('namespaces')).toHaveTextContent('default, demo')

    vi.unstubAllGlobals()
  })
})
