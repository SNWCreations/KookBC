/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.network;

import static snw.kookbc.util.JacksonUtil.get;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import snw.kookbc.util.JacksonUtil;

import snw.jkook.command.CommandException;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.event.Event;
import snw.jkook.event.channel.ChannelMessageEvent;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.exceptions.BadResponseException;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.TextComponent;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.WrappedCommand;
import snw.kookbc.impl.network.ws.Connector;
import snw.kookbc.interfaces.network.FrameHandler;
import snw.kookbc.interfaces.network.webhook.WebhookNetworkSystem;

public class ListenerImpl implements FrameHandler {
    protected final KBCClient client;
    protected final Connector connector;
    protected final Object lck = new Object();

    public ListenerImpl(KBCClient client, Connector connector) {
        this.client = client;
        this.connector = connector;
    }

    @Override
    public void handle(Frame frame) {
        if (!(frame.getType() == MessageType.PONG)) { // I hate PONG logging messages
            client.getCore().getLogger().debug("收到载荷帧: {}", frame);
        }
        if (frame.getType() == null) {
            client.getCore().getLogger().warn("未知的事件类型！");
            return;
        }
        switch (frame.getType()) {
            case EVENT:
                client.getEventExecutor().execute(() -> event(frame));
                break;
            case HELLO:
                hello(frame);
                break;
            case PING:
                client.getCore().getLogger().debug("收到不可能的远程消息: 类型为 PING");
                break;
            case PONG:
                client.getCore().getLogger().trace("收到 PONG");
                connector.pong();
                break;
            case RESUME:
                client.getCore().getLogger().debug("收到不可能的远程消息: 类型为 RESUME");
                break;
            case RECONNECT:
                client.getCore().getLogger().warn("收到远程重连请求，正在尝试重连");
                connector.requestReconnect();
                break;
            case RESUME_ACK:
                client.getCore().getLogger().info("恢复完成");
                JsonNode sessionIdNode = frame.getData().get("session_id");
                if (sessionIdNode != null) {
                    client.getSession().setId(sessionIdNode.asText());
                }
                break;
        }
    }

    protected void event(Frame frame) {
        synchronized (lck) {
            client.getCore().getLogger().debug("收到 EVENT");
            Session session = client.getSession();
            AtomicInteger sn = session.getSN();
            int expected = Session.UPDATE_FUNC.applyAsInt(sn.get());
            int actual = frame.getSN();

            if (actual > expected) {
                client.getCore().getLogger().warn("意外的错误 SN，期望 {}，实际收到 {}", expected, actual);
                session.getBuffer().add(frame);
            } else if (expected == actual) {
                event0(frame);
                session.increaseSN();
                saveSN();

                // 处理缓冲区中的连续帧
                int continueId = sn.get() + 1;
                Frame bufFrame;
                while ((bufFrame = find(continueId)) != null) {
                    event0(bufFrame);
                    session.increaseSN();
                    saveSN();
                    continueId++;
                    client.getCore().getLogger().debug("已处理缓冲消息，SN: {}", bufFrame.getSN());
                }
            } else if (client.getConfig().getBoolean("allow-warn-old-message")) {
                client.getCore().getLogger().warn("收到来自远程的意外旧消息，已丢弃");
            }
        }
    }

    private Frame find(int sn) {
        for (Frame frame : client.getSession().getBuffer()) {
            if (frame.getSN() == sn) {
                client.getSession().getBuffer().remove(frame);
                return frame;
            }
        }
        return null;
    }

    protected void event0(Frame frame) {
        Event event;
        try {
            // 直接使用 Jackson JsonNode 进行事件创建
            JsonNode jacksonData = frame.getData();
            event = client.getEventFactory().createEvent(jacksonData);
        } catch (Exception e) {
            client.getCore().getLogger().error("无法从载荷创建事件");
            client.getCore().getLogger().error("事件载荷: {}", frame);
            e.printStackTrace();
            return;
        }
        if (event == null) {
            return;
        }
        if (!executeCommand(event)) {
            client.getCore().getEventManager().callEvent(event);
        }
    }

