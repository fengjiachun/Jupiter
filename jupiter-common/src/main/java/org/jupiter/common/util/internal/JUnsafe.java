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

package org.jupiter.common.util.internal;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * For the {@link sun.misc.Unsafe} access.
 *
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
public class JUnsafe {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JUnsafe.class);

    private static final Unsafe UNSAFE;

    static {
        Unsafe unsafe;
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
        } catch (Throwable t) {
            logger.warn("sun.misc.Unsafe.theUnsafe: unavailable, {}.", t);

            unsafe = null;
        }

        UNSAFE = unsafe;
    }

    public static Unsafe getUnsafe() {
        return UNSAFE;
    }

    /**
     * Raises an exception bypassing compiler checks for checked exceptions.
     */
    public static void throwException(Throwable t) {
        if (UNSAFE != null) {
            UNSAFE.throwException(t);
        } else {
            JUnsafe.<RuntimeException>throwException0(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException0(Throwable t) throws E {
        // 类型转换只是骗过前端javac编译器, 泛型只是个语法糖, 在javac编译后会将类型擦除,
        // 也就是说javac并不会生成checkcast指令, 所以在运行期不会抛出ClassCastException异常
        throw (E) t;
    }

    private JUnsafe() {}
}
