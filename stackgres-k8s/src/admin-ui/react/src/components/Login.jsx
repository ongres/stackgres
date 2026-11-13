import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'

export default function Login() {
  const { authType, login, loginExternal } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [failed, setFailed] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setFailed(false)
    try {
      await login(username, password)
    } catch {
      setFailed(true)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center p-8">
      <form
        onSubmit={submit}
        className="flex w-full max-w-sm flex-col gap-4 rounded-lg border bg-[hsl(var(--card))] p-8 shadow"
      >
        <h1 className="text-center text-lg leading-none">
          Stack<strong className="font-bold">Gres</strong>
        </h1>
        {authType === 'OIDC' ? (
          <button
            type="button"
            onClick={loginExternal}
            className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]"
          >
            Sign in with single sign-on
          </button>
        ) : (
          <>
            <label className="flex flex-col gap-1 text-sm">
              <span className="text-[hsl(var(--muted-foreground))]">Username</span>
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
                className="w-full border p-2 rounded bg-[hsl(var(--input-background))]"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              <span className="text-[hsl(var(--muted-foreground))]">Password</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
                className="w-full border p-2 rounded bg-[hsl(var(--input-background))]"
              />
            </label>
            {failed && (
              <p role="alert" className="text-sm text-red-500">
                Invalid username or password
              </p>
            )}
            <button
              type="submit"
              className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]"
            >
              Login
            </button>
          </>
        )}
      </form>
    </main>
  )
}
