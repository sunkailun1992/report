# GitHub 环境配置入口规范

个人项目与美联公司开发环境分别维护。代码使用 main（正式主干）、test（集成测试）、preprod（发布候选）和短功能分支；Git 阶段不自动切换 Spring profile 或 Nacos namespace，也不代表环境已部署。

| profile | 默认 namespace | Nacos 地址变量 |
|---|---|---|
| dev | github-dev | GITHUB_NACOS_DEV_SERVER_ADDR → GITHUB_NACOS_SERVER_ADDR；缺失即失败 |
| test | github-test | GITHUB_NACOS_TEST_SERVER_ADDR → GITHUB_NACOS_SERVER_ADDR → localhost |
| prod | github-prod | GITHUB_NACOS_PROD_SERVER_ADDR → GITHUB_NACOS_SERVER_ADDR → localhost |

各环境 namespace 仅由 GITHUB_NACOS_<ENV>_NAMESPACE 显式覆盖，不再接受旧 NACOS_NAMESPACE 回退。当前只建立 github-dev；github-test/github-prod 未创建、未部署，不得指向公司 dev 临时替代。

application.yml 保留端口、服务名、默认 APP_ENV=dev 与 GITHUB_NACOS_GROUP，原有内容不变；dev 的本机引导键通过标准外置配置文件 config/application-dev.properties 自动读取。application-*.yml 同时声明连接地址、namespace、config/discovery 绑定和本 profile 的 spring.config.import。真实中间件、业务开关和密钥仍在对应 Nacos；不将业务配置搬回本地 profile。

config 与 discovery 共用 custom.nacos-ip/custom.namespace。Dubbo、Nacos AI Registry 和 Prompt SDK 也必须显式引用它们；独立 SDK 的 username/password 不能假设继承 Spring Cloud 配置。Gateway 不承担业务认证。

个人环境的可提交模板、私有渲染、CAS 发布、数据库与统一 X File Storage 入口统一见同级 utils/ops/github-dev/README.md。私有环境源保存在 ~/.config/kellen-github/；各服务 config/application-dev.properties 是受控生成且精确忽略的本机副本，不进 Git。前端不连接 Nacos，后端 base URL 沿用 http://localhost:7100。

只读校验：在 utils 运行 python3 ops/github-dev/scripts/verify_configuration.py。发布前读回旧配置，已有内容更新必须 CAS；发布后逐项读回，不能以命令成功代替运行验收。

## IDE、bootRun 与 java-jar 的 dev 引导

- 私有引导文件固定为服务仓库根目录下 `config/application-dev.properties`，利用 Spring Boot 标准 `file:./config/` 外置配置搜索路径加载；base `application.yml` 不增加私有 import，原有文档保持不变。不要把私有文件加入 `application-dev.yml` 的同一份 Nacos import 清单，这不能保证 Nacos resolver 的读取时序。
- IDE Run/Debug 的 working directory 必须是当前服务仓库根目录；`./gradlew bootRun` 使用 Gradle 项目目录；普通 `java -jar build/libs/<service>.jar` 从当前服务仓库根目录执行。改变程序工作目录时必须显式配置外置位置，不能假定 `config/` 相对 jar 所在目录解析。
- 私有文件由同级 `utils/ops/github-dev` 的受控本机入口生成，权限为 `0600`，只保存必要 Nacos 引导键。不要把 shell dotenv 原文直接当 properties，不提交真实地址、用户名、密码或文件内容。`.gitignore` 只精确排除 `/config/application-dev.properties`，不忽略公共配置源码。
- OS 环境变量优先于私有 properties，已有 launcher 注入的环境仍可覆盖文件值。dev 地址依次取 `GITHUB_NACOS_DEV_SERVER_ADDR`、`GITHUB_NACOS_SERVER_ADDR`，不再回落 `127.0.0.1`。文件不存在可以使用完整环境变量启动，但文件和环境都未提供地址时必须在启动阶段失败，不能连接默认本机 Nacos。
- `spring.profiles.active` 的显式启动参数优先于默认 `APP_ENV=dev`。仅选择 `test` 或 `prod` 时，不读取 `application-dev.properties`，继续使用对应 profile 的独立 namespace；不得拿公司 dev 作为替代。
- Nacos 控制台端口 `18080` 只用于浏览器管理；SDK 配置与服务发现使用 `8848`，并需开放对应 gRPC 端口 `9848`。控制台 URL 不能直接填到 SDK server-addr；实际主机与凭据仅写入本机私有文件。
- 验收覆盖默认 dev 外置文件在 Nacos resolver 前可见、OS 环境覆盖私有值、test/prod 不读取 dev 文件，以及缺少文件和地址时启动失败。配置解析测试不替代真实 Nacos、数据库或业务接口验收。
