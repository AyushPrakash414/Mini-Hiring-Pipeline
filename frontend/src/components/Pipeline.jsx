import { formatDuration } from '../utils'

const STAGE_ORDER = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'HIRED', 'REJECTED']

export default function Pipeline({ data, onSelectCandidate }) {
  if (!data) return null

  return (
    <div className="pipeline">
      {STAGE_ORDER.map((stage) => {
        const candidates = data[stage] || []
        return (
          <div className="pipeline-column" key={stage}>
            <div className="pipeline-column-header">
              <div className="pipeline-column-name">
                <div className={`stage-dot stage-dot-${stage}`}></div>
                <h3>{stage}</h3>
              </div>
              <span className="pipeline-count">{candidates.length}</span>
            </div>
            <div className="pipeline-cards">
              {candidates.length === 0 ? (
                <div className="empty-state" style={{ padding: '20px 8px' }}>
                  <p className="empty-state-text" style={{ fontSize: '12px' }}>No candidates</p>
                </div>
              ) : (
                candidates.map((c) => (
                  <CandidateCard
                    key={c.id}
                    candidate={c}
                    onClick={() => onSelectCandidate(c.id)}
                  />
                ))
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}

function CandidateCard({ candidate, onClick }) {
  const timeStr = candidate.timeInCurrentStageSeconds != null
    ? formatDuration(candidate.timeInCurrentStageSeconds)
    : null

  return (
    <div
      className="candidate-card"
      onClick={onClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
      aria-label={`View ${candidate.name}`}
    >
      <div className="candidate-card-name">{candidate.name}</div>
      <div className="candidate-card-email">{candidate.email}</div>
      <div className="candidate-card-footer">
        {timeStr && (
          <span className="candidate-card-time">{timeStr} in stage</span>
        )}
        <span className={`stage-badge stage-${candidate.currentStage}`} style={{ marginLeft: 'auto' }}>
          {candidate.currentStage}
        </span>
      </div>
    </div>
  )
}
