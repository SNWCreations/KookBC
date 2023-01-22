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

import net.kyori.event.method.EventExecutor;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.event.Event;
import snw.jkook.event.Listener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class EventExecutorFactoryImpl implements EventExecutor.Factory<Event, Listener> {
    public static final EventExecutorFactoryImpl INSTANCE = new EventExecutorFactoryImpl();

    private EventExecutorFactoryImpl() {
    }

    @Override
    public @NonNull EventExecutor<Event, Listener> create(@NonNull Object object, @NonNull Method method) throws Exception {
        method.setAccessible(true);
        final Class<? extends Event> actualEventType = method.getParameterTypes()[0].asSubclass(Event.class);
        if (Modifier.isAbstract(actualEventType.getModifiers())) {
            throw new IllegalArgumentException("You cannot create listener for an abstract event type.");
        }
        final MethodHandle handle = MethodHandles.lookup().unreflect(method).bindTo(object);
        return (listener, event) -> {
            if (!actualEventType.isInstance(event))
                return;
            handle.invoke(event);
        };
    }

}
