### 从Cloud迁移到LiteCommands

#### 兼容层

如果你不想迁移，可以安装 [kook-cloud-compatible](https://github.com/huanmeng-qwq/kookbc-cloud-compatible/releases)
插件来允许使用cloud框架注册命令的插件正常运行

### 注解

| Cloud                  | LiteCommands          |
|------------------------|-----------------------|
| @CommandContainer      | @RootCommand          |
| @CommandMethod("test") | @Command(name="test") |
| @Argument              | @Arg                  |

### 示例

#### Cloud

```java

@CommandContainer
@CommandPrefix("!")
@CommandMethod("test")
public class TestCloudCommand {
    @CommandMethod("sub1 <parm1>")
    public void sub1(CommandSender sender, Message message, @Argument("parm1") String parm1) {
        // ...
    }
}
```

#### LiteCommands

> Github: [LiteCommands](https://github.com/Rollczi/LiteCommands)
>
> IDEA中安装[LiteCommands](https://plugins.jetbrains.com/plugin/20799-litecommands)插件后，可以自动识别LiteCommands的注解，并能够很好的辅助开发。

```java
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import snw.kookbc.impl.command.litecommands.annotations.prefix.Prefix;

@Command(name = "test")
@Prefix("!")
public class TestLiteCommand {
    @Execute(name = "sub1")
    public void sub1(@Sender CommandSender sender,
                     @Context Message message,
                     @Arg("parm1") String parm1) {
        // ...
    }
}
```