import { useState } from 'react'
import * as api from '../api'

export default function TransitionModal({ candidateId, targetStage, label, onClose, onComplete, showToast }) {
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const isReject = targetStage === 'REJECTED'

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    try {
      setLoading(true)
      await api.transitionStage(candidateId, targetStage, reason.trim() || null)
      onComplete()
    } catch (err) {
      setError(err.message || 'Transition failed')
      showToast(err.message || 'Transition failed', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-labelledby="transition-modal-title" aria-modal="true" style={{ maxWidth: 440 }}>
        <div className="modal-header">
          <h2 id="transition-modal-title">{label}</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">&times;</button>
        </div>
        <div className="modal-body">
          <form onSubmit={handleSubmit}>
            <p style={{ fontSize: 14, color: 'var(--text-secondary)', marginBottom: 16 }}>
              {isReject
                ? 'This candidate will be moved to REJECTED. This action is final.'
                : `This candidate will be moved to ${targetStage}.`}
            </p>
            <div className="form-group">
              <label htmlFor="transition-reason">
                Reason / Notes {isReject && <span className="required">*</span>}
              </label>
              <textarea
                id="transition-reason"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Enter reason for this transition..."
                rows={3}
                autoFocus
              />
            </div>
            {error && <div className="form-error">{error}</div>}
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
              <button
                type="submit"
                className={isReject ? 'btn btn-danger' : 'btn btn-primary'}
                disabled={loading}
              >
                {loading ? 'Processing...' : (isReject ? 'Confirm Rejection' : 'Confirm')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
