import { useState } from 'react'
import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import { clearSession, getSession } from './api.ts'
import AuthPage from './pages/AuthPage.tsx'
import FormsPage from './pages/FormsPage.tsx'
import LeadsPage from './pages/LeadsPage.tsx'
import MessagesPage from './pages/MessagesPage.tsx'

export default function App() {
  const [session, setSession] = useState(getSession)
  const navigate = useNavigate()

  function signedIn() {
    setSession(getSession())
    navigate('/leads')
  }

  function logOut() {
    clearSession()
    setSession(null)
    navigate('/login')
  }

  if (!session) {
    return (
      <main className="narrow">
        <h1>SmartLead</h1>
        <Routes>
          <Route path="/login" element={<AuthPage mode="login" onSignedIn={signedIn} />} />
          <Route path="/register" element={<AuthPage mode="register" onSignedIn={signedIn} />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </main>
    )
  }

  return (
    <>
      <header>
        <strong>SmartLead</strong>
        <nav>
          <NavLink to="/leads">Leads</NavLink>
          <NavLink to="/messages">Messages</NavLink>
          <NavLink to="/forms">Forms</NavLink>
        </nav>
        <span className="account">
          {session.email}
          <button className="link" onClick={logOut}>Log out</button>
        </span>
      </header>
      <main>
        <Routes>
          <Route path="/leads" element={<LeadsPage />} />
          <Route path="/messages" element={<MessagesPage />} />
          <Route path="/forms" element={<FormsPage />} />
          <Route path="*" element={<Navigate to="/leads" replace />} />
        </Routes>
      </main>
    </>
  )
}
