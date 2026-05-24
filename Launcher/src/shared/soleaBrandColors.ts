/** Jaune abeille / or — accent principal AETHER UI v3 (boutons, barres, chrome studio). */
export const SOLEA_ACCENT_BEE_GOLD = '#FFD54A'
export const SOLEA_ACCENT_BEE_GOLD_BRIGHT = '#FFE082'
export const SOLEA_ACCENT_BEE_GOLD_DEEP = '#FFB300'
export const SOLEA_ACCENT_BEE_GOLD_AMBER = '#FFAB00'

/** Orange-rouge historique du launcher (thème « Ancien SOLEA PIXEL »). */
export const SOLEA_ACCENT_LEGACY_ORANGE = '#ff6a1a'
export const SOLEA_ACCENT_LEGACY_ORANGE_DEEP = '#e85a10'

export function isLegacyOrangeAccent(hex: string): boolean {
  const h = hex.trim().toLowerCase()
  return h === SOLEA_ACCENT_LEGACY_ORANGE || h === '#ff6a1b' || h === '#ff6b1a'
}
