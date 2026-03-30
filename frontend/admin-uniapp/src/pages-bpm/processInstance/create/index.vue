<template>
  <!-- TODO vben 对应的地址：/Users/yunai/Java/yudao-ui-admin-vben-v5/apps/web-antd/src/views/bpm/processInstance/create/index.vue -->
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="发起申请"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 搜索框 -->
    <wd-search
      v-model="searchName"
      placeholder="请输入流程名称"
      placeholder-left
      hide-cancel
      @search="handleSearch"
      @clear="handleSearch"
    />

    <!-- 分类标签 -->
    <wd-tabs
      v-model="activeCategory"
      slidable="always"
      sticky
      @click="handleTabClick"
    >
      <wd-tab v-for="item in categoryList" :key="item.code" :title="item.name" :name="item.code" />
    </wd-tabs>

    <!-- 流程定义列表 -->
    <scroll-view
      scroll-y
      class="h-[calc(100vh-320rpx)]"
      :scroll-into-view="scrollIntoView"
      scroll-with-animation
      @scroll="handleScroll"
    >
      <view
        v-for="item in categoryList"
        :id="`category-${item.code}`"
        :key="item.code"
        class="category-section mx-24rpx mt-24rpx"
        :data-category="item.code"
      >
        <!-- 分类标题 -->
        <view class="mb-16rpx flex items-center">
          <text class="text-28rpx text-[#333] font-bold">{{ item.name }}</text>
        </view>
        <!-- 流程列表 -->
        <view v-if="groupedDefinitions[item.code]?.length" class="overflow-hidden rounded-16rpx bg-white">
          <view
            v-for="definition in groupedDefinitions[item.code]"
            :key="definition.id"
            class="flex items-center border-b border-[#f5f5f5] p-24rpx last:border-b-0"
            @click="handleSelect(definition)"
          >
            <wd-img
              v-if="definition.icon"
              :src="definition.icon"
              width="64rpx"
              height="64rpx"
              mode="aspectFit"
              radius="24rpx"
              class="mr-16rpx"
            />
            <view
              v-else
              class="mr-16rpx h-64rpx w-64rpx flex items-center justify-center rounded-12rpx"
              :style="{ backgroundColor: getIconColor(definition.name) }"
            >
              <text class="text-24rpx text-white font-bold">{{ getIconText(definition.name) }}</text>
            </view>
            <text class="text-28rpx text-[#333]">{{ definition.name }}</text>
          </view>
        </view>
        <view v-else class="overflow-hidden rounded-16rpx bg-white p-24rpx text-center">
          <text class="text-26rpx text-[#999]">该分类下暂无流程</text>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-if="categoryList.length === 0" class="py-100rpx">
        <wd-status-tip image="content" tip="暂无可发起的流程" />
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import type { Category } from '@/api/bpm/category'
import type { ProcessDefinition } from '@/api/bpm/definition'
import { onLoad } from '@dcloudio/uni-app'
import { computed, nextTick, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { getCategorySimpleList } from '@/api/bpm/category'
import { getProcessDefinitionList } from '@/api/bpm/definition'
import { getMobileFormCustomPath } from '@/pages-bpm/utils'
import { navigateBackPlus } from '@/utils'
import { BpmModelFormType } from '@/utils/constants'

// TODO @芋艿：【重新发起流程】支持通过 processInstanceId 参数重新发起已有流程
// 对应 vben 第 44-60 行：从路由获取 processInstanceId，查询流程实例后自动选中对应流程定义并填充表单数据

// TODO @芋艿：【流程表单填写】选择流程后跳转到表单填写页面
// 对应 vben form.vue 全部：包含以下子功能：
// - 表单渲染 (form-create)：vben form.vue 第 145-152 行
// - 审批流程预览时间线：vben form.vue 第 153-162 行
// - 流程图预览 (BPMN/简易)：vben form.vue 第 163-178 行
// - 发起人自选审批人：vben form.vue 第 30-32, 85-95 行
// - 表单字段权限控制 (读/写/隐藏)：vben form.vue 第 119-131 行
// - 业务表单跳转 (formCustomCreatePath)：vben form.vue 第 79-85 行
// - 表单值变化重新预测审批节点：vben form.vue 第 87-102 行
// - 提交流程实例：vben form.vue 第 56-76 行

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()

const searchName = ref('')
const activeCategory = ref('')
const categoryList = ref<Category[]>([])
const categoryPositions = ref<{ code: string, top: number }[]>([]) // 分类区域位置信息（用于滚动时自动切换 tab）
const scrollIntoView = ref('')
const isTabClicking = ref(false) // 是否正在通过点击 tab 触发滚动（避免滚动事件反向更新 tab）

const definitionList = ref<ProcessDefinition[]>([])

/** 根据流程名称获取图标背景色 */
function getIconColor(name: string): string {
  const iconColors = ['#D98469', '#7BC67C', '#4A7FEB', '#9B7FEB', '#4A9DEB']
  // 根据名称 hashcode 取模选择颜色
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = (hash * 31 + name.charCodeAt(i)) | 0
  }
  return iconColors[Math.abs(hash) % iconColors.length]
}

