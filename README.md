# Report 服务说明

`report` 是报告服务后端微服务骨架。

当前项目从现有 Java 微服务工程骨架迁移而来，只保留基础工程、统一异常处理、DDL 入口、CI、脚本和 AI 编码规范；不包含任何历史服务业务代码，也不包含 `user` 服务中的用户、租户、角色、权限资源等业务类。

个人公共库固定为 `com:utils:1.5.5`、`com:rpc-api:1.0.7`。个人与美联共用同一阿里云私有 Maven 地址：个人沿用 `com` group，公司保留 `com.kellen` group；artifact 名仍为 `utils` / `rpc-api`，Java package 不随 Maven 坐标改变。公共库须先完成 Maven Local 验证并独立发布私库，消费者再按精确坐标拉取；已有同版制品不得覆盖。

## 启动入口

主应用类为 `com.kellen.ReportApplication`，IDE 可直接运行该类。Gradle `bootRun` 与可执行 JAR 的入口由 `springBoot.mainClass` 统一指定；仍使用 `./gradlew bootRun` 或 `java -jar build/libs/report.jar`，工作目录保持为当前服务仓库根目录。

## 技术基线

- Java 17
- Spring Boot 4.0.4
- Spring Cloud 2025.1.1
- Spring Cloud Alibaba 2025.1.0.0
- Nacos Client 3.2.2
- Seata Server 2.7.0
- Seata Client 2.6.0
- MyBatis-Plus
- 公共能力依赖 `com:utils`
- 测试框架统一使用 JUnit 5

## 服务职责

当前服务定位为报告模块，后续新增业务时优先围绕以下边界展开：

- 报告生成、解析、归档、查询、审核、分享和导出。
- 报告文件、附件、预览图和外部存储访问控制。
- 报告归属、租户隔离、部门/负责人数据权限和字段脱敏。
- 异步生成、重试、补偿、进度记录和任务幂等。
- 按真实需求接入 RabbitMQ、XXL-JOB、Dubbo RPC 或第三方通知能力。

公共工具、认证上下文、多租户、统一返回、统一异常、数据权限、MyBatis-Plus 公共配置等能力不在本服务重复实现，统一复用同级 `utils` 项目。

## 基础设施地址

除 `application.yml` 中连接 Nacos 自身的启动入口外，MySQL、Redis、RabbitMQ、Seata、XXL-JOB、Elasticsearch、Kibana、Zipkin 等基础设施地址统一放在 Nacos `reuse-configuration.yaml`。

蒲公英、Tailscale、节点小宝等组网地址变化时，优先只修改公共配置中的基础设施变量，例如：

```yaml
custom:
  infra-host: <INFRA_HOST>
  infra-nacos-addr: ${custom.infra-host}:8848
  infra-mysql-addr: ${custom.infra-host}:3306
  infra-redis-addr: ${custom.infra-host}:6379
  infra-rabbitmq-addr: ${custom.infra-host}:5672
  infra-seata-addr: ${custom.infra-host}:8091
  infra-xxl-job-admin: http://${custom.infra-host}:19090/xxl-job-admin
  infra-elasticsearch-addr: ${custom.infra-host}:9200
  infra-elasticsearch-uri: http://${custom.infra-host}:9200
  local-service-host: <LOCAL_SERVICE_HOST>
```

业务配置只引用公共变量，不直接散落基础设施 IP。

## Nacos 配置

### 普通开发启动

默认 profile 为 `dev`。Spring Boot 会自动读取服务根目录下的 `config/application-dev.properties`：IDE Run/Debug 的 working directory 必须设为当前服务仓库根目录；`./gradlew bootRun` 使用项目目录；`java -jar build/libs/<service>.jar` 也从服务仓库根目录执行。base `application.yml` 保持原样，不增加私有 import。

该本机私有文件由同级 `utils/ops/github-dev` 的受控环境入口生成，权限保持 `0600`，仅保存 Nacos 引导键，已按精确路径忽略；不要提交地址、用户名或密码，也不要把 shell dotenv 原文直接当 properties。OS 环境变量优先于私有文件，原 launcher 的 env 注入仍然有效。dev 地址依次取 `GITHUB_NACOS_DEV_SERVER_ADDR`、`GITHUB_NACOS_SERVER_ADDR`，namespace 保留 `github-dev`；私有文件不存在且环境未提供地址时，启动必须失败，不再尝试本机 Nacos。

