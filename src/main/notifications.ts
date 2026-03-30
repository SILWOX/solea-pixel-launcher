import { Notification } from 'electron'

export function showNativeNotification(title: string, body: string): void {
  if (!Notification.isSupported()) return
  try {
    const n = new Notification({ title, body })
    n.show()
  } catch {
    /* ignore */
  }
}
