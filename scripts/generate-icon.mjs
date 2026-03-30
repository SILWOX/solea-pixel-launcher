import { createRequire } from 'module'
import { fileURLToPath } from 'url'
import { dirname, join } from 'path'
import { existsSync, mkdirSync, writeFileSync } from 'fs'
import sharp from 'sharp'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const require = createRequire(import.meta.url)
const toIco = require('to-ico')

const srcImg = join(root, 'resources', 'app-icon.png')
const outDir = join(root, 'build')
const outIco = join(outDir, 'icon.ico')

const SIZES = [16, 24, 32, 48, 64, 128, 256]

if (!existsSync(srcImg)) {
  console.error('Icône source introuvable :', srcImg)
  process.exit(1)
}

mkdirSync(outDir, { recursive: true })

const pngBuffers = await Promise.all(
  SIZES.map((s) =>
    sharp(srcImg)
      .resize(s, s, { fit: 'cover' })
      .png()
      .toBuffer()
  )
)

const buf = await toIco(pngBuffers)
writeFileSync(outIco, buf)
console.log('OK →', outIco)
