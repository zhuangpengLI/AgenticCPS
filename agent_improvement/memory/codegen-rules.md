# 代码生成规则 (CodeGen Rules)

> 基于 `yudao-module-infra/src/main/resources/codegen` 模板库总结的业务系统代码生成规范

## 1. 层级结构

```
module-{moduleName}/
├── controller/
│   └── {sceneEnum.basePackage}/           # 场景包名，如 admin/app/api
│       └── {businessName}/
│           ├── {ClassName}Controller.java
│           └── vo/
│               ├── {ClassName}PageReqVO.java    # 分页请求
│               ├── {ClassName}ListReqVO.java    # 列表请求（树表）
│               ├── {ClassName}SaveReqVO.java    # 新增/修改请求
│               └── {ClassName}RespVO.java       # 响应 VO
├── service/
│   └── {businessName}/
│       ├── {ClassName}Service.java        # 接口
│       └── {ClassName}ServiceImpl.java    # 实现
└── dal/
    ├── dataobject/{businessName}/
    │   ├── {ClassName}DO.java             # 主表 DO
    │   └── {SubClassName}DO.java          # 子表 DO（主子表）
    └── mysql/{businessName}/
        ├── {ClassName}Mapper.java         # 主表 Mapper
        └── {SubClassName}Mapper.java       # 子表 Mapper（主子表）
```

## 2. 命名约定

| 元素 | 规则 | 示例 |
|------|------|------|
| 模块名 | module-{moduleName} | module-infra, module-system |
| 业务名 | 小写中划线 | order-item, product-category |
| 类名 | PascalCase | OrderItem, ProductCategory |
| 变量名 | camelCase | orderItem, productCategory |
| 包路径 | 小写点分隔 | admin.order, app.product |
| HTTP路径 | /{moduleName}/{className_strike_case} | /infra/demo-sample |

### 变量名转换规则
- `${simpleClassName}` = PascalCase 类名（去前缀），如 `DemoSample`
- `${classNameVar}` = camelCase 变量名，如 `demoSample`
- `${simpleClassName_strikeCase}` = 小写下划线转小写，如 `demo_sample`
- `${simpleClassName_underlineCase.toUpperCase()}` = 全大写下划线，如 `DEMO_SAMPLE`
- `${primaryColumn.javaType}` = 主键 Java 类型，如 `Long`
- `${saveReqVOClass}` = 保存请求 VO 全类名
- `${respVOClass}` = 响应 VO 全类名

## 3. DO（Data Object）规范

```java
package {basePackage}.module.{moduleName}.dal.dataobject.{businessName};

@TableName("{table_name}")
@KeySequence("{table_name}_seq")  // Oracle/PG/Kingbase/DB2/H2 需要
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {ClassName}DO extends BaseDO {
    // 排除 BaseDO 已包含字段 (id, createdBy, createTime, updatedBy, updateTime, deleted, tenantId)
    // 主键使用 @TableId，如果是 String 类型则 @TableId(type = IdType.INPUT)
}
```

### 特殊处理
- **树表**: 定义 `${treeParentColumn_javaField_underlineCase.toUpperCase()}_ROOT = 0L` 常量
- **主子表**: 子表列表/对象用 `@TableField(exist = false)` 标记
- **枚举字段**: 添加 `@Schema(description = "...")` + `@ExcelProperty(value = "...", converter = DictConvert.class)`
- **时间字段**: `LocalDateTime` 需 import

## 4. Mapper 规范

```java
@Mapper
public interface {ClassName}Mapper extends BaseMapperX<{ClassName}DO> {

    // 分页查询（非树表）
    default PageResult<{ClassName}DO> selectPage({ClassName}PageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<{ClassName}DO>()
            .eqIfPresent({ClassName}DO::getXxx, reqVO.getXxx())
            // ... 更多查询条件
            .orderByDesc({ClassName}DO::getId));
    }

    // 列表查询（树表）
    default List<{ClassName}DO> selectList({ClassName}ListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<{ClassName}DO>()
            .eqIfPresent({ClassName}DO::getXxx, reqVO.getXxx())
            .orderByDesc({ClassName}DO::getId));
    }
}
```

