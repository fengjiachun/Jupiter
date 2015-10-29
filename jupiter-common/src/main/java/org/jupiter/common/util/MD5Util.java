package org.jupiter.common.util;

import org.jupiter.common.util.internal.UnsafeAccess;

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

    private static ThreadLocal<MessageDigest> messageDigestHolder = new ThreadLocal<>();

    // 用来将字节转换成 16 进制表示的字符
    private static final char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    static {
        try {
            MessageDigest message = java.security.MessageDigest.getInstance("MD5");
            messageDigestHolder.set(message);
        } catch (NoSuchAlgorithmException e) {
            UnsafeAccess.UNSAFE.throwException(e);
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
            UnsafeAccess.UNSAFE.throwException(e);
        }
        return "";
    }

    private static String byteHEX(byte ib) {
        char[] ob = new char[2];
        ob[0] = hexDigits[(ib >>> 4) & 0X0F];
        ob[1] = hexDigits[ib & 0X0F];
        return new String(ob);
    }
}
