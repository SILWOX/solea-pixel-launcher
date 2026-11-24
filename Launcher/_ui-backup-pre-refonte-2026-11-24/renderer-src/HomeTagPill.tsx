/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useId } from 'react'
import type { HomeTagPresetId } from './homeTagPresets'

function kebabPreset(id: HomeTagPresetId): string {
  return id.replace(/_/g, '-')
}

function TagIcon({ presetId }: { presetId: HomeTagPresetId }) {
  const common = { width: 13, height: 13, viewBox: '0 0 16 16', 'aria-hidden': true as const }
  switch (presetId) {
    case 'modded_survival':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="M2 3h4v4H2V3zm8 0h4v4h-4V3zM2 9h4v4H2V9zm6 0l2-2 4 4-2 2-2-2-2 2-2-2 2-2z"
          />
        </svg>
      )
    case 'news':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="M9.5 2 4 9h3.5l-1 5 7-6.5H10L9.5 2z"
          />
        </svg>
      )
    case 'featured':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="M5 2h6a1 1 0 0 1 1 1v11l-4-3-4 3V3a1 1 0 0 1 1-1z"
          />
        </svg>
      )
    case 'most_popular':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="m8 1.8 2.1 4.3 4.7.7-3.4 3.3.8 4.7L8 12.9 3.8 14.8l.8-4.7-3.4-3.3 4.7-.7L8 1.8z"
          />
        </svg>
      )
    case 'vanilla':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="M3 3h10v10H3V3zm2 2v6h6V5H5zm3 1.5h2v3H8v-3z"
          />
        </svg>
      )
    case 'huge':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="M8 14c-2.8 0-5-2-5-4.5C3 6 8 2 8 2s5 4 5 7.5c0 2.5-2.2 4.5-5 4.5zm0-3a1.8 1.8 0 1 0 0-3.6A1.8 1.8 0 0 0 8 11z"
          />
        </svg>
      )
    case 'medium':
      return (
        <svg {...common}>
          <path fill="currentColor" d="M3 13h2.5V6H3v7zm5.25-4h2.5V4h-2.5v5zm5.25 2H16v7h-2.5V11z" />
        </svg>
      )
    case 'small':
      return (
        <svg {...common}>
          <path
            fill="currentColor"
            d="M12.5 3.5 7 9l-2-2-3 3v3h3l5.5-5.5-1.5-1.5zM3 12.5h3v1H3v-1z"
          />
        </svg>
      )
    default:
      return null
  }
}

type Props = {
  presetId: HomeTagPresetId
  label: string
  description: string
}

export function HomeTagPill({ presetId, label, description }: Props) {
  const tipId = useId()
  const k = kebabPreset(presetId)

  return (
    <span
      className={`tag-pill tag-pill--preset-${k} tag-pill--with-tip`}
      role="listitem"
    >
      <span
        className="tag-pill__surface"
        tabIndex={0}
        aria-describedby={tipId}
      >
        <span className="tag-pill__ico" aria-hidden>
          <TagIcon presetId={presetId} />
        </span>
        <span className="tag-pill__txt">{label}</span>
      </span>
      <span id={tipId} className={`tag-pill__tip tag-pill__tip--preset-${k}`} role="tooltip">
        {description}
      </span>
    </span>
  )
}
