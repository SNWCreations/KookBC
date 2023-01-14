# 浅析 KookBC 的结构与工作流程

编写时间: 2023/1/13

这个项目也有半年了，也算是进入了一个稳定期了。

所以，我总算有空坐下来谈谈 KookBC 的结构与工作流程了。

本文是为准备对 KookBC 进行二次开发的开发者准备的。

建议阅读本文前先阅读 [嵌入 KookBC 到您的项目](Embedding_KookBC.md) 一文，该文章能帮助您理解 KookBC 的生命周期。

在阅读本文前，先明确两点:

* **KookBC 主要基于 AGPLv3 许可证授权，基于 KookBC 进行二次开发的项目受到 AGPLv3 许可证管辖。对于需要用于商业用途的，需要联系 SNWCreations ，并得到 JKook 开发社区中 3/4 的活跃贡献者同意。**

* **KookBC 不是 API。在 KookBC 中，包括但不限于所有的类、方法、字段均有可能在未来被重构或移除。若要了解详细变动，请自行查阅 Git 提交记录。**

## 包结构

先总览 KookBC 的包结构。

KookBC 的主要代码都在 `snw.kookbc.impl` 包中。并且包结构与 JKook API 基本一致。

因此，一个 JKook API 中的接口所对应的 KookBC 实现很好找，基本步骤就是将目标接口的完整限定名中的 `snw.jkook` 改为 `snw.kookbc.impl` ，并在末尾加上 `Impl` 。
* 如 `snw.jkook.Core` 的实现是 `snw.kookbc.impl.CoreImpl`。

以下包(及其子包)是 KookBC 独有的:
```text
snw.kookbc.impl.console
snw.kookbc.impl.network
snw.kookbc.impl.pageiter
snw.kookbc.impl.storage
snw.kookbc.impl.tasks
snw.kookbc.impl.entity.builder
```

## 重要的类

本节的内容将有助于你理解下文有关 KookBC 工作周期的内容。

### KBCClient

位于 `snw.kookbc.impl` 包。

`KBCClient` 表示一个 KookBC 的实例，此类提供了很多有用的方法，允许你访问 KookBC 中的各种组件。

关于此类的详细讲解见 [嵌入 KookBC 到您的项目](Embedding_KookBC.md) 一文。

### Frame

位于 `snw.kookbc.impl.network` 包。

此类借鉴于 KOOK 官方的 [PHP Bot](https://github.com/kaiheila/php-bot) 包的设计。

表示一条来自 KOOK 的消息，是对 KOOK 网络通信中信令的抽象。

### Session

位于 `snw.kookbc.impl.network` 包。

此类也借鉴于 KOOK 官方的 [PHP Bot](https://github.com/kaiheila/php-bot) 包的设计。

表示与服务端的会话，存储了 SN，会话 ID (非 WebSocket 模式下为 `null`) 以及 buffer (在 WebSocket 模式下用于存储乱序消息)

### Listener

是 `snw.kookbc.impl.network.Listener` 接口，不是 `snw.jkook.event.Listener` 接口。

_下文中的 `Listener` 在无特殊说明情况下也均指此接口。_

此接口的实现用于处理 `Frame` ，见下文[事件系统](#事件系统)。

此接口的已知实现是 `snw.kookbc.impl.network.ListenerImpl` 与 `snw.kookbc.impl.network.IgnoreSNListenerImpl` 。

可以通过 `snw.kookbc.impl.network.ListenerFactory#getListener` 方法构造其实例。

### NetworkClient

位于 `snw.kookbc.impl.network` 包。

此类封装了发出 HTTP GET/POST 请求的方法。

整个 KookBC 项目中几乎所有对 KOOK API 的网络请求均由此类完成。

### EventFactory

位于 `snw.kookbc.impl.event` 包。

此类用于构造 JKook API 中的事件。

## 工作流程

### 网络模块

阅读本节前推荐阅读 [JKook API 实现规范](https://github.com/SNWCreations/JKook/wiki/Implementation-Specification) (来自 JKook Wiki) 一文。

本节我们依照 `snw.kookbc.Main` 类(KookBC JAR 主类)的代码，讲解 KookBC 的工作流程。

KookBC 支持机器人以 WebSocket 或 Webhook 模式运行。

对于 WebSocket 模式，我们会 `new` 一个 `KBCClient` ，传入相应参数，然后调用其 `start` 方法。

对于 Webhook 模式，我们 `new` 一个 `WebhookClient` ，传入相应参数，然后调用其 `start` 方法。
* `WebhookClient` 位于 `snw.kookbc.impl.network.webhook` 包，是 `KBCClient` 的子类，主要重写了 `KBCClient#startNetwork` 方法，使其启动一个 HTTP Server ，而不是打开 WebSocket 连接。

若你想了解 KookBC 的 WebSocket 连接流程的实现，请看 `snw.kookbc.impl.network.Connector` 与 `snw.kookbc.impl.network.MessageProcessor` 类。

若你想了解 KookBC 对 Webhook 模式下收到的 HTTP 请求的处理流程，请见 `snw.kookbc.impl.network.webhook.SimpleHttpHandler` 类。

整个 KookBC 网络模块的工作流程主要为:
```text
WebSocket / Webhook 模块等待 KOOK 服务端发送事件内容
收到 KOOK 服务端发送的事件内容
(未禁用压缩时) 解压缩经过 Deflate 算法压缩的消息
(Webhook 模式下) 根据 KBCClient 存储的配置信息尝试解密消息内容
封装为 Frame
传递给 Listener 接口的实现进一步处理
```

### 事件系统

处理了这堆繁琐的网络操作之后，我们可以安心的专注处理已经经过封装的消息对象了。

`ListenerImpl` (或其子类) 将解析传入的 `Frame` 。

若是一个其他类型的 WebSocket 信令，按照信令的含义进行处理。

若是一个事件，则传递给 `EventFactory` 以得到具体的事件对象。

得到事件对象后，我们将检查其是否是一个用户发送消息的事件。
* "用户发送消息的事件" 指 `snw.jkook.event.channel.ChannelMessageEvent` 以及 `snw.jkook.event.pm.PrivateMessageReceivedEvent` 。

若是，则尝试将当前 KookBC 实例中已注册的命令的名称与用户发送的内容开头进行比对，若查询到对应命令，则执行，**并抛弃事件对象**。

若不是或未找到匹配的命令，通过 `snw.jkook.event.EventManager#callEvent` 方法的实现将事件对象发布。

这就是事件系统的工作流程。

---

至此，KookBC 的核心工作流程就讲完了。

希望本文对你有所帮助。