显式 `spring.profiles.active` 可覆盖默认 `APP_ENV=dev`；仅选择 `test` 或 `prod` 时不读取 dev 私有文件，使用各自 profile 的地址与 namespace。控制台入口端口 `18080` 与 SDK 配置/注册服务端口 `8848` 不可混用；SDK 还需能访问 gRPC 端口 `9848`。真实主机与凭据仅保存在本机私有配置。完整约定见 `docs/ai-coding/ENVIRONMENT_CONFIG_SPEC.md`。

### 远程配置列表

base `application.yml` 保留服务入口，dev 私有引导由外置 `config/application-dev.properties` 提供；`application-*.yml` 保存对应 profile 的 Nacos 连接与远程配置列表。当前 report 加载（完整列表以对应 profile 为准）：

```text
logging.yml
reuse-configuration.yaml
security-auth.yaml
report.yaml
report-spring.yaml
redis.yaml
seata.yaml
xxl-job.yaml
mybatis-plus.yaml
dubbo.yaml
rabbitmq.yaml
elasticsearch.yaml
```

## RabbitMQ

RabbitMQ 公共增强配置统一放在 `utils`：

```text
utils/src/main/java/com/kellen/config/rabbit/RabbitMqConfig.java
```

公共配置负责：

- 复用 Spring Boot 标准 `spring.rabbitmq.*` 自动配置。
- 注册 RabbitMQ JSON 消息转换器。
- 发送 MQ 消息时透传当前动态数据源、`traceId` 和租户上下文。
- 消费 MQ 消息时恢复动态数据源、`traceId` 和租户上下文。
- 消费完成后清理线程上下文，避免消费线程复用串数据。

当前 `report` 没有业务队列。后续业务服务只声明自己的 exchange、queue、binding 和 listener；不得在业务服务复制公共 MQ 配置。

## DDL

MyBatis-Plus DDL 入口在：

```text
src/main/java/com/kellen/bean/MysqlDdl.java
```

当前报告模块暂无业务表初始化脚本，`MysqlDdl#getSqlFiles()` 返回空列表。

全新或空业务库首次启动前，必须先在目标业务库手动执行同级公共基础设施脚本：

```text
../utils/src/main/resources/db/common-infra-schema.sql
```

该脚本只维护 MyBatis-Plus DDL 记录表 `ddl_history` 和 Seata AT 事务补偿表 `undo_log`，不包含报告业务表。Seata AT 会在 `DataSource` 初始化时先检查 `undo_log`，不能依赖应用首次启动自动创建该表。

后续新增报告业务表时：

- SQL 放在 `src/main/resources/db/*.sql`。
- 先检查目标数据库 `ddl_history`。
- 已执行或无法确认是否执行过的 SQL 文件不得回改。
- 新增变更使用新的 SQL 文件，并追加到 `MysqlDdl#getSqlFiles()`。
- 表字段、Entity、BO、Query、VO、ServiceQuery、ServiceResults 和测试同步补齐。

## 接口文档

OpenAPI 原始文档地址：

```text
http://127.0.0.1:7600/v3/api-docs
```

第三方文档 UI 已移除，服务只保留标准 OpenAPI3 `/v3/api-docs`。

Controller 必须使用 OpenAPI3 注解：

```java
@Tag(name = "报告管理", description = "维护报告基础信息、归属、状态和文件访问边界")
@Operation(summary = "分页查询报告", description = "按查询条件分页返回报告数据")
```

需要鉴权的接口使用：

```java
@PreAuthorize("hasAuthority('report:record:list')")
```

权限码命名建议使用 `report:<resource>:<action>`，例如：

```text
report:template:list
report:template:create
report:template:update
report:template:delete
report:record:list
report:record:export
```

## RESTful 接口约定

新增 Controller 必须优先使用 RESTful 风格：

```text
GET    /report/manage/records?current=1&size=10
GET    /report/manage/records/options
GET    /report/manage/records/{id}
POST   /report/manage/records
PUT    /report/manage/records/{id}
DELETE /report/manage/records/{id}
```

