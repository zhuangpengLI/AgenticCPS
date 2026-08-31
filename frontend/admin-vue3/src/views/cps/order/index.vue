<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="order-query-form"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="90px"
    >
      <el-form-item label="平台" prop="platformCode">
        <el-select
          v-model="queryParams.platformCode"
          placeholder="请选择平台"
          clearable
          class="!w-160px"
        >
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select
          v-model="queryParams.orderStatus"
          placeholder="请选择状态"
          clearable
          class="!w-160px"
        >
          <el-option label="已下单" value="ordered" />
          <el-option label="已付款" value="paid" />
          <el-option label="已收货" value="received" />
          <el-option label="已结算" value="settled" />
          <el-option label="已到账" value="credited" />
          <el-option label="已退款" value="refunded" />
          <el-option label="已失效" value="invalid" />
        </el-select>
      </el-form-item>
      <el-form-item label="会员名" prop="memberName">
        <el-input
          v-model="queryParams.memberName"
          placeholder="请输入会员名"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="平台单号" prop="platformOrderId">
        <el-input
          v-model="queryParams.platformOrderId"
          placeholder="请输入平台订单号"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品标题" prop="itemTitle">
        <el-input
          v-model="queryParams.itemTitle"
          placeholder="请输入商品关键词"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item class="query-actions">
        <el-button @click="handleQuery"> <Icon icon="ep:search" class="mr-5px" /> 搜索 </el-button>
        <el-button @click="resetQuery"> <Icon icon="ep:refresh" class="mr-5px" /> 重置 </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selectedOrderIds.length === 0"
          @click="handleBatchDelete"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
        <el-dropdown @command="handleSync" class="ml-8px">
          <el-button type="primary" plain>
            <Icon icon="ep:refresh-right" class="mr-5px" /> 同步订单
            <Icon icon="ep:arrow-down" class="ml-5px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="taobao">同步淘宝</el-dropdown-item>
              <el-dropdown-item command="jd">同步京东</el-dropdown-item>
              <el-dropdown-item command="pdd">同步拼多多</el-dropdown-item>
              <el-dropdown-item command="douyin">同步抖音</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <el-dialog v-model="syncDialogVisible" title="同步订单" width="560px" destroy-on-close>
    <div class="sync-dialog-platform">
      <span class="sync-dialog-label">同步平台</span>
      <el-tag :type="platformTagType(syncForm.platformCode)">
        {{ platformLabel(syncForm.platformCode) }}
      </el-tag>
      <span class="sync-dialog-hint">{{ syncForm.queryType === 4 ? '订单及状态' : '订单' }}</span>
    </div>
    <el-form label-width="88px" class="sync-form">
      <el-form-item label="时间范围" required>
        <div class="sync-range-picker">
          <el-button-group class="sync-presets">
            <el-button
              v-for="preset in syncPresets"
              :key="preset.key"
              :type="syncForm.preset === preset.key ? 'primary' : ''"
              @click="selectSyncPreset(preset.key)"
              >{{ preset.label }}</el-button
            >
          </el-button-group>
          <el-date-picker
            v-model="syncForm.dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="sync-date-picker"
            @change="onSyncDateRangeChange"
          />
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="syncDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="syncLoading" @click="confirmSync">开始同步</el-button>
    </template>
  </el-dialog>

  <!-- 同步任务监控 -->
  <ContentWrap class="sync-monitor-wrap">
    <div class="sync-monitor-header">
      <div>
        <div class="sync-monitor-title">同步任务监控</div>
        <div class="sync-monitor-subtitle">实时、夜间及近30天补偿任务状态</div>
      </div>
      <div class="sync-monitor-actions">
        <el-button size="small" @click="loadSyncMonitor"
          ><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button
        >
        <el-button size="small" type="primary" plain @click="syncBatchDialogVisible = true"
          ><Icon icon="ep:plus" class="mr-5px" />创建补偿批次</el-button
        >
      </div>
    </div>
    <div class="sync-metrics-grid">
      <div
        ><span>运行中批次</span><b>{{ syncMetrics.runningBatches ?? '-' }}</b></div
      >
      <div
        ><span>待执行窗口</span><b>{{ syncMetrics.pendingWindows ?? '-' }}</b></div
      >
      <div
        ><span>重试窗口</span><b>{{ syncMetrics.retryWindows ?? '-' }}</b></div
      >
      <div
        ><span>死信窗口</span><b class="danger-text">{{ syncMetrics.deadWindows ?? '-' }}</b></div
      >
      <div
        ><span>窗口成功率</span><b>{{ formatRate(syncMetrics.successRate) }}</b></div
      >
      <div
        ><span>最大延迟</span
        ><b>{{
          syncMetrics.maxDelayMinutes != null ? `${syncMetrics.maxDelayMinutes}分钟` : '-'
        }}</b></div
      >
    </div>
    <el-table
      v-loading="syncBatchLoading"
      :data="syncBatches"
      size="small"
      class="sync-batch-table"
    >
      <el-table-column label="批次" prop="id" width="80" />
      <el-table-column label="平台" width="90"
        ><template #default="scope">{{
          platformLabel(scope.row.platformCode)
        }}</template></el-table-column
      >
      <el-table-column label="轨道" width="100"
        ><template #default="scope">{{
          syncQueryTypeLabel(scope.row.queryType)
        }}</template></el-table-column
      >
      <el-table-column label="时间范围" min-width="250"
        ><template #default="scope"
          >{{ scope.row.startTime }} 至 {{ scope.row.endTime }}</template
        ></el-table-column
      >
      <el-table-column label="进度" width="150"
        ><template #default="scope"
          >{{ scope.row.successWindows || 0 }}/{{ scope.row.totalWindows || 0 }}</template
        ></el-table-column
      >
      <el-table-column label="状态" width="110"
        ><template #default="scope"
          ><el-tag size="small" :type="syncStatusTagType(scope.row.status)">{{
            syncStatusLabel(scope.row.status)
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="操作" fixed="right" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openSyncWindows(scope.row)">窗口</el-button>
          <el-button
            v-if="scope.row.status === 'RUNNING'"
            link
            type="warning"
            @click="operateSyncBatch(scope.row, 'pause')"
            >暂停</el-button
          >
          <el-button
            v-if="scope.row.status === 'PAUSED'"
            link
            type="success"
            @click="operateSyncBatch(scope.row, 'resume')"
            >恢复</el-button
          >
          <el-button
            v-if="['RUNNING', 'PAUSED'].includes(scope.row.status)"
            link
            type="danger"
            @click="operateSyncBatch(scope.row, 'cancel')"
            >取消</el-button
          >
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <el-dialog
    v-model="syncBatchDialogVisible"
    title="创建同步补偿批次"
    width="520px"
    destroy-on-close
  >
    <el-form label-width="90px">
      <el-form-item label="平台" required
        ><el-select v-model="syncBatchForm.platformCode" class="w-full"
          ><el-option label="淘宝" value="taobao" /><el-option label="京东" value="jd" /><el-option
            label="拼多多"
            value="pdd" /></el-select
      ></el-form-item>
      <el-form-item label="同步轨道" required
        ><el-select v-model="syncBatchForm.queryType" class="w-full"
          ><el-option :value="2" label="付款订单" /><el-option
            :value="3"
            label="结算订单" /><el-option :value="4" label="订单更新时间" /></el-select
      ></el-form-item>
      <el-form-item label="时间范围" required
        ><el-date-picker
          v-model="syncBatchForm.dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="w-full"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="syncBatchDialogVisible = false">取消</el-button
      ><el-button type="primary" :loading="syncBatchCreating" @click="createSyncBatch"
        >创建</el-button
      ></template
    >
  </el-dialog>

  <el-dialog v-model="syncWindowDialogVisible" title="同步窗口" width="760px" destroy-on-close>
    <el-table :data="syncWindows" size="small">
      <el-table-column label="时间窗口" min-width="240"
        ><template #default="scope"
          >{{ scope.row.windowStart }} 至 {{ scope.row.windowEnd }}</template
        ></el-table-column
      >
      <el-table-column label="状态" width="110"
        ><template #default="scope"
          ><el-tag size="small" :type="syncStatusTagType(scope.row.status)">{{
            syncStatusLabel(scope.row.status)
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="重试" prop="retryCount" width="70" />
      <el-table-column label="错误" prop="lastErrorMessage" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="90"
        ><template #default="scope"
          ><el-button
            v-if="scope.row.status === 'DEAD'"
            link
            type="primary"
            @click="replaySyncWindow(scope.row)"
            >重放</el-button
          ></template
        ></el-table-column
      >
    </el-table>
  </el-dialog>

  <ContentWrap v-hasPermi="['cps:order:attribution-bind']">
    <div class="claim-review-toolbar">
      <el-select
        v-model="claimQuery.reviewStatus"
        class="claim-status-select"
        @change="getClaimList"
      >
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
        <el-option label="冲突" value="CONFLICT" />
      </el-select>
      <el-input
        v-model="claimQuery.platformOrderId"
        clearable
        class="claim-order-search"
        placeholder="平台订单号"
        @keyup.enter="getClaimList"
      />
      <el-button type="primary" @click="getClaimList">
        <Icon icon="ep:search" class="mr-1" />查询
      </el-button>
    </div>

    <el-table v-loading="claimLoading" :data="claimList">
      <el-table-column label="申领ID" prop="id" width="90" />
      <el-table-column label="平台" width="100">
        <template #default="scope">{{ platformLabel(scope.row.platformCode) }}</template>
      </el-table-column>
      <el-table-column
        label="平台订单号"
        prop="platformOrderId"
        min-width="190"
        show-overflow-tooltip
      />
      <el-table-column label="申领会员" prop="candidateMemberId" width="110" />
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag :type="claimStatusTagType(scope.row.reviewStatus)" size="small">
            {{ claimStatusLabel(scope.row.reviewStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申领原因" prop="rejectReason" min-width="220" show-overflow-tooltip />
      <el-table-column label="申领时间" prop="createTime" width="165" :formatter="dateFormatter" />
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="scope">
          <template v-if="scope.row.reviewStatus === 'PENDING_REVIEW'">
            <el-button type="success" link @click="openClaimReview(scope.row, true)"
              >通过</el-button
            >
            <el-button type="danger" link @click="openClaimReview(scope.row, false)"
              >拒绝</el-button
            >
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="claimTotal"
      v-model:page="claimQuery.pageNo"
      v-model:limit="claimQuery.pageSize"
      @pagination="getClaimList"
    />
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      stripe
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="平台" align="center" prop="platformCode" width="80">
        <template #default="scope">
          <el-tag :type="platformTagType(scope.row.platformCode)" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="平台单号"
        align="left"
        prop="platformOrderId"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column label="商品" align="left" min-width="200">
        <template #default="scope">
          <div class="flex items-center gap-2">
            <el-image
              v-if="scope.row.itemPic"
              :src="scope.row.itemPic"
              style="width: 40px; height: 40px; flex-shrink: 0"
              fit="cover"
            />
            <span class="text-sm truncate">{{ scope.row.itemTitle || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        label="会员名"
        align="center"
        prop="memberNickname"
        width="120"
        show-overflow-tooltip
      >
        <template #default="scope">
          <span>{{ scope.row.memberNickname || scope.row.memberId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="归因" align="center" width="105">
        <template #default="scope">
          <el-tag size="small" type="info">
            {{ attributionSourceLabel(scope.row.attributionSource) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="券后价" align="center" width="90">
        <template #default="scope"> ¥{{ formatAmount(scope.row.finalPrice) }} </template>
      </el-table-column>
      <el-table-column label="预估返利" align="center" width="90">
        <template #default="scope">
          <span :class="scope.row.memberId ? 'text-green-600' : 'text-gray-500'">
            {{ formatEstimateRebate(scope.row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="实际返利" align="center" width="90">
        <template #default="scope">
          <span class="text-orange-500">¥{{ formatAmount(scope.row.realRebate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" prop="orderStatus" width="90">
        <template #default="scope">
          <el-tag :type="orderStatusTagType(scope.row.orderStatus)" size="small">
            {{ orderStatusLabel(scope.row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="165"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" fixed="right" width="120">
        <template #default="scope">
          <el-button
            type="primary"
            link
            @click="openDetail(scope.row)"
            v-hasPermi="['cps:order:query']"
          >
            详情
          </el-button>
          <el-button type="danger" link @click="handleDelete(scope.row.id)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情弹窗 -->
  <el-dialog
    v-model="detailVisible"
    title="订单详情"
    width="960px"
    class="order-detail-dialog"
    destroy-on-close
  >
    <el-descriptions
      v-loading="detailLoading"
      :column="2"
      border
      class="order-detail-descriptions"
      v-if="detailData"
    >
      <el-descriptions-item label="订单ID">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="平台">{{
        platformLabel(detailData.platformCode)
      }}</el-descriptions-item>
      <el-descriptions-item label="平台单号" :span="2">{{
        detailData.platformOrderId
      }}</el-descriptions-item>
      <el-descriptions-item label="父订单号" :span="2">{{
        detailData.parentOrderId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="会员名">{{
        detailData.memberNickname || detailData.memberId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="归因来源">{{
        attributionSourceLabel(detailData.attributionSource)
      }}</el-descriptions-item>
      <el-descriptions-item label="推广位ID" :span="2">{{
        detailData.adzoneId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="外部追踪">{{
        detailData.externalInfo || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="淘宝订单场景">{{
        orderSceneLabel(detailData.orderScene)
      }}</el-descriptions-item>
      <el-descriptions-item label="special_id">
        <div class="attribution-field">
          <span>{{ detailData.specialId || '-' }}</span>
          <el-button
            v-if="detailData.specialId"
            type="primary"
            link
            :disabled="!detailData.adzoneId || bindSpecialIdLoading"
            @click="handleBindSpecialId"
            v-hasPermi="['cps:order:attribution-bind']"
          >
            手动绑定会员
          </el-button>
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="relation_id">{{
        detailData.relationId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="商品标题" :span="2">{{
        detailData.itemTitle || '-'
      }}</el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            商品原价
            <el-tooltip
              content="平台订单返回的商品原价；若平台未返回原价且返回券后价/优惠券金额，后端会用券后价 + 优惠券金额推导。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.itemPrice) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            券后价
            <el-tooltip
              content="平台订单返回的实付/付款金额，优先用于订单展示和佣金核对。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.finalPrice) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            优惠券金额
            <el-tooltip
              content="平台返回的优惠券金额；若缺失且原价/券后价齐全，后端会用商品原价 - 券后价推导。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.couponAmount) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            佣金比例
            <el-tooltip
              content="平台返回或商品快照中的佣金比例，用于估算佣金展示。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        {{ detailData.commissionRate }}%
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            预估佣金
            <el-tooltip
              content="平台预估佣金：优先使用平台订单返回的佣金金额；商品搜索/转链场景可能按券后价 x 佣金比例估算。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.commissionAmount) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            预估返利
            <el-tooltip
              content="预估返利使用与正式结算一致的会员、等级与平台返利规则；未可信归因时不做比例猜测。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        {{ formatEstimateRebate(detailData) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            实际返利
            <el-tooltip
              content="实际返利按返利配置结算后入账，可能受会员等级、平台配置、封顶/保底和退款扣回影响。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.realRebate) }}
      </el-descriptions-item>
      <el-descriptions-item label="订单状态">
        <el-tag :type="orderStatusTagType(detailData.orderStatus)" size="small">
          {{ orderStatusLabel(detailData.orderStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="同步时间">{{
        formatDate(detailData.syncTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="确认收货时间">{{
        formatDate(detailData.confirmReceiptTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="结算时间">{{
        formatDate(detailData.settleTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="返利入账时间">{{
        formatDate(detailData.rebateTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="退款时间">{{
        formatDate(detailData.refundTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{
        formatDate(detailData.createTime)
      }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="bindSpecialIdDialogVisible"
    title="手动绑定会员"
    width="420px"
    destroy-on-close
  >
    <el-form label-width="88px">
      <el-form-item label="special_id">
        <el-input :model-value="bindSpecialIdForm.specialId" disabled />
      </el-form-item>
      <el-form-item label="会员">
        <el-select
          v-model="bindSpecialIdForm.memberId"
          filterable
          remote
          reserve-keyword
          clearable
          class="w-full"
          placeholder="请输入会员名搜索"
          :remote-method="searchBindMemberOptions"
          :loading="bindMemberLoading"
          @visible-change="handleBindMemberVisibleChange"
        >
          <el-option
            v-for="item in bindMemberOptions"
            :key="item.id"
            :label="formatMemberLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="复核说明">
        <el-input
          v-model="bindSpecialIdForm.auditNote"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入平台截图、申诉单或复核依据"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="bindSpecialIdDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="bindSpecialIdLoading" @click="handleConfirmBindSpecialId">
        确定
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="claimReviewVisible"
    :title="claimReviewForm.approved ? '通过订单申领' : '拒绝订单申领'"
    width="460px"
    destroy-on-close
  >
    <el-form label-width="88px">
      <el-form-item label="平台单号">
        <el-input :model-value="claimReviewForm.platformOrderId" disabled />
      </el-form-item>
      <el-form-item label="申领会员">
        <el-input :model-value="claimReviewForm.candidateMemberId" disabled />
      </el-form-item>
      <el-form-item label="审核说明" required>
        <el-input
          v-model="claimReviewForm.auditNote"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入联盟后台核验依据"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="claimReviewVisible = false">取消</el-button>
      <el-button
        :type="claimReviewForm.approved ? 'success' : 'danger'"
        :loading="claimReviewLoading"
        @click="submitClaimReview"
      >
        确认{{ claimReviewForm.approved ? '通过' : '拒绝' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import * as OrderApi from '@/api/cps/order'
import type {
  CpsOrderClaimPageReqVO,
  CpsOrderClaimVO,
  CpsOrderPageReqVO,
  CpsOrderVO
} from '@/api/cps/order'
import { getUserPage, type UserVO } from '@/api/member/user/index'
import dayjs from 'dayjs'

defineOptions({ name: 'CpsOrder' })

type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<CpsOrderVO[]>([])
const selectedOrderIds = ref<number[]>([])
const queryFormRef = ref()
const claimLoading = ref(false)
const claimTotal = ref(0)
const claimList = ref<CpsOrderClaimVO[]>([])
const claimReviewVisible = ref(false)
const claimReviewLoading = ref(false)
const claimQuery = reactive<CpsOrderClaimPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  reviewStatus: 'PENDING_REVIEW',
  platformOrderId: undefined
})
const claimReviewForm = reactive<{
  claimId?: number
  platformOrderId?: string
  candidateMemberId?: number
  approved: boolean
  auditNote: string
}>({
  approved: true,
  auditNote: ''
})

const queryParams = reactive<CpsOrderPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  memberName: undefined,
  orderStatus: undefined,
  itemTitle: undefined,
  platformOrderId: undefined,
  createTime: undefined
})

type SyncPresetKey = 'today' | 'yesterday' | '7d' | '30d' | 'month' | 'custom'
const syncPresets: Array<{ key: SyncPresetKey; label: string }> = [
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: '7d', label: '近7天' },
  { key: '30d', label: '近30天' },
  { key: 'month', label: '本月' },
  { key: 'custom', label: '自定义' }
]
const syncDialogVisible = ref(false)
const syncLoading = ref(false)
const syncForm = reactive<{
  platformCode: string
  queryType: number
  preset: SyncPresetKey
  dateRange: [string, string] | null
}>({ platformCode: '', queryType: 1, preset: 'today', dateRange: null })

const syncBatches = ref<OrderApi.CpsOrderSyncBatchVO[]>([])
const syncBatchLoading = ref(false)
const syncMetrics = ref<OrderApi.CpsOrderSyncMetricsVO>({
  runningBatches: 0,
  pendingWindows: 0,
  retryWindows: 0,
  deadWindows: 0,
  successRate: 0
})
const syncBatchDialogVisible = ref(false)
const syncBatchCreating = ref(false)
const syncBatchForm = reactive<{
  platformCode: string
  queryType: number
  dateRange: [string, string] | null
}>({
  platformCode: 'taobao',
  queryType: 4,
  dateRange: null
})
const syncWindowDialogVisible = ref(false)
const syncWindows = ref<OrderApi.CpsOrderSyncWindowVO[]>([])

const syncDateFormat = 'YYYY-MM-DD HH:mm:ss'
const presetRange = (key: SyncPresetKey): [string, string] | null => {
  const now = dayjs()
  if (key === 'custom') return syncForm.dateRange
  if (key === 'today')
    return [now.startOf('day').format(syncDateFormat), now.format(syncDateFormat)]
  if (key === 'yesterday') {
    return [
      now.subtract(1, 'day').startOf('day').format(syncDateFormat),
      now.subtract(1, 'day').endOf('day').format(syncDateFormat)
    ]
  }
  if (key === 'month')
    return [now.startOf('month').format(syncDateFormat), now.format(syncDateFormat)]
  const days = key === '7d' ? 7 : 30
  return [
    now
      .subtract(days - 1, 'day')
      .startOf('day')
      .format(syncDateFormat),
    now.format(syncDateFormat)
  ]
}

const selectSyncPreset = (key: SyncPresetKey) => {
  syncForm.preset = key
  if (key !== 'custom') syncForm.dateRange = presetRange(key)
}

const onSyncDateRangeChange = (value: [string, string] | null) => {
  syncForm.dateRange = value
  syncForm.preset = 'custom'
}

const syncStatusLabel = (status?: string) =>
  ({
    PENDING: '待执行',
    RUNNING: '执行中',
    PAUSED: '已暂停',
    RETRY_WAIT: '等待重试',
    SUCCESS: '已完成',
    DEAD: '死信',
    CANCELLED: '已取消'
  })[status || ''] ||
  status ||
  '-'
const syncStatusTagType = (status?: string): ElTagType =>
  (({
    SUCCESS: 'success',
    RUNNING: 'primary',
    PAUSED: 'warning',
    RETRY_WAIT: 'warning',
    DEAD: 'danger',
    CANCELLED: 'info'
  })[status || ''] as ElTagType) || 'info'
const syncQueryTypeLabel = (queryType?: number) =>
  ({ 1: '创建时间', 2: '付款时间', 3: '结算时间', 4: '更新时间' })[queryType || 0] || '-'
const formatRate = (rate?: number) => (rate == null ? '-' : `${(rate * 100).toFixed(1)}%`)
const loadSyncMonitor = async () => {
  syncBatchLoading.value = true
  try {
    const [page, metrics] = await Promise.all([
      OrderApi.getOrderSyncBatchPage({ pageNo: 1, pageSize: 10 }),
      OrderApi.getOrderSyncMetrics()
    ])
    syncBatches.value = page.list || []
    syncMetrics.value = metrics || syncMetrics.value
  } catch {
    // 后端接口尚未部署时不影响订单列表使用
  } finally {
    syncBatchLoading.value = false
  }
}
const createSyncBatch = async () => {
  const [startTime, endTime] = syncBatchForm.dateRange || []
  if (!startTime || !endTime) return message.warning('请选择时间范围')
  syncBatchCreating.value = true
  try {
    await OrderApi.createOrderSyncBatch({
      platformCode: syncBatchForm.platformCode,
      queryType: syncBatchForm.queryType,
      startTime,
      endTime,
      batchType: 'MANUAL'
    })
    message.success('同步批次已创建')
    syncBatchDialogVisible.value = false
    await loadSyncMonitor()
  } finally {
    syncBatchCreating.value = false
  }
}
const operateSyncBatch = async (
  row: OrderApi.CpsOrderSyncBatchVO,
  action: 'pause' | 'resume' | 'cancel'
) => {
  const fn =
    action === 'pause'
      ? OrderApi.pauseOrderSyncBatch
      : action === 'resume'
        ? OrderApi.resumeOrderSyncBatch
        : OrderApi.cancelOrderSyncBatch
  await fn(row.id)
  message.success('操作成功')
  await loadSyncMonitor()
}
const openSyncWindows = async (row: OrderApi.CpsOrderSyncBatchVO) => {
  const data = await OrderApi.getOrderSyncBatchWindows(row.id, { pageNo: 1, pageSize: 100 })
  syncWindows.value = data.list || []
  syncWindowDialogVisible.value = true
}
const replaySyncWindow = async (row: OrderApi.CpsOrderSyncWindowVO) => {
  await OrderApi.replayOrderSyncWindow(row.id)
  message.success('窗口已重新加入队列')
  if (row.batchId)
    await openSyncWindows(
      syncBatches.value.find((batch) => batch.id === row.batchId) ||
        ({ id: row.batchId } as OrderApi.CpsOrderSyncBatchVO)
    )
}

/** 平台标签类型 */
const platformTagType = (code: string): ElTagType => {
  const map: Record<string, ElTagType> = {
    taobao: 'danger',
    jd: 'primary',
    pdd: 'warning',
    douyin: 'info',
    eleme: 'success'
  }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = {
    taobao: '淘宝',
    jd: '京东',
    pdd: '拼多多',
    douyin: '抖音',
    eleme: '淘宝闪购'
  }
  return map[code] || code
}

/** 订单状态 */
const orderStatusTagType = (status: string): ElTagType => {
  const map: Record<string, ElTagType> = {
    created: 'info',
    ordered: 'info',
    paid: 'primary',
    received: 'warning',
    settled: 'success',
    rebate_received: 'success',
    credited: 'success',
    refunded: 'danger',
    invalid: 'info'
  }
  return map[status] || 'info'
}
const orderStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    created: '已下单',
    ordered: '已下单',
    paid: '已付款',
    received: '已收货',
    settled: '已结算',
    rebate_received: '已到账',
    credited: '已到账',
    refunded: '已退款',
    invalid: '已失效'
  }
  return map[status] || status
}

/** 金额格式化（直接为元） */
const formatAmount = (val?: number) => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

/** 未完成可信会员归因时，预计返利金额尚不能确定。 */
const formatEstimateRebate = (order?: CpsOrderVO | null) => {
  if (!order?.memberId) return '待归因'
  return `¥${formatAmount(order.estimateRebate)}`
}

/** 日期格式化 */
const formatDate = (val?: Date | string) => {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN')
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await OrderApi.getCpsOrderPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const getClaimList = async () => {
  claimLoading.value = true
  try {
    const data = await OrderApi.getOrderClaimPage(claimQuery)
    claimList.value = data.list
    claimTotal.value = data.total
  } finally {
    claimLoading.value = false
  }
}

const claimStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PENDING_REVIEW: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CONFLICT: '冲突',
    PENDING_SYNC: '等待同步',
    ASSET_LOCKED: '资金锁定'
  }
  return status ? map[status] || status : '-'
}

const claimStatusTagType = (status?: string): ElTagType => {
  const map: Record<string, ElTagType> = {
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CONFLICT: 'danger',
    PENDING_SYNC: 'info',
    ASSET_LOCKED: 'danger'
  }
  return status ? map[status] || 'info' : 'info'
}

const openClaimReview = (claim: CpsOrderClaimVO, approved: boolean) => {
  claimReviewForm.claimId = claim.id
  claimReviewForm.platformOrderId = claim.platformOrderId
  claimReviewForm.candidateMemberId = claim.candidateMemberId
  claimReviewForm.approved = approved
  claimReviewForm.auditNote = ''
  claimReviewVisible.value = true
}

const submitClaimReview = async () => {
  if (!claimReviewForm.claimId || !claimReviewForm.auditNote.trim()) {
    message.warning('请输入审核说明')
    return
  }
  claimReviewLoading.value = true
  try {
    const result = await OrderApi.reviewOrderClaim({
      claimId: claimReviewForm.claimId,
      approved: claimReviewForm.approved,
      auditNote: claimReviewForm.auditNote.trim()
    })
    message.success(result.message || '审核完成')
    claimReviewVisible.value = false
    await Promise.all([getClaimList(), getList()])
  } finally {
    claimReviewLoading.value = false
  }
}

const attributionSourceLabel = (source?: string) => {
  const map: Record<string, string> = {
    specialId: '会员运营ID',
    relationId: '渠道ID',
    externalId: '外部追踪',
    adzone: '专属PID',
    transferRecord: '转链记录',
    sid: '闪购SID'
  }
  return source ? map[source] || source : '-'
}

const orderSceneLabel = (scene?: number) => {
  const map: Record<number, string> = {
    1: '常规订单',
    2: '渠道订单',
    3: '会员运营订单'
  }
  return scene ? map[scene] || String(scene) : '-'
}

/** 表格选择 */
const handleSelectionChange = (rows: CpsOrderVO[]) => {
  selectedOrderIds.value = rows.map((row) => row.id)
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await OrderApi.deleteCpsOrder(id)
    message.success('删除成功')
    selectedOrderIds.value = selectedOrderIds.value.filter((selectedId) => selectedId !== id)
    await getList()
  } catch {}
}

/** 批量删除 */
const handleBatchDelete = async () => {
  const ids = [...selectedOrderIds.value]
  if (ids.length === 0) return
  try {
    await message.confirm(`确认删除选中的 ${ids.length} 条订单？`)
    await OrderApi.deleteCpsOrderList(ids)
    message.success('批量删除成功')
    selectedOrderIds.value = []
    await getList()
  } catch {}
}

/** 手动同步 */
const handleSync = async (command: string) => {
  syncForm.platformCode = command
  syncForm.queryType = command === 'taobao' ? 4 : 1
  syncForm.preset = 'today'
  syncForm.dateRange = presetRange('today')
  syncDialogVisible.value = true
}

const confirmSync = async () => {
  const [startTime, endTime] = syncForm.dateRange || []
  if (!startTime || !endTime) {
    message.warning('请选择同步时间范围')
    return
  }
  const start = dayjs(startTime)
  const end = dayjs(endTime)
  if (!start.isValid() || !end.isValid() || !start.isBefore(end)) {
    message.warning('同步开始时间必须早于结束时间')
    return
  }
  const hours = Math.max(1, Math.ceil(end.diff(start, 'minute') / 60))
  try {
    await message.confirm(
      `确认同步 ${platformLabel(syncForm.platformCode)}\n${start.format(syncDateFormat)} 至 ${end.format(syncDateFormat)}？`
    )
    syncLoading.value = true
    const result = await OrderApi.syncCpsOrders(
      syncForm.platformCode,
      hours,
      syncForm.queryType,
      start.format(syncDateFormat),
      end.format(syncDateFormat)
    )
    message.success(result || '同步任务已触发')
    syncDialogVisible.value = false
    await getList()
  } catch {
  } finally {
    syncLoading.value = false
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const bindSpecialIdLoading = ref(false)
const bindSpecialIdDialogVisible = ref(false)
const bindMemberLoading = ref(false)
const bindMemberOptions = ref<UserVO[]>([])
const bindSpecialIdForm = reactive<{
  orderId?: number
  specialId?: string
  memberId?: number
  auditNote?: string
}>({})
const MEMBER_OPTION_PAGE_SIZE = 100
const detailData = ref<CpsOrderVO | null>(null)
const openDetail = async (row: CpsOrderVO) => {
  detailData.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await OrderApi.getCpsOrder(row.id)
  } finally {
    detailLoading.value = false
  }
}

const handleBindSpecialId = async () => {
  if (!detailData.value?.id || !detailData.value.specialId) return
  if (!detailData.value.adzoneId) {
    message.warning('订单缺少推广位ID，不能建立 special_id 绑定关系')
    return
  }
  bindSpecialIdForm.orderId = detailData.value.id
  bindSpecialIdForm.specialId = detailData.value.specialId
  bindSpecialIdForm.memberId = undefined
  bindSpecialIdForm.auditNote = undefined
  bindSpecialIdDialogVisible.value = true
  await searchBindMemberOptions('')
}

const searchBindMemberOptions = async (keyword: string) => {
  bindMemberLoading.value = true
  try {
    const queryText = keyword?.trim()
    const params = {
      mobile: /^\d+$/.test(queryText || '') ? queryText : undefined,
      nickname: queryText && !/^\d+$/.test(queryText) ? queryText : undefined
    }
    const firstPage = await getUserPage({
      pageNo: 1,
      pageSize: MEMBER_OPTION_PAGE_SIZE,
      ...params
    })
    const options = firstPage?.list || []
    const total = firstPage?.total || options.length
    const pageCount = Math.ceil(total / MEMBER_OPTION_PAGE_SIZE)
    for (let pageNo = 2; pageNo <= pageCount; pageNo++) {
      const data = await getUserPage({
        pageNo,
        pageSize: MEMBER_OPTION_PAGE_SIZE,
        ...params
      })
      options.push(...(data?.list || []))
    }
    bindMemberOptions.value = options
  } finally {
    bindMemberLoading.value = false
  }
}

const handleBindMemberVisibleChange = (visible: boolean) => {
  if (visible && bindMemberOptions.value.length === 0) {
    searchBindMemberOptions('')
  }
}

const handleConfirmBindSpecialId = async () => {
  if (!bindSpecialIdForm.orderId || !bindSpecialIdForm.memberId) {
    message.warning('请选择会员')
    return
  }
  try {
    bindSpecialIdLoading.value = true
    await OrderApi.bindSpecialIdToMember({
      orderId: bindSpecialIdForm.orderId,
      memberId: bindSpecialIdForm.memberId,
      idempotencyKey: createManualBindIdempotencyKey(bindSpecialIdForm.orderId),
      auditNote: bindSpecialIdForm.auditNote?.trim() || undefined
    })
    message.success('绑定成功')
    bindSpecialIdDialogVisible.value = false
    detailData.value = await OrderApi.getCpsOrder(bindSpecialIdForm.orderId)
    await getList()
  } catch {
  } finally {
    bindSpecialIdLoading.value = false
  }
}

const formatMemberLabel = (item: UserVO) => {
  const parts = [`ID: ${item.id}`]
  if (item.nickname) parts.push(item.nickname)
  if (item.name && item.name !== item.nickname) parts.push(item.name)
  if (item.mobile) parts.push(item.mobile)
  return parts.join(' / ')
}

const createManualBindIdempotencyKey = (orderId: number) => {
  const randomId =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `manual-bind:${orderId}:${randomId}`
}

onMounted(() => {
  getList()
  getClaimList()
  loadSyncMonitor()
})
</script>

<style scoped>
.order-detail-descriptions :deep(.el-descriptions__label) {
  width: 112px;
  min-width: 112px;
  white-space: nowrap;
}

.order-detail-descriptions :deep(.el-descriptions__content) {
  min-width: 220px;
}

.detail-label-with-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.detail-tip-icon {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.attribution-field {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.attribution-field span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.claim-review-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.claim-status-select {
  width: 140px;
}

.claim-order-search {
  width: min(320px, 100%);
}

.order-query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  column-gap: 16px;
}

.order-query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 16px;
}

.query-actions {
  flex: 1 0 100%;
  justify-content: flex-end;
}

.sync-dialog-platform {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 18px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.sync-dialog-label {
  color: var(--el-text-color-secondary);
}

.sync-dialog-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.sync-range-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.sync-presets {
  display: flex;
  flex-wrap: wrap;
}

.sync-date-picker {
  width: 100%;
}

.sync-monitor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.sync-monitor-title {
  font-size: 16px;
  font-weight: 600;
}

.sync-monitor-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.sync-monitor-actions {
  display: flex;
  gap: 8px;
}

.sync-metrics-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.sync-metrics-grid > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.sync-metrics-grid span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.sync-metrics-grid b {
  font-size: 18px;
}

.danger-text {
  color: var(--el-color-danger);
}

@media (max-width: 768px) {
  .sync-monitor-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .sync-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-query-form {
    display: block;
  }

  .order-query-form :deep(.el-form-item) {
    width: 100%;
  }

  .order-query-form :deep(.el-form-item .el-input),
  .order-query-form :deep(.el-form-item .el-select),
  .order-query-form :deep(.el-form-item .el-date-editor) {
    width: 100% !important;
  }

  .query-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .order-detail-dialog {
    width: calc(100vw - 24px) !important;
  }

  .order-detail-descriptions :deep(.el-descriptions__label) {
    width: 96px;
    min-width: 96px;
  }
}
</style>