### 查询条件操作符
| condition | 方法 |
|-----------|------|
| `=` | `.eqIfPresent(DO::getXxx, reqVO.getXxx())` |
| `!=` | `.neIfPresent(...)` |
| `>` | `.gtIfPresent(...)` |
| `>=` | `.geIfPresent(...)` |
| `<` | `.ltIfPresent(...)` |
| `<=` | `.leIfPresent(...)` |
| `LIKE` | `.likeIfPresent(...)` |
| `BETWEEN` | `.betweenIfPresent(...)` |

## 5. Service 接口规范

```java
public interface {ClassName}Service {
    {primaryType} create{ClassName}(@Valid {SaveReqVO} reqVO);
    void update{ClassName}(@Valid {SaveReqVO} reqVO);
    void delete{ClassName}({primaryType} id);
    // 批量删除（可选）
    void delete{ClassName}ListByIds(List<{primaryType}> ids);
    {ClassName}DO get{ClassName}({primaryType} id);
    // 分页（普通表）或列表（树表）
    PageResult<{ClassName}DO> get{ClassName}Page({ClassName}PageReqVO pageReqVO);
    List<{ClassName}DO> get{ClassName}List({ClassName}ListReqVO listReqVO);
}
```

### 子表操作（ERP 模式 templateType==11）
```java
// 子表分页
PageResult<{SubClassName}DO> get{SubClassName}Page(PageParam pageReqVO, {joinType} {joinField});
// 子表增删改
{SubPrimaryType} create{SubClassName}(@Valid {SubClassName}DO reqVO);
void update{SubClassName}(@Valid {SubClassName}DO reqVO);
void delete{SubClassName}({SubPrimaryType} id);
{SubClassName}DO get{SubClassName}({SubPrimaryType} id);
```

## 6. ServiceImpl 规范

```java
@Service
@Validated
public class {ClassName}ServiceImpl implements {ClassName}Service {

    @Resource
    private {ClassName}Mapper {classNameVar}Mapper;

    @Override
    @Transactional(rollbackFor = Exception.class)  // 主子表必须
    public {primaryType} create{ClassName}({SaveReqVO} reqVO) {
        // 树表：校验父级有效性 + 名称唯一性
        // 插入
        {ClassName}DO obj = BeanUtils.toBean(reqVO, {ClassName}DO.class);
        {classNameVar}Mapper.insert(obj);
        // 主子表：插入子表
        return obj.getId();
    }

    @Override
    public void update{ClassName}({SaveReqVO} reqVO) {
        validateExists(reqVO.getId());
        // 树表：校验父级 + 名称唯一性
        {ClassName}DO updateObj = BeanUtils.toBean(reqVO, {ClassName}DO.class);
        {classNameVar}Mapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete{ClassName}({primaryType} id) {
        validateExists(id);
        // 树表：校验是否有子节点
        {classNameVar}Mapper.deleteById(id);
        // 主子表：删除子表
    }

    private void validate{ClassName}Exists({primaryType} id) {
        if ({classNameVar}Mapper.selectById(id) == null) {
            throw exception({CLASS_NAME}_NOT_EXISTS);
        }
    }
}
```

### 子表批量操作（非 ERP 模式）
```java
// 一对多：批量新增/更新/删除子表
private void create{SubClassName}List({primaryType} {joinField}, List<{SubClassName}DO> list) {
    list.forEach(o -> o.set{JoinFieldName}({joinField}).clean());
    {subClassNameVar}Mapper.insertBatch(list);
}

private void update{SubClassName}List({primaryType} {joinField}, List<{SubClassName}DO> list) {
    // diffList 计算差异，分批 insert/update/delete
}

// 一对一：单个新增/更新
private void create{SubClassName}({primaryType} {joinField}, {SubClassName}DO subObj) {
    subObj.set{JoinFieldName}({joinField});
    subObj.clean();
    {subClassNameVar}Mapper.insert(subObj);
}
```

