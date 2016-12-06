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

/**
 * 这里利用了对象继承另一个对象时的内存布局规则, 比如下面这个例子
 * ----------------------------------
 *  class A {
 *      byte a;
 *  }
 *
 *  class B {
 *      byte b;
 *  }
 *
 *  [HEADER:  8 bytes]  8
 *  [a:       1 byte ]  9
 *  [padding: 3 bytes] 12
 *  [b:       1 byte ] 13
 *  [padding: 3 bytes] 16
 * ----------------------------------
 * 首先java中所有对象在内存中都按照8字节对齐, 不足8字节用padding补齐,
 * 其次父类A和子类B的内存布局是连续的, 并且有严格界限不会被打乱分配在内存里,
 * 这样子类B的对象头再加上15个long padding占用大于等于128个字节的空间,
 * 这128个字节填充在子类B的前边, 保证不会有其他对象从左边与子类B形成false sharing,
 * 要保证后边不会有false sharing的话需要再来一个子类C.
 *
 * 这个例子并不完全准确, 对象头并不一定是8个字节, 实际上是下面这个样子:
 * ---------------------------------------
 *  For 32 bit JVM:
 *      _mark   : 4 byte constant
 *      _klass  : 4 byte pointer to class
 *  For 64 bit JVM:
 *      _mark   : 8 byte constant
 *      _klass  : 8 byte pointer to class
 *  For 64 bit JVM with compressed-oops:
 *      _mark   : 8 byte constant
 *      _klass  : 4 byte pointer to class
 * ---------------------------------------
 *
 * jupiter
 * org.jupiter.common.util.internal
 *
 * @author jiachun.fjc
 */
class InternalThreadLocalMapL0Pad {
    @SuppressWarnings("unused")
    public long p01, p02, p03, p04, p05, p06, p07, p08, p09, p10, p11, p12, p13, p14, p15;
}
