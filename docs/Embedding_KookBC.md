# 嵌入 KookBC 到您的项目

**警告: 此文档不保证始终有效，因为有时我可能会重构 KookBC，导致文档提到的类、方法等元素不可用，如果出现这种情况，请联系我。**

此文档旨在教您如何将 KookBC 嵌入您的项目，以便于与 Kook 交互。

我们假设您的项目使用 Maven 作为构建工具。

## 准备

* 一个待嵌入 KookBC 的项目
* 对 JitPack 连接通畅的网络

## 执行

### 准备 Maven 依赖

首先，请在您的 pom.xml 里添加 JitPack 作为仓库。_如果您之前已经添加过，请跳过此步。_

然后，在您的 pom.xml 里添加 KookBC 作为依赖项。

格式如下:

```xml
<dependency>
    <groupId>com.github.SNWCreations</groupId>
    <artifactId>KookBC</artifactId>
    <version>0.2.0</version>
    <scope>compile</scope>
</dependency>
```

**注意: 本文不会随着 KookBC 的更新而修改 `version` 项，请自行将其替换为最新版本。**

之后，刷新您的项目，等待 KookBC 从 JitPack 下载。

失败了？没关系，那很正常，对于一个从未在 JitPack 编译过的工件，第一次请求总是失败的。 

如果这个情况出现了，请去 JitPack 网站手动启动构建，并在完成后去您的本地 Maven 仓库将 `com\github\SNWCreations\KookBC\<version>` (自行替换 `<version>` 为使用的版本) 文件夹删除，然后再次刷新您的项目，应该就可以了。

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
            snw.jkook.Core core,
            snw.jkook.config.file.YamlConfiguration config,
            java.io.File botDataFolder
    ) {
        // Actual logic here
    }
}
```

由此可见，您需要一个 `Core` 实例，一个 `YamlConfiguration` 实例，和一个 `File` 实例。

我需要解释一下这三者的作用:

* `core`: `Core` 的实例，用于 KookBC 实例的操作。
* `config`: 即加载了 `kbc.yml` 后得到的结果
* `botDataFolder`: 即用于存放 Bot 数据的**文件夹**

按照这个方法，您应该得到了一个 `KBCClient` 的实例。

在那之后，请用这个实例调用 `KBCClient#setInstance` 方法，使需要 `KBCClient` 实例的方法能正常运行。

### 启动 KookBC

现在就是启动了。

请看 `KBCClient#start` 方法。

```java
public class KBCClient {
    public void start(File file, String token) {
        // Actual logic here 
    }
}
```

由此可见，您需要提供两个参数，我来解释一下吧。

* `file`: 即 Bot JAR 文件，KookBC 将把此文件视作 Bot 程序并尝试加载。
* `token`: 即 Bot Token ，用于使 Bot 与 Kook API 进行交互。

**但请注意，此方法可能会抛出 `RuntimeException` ，请注意捕获并处理。**

此方法返回后，意味着您的 Kook Bot 已经启动，可以开始用 JKook API 进行操作了。

### 关闭 KookBC

只需要调用 `KBCClient#shutdown` 方法即可。

**但请注意，此方法可能会抛出 `RuntimeException` ，请注意捕获并处理。**

## 额外内容

此节介绍额外的 `KBCClient` 内的方法。

## KBCClient#loop

此方法会启动控制台并一直循环监听用户输入。

除非 KookBC 实例终止，或此方法执行过程中出现错误，此方法永远不会返回。