## 7. Controller 规范

```java
@Tag(name = "{sceneName} - {classComment}")
@RestController
@RequestMapping("/{moduleName}/{className_strike_case}")
@Validated
public class {ClassName}Controller {

    @Resource
    private {ClassName}Service {classNameVar}Service;

    @PostMapping("/create")
    @Operation(summary = "创建{classComment}")
    @PreAuthorize("@ss.hasPermission('{permissionPrefix}:create')")
    public CommonResult<{primaryType}> create{ClassName}(@Valid @RequestBody {SaveReqVO} reqVO) {
        return success({classNameVar}Service.create{ClassName}(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新{classComment}")
    public CommonResult<Boolean> update{ClassName}(@Valid @RequestBody {SaveReqVO} reqVO) {
        {classNameVar}Service.update{ClassName}(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除{classComment}")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> delete{ClassName}(@RequestParam("id") {primaryType} id) {
        {classNameVar}Service.delete{ClassName}(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得{classComment}")
    public CommonResult<{RespVO}> get{ClassName}(@RequestParam("id") {primaryType} id) {
        return success(BeanUtils.toBean({classNameVar}Service.get{ClassName}(id), {RespVO}.class));
    }

    @GetMapping("/page")  // 或 /list（树表）
    @Operation(summary = "获得{classComment}分页")
    public CommonResult<PageResult<{RespVO}>> get{ClassName}Page(@Valid {PageReqVO} pageReqVO) {
        PageResult<{ClassName}DO> pageResult = {classNameVar}Service.get{ClassName}Page(pageReqVO);
        return success(BeanUtils.toBean(pageResult, {RespVO}.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出{classComment} Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void export{ClassName}Excel(@Valid {PageReqVO} pageReqVO, HttpServletResponse response) {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<{ClassName}DO> list = {classNameVar}Service.get{ClassName}Page(pageReqVO).getList();
        ExcelUtils.write(response, "{classComment}.xls", "数据", {RespVO}.class,
            BeanUtils.toBean(list, {RespVO}.class));
    }
}
```

## 8. VO 规范

### PageReqVO / ListReqVO
```java
@Schema(description = "{sceneName} - {classComment}分页 Request VO")
@Data
public class {ClassName}PageReqVO extends PageParam {
    // 查询字段（listOperation == true）
    // BETWEEN 条件用数组类型 + @DateTimeFormat
    @Schema(description = "{columnComment}")
    private {columnType} {columnName};

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private {columnType}[] {betweenField};
}
```

### SaveReqVO
```java
@Schema(description = "{sceneName} - {classComment}新增/修改 Request VO")
@Data
public class {ClassName}SaveReqVO {
    // createOperation || updateOperation 的字段
    // 非空校验：@NotNull / @NotEmpty（String 类型）
    @Schema(description = "{columnComment}", requiredMode = RequiredMode.REQUIRED)
    @NotNull(message = "{columnComment}不能为空")
    private {columnType} {columnName};
}
```

### RespVO
```java
@Schema(description = "{sceneName} - {classComment} Response VO")
@Data
@ExcelIgnoreUnannotated
public class {ClassName}RespVO {
    // listOperationResult == true 的字段
    // 枚举字段：@ExcelProperty(value = "...", converter = DictConvert.class)
    @Schema(description = "{columnComment}")
    @ExcelProperty("{columnComment}")
    private {columnType} {columnName};
}
```

## 9. 模板类型 (templateType)

| templateType | 类型 | 特点 |
|-------------|------|------|
| 1 | 通用 | 标准 CRUD + 分页 |
| 2 | 树表 | 列表查询 + 树父子校验 |
| 11 | ERP主表 | 主子表 + 独立子表增删改查 |

