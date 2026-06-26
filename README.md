# Report 服务说明

`report` 是报告服务后端微服务骨架。

当前项目从现有 Java 微服务工程骨架迁移而来，只保留基础工程、统一异常处理、DDL 入口、CI、脚本和 AI 编码规范；不包含任何历史服务业务代码，也不包含 `user` 服务中的用户、租户、角色、权限资源等业务类。

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
  admin-server-url: <ADMIN_SERVER_URL>
  zipkin-base-url: <ZIPKIN_BASE_URL>
  local-service-host: <LOCAL_SERVICE_HOST>
```

业务配置只引用公共变量，不直接散落基础设施 IP。

## Nacos 配置

本地 `application.yml` 只保存连接 Nacos 需要的启动入口和 `spring.config.import` 远程配置列表。当前 report 加载：

```text
logging.yml
reuse-configuration.yaml
security-auth.yaml
report.yaml
report-spring.yaml
redis.yaml
seata.yaml
xxl-job.yaml
zipkin.yaml
admin.yaml
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

当前报告模块默认初始化公共基础设施表：

```text
src/main/resources/db/common-infra-schema.sql
```

该脚本只维护 MyBatis-Plus DDL 记录表 `ddl_history` 和 Seata AT 事务补偿表 `undo_log`，不包含报告业务表。全新或空业务库首次启动前，必须先在目标业务库手动执行同级 `../utils/src/main/resources/db/common-infra-schema.sql`；Seata AT 会在 `DataSource` 初始化时先检查 `undo_log`，不能依赖应用首次启动自动创建该表。

后续新增报告业务表时：

- SQL 放在 `src/main/resources/db/*.sql`。
- 先检查目标数据库 `ddl_history`。
- 已执行或无法确认是否执行过的 SQL 文件不得回改。
- 新增变更使用新的 SQL 文件，并追加到 `MysqlDdl#getSqlFiles()`，不要删除 `db/common-infra-schema.sql`。
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
