/**
 * Analyse locale du dernier crash-report (règles simples, pas d’IA).
 */

export type CrashActionId =
  | 'open_ram_settings'
  | 'open_crash_file'
  | 'open_reinstall_confirm'
  | 'open_account'
  | 'open_java_download'

export type CrashDiagAction = { id: CrashActionId; labelKey: string }

export type CrashAnalysis = {
  summaryKey: string
  actions: CrashDiagAction[]
}

function uniqActions(actions: CrashDiagAction[]): CrashDiagAction[] {
  const seen = new Set<CrashActionId>()
  const out: CrashDiagAction[] = []
  for (const a of actions) {
    if (seen.has(a.id)) continue
    seen.add(a.id)
    out.push(a)
  }
  return out
}

export function analyzeCrashText(text: string): CrashAnalysis {
  const s = text
  const low = s.toLowerCase()

  const openCrash: CrashDiagAction = {
    id: 'open_crash_file',
    labelKey: 'home.crashDiag.action.openCrash'
  }

  if (/outofmemoryerror|java\.lang\.outofmemory|heap space|gc overhead limit|direct buffer memory/i.test(s)) {
    return {
      summaryKey: 'home.crashDiag.summary.oom',
      actions: uniqActions([
        { id: 'open_ram_settings', labelKey: 'home.crashDiag.action.ramSettings' },
        openCrash
      ])
    }
  }

  if (
    /invalid.?session|401|403|not authenticated|authlib|yggdrasil|microsoft.*oauth|com\.mojang\.auth/i.test(low)
  ) {
    return {
      summaryKey: 'home.crashDiag.summary.auth',
      actions: uniqActions([
        { id: 'open_account', labelKey: 'home.crashDiag.action.account' },
        openCrash
      ])
    }
  }

  if (
    /modloadingexception|loaderexception|incompatible mod set|missing mods|could not find required mod|fabric-loader|forge mod/i.test(
      low
    )
  ) {
    return {
      summaryKey: 'home.crashDiag.summary.mods',
      actions: uniqActions([
        { id: 'open_reinstall_confirm', labelKey: 'home.crashDiag.action.reinstall' },
        openCrash
      ])
    }
  }

  if (/unsupportedclassversionerror|class file version|has been compiled by a more recent version/i.test(low)) {
    return {
      summaryKey: 'home.crashDiag.summary.java',
      actions: uniqActions([
        { id: 'open_java_download', labelKey: 'home.crashDiag.action.java' },
        openCrash
      ])
    }
  }

  return {
    summaryKey: 'home.crashDiag.summary.unknown',
    actions: uniqActions([openCrash])
  }
}
