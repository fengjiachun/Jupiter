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

import org.jupiter.common.util.internal.UnsafeUtil;

import java.nio.ByteBuffer;

/**
 * Base implementation of {@link Constant}.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
public abstract class AbstractConstant<T extends AbstractConstant<T>> implements Constant<T> {

    private final int id;
    private final String name;
    private volatile long uniqueKey;

    public AbstractConstant(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    @SuppressWarnings("NullableProblems")
    public int compareTo(T o) {
        if (this == o) {
            return 0;
        }

        int returnCode = hashCode() - o.hashCode();
        if (returnCode != 0) {
            return returnCode;
        }

        @SuppressWarnings("UnnecessaryLocalVariable") AbstractConstant<T> other = o;
        long thisUK = uniqueKey();
        long otherUK = other.uniqueKey();
        if (thisUK < otherUK) {
            return -1;
        }
        if (thisUK > otherUK) {
            return 1;
        }

        throw new Error("failed to compare two different constants");
    }

    @Override
    public final boolean equals(Object obj) { // make it final, cannot be override
        return super.equals(obj);
    }

    @Override
    public final int hashCode() { // make it final, cannot be override
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Constant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }

    /**
     * Ensure that returns a unique key value
     */
    private long uniqueKey() {
        long uniqueKey;
        if ((uniqueKey = this.uniqueKey) == 0) {
            synchronized (this) {
                while ((uniqueKey = this.uniqueKey) == 0) {
                    ByteBuffer directBuffer = ByteBuffer.allocateDirect(1);
                    this.uniqueKey = UnsafeUtil.directBufferAddress(directBuffer); // direct buf的内存地址是唯一的
                }
            }
        }
        return uniqueKey;
    }
}
