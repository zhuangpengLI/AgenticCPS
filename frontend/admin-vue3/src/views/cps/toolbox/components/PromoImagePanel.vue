<template>
  <div class="promo-image-panel">
    <el-alert
      class="tutorial-alert"
      type="warning"
      show-icon
      :closable="false"
      title="多样的拼图模板、丰富的信息标签、海量的图片素材，让你轻松、快速获得你的专属营销推广图！ 查看详细使用教程>"
    />

    <div class="promo-workbench">
      <section class="preview-column">
        <div class="pane-title">效果预览</div>
        <div class="poster-preview" @click="cycleActiveLabel">
          <div class="main-photo" :style="previewMainStyle">
            <button class="upload-box" @click.stop="triggerUpload(0)">
              <Icon icon="ep:plus" />
              <span>上传图片</span>
            </button>
          </div>
          <div class="price-strip">
            <div class="price-left">
              <span>{{ labelDrafts.textOne }}</span>
              <strong>{{ labelDrafts.textTwo }}</strong>
            </div>
            <div class="price-right">
              <strong>{{ labelDrafts.textThree }}</strong>
              <span>{{ labelDrafts.textFour }}</span>
            </div>
          </div>
          <div class="thumb-grid">
            <button
              v-for="slot in [1, 2]"
              :key="slot"
              class="upload-box small"
              :style="slotStyle(slot)"
              @click.stop="triggerUpload(slot)"
            >
              <Icon icon="ep:plus" />
              <span>上传图片</span>
            </button>
          </div>
          <canvas ref="previewCanvasRef" class="hidden-canvas" width="560" height="840"></canvas>
        </div>
      </section>

      <section class="editor-column">
        <div class="pane-title">编辑图片</div>
        <div class="editor-card">
          <div class="editor-label">选择拼图模板：</div>
          <div class="template-picker">
            <button
              v-for="item in templateOptions"
              :key="item.value"
              class="template-thumb"
              :class="[item.value, { active: templateCode === item.value }]"
              :title="item.label"
              @click="templateCode = item.value"
            >
              <span v-for="block in item.blocks" :key="block"></span>
            </button>
          </div>

          <div class="editor-label mt-28px">选择标签样式：</div>
          <el-radio-group v-model="labelStyle" class="label-radios">
            <el-radio label="bar">长条标签</el-radio>
            <el-radio label="circle">圆形标签</el-radio>
            <el-radio label="notice">预告标签</el-radio>
            <el-radio label="jhs">聚划算</el-radio>
            <el-radio label="tqg">淘抢购</el-radio>
            <el-radio label="brand">品牌标签</el-radio>
          </el-radio-group>

          <div class="label-preview">
            <div class="drag-tip">标签在效果预览中可点击拖动</div>
            <div class="promo-label" :class="labelStyle">
              <span>{{ labelDrafts.textOne }}</span>
              <strong>{{ labelDrafts.textTwo }}</strong>
              <b>{{ labelDrafts.textThree }}</b>
              <em>{{ labelDrafts.textFour }}</em>
            </div>
          </div>

          <div class="label-grid">
            <div class="label-head">位置</div>
            <div class="label-head">文案</div>
            <div class="label-head">大小</div>
            <div class="label-head">颜色</div>

            <span>文字一</span>
            <el-input v-model="labelDrafts.textOne" />
            <el-select v-model="labelDrafts.sizeOne">
              <el-option label="14PX" :value="14" />
              <el-option label="16PX" :value="16" />
              <el-option label="18PX" :value="18" />
            </el-select>
            <el-color-picker v-model="labelDrafts.colorOne" />

            <span>文字二</span>
            <el-input v-model="labelDrafts.textTwo" />
            <el-select v-model="labelDrafts.sizeTwo">
              <el-option label="18PX" :value="18" />
              <el-option label="20PX" :value="20" />
              <el-option label="24PX" :value="24" />
            </el-select>
            <el-color-picker v-model="labelDrafts.colorTwo" />

            <span>文字三</span>
            <el-input v-model="labelDrafts.textThree" />
            <el-select v-model="labelDrafts.sizeThree">
              <el-option label="14PX" :value="14" />
              <el-option label="16PX" :value="16" />
              <el-option label="18PX" :value="18" />
            </el-select>
            <el-color-picker v-model="labelDrafts.colorThree" />

            <span>文字四</span>
            <el-input v-model="labelDrafts.textFour" />
            <el-select v-model="labelDrafts.sizeFour">
              <el-option label="14PX" :value="14" />
              <el-option label="16PX" :value="16" />
              <el-option label="18PX" :value="18" />
            </el-select>
            <el-color-picker v-model="labelDrafts.colorFour" />
          </div>

          <el-checkbox v-model="showQr" class="qr-check">显示该商品的推广二维码</el-checkbox>

          <el-button type="primary" class="generate-btn" :loading="rendering" @click="generateImage">
            生成图片
          </el-button>
        </div>
      </section>

      <section class="material-column">
        <div class="pane-title">获取素材</div>
        <div class="material-box">
          <el-input v-model="materialInput" placeholder="在此输入淘宝链接地址（新商品ID）" />
          <el-button type="primary" :loading="materialLoading" @click="fetchMaterial">获取图片</el-button>
        </div>
        <div class="material-tip">因淘宝联盟升级，数字ID商品链接不再支持，请使用加密新商品ID链接。</div>

        <div v-if="imageDataUrl" class="material-actions">
          <el-button @click="handleCopyImage">复制图片</el-button>
          <el-button @click="handleDownload">保存图片</el-button>
          <el-button @click="handleCopyImageUrl">复制图片地址</el-button>
          <el-button @click="syncPromotionText">写入文案区</el-button>
        </div>
      </section>
    </div>

    <input ref="fileInputRef" class="file-input" type="file" accept="image/*" @change="handleFileChange" />
  </div>
