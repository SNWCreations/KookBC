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
package snw.kookbc.util;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.MultipleCardComponent;
import snw.jkook.message.component.card.element.ButtonElement;
import snw.jkook.message.component.card.element.ImageElement;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.element.PlainTextElement;
import snw.jkook.message.component.card.module.*;
import snw.jkook.message.component.card.structure.Paragraph;
import snw.jkook.util.Validate;
import snw.kookbc.impl.serializer.component.CardComponentSerializer;
import snw.kookbc.impl.serializer.component.MultipleCardComponentSerializer;
import snw.kookbc.impl.serializer.component.element.ButtonElementSerializer;
import snw.kookbc.impl.serializer.component.element.ImageElementSerializer;
import snw.kookbc.impl.serializer.component.element.MarkdownElementSerializer;
import snw.kookbc.impl.serializer.component.element.PlainTextElementSerializer;
import snw.kookbc.impl.serializer.component.module.*;
import snw.kookbc.impl.serializer.component.structure.ParagraphSerializer;

public class GsonUtil {
    public static final Gson CARD_GSON = new GsonBuilder()
            // Card
            .registerTypeAdapter(CardComponent.class, new CardComponentSerializer())
            .registerTypeAdapter(MultipleCardComponent.class, new MultipleCardComponentSerializer())

            // Element
            .registerTypeAdapter(ButtonElement.class, new ButtonElementSerializer())
            .registerTypeAdapter(ImageElement.class, new ImageElementSerializer())
            .registerTypeAdapter(MarkdownElement.class, new MarkdownElementSerializer())
            .registerTypeAdapter(PlainTextElement.class, new PlainTextElementSerializer())

            //Structure
            .registerTypeAdapter(Paragraph.class, new ParagraphSerializer())

            // Module
            .registerTypeAdapter(ActionGroupModule.class, new ActionGroupModuleSerializer())
            .registerTypeAdapter(ContainerModule.class, new ContainerModuleSerializer())
            .registerTypeAdapter(ContextModule.class, new ContextModuleSerializer())
            .registerTypeAdapter(CountdownModule.class, new CountdownModuleSerializer())
            .registerTypeAdapter(DividerModule.class, new DividerModuleSerializer())
            .registerTypeAdapter(FileModule.class, new FileModuleSerializer())
            .registerTypeAdapter(HeaderModule.class, new HeaderModuleSerializer())
            .registerTypeAdapter(ImageGroupModule.class, new ImageGroupModuleSerializer())
            .registerTypeAdapter(InviteModule.class, new InviteModuleSerializer())
            .registerTypeAdapter(SectionModule.class, new SectionModuleSerializer())

            .disableHtmlEscaping()
            .create();

    public static final Gson NORMAL_GSON = new Gson();

    public static Type createListType(Class<?> elementType) {
        Validate.notNull(elementType);
        return TypeToken.getParameterized(List.class, elementType).getType();
    }

    private GsonUtil() {
    }
}
