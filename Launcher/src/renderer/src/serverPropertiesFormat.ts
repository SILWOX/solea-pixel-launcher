/** Parse / fusion server.properties (renderer, sans fs). */

export function parseServerProperties(content: string): Map<string, string> {
  const m = new Map<string, string>()
  for (const line of content.split('\n')) {
    const s = line.replace(/\r$/, '').trim()
    if (!s || s.startsWith('#')) continue
    const i = s.indexOf('=')
    if (i <= 0) continue
    const k = s.slice(0, i).trim()
    let v = s.slice(i + 1).trim()
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1)
    }
    m.set(k, v)
  }
  return m
}

/** Extrait un texte lisible depuis motd (JSON ou chaîne). */
export function motdToPlain(stored: string | undefined): string {
  if (!stored) return ''
  const t = stored.trim()
  try {
    const parsed = JSON.parse(t) as unknown
    if (typeof parsed === 'string') return parsed
    if (Array.isArray(parsed)) {
      const parts: string[] = []
      for (const el of parsed) {
        if (typeof el === 'string') parts.push(el)
        else if (el && typeof el === 'object' && typeof (el as { text?: string }).text === 'string') {
          parts.push((el as { text: string }).text)
        }
      }
      return parts.join('')
    }
  } catch {
    /* brut */
  }
  if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
    return t.slice(1, -1).replace(/\\"/g, '"')
  }
  return t
}

export function plainToMotd(plain: string): string {
  return JSON.stringify(plain)
}

export function parseBoolProp(raw: string | undefined, defaultVal: boolean): boolean {
  const v = (raw ?? '').trim().toLowerCase()
  if (v === 'true') return true
  if (v === 'false') return false
  return defaultVal
}

/** Retire les lignes clé=valeur pour ces clés puis ajoute les nouvelles valeurs en fin (préserve commentaires et clés non gérées). */
export function mergeManagedKeysIntoRaw(raw: string, updates: Record<string, string>): string {
  const managed = new Set(Object.keys(updates))
  const lines = raw.split('\n')
  const out: string[] = []
  for (const line of lines) {
    const s = line.replace(/\r$/, '')
    const t = s.trim()
    if (!t || t.startsWith('#')) {
      out.push(s)
      continue
    }
    const eq = t.indexOf('=')
    if (eq <= 0) {
      out.push(s)
      continue
    }
    const k = t.slice(0, eq).trim()
    if (managed.has(k)) continue
    out.push(s)
  }
  const body = out.join('\n').replace(/\s+$/, '')
  const additions = Object.entries(updates).map(([k, v]) => `${k}=${v}`)
  if (!body) return `${additions.join('\n')}\n`
  return `${body}\n${additions.join('\n')}\n`
}

const DIFFICULTIES = new Set(['peaceful', 'easy', 'normal', 'hard'])
const GAMEMODES = new Set(['survival', 'creative', 'adventure', 'spectator'])

export type ServerPropsForm = {
  serverPort: string
  maxPlayers: string
  viewDistance: string
  simulationDistance: string
  spawnProtection: string
  difficulty: 'peaceful' | 'easy' | 'normal' | 'hard'
  gamemode: 'survival' | 'creative' | 'adventure' | 'spectator'
  motdPlain: string
  onlineMode: boolean
  pvp: boolean
  allowFlight: boolean
  whiteList: boolean
  enforceWhitelist: boolean
  enableCommandBlock: boolean
  spawnMonsters: boolean
  spawnNpcs: boolean
  spawnAnimals: boolean
  forceGamemode: boolean
  hardcore: boolean
  enforceSecureProfile: boolean
}

export function defaultFormFromMap(m: Map<string, string>): ServerPropsForm {
  const d = (m.get('difficulty') ?? 'easy').toLowerCase()
  const g = (m.get('gamemode') ?? 'survival').toLowerCase()
  const vd = m.get('view-distance') ?? '10'
  return {
    serverPort: m.get('server-port') ?? '25565',
    maxPlayers: m.get('max-players') ?? '20',
    viewDistance: vd,
    simulationDistance: m.get('simulation-distance') ?? vd,
    spawnProtection: m.get('spawn-protection') ?? '16',
    difficulty: (DIFFICULTIES.has(d) ? d : 'easy') as ServerPropsForm['difficulty'],
    gamemode: (GAMEMODES.has(g) ? g : 'survival') as ServerPropsForm['gamemode'],
    motdPlain: motdToPlain(m.get('motd')),
    onlineMode: parseBoolProp(m.get('online-mode'), true),
    pvp: parseBoolProp(m.get('pvp'), true),
    allowFlight: parseBoolProp(m.get('allow-flight'), false),
    whiteList: parseBoolProp(m.get('white-list'), false),
    enforceWhitelist: parseBoolProp(m.get('enforce-whitelist'), false),
    enableCommandBlock: parseBoolProp(m.get('enable-command-block'), false),
    spawnMonsters: parseBoolProp(m.get('spawn-monsters'), true),
    spawnNpcs: parseBoolProp(m.get('spawn-npcs'), true),
    spawnAnimals: parseBoolProp(m.get('spawn-animals'), true),
    forceGamemode: parseBoolProp(m.get('force-gamemode'), false),
    hardcore: parseBoolProp(m.get('hardcore'), false),
    enforceSecureProfile: parseBoolProp(m.get('enforce-secure-profile'), true)
  }
}

export function formToUpdates(f: ServerPropsForm): Record<string, string> {
  return {
    'server-port': f.serverPort.trim(),
    'max-players': f.maxPlayers.trim(),
    'view-distance': f.viewDistance.trim(),
    'simulation-distance': f.simulationDistance.trim(),
    'spawn-protection': f.spawnProtection.trim(),
    difficulty: f.difficulty,
    gamemode: f.gamemode,
    motd: plainToMotd(f.motdPlain),
    'online-mode': f.onlineMode ? 'true' : 'false',
    pvp: f.pvp ? 'true' : 'false',
    'allow-flight': f.allowFlight ? 'true' : 'false',
    'white-list': f.whiteList ? 'true' : 'false',
    'enforce-whitelist': f.enforceWhitelist ? 'true' : 'false',
    'enable-command-block': f.enableCommandBlock ? 'true' : 'false',
    'spawn-monsters': f.spawnMonsters ? 'true' : 'false',
    'spawn-npcs': f.spawnNpcs ? 'true' : 'false',
    'spawn-animals': f.spawnAnimals ? 'true' : 'false',
    'force-gamemode': f.forceGamemode ? 'true' : 'false',
    hardcore: f.hardcore ? 'true' : 'false',
    'enforce-secure-profile': f.enforceSecureProfile ? 'true' : 'false'
  }
}

export function validatePropsForm(f: ServerPropsForm): string | null {
  const port = parseInt(f.serverPort.trim(), 10)
  if (!Number.isFinite(port) || port < 1024 || port > 65535) return 'port'
  const mp = parseInt(f.maxPlayers.trim(), 10)
  if (!Number.isFinite(mp) || mp < 1 || mp > 999) return 'maxPlayers'
  const vd = parseInt(f.viewDistance.trim(), 10)
  if (!Number.isFinite(vd) || vd < 2 || vd > 32) return 'viewDistance'
  const sd = parseInt(f.simulationDistance.trim(), 10)
  if (!Number.isFinite(sd) || sd < 3 || sd > 32) return 'simulationDistance'
  const sp = parseInt(f.spawnProtection.trim(), 10)
  if (!Number.isFinite(sp) || sp < 0 || sp > 512) return 'spawnProtection'
  return null
}
