/**
 * TEST : à `true`, le lancement ne vérifie plus les mods / hash (extra mods, SHA-512).
 * Doit rester à `false` pour une diffusion publique.
 */
export const SKIP_MOD_INTEGRITY_FOR_LAUNCH = false
