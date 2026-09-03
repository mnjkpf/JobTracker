import type { CSSProperties } from 'react'

// Status colors are now per-user hex values coming from the API (status.color),
// not a static map. These helpers turn a hex into inline styles for a badge/dot.

function hexToRgba(hex: string, alpha: number): string {
  const m = hex.replace('#', '')
  const full = m.length === 3 ? m.split('').map((c) => c + c).join('') : m
  const r = parseInt(full.slice(0, 2), 16)
  const g = parseInt(full.slice(2, 4), 16)
  const b = parseInt(full.slice(4, 6), 16)
  if ([r, g, b].some(Number.isNaN)) return `rgba(100, 116, 139, ${alpha})` // slate fallback
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/** Light background + solid colored text — for status pills/badges. */
export function statusBadgeStyle(hex: string): CSSProperties {
  return { backgroundColor: hexToRgba(hex, 0.14), color: hex }
}

/** Solid dot — for column headers. */
export function statusDotStyle(hex: string): CSSProperties {
  return { backgroundColor: hex }
}