## 10. 错误码规范

```java
// ========== {classComment} ==========
ErrorCode {CLASS_NAME}_NOT_EXISTS = new ErrorCode(补充编号, "{classComment}不存在");
// 树表额外错误码
ErrorCode {CLASS_NAME}_EXITS_CHILDREN = new ErrorCode(补充编号, "存在子{caption}，无法删除");
ErrorCode {CLASS_NAME}_PARENT_NOT_EXITS = new ErrorCode(补充编号, "父级{caption}不存在");
ErrorCode {CLASS_NAME}_PARENT_ERROR = new ErrorCode(补充编号, "不能设置自己为父{caption}");
ErrorCode {CLASS_NAME}_{FIELD}_DUPLICATE = new ErrorCode(补充编号, "已存在该{field}的{caption}");
```

## 11. 前端代码生成规则

### 11.1 Vue3 Element Plus 模板

#### 目录结构
```
src/api/{moduleName}/{businessName}/
├── index.ts              # API 接口
src/views/{moduleName}/{businessName}/
├── index.vue             # 列表页
├── {ClassName}Form.vue   # 表单弹窗
└── components/           # 子表组件
    ├── {SubClassName}List.vue
    └── {SubClassName}Form.vue
```

#### API 接口定义
```typescript
import request from '@/config/axios'
const baseURL = '/{moduleName}/{className_strike_case}'

// TypeScript 接口
export interface {ClassName} {
  id?: number
  // ... 字段定义，可选字段用 ?
}

// API 对象
export const {ClassName}Api = {
  get{ClassName}Page: async (params) => await request.get({ url: `${baseURL}/page`, params }),
  get{ClassName}: async (id) => await request.get({ url: `${baseURL}/get?id=` + id }),
  create{ClassName}: async (data) => await request.post({ url: `${baseURL}/create`, data }),
  update{ClassName}: async (data) => await request.put({ url: `${baseURL}/update`, data }),
  delete{ClassName}: async (id) => await request.delete({ url: `${baseURL}/delete?id=` + id }),
  export{ClassName}: async (params) => await request.download({ url: `${baseURL}/export-excel`, params }),
}
```

#### 列表页 (index.vue)
```vue
<template>
  <ContentWrap>
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true">
      <el-form-item label="{comment}" prop="{javaField}">
        <el-input v-model="queryParams.{javaField}" placeholder="请输入{comment}" />
      </el-form-item>
      <!-- 更多查询条件... -->
      <el-form-item>
        <el-button @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="primary" @click="openForm('create')" v-hasPermi="['{permissionPrefix}:create']">新增</el-button>
        <el-button type="success" @click="handleExport">导出</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table :data="list" v-loading="loading" row-key="id">
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button link type="primary" @click="openForm('update', scope.row.id)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination :total="total" v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" />
  </ContentWrap>

  <{ClassName}Form ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { {ClassName}Api, {ClassName} } from '@/api/{moduleName}/{businessName}'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'

const list = ref<{ClassName}[]>([])
const total = ref(0)
const queryParams = reactive({ pageNo: 1, pageSize: 10 })

const getList = async () => {
  loading.value = true
  try {
    const data = await {ClassName}Api.get{ClassName}Page(queryParams)
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}

const openForm = (type: string, id?: number) => formRef.value.open(type, id)
const handleDelete = async (id: number) => {
  await message.delConfirm()
  await {ClassName}Api.delete{ClassName}(id)
  message.success(t('common.delSuccess'))
  getList()
}
</script>
```

#### 表单弹窗 ({ClassName}Form.vue)
```vue
<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="{comment}" prop="{javaField}">
        <el-input v-model="formData.{javaField}" />
      </el-form-item>
      <!-- 更多表单项... -->
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary">确定</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { {ClassName}Api } from '@/api/{moduleName}/{businessName}'

const dialogVisible = ref(false)
const formData = ref({ /* 字段初始值 */ })
const formRules = reactive({
  {javaField}: [{ required: true, message: '{comment}不能为空', trigger: 'blur' }],
})

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await {ClassName}Api.create{ClassName}(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await {ClassName}Api.update{ClassName}(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally { formLoading.value = false }
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  formType.value = type
  if (id) {
    formData.value = await {ClassName}Api.get{ClassName}(id)
  }
}
defineExpose({ open })
</script>
```

#### 树表特殊处理
```typescript
// 列表查询使用 handleTree 转换
import { handleTree } from '@/utils/tree'
const data = await {ClassName}Api.get{ClassName}List(queryParams)
list.value = handleTree(data, 'id', 'parentId')

// 表单使用 el-tree-select
<el-tree-select v-model="formData.parentId" :data="{classNameVar}Tree" />
```

### 11.2 Vue3 Vben Admin 模板

#### 目录结构
```
src/api/{moduleName}/{businessName}/
├── index.ts              # API 函数
src/views/{moduleName}/{businessName}/
├── index.vue             # 列表页
├── {ClassName}Modal.vue  # 表单弹窗
├── {ClassName}.data.ts   # 表格/表单配置
└── modules/               # 子表组件
    ├── {SubClassName}-list.vue
    └── {SubClassName}Form.vue
```

#### API 接口 (index.ts)
```typescript
import { defHttp } from '@/utils/http/axios'
const baseURL = '/{moduleName}/{className_strike_case}'

export const get{ClassName}Page = (params) => defHttp.get({ url: `${baseURL}/page`, params })
export const get{ClassName} = (id: number) => defHttp.get({ url: `${baseURL}/get?id=${id}` })
export const create{ClassName} = (data) => defHttp.post({ url: `${baseURL}/create`, data })
export const update{ClassName} = (data) => defHttp.put({ url: `${baseURL}/update`, data })
export const delete{ClassName} = (id: number) => defHttp.delete({ url: `${baseURL}/delete?id=${id}` })
export const export{ClassName} = (params) => defHttp.download({ url: `${baseURL}/export-excel`, params }, '{classComment}.xls')
```

#### 配置数据 ({ClassName}.data.ts)
```typescript
import type { BasicColumn, FormSchema } from '@/components/Table'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'

export const columns: BasicColumn[] = [
  {
    title: '{comment}',
    dataIndex: '{javaField}',
    width: 180,
    customRender: ({ text }) => useRender.renderDict(text, DICT_TYPE.{DICT_TYPE}_UPPER),
  },
  // 时间类型
  { title: '{comment}', dataIndex: '{javaField}', width: 180, customRender: ({ text }) => useRender.renderDate(text) },
]

export const searchFormSchema: FormSchema[] = [
  {
    label: '{comment}',
    field: '{javaField}',
    component: 'Input', // 或 Select, DatePicker, RangePicker
    colProps: { span: 8 },
  },
]

export const createFormSchema: FormSchema[] = [
  { label: '编号', field: 'id', show: false, component: 'Input' },
  {
    label: '{comment}',
    field: '{javaField}',
    required: true,
    component: 'Input', // 或 Select, DatePicker, FileUpload, Editor
  },
]
```

