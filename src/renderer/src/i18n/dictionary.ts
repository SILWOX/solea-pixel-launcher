import type { Locale } from './types'
import { EN } from './en'
import { FR } from './fr'

export type { Locale } from './types'
export const DICTIONARY: Record<Locale, Record<string, string>> = { en: EN, fr: FR }
