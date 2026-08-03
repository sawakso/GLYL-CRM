# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是 Cordys CRM 的**纷享销客风格独立定制版**，服务端口固定 `5174`，数据库/Redis/容器/网络/数据卷均与原 Cordys CRM 隔离。上游仓库：`1Panel-dev/CordysCRM`。

## 常用命令

### 环境准备

```bash
# 1. 安装父 POM（必须先执行，让子模块继承 properties）
./mvnw install -N

# 2. 安装前端依赖
cd frontend && pnpm install
```

### 后端

```bash
# 构建所有后端模块（跳过测试）
./mvnw clean install -DskipTests -DskipAntRunForJenkins --file backend/pom.xml

# 整体打包
./mvnw clean package
```

### 前端

```bash
# Web 端（Naive-UI，桌面端）
cd frontend/packages/web && pnpm run dev

# Mobile 端（Vant-UI，移动端）
cd frontend/packages/mobile && pnpm run dev

# 统一构建前端
cd frontend && pnpm run build
```

### Docker 部署

```powershell
Copy-Item installer/.env.example installer/.env
# 编辑 installer/.env，替换所有 CHANGE_ME
docker compose -f installer/docker-compose.fxiaoke.yml up -d --build
# 访问 http://localhost:5174
```

### 日常协作

```powershell
git pull --rebase
git switch -c feature/<功能名称>
# 提交到功能分支并发起 MR，不要直接推 main
```

## 架构总览

### 后端三层模块

| 模块 | 定位 | 关键内容 |
|---|---|---|
| `backend/framework` | 核心框架层 | Shiro 安全 (`ShiroFilter`)、自定义 MyBatis DAL (`DataAccessLayer`)、AOP 操作日志、Snowflake ID 生成器、通用工具 |
| `backend/crm` | 业务层 | 16 个业务领域（clue/customer/opportunity/contract/order/product/approval/workflow/form/marketingform/dashboard/search/system/follow/home/integration）+ `cn.cordys.common` 共享服务 |
| `backend/app` | 入口 | `cn.cordys.Application`，嵌入式 MySQL + Redis，Flyway 迁移 |

### 自定义 DAL 层（核心模式）

项目**不使用**标准 MyBatis Mapper 扫描。CRUD 通过 `DataAccessLayer.with(Entity.class)` 链式调用，SQL 根据实体注解动态生成：

```java
// 查询
DataAccessLayer.with(Clue.class).select(criteria);
DataAccessLayer.with(Clue.class).selectListByLambda(wrapper);

// 插入/更新/删除
DataAccessLayer.with(Clue.class).insert(entity);
DataAccessLayer.with(Clue.class).updateById(entity);
DataAccessLayer.with(Clue.class).delete(criteria);
```

- **Lambda 查询**：`LambdaQueryWrapper<Clue>` 提供类型安全的动态条件
- **复杂查询**：写在 `ExtXxxMapper.xml`（如 `ExtClueMapper.xml`），通过传统 MyBatis Mapper 接口调用
- **实体映射**：`@EntityTable` 注解标记的 domain 类字段通过 `EntityTableMapper.extractTableInfo()` 自动映射

### 前端 Monorepo

| 包 | 框架 | 用途 |
|---|---|---|
| `packages/lib-shared` | — | 共享：API 封装、hooks、枚举、工具函数、数据模型、i18n、类型 |
| `packages/web` | Naive-UI | 桌面端，Hash 路由，views 按业务模块组织 |
| `packages/mobile` | Vant-UI | 移动端，支持企业微信登录 |

### 后端业务域结构

每个 CRM 业务域（如 `crm/clue`）统一采用以下包结构：

```
clue/
├── controller/    # REST 控制器
├── service/       # 业务逻辑
├── domain/        # 实体类（@EntityTable）
├── dto/
│   ├── request/   # 请求 DTO
│   └── response/  # 响应 DTO
├── mapper/        # MyBatis Mapper（仅用于复杂 XML 查询）
├── constants/     # 域常量
└── utils/         # 域工具
```

`cn.cordys.common` 提供共享基类：`BaseService`、`BaseResourceService`、`BaseExportService`、`DataScopeService`、`DataInitService`。

### 安全与认证

- **Shiro**（Jakarta 适配）：`ShiroFilter` 处理认证/授权
- **Session**：Redis 存储，`SessionUtils` 获取当前用户
- **操作日志**：通过 `@OperationLog` 注解驱动，`OperationLogAopAdvisor` AOP 拦截
- **数据权限**：`DataScopeService` 实现行级数据隔离

### 配置体系

- 部署配置：`installer/conf/cordys-crm.properties`（MySQL/Redis/MCP 等）
- 环境变量：`installer/.env`（密码和密钥，不提交 Git）
- 嵌入式服务：MySQL 和 Redis 均为容器内嵌入式运行（`mysql.embedded.enabled=true` / `redis.embedded.enabled=true`）

### 关键依赖

- Spring Boot 3.5.14 / Java 21 / Jetty（非 Tomcat）
- MyBatis 3.0.5 + PageHelper 6.1.1
- Shiro 2.1.0 (Jakarta classifier)
- Redisson 3.52.0 / Spring Session Redis
- Flyway（表版本前缀：`fxiaoke_crm_5174_version`）
- Quartz（调度器名：`fxiaoke-crm-5174-quartz`）
- FastExcel 1.3.0（导入导出）
- 前端：Vue 3.5.22 / Vite / TypeScript 5.9 / Pinia / vue-i18n / ECharts
