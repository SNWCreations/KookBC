# KookBC (Kook Bot Client)

这是一个 [JKook API](https://github.com/SNWCreations/JKook) 的标准 Java 客户端实现。

## 用法

从右侧 Releases 下载最新版本。

* **如果可能，请始终使用最新版本！**

下载后，请将下载的 KookBC 程序放在一个全新目录里。

然后用如下命令行启动 KookBC:

```text
java -jar kookbc-<version>.jar
```

其中，`<version>`是 KookBC 的版本。

之后，KookBC 会在当前目录下生成一个名为 kbc.yml 的 KookBC YAML 配置文件并退出。

配置内容详解请见 [KookBC YAML 配置详解](docs/KookBC_Config.md) 。请按照文档的解释，配置您的 KookBC 。

配置完成后，再次使用之前的命令行启动 KookBC ，当如下语句出现时，您的 KookBC 就已经准备就绪，可以使用。

```text
[XX:XX:XX] [Main Thread/INFO] Done! Type "help" for help.
```

其中，`X` 为任意可能的值，您可以忽视。

安装一个基于 JKook API 的 插件程序 (假设其基于最新版本的 JKook) ，只需要将其 .jar 文件放入 KookBC 目录下的 plugins 目录即可。

更多 KookBC 的命令行选项可以通过以下命令获得:

```text
java -jar kookbc-<version>.jar --help
```

其中，`<version>`是 KookBC 的版本。

更详细的命令行选项见 [KookBC 命令行](docs/KookBC_CommandLine.md) 。

我们在本仓库的 docs 文件夹下放了一些 KookBC 的相关文档，善用它们会帮助您！

如：\
[嵌入 KookBC 到您的项目](docs/Embedding_KookBC.md)\
[KookBC 与 Webhook](docs/KookBC_with_Webhook.md)\
[浅析 KookBC 的结构与工作流程](docs/The_Design_of_KookBC.md)


## 贡献

很感谢您愿意帮助我们改进 KookBC ！

您有两种贡献方法:
* 提出 Issue
* 提出 Pull Request

您可以在 Issues 提出您使用 KookBC (不包括其搭载的 Bot 程序) 的过程中遇到的问题，包括但不限于 KookBC 的程序错误，漏洞，或文档错误等。

如果您会 Java 编程 ，并且希望帮助我们改进 KookBC 的程序，请按照以下过程进行您的 Pull Request 过程:
* 创建一份此仓库的 Fork
* 从 dev 分支签出一个独立的分支，在其中做好您的修改
* 提交，并 Push 到您的 Fork
* 根据 Pull Request Template ，向我们发出 Pull Request
* 等待我们审核
* 若不同意，根据我们的修改意见做出改进，并请求继续
* 或通过，并被并入 dev 分支，若到这里，您的 Pull Request 过程就宣告结束

上述过程对文档的修改也适用，~~但请从 doc 分支签出~~。**我们不再使用 doc 分支，其已经于 2022/8/18 删除。**

但是，Pull Request 的提出遵循一个原则: **一个 Pull Request 只解决一个问题。**
如一个 Fork 同时解决了文档错误和程序错误，则应该分别提出 Pull Request。

## 技术信息

网络通信库: OkHttp 3

Webhook HTTP 服务器库: [JLHTTP](http://www.freeutils.net/source/jlhttp/)

JSON 处理库: Google GSON

控制台功能库: JLine 3, TerminalConsoleAppender

日志库: Apache Log4j2

此程序提供了对 [SpongePowered Mixin](https://github.com/SpongePowered/Mixin)([FabricMC Mixin](https://github.com/FabricMC/Mixin)) 的支持。

## 版权

Copyright (C) 2022 - 2024 KookBC contributors

在编写 Mixin 支持 的实现代码时引用了部分来自 Mixin 项目的源代码，以及 [LegacyLauncher](https://github.com/Mojang/LegacyLauncher) 项目的源代码，在此一并表示感谢。

对于下文，提供如下定义以便于讨论:
* "此仓库"即 此 [GitHub 仓库](https://github.com/SNWCreations/KookBC) 及其本地副本，此仓库的 Fork 及其本地副本不等同于此仓库。
* "此项目"即 此仓库中存放的 KookBC 源代码及其附属内容。
* "源代码"即 人类直接可读的 Java 程序文本文件。
* "原始"版本即 使用此仓库中的源代码编译得到的 KookBC 副本。

若你以任何形式使用此项目，则表明你同意此节的内容以及 AGPLv3 许可证的条款。

若你在下文提及的特定情形下使用此项目，则表明你在同意上文内容的同时同意 LGPLv3 许可证的条款。

若此节的内容与 AGPLv3 许可证的条款或 LGPLv3 许可证的条款冲突，以此节内容为准。

若此节的内容在未来有更新，更新的内容适用于此项目的任何版本，同时旧的内容失效。

此仓库的创建者 [SNWCreations](https://github.com/SNWCreations) 享有对此节的内容的最终解释权。

若你只是直接修改了此项目，按照 AGPLv3 许可证的条款以及 LGPLv3 许可证的条款处理。
* 对此项目的源代码进行直接修改的方式可以认为是"直接修改"。
* 其他修改方式均认为是"间接修改"。
* 比如在你自己的项目中，通过使用 `extends` 关键字对此项目的内容修改得到的 Java 类可以认为是对此项目的间接修改。

对于仅将此项目作为依赖项的，可以在适用 AGPLv3 许可证的同时应用 LGPLv3 许可证的条款，即意味着:

若你的项目只是依赖此项目，并且你依赖的此项目是"原始"版本，你依赖此项目的项目可以不公开源代码并按其他许可证授权。

若你的项目只是依赖此项目，但你依赖的此项目是你基于此仓库直接修改的版本，
你需要依据 AGPLv3 许可证以及 LGPLv3 许可证的条款公开修改过的此项目的源代码，
然后你依赖此项目的项目可以不公开源代码并按其他许可证授权。
