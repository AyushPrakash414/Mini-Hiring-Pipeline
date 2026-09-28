export default function SearchResults({ results, type, loading, error, onSelectCandidate, onClear }) {
  if (loading) {
    return (
      <div className="search-results">
        <div className="loading">
          <div className="spinner"></div>
          <span className="loading-text">Searching...</span>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="search-results">
        <div className="search-error-banner">
          {error}
        </div>
      </div>
    )
  }

  if (!results) return null

  // Natural Language results
  if (type === 'nl') {
    return <NlResults data={results} onSelectCandidate={onSelectCandidate} onClear={onClear} />
  }

  // Name search results
  return <NameResults results={results} onSelectCandidate={onSelectCandidate} onClear={onClear} />
}

function NameResults({ results, onSelectCandidate, onClear }) {
  if (!results || results.length === 0) {
    return (
      <div className="search-results">
        <div className="empty-state" style={{ padding: '24px' }}>
          <p className="empty-state-title">No candidates found</p>
          <p className="empty-state-text">Try a different name or spelling.</p>
          <button className="btn btn-ghost btn-sm" onClick={onClear} style={{ marginTop: 8 }}>
            Clear search
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="search-results">
      <div className="search-results-header">
        <h3>Search Results</h3>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="search-results-count">{results.length} found</span>
          <button className="btn btn-ghost btn-xs" onClick={onClear}>Clear</button>
        </div>
      </div>
      {results.map((r) => (
        <div
          key={r.id}
          className="search-result-item"
          onClick={() => onSelectCandidate(r.id)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && onSelectCandidate(r.id)}
        >
          <div className="search-result-info">
            <h4>{r.name}</h4>
            <p>{r.email}</p>
          </div>
          <span className={`stage-badge stage-${r.currentStage}`}>
            {r.currentStage}
          </span>
        </div>
      ))}
    </div>
  )
}

function NlResults({ data, onSelectCandidate, onClear }) {
  // Handle denied/error statuses
  if (data.status !== 'APPROVED') {
    const reason = data.rejection_reason || data.rejectionReason || data.explanation || 'Query could not be processed.'
    return (
      <div className="search-results">
        <div className="search-results-header">
          <h3>Query Result</h3>
          <button className="btn btn-ghost btn-xs" onClick={onClear}>Clear</button>
        </div>
        <div className="search-error-banner">
          {reason}
        </div>
      </div>
    )
  }

  const rows = data.query_results || data.queryResults || []
  const explanation = data.explanation

  return (
    <div className="search-results">
      <div className="search-results-header">
        <h3>Query Result</h3>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="search-results-count">{rows.length} found</span>
          <button className="btn btn-ghost btn-xs" onClick={onClear}>Clear</button>
        </div>
      </div>
      <div className="search-nl-results">
        {explanation && (
          <div className="search-nl-explanation">{explanation}</div>
        )}
        {rows.length === 0 ? (
          <div className="empty-state" style={{ padding: '16px' }}>
            <p className="empty-state-title">No results</p>
            <p className="empty-state-text">No candidates match this query.</p>
          </div>
        ) : (
          <table className="search-nl-table">
            <thead>
              <tr>
                {Object.keys(rows[0]).map((key) => (
                  <th key={key}>{formatColumnName(key)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, i) => (
                <tr
                  key={i}
                  className="search-result-item"
                  onClick={() => {
                    if (row.id) onSelectCandidate(row.id)
                  }}
                  style={{ cursor: row.id ? 'pointer' : 'default' }}
                >
                  {Object.values(row).map((val, j) => (
                    <td key={j}>{formatCellValue(val)}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}

function formatColumnName(key) {
  return key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function formatCellValue(val) {
  if (val === null || val === undefined) return '—'
  if (typeof val === 'object') {
    // Handle PostgreSQL citext objects { type: "citext", value: "..." }
    if (val.value !== undefined) return String(val.value)
    return JSON.stringify(val)
  }
  return String(val)
}
