import { useState, useEffect, useCallback } from 'react'
import './App.css'
import * as api from './api'
import Pipeline from './components/Pipeline'
import SearchBar from './components/SearchBar'
import SearchResults from './components/SearchResults'
import AddCandidateModal from './components/AddCandidateModal'
import CandidateDetailModal from './components/CandidateDetailModal'
import TransitionModal from './components/TransitionModal'
import Toast from './components/Toast'

function App() {
  const [pipeline, setPipeline] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Search
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState(null)
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState(null)
  const [searchType, setSearchType] = useState(null) // 'name' | 'nl'

  // Modals
  const [showAddModal, setShowAddModal] = useState(false)
  const [selectedCandidateId, setSelectedCandidateId] = useState(null)
  const [transitionInfo, setTransitionInfo] = useState(null)

  // Toast
  const [toasts, setToasts] = useState([])

  const showToast = useCallback((message, type = 'success') => {
    const id = Date.now()
    setToasts(t => [...t, { id, message, type }])
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 3500)
  }, [])

  const loadPipeline = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await api.fetchPipeline()
      setPipeline(data)
    } catch (err) {
      setError(err.message || 'Failed to load pipeline')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadPipeline()
  }, [loadPipeline])

  const handleSearch = async (query) => {
    const q = query.trim()
    if (!q) {
      setSearchResults(null)
      setSearchType(null)
      setSearchError(null)
      return
    }

    setSearchLoading(true)
    setSearchError(null)
    setSearchResults(null)
    setSearchType(null)

    try {
      // Step 1: Route the query using OpenNLP
      const route = await api.routeQuery(q)

      if (route.intent === 'NATURAL_LANGUAGE') {
        // Step 2a: NL query → Gemini + SQL
        setSearchType('nl')
        const nlResult = await api.nlQuery(q)
        setSearchResults(nlResult)
      } else {
        // Step 2b: Name search → pg_trgm fuzzy
        setSearchType('name')
        const results = await api.searchCandidates(q)
        setSearchResults(results)
      }
    } catch (err) {
      setSearchError(err.message || 'Search failed')
    } finally {
      setSearchLoading(false)
    }
  }

  const handleClearSearch = () => {
    setSearchQuery('')
    setSearchResults(null)
    setSearchType(null)
    setSearchError(null)
  }

  const handleCandidateCreated = () => {
    setShowAddModal(false)
    loadPipeline()
    showToast('Candidate registered successfully')
  }

  const handleTransition = (candidateId, targetStage, label) => {
    setTransitionInfo({ candidateId, targetStage, label })
  }

  const handleTransitionComplete = () => {
    setTransitionInfo(null)
    loadPipeline()
    // If detail modal is open, refresh it
    if (selectedCandidateId) {
      const id = selectedCandidateId
      setSelectedCandidateId(null)
      setTimeout(() => setSelectedCandidateId(id), 50)
    }
    showToast('Stage transition completed')
  }

  const handleSeed = async () => {
    try {
      const res = await api.seedData()
      showToast(res.message || 'Demo data seeded')
      loadPipeline()
    } catch (err) {
      showToast(err.message || 'Failed to seed data', 'error')
    }
  }

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <div className="header-left">
          <h1 className="header-title">
            <span className="dot"></span>
            Hiring Pipeline
          </h1>
          <p className="header-subtitle">Manage candidates through each hiring stage</p>
        </div>
        <div className="header-right">
          <button className="btn btn-secondary btn-sm" onClick={handleSeed}>
            Seed Demo Data
          </button>
          <button className="btn btn-primary" onClick={() => setShowAddModal(true)}>
            + Add Candidate
          </button>
        </div>
      </header>

      {/* Main */}
      <div className="main-content">
        {/* Search */}
        <SearchBar
          query={searchQuery}
          onQueryChange={setSearchQuery}
          onSearch={handleSearch}
          onClear={handleClearSearch}
          loading={searchLoading}
        />

        {/* Search Results */}
        {(searchResults || searchLoading || searchError) && (
          <SearchResults
            results={searchResults}
            type={searchType}
            loading={searchLoading}
            error={searchError}
            onSelectCandidate={(id) => setSelectedCandidateId(id)}
            onClear={handleClearSearch}
          />
        )}

        {/* Pipeline */}
        {loading ? (
          <div className="loading">
            <div className="spinner"></div>
            <span className="loading-text">Loading pipeline...</span>
          </div>
        ) : error ? (
          <div className="empty-state">
            <p className="empty-state-title">Failed to load pipeline</p>
            <p className="empty-state-text">{error}</p>
            <button className="btn btn-secondary btn-sm" onClick={loadPipeline} style={{ marginTop: 12 }}>
              Retry
            </button>
          </div>
        ) : (
          <Pipeline
            data={pipeline}
            onSelectCandidate={(id) => setSelectedCandidateId(id)}
          />
        )}
      </div>

      {/* Modals */}
      {showAddModal && (
        <AddCandidateModal
          onClose={() => setShowAddModal(false)}
          onCreated={handleCandidateCreated}
        />
      )}

      {selectedCandidateId && (
        <CandidateDetailModal
          candidateId={selectedCandidateId}
          onClose={() => setSelectedCandidateId(null)}
          onTransition={handleTransition}
        />
      )}

      {transitionInfo && (
        <TransitionModal
          candidateId={transitionInfo.candidateId}
          targetStage={transitionInfo.targetStage}
          label={transitionInfo.label}
          onClose={() => setTransitionInfo(null)}
          onComplete={handleTransitionComplete}
          showToast={showToast}
        />
      )}

      {/* Toast */}
      <Toast toasts={toasts} />
    </div>
  )
}

export default App
