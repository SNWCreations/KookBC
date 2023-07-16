/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package snw.kookbc.impl.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import static snw.kookbc.LaunchMain.blackboard;

@SuppressWarnings("unchecked")
public class Blackboard implements IGlobalPropertyService {
    public Blackboard() {
        if (blackboard == null) {
            throw new RuntimeException("Blackboard is null!");
        }
    }

    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    public final <T> T getProperty(IPropertyKey key) {
        return (T) blackboard.get(key.toString());
    }

    public final void setProperty(IPropertyKey key, Object value) {
        blackboard.put(key.toString(), value);
    }

    public final <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T) blackboard.getOrDefault(key.toString(), defaultValue);
    }

    public final String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = blackboard.get(key.toString());
        return value != null ? value.toString() : defaultValue;
    }

    static class Key implements IPropertyKey {
        private final String key;

        Key(String key) {
            this.key = key;
        }

        public String toString() {
            return this.key;
        }
    }
}
