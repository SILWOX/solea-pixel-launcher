/** Codes courts pour le support (affichés dans les messages d’erreur). */
export const SPX = {
  LAUNCH_INTEGRITY: 'SPX-310',
  LAUNCH_AUTH: 'SPX-311',
  LAUNCH_NOT_INSTALLED: 'SPX-312',
  LAUNCH_LOADER: 'SPX-313',
  LAUNCH_GENERIC: 'SPX-319'
} as const

export function errWithCode(code: string, message: string): string {
  return `${message} [${code}]`
}
