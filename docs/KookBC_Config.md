# KookBC 配置详解

此文档为您介绍 KookBC 配置文件 (kbc.yml) 中的配置项及其作用。

一份未经修改的 kbc.yml 模板如下:
```yaml
bot-file: ""

token: ""

compress: true

ignore-remote-call-invisible-internal-command: true

save-console-history: true

allow-help-ad: true
```

其中的注释已经移除。

---

## bot-file

表示 Bot JAR 的路径 (绝对路径 或 相对路径均允许)。

此配置项允许一个字符串。

KookBC 会将此配置项指定的文件看做 Bot 程序文件，并尝试加载，若失败，程序会报错并退出。

示例:

```yaml
bot-file: "D:\KookBCDevelopment\example\example.jar"
```

或

```yaml
bot-file: "example.jar"
```

## token

表示 Bot 程序将使用的 Bot Token 。

可以从 [Kook 开发者中心](https://developer.kookapp.cn) 获取。_前提是您拥有开发者资格。_

此配置项允许一个字符串。

**警告: 值不会加密保存！请妥善保管您的 Token ！因 Token 泄露导致的一切损失，KookBC 作者不会负责。**

如果提供的 Token 无效，KookBC 以及 Bot 将无法正常工作。

因 Token 属于隐私内容，故本配置项没有示例。

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

## _allow-help-ad_

决定是否在用户所看到的命令帮助列表 (通过 `/help` 命令获取) 的结尾增加 KookBC 的仓库地址。

此配置项允许一个布尔值。

希望您保持为 `true` ，这是对我们创作的支持！

示例:

```yaml
allow-help-ad: true
```
