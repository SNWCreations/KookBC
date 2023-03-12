# 嵌入 KookBC 到您的项目

**警告: 此文档不保证始终有效，因为有时我可能会重构 KookBC，导致文档提到的类、方法等元素不可用，如果出现这种情况，请联系我。**

此文档旨在教您如何将 KookBC 嵌入您的项目，以便于与 Kook 交互。

我们假设您的项目使用 Maven 作为构建工具。

## 准备

* 一个待嵌入 KookBC 的项目
* 对 Maven 中央仓库，以及 JitPack 连接通畅的网络

## 执行

### 准备 Maven 依赖

首先，请在您的 pom.xml 里添加 JitPack 作为仓库。_如果您之前已经添加过，请跳过此步。_

然后，在您的 pom.xml 里添加 KookBC 作为依赖项。

格式如下:

```xml
<dependency>
    <groupId>com.github.SNWCreations</groupId>
    <artifactId>KookBC</artifactId>
    <version>0.37.0</version>
    <scope>compile</scope>
</dependency>
```

* **注意: 本文可能不会随着 KookBC 的更新而修改 `version` 项，请自行将其替换为您需要的版本。**

之后，刷新您的项目，等待 KookBC 从 JitPack 下载。

失败了？没关系，那很正常，对于一个从未在 JitPack 编译过的工件，第一次请求总是失败的。

如果这个情况出现了，请去您的本地 Maven 仓库将 `com\github\SNWCreations\KookBC\<version>`
(自行替换 `<version>` 为使用的版本) 文件夹删除，然后等待几分钟，再次刷新您的项目，应该就可以了。

如果 JitPack 构建出错，请联系我。

### 常见误区

我们遇见过使用 `snw.kookbc.LaunchMain` 或 `snw.kookbc.Main` 类启动 KookBC 的情况，但是那是**不推荐**的。

你可以参考 `snw.kookbc.Main` 类的代码，以了解 KookBC 的正常启动流程。

* 请不要尝试阅读 `snw.kookbc.LaunchMain` 类以了解 KookBC 的启动流程！那是为了配合 Mixin 支持而编写的。那其中并没有启动的逻辑。

请直接使用 `KBCClient` 类。

### 了解&构造 KookBC 的关键类

有两个类是很重要的：

* `snw.kookbc.impl.CoreImpl` (实现了 `snw.jkook.Core`)
* `snw.kookbc.impl.KBCClient` (表示一个 KookBC 实例)

首先，你需要构造一个 `CoreImpl` 的实例。

它有两个构造方法:

* `CoreImpl()`
* `Coreimpl(org.slf4j.Logger)`

一般情况下，我们推荐前者，因为前者默认使用 `org.slf4j.helpers.NOPLogger` 作为 Logger 。`NOPLogger` 使日志不会输出。

后者也是可以用的，您只是需要传入一个 `org.slf4j.Logger` 的实例。

构造之后请**务必**用这个实例调用 `snw.jkook.JKook#setCore(Core)` 方法，以保证需要通过 JKook Class 访问 `Core` 的方法能正常运行。

现在，您有了一个 `CoreImpl` 的实例。

但这只是第一步。真正重要的是 `KBCClient` 。

让我们看 `KBCClient` 的构造方法。

```java
public class KBCClient {
    public KBCClient(
            snw.kookbc.impl.CoreImpl core,
            snw.jkook.config.ConfigurationSection config,
            java.io.File pluginsFolder,
            String token
    ) {
        // Actual logic here
    }
}
```

由此可见，您需要一个 `CoreImpl` 实例，一个 `ConfigurationSection` 实例，和一个 `File` 实例。

解释一下这四者的作用:

* `core`: 用于 KookBC 实例的操作。在此项目中，通过调用 `JKook.getCore()` (高版本还有 `Plugin#getCore`) 得到的就是这个。
* `config`: 即此客户端的配置数据。一般是 `kbc.yml` 加载后的结果。(`kbc.yml` 使用 `YamlConfiguration` 类的方法加载)
* `pluginsFolder`: 即存放插件的 **文件夹**，可以为 `null`，为 `null` 时，KookBC 将不会尝试从任何地方加载插件。
* `token`: Bot Token

按照这个方法，您应该得到了一个 `KBCClient` 的实例。

### 启动 KookBC

现在就是启动了。

请看 `KBCClient#start` 方法。

```java
public class KBCClient {
    public void start() {
        // Actual logic here 
    }
}
```

没有需要的参数，直接调用吧。

**请注意，此方法可能会抛出 `RuntimeException` ，请注意捕获并处理。**

