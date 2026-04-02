/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import type { CSSProperties } from 'react'
import { useI18n } from './i18n/I18nContext'
import { RAM_RECOMMENDED_GB, clampGbToRange, clampRamAllocRange } from './memoryRam'
import './MemoryRamSlider.css'

type Props = {
  allocGb: number
  totalGiB: number
  onChangeAllocGb: (gb: number) => void
}

export function MemoryRamSlider({ allocGb, totalGiB, onChangeAllocGb }: Props) {
  const { t } = useI18n()
  const { minGb, maxGb } = clampRamAllocRange(totalGiB)
  const value = clampGbToRange(allocGb, minGb, maxGb)
  const span = maxGb - minGb
  const pct = span <= 0 ? 0 : ((value - minGb) / span) * 100

  return (
    <div
      className="memory-ram-slider"
      role="group"
      aria-label={t('settings.ram')}
      style={{ '--ram-pct': `${pct}%` } as CSSProperties}
    >
      <div className="memory-ram-top">
        <span className="memory-ram-cap-label">{t('settings.ramBarMin', { gb: minGb })}</span>
        <span className="memory-ram-cap-label memory-ram-cap-label--pc">
          {t('settings.ramPcTotal', { gb: Math.round(totalGiB) })}
        </span>
      </div>

      <div className="memory-ram-hero">
        <span className="memory-ram-value-pill">{t('settings.ramSelected', { gb: value })}</span>
      </div>

      <div className="memory-ram-range-shell">
        <input
          type="range"
          className="memory-ram-range"
          min={minGb}
          max={maxGb}
          step={1}
          value={value}
          onChange={(e) => onChangeAllocGb(Number(e.target.value))}
          aria-valuemin={minGb}
          aria-valuemax={maxGb}
          aria-valuenow={value}
          aria-valuetext={t('settings.ramAriaValue', { gb: value })}
        />
      </div>

      <p className="memory-ram-quick-label">{t('settings.ramQuickPresets')}</p>
      <div className="memory-ram-quick" role="group" aria-label={t('settings.ramQuickPresets')}>
        {RAM_RECOMMENDED_GB.map((g) => {
          if (g < minGb || g > maxGb) return null
          const on = value === g
          return (
            <button
              key={g}
              type="button"
              className={`memory-ram-quick-btn${on ? ' memory-ram-quick-btn--active' : ''}`}
              onClick={() => onChangeAllocGb(g)}
              title={t('settings.ramTickTitle', { gb: g })}
            >
              {g}G
            </button>
          )
        })}
      </div>
    </div>
  )
}
