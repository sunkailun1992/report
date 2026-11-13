# AGENTS.md

本文件是 `report` 服务的 AI 编码入口。AI 修改本项目代码前，必须先阅读本文件，再按任务风险阅读 `README.md` 和 `docs/ai-coding` 下的规范。

## Nacos 配置注释要求

- 新增或修改 Nacos 配置、模板、引导配置及渲染/发布工具前，必须阅读 [Nacos 注释与归类规范](docs/ai-coding/NACOS_COMMENT_SPEC.md)。
- 每个 dataId 写明职责、环境归属、消费者；每个可调参数写明用途、必要单位、格式、空值及开关影响。YAML 使用 `#` 注释，渲染结果必须保留说明。
- 注释整理保持键、类型、值、占位符和列表/import 顺序不变；发布前读回权威配置并备份，使用旧版本比对/CAS，发布后逐项读回。真实凭据不得进入 Git 或注释。
- 只处理明确授权的仓库及环境；Yudao 项目排除，不随本规范同步修改。

## Git 分支、完整交付与清理

- 长期分支只保留 `main`（正式主干），禁止 force push 和删除，不再创建 `test`、`preprod`。新任务 fetch 后从最新 `origin/main` 的确定 SHA 建独立短分支，续作可复用已核实归属的本功能分支，保留其他会话与无关 dirty。
- 完整功能包含必要代码、测试、文档、migration、公共契约和依赖。候选按最新 main 加完整功能及必需跨仓配套验证，不逐提交拼装常规交付。
- 完整功能合入 main、远端 refs 和包含关系核实后，清理本次已结束的本地及远端短分支。未合入、用途不明或被其他 worktree 占用的分支不能删；`main` 永久排除在短分支清理外。详情见 `docs/ai-coding/BRANCHING_SPEC.md`。
- Git 分支不自动切换 profile 或触发部署。现有 APP_ENV=dev、github-dev 与服务根 config/application-dev.properties 模式保持；base application.yml 不改写，未来环境映射和部署另行配置与验收。
- 按风险执行编译、测试、安全专项检查及必要独立验证；Review 是补充，不新增人工代码审批。只按已有用户授权执行提交、推送、合并、tag、制品和部署；纯规范不升业务版本或重复发布 Maven 制品。
- 维护者本人，以及维护者本机用维护者账号运行的 Claude / Codex 会话，可以直接合并到 `main`：合并前本机验证命令全过，合并后看 CI，红了马上修。推送后读回精确提交的 CI 结论。
- 云端会话（`CLAUDE_CODE_REMOTE=true`，或会话里没有本机配置）只推会话分支，由维护者或本机会话合并。其他人的代码走 PR：CI 全绿、维护者审核批准、评审对话解决后再合并。代理不修改 ruleset 与仓库权限，除非用户当场明确要求。

## 项目定位

- 项目名称：`report`
- 项目类型：报告服务后端
- 技术栈：Java 17、Spring Boot、Gradle、MyBatis-Plus、RabbitMQ、Nacos、Dubbo、`com:utils`、`com:rpc-api`
- 同级依赖：`../rpc-api` 提供跨服务 RPC 接口和 DTO 契约；`../utils` 提供公共响应、认证上下文、租户、多数据源和基础工具；`../gateway` 负责路由；`../admin-web` 负责后台页面
- 当前状态：从现有 Java 微服务工程骨架迁移而来，只保留基础工程、配置入口、公共基础设施 DDL、CI、脚本和 AI 编码规范；不携带任何历史服务业务代码。
- 核心风险：报告归属、租户隔离、数据权限、报告文件访问、导出脱敏、异步生成幂等、MQ 和外部存储失败处理

## 修改前阅读顺序

任何代码修改前必须先阅读：

1. `README.md`：确认当前报告服务职责、接口范围、表结构和验证命令。
2. `docs/ai-coding/README.md`：确认 AI 编码入口和阅读顺序。
3. `docs/ai-coding/AI_CODING_GUIDE.md`：确认执行步骤、注释规则、测试和安全要求。
4. `docs/ai-coding/AI_DIRECTORY_STRUCTURE_GUIDE.md`：确认 Java 微服务目录、测试、资源、文档和跨项目边界。
5. `docs/ai-coding/AI_COMMENT_STYLE_GUIDE.md`：确认注释规范、自解释优先、禁止注释掉死代码和排版要求。
6. `docs/ai-coding/AI_DESIGN_PATTERN_GUIDE.md`：确认报告生成、存储、导出、审核、异步任务和通知等设计模式边界。
7. `docs/ai-coding/BRANCHING_SPEC.md`：确认分支命名、短分支生命周期、release/hotfix、tag 和清理规则。
8. `docs/ai-coding/ENVIRONMENT_CONFIG_SPEC.md`：确认环境、Nacos namespace、Java profile 和前端边界。
9. `docs/ai-coding/VERSIONING_SPEC.md`：确认 `group = 'com'`、`version = '1.0.0'`、补丁递增和消费者同步规则。
10. `docs/ai-coding/RPC_API_CODING_SPEC.md`：涉及 Dubbo RPC provider、consumer、接口或 DTO 时必须阅读。
11. `docs/ai-coding/TESTING_SPEC.md`：确认业务模块 SpringBootTest、真实 HTTP 集成测试、测试库和 AssertJ 边界。
12. `docs/ai-coding/PROJECT_CODING_SPEC.md`：确认微服务分层、RESTful、权限、多租户、数据权限和 DDL 规范。
13. `docs/ai-coding/AI_ENGINEERING_GUARDRAILS.md`：确认风险分级、Definition of Done 和交付门禁。
14. `docs/ai-coding/SECURITY_CODING_SPEC.md`：涉及接口、权限、报告内容、数据隔离、脱敏、SQL、XSS、上传下载或测试安全时必须阅读。
15. `docs/ai-coding/UTILS_PUBLIC_SPEC.md`：涉及公共规范、错误码、数据库、乐观锁或 `utils` 能力时阅读。
16. `docs/ai-coding/NACOS_CONFIG_SPEC.md`：修改 Nacos 配置中心、共享 dataId 或 `application.yml` import 前必读。

