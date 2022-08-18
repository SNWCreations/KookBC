# KookBC 命令行

本文档提供了 KookBC 命令行的基本介绍与进阶使用。

## 基础使用

```text
java -jar kookbc-<version>.jar
```

其中 `<version>` 是 KookBC 的版本。

只要你的 kbc.yml 配置写入了正确的 Token ，KookBC 即可正常工作。

### 参数

`--token <token>`: 指定 KookBC 将要使用的 Token ，一旦指定，无论 kbc.yml 的 Token 是否正确，都会使用。(除非只指定选项而未提供值)

`--help`: 获取英文帮助并退出。

### JVM 参数

此节的 JVM 参数由 KookBC 提供，不保证所有 JKook 实现均拥有。

`-Dlog4j2.log.level`: 指定 Log4j2 的控制台日志级别，`latest.log` 和 `debug.log` 不受此项影响。
* 例如 `-Dlog4j2.log.level=DEBUG` 使控制台可以显示 DEBUG 级别日志。
