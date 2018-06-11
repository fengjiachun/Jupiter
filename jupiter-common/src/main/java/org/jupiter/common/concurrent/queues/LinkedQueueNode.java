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
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.common.concurrent.queues;

import org.jupiter.common.util.internal.UnsafeUtil;
import sun.misc.Unsafe;

/**
 * Forked from <a href="https://github.com/JCTools/JCTools">JCTools</a>.
 */
final class LinkedQueueNode<E> {

    private static final long NEXT_OFFSET = UnsafeUtil.objectFieldOffset(LinkedQueueNode.class, "next");

    private static final Unsafe unsafe = UnsafeUtil.getUnsafe();

    private E value;
    @SuppressWarnings("unused")
    private volatile LinkedQueueNode<E> next;

    LinkedQueueNode() {
        this(null);
    }

    LinkedQueueNode(E val) {
        spValue(val);
    }

    /**
     * Gets the current value and nulls out the reference to it from this node.
     *
     * @return value
     */
    public E getAndNullValue() {
        E temp = lpValue();
        spValue(null);
        return temp;
    }

    public E lpValue() {
        return value;
    }

    public void spValue(E newValue) {
        value = newValue;
    }

    public void soNext(LinkedQueueNode<E> n) {
        unsafe.putOrderedObject(this, NEXT_OFFSET, n);
    }

    public LinkedQueueNode<E> lvNext() {
        return next;
    }
}
