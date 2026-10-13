const TOKEN_KEY = 'smartlead.token'
const EMAIL_KEY = 'smartlead.email'

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8081'

export type Form = {
  id: string
  name: string
  formKey: string
  createdAt: string
}

export type Message = {
  id: string
  formName: string
  senderName: string
  senderEmail: string
  content: string
  status: 'PENDING' | 'QUALIFIED' | 'NOT_A_LEAD' | 'FAILED'
  createdAt: string
}

export type Lead = {
  id: string
  formName: string
  senderName: string
  senderEmail: string
  content: string
  title: string
  type: string
  urgency: string
  summary: string
  createdAt: string
}

export type Page<T> = {
  content: T[]
  page: { number: number; totalPages: number; totalElements: number }
}

export function getSession() {
  const token = localStorage.getItem(TOKEN_KEY)
  const email = localStorage.getItem(EMAIL_KEY)
  return token && email ? { token, email } : null
}

export function saveSession(token: string, email: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(EMAIL_KEY, email)
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EMAIL_KEY)
}

export function submitUrl(form: Form) {
  return `${API_URL}/api/v1/forms/${form.formKey}/messages`
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const session = getSession()
  const response = await fetch(`/api/v1${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
    },
  })

  // An expired or revoked token: send the user back to the login page
  if (response.status === 401 && session) {
    clearSession()
    window.location.assign('/login')
    throw new Error('Your session has expired')
  }
  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

async function errorMessage(response: Response) {
  try {
    const body = await response.json()
    const fieldErrors = Object.values(body.validationErrors ?? {})
    return fieldErrors.length > 0 ? fieldErrors.join('. ') : (body.message ?? response.statusText)
  } catch {
    return `Request failed (${response.status})`
  }
}

type AuthResult = { token: string; email: string }

export const api = {
  register: (email: string, password: string) =>
    request<AuthResult>('/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),

  login: (email: string, password: string) =>
    request<AuthResult>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),

  forms: () => request<Form[]>('/forms'),

  createForm: (name: string) => request<Form>('/forms', { method: 'POST', body: JSON.stringify({ name }) }),

  deleteForm: (id: string) => request<void>(`/forms/${id}`, { method: 'DELETE' }),

  messages: (page: number) => request<Page<Message>>(`/messages?page=${page}`),

  leads: (page: number, type: string, urgency: string) =>
    request<Page<Lead>>(`/leads?page=${page}&type=${type}&urgency=${urgency}`),
}
