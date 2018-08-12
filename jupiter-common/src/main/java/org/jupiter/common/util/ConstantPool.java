/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.jupiter.common.util;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 * A pool of {@link Constant}s.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public abstract class ConstantPool<T extends Constant<T>> {

    private final ConcurrentMap<String, T> constants = Maps.newConcurrentMap();

    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */
    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        checkNotNull(firstNameComponent, "firstNameComponent");
        checkNotNull(secondNameComponent, "secondNameComponent");

        return valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
    }

    /**
     * Returns the {@link Constant} which is assigned to the specified {@code name}.
     * If there's no such {@link Constant}, a new one will be created and returned.
     * Once created, the subsequent calls with the same {@code name} will always return the previously created one
     * (i.e. singleton.)
     *
     * @param name the name of the {@link Constant}
     */
    public T valueOf(String name) {
        checkArgument(!Strings.isNullOrEmpty(name), "empty name");
        return getOrCreate(name);
    }

    /**
     * Get existing constant by name or creates new one if not exists. Threadsafe
     *
     * @param name the name of the {@link Constant}
     */
    private T getOrCreate(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T newConstant = newConstant(nextId.getAndIncrement(), name);
            constant = constants.putIfAbsent(name, newConstant);
            if (constant == null) {
                constant = newConstant;
            }
        }
        return constant;
    }

    /**
     * Returns {@code true} if exists for the given {@code name}.
     */
    public boolean exists(String name) {
        checkArgument(!Strings.isNullOrEmpty(name), "empty name");
        return constants.containsKey(name);
    }

    /**
     * Creates a new {@link Constant} for the given {@code name} or fail with an
     * {@link IllegalArgumentException} if a {@link Constant} for the given {@code name} exists.
     */
    public T newInstance(String name) {
        checkArgument(!Strings.isNullOrEmpty(name), "empty name");
        return createOrThrow(name);
    }

    /**
     * Creates constant by name or throws exception. Threadsafe
     *
     * @param name the name of the {@link Constant}
     */
    private T createOrThrow(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T newConstant = newConstant(nextId.getAndIncrement(), name);
            constant = constants.putIfAbsent(name, newConstant);
            if (constant == null) {
                return newConstant;
            }
        }

        throw new IllegalArgumentException(String.format("'%s' is already in use", name));
    }

    protected abstract T newConstant(int id, String name);
}
