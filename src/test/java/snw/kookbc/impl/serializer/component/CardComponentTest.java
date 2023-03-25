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

package snw.kookbc.impl.serializer.component;

import org.junit.jupiter.api.Test;
import snw.jkook.message.component.card.CardComponent;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.element.MarkdownElement;
import snw.jkook.message.component.card.module.ContextModule;

import java.util.Arrays;

/**
 * @author huanmeng_qwq
 */
public class CardComponentTest extends BaseCardComponentTest {
    @Test
    void testSimpleCard() {
        assertComponent(new CardComponent(Arrays.asList(
                new ContextModule(Arrays.asList(
                        new MarkdownElement("Hello, world!")
                ))
        ), Size.LG, Theme.PRIMARY), simpleCardObject(Theme.PRIMARY, Size.LG, modules -> {
            modules.add(simpleContextModuleObject(elements -> {
                elements.add(markdownElementObject("Hello, world!"));
            }));
        }));
    }
}