</template>

<script setup lang="ts">
import QRCode from 'qrcode'
import { useClipboard } from '@vueuse/core'

const emit = defineEmits<{
  promotion: [value: string]
}>()

type TemplateCode = 'three' | 'single' | 'stack'
type LabelStyle = 'bar' | 'circle' | 'notice' | 'jhs' | 'tqg' | 'brand'

const message = useMessage()
const { copy } = useClipboard()
const previewCanvasRef = ref<HTMLCanvasElement>()
const fileInputRef = ref<HTMLInputElement>()
const uploadTarget = ref(0)
const imageDataUrl = ref('')
const rendering = ref(false)
const materialLoading = ref(false)
const templateCode = ref<TemplateCode>('three')
const labelStyle = ref<LabelStyle>('bar')
const showQr = ref(false)
const activeLabelPosition = ref(0)
const materialInput = ref('')

const uploadSlots = reactive<string[]>(['', '', ''])
const material = reactive({
  title: '高佣精选爆品',
  promotionUrl: '',
  coupon: '省60元'
})

const labelDrafts = reactive({
  textOne: '券后仅',
  textTwo: '￥13.5',
  textThree: '省60元!',
  textFour: '福利·第2、3件0元',
  sizeOne: 14,
  sizeTwo: 20,
  sizeThree: 14,
  sizeFour: 14,
  colorOne: '#ffffff',
  colorTwo: '#ffffff',
  colorThree: '#ffff00',
  colorFour: '#ffffff'
})

const templateOptions: Array<{ value: TemplateCode; label: string; blocks: number[] }> = [
  { value: 'three', label: '三图模板', blocks: [1, 2, 3] },
  { value: 'single', label: '单图模板', blocks: [1] },
  { value: 'stack', label: '竖排模板', blocks: [1, 2] }
]

const previewMainStyle = computed(() => ({
  backgroundImage: uploadSlots[0] ? `url(${uploadSlots[0]})` : undefined
}))

const slotStyle = (index: number) => ({
  backgroundImage: uploadSlots[index] ? `url(${uploadSlots[index]})` : undefined
})

const triggerUpload = (index: number) => {
  uploadTarget.value = index
  fileInputRef.value?.click()
}

const handleFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    uploadSlots[uploadTarget.value] = String(reader.result || '')
  }
  reader.readAsDataURL(file)
  ;(event.target as HTMLInputElement).value = ''
}

const fetchMaterial = async () => {
  materialLoading.value = true
  try {
    const text = materialInput.value.trim()
    material.promotionUrl = text
    material.title = text ? '淘客精选商品素材' : material.title
    material.coupon = labelDrafts.textThree
    message.success('素材已填充，可继续上传或编辑图片')
  } finally {
    materialLoading.value = false
  }
}