规则：

- 不使用 `/save`、`/update`、`/remove`、`/select`、`/page` 等动词路径。
- 查询使用 `GET` URL 参数，不给普通查询接口加 `@RequestBody`。
- 分页接口使用 `@GetMapping(params = {"current", "size"})` 表达分页参数存在性。
- `Query.current` 和 `Query.size` 不使用 `@NotNull(groups = Select.class)`，避免 OpenAPI 工具把复用 Query 的 `/options` 接口也标记为必填。
- `options` 只表示轻量选择项集合，不承接完整管理列表。

## AI 编码规范

AI 编码规范入口：

```text
AGENTS.md
docs/ai-coding/README.md
```

阅读顺序：

```text
docs/ai-coding/AI_CODING_GUIDE.md
docs/ai-coding/PROJECT_CODING_SPEC.md
docs/ai-coding/UTILS_PUBLIC_SPEC.md
docs/ai-coding/examples
```

新增或修改 Java 代码时：

- 类、字段、公开方法和关键业务方法必须补充说明职责、业务含义和边界的注释。
- 方法前优先使用 JavaDoc；复杂逻辑优先使用逻辑块前置说明。
- 关键业务逻辑、认证上下文、租户上下文、权限、Redis、MQ、DDL、事务、异常处理和返回值组装必须解释为什么这样做，避免机械逐行注释。
- 公共能力先检查同级 `utils`，不要在 report 微服务里重复写通用工具类。
- 修改完成后同步更新本 README。

## 验证命令

```bash
./gradlew clean compileJava -x test --no-daemon
./gradlew test --no-daemon
bash scripts/check-secrets.sh
```

如果依赖 `utils` 有调整，先在同级 `utils` 项目执行：

```bash
./gradlew publishToMavenLocal
```

## 通用安全与架构同步

公共安全依赖精确固定为 `com:utils:1.5.5`。Actuator 除 health 外要求运行时配置 `security.actuator.token` 并在 `actuatorReq` 请求头提交令牌；缺失或错误令牌一律拒绝。令牌由独立环境配置提供，不写入仓库。隔离的消费者回归测试只装配真实 MVC/Actuator 与安全链，不连接 Nacos、数据库、MQ 或远程服务；它不替代目标环境验收。

版本由 `1.0.3` 升到 `1.0.4`，保持空业务骨架；未引入医疗报告、患者、问卷、CGA、医院投递与业务 DDL。

公共 RPC 契约固定为 `com:rpc-api:1.0.7`，包含供执行层使用的强制验签入口；本服务不调用旧的可选签名方法来授予 Agent 身份。JWT 签发/验签共用运行时 `security.jwt.secret`（Base64 编码且解码至少 32 字节），无内置默认密钥；测试使用独立合成密钥。

## 本批可观测性接入

当前服务版本为 `1.1.1`，公共能力精确消费 `com:utils:1.5.5`，并由公共包传递引入轻量 `com:fleet-observability:0.1.0` 的安全 HTTP 终态事件。公共制品先完成 Maven Local 验证，再按对应 main 确定提交发布远端；目标环境的实际采集与全链读回单独验收。九服务共享 `actuator.yaml` 暴露 health/info/prometheus；除 health 外仍要求 `actuatorReq` 运维令牌，空值或错误值失败关闭。`logging.yml` 使用 ECS 固定字段白名单，未知 MDC、原始消息、正文和异常不进入采集结果；OTel agent 标准 `trace_id/span_id` 与业务会话 ID 分开。应用指标带固定 application/environment 标签，禁止动态业务 ID。

Admin/Zipkin 模板与三 profile 的 imports 已退役。服务 JVM 的可选 OTel agent 由统一启动入口负责，不通过全局 JAVA_TOOL_OPTIONS 注入 Gradle 或测试。部署时 Java 指标只由 Micrometer Prometheus 提供，OTEL_METRICS_EXPORTER 与 OTEL_LOGS_EXPORTER 均为 none；监控组件、配置发布和实际采集仍须独立验证。回滚须恢复匹配的制品与配置，先恢复旧 dataId 再回退旧消费者，不能用已删除的配置项假装完成回滚。
