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
import snw.jkook.message.component.FileComponent;
import snw.jkook.message.component.card.CardBuilder;
import snw.jkook.message.component.card.Size;
import snw.jkook.message.component.card.Theme;
import snw.jkook.message.component.card.module.FileModule;

/**
 * @author huanmeng_qwq
 */
public class MultipleCardComponentTest extends BaseCardComponentTest {
    @Test
    public void fileModule() {
        assertComponent(new CardBuilder().setTheme(Theme.WARNING).setSize(Size.SM)
                        .addModule(new FileModule(FileComponent.Type.FILE, "https://img.kaiheila.cn/attachments/2021-01/21/600972b5d0d31.txt", "KOOK介绍.txt", null))
                        .build(),
                jsonArray(
                        simpleCardObject(Theme.WARNING, Size.SM, array -> array.add(jsonObject(fileJson -> {
                            fileJson.addProperty("type", "file");
                            fileJson.addProperty("src", "https://img.kaiheila.cn/attachments/2021-01/21/600972b5d0d31.txt");
                            fileJson.addProperty("title", "KOOK介绍.txt");
                        })))
                ));
    }

}