#### 列表页 (index.vue)
```vue
<script lang="ts" setup>
import { BasicTable, TableAction, useTable } from '@/components/Table'
import { useModal } from '@/components/Modal'
import { {ClassName}Modal } from './{ClassName}Modal.vue'
import { get{ClassName}Page, delete{ClassName}, export{ClassName} } from '@/api/{moduleName}/{businessName}'

const [registerTable, { getForm, reload }] = useTable({
  title: '{classComment}列表',
  api: get{ClassName}Page,
  columns,
  formConfig: { labelWidth: 120, schemas: searchFormSchema },
  useSearchForm: true,
  actionColumn: { width: 140, title: '操作', dataIndex: 'action' },
})

const [registerModal, { openModal }] = useModal()
const handleEdit = (record) => openModal(true, { record, isUpdate: true })
const handleDelete = async (record) => {
  await delete{ClassName}(record.id)
  reload()
}
</script>

<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar>
        <a-button type="primary" @click="openModal(true, { isUpdate: false })">新增</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <TableAction :actions="[
            { label: '编辑', onClick: handleEdit.bind(null, record) },
            { label: '删除', danger: true, onClick: handleDelete.bind(null, record) },
          ]" />
        </template>
      </template>
    </BasicTable>
    <{ClassName}Modal @register="registerModal" @success="reload()" />
  </div>
</template>
```

#### 表单弹窗 ({ClassName}Modal.vue)
```vue
<script lang="ts" setup>
import { createFormSchema } from './{ClassName}.data'
import { BasicModal, useModalInner } from '@/components/Modal'
import { BasicForm, useForm } from '@/components/Form'
import { create{ClassName}, get{ClassName}, update{ClassName} } from '@/api/{moduleName}/{businessName}'

const [registerModal, { closeModal }] = useModalInner(async (data) => {
  if (data?.isUpdate) {
    const res = await get{ClassName}(data.record.id)
    setFieldsValue({ ...res })
  }
})

const handleSubmit = async () => {
  const values = await validate()
  unref(isUpdate) ? await update{ClassName}(values) : await create{ClassName}(values)
  closeModal()
  emit('success')
}
</script>

<template>
  <BasicModal :title="isUpdate ? '编辑' : '新建'" @register="registerModal" @ok="handleSubmit">
    <BasicForm @register="registerForm" />
  </BasicModal>
</template>
```

### 11.3 Vue3 Vben5 Antd 模板

```vue
<!-- 使用 Ant Design Vue 组件 -->
<template>
  <Page auto-content-height>
    <Card>
      <Form :model="queryParams" layout="inline">
        <Form.Item label="{comment}">
          <Input v-model:value="queryParams.{javaField}" />
        </Form.Item>
      </Form>
    </Card>
    <Card>
      <VxeTable :data="list" :loading="loading" show-overflow>
        <VxeColumn field="{javaField}" title="{comment}" />
        <!-- 更多列... -->
        <VxeColumn field="operation" title="操作">
          <template #default="{row}">
            <Button type="link" @click="handleEdit(row)">编辑</Button>
            <Button type="link" danger @click="handleDelete(row)">删除</Button>
          </template>
        </VxeColumn>
      </VxeTable>
      <Pagination :total="total" v-model:current="queryParams.pageNo" @change="getList" />
    </Card>
  </Page>
</template>
```

### 11.4 UniApp 移动端模板

#### 目录结构
```
src/
├── api/{moduleName}/{businessName}.ts   # API
└── pages-{moduleName}/{businessName}/
    ├── index.vue                        # 列表页
    ├── form/index.vue                   # 表单页
    ├── detail/index.vue                 # 详情页
    └── components/
        └── search-form.vue              # 搜索组件
```

#### API 定义
```typescript
import { http } from '@/http/http'
import type { PageParam, PageResult } from '@/http/types'

export interface {ClassName} {
  id?: number
  {javaField}?: string
  // ...
}

export const get{ClassName}Page = (params: PageParam) =>
  http.get<PageResult<{ClassName}>>('/{moduleName}/{className_strike_case}/page', params)

export const get{ClassName} = (id: number) =>
  http.get<{ClassName}>(`/{moduleName}/{className_strike_case}/get?id=${id}`)

export const create{ClassName} = (data: {ClassName}) =>
  http.post<number>('/{moduleName}/{className_strike_case}/create', data)

export const update{ClassName} = (data: {ClassName}) =>
  http.put<boolean>('/{moduleName}/{className_strike_case}/update', data)

export const delete{ClassName} = (id: number) =>
  http.delete<boolean>(`/{moduleName}/{className_strike_case}/delete?id=${id}`)
```