/** 获取流程名称的前两个字符作为图标文字 */
function getIconText(name: string): string {
  return name?.slice(0, 2) || ''
}

/** 过滤后的流程定义 */
const filteredDefinitions = computed(() => {
  if (!searchName.value.trim()) {
    return definitionList.value
  }
  return definitionList.value.filter(item =>
    item.name.toLowerCase().includes(searchName.value.toLowerCase()),
  )
})

/** 按分类分组的流程定义 */
const groupedDefinitions = computed<Record<string, ProcessDefinition[]>>(() => {
  const grouped: Record<string, ProcessDefinition[]> = {}
  filteredDefinitions.value.forEach((item) => {
    if (!item.category)
      return
    if (!grouped[item.category])
      grouped[item.category] = []
    grouped[item.category].push(item)
  })
  return grouped
})

/** 返回上一页 */
function handleBack() {
  navigateBackPlus('/pages/bpm/index')
}

/** 搜索 */
async function handleSearch() {
  // 搜索后重新计算分类位置
  await nextTick()
  updateCategoryPositions()
}

/** Tab 点击 */
function handleTabClick({ name }: { index: number, name: string }) {
  isTabClicking.value = true
  // 滚动到对应分类
  scrollIntoView.value = ''
  nextTick(() => {
    scrollIntoView.value = `category-${name}`
    // 300ms 后恢复滚动监听
    setTimeout(() => {
      isTabClicking.value = false
    }, 300)
  })
}

/** 滚动事件 - 自动切换 tab */
function handleScroll(e: { detail: { scrollTop: number } }) {
  if (isTabClicking.value || categoryPositions.value.length === 0) {
    return
  }
  // 找到当前滚动位置对应的分类
  const scrollTop = e.detail.scrollTop
  for (let i = categoryPositions.value.length - 1; i >= 0; i--) {
    if (scrollTop >= categoryPositions.value[i].top - 20) {
      if (activeCategory.value !== categoryPositions.value[i].code) {
        activeCategory.value = categoryPositions.value[i].code
      }
      break
    }
  }
}

/** 更新分类区域位置信息 */
function updateCategoryPositions() {
  const query = uni.createSelectorQuery()
  query.selectAll('.category-section').boundingClientRect()
  query.exec((res) => {
    if (res && res[0]) {
      const positions: { code: string, top: number }[] = []
      const firstTop = res[0][0]?.top || 0
      res[0].forEach((item: { top: number, dataset?: { category?: string } }, index: number) => {
        const cat = categoryList.value[index]
        if (cat) {
          positions.push({
            code: cat.code,
            top: item.top - firstTop,
          })
        }
      })
      categoryPositions.value = positions
    }
  })
}

/** 选择流程定义 */
function handleSelect(item: ProcessDefinition) {
  // 情况一：流程表单，提示仅允许 PC 端发起
  if (item.formType === BpmModelFormType.NORMAL) {
    // TODO @jason：业务表单：/Users/yunai/Java/yudao-ui-admin-vben-v5/apps/web-antd/src/views/bpm/processInstance/create/modules/form.vue
    toast.show('流程表单仅支持 PC 端发起')
    return
  }

  // 情况二：业务表单，跳转到对应的移动端页面
  if (item.formType === BpmModelFormType.CUSTOM) {
    const mobilePath = getMobileFormCustomPath(item.formCustomCreatePath)
    if (mobilePath) {
      uni.navigateTo({ url: mobilePath })
    } else {
      toast.show('该业务表单暂不支持移动端发起')
    }
  }
}

/** 加载分类列表 */
async function loadCategoryList() {
  categoryList.value = await getCategorySimpleList()
}

/** 加载流程定义列表 */
async function loadDefinitionList() {
  definitionList.value = await getProcessDefinitionList({ suspensionState: 1 })
}

/** 初始化 */
onLoad(async () => {
  await Promise.all([loadCategoryList(), loadDefinitionList()])
  // 默认选中第一个分类
  if (categoryList.value.length > 0) {
    activeCategory.value = categoryList.value[0].code
  }
  // 等待 DOM 渲染后计算分类位置
  await nextTick()
  setTimeout(() => {
    updateCategoryPositions()
  }, 100)
})
</script>

<style lang="scss" scoped>
</style>
