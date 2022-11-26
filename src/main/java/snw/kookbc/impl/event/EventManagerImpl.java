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

package snw.kookbc.impl.event;

import snw.jkook.event.*;
import snw.jkook.plugin.Plugin;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static snw.kookbc.util.Util.ensurePluginEnabled;

public class EventManagerImpl implements EventManager {
    private final KBCClient client;

    public EventManagerImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public void callEvent(Event event) {
        try {
            ((HandlerList) event.getClass().getMethod("getHandlers").invoke(null)).callAll(event);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            client.getCore().getLogger().error("Unable to call event.", e);
        }
    }

    @Override
    public void registerHandlers(Plugin plugin, Listener listener) {
        ensurePluginEnabled(plugin);
        registerHandlers0(plugin, listener);
    }

    public void registerHandlers0(Plugin plugin, Listener listener) {
        for (Method method : listener.getClass().getMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                Listener object = (Modifier.isStatic(method.getModifiers())) ? null : listener;
                try {
                    ((HandlerList) method.getParameterTypes()[0].getMethod("getHandlers").invoke(null)).add(plugin, method, object);
                } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                    throw new RuntimeException("Unable to register handler.", e);
                }
            }
        }
    }
}
