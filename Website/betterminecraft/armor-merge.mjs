/**
 * Extrait adapté de bs-community/skinview-utils (MIT) — merge des textures
 * d’armure Minecraft (layer_1 / layer_2, 64×32) vers une grille skin 64×64.
 * @see https://github.com/bs-community/skinview-utils/blob/master/src/process.ts
 */

function hasTransparency(context, x0, y0, w, h) {
  const imgData = context.getImageData(x0, y0, w, h)
  for (let x = 0; x < w; x++) {
    for (let y = 0; y < h; y++) {
      const offset = (x + y * w) * 4
      if (imgData.data[offset + 3] !== 0xff) return true
    }
  }
  return false
}

function computeSkinScale(width) {
  return width / 64.0
}

function fixOpaqueSkin(context, width, format1_8) {
  if (format1_8) {
    if (hasTransparency(context, 0, 0, width, width)) return
  } else {
    if (hasTransparency(context, 0, 0, width, width / 2)) return
  }

  const scale = computeSkinScale(width)
  const clearArea = (x, y, w, h) => context.clearRect(x * scale, y * scale, w * scale, h * scale)

  clearArea(40, 0, 8, 8)
  clearArea(48, 0, 8, 8)
  clearArea(32, 8, 8, 8)
  clearArea(40, 8, 8, 8)
  clearArea(48, 8, 8, 8)
  clearArea(56, 8, 8, 8)

  if (format1_8) {
    clearArea(4, 32, 4, 4)
    clearArea(8, 32, 4, 4)
    clearArea(0, 36, 4, 12)
    clearArea(4, 36, 4, 12)
    clearArea(8, 36, 4, 12)
    clearArea(12, 36, 4, 12)
    clearArea(20, 32, 8, 4)
    clearArea(28, 32, 8, 4)
    clearArea(16, 36, 4, 12)
    clearArea(20, 36, 8, 12)
    clearArea(28, 36, 4, 12)
    clearArea(32, 36, 8, 12)
    clearArea(44, 32, 4, 4)
    clearArea(48, 32, 4, 4)
    clearArea(40, 36, 4, 12)
    clearArea(44, 36, 4, 12)
    clearArea(48, 36, 4, 12)
    clearArea(52, 36, 12, 12)
    clearArea(4, 48, 4, 4)
    clearArea(8, 48, 4, 4)
    clearArea(0, 52, 4, 12)
    clearArea(4, 52, 4, 12)
    clearArea(8, 52, 4, 12)
    clearArea(12, 52, 4, 12)
    clearArea(52, 48, 4, 4)
    clearArea(56, 48, 4, 4)
    clearArea(48, 52, 4, 12)
    clearArea(52, 52, 4, 12)
    clearArea(56, 52, 4, 12)
    clearArea(60, 52, 4, 12)
  }
}

function convertSkinTo1_8(context, width) {
  context.save()
  context.scale(-1, 1)

  const scale = computeSkinScale(width)
  const copySkin = (sX, sY, w, h, dX, dY) =>
    context.drawImage(
      context.canvas,
      sX * scale,
      sY * scale,
      w * scale,
      h * scale,
      -dX * scale,
      dY * scale,
      -w * scale,
      h * scale,
    )

  copySkin(4, 16, 4, 4, 20, 48)
  copySkin(8, 16, 4, 4, 24, 48)
  copySkin(0, 20, 4, 12, 24, 52)
  copySkin(4, 20, 4, 12, 20, 52)
  copySkin(8, 20, 4, 12, 16, 52)
  copySkin(12, 20, 4, 12, 28, 52)
  copySkin(44, 16, 4, 4, 36, 48)
  copySkin(48, 16, 4, 4, 40, 48)
  copySkin(40, 20, 4, 12, 40, 52)
  copySkin(44, 20, 4, 12, 36, 52)
  copySkin(48, 20, 4, 12, 32, 52)
  copySkin(52, 20, 4, 12, 44, 52)

  context.restore()
}

export function loadSkinToCanvas(canvas, image) {
  let isOldFormat = false
  if (image.width !== image.height) {
    if (image.width === 2 * image.height) {
      isOldFormat = true
    } else {
      throw new Error(`Bad skin size: ${image.width}x${image.height}`)
    }
  }

  const context = canvas.getContext('2d', { willReadFrequently: true })
  if (isOldFormat) {
    const sideLength = image.width
    canvas.width = sideLength
    canvas.height = sideLength
    context.clearRect(0, 0, sideLength, sideLength)
    context.drawImage(image, 0, 0, sideLength, sideLength / 2.0)
    convertSkinTo1_8(context, sideLength)
    fixOpaqueSkin(context, canvas.width, false)
  } else {
    canvas.width = image.width
    canvas.height = image.height
    context.clearRect(0, 0, image.width, image.height)
    context.drawImage(image, 0, 0, canvas.width, canvas.height)
    fixOpaqueSkin(context, canvas.width, true)
  }
}

export function loadArmorToCanvas(canvas, layer1Image, layer2Image) {
  if (!layer1Image && !layer2Image) return

  const context = canvas.getContext('2d', { willReadFrequently: true })
  const width = layer1Image ? layer1Image.width : layer2Image?.width
  const height = layer1Image ? layer1Image.height : layer2Image?.height
  if (!width || !height || width !== height * 2) {
    throw new Error(`Bad armor size: ${width}x${height}`)
  }
  const scale = computeSkinScale(width)
  const copyRegion = (sX, sY, w, h, dX, dY) =>
    context.drawImage(
      canvas,
      sX * scale,
      sY * scale,
      w * scale,
      h * scale,
      dX * scale,
      dY * scale,
      w * scale,
      h * scale,
    )
  const clearArea = (x, y, w, h) => context.clearRect(x * scale, y * scale, w * scale, h * scale)

  canvas.width = width
  canvas.height = width
  context.clearRect(0, 0, width, width)
  if (layer1Image) {
    context.drawImage(layer1Image, 0, 0, width, height)
    convertSkinTo1_8(context, width)
    copyRegion(0, 0, 32, 16, 32, 0)
    clearArea(0, 0, 32, 16)
    copyRegion(16, 16, 24, 16, 16, 32)
    clearArea(16, 16, 24, 16)
    copyRegion(40, 16, 16, 16, 40, 32)
    clearArea(40, 16, 16, 16)
    copyRegion(32, 48, 16, 16, 48, 48)
    clearArea(32, 48, 16, 16)
    copyRegion(0, 16, 16, 16, 0, 32)
    clearArea(0, 16, 16, 16)
    copyRegion(16, 48, 16, 16, 0, 48)
    clearArea(16, 48, 16, 16)
  }
  if (layer2Image) {
    context.drawImage(layer2Image, 0, 0, width, height)
    convertSkinTo1_8(context, width)
  }
  fixOpaqueSkin(context, canvas.width, true)
}
