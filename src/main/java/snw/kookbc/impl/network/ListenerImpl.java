/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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

import com.google.gson.JsonObject;
import snw.jkook.command.CommandException;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.event.Event;
import snw.jkook.event.channel.ChannelMessageEvent;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TextComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.event.EventFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ListenerImpl implements Listener {
    protected final KBCClient client;
    private final Object lck = new Object();

    public ListenerImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public void executeEvent(Frame frame) {
        if (frame.getType() == null) {
            client.getCore().getLogger().warn("Unknown event type! The raw frame content: {}", frame);
            return;
        }
        switch (frame.getType()) {
            case EVENT:
                client.getCore().getScheduler().runTask(() -> event(frame));
                break;
            case HELLO:
                hello(frame);
                break;
            case PING:
                client.getCore().getLogger().debug("Impossible Message from remote: type is PING.");
                break;
            case PONG:
                client.getCore().getLogger().debug("Got PONG");
                client.getConnector().pong();
                break;
            case RESUME:
                client.getCore().getLogger().debug("Impossible Message from remote: type is RESUME.");
                break;
            case RECONNECT:
                client.getCore().getLogger().warn("Got RECONNECT request from remote. Attempting to reconnect.");
                client.getConnector().requestReconnect();
                break;
            case RESUME_ACK:
                client.getCore().getLogger().info("Resume finished");
                client.getSession().setId(frame.getData().get("session_id").getAsString());
                break;
        }
    }

    protected void event(Frame frame) {
        synchronized (lck) {
            client.getCore().getLogger().debug("Got EVENT");
            Session session = client.getSession();
            AtomicInteger sn = session.getSN();
            Set<Frame> buffer = session.getBuffer();
            int expected = sn.get() == 65535 ? 1 : sn.get() + 1;
            int actual = frame.getSN();
            if (actual > expected) {
                client.getCore().getLogger().warn("Unexpected wrong SN, expected {}, got {}", expected, actual);
                client.getCore().getLogger().warn("We will process it later.");
                buffer.add(frame);
            } else if (expected == actual) {
                event0(frame);
                sn.getAndAdd(1);
                if (!buffer.isEmpty()) {
                    int continueId = sn.get() + 1;
                    do {
                        boolean found = false;
                        Iterator<Frame> bufferIterator = buffer.iterator();
                        while (bufferIterator.hasNext()) {
                            Frame bufFrame = bufferIterator.next();
                            if (bufFrame.getSN() == continueId) {
                                found = true;       // we found the frame matching the continueId,
                                // so we will continue after the frame got processed
                                event0(bufFrame);
                                sn.set(continueId); // make sure the SN will update!
                                continueId++;
                                bufferIterator.remove(); // we won't need this frame, because it has processed
                                client.getCore().getLogger().debug("Processed message in buffer with SN {}", bufFrame.getSN());
                                break;
                            }
                        }
                        if (!found) {
                            break;
                        }
                    } while (true);
                }
            } else {
                client.getCore().getLogger().warn("Unexpected old message from remote. Dropped it.");
            }
        }
    }

    protected void event0(Frame frame) {
        Event event = EventFactory.getEvent(frame.getData());
        if (!executeCommand(event)) {
            client.getCore().getEventManager().callEvent(event);
        }
    }

    protected void hello(Frame frame) {
        client.getCore().getLogger().debug("Got HELLO");
        client.getConnector().setConnected(true);
        JsonObject object = frame.getData();
        int status = object.get("code").getAsInt();
        if (status == 0) {
            KBCClient.getInstance().getSession().setId(object.get("session_id").getAsString());
        } else {
            switch (status) {
                case 40101:
                    throw new RuntimeException("Invalid Bot Token!");
                case 40103:
                    client.getCore().getLogger().debug("WebSocket Token is invalid. Attempting to reconnect.");
                    client.getConnector().requestReconnect();
                    break;
                default:
                    throw new RuntimeException("Unexpected response code: " + status);
            }
        }
    }

    // return true if the component is a command and executed (whether success or failed).
    protected boolean executeCommand(Event event) {
        if (!(event instanceof ChannelMessageEvent || event instanceof PrivateMessageReceivedEvent))
            return false; // not a message-related event
        User sender;
        Message msg;
        TextChannel channel = null;
        TextComponent component = null;
        if (event instanceof ChannelMessageEvent) {
            msg = ((ChannelMessageEvent) event).getMessage();
            BaseComponent baseComponent = msg.getComponent();
            channel = ((ChannelMessageEvent) event).getChannel();
            sender = msg.getSender();
            if (baseComponent instanceof TextComponent) {
                component = (TextComponent) baseComponent;
            }
        } else {
            msg = ((PrivateMessageReceivedEvent) event).getMessage();
            BaseComponent baseComponent = msg.getComponent();
            sender = ((PrivateMessageReceivedEvent) event).getUser();
            if (baseComponent instanceof TextComponent) {
                component = (TextComponent) baseComponent;
            }
        }
        if (component == null) return false; // not a text component!
        try {
            return ((CommandManagerImpl) client.getCore().getCommandManager()).executeCommand0(sender, component.toString(), msg);
        } catch (Exception e) {
            StringWriter strWrt = new StringWriter();
            MarkdownComponent markdownComponent;
            // remove CommandException stacktrace to make the stacktrace smaller
            (e instanceof CommandException ? e.getCause() : e).printStackTrace(new PrintWriter(strWrt));
            markdownComponent = new MarkdownComponent(
                    "执行命令时发生异常，请联系 Bot 的开发者和 KookBC 的开发者！\n" +
                            "以下是堆栈信息 (可以提供给开发者，有助于其诊断问题):\n" +
                            "---\n" +
                            strWrt
            );
            if (event instanceof ChannelMessageEvent) {
                channel.sendComponent(
                        markdownComponent,
                        null,
                        sender
                );
            } else {
                ((PrivateMessageReceivedEvent) event).getUser().sendPrivateMessage(markdownComponent);
            }
            client.getCore().getLogger().error("Unexpected exception while we attempting to execute command from remote.", e);
            return true; // Although this failed, but it is a valid command
        }
    }
}
