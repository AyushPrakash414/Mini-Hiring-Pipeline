/**
 * Format seconds into a human-readable duration string.
 */
export function formatDuration(seconds) {
  if (seconds == null || seconds < 0) return '—'

  const s = Math.floor(seconds)
  if (s < 60) return `${s}s`

  const minutes = Math.floor(s / 60)
  if (minutes < 60) return `${minutes}m`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) {
    const m = minutes % 60
    return m > 0 ? `${hours}h ${m}m` : `${hours}h`
  }

  const days = Math.floor(hours / 24)
  const h = hours % 24
  if (days < 30) {
    return h > 0 ? `${days}d ${h}h` : `${days}d`
  }

  const months = Math.floor(days / 30)
  const d = days % 30
  return d > 0 ? `${months}mo ${d}d` : `${months}mo`
}

/**
 * Format an ISO date string into a locale-friendly display.
 */
export function formatDate(isoString) {
  if (!isoString) return '—'
  try {
    const d = new Date(isoString)
    return d.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return isoString
  }
}
