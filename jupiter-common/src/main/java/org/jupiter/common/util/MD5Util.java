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
package org.jupiter.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5转换工具
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class MD5Util {

    private static final ThreadLocal<MessageDigest> messageDigestHolder = new ThreadLocal<>();

    // 用来将字节转换成 16 进制表示的字符
    @SuppressWarnings("CStyleArrayDeclaration")
    private static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    static {
        try {
            MessageDigest message = java.security.MessageDigest.getInstance("MD5");
            messageDigestHolder.set(message);
        } catch (NoSuchAlgorithmException e) {
            ThrowUtil.throwException(e);
        }
    }

    public static String getMD5(String data) {
        try {
            MessageDigest message = messageDigestHolder.get();
            if (message == null) {
                message = java.security.MessageDigest.getInstance("MD5");
                messageDigestHolder.set(message);
            }
            message.update(data.getBytes(JConstants.UTF8));
            byte[] b = message.digest();

            StringBuilder digestHex = new StringBuilder(32);
            for (int i = 0; i < 16; i++) {
                digestHex.append(byteHEX(b[i]));
            }

            return digestHex.toString();
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        }
        return "";
    }

    private static String byteHEX(byte ib) {
        char[] ob = new char[2];
        ob[0] = hexDigits[(ib >>> 4) & 0X0F];
        ob[1] = hexDigits[ib & 0X0F];
        return new String(ob);
    }

    private MD5Util() {}
}
