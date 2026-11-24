/**
 * Génère les bitmaps NSIS (sidebar + header) à l'identité Solea Pixel.
 * Formats MUI : sidebar 164×314, header 150×57 — BMP 24-bit.
 */
import { createRequire } from 'module'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, writeFileSync, readFileSync } from 'fs'
import sharp from 'sharp'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const require = createRequire(import.meta.url)
const toIco = require('to-ico')

const srcImg = join(root, 'resources', 'app-icon.png')
const outDir = join(root, 'build')
const outIco = join(outDir, 'icon.ico')

const SIDEBAR_W = 164
const SIDEBAR_H = 314
const HEADER_W = 150
const HEADER_H = 57

if (!existsSync(srcImg)) {
  console.error('Icône source introuvable :', srcImg)
  process.exit(1)
}

mkdirSync(outDir, { recursive: true })

/** Convertit un PNG RGB en BMP 24-bit non compressé (requis NSIS MUI). */
function pngRgbToBmp24(pngBuffer, width, height) {
  // sharp already provides raw; this helper expects raw RGB buffer
  const rowSize = Math.floor((width * 3 + 3) / 4) * 4
  const pixelBytes = rowSize * height
  const fileSize = 54 + pixelBytes
  const buf = Buffer.alloc(fileSize)

  buf.write('BM', 0)
  buf.writeUInt32LE(fileSize, 2)
  buf.writeUInt32LE(0, 6)
  buf.writeUInt32LE(54, 10)
  buf.writeUInt32LE(40, 14)
  buf.writeInt32LE(width, 18)
  buf.writeInt32LE(height, 22) // bottom-up
  buf.writeUInt16LE(1, 26)
  buf.writeUInt16LE(24, 28)
  buf.writeUInt32LE(0, 30)
  buf.writeUInt32LE(pixelBytes, 34)
  buf.writeInt32LE(2835, 38)
  buf.writeInt32LE(2835, 42)
  buf.writeUInt32LE(0, 46)
  buf.writeUInt32LE(0, 50)

  for (let y = 0; y < height; y++) {
    const srcY = height - 1 - y
    const dstRow = 54 + y * rowSize
    for (let x = 0; x < width; x++) {
      const si = (srcY * width + x) * 3
      const di = dstRow + x * 3
      buf[di] = pngBuffer[si + 2] // B
      buf[di + 1] = pngBuffer[si + 1] // G
      buf[di + 2] = pngBuffer[si] // R
    }
  }
  return buf
}

async function writeBmp(path, rgb, width, height) {
  writeFileSync(path, pngRgbToBmp24(rgb, width, height))
  console.log('OK →', path)
}

const iconPng = readFileSync(srcImg)
const iconSidebar = await sharp(iconPng)
  .resize(88, 88, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
  .png()
  .toBuffer()

const iconHeader = await sharp(iconPng)
  .resize(36, 36, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
  .png()
  .toBuffer()

const sidebarSvg = `
<svg width="${SIDEBAR_W}" height="${SIDEBAR_H}" viewBox="0 0 ${SIDEBAR_W} ${SIDEBAR_H}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#0c0a0e"/>
      <stop offset="45%" stop-color="#08060a"/>
      <stop offset="100%" stop-color="#040308"/>
    </linearGradient>
    <radialGradient id="glow" cx="50%" cy="28%" r="55%">
      <stop offset="0%" stop-color="#ffd54a" stop-opacity="0.42"/>
      <stop offset="45%" stop-color="#ffab00" stop-opacity="0.14"/>
      <stop offset="100%" stop-color="#ffab00" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="edge" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="#ffe082" stop-opacity="0.55"/>
      <stop offset="50%" stop-color="#ffb300" stop-opacity="0.2"/>
      <stop offset="100%" stop-color="#ffb300" stop-opacity="0"/>
    </linearGradient>
  </defs>
  <rect width="100%" height="100%" fill="url(#bg)"/>
  <rect width="100%" height="100%" fill="url(#glow)"/>
  <rect x="0" y="0" width="3" height="100%" fill="url(#edge)"/>
  <rect x="12" y="248" width="140" height="1" fill="#ffd54a" opacity="0.28"/>
  <text x="82" y="272" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="11" font-weight="700" letter-spacing="2.4" fill="#ffe082">SOLEA PIXEL</text>
  <text x="82" y="292" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="9" fill="#a89f92">INSTALLATEUR</text>
</svg>`

const sidebarBase = await sharp(Buffer.from(sidebarSvg))
  .resize(SIDEBAR_W, SIDEBAR_H)
  .composite([{ input: iconSidebar, top: 78, left: Math.round((SIDEBAR_W - 88) / 2) }])
  .removeAlpha()
  .raw()
  .toBuffer({ resolveWithObject: true })

await writeBmp(join(outDir, 'installerSidebar.bmp'), sidebarBase.data, SIDEBAR_W, SIDEBAR_H)
await writeBmp(join(outDir, 'uninstallerSidebar.bmp'), sidebarBase.data, SIDEBAR_W, SIDEBAR_H)

const headerSvg = `
<svg width="${HEADER_W}" height="${HEADER_H}" viewBox="0 0 ${HEADER_W} ${HEADER_H}" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="hbg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#141018"/>
      <stop offset="100%" stop-color="#0a080c"/>
    </linearGradient>
    <linearGradient id="hline" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0%" stop-color="#ffd54a" stop-opacity="0"/>
      <stop offset="35%" stop-color="#ffd54a" stop-opacity="0.85"/>
      <stop offset="100%" stop-color="#ffab00" stop-opacity="0.15"/>
    </linearGradient>
  </defs>
  <rect width="100%" height="100%" fill="url(#hbg)"/>
  <rect x="0" y="${HEADER_H - 3}" width="100%" height="3" fill="url(#hline)"/>
  <text x="48" y="26" font-family="Segoe UI, Arial, sans-serif" font-size="12" font-weight="700" fill="#f7f2ea">Solea Pixel</text>
  <text x="48" y="42" font-family="Segoe UI, Arial, sans-serif" font-size="8" fill="#ffd54a">Launcher · Setup</text>
</svg>`

const headerBase = await sharp(Buffer.from(headerSvg))
  .resize(HEADER_W, HEADER_H)
  .composite([{ input: iconHeader, top: 10, left: 8 }])
  .removeAlpha()
  .raw()
  .toBuffer({ resolveWithObject: true })

await writeBmp(join(outDir, 'installerHeader.bmp'), headerBase.data, HEADER_W, HEADER_H)

/* Icône .ico (même pipeline que generate-icon) */
const SIZES = [16, 24, 32, 48, 64, 128, 256]
const pngBuffers = await Promise.all(
  SIZES.map((s) =>
    sharp(srcImg)
      .resize(s, s, { fit: 'cover' })
      .png()
      .toBuffer()
  )
)
writeFileSync(outIco, await toIco(pngBuffers))
console.log('OK →', outIco)

/* Petite icône header NSIS (optionnelle) */
writeFileSync(
  join(outDir, 'installerHeaderIcon.ico'),
  await toIco(
    await Promise.all(
      [16, 32, 48].map((s) =>
        sharp(srcImg).resize(s, s, { fit: 'cover' }).png().toBuffer()
      )
    )
  )
)
console.log('OK →', join(outDir, 'installerHeaderIcon.ico'))
