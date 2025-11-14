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

package snw.kookbc.impl.network.ws;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Frame;
import snw.kookbc.impl.network.ListenerFactory;
import snw.kookbc.interfaces.network.FrameHandler;
import snw.kookbc.util.JacksonUtil;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.util.zip.DataFormatException;

import static snw.kookbc.util.Util.decompressDeflate;

public class WebSocketMessageProcessor extends WebSocketListener {
    private final KBCClient client;
    private final Connector connector;
    private final FrameHandler listener;

    @SuppressWarnings("deprecation")
    public WebSocketMessageProcessor(KBCClient client, Connector connector) {
        this.client = client;
        this.connector = connector;
        this.listener = ListenerFactory.getListener(client, connector)::executeEvent;
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
        Thread.currentThread().setName("Network Thread");
    }

    // for non-compressed messages
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        super.onMessage(webSocket, text);
        try {
            JsonNode object = JacksonUtil.parse(text);
            JsonNode sNode = object.get("s");
            JsonNode snNode = object.get("sn");
            JsonNode dNode = object.get("d");

            int s = sNode.asInt();
            int sn = snNode != null ? snNode.asInt() : -1;

            Frame frame = new Frame(s, sn, dNode);
            listener.executeEvent(frame);
        } catch (Exception e) {
            client.getCore().getLogger().error("处理 WebSocket 消息时发生异常: {}, 原始消息: {}", e.getMessage(), text, e);
            // 不触发重连，因为可能只是单个消息格式错误
        }
    }

    // for compressed messages, so we will extract it before processing
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
        String res = null;
        try {
            res = new String(decompressDeflate(bytes.toByteArray()));
            JsonNode object = JacksonUtil.parse(res);
            JsonNode sNode = object.get("s");
            JsonNode snNode = object.get("sn");
            JsonNode dNode = object.get("d");

            int s = sNode.asInt();
            int sn = snNode != null ? snNode.asInt() : -1;

            Frame frame = new Frame(s, sn, dNode);
            listener.executeEvent(frame);
        } catch (DataFormatException | IOException e) {
            client.getCore().getLogger().error("解压缩 WebSocket 数据失败: {}, 数据长度: {} 字节", e.getMessage(), bytes.size(), e);
            // 不触发重连，因为可能只是单个消息损坏
        } catch (Exception e) {
            if (res != null) {
                client.getCore().getLogger().error("处理压缩 WebSocket 消息时发生异常: {}, 原始消息: {}", e.getMessage(), res, e);
            } else {
                client.getCore().getLogger().error("处理压缩 WebSocket 消息时发生异常: {}", e.getMessage(), e);
            }
            // 不触发重连，因为可能只是单个消息格式错误
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);

        // 分类记录异常信息
        String exceptionType = t.getClass().getSimpleName();
        String message = t.getMessage();

        if (t instanceof ProtocolException) {
            // 协议异常，通常是正常的连接关闭
            client.getCore().getLogger().debug("WebSocket 协议异常（可能是正常关闭）: {}", message);
        } else if (t instanceof UnknownHostException) {
            // DNS 解析失败
            client.getCore().getLogger().error("DNS 解析失败，无法连接到 Kook 服务器: {}", message);
            client.getCore().getLogger().error("请检查：1) 网络连接是否正常 2) DNS 服务器配置 3) 域名 kookapp.cn 是否可访问");
        } else if (message != null && (message.contains("timeout") || message.contains("Timeout"))) {
            // 连接超时
            client.getCore().getLogger().error("连接超时: {}", message);
            client.getCore().getLogger().error("请检查网络连接是否稳定");
        } else if (exceptionType.contains("IOException") || exceptionType.contains("SocketException")) {
            // I/O 或 Socket 异常
            client.getCore().getLogger().error("网络 I/O 异常 ({}): {}", exceptionType, message);
            client.getCore().getLogger().error("这可能是由于网络不稳定或防火墙配置导致");
        } else {
            // 其他未知异常
            client.getCore().getLogger().error("WebSocket 连接发生异常 ({})", exceptionType);
            client.getCore().getLogger().error("响应信息: {}", response);
            client.getCore().getLogger().error("异常堆栈:", t);
        }

        // 关闭 WebSocket 连接
        try {
            webSocket.close(1000, "Connection Failed - Reconnecting");
        } catch (Exception e) {
            client.getCore().getLogger().debug("关闭 WebSocket 时发生异常（可以忽略）: {}", e.getMessage());
        }

        // 请求重连，并将异常传递给重连策略
        client.getCore().getLogger().info("将启动智能重连流程...");
        connector.requestReconnect();
    }

}