const cycleActiveLabel = () => {
  activeLabelPosition.value = (activeLabelPosition.value + 1) % 3
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

const drawUploadPlaceholder = (ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number) => {
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(x, y, w, h)
  ctx.strokeStyle = '#d1d5db'
  ctx.strokeRect(x, y, w, h)
  ctx.fillStyle = '#9ca3af'
  ctx.font = '42px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('+', x + w / 2, y + h / 2 - 10)
  ctx.font = '22px sans-serif'
  ctx.fillText('上传图片', x + w / 2, y + h / 2 + 28)
  ctx.textAlign = 'left'
}

const drawImageSlot = async (ctx: CanvasRenderingContext2D, url: string, x: number, y: number, w: number, h: number) => {
  const image = await loadImage(url)
  if (!image) {
    drawUploadPlaceholder(ctx, x, y, w, h)
    return
  }
  const ratio = Math.max(w / image.width, h / image.height)
  const dw = image.width * ratio
  const dh = image.height * ratio
  ctx.save()
  ctx.beginPath()
  ctx.rect(x, y, w, h)
  ctx.clip()
  ctx.drawImage(image, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh)
  ctx.restore()
}

const drawLabel = async (ctx: CanvasRenderingContext2D) => {
  ctx.fillStyle = '#f01446'
  ctx.fillRect(0, 460, 560, 78)
  ctx.fillStyle = '#ff5a2c'
  ctx.beginPath()
  ctx.moveTo(0, 460)
  ctx.lineTo(146, 460)
  ctx.lineTo(184, 499)
  ctx.lineTo(146, 538)
  ctx.lineTo(0, 538)
  ctx.closePath()
  ctx.fill()

  ctx.fillStyle = labelDrafts.colorOne
  ctx.font = `${labelDrafts.sizeOne * 2}px sans-serif`
  ctx.fillText(labelDrafts.textOne, 22, 494)
  ctx.fillStyle = labelDrafts.colorTwo
  ctx.font = `700 ${labelDrafts.sizeTwo * 2}px sans-serif`
  ctx.fillText(labelDrafts.textTwo, 22, 526)
  ctx.fillStyle = labelDrafts.colorThree
  ctx.font = `700 ${labelDrafts.sizeThree * 2}px sans-serif`
  ctx.fillText(labelDrafts.textThree, 330, 488)
  ctx.fillStyle = labelDrafts.colorFour
  ctx.font = `${labelDrafts.sizeFour * 2}px sans-serif`
  ctx.fillText(labelDrafts.textFour, 292, 524)

  if (showQr.value) {
    const qrUrl = await QRCode.toDataURL(material.promotionUrl || material.title, { width: 92, margin: 1 })
    const image = await loadImage(qrUrl)
    if (image) ctx.drawImage(image, 450, 560, 92, 92)
  }
}

const drawPoster = async () => {
  const canvas = previewCanvasRef.value
  const ctx = canvas?.getContext('2d')
  if (!canvas || !ctx) return
  canvas.width = 560
  canvas.height = 840
  ctx.fillStyle = '#ffdfe1'
  ctx.fillRect(0, 0, 560, 840)
  await drawImageSlot(ctx, uploadSlots[0], 18, 18, 524, 442)
  await drawLabel(ctx)
  await drawImageSlot(ctx, uploadSlots[1], 40, 568, 200, 200)
  await drawImageSlot(ctx, uploadSlots[2], 320, 568, 200, 200)
  imageDataUrl.value = canvas.toDataURL('image/png')
}

const generateImage = async () => {
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
  if (!imageDataUrl.value) await generateImage()
  return imageDataUrl.value
}

const handleCopyImage = async () => {
  const dataUrl = await ensureImageDataUrl()
  const clipboard = navigator.clipboard as Clipboard & { write?: (items: ClipboardItem[]) => Promise<void> }
  if (clipboard?.write && window.ClipboardItem) {
    const blob = await (await fetch(dataUrl)).blob()
    await clipboard.write([new ClipboardItem({ [blob.type]: blob })])
    message.success('图片已复制')
    return
  }
  await copy(dataUrl)
  message.success('已复制图片地址')
}

const handleDownload = async () => {
  const link = document.createElement('a')
  link.href = await ensureImageDataUrl()
  link.download = `cps-promo-${Date.now()}.png`
  link.click()
}

const handleCopyImageUrl = async () => {
  await copy(await ensureImageDataUrl())
  message.success('图片地址已复制')
}

const syncPromotionText = () => {
  emit('promotion', [material.title, labelDrafts.textThree, labelDrafts.textFour, material.promotionUrl].filter(Boolean).join('\n'))
  message.success('已同步到文案编辑区')
}

watch([uploadSlots, labelDrafts, showQr], () => drawPoster(), { deep: true })
onMounted(drawPoster)
</script>

<style scoped>
.promo-image-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.tutorial-alert {
  border: 0;
  background: transparent;
}

.promo-workbench {
  display: grid;
  grid-template-columns: 290px 400px minmax(260px, 1fr);
  gap: 36px;
  align-items: flex-start;
}

.pane-title {
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.poster-preview {
  width: 280px;
  border: 5px solid #f2254d;
  background: #ffdfe1;
  cursor: pointer;
}

.main-photo {
  display: flex;
  height: 225px;
  align-items: center;
  justify-content: center;
  background-position: center;
  background-size: cover;
}

.upload-box {
  display: inline-flex;
  width: 110px;
  height: 102px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background-color: #f8fafc;
  background-position: center;
  background-size: cover;
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.upload-box :deep(.el-icon) {
  font-size: 30px;
}

.price-strip {
  display: grid;
  height: 60px;
  grid-template-columns: 92px 1fr;
  background: #f01446;
  color: #fff;
}

.price-left {
  display: flex;
  justify-content: center;
  flex-direction: column;
  padding-left: 10px;
  background: #ff5a2c;
  clip-path: polygon(0 0, 78% 0, 100% 50%, 78% 100%, 0 100%);
}

.price-left strong,
.price-right strong {
  font-size: 20px;
}

.price-right {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.price-right strong {
  color: #ffff00;
}

.thumb-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  padding: 18px;
}

.upload-box.small {
  width: 102px;
  height: 102px;
}

.editor-card {
  padding: 28px 30px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.editor-label {
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.template-picker {
  display: flex;
  gap: 16px;
}

.template-thumb {
  display: grid;
  width: 72px;
  height: 72px;
  gap: 4px;
  padding: 5px;
  border: 2px solid var(--el-border-color);
  background: #fff;
  cursor: pointer;
}

.template-thumb.active {
  border-color: #ff7a59;
}

.template-thumb span {
  background: #e5e7eb;
}

.template-thumb.three {
  grid-template-rows: 1fr 20px;
  grid-template-columns: 1fr 1fr;
}

.template-thumb.three span:first-child {
  grid-column: span 2;
}

.template-thumb.single {
  grid-template-columns: 1fr;
}

.template-thumb.stack {
  grid-template-rows: 1fr 1fr;
}

.label-radios {
  display: grid;
  grid-template-columns: repeat(3, minmax(80px, 1fr));
  gap: 8px 12px;
}

.label-preview {
  position: relative;
  display: flex;
  height: 166px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  margin: 26px 0;
  background: #f5f5f5;
}

.drag-tip {
  position: absolute;
  top: -11px;
  left: 50%;
  padding: 2px 10px;
  background: #ff9800;
  color: #fff;
  font-size: 12px;
  transform: translateX(-50%);
}

.promo-label {
  display: grid;
  width: 284px;
  height: 60px;
  grid-template-columns: 92px 1fr;
  align-items: center;
  background: #f01446;
  color: #fff;
}

.promo-label span,
.promo-label strong {
  grid-row: span 2;
  padding-left: 12px;
  background: #ff5a2c;
}

.promo-label b,
.promo-label em {
  text-align: center;
}

.promo-label b {
  color: #ffff00;
}

.label-grid {
  display: grid;
  grid-template-columns: 42px minmax(120px, 1fr) 72px 34px;
  gap: 10px 8px;
  align-items: center;
}

.label-head {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.qr-check {
  width: 100%;
  margin: 18px 0;
  padding: 6px;
  background: var(--el-color-primary-light-9);
}

.generate-btn {
  width: 100%;
}

.material-box {
  display: flex;
  gap: 4px;
  padding: 10px;
  background: #f0f3fb;
}

.material-tip {
  margin-top: 10px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.7;
}

.material-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.file-input,
.hidden-canvas {
  display: none;
}

@media (max-width: 1280px) {
  .promo-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
