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
package org.jupiter.flight.exec;

import org.jupiter.common.util.Bytes;

/**
 * 代码来自周志明的 [深入理解Java虚拟机] 一书, 第九章第三小节[自己动手实现远程执行功能], 稍有改动.
 *
 * jupiter
 * org.jupiter.flight.exec
 *
 * @author jiachun.fjc
 */
public class ClassModifier {

    // Class文件中常量池的起始偏移
    private static final int CONSTANT_POOL_COUNT_INDEX = 8;

    // CONSTANT_Utf8_info常量的tag标志
    private static final int CONSTANT_Utf8_info = 1;

    // 常量池中14种常量所占的长度, CONSTANT_Utf8_info型常量除外, 因为它不是定长的
    private static final int[] CONSTANT_ITEM_LENGTH = {
            -1,         // 占位用的
            -1,         // tag 1:   CONSTANT_Utf8_info
            -1,         // 占位用的
            5,          // tag 3:   CONSTANT_Integer_info
            5,          // tag 4:   CONSTANT_Float_info
            9,          // tag 5:   CONSTANT_Long_info
            9,          // tag 6:   CONSTANT_Double_info
            3,          // tag 7:   CONSTANT_Class_info
            3,          // tag 8:   CONSTANT_String_info
            5,          // tag 9:   CONSTANT_Fieldref_info
            5,          // tag 10:  CONSTANT_Methodref_info
            5,          // tag 11:  CONSTANT_InterfaceMethodref_info
            5,          // tag 12:  CONSTANT_NameAndType_info
            -1,         // 占位用的
            -1,         // 占位用的
            4,          // tag 15:  CONSTANT_MethodHandle_info
            3,          // tag 16:  CONSTANT_MethodType_info
            -1,         // 占位用的
            5           // tag 18:  CONSTANT_MethodDynamic_info
    };

    private static final int u1 = 1;
    private static final int u2 = 2;

    private byte[] classBytes;

    public ClassModifier(byte[] classBytes) {
        this.classBytes = classBytes;
    }

    /**
     * 修改常量池中CONSTANT_Utf8_info常量的内容
     */
    public byte[] modifyUTF8Constant(String originalString, String replaceString) {
        int offset = CONSTANT_POOL_COUNT_INDEX;

        // 获取常量池中常量的数量
        int cpCount = Bytes.bytes2Int(classBytes, CONSTANT_POOL_COUNT_INDEX, u2);
        offset += u2;
        for (int i = 0; i < cpCount; i++) {
            int tag = Bytes.bytes2Int(classBytes, offset, u1);
            if (tag == CONSTANT_Utf8_info) {
                offset += u1;

                /*
                 * CONSTANT_Utf8_info数据类型的结构:
                 *
                 * 项目     类型     描述
                 * tag      u1      值为1
                 * length   u2      UTF-8编码的字符串占用的字节数
                 * bytes            长度为length的UTF-8编码格式的字符串
                 */
                int length = Bytes.bytes2Int(classBytes, offset, u2);
                offset += u2;

                String str = Bytes.bytes2String(classBytes, offset, length);
                if (str.equalsIgnoreCase(originalString)) {
                    byte[] strBytes = Bytes.string2Bytes(replaceString);
                    byte[] strLen = Bytes.int2Bytes(replaceString.length(), u2);

                    classBytes = Bytes.replace(classBytes, offset - u2, u2, strLen); // 替换为新字符串的 "长度"
                    classBytes = Bytes.replace(classBytes, offset, length, strBytes); // 替换为新字符串的 "内容"

                    return classBytes;
                } else {
                    offset += length;
                }
            } else {
                offset += CONSTANT_ITEM_LENGTH[tag];
            }
        }
        return classBytes;
    }
}
