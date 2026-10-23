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
      <form onSubmit={submit} className="flex w-full max-w-sm flex-col gap-4">
        <h1 className="text-center text-2xl font-semibold">StackGres</h1>
        {authType === 'OIDC' ? (
          <button type="button" onClick={loginExternal} className="border p-2">
            Sign in with single sign-on
          </button>
        ) : (
          <>
            <label className="flex flex-col gap-1 text-sm">
              Username
              <input
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                required
                className="border p-2"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm">
              Password
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                required
                className="border p-2"
              />
            </label>
            {failed && (
              <p role="alert" className="text-sm">
                Invalid username or password
              </p>
            )}
            <button type="submit" className="border p-2 font-semibold">
              Login
            </button>
          </>
        )}
      </form>
    </main>
  )
}