此方法返回后，意味着您的 Kook Bot 已经启动，可以开始用 JKook API 进行操作了。

### 关闭 KookBC

只需要调用 `KBCClient#shutdown` 方法即可。

**但请注意，此方法可能会抛出 `RuntimeException` ，请注意捕获并处理。**

### 使用需要 Plugin 实例作为参数的 JKook API 方法

首先，在嵌入 KookBC 的环境中，请不要直接通过 `new` 关键字使用 `snw.jkook.plugin.BasePlugin` 及其子类。
* **为什么？** `BasePlugin` 在构造时会检查加载其类的类加载器是否是 `snw.jkook.plugin.MarkedClassLoader` 的子类 (在过去的 API 版本中比较的是 `snw.jkook.plugin.PluginLoader`)，如果不是，会抛出异常。

怎么办？

你可以参考 `snw.kookbc.impl.plugin.InternalPlugin` ，编写一个始终被启用的，除了能返回一个 `PluginDescription` 以外什么都不能做的 `Plugin` 。

然后直接 `new` 这个占位符插件，用来调用需要 `Plugin` 实例的方法。

这个方法是最推荐的。

## 再看 KBCClient

此节介绍 `KBCClient` 类内的部分方法。

### KBCClient#loop

此方法会启动控制台并一直循环监听用户输入。

除非 KookBC 实例终止，或此方法执行过程中出现错误，此方法永远不会返回。

### KBCClient#loadAllPlugins

此方法会从 pluginsFolder 加载插件。

您可以重写此方法，只需要把您所加载的插件通过 `plugins.add(Plugin)` 添加就可以了。

### KBCClient#getCore

此方法获取绑定到此客户端实例的 `CoreImpl` 实例。

### KBCClient#startNetwork

此方法会启动网络模块。

您可以重写此方法，如果您对于 Kook 连接有自己的处理方法。

如果要覆盖此方法，请同时覆盖 `KBCClient#shutdownNetwork` 方法，以便于您可以关闭您自己的网络相关模块。

### KBCClient#registerInternal

此方法会向当前客户端实例注册一些内部命令，如 `/help`，`/plugins`，`/stop`，以及注册一些可以比由插件注册的事件监听器更优先运行的事件监听器。

### KBCClient#registerHelpCommand

此方法会被 `KBCClient#registerInternal` 方法调用，用于向当前客户端注册 `/help` 命令的实现。

如果你需要禁用默认的 `/help` 的实现，请用空方法体覆盖此方法。

### KBCClient#registerStopCommand

此方法会被 `KBCClient#registerInternal` 方法调用，用于向当前客户端注册 `/stop` 命令的实现。

如果你需要禁用默认的 `/stop` 的实现，请用空方法体覆盖此方法。

### KBCClient#registerPluginsCommand

此方法会被方法调用，用于向当前客户端实例注册 `/plugins` 命令的实现。

如果你需要禁用默认的 `/plugins` 实现，请用空方法体覆盖此方法。

## 将 KookBC 嵌入 Bukkit 插件？

哦，这是一个有趣的想法！

但是在你这样做之前，让我们先告诉你一些有关这个的坑...

你是 "Minecraft Development" IDEA 插件的用户吗？

如果是，请注意！由这个插件生成的 Maven Bukkit 项目 (包括但不限于 Bukkit, Spigot) 所使用的 Maven Shade 插件是 3.2.4 。

而这个版本在 Shade KookBC 时会出现 "类版本不受支持" (Unsupported class version) 的问题，应该先升级插件。

经过测试，3.3.0 版本是正常的。

另外，在 Shade KookBC 时，你需要 Relocate 两个包:

* Google GSON (com.google.gson)
* SnakeYAML (org.yaml.snakeyaml)

为什么？

这两个包是很容易与 Bukkit 服务端冲突的，因为 Bukkit 项目也在使用它们。

怎么做？

像下面这样配置你的 Maven Shade 插件:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <id>shade</id>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>com.google.gson</pattern>
                        <shadedPattern>your.package.here.com.google.gson</shadedPattern>
                    </relocation>
                    <relocation>
                        <pattern>org.yaml.snakeyaml</pattern>
                        <shadedPattern>your.package.here.org.yaml.snakeyaml</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

其他的配置项按你自己的需求配置即可，但 `relocations` 标签以及指定的两个包的 `relocation` 必不可少。

`shadedPattern` 的内容随意，并不一定要按我们提供的写，这只是模板，但内容不可以与原本的 `pattern` 相同。
* 诸如 `snw.myproject.include.com.google.gson` 的字符串作为 `shadedPattern` 是可以的。
