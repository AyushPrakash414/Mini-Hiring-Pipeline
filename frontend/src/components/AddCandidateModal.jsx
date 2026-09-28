import { useState } from 'react'
import * as api from '../api'

export default function AddCandidateModal({ onClose, onCreated }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    if (!name.trim() || !email.trim()) {
      setError('Name and email are required.')
      return
    }

    try {
      setLoading(true)
      await api.createCandidate({
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim() || null,
      })
      onCreated()
    } catch (err) {
      setError(err.message || 'Failed to create candidate')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-labelledby="add-modal-title" aria-modal="true">
        <div className="modal-header">
          <h2 id="add-modal-title">Add Candidate</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">&times;</button>
        </div>
        <div className="modal-body">
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="add-name">
                Full Name <span className="required">*</span>
              </label>
              <input
                id="add-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Ayush Prakash"
                maxLength={255}
                autoFocus
              />
            </div>
            <div className="form-group">
              <label htmlFor="add-email">
                Email Address <span className="required">*</span>
              </label>
              <input
                id="add-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="e.g. ayush@company.com"
                maxLength={255}
              />
            </div>
            <div className="form-group">
              <label htmlFor="add-phone">Phone Number</label>
              <input
                id="add-phone"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="e.g. +91-9876543210"
                maxLength={32}
              />
            </div>
            {error && <div className="form-error">{error}</div>}
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Adding...' : 'Add Candidate'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
