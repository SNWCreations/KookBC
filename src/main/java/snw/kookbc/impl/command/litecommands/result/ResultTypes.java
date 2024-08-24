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

package snw.kookbc.impl.command.litecommands.result;

import dev.rollczi.litecommands.invocation.Invocation;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public enum ResultTypes implements ResultType {
    DEFAULT(null) {
        @Override
        public void message(Invocation<CommandSender> invocation, Message message, Object result) {
            Optional<ResultType> resultType = invocation.context().get(ResultType.class);
            ResultType type = resultType.orElse(REPLY);
            if (type instanceof ExecuteResultType) {
                type = ((ExecuteResultType) type).getResultType();
            }
            if (type == DEFAULT) { // why...?
                type = REPLY;
            }
            type.message(invocation, message, result);
        }
    },
    REPLY("reply"),
    REPLY_TEMP("reply_temp"),
    SEND("send"),
    SEND_TEMP("send_temp");
    private final String type;

    ResultTypes(String type) {
        this.type = type;
    }

    public static <T> void execute(Message message, T result, BiConsumer<Message, T> method) {
        method.accept(message, result);
    }

    private static final Map<Class<?>, Map<String, BiConsumer<Message, Object>>> methods = new HashMap<>();

    static {
        methods.put(String.class, new HashMap<>());
        methods.put(BaseComponent.class, new HashMap<>());

        methods.get(String.class).put("reply", (message, object) -> message.reply(object.toString()));
        methods.get(String.class).put("reply_temp", (message, object) -> {
            String msg = object.toString();
            if (message instanceof ChannelMessage) {
                ((ChannelMessage) message).replyTemp(msg);
            } else {
                message.reply(msg);
            }
        });
        methods.get(String.class).put("send", (message, object) -> message.sendToSource(object.toString()));
        methods.get(String.class).put("send_temp", (message, object) -> {
            String msg = object.toString();
            if (message instanceof ChannelMessage) {
                ((ChannelMessage) message).sendToSourceTemp(msg);
            } else {
                message.sendToSource(msg);
            }
        });

        methods.get(BaseComponent.class).put("reply", (message, object) -> message.reply(((BaseComponent) object)));
        methods.get(BaseComponent.class).put("reply_temp", (message, object) -> {
            BaseComponent msg = (BaseComponent) object;
            if (message instanceof ChannelMessage) {
                ((ChannelMessage) message).replyTemp(msg);
            } else {
                message.reply(msg);
            }
        });
        methods.get(BaseComponent.class).put("send", (message, object) -> message.sendToSource(((BaseComponent) object)));
        methods.get(BaseComponent.class).put("send_temp", (message, object) -> {
            BaseComponent msg = (BaseComponent) object;
            if (message instanceof ChannelMessage) {
                ((ChannelMessage) message).sendToSourceTemp(msg);
            } else {
                message.sendToSource(msg);
            }
        });
    }


    @Override
    public void message(Invocation<CommandSender> invocation, Message message, Object result) {
        CommandSender sender = invocation.sender();
        if (sender instanceof ConsoleCommandSender) {
            ((ConsoleCommandSender) sender).getLogger().info("The execution result of command {}: {}", invocation.label(), result);
        } else if (sender instanceof User) {
            Class<?> clazz = null;
            if (result instanceof BaseComponent) {
                clazz = BaseComponent.class;
            } else {
                clazz = result.getClass();
            }
            if (!methods.containsKey(clazz)) {
                clazz = String.class;
                result = result.toString();
            }
            execute(message, result, methods.get(clazz).get(type));
        } else {
            throw new IllegalStateException("Unknown sender type: " + sender.getClass().getName());
        }
    }
}
