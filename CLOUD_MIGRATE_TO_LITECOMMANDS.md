### 从Cloud迁移到LiteCommands

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
    public void sub1(@Context CommandSender sender,
                     @Context Message message,
                     @Arg("parm1") String parm1) {
        // ...
    }
}
```