# KookBC 配置详解

此文档为您介绍 KookBC 配置文件 (kbc.yml) 中的配置项及其作用。

一份未经修改的 kbc.yml 模板如下:
```yaml
token: ""

mode: "websocket"

## ---- START WEBHOOK CONFIGURATION ----

webhook-port: 8080

webhook-encrypt-key: ""

webhook-verify-token: ""

webhook-route: "kookbc-webhook"

## ---- END WEBHOOK CONFIGURATION ----

botmarket-uuid: ""

compress: true

ignore-remote-call-invisible-internal-command: true

save-console-history: true

ignore-sn-order: false

allow-help-ad: true
```

其中的注释已经移除。

**注意: 以 "webhook" 开头的配置项不会在这里讲解，请见 [KookBC 与 Webhook](KookBC_with_Webhook.md) 。**

---

## token

表示 Bot 程序将使用的 Bot Token 。

可以从 [Kook 开发者中心](https://developer.kookapp.cn) 获取。_前提是您拥有开发者资格。_

此配置项允许一个字符串。

**警告: 值不会加密保存！请妥善保管您的 Token ！因 Token 泄露导致的一切损失，KookBC 作者不会负责。**

如果提供的 Token 无效，KookBC 以及 Bot 将无法正常工作。

因 Token 属于隐私内容，故本配置项没有示例。

## mode

决定 KookBC 与 Kook 交互的模式。

只有 "websocket" 和 "webhook" 是有效值，其他值会导致报错并退出。

示例:

```yaml
mode: "websocket"
```

## botmarket-uuid

此配置项用于声明您的 Bot 在 [BotMarket (一个 Kook Bot 发布平台)](https://www.botmarket.cn) 的 UUID 。

若此配置项不为空，则 KookBC 将会创建一个计划任务，自动完成 BotMarket 的 PING 操作。

此配置项没有合适的示例。

## compress

决定从 Kook 的 WebSocket 服务器获得的消息是否将被压缩。

这是 KookBC 的内部功能，不会对 Bot 产生影响。

此配置项允许一个布尔值。

推荐保持为 true ，因为可以节省带宽。

示例:

```yaml
compress: true
```

## ignore-remote-call-invisible-internal-command

决定是否在 Kook 用户尝试执行内部命令时忽略。

此配置项允许一个布尔值。

如果为 `false` ，则尝试执行内部命令的用户将收到一条消息。

示例:

```yaml
ignore-remote-call-invisible-internal-command: true
```

## save-console-history

决定 KookBC 是否会保留下每次后台执行的命令内容以及执行时间。

保存的内容位于 KookBC 文件夹下的 ".console_history" 文件。

如果为 false ，该文件将不会生成，也不会向已有的文件追加内容。

示例:

```yaml
save-console-history: true
```

## ignore-sn-order

若打开此配置项，KookBC 将忽略事件的顺序，并尽可能保证不处理重复事件。

此配置项推荐在您的机器人所服务的用户量大的时候开启，若后台经常跳 wrong sn 警告，打开这个吧。

示例:
```yaml
ignore-sn-order: true
```

## _allow-help-ad_

决定是否在用户所看到的命令帮助列表 (通过 `/help` 命令获取) 的结尾增加 KookBC 的仓库地址。

此配置项允许一个布尔值。

希望您保持为 `true` ，这是对我们创作的支持！

示例:

```yaml
allow-help-ad: true
```
