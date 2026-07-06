<template>
  <div class="promo-image-panel">
    <section class="preview-pane">
      <div class="pane-title-row">
        <div>
          <div class="pane-title">效果预览</div>
          <div class="pane-subtitle">生成后可复制图片、保存图片或复制图片地址。</div>
        </div>
        <div class="preview-actions">
          <el-button :disabled="!imageDataUrl" @click="handleCopyImage">复制图片</el-button>
          <el-button :disabled="!imageDataUrl" @click="handleDownload">保存图片</el-button>
          <el-button :disabled="!imageDataUrl" @click="handleCopyImageUrl">复制图片地址</el-button>
        </div>
      </div>
      <div class="canvas-shell">
        <canvas ref="previewCanvasRef" class="promo-canvas" width="720" height="960"></canvas>
      </div>
    </section>

    <section class="editor-pane">
      <div class="pane-title-row">
        <div>
          <div class="pane-title">编辑图片</div>
          <div class="pane-subtitle">选择拼图模板、标签样式和文案字号，标签会渲染到预览图。</div>
        </div>
      </div>
      <el-form label-width="92px" class="editor-form">
        <el-form-item label="拼图模板">
          <el-radio-group v-model="templateCode">
            <el-radio-button label="single">单品海报</el-radio-button>
            <el-radio-button label="split">左右分栏</el-radio-button>
            <el-radio-button label="banner">横幅主图</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签样式">
          <el-radio-group v-model="labelStyle">
            <el-radio label="bar">长条标签</el-radio>
            <el-radio label="circle">圆形标签</el-radio>
            <el-radio label="notice">预告标签</el-radio>
            <el-radio label="jhs">聚划算</el-radio>
            <el-radio label="tqg">淘抢购</el-radio>
            <el-radio label="brand">品牌标签</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="文字字号">
          <el-radio-group v-model="fontSize">
            <el-radio-button :label="12">12号</el-radio-button>
            <el-radio-button :label="14">14号</el-radio-button>
            <el-radio-button :label="16">16号</el-radio-button>
            <el-radio-button :label="18">18号</el-radio-button>
            <el-radio-button :label="20">20号</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="二维码">
          <el-switch v-model="showQr" active-text="显示推广二维码" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">获取素材</el-divider>
      <el-form label-width="92px" class="editor-form">
        <el-form-item label="商品标题">
          <el-input v-model="material.title" maxlength="48" show-word-limit />
        </el-form-item>
        <el-form-item label="商品图片">
          <el-input v-model="material.imageUrl" clearable placeholder="可粘贴商品主图 URL，留空使用占位图" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="券后价">
              <el-input v-model="material.price" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="原价">
              <el-input v-model="material.originPrice" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="优惠信息">
          <el-input v-model="material.coupon" />
        </el-form-item>
        <el-form-item label="推广链接">
          <el-input v-model="material.promotionUrl" clearable />
        </el-form-item>
        <el-form-item label="文案一">
          <el-input v-model="material.copyOne" maxlength="28" show-word-limit />
        </el-form-item>
        <el-form-item label="文案二">
          <el-input v-model="material.copyTwo" maxlength="28" show-word-limit />
        </el-form-item>
      </el-form>

      <div class="editor-actions">
        <el-button type="primary" :loading="rendering" @click="renderPreview">
          <Icon icon="ep:picture" class="mr-5px" /> 生成图片
        </el-button>
        <el-button @click="syncPromotionText">
          <Icon icon="ep:edit-pen" class="mr-5px" /> 同步到文案
        </el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import QRCode from 'qrcode'
import { useClipboard } from '@vueuse/core'

const emit = defineEmits<{
  promotion: [value: string]
}>()

const message = useMessage()
const { copy } = useClipboard()
const previewCanvasRef = ref<HTMLCanvasElement>()
const imageDataUrl = ref('')
const rendering = ref(false)
const templateCode = ref<'single' | 'split' | 'banner'>('single')
const labelStyle = ref<'bar' | 'circle' | 'notice' | 'jhs' | 'tqg' | 'brand'>('bar')
const fontSize = ref(16)
const showQr = ref(true)

const material = reactive({
  title: '高佣精选爆品，限时到手价更划算',
  imageUrl: '',
  price: '39.90',
  originPrice: '99.00',
  coupon: '券后立减 20 元',
  promotionUrl: 'https://example.com/cps/promotion',
  copyOne: '今日爆款推荐',
  copyTwo: '领券下单更省钱'
})

const labelTextMap: Record<typeof labelStyle.value, string> = {
  bar: '限时优惠',
  circle: '爆款',
  notice: '明日开抢',
  jhs: '聚划算',
  tqg: '淘抢购',
  brand: '品牌精选'
}

