import { useEffect, useState } from 'react'
import { api, type Message, type Page } from '../api.ts'
import { formatDate } from './format.ts'
import Pager from './Pager.tsx'

const STATUS_LABELS: Record<Message['status'], string> = {
  PENDING: 'Processing',
  QUALIFIED: 'Lead',
  NOT_A_LEAD: 'Not a lead',
  FAILED: 'Failed',
}

export default function MessagesPage() {
  const [messages, setMessages] = useState<Page<Message> | null>(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')

  useEffect(() => {
    api.messages(page).then(setMessages).catch((e: Error) => setError(e.message))
  }, [page])

  return (
    <>
      <h2>Messages</h2>
      <p>Everything sent to your forms, whether or not it turned out to be a lead.</p>
      {error && <p className="error">{error}</p>}
      {messages === null && !error && <p>Loading…</p>}
      {messages?.content.length === 0 && <p>No messages yet. Add your form URL to your website to start receiving them.</p>}

      {messages !== null && messages.content.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Received</th>
              <th>From</th>
              <th>Message</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {messages.content.map((message) => (
              <tr key={message.id}>
                <td className="nowrap">{formatDate(message.createdAt)}</td>
                <td>
                  {message.senderName}
                  <div><a href={`mailto:${message.senderEmail}`}>{message.senderEmail}</a></div>
                  <small>via {message.formName}</small>
                </td>
                <td className="message">{message.content}</td>
                <td className={`nowrap status-${message.status}`}>{STATUS_LABELS[message.status]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {messages && <Pager page={page} totalPages={messages.page.totalPages} onChange={setPage} />}
    </>
  )
}
