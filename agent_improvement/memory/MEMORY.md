# Claude Memory Index

本目录存储 Claude Code 的规则和记忆文件。

## 文件

- [codegen-rules.md](codegen-rules.md) - yudao-module-infra 代码生成器模板规则

## 规则说明

### codegen-rules.md
基于 `yudao-module-infra/src/main/resources/codegen` Velocity 模板库总结的业务系统代码生成规范。

包含：
- 后端：DO/Mapper/Service/Controller/VO 分层结构
- 前端：Vue3 Element Plus、Vue3 Vben Admin、Vben5 Antd、UniApp 移动端模板
- 命名约定（PascalCase/camelCase/kebab-case）
- 模板类型：通用(1)、树表(2)、ERP主表(11)
- 主子表处理逻辑
- VO 类型：PageReqVO/ListReqVO/SaveReqVO/RespVO
