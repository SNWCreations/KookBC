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

package snw.kookbc.impl.event;

import net.kyori.event.EventBus;
import net.kyori.event.PostResult;
import net.kyori.event.SimpleEventBus;
import net.kyori.event.method.MethodSubscriptionAdapter;
import net.kyori.event.method.SimpleMethodSubscriptionAdapter;
import snw.jkook.event.Event;
import snw.jkook.event.EventManager;
import snw.jkook.event.Listener;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.KBCClient;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static snw.kookbc.util.Util.ensurePluginEnabled;

public class EventManagerImpl implements EventManager {
    private final KBCClient client;
    private final EventBus<Event> bus;
    private final MethodSubscriptionAdapter<Listener> msa;
    private final Map<Plugin, List<Listener>> listeners = new ConcurrentHashMap<>();

    public EventManagerImpl(KBCClient client) {
        this.client = client;
        this.bus = new SimpleEventBus<>(Event.class);
        this.msa = new SimpleMethodSubscriptionAdapter<>(bus, EventExecutorFactoryImpl.INSTANCE, MethodScannerImpl.INSTANCE);
    }

    @Override
    public void callEvent(Event event) {
        final PostResult result = bus.post(event);
        if (!result.wasSuccessful()) {
            client.getCore().getLogger().error("Unexpected exception while posting event.");
            for (final Throwable t : result.exceptions().values()) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void registerHandlers(Plugin plugin, Listener listener) {
        ensurePluginEnabled(plugin);
        try {
            msa.register(listener);
        } catch (SimpleMethodSubscriptionAdapter.SubscriberGenerationException e) {
            msa.unregister(listener); // rollback
            throw e; // rethrow
        }
        getListeners(plugin).add(listener);
    }

    @Override
    public void unregisterAllHandlers(Plugin plugin) {
        if (!listeners.containsKey(plugin)) {
            return; // it is not necessary to waste a List.
        }
        getListeners(plugin).forEach(this::unregisterHandlers);
        listeners.remove(plugin);
    }

    @Override
    public void unregisterHandlers(Listener listener) {
        msa.unregister(listener);
    }

    private List<Listener> getListeners(Plugin plugin) {
        return listeners.computeIfAbsent(plugin, p -> new LinkedList<>());
    }

}
