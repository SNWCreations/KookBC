# KookBC 项目架构文档

## 变更记录 (Changelog)

### 2025-09-23 19:21:26
- 🚀 **首次架构扫描完成** - 通过自适应初始化架构对整个 KookBC 项目进行了深度分析
- 📊 **识别核心模块** - 发现主要模块包括核心实现、命令系统、事件系统、网络通信、插件管理等
- 🔧 **技术栈识别** - Java 8, Gradle 构建, 基于 JKook API 的 Kook Bot 客户端实现
- 📝 **文档初始化** - 建立了项目的基础架构文档和模块索引

### 2025-09-23 19:21:26 (补充扫描)
- 🔍 **深度模块发现** - 使用 everything-search 补完了网络通信、插件系统、实体系统详细架构
- 📋 **配置文件识别** - 发现完整的构建配置、资源配置、CI/CD 工作流
- 🌐 **网络子模块解析** - WebSocket (ws/) 和 Webhook (webhook/) 双模式实现详情
- 🔌 **插件生态完善** - Mixin 插件支持、ClassLoader 隔离机制识别
- ⚠️ **测试缺口确认** - 项目缺少完整的测试体系，需要建立 JUnit 5 + Mockito 框架

---

## 项目愿景

KookBC 是 [JKook API](https://github.com/SNWCreations/JKook) 的标准 Java 客户端实现，为 Kook 聊天平台提供了完整的 Bot 开发框架。项目致力于提供：

- 🤖 **完整的 Bot 客户端** - 支持 WebSocket 和 Webhook 两种连接模式
- 🔌 **插件系统** - 基于 JKook API 的灵活插件架构，支持 Mixin 扩展
- 🎯 **命令框架** - 内置 LiteCommands 框架，提供现代化的命令处理系统
- 🌐 **网络通信** - 高效的 HTTP API 客户端和实时事件处理
- 🛡️ **稳定可靠** - 完善的错误处理、重连机制和权限管理

## 架构总览

KookBC 采用模块化的单体架构，主要分为以下几个层次：

```
┌─────────────────────────────────────┐
│            应用入口层                │  LaunchMain.java / Main.java
├─────────────────────────────────────┤
│            核心业务层                │  CoreImpl / KBCClient
├─────────────────────────────────────┤
│         功能模块层                   │  Command / Event / Network / Plugin
├─────────────────────────────────────┤
│         JKook API 抽象层             │  实现 JKook 接口规范
├─────────────────────────────────────┤
│         基础设施层                   │  HTTP / WebSocket / Storage / Scheduler
└─────────────────────────────────────┘
```

### 核心技术栈
- **语言**: Java 8+
- **构建工具**: Gradle 与 Kotlin DSL
- **HTTP 客户端**: OkHttp 3
- **WebSocket**: OkHttp WebSocket
- **JSON 处理**: Google GSON
- **日志框架**: Apache Log4j2
- **命令框架**: LiteCommands
- **字节码操作**: SpongePowered Mixin (FabricMC Mixin)
- **控制台**: JLine 3, TerminalConsoleAppender

## 模块结构图

```mermaid
graph TD
    A["(根) KookBC"] --> B["src/main/java"];
    B --> C["snw.kookbc"];
    C --> D["impl"];
    D --> E["command"];
    D --> F["entity"];
    D --> G["event"];
    D --> H["network"];
    D --> I["plugin"];
    D --> J["mixin"];
    D --> K["console"];
    D --> L["launch"];
    D --> M["message"];
    D --> N["scheduler"];
    D --> O["storage"];
    A --> P["docs"];
    A --> Q["src/main/resources"];
    A --> R[".github"];
    A --> S["buildSrc"];

    click C "#core-implementation" "查看核心实现模块"
    click E "#command-system" "查看命令系统模块"
    click F "#entity-system" "查看实体系统模块"
    click G "#event-system" "查看事件系统模块"
    click H "#network-system" "查看网络通信模块"
    click I "#plugin-system" "查看插件系统模块"
    click P "#documentation" "查看文档模块"
```

## 模块索引

| 模块路径 | 职责描述 | 入口类 | 重要文件 | 状态 |
|---------|---------|--------|----------|------|
| `src/main/java/snw/kookbc` | 核心启动与主要实现 | `Main.java`, `LaunchMain.java` | `CoreImpl.java`, `KBCClient.java` | ✅ 核心 |
| `src/main/java/snw/kookbc/impl` | 核心业务实现层 | `CoreImpl.java`, `KBCClient.java` | `HttpAPIImpl.java` | ✅ 核心 |
| `src/main/java/snw/kookbc/impl/command` | 命令系统实现 | `CommandManagerImpl.java` | `LiteCommandsImpl.java` | ✅ 完善 |
| `src/main/java/snw/kookbc/impl/entity` | 实体与对象模型 | `*Impl.java` 各实体实现 | `EntityBuilder.java`, `MessageBuilder.java` | ✅ 完善 |
| `src/main/java/snw/kookbc/impl/event` | 事件系统实现 | `EventManagerImpl.java` | `EventFactory.java`, `EventTypeMap.java` | ✅ 完善 |
| `src/main/java/snw/kookbc/impl/network` | 网络通信层 | `NetworkClient.java` | `HttpAPIRoute.java`, `Bucket.java` | ✅ 完善 |
| `├─ network/ws/` | WebSocket 连接实现 | `OkhttpWebSocketNetworkSystem.java` | `Connector.java`, `Reconnector.java` | ✅ 核心 |
| `├─ network/webhook/` | Webhook 服务器实现 | `JLHttpWebhookNetworkSystem.java` | `JLHttpWebhookServer.java`, `EncryptUtils.java` | ✅ 核心 |
| `├─ network/policy/` | 限流策略实现 | `RateLimitPolicy` 实现类 | `WaitUntilOKRateLimitPolicy.java` | ✅ 策略 |
| `src/main/java/snw/kookbc/impl/plugin` | 插件管理系统 | `SimplePluginManager.java` | `SimplePluginClassLoader.java`, `MixinPluginManager.java` | ✅ 完善 |
| `src/main/java/snw/kookbc/impl/mixin` | Mixin 字节码支持 | `MixinServiceKookBC.java` | Mixin 服务配置 | ✅ 高级 |
| `docs/` | 项目文档 | 各类 Markdown 文档 | `Embedding_KookBC.md`, `The_Design_of_KookBC.md` | ✅ 齐全 |
| `src/main/resources` | 配置与资源文件 | `kbc.yml`, `log4j2.xml` | `META-INF/services/` 服务配置 | ✅ 配置 |
| `.github/` | CI/CD 与项目模板 | 工作流配置文件 | `build.yml`, `publish.yml`, `snapshot.yml` | ✅ 运维 |
| `buildSrc/` | Gradle 构建配置 | 发布约定配置 | `publish-conventions.gradle.kts` | ✅ 构建 |

## 运行与开发

### 快速启动
```bash
# 下载最新版本并启动（会生成配置文件）
java -jar kookbc-<version>.jar

# 配置 token 后再次启动
java -jar kookbc-<version>.jar
```

### 开发环境
```bash
# 克隆项目
git clone https://github.com/SNWCreations/KookBC.git
cd KookBC

# 构建项目
./gradlew build

# 构建带 Shadow 的完整 JAR
./gradlew shadowJar

# 跳过 Shadow 构建
./gradlew build -PskipShade=true
```

### 配置文件
- **主配置**: `kbc.yml` - Bot token、连接模式、Webhook 设置等
- **日志配置**: `src/main/resources/log4j2.xml`
- **构建配置**: `build.gradle.kts` 主构建脚本
- **版本管理**: `gradle.properties` 项目属性

## 测试策略

⚠️ **当前项目缺少测试模块** - 建议添加以下测试：

### 建议测试范围
1. **单元测试**
   - 核心 API 实现 (`CoreImpl`, `HttpAPIImpl`)
   - 实体对象构建 (`EntityBuilder`, `MessageBuilder`)
   - 命令解析与执行
   - 事件处理机制

2. **集成测试**
   - 网络连接 (WebSocket/Webhook)
   - 插件加载与通信
   - 配置文件解析

3. **端到端测试**
   - 完整的 Bot 启动流程
   - 命令响应流程
   - 事件接收与处理

### 推荐测试框架
- **JUnit 5** - 主要测试框架
- **Mockito** - Mock 框架
- **TestContainers** - 集成测试环境

## 编码规范

### Java 代码规范
- **版本**: Java 8 兼容
- **编码**: UTF-8
- **包结构**: `snw.kookbc.*` 命名空间
- **许可证头**: 每个 Java 文件包含 AGPL-3.0 许可证声明

### 依赖管理
- 使用 Gradle Version Catalog (`gradle/libs.versions.toml`)
- Shadow 插件打包所有依赖
- 谨慎添加新依赖，避免冲突

### Git 工作流
- **主分支**: `main` (稳定版本)
- **开发分支**: `dev` (活跃开发)
- **贡献流程**: Fork → Feature Branch → Pull Request
- **一个 PR 只解决一个问题**

## AI 使用指引

### 代码理解要点
1. **启动流程**: `LaunchMain` → `Main` → `KBCClient` → 各模块初始化
2. **核心接口**: 实现 JKook API 规范，重点关注 `Core` 接口实现
3. **插件系统**: 基于 ClassLoader 隔离，支持 Mixin 字节码增强
4. **网络层**: 双模式支持 WebSocket 实时连接和 Webhook 回调
5. **命令系统**: LiteCommands 框架 + 内置命令 + 插件命令

### 常见任务
- **添加新功能**: 遵循现有的模块划分，在对应 `impl` 包下实现
- **修复 Bug**: 重点关注网络重连、事件处理顺序、内存泄漏
- **性能优化**: 关注事件处理线程池、HTTP 连接复用、缓存策略
- **插件开发**: 参考 JKook API 文档和现有内置命令实现

### 架构决策记录
- **单体架构**: 便于部署和调试，通过模块化保持可维护性
- **Java 8**: 保持较广的兼容性，支持旧版本服务器环境
- **Mixin 支持**: 为高级插件提供字节码操作能力
- **双网络模式**: 适应不同的部署环境和性能需求

### 注意事项
- 所有网络操作需要考虑重连和错误处理
- 插件加载使用独立 ClassLoader，注意类加载顺序
- 事件处理支持 SN 顺序检查，确保消息不重复处理
- 配置文件变更需要考虑向后兼容性