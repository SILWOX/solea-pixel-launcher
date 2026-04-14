export type ActuSoleaJson = {
  /** Schéma optionnel pour évolutions */
  version?: number
  /** ISO date affichable */
  updatedAt?: string
  /**
   * Texte complet : blocs séparés par une ligne contenant uniquement `___` = bulles distinctes.
   * Alternative : `segments`.
   */
  content?: string
  /** Une entrée = une bulle (prioritaire sur `content` si les deux sont présents) */
  segments?: string[]
}