#### 列表页 (index.vue)
```vue
<template>
  <view class="yd-page-container">
    <wd-navbar title="{table.classComment}管理" left-arrow @click-left="handleBack" />
    <SearchForm @search="handleQuery" @reset="handleReset" />

    <view v-for="item in list" :key="item.id" class="mb-24rpx bg-white rounded-12rpx">
      <view class="p-24rpx" @click="handleDetail(item)">
        <view class="text-32rpx font-semibold">{{ item.{titleField} }}</view>
        <view class="mt-12rpx text-28rpx text-[#666]">{comment}: {{ item.{javaField} }}</view>
      </view>
    </view>

    <wd-loadmore :state="loadMoreState" @reload="loadMore" />
    <wd-fab v-if="hasAccessByCodes(['{permissionPrefix}:create'])" @click="handleAdd" />
  </view>
</template>

<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { onReachBottom } from '@dcloudio/uni-app'
import { get{ClassName}Page } from '@/api/{moduleName}/{businessName}'

const list = ref<{ClassName}[]>([])
const queryParams = ref({ pageNo: 1, pageSize: 10 })

const getList = async () => {
  const data = await get{ClassName}Page(queryParams.value)
  list.value = [...list.value, ...data.list]
  loadMoreState.value = list.value.length >= data.total ? 'finished' : 'loading'
}

const handleQuery = (data) => { /* 搜索 */ }
const handleReset = () => { /* 重置 */ }
const loadMore = () => { /* 加载更多 */ }
const handleAdd = () => uni.navigateTo({ url: '/pages-{moduleName}/{businessName}/form/index' })
const handleDetail = (item) => uni.navigateTo({ url: `/pages-{moduleName}/{businessName}/detail/index?id=${item.id}` })

onReachBottom(() => loadMore())
onMounted(() => getList())
</script>
```

## 12. 前端模板变量

| 变量 | 说明 |
|------|------|
| `${moduleName}` | 模块名 |
| `${businessName}` | 业务名（小写） |
| `${className}` | PascalCase 类名 |
| `${simpleClassName}` | 简化类名（去前缀） |
| `${classNameVar}` | camelCase 变量名 |
| `${simpleClassName_strikeCase}` | 小写下划线 |
| `${permissionPrefix}` | 权限前缀 |
| `${primaryColumn.javaType}` | 主键 Java 类型 |
| `${primaryColumn.javaField}` | 主键字段名 |
| `${treeParentColumn.javaField}` | 树父字段名 |
| `${treeNameColumn.javaField}` | 树名称字段名 |

## 13. 前端 HTML 类型映射

| 后端类型 | Element Plus | Vben/Vben5 | UniApp |
|----------|-------------|------------|--------|
| String + input | el-input | Input | - |
| Integer/Long + select | el-select | Select | wd-select |
| Boolean + checkbox | el-checkbox | Checkbox | - |
| LocalDateTime + date | el-date-picker | DatePicker | - |
| LocalDateTime + daterange | el-date-picker (daterange) | RangePicker | - |
| imageUpload | UploadImg | FileUpload (image) | - |
| fileUpload | UploadFile | FileUpload (file) | - |
| editor | Editor | Editor | - |

## 14. 包路径模板变量

| 变量 | 说明 |
|------|------|
| `${basePackage}` | 基础包名，如 `com.ruoyi` |
| `${jakartaPackage}` | Jakarta 包名，如 `jakartaakarta` |
| `${table.moduleName}` | 模块名 |
| `${table.businessName}` | 业务名（小写） |
| `${table.className}` | 类名（PascalCase） |
| `${table.classComment}` | 类注释 |
| `${sceneEnum.basePackage}` | 场景包（admin/app/api） |
| `${sceneEnum.prefixClass}` | 类名前缀 |
| `${permissionPrefix}` | 权限前缀 |
