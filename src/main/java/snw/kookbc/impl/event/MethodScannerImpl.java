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

import net.kyori.event.PostOrders;
import net.kyori.event.method.MethodScanner;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.event.EventHandler;
import snw.jkook.event.Listener;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MethodScannerImpl implements MethodScanner<Listener> {
    public static final MethodScannerImpl INSTANCE = new MethodScannerImpl();

    private MethodScannerImpl() {
    }

    @Override
    public boolean shouldRegister(@NonNull Listener listener, @NonNull Method method) {
        return Modifier.isPublic(method.getModifiers()) && method.isAnnotationPresent(EventHandler.class);
    }

    @Override
    public int postOrder(@NonNull Listener listener, @NonNull Method method) {
        return method.getAnnotation(EventHandler.class).internal() ? PostOrders.EARLY : PostOrders.NORMAL;
    }

    @Override
    public boolean consumeCancelledEvents(@NonNull Listener listener, @NonNull Method method) {
        return false;
    }

}