## 项目边界

- 个人公共库固定使用 `com:utils:1.6.2` 与 `com:rpc-api:1.0.7`；公司库保留 `com.kellen:utils` 与 `com.kellen:rpc-api`。双方共用同一阿里云私有 Maven 地址，沿用各自 group 坐标隔离，不改 Java package，也不迁移到另一仓库或 GitHub Packages。

- `report` 当前只定义报告服务工程边界，业务表、Controller、Service、Mapper、RPC 契约和权限码都需要在真实报告需求明确后再补。
- 新增报告生成、上传、解析、归档、导出、审核、分享、通知或异步任务时，必须优先沿用 `docs/ai-coding/AI_DESIGN_PATTERN_GUIDE.md` 中的 Service Layer、Strategy、Observer、State、Pipeline、Adapter 等项目适用模式。
- 涉及当前用户可见范围时，必须从认证上下文和数据归属推导，不能信任前端传入的用户、租户、部门或归属字段。
- 管理侧、个人侧、异步生成侧和外部回调侧是不同权限场景，不得复用单一权限判断覆盖所有入口。
- 公共响应、认证上下文、多租户、错误码和工具能力优先复用 `../utils`。
- 跨服务 Dubbo RPC 调用优先复用 `../rpc-api` 中的接口和 DTO，不在本服务复制契约。
- 新增本服务 OpenAPI 入口、调整服务前缀，或新增同级 Java 微服务需要接入网关时，必须同步检查 `../gateway` 的 Nacos `gateway-spring.yaml`；需要聚合到 Swagger UI 的服务要补业务路由和 `springdoc.swagger-ui.urls`，并验证对应网关文档路径与 `/swagger-ui/index.html`。

## AI 工程门禁

- 报告生成、文件上传下载、批量导出、分享访问、审核发布、异步任务、数据权限和 MQ 处理默认中高风险。
- 新增或修改功能前，必须按 `AI_AUTOMATION_WORKFLOW.md` 整理需求说明、验收标准和开发手册。
- 完成后必须按 `docs/ai-coding/AI_ENGINEERING_GUARDRAILS.md` 做风险分级、Definition of Done、测试证据、安全检查、风险和回滚说明。
- 涉及当前用户上下文、租户、数据权限、文件访问、MQ 或重复生成时，必须有针对性测试或清楚说明未验证项。
- 测试分层按 `docs/ai-coding/TESTING_SPEC.md` 执行；核心业务不能只靠 mock 或纯对象 `assertThat`，必须补 Spring Boot 级别测试。
- 测试必须稳定可重复，按 `docs/ai-coding/TESTING_SPEC.md`「测试稳定性」：不依赖时序、连接复用或执行顺序；偶发失败按缺陷处理，不用重试、跳过或删除测试掩盖。

## 多智能体协作规则

- 子智能体可以并行分析 Controller、Service、Mapper、DDL、MQ 配置、admin-web 调用、gateway 路由和报告文件链路。
- 不允许多个 worker 同时修改同一核心 Service、Mapper XML、权限规则或 DDL 脚本。
- 报告服务全新或空业务库首次启动前，必须先在目标业务库手动执行 `../utils/src/main/resources/db/postgresql/common-infra-schema.sql`；Seata AT 会在 `DataSource` 初始化时先检查 `undo_log`，不能依赖应用首次启动自动创建该表。新增报告业务 DDL 时追加独立 SQL 文件，不修改已执行的公共基础设施脚本。
- 最终报告归属、访问权限、数据权限、文件权限和测试结论必须由主智能体统一判断。

## 验证命令

按风险选择验证：

```bash
./gradlew clean compileJava -x test
./gradlew test
bash scripts/check-secrets.sh
```

涉及 `rpc-api` 契约、报告生成、文件链路、MQ、权限或数据权限时，还需要说明契约编译、接口验证、数据库验证、Nacos/网关路由验证或依赖外部环境的未验证项。

## 禁止事项

- 禁止信任前端传入的当前用户、租户、报告归属和权限字段。
- 禁止把报告内容、富文本、回调内容、文件解析结果直接作为 HTML 输出或日志全文输出。
- 禁止写死测试用户、测试租户、报告归属人、RabbitMQ 地址、Nacos 地址或本机路径。
- 禁止 AI 触碰真实密钥/凭证、RabbitMQ/数据库密码（疑似密钥只能告警，由项目负责人处理）；配置中心结构性调整（dataId 拆分/合并、import 顺序、`${}` 引用、Nacos/RabbitMQ 接入地址、namespace/group）允许 AI 自主完成，但必须保值不改值，不得擅自变更生产业务配置的实际取值。
- 禁止在当前服务复制 `utils` 公共工具源码或 `rpc-api` 契约源码；业务 RPC 契约缺失时回到真实 `../rpc-api` 实现，公共能力缺失时先评估是否应回到 `../utils` 实现。
