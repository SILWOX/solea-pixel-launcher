/**
 * Hôtes autorisés pour shell.openExternal depuis le renderer (évite les liens arbitraires).
 * Toute URL doit être https://. Ajouter ici les domaines nécessaires à l’auth ou aux ressources officielles.
 */
const ALLOWED_HOSTS = new Set<string>([
  'discord.gg',
  'modrinth.com',
  'www.modrinth.com',
  'api.modrinth.com',
  'login.live.com',
  'login.microsoftonline.com',
  'account.microsoft.com',
  'www.microsoft.com',
  'microsoft.com',
  'www.xbox.com',
  'xbox.com',
  'minecraft.net',
  'www.minecraft.net',
  'help.minecraft.net'
])

function hostAllowed(hostname: string): boolean {
  const h = hostname.toLowerCase()
  if (ALLOWED_HOSTS.has(h)) return true
  if (h.endsWith('.discord.gg')) return true
  if (h.endsWith('.modrinth.com')) return true
  if (h.endsWith('.microsoft.com')) return true
  if (h.endsWith('.microsoftonline.com')) return true
  if (h.endsWith('.live.com')) return true
  return false
}

/** true si l’URL peut être ouverte via IPC « open-external ». */
export function isUrlAllowedForExternalOpen(urlString: string): boolean {
  if (typeof urlString !== 'string' || !urlString.startsWith('https://')) return false
  try {
    const u = new URL(urlString)
    if (u.protocol !== 'https:') return false
    return hostAllowed(u.hostname)
  } catch {
    return false
  }
}