const colors = {
  red: '#f04438',
  orange: '#ff7a1a',
  ink: '#1f2937',
  muted: '#6b7280',
  line: '#e5e7eb',
  cream: '#fff7ed'
}

const loadImage = (url: string) =>
  new Promise<HTMLImageElement | null>((resolve) => {
    if (!url) {
      resolve(null)
      return
    }
    const image = new Image()
    image.crossOrigin = 'anonymous'
    image.onload = () => resolve(image)
    image.onerror = () => resolve(null)
    image.src = url
  })

const roundRect = (
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  width: number,
  height: number,
  radius: number
) => {
  ctx.beginPath()
  ctx.moveTo(x + radius, y)
  ctx.arcTo(x + width, y, x + width, y + height, radius)
  ctx.arcTo(x + width, y + height, x, y + height, radius)
  ctx.arcTo(x, y + height, x, y, radius)
  ctx.arcTo(x, y, x + width, y, radius)
  ctx.closePath()
}

const drawWrappedText = (
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
  maxLines = 2
) => {
  const chars = text.split('')
  let line = ''
  let currentY = y
  let lines = 0
  for (const char of chars) {
    const testLine = line + char
    if (ctx.measureText(testLine).width > maxWidth && line) {
      ctx.fillText(line, x, currentY)
      line = char
      currentY += lineHeight
      lines += 1
      if (lines >= maxLines - 1) break
    } else {
      line = testLine
    }
  }
  if (line) ctx.fillText(line, x, currentY)
}

const drawProductImage = async (ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number) => {
  const image = await loadImage(material.imageUrl)
  roundRect(ctx, x, y, width, height, 18)
  ctx.save()
  ctx.clip()
  if (image) {
    const ratio = Math.max(width / image.width, height / image.height)
    const drawWidth = image.width * ratio
    const drawHeight = image.height * ratio
    ctx.drawImage(image, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight)
  } else {
    const gradient = ctx.createLinearGradient(x, y, x + width, y + height)
    gradient.addColorStop(0, '#fef3c7')
    gradient.addColorStop(1, '#fed7aa')
    ctx.fillStyle = gradient
    ctx.fillRect(x, y, width, height)
    ctx.fillStyle = colors.orange
    ctx.font = '700 42px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('商品图片', x + width / 2, y + height / 2)
    ctx.textAlign = 'left'
  }
  ctx.restore()
}

const drawLabel = (ctx: CanvasRenderingContext2D, x: number, y: number) => {
  const label = labelTextMap[labelStyle.value]
  ctx.save()
  if (labelStyle.value === 'circle') {
    ctx.fillStyle = colors.red
    ctx.beginPath()
    ctx.arc(x + 52, y + 52, 52, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#ffffff'
    ctx.font = '700 22px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(label, x + 52, y + 58)
    ctx.textAlign = 'left'
  } else {
    const bg = labelStyle.value === 'brand' ? '#111827' : labelStyle.value === 'notice' ? '#2563eb' : colors.red
    ctx.fillStyle = bg
    roundRect(ctx, x, y, 170, 46, 23)
    ctx.fill()
    ctx.fillStyle = '#ffffff'
    ctx.font = '700 22px sans-serif'
    ctx.fillText(label, x + 22, y + 30)
  }
  ctx.restore()
}

const drawQr = async (ctx: CanvasRenderingContext2D, x: number, y: number, width: number) => {
  if (!showQr.value) return
  const qrUrl = await QRCode.toDataURL(material.promotionUrl || material.title, { margin: 1, width })
  const image = await loadImage(qrUrl)
  if (!image) return
  ctx.fillStyle = '#ffffff'
  roundRect(ctx, x - 10, y - 10, width + 20, width + 20, 12)
  ctx.fill()
  ctx.drawImage(image, x, y, width, width)
}

const drawPoster = async () => {
  const canvas = previewCanvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  canvas.width = templateCode.value === 'banner' ? 900 : 720
  canvas.height = templateCode.value === 'banner' ? 520 : 960
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height)
  gradient.addColorStop(0, '#fff7ed')
  gradient.addColorStop(1, '#ffffff')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  if (templateCode.value === 'banner') {
    await drawProductImage(ctx, 36, 42, 360, 360)
    drawLabel(ctx, 420, 54)
    ctx.fillStyle = colors.ink
    ctx.font = `700 ${fontSize.value + 18}px sans-serif`
    drawWrappedText(ctx, material.title, 420, 146, 420, 48, 2)
    ctx.fillStyle = colors.red
    ctx.font = '700 54px sans-serif'
    ctx.fillText(`￥${material.price}`, 420, 278)
    ctx.fillStyle = colors.muted
    ctx.font = '24px sans-serif'
    ctx.fillText(`原价 ￥${material.originPrice}`, 420, 320)
    ctx.fillStyle = colors.orange
    ctx.font = `700 ${fontSize.value + 8}px sans-serif`
    ctx.fillText(material.coupon, 420, 370)
    await drawQr(ctx, 740, 338, 118)
  } else {
    const imageX = templateCode.value === 'split' ? 36 : 60
    const imageY = templateCode.value === 'split' ? 96 : 58
    const imageW = templateCode.value === 'split' ? 300 : 600
    const imageH = templateCode.value === 'split' ? 520 : 520
    await drawProductImage(ctx, imageX, imageY, imageW, imageH)
    drawLabel(ctx, imageX + 24, imageY + 24)

    const textX = templateCode.value === 'split' ? 374 : 60
    const textY = templateCode.value === 'split' ? 130 : 650
    const textW = templateCode.value === 'split' ? 286 : 600
    ctx.fillStyle = colors.ink
    ctx.font = `700 ${fontSize.value + 16}px sans-serif`
    drawWrappedText(ctx, material.title, textX, textY, textW, 44, 2)
    ctx.fillStyle = colors.red
    ctx.font = '700 58px sans-serif'
    ctx.fillText(`￥${material.price}`, textX, textY + 126)
    ctx.fillStyle = colors.muted
    ctx.font = '24px sans-serif'
    ctx.fillText(`原价 ￥${material.originPrice}`, textX, textY + 166)
    ctx.fillStyle = colors.orange
    ctx.font = `700 ${fontSize.value + 8}px sans-serif`
    ctx.fillText(material.coupon, textX, textY + 218)
    ctx.fillStyle = colors.ink
    ctx.font = `${fontSize.value + 4}px sans-serif`
    ctx.fillText(material.copyOne, textX, textY + 272)
    ctx.fillText(material.copyTwo, textX, textY + 310)
    await drawQr(ctx, templateCode.value === 'split' ? 532 : 520, templateCode.value === 'split' ? 762 : 760, 128)
  }

  imageDataUrl.value = canvas.toDataURL('image/png')
}

