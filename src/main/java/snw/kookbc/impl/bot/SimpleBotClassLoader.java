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

package snw.kookbc.impl.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import snw.jkook.HttpAPI;
import snw.jkook.bot.Bot;
import snw.jkook.bot.BotClassLoader;
import snw.jkook.bot.BotDescription;
import snw.kookbc.impl.HttpAPIImpl;
import snw.kookbc.impl.KBCClient;

import java.io.File;
import java.lang.reflect.Constructor;

public class SimpleBotClassLoader extends BotClassLoader {
    private final KBCClient client;

    public SimpleBotClassLoader(KBCClient client) {
        this.client = client;
    }

    protected <T extends Bot> T construct(final Class<T> cls, final BotDescription description, final String token) throws Exception {
        Constructor<T> constructor = cls.getDeclaredConstructor(
                String.class,
                File.class,
                File.class,
                HttpAPI.class,
                BotDescription.class,
                File.class,
                Logger.class
        );
        boolean prev = constructor.isAccessible(); // if other reflect operation turn this to true?
        constructor.setAccessible(true);
        HttpAPIImpl httpApi = new HttpAPIImpl(KBCClient.getInstance());
        T bot = constructor.newInstance(
                token,
                new File(client.getBotDataFolder(), "config.yml"),
                client.getBotDataFolder(),
                httpApi,
                description,
                new File(cls.getProtectionDomain().getCodeSource().getLocation().toURI()),
                new BotLogger(description.getName(), LoggerFactory.getLogger(cls))
        );
        httpApi.init(bot); // ensure the calls that need bot instance can be executed correctly
        constructor.setAccessible(prev);
        return bot;
    }
}
