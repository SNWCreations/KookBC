package snw.kookbc.impl.event.internal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import snw.jkook.event.EventHandler;
import snw.jkook.event.Listener;
import snw.jkook.event.user.UserClickButtonEvent;
import snw.jkook.message.Message;
import snw.jkook.message.component.card.CardBuilder;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.*;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.cloud.CloudCommandManagerImpl;
import snw.kookbc.impl.message.MessageImpl;
import snw.kookbc.impl.message.PrivateMessageImpl;
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.util.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static snw.kookbc.impl.command.internal.HelpCommand.HELP_VALUE_HEADER;

public final class UserClickButtonListener implements Listener {
    private static final PlainTextElement EMPTY_PLAIN_TEXT_ELEMENT = new PlainTextElement(" ");

    private final KBCClient client;

    public UserClickButtonListener(KBCClient client) {
        this.client = client;
    }

    @EventHandler
    public void event(UserClickButtonEvent event) {
        String value = event.getValue();
        if (!value.startsWith(HELP_VALUE_HEADER)) {
            return;
        }
        JsonObject detail = JsonParser.parseString(value.substring(HELP_VALUE_HEADER.length())).getAsJsonObject();
        int page = detail.get("page").getAsInt();
        int currentPage = detail.get("current").getAsInt();
        String messageType = detail.get("messageType").getAsString();
        boolean force = detail.has("force") && detail.get("force").getAsBoolean();
        if (page == currentPage) {
            return;
        }

        List<String> content;
        if (client.getCore().getCommandManager() instanceof CloudCommandManagerImpl) {
            content = Util.listCloudCommandsHelp(this.client, force);
        } else {
            content = Util.listCommandsHelp(this.client);
        }
        MultipleCardComponent finalComponent;
        if (content.isEmpty()) {
            finalComponent = new CardBuilder()
                    .setTheme(Theme.DANGER)
                    .setSize(Size.LG)
                    .addModule(new HeaderModule("找不到命令"))
                    .build();
        } else {
            int totalPages = content.size() % 5 == 0 ? content.size() / 5 : content.size() / 5 + 1;
            CardBuilder builder = new CardBuilder()
                    .setTheme(Theme.SUCCESS)
                    .setSize(Size.LG)
                    .addModule(new HeaderModule(String.format("命令帮助 (%d/%d)", page, totalPages)))
                    .addModule(DividerModule.INSTANCE);
            content.removeIf(IT -> IT.startsWith("(/)stop:"));
            if (content.size() <= 5) {
                content.stream()
                        .map(SectionModule::new)
                        .forEachOrdered(builder::addModule);
            } else {
                content.stream()
                        .skip(5L * (page - 1))
                        .limit(5L)
                        .map(SectionModule::new)
                        .forEachOrdered(builder::addModule);
                builder.addModule(DividerModule.INSTANCE)
                        .addModule(new ActionGroupModule(
                                Arrays.asList(
                                        new ButtonElement(
                                                Theme.PRIMARY,
                                                String.format(HELP_VALUE_HEADER + "{\"page\": %d, \"current\": %d, \"messageType\": %s}", page - 1, page, messageType), // Placeholder
                                                page > 1 ? ButtonElement.EventType.RETURN_VAL : ButtonElement.EventType.NO_ACTION,
                                                new PlainTextElement("上一页")
                                        ),
                                        new ButtonElement(Theme.SECONDARY, "", EMPTY_PLAIN_TEXT_ELEMENT), // Placeholder
                                        new ButtonElement(Theme.SECONDARY, "", EMPTY_PLAIN_TEXT_ELEMENT), // Placeholder
                                        new ButtonElement(
                                                Theme.PRIMARY,
                                                String.format(HELP_VALUE_HEADER + "{\"page\": %d, \"current\": %d, \"messageType\": %s}", page + 1, page, messageType),
                                                (5 * page) < content.size() ? ButtonElement.EventType.RETURN_VAL : ButtonElement.EventType.NO_ACTION,
                                                new PlainTextElement("下一页")
                                        )
                                )

                        ));
            }
            if (client.getConfig().getBoolean("allow-help-ad", true)) {
                builder.addModule(DividerModule.INSTANCE)
                        .addModule(new ContextModule(
                                Collections.singletonList(
                                        new MarkdownElement(
                                                String.format(
                                                        "由 [%s](%s) v%s 驱动 - %s API %s",
                                                        SharedConstants.IMPL_NAME,
                                                        SharedConstants.REPO_URL,
                                                        SharedConstants.IMPL_VERSION,
                                                        SharedConstants.SPEC_NAME,
                                                        client.getCore().getAPIVersion()
                                                )
                                        )
                                )
                        ));
            }
            finalComponent = builder.build();
        }

        if (messageType.equals("PM")){
            Message message = this.client.getCore().getUnsafe().getPrivateMessage(event.getMessageId());;
            message.setComponent(finalComponent);
        }else if (messageType.equals("CM")){
            Message message = this.client.getCore().getUnsafe().getTextChannelMessage(event.getMessageId());
            message.setComponent(finalComponent);
        }

    }

}