    protected void saveSN() {
        if (client.getNetworkSystem() instanceof WebhookNetworkSystem) {
            File snfile = new File(client.getPluginsFolder(), "sn");
            try {
                if (!snfile.exists()) {
                    // noinspection ResultOfMethodCallIgnored
                    snfile.createNewFile();
                }
                FileWriter writer = new FileWriter(snfile, false);
                writer.write(String.valueOf(client.getSession().getSN().get()));
                writer.close();
            } catch (IOException e) {
                client.getCore().getLogger().warn("无法将 SN 写入本地", e);
            }
        }
    }

    protected void hello(Frame frame) {
        client.getCore().getLogger().debug("收到 HELLO");
        connector.setConnected(true);
        JsonNode object = frame.getData();
        JsonNode codeNode = object.get("code");
        int status = codeNode != null ? codeNode.asInt() : -1;
        if (status == 0) {
            JsonNode sessionIdNode = object.get("session_id");
            if (sessionIdNode != null) {
                client.getSession().setId(sessionIdNode.asText());
            }
        } else {
            connector.requestReconnect();
        }
    }

    // return true if the component is a command and executed (whether success or failed).
    protected boolean executeCommand(Event event) {
        if (!(event instanceof ChannelMessageEvent || event instanceof PrivateMessageReceivedEvent))
            return false; // not a message-related event

        // region extract data
        User sender;
        Message msg;
        NonCategoryChannel channel = null;
        BaseComponent baseComponent; // raw
        TextComponent component = null;
        if (event instanceof ChannelMessageEvent) {
            msg = ((ChannelMessageEvent) event).getMessage();
            channel = ((ChannelMessageEvent) event).getChannel();
        } else {
            msg = ((PrivateMessageReceivedEvent) event).getMessage();
        }
        sender = msg.getSender();
        baseComponent = msg.getComponent();
        if (baseComponent instanceof TextComponent) {
            component = (TextComponent) baseComponent;
        }
        // endregion

        // condition check
        if (component == null) return false; // not a text component!
        if (sender == client.getCore().getUser()) return false; // prevent self call

        // prepare data
        String cmdLine = component.toString();
        CommandManagerImpl cmdMan = (CommandManagerImpl) client.getCore().getCommandManager();

        // execute command
        try {
            return cmdMan.executeCommand(sender, cmdLine.trim(), msg);
        } catch (Exception e) {
            if (client.getConfig().getBoolean("allow-error-feedback", true)) {
                // load plugin data
                WrappedCommand wrappedCommand = cmdMan.getCommandMap()
                        .getView(true)
                        .get(cmdLine.contains(" ") ? cmdLine.substring(0, cmdLine.indexOf(" ")) : cmdLine);
                if (wrappedCommand == null) {
                    return true;
                }
                PluginDescription description = wrappedCommand.getPlugin().getDescription();
                String pluginName = description.getName();
                String pluginVer = description.getVersion();
                String pluginWebsite = description.getWebsite();

                // write exception data
                StringWriter strWrt = new StringWriter();
                // remove CommandException stacktrace to make the stacktrace smaller
                (e instanceof CommandException ? e.getCause() : e).printStackTrace(new PrintWriter(strWrt));
                String content =
                        "执行命令时发生异常，请联系 Bot 的所有者，插件的开发者和 " + SharedConstants.IMPL_NAME + " 的开发者！\n" +
                                "命令来自于插件: " + pluginName + " (版本: " + pluginVer + ")"
                                + (pluginWebsite.isEmpty() ? "" : "\n另外，我们发现这个插件有网站，链接在[这](" + pluginWebsite + ")。")
                                + "\n" +
                                "以下是堆栈信息 (可以提供给开发者，有助于其诊断问题):\n" +
                                "---\n" +
                                strWrt;

                // send
                try {
                    if (event instanceof ChannelMessageEvent) {
                        channel.sendComponent(
                                content,
                                null,
                                sender
                        );
                    } else {
                        sender.sendPrivateMessage(content);
                    }
                } catch (BadResponseException ex) { // too long? or timed out? however, we won't retry.
                    client.getCore().getLogger().error("无法发送命令失败消息", ex);
                }
            }
            client.getCore().getLogger().error("执行来自远程的命令时发生意外异常", e);
            return true; // Although this failed, but it is a valid command
        }
    }
}
