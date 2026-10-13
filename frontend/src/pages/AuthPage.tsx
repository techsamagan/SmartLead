import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, saveSession } from '../api.ts'

type Props = {
  mode: 'login' | 'register'
  onSignedIn: () => void
}

export default function AuthPage({ mode, onSignedIn }: Props) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const registering = mode === 'register'

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      const result = registering ? await api.register(email, password) : await api.login(email, password)
      saveSession(result.token, result.email)
      onSignedIn()
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="stack" onSubmit={submit}>
      <h2>{registering ? 'Create an account' : 'Log in'}</h2>
      <label>
        Email
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
      </label>
      <label>
        Password
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={registering ? 8 : undefined}
          required
        />
        {registering && <small>At least 8 characters.</small>}
      </label>
      {error && <p className="error">{error}</p>}
      <button type="submit" disabled={busy}>
        {registering ? 'Create account' : 'Log in'}
      </button>
      <p>
        {registering ? (
          <>Already have an account? <Link to="/login">Log in</Link></>
        ) : (
          <>No account yet? <Link to="/register">Create one</Link></>
        )}
      </p>
    </form>
  )
}
