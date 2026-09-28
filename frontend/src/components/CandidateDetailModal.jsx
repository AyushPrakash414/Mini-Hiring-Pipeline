import { useState, useEffect } from 'react'
import * as api from '../api'
import { formatDuration, formatDate } from '../utils'

const NEXT_STAGE = {
  APPLIED: 'SCREENING',
  SCREENING: 'INTERVIEW',
  INTERVIEW: 'OFFER',
  OFFER: 'HIRED',
}

const NEXT_LABEL = {
  APPLIED: 'Move to Screening',
  SCREENING: 'Move to Interview',
  INTERVIEW: 'Move to Offer',
  OFFER: 'Mark as Hired',
}

export default function CandidateDetailModal({ candidateId, onClose, onTransition }) {
  const [candidate, setCandidate] = useState(null)
  const [history, setHistory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        setLoading(true)
        setError(null)
        const [c, h] = await Promise.all([
          api.fetchCandidate(candidateId),
          api.fetchCandidateHistory(candidateId),
        ])
        if (!cancelled) {
          setCandidate(c)
          setHistory(h)
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load candidate')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [candidateId])

  const stage = candidate?.currentStage
  const isTerminal = stage === 'HIRED' || stage === 'REJECTED'
  const nextStage = NEXT_STAGE[stage]
  const nextLabel = NEXT_LABEL[stage]

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-labelledby="detail-modal-title" aria-modal="true" style={{ maxWidth: 600 }}>
        <div className="modal-header">
          <h2 id="detail-modal-title">Candidate Details</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">&times;</button>
        </div>
        <div className="modal-body">
          {loading ? (
            <div className="loading">
              <div className="spinner"></div>
              <span className="loading-text">Loading...</span>
            </div>
          ) : error ? (
            <div className="form-error">{error}</div>
          ) : candidate ? (
            <>
              {/* Info */}
              <div className="detail-section">
                <div className="detail-info-grid">
                  <div className="detail-info-item">
                    <div className="detail-info-label">Name</div>
                    <div className="detail-info-value">{candidate.name}</div>
                  </div>
                  <div className="detail-info-item">
                    <div className="detail-info-label">Email</div>
                    <div className="detail-info-value">{candidate.email}</div>
                  </div>
                  <div className="detail-info-item">
                    <div className="detail-info-label">Phone</div>
                    <div className="detail-info-value">{candidate.phone || '—'}</div>
                  </div>
                  <div className="detail-info-item">
                    <div className="detail-info-label">Current Stage</div>
                    <div className="detail-info-value">
                      <span className={`stage-badge stage-${stage}`}>{stage}</span>
                    </div>
                  </div>
                  <div className="detail-info-item">
                    <div className="detail-info-label">Stage Since</div>
                    <div className="detail-info-value">
                      {candidate.stageStartedAt ? formatDate(candidate.stageStartedAt) : '—'}
                    </div>
                  </div>
                  <div className="detail-info-item">
                    <div className="detail-info-label">Time in Stage</div>
                    <div className="detail-info-value">
                      {candidate.timeInCurrentStageSeconds != null
                        ? formatDuration(candidate.timeInCurrentStageSeconds)
                        : '—'}
                    </div>
                  </div>
                </div>
              </div>

              {/* Actions */}
              {isTerminal ? (
                <div className="detail-terminal">
                  {stage === 'HIRED' ? '✓ Hired — no further actions' : '✕ Rejected — no further actions'}
                </div>
              ) : (
                <div className="detail-actions">
                  {nextStage && (
                    <button
                      className="btn btn-primary btn-sm"
                      onClick={() => onTransition(candidateId, nextStage, nextLabel)}
                    >
                      {nextLabel}
                    </button>
                  )}
                  <button
                    className="btn btn-danger btn-sm"
                    onClick={() => onTransition(candidateId, 'REJECTED', 'Reject Candidate')}
                  >
                    Reject
                  </button>
                </div>
              )}

              {/* History Timeline */}
              <div className="detail-section" style={{ marginTop: 16 }}>
                <div className="timeline-title">
                  Stage History ({history?.length || 0} events)
                </div>
                {history && history.length > 0 ? (
                  <div className="timeline">
                    {history.map((h, i) => {
                      const isCurrent = i === history.length - 1
                      return (
                        <div className={`timeline-item ${isCurrent ? 'current' : ''}`} key={h.id || i}>
                          <div className="timeline-item-header">
                            {h.fromStage ? (
                              <>
                                <span className={`stage-badge stage-${h.fromStage}`}>{h.fromStage}</span>
                                <span className="timeline-arrow">→</span>
                                <span className={`stage-badge stage-${h.toStage}`}>{h.toStage}</span>
                              </>
                            ) : (
                              <span className={`stage-badge stage-${h.toStage}`}>{h.toStage}</span>
                            )}
                            {isCurrent && <span className="timeline-current-label">Current</span>}
                            <span className="timeline-item-time">{formatDate(h.changedAt)}</span>
                          </div>
                          {h.reason && (
                            <div className="timeline-item-reason">{h.reason}</div>
                          )}
                        </div>
                      )
                    })}
                  </div>
                ) : (
                  <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No history records.</p>
                )}
              </div>
            </>
          ) : null}
        </div>
      </div>
    </div>
  )
}
