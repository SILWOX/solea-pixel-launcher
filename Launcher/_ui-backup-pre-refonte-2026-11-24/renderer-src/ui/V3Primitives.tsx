import type { ReactNode, ButtonHTMLAttributes, HTMLAttributes } from 'react'

export function V3Surface({
  children,
  className = '',
  soft = false,
  ...rest
}: HTMLAttributes<HTMLDivElement> & { soft?: boolean }) {
  return (
    <div className={`v3-surface${soft ? ' v3-surface--soft' : ''}${className ? ` ${className}` : ''}`} {...rest}>
      {children}
    </div>
  )
}

export function V3Button({
  children,
  className = '',
  accent = false,
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { accent?: boolean }) {
  return (
    <button className={`v3-btn${accent ? ' v3-btn--accent' : ''}${className ? ` ${className}` : ''}`} {...rest}>
      {children}
    </button>
  )
}

export function V3Tab({
  children,
  className = '',
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { children: ReactNode }) {
  return (
    <button className={`v3-tab${className ? ` ${className}` : ''}`} {...rest}>
      {children}
    </button>
  )
}
