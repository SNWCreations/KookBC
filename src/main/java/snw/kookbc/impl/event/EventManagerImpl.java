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
import snw.kookbc.impl.KBCClient;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.kyori.event.EventBus;
import net.kyori.event.PostResult;
import net.kyori.event.SimpleEventBus;
import net.kyori.event.PostResult.CompositeException;
import net.kyori.event.method.MethodSubscriptionAdapter;
import net.kyori.event.method.SimpleMethodSubscriptionAdapter;

import static snw.kookbc.util.Util.ensurePluginEnabled;

public class EventManagerImpl implements EventManager {
    private final KBCClient client;
    private final EventBus<Event> bus;
    private final MethodSubscriptionAdapter<Listener> msa;
    private final Map<Plugin, List<Listener>> listeners = new HashMap<>();

    public EventManagerImpl(KBCClient client) {
        this.client = client;
        this.bus = new SimpleEventBus<>(Event.class);
        this.msa = new SimpleMethodSubscriptionAdapter<>(bus, EventExecutorFactoryImpl.INSTANCE, MethodScannerImpl.INSTANCE);
    }

    @Override
    public void callEvent(Event event) {
        PostResult result = bus.post(event);
        try {
            result.raise();
        } catch (CompositeException e) {
            client.getCore().getLogger().error("Unexpected exception while posting event.");
            for (final Throwable t : e.result().exceptions().values()) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void registerHandlers(Plugin plugin, Listener listener) {
        ensurePluginEnabled(plugin);
        registerHandlers0(plugin, listener);
    }

    public void registerHandlers0(Plugin plugin, Listener listener) {
        msa.register(listener);
        if (plugin != null) {
            getListeners(plugin).add(listener);
        }
    }

    public void unregisterHandlers(Plugin plugin) {
        getListeners(plugin).forEach(this::unregisterHandler);
    }

    public void unregisterHandler(Listener listener) {
        msa.unregister(listener);
    }

    private List<Listener> getListeners(Plugin plugin) {
        return listeners.computeIfAbsent(plugin, p -> new LinkedList<>());
    }

}