const renderPreview = async () => {
  rendering.value = true
  try {
    await nextTick()
    await drawPoster()
    message.success('推广图已生成')
  } finally {
    rendering.value = false
  }
}

const ensureImageDataUrl = async () => {
  if (!imageDataUrl.value) await renderPreview()
  return imageDataUrl.value
}

const handleCopyImage = async () => {
  const dataUrl = await ensureImageDataUrl()
  const clipboard = navigator.clipboard as Clipboard & {
    write?: (items: ClipboardItem[]) => Promise<void>
  }
  if (clipboard?.write && window.ClipboardItem) {
    const blob = await (await fetch(dataUrl)).blob()
    await clipboard.write([new ClipboardItem({ [blob.type]: blob })])
    message.success('图片已复制')
    return
  }
  await copy(dataUrl)
  message.success('当前浏览器不支持直接复制图片，已复制图片地址')
}

const handleDownload = async () => {
  const dataUrl = await ensureImageDataUrl()
  const link = document.createElement('a')
  link.href = dataUrl
  link.download = `cps-promo-${Date.now()}.png`
  link.click()
}

const handleCopyImageUrl = async () => {
  await copy(await ensureImageDataUrl())
  message.success('图片地址已复制')
}

const syncPromotionText = () => {
  emit(
    'promotion',
    [material.title, material.coupon, `券后价 ￥${material.price}`, material.copyOne, material.copyTwo, material.promotionUrl]
      .filter(Boolean)
      .join('\n')
  )
  message.success('已同步到文案编辑区')
}

watch(
  () => ({ ...material, templateCode: templateCode.value, labelStyle: labelStyle.value, fontSize: fontSize.value, showQr: showQr.value }),
  () => drawPoster(),
  { deep: true }
)

onMounted(drawPoster)
</script>

<style scoped>
.promo-image-panel {
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(420px, 1.1fr);
  gap: 16px;
}

.preview-pane,
.editor-pane {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color-page);
}

.pane-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.pane-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.pane-subtitle {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.preview-actions,
.editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.canvas-shell {
  display: flex;
  min-height: 520px;
  align-items: center;
  justify-content: center;
  overflow: auto;
  padding: 12px;
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.promo-canvas {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 8px 28px rgb(15 23 42 / 12%);
}

.editor-form :deep(.el-radio-group) {
  gap: 8px 12px;
}

.editor-actions {
  margin-top: 14px;
}

@media (max-width: 1280px) {
  .promo-image-panel {
    grid-template-columns: 1fr;
  }
}
</style>
