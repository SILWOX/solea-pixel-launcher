import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { DICTIONARY, type Locale } from './dictionary'

export type TFunction = (key: string, vars?: Record<string, string | number>) => string

type I18nCtx = {
  locale: Locale
  setLocale: (l: Locale) => void
  t: TFunction
  /** Entiers avec séparateurs selon la langue (ex. 1 234). */
  formatInteger: (n: number) => string
  formatNumber: (n: number, options?: Intl.NumberFormatOptions) => string
  /** Pourcentage pour une valeur entre 0 et 100 (ex. 45 %). */
  formatPercent: (value0to100: number) => string
  formatDate: (d: Date | number, options?: Intl.DateTimeFormatOptions) => string
}

const Ctx = createContext<I18nCtx | null>(null)

function interpolate(template: string, vars?: Record<string, string | number>): string {
  if (!vars) return template
  let s = template
  for (const [k, v] of Object.entries(vars)) {
    s = s.split(`{{${k}}}`).join(String(v))
  }
  return s
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>('en')

  const setLocale = useCallback((l: Locale) => {
    setLocaleState(l === 'fr' ? 'fr' : 'en')
  }, [])

  const t = useMemo<TFunction>(() => {
    const table = DICTIONARY[locale] ?? DICTIONARY.en
    return (key, vars) => {
      const raw = table[key] ?? DICTIONARY.en[key] ?? key
      return interpolate(raw, vars)
    }
  }, [locale])

  const intlLocale = locale === 'fr' ? 'fr-FR' : 'en-US'

  const formatInteger = useCallback(
    (n: number) => new Intl.NumberFormat(intlLocale, { maximumFractionDigits: 0 }).format(n),
    [intlLocale]
  )

  const formatNumber = useCallback(
    (n: number, options?: Intl.NumberFormatOptions) => new Intl.NumberFormat(intlLocale, options).format(n),
    [intlLocale]
  )

  const formatPercent = useCallback(
    (value0to100: number) =>
      new Intl.NumberFormat(intlLocale, { style: 'percent', maximumFractionDigits: 0 }).format(
        Math.min(100, Math.max(0, value0to100)) / 100
      ),
    [intlLocale]
  )

  const formatDate = useCallback(
    (d: Date | number, options?: Intl.DateTimeFormatOptions) => {
      const date = typeof d === 'number' ? new Date(d) : d
      return new Intl.DateTimeFormat(intlLocale, { dateStyle: 'medium', ...options }).format(date)
    },
    [intlLocale]
  )

  const value = useMemo(
    () => ({ locale, setLocale, t, formatInteger, formatNumber, formatPercent, formatDate }),
    [locale, setLocale, t, formatInteger, formatNumber, formatPercent, formatDate]
  )

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>
}

export function useI18n(): I18nCtx {
  const x = useContext(Ctx)
  if (!x) throw new Error('useI18n outside I18nProvider')
  return x
}
