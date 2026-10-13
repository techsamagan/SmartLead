import { useEffect, useState, type FormEvent } from 'react'
import { api, submitUrl, type Form } from '../api.ts'

function snippet(form: Form) {
  return `<form action="${submitUrl(form)}" method="POST">
  <input name="senderName" placeholder="Your name" required>
  <input name="senderEmail" type="email" placeholder="Your email" required>
  <textarea name="content" placeholder="Your message" required></textarea>
  <input name="_gotcha" style="display:none" tabindex="-1" autocomplete="off">
  <button type="submit">Send</button>
</form>`
}

export default function FormsPage() {
  const [forms, setForms] = useState<Form[] | null>(null)
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [copied, setCopied] = useState('')

  useEffect(() => {
    api.forms().then(setForms).catch((e: Error) => setError(e.message))
  }, [])

  async function create(event: FormEvent) {
    event.preventDefault()
    setError('')
    try {
      const form = await api.createForm(name)
      setForms((current) => [...(current ?? []), form])
      setName('')
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function remove(form: Form) {
    if (!window.confirm(`Delete "${form.name}"? Its messages and leads will be deleted too.`)) {
      return
    }
    try {
      await api.deleteForm(form.id)
      setForms((current) => (current ?? []).filter((f) => f.id !== form.id))
    } catch (e) {
      setError((e as Error).message)
    }
  }

  async function copy(key: string, text: string) {
    await navigator.clipboard.writeText(text)
    setCopied(key)
  }

  return (
    <>
      <h2>Forms</h2>
      <p>
        Each form has its own URL. Point the contact form on your website at it, and what people send
        shows up under Messages and Leads.
      </p>
      {error && <p className="error">{error}</p>}
      {forms === null && !error && <p>Loading…</p>}

      {forms?.map((form) => (
        <section key={form.id} className="form-card">
          <div className="row">
            <h3>{form.name}</h3>
            <button className="link danger" onClick={() => remove(form)}>Delete</button>
          </div>
          <label>
            Form URL
            <div className="row">
              <input readOnly value={submitUrl(form)} onFocus={(e) => e.target.select()} />
              <button onClick={() => copy(form.id, submitUrl(form))}>
                {copied === form.id ? 'Copied' : 'Copy'}
              </button>
            </div>
          </label>
          <details>
            <summary>HTML to paste into your site</summary>
            <pre>{snippet(form)}</pre>
            <button onClick={() => copy(form.id + 'html', snippet(form))}>
              {copied === form.id + 'html' ? 'Copied' : 'Copy HTML'}
            </button>
          </details>
        </section>
      ))}

      <form className="row" onSubmit={create}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Name, e.g. Pricing page"
          maxLength={100}
          required
        />
        <button type="submit">Add form</button>
      </form>
    </>
  )
}
