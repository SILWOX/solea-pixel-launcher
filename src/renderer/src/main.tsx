import React from 'react'
import ReactDOM from 'react-dom/client'
import { App } from './App'
import { LogConsoleApp } from './LogConsoleApp'
import { DebugApp } from './DebugApp'
import { I18nProvider } from './i18n/I18nContext'
import { ToastProvider } from './ui/ToastContext'
import './typography.css'
import './styles.css'
import './theme.css'

const soleaMode = new URLSearchParams(window.location.search).get('solea')
const rootEl = document.getElementById('root')!

if (soleaMode === 'log') {
  ReactDOM.createRoot(rootEl).render(
    <React.StrictMode>
      <I18nProvider>
        <LogConsoleApp />
      </I18nProvider>
    </React.StrictMode>
  )
} else if (soleaMode === 'debug') {
  ReactDOM.createRoot(rootEl).render(
    <React.StrictMode>
      <I18nProvider>
        <DebugApp />
      </I18nProvider>
    </React.StrictMode>
  )
} else {
  ReactDOM.createRoot(rootEl).render(
    <React.StrictMode>
      <I18nProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </I18nProvider>
    </React.StrictMode>
  )
}
