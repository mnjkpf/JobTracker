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

/** Access tokens carry the user's email as the JWT subject (see backend JwtService). */
export function getUserEmailFromToken(): string | null {
  const token = localStorage.getItem('accessToken')
  if (!token) return null
  const payload = decodeJwtPayload(token)
  const sub = payload?.sub
  return typeof sub === 'string' ? sub : null
}
