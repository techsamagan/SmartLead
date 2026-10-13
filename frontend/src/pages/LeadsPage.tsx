import { useEffect, useState } from 'react'
import { api, type Lead, type Page } from '../api.ts'
import { formatDate } from './format.ts'
import Pager from './Pager.tsx'

const TYPES = ['demo-request', 'pricing-inquiry', 'partnership', 'support', 'other']
const URGENCIES = ['high', 'medium', 'low']

export default function LeadsPage() {
  const [leads, setLeads] = useState<Page<Lead> | null>(null)
  const [page, setPage] = useState(0)
  const [type, setType] = useState('')
  const [urgency, setUrgency] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    api.leads(page, type, urgency).then(setLeads).catch((e: Error) => setError(e.message))
  }, [page, type, urgency])

  const filtered = type !== '' || urgency !== ''

  return (
    <>
      <h2>Leads</h2>
      <div className="row filters">
        <label>
          Urgency
          <select value={urgency} onChange={(e) => { setUrgency(e.target.value); setPage(0) }}>
            <option value="">Any</option>
            {URGENCIES.map((u) => <option key={u}>{u}</option>)}
          </select>
        </label>
        <label>
          Type
          <select value={type} onChange={(e) => { setType(e.target.value); setPage(0) }}>
            <option value="">Any</option>
            {TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </label>
      </div>
      {error && <p className="error">{error}</p>}
      {leads === null && !error && <p>Loading…</p>}
      {leads?.content.length === 0 && (
        <p>{filtered ? 'No leads match these filters.' : 'No leads yet. Messages that look like sales inquiries will appear here.'}</p>
      )}

      {leads !== null && leads.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Received</th>
              <th>Lead</th>
              <th>From</th>
              <th>Type</th>
              <th>Urgency</th>
            </tr>
          </thead>
          <tbody>
            {leads.content.map((lead) => (
              <tr key={lead.id}>
                <td className="nowrap">{formatDate(lead.createdAt)}</td>
                <td>
                  <strong>{lead.title}</strong>
                  <div>{lead.summary}</div>
                  <details>
                    <summary>Original message</summary>
                    <p className="message">{lead.content}</p>
                  </details>
                </td>
                <td>
                  {lead.senderName}
                  <div><a href={`mailto:${lead.senderEmail}`}>{lead.senderEmail}</a></div>
                  <small>via {lead.formName}</small>
                </td>
                <td className="nowrap">{lead.type}</td>
                <td className={`nowrap urgency-${lead.urgency}`}>{lead.urgency}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {leads && <Pager page={page} totalPages={leads.page.totalPages} onChange={setPage} />}
    </>
  )
}
