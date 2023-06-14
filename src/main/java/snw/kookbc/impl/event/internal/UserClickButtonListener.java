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
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.util.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class UserClickButtonListener implements Listener {
    private static final PlainTextElement EMPTY_PLAIN_TEXT_ELEMENT = new PlainTextElement(" ");

    private final KBCClient client;

    public UserClickButtonListener(KBCClient client) {
        this.client = client;
    }

    @EventHandler
    public void event(UserClickButtonEvent event) {
        String value = event.getValue();
        if (!value.startsWith("kookbc-help")) {
            return;
        }
        JsonObject detail = JsonParser.parseString(value.substring(11)).getAsJsonObject();
        int page = detail.get("page").getAsInt();
        int currentPage = detail.get("current").getAsInt();
        if (page == currentPage) {
            return;
        }

        List<String> content = Util.listCommandsHelp(this.client);
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
                                                String.format("kookbc-help{\"page\": %d, \"current\": %d}", page - 1, page), // Placeholder
                                                page > 1 ? ButtonElement.EventType.RETURN_VAL : ButtonElement.EventType.NO_ACTION,
                                                new PlainTextElement("上一页")
                                        ),
                                        new ButtonElement(Theme.SECONDARY, "", EMPTY_PLAIN_TEXT_ELEMENT), // Placeholder
                                        new ButtonElement(Theme.SECONDARY, "", EMPTY_PLAIN_TEXT_ELEMENT), // Placeholder
                                        new ButtonElement(
                                                Theme.PRIMARY,
                                                String.format("kookbc-help{\"page\": %d, \"current\": %d}", page + 1, page),
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
        Message message = new TextChannelMessageImpl(this.client, event.getMessageId(), null, null, 0L, null, null);
        message.setComponent(finalComponent);
    }

}
