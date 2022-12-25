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

如果这个情况出现了，请去您的本地 Maven 仓库将 `com\github\SNWCreations\KookBC\<version>` (自行替换 `<version>` 为使用的版本) 文件夹删除，然后等待几分钟，再次刷新您的项目，应该就可以了。

如果 JitPack 构建出错，请联系我。

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

### KBCClient#registerPluginsCommand

此方法会被方法调用，用于向当前客户端实例注册 `/plugins` 命令的实现。

如果你需要禁用默认的 `/plugins` 实现，请用空方法体覆盖此方法。
