/** Decodes the JWT payload without verifying the signature — display only. */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch {
    return null
  }
}

/** Guest sessions have no email, so their subject is "guest:<uuid>". */
const GUEST_SUBJECT_PREFIX = 'guest:'

function getSubject(): string | null {
  const token = localStorage.getItem('accessToken')
  if (!token) return null
  const payload = decodeJwtPayload(token)
  const sub = payload?.sub
  return typeof sub === 'string' ? sub : null
}

/**
 * Access tokens carry the user's email as the JWT subject (see backend JwtService).
 * Returns null for a guest session, which has no email yet.
 */
export function getUserEmailFromToken(): string | null {
  const sub = getSubject()
  if (!sub || sub.startsWith(GUEST_SUBJECT_PREFIX)) return null
  return sub
}

/**
 * Whether the current token belongs to an anonymous session. Read straight off
 * the JWT so the UI can branch synchronously, before /account/me resolves.
 */
export function isGuestSession(): boolean {
  return getSubject()?.startsWith(GUEST_SUBJECT_PREFIX) ?? false
}
