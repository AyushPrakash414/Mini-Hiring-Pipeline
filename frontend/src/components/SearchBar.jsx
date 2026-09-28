import { useState, useRef, useEffect } from 'react'

export default function SearchBar({ query, onQueryChange, onSearch, onClear, loading }) {
  const inputRef = useRef(null)

  const handleSubmit = (e) => {
    e.preventDefault()
    onSearch(query)
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Escape') {
      onClear()
      inputRef.current?.blur()
    }
  }

  return (
    <div className="search-section">
      <form onSubmit={handleSubmit}>
        <div className="search-bar">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Search candidates or ask a question..."
            aria-label="Search candidates"
          />
          {query && (
            <button type="button" className="btn btn-ghost btn-xs" onClick={onClear} aria-label="Clear search">
              ✕
            </button>
          )}
          <button type="submit" className="btn btn-primary btn-sm" disabled={loading || !query.trim()}>
            {loading ? 'Searching...' : 'Search'}
          </button>
        </div>
      </form>
    </div>
  )
}
