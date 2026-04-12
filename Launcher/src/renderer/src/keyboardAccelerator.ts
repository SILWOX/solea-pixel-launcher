/** Accélérateurs style Electron (voir https://www.electronjs.org/docs/latest/api/accelerator). */

function isMacPlatform(): boolean {
  return typeof navigator !== 'undefined' && /Mac|iPhone|iPod|iPad/i.test(navigator.platform)
}

function keyTokenToCode(token: string): string | null {
  const t = token.trim()
  if (/^Key[A-Z]$/.test(t)) return t
  if (/^Digit[0-9]$/.test(t)) return t
  if (/^F([1-9]|1[0-2])$/.test(t)) return t
  const map: Record<string, string> = {
    Comma: 'Comma',
    Period: 'Period',
    Minus: 'Minus',
    Equal: 'Equal',
    Slash: 'Slash',
    BracketLeft: 'BracketLeft',
    BracketRight: 'BracketRight',
    Backslash: 'Backslash',
    Semicolon: 'Semicolon',
    Quote: 'Quote',
    Backquote: 'Backquote',
    Tab: 'Tab',
    Space: 'Space',
    Enter: 'Enter',
    Escape: 'Escape',
    Backspace: 'Backspace',
    Delete: 'Delete',
    Insert: 'Insert',
    Home: 'Home',
    End: 'End',
    PageUp: 'PageUp',
    PageDown: 'PageDown',
    ArrowUp: 'ArrowUp',
    ArrowDown: 'ArrowDown',
    ArrowLeft: 'ArrowLeft',
    ArrowRight: 'ArrowRight',
    Plus: 'Equal'
  }
  return map[t] ?? (t.length > 0 ? t : null)
}

export function acceleratorMatches(accelerator: string, e: KeyboardEvent): boolean {
  const acc = accelerator.trim()
  if (!acc) return false
  const parts = acc.split('+').map((p) => p.trim()).filter(Boolean)
  if (parts.length < 2) return false
  const keyTok = parts[parts.length - 1]!
  const mods = parts.slice(0, -1)
  const code = keyTokenToCode(keyTok)
  if (!code || e.code !== code) return false

  let needCmdOrCtrl = false
  let needCommand = false
  let needControl = false
  let needShift = false
  let needAlt = false

  for (const m of mods) {
    if (m === 'CommandOrControl' || m === 'CmdOrCtrl') needCmdOrCtrl = true
    else if (m === 'Command' || m === 'Cmd') needCommand = true
    else if (m === 'Control' || m === 'Ctrl') needControl = true
    else if (m === 'Shift') needShift = true
    else if (m === 'Alt' || m === 'Option') needAlt = true
  }

  if (e.shiftKey !== needShift) return false
  if (e.altKey !== needAlt) return false

  const mac = isMacPlatform()
  const hasMainMod = needCmdOrCtrl || needCommand || needControl

  if (needCmdOrCtrl) {
    if (mac) {
      if (!e.metaKey && !e.ctrlKey) return false
    } else {
      if (!e.ctrlKey || e.metaKey) return false
    }
    return true
  }

  if (needCommand && !e.metaKey) return false
  if (needControl && !e.ctrlKey) return false

  if (!hasMainMod) {
    if (!/^F([1-9]|1[0-2])$/.test(code)) return false
  }

  return true
}

const IGNORE_CAPTURE = new Set([
  'ControlLeft',
  'ControlRight',
  'ShiftLeft',
  'ShiftRight',
  'AltLeft',
  'AltRight',
  'MetaLeft',
  'MetaRight'
])

function codeToAccelToken(code: string): string | null {
  if (code === 'Comma') return 'Comma'
  if (code.startsWith('Key')) return code
  if (code.startsWith('Digit')) return code
  if (/^F([1-9]|1[0-2])$/.test(code)) return code
  const map: Record<string, string> = {
    Period: 'Period',
    Minus: 'Minus',
    Equal: 'Equal',
    Slash: 'Slash',
    BracketLeft: 'BracketLeft',
    BracketRight: 'BracketRight',
    Backslash: 'Backslash',
    Semicolon: 'Semicolon',
    Quote: 'Quote',
    Backquote: 'Backquote',
    Tab: 'Tab',
    Space: 'Space',
    Enter: 'Enter',
    Escape: 'Escape',
    Backspace: 'Backspace',
    Delete: 'Delete',
    Insert: 'Insert',
    Home: 'Home',
    End: 'End',
    PageUp: 'PageUp',
    PageDown: 'PageDown',
    ArrowUp: 'ArrowUp',
    ArrowDown: 'ArrowDown',
    ArrowLeft: 'ArrowLeft',
    ArrowRight: 'ArrowRight'
  }
  return map[code] ?? null
}

/** Construit une chaîne à enregistrer (CommandOrControl pour Ctrl ou ⌘). */
export function keyboardEventToAcceleratorString(e: KeyboardEvent): string | null {
  if (e.repeat) return null
  if (e.key === 'Escape') return null
  if (IGNORE_CAPTURE.has(e.code)) return null

  const token = codeToAccelToken(e.code)
  if (!token) return null

  const parts: string[] = []
  if (e.shiftKey) parts.push('Shift')
  if (e.altKey) parts.push('Alt')
  const hasMainMod = e.ctrlKey || e.metaKey
  const isFn = /^F([1-9]|1[0-2])$/.test(e.code)
  if (hasMainMod) parts.push('CommandOrControl')
  else if (!isFn) return null

  parts.push(token)
  return parts.join('+')
}

export function formatAcceleratorForDisplay(accelerator: string): string {
  const acc = accelerator.trim()
  if (!acc) return ''
  const mac = isMacPlatform()
  return acc
    .split('+')
    .map((p) => {
      const x = p.trim()
      if (x === 'CommandOrControl' || x === 'CmdOrCtrl') return mac ? '⌘' : 'Ctrl'
      if (x === 'Command' || x === 'Cmd') return '⌘'
      if (x === 'Control' || x === 'Ctrl') return 'Ctrl'
      if (x === 'Shift') return 'Shift'
      if (x === 'Alt' || x === 'Option') return mac ? '⌥' : 'Alt'
      if (x.startsWith('Key')) return x.slice(3)
      if (x.startsWith('Digit')) return x.slice(5)
      if (x === 'Comma') return ','
      if (x === 'Period') return '.'
      if (x === 'Minus') return '-'
      if (x === 'Equal') return '='
      if (x === 'Slash') return '/'
      return x
    })
    .join(mac ? ' ' : ' + ')
}
