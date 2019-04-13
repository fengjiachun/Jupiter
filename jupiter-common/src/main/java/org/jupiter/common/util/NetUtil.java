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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class NetUtil {

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final String LOCAL_IP_ADDRESS;

    static {
        InetAddress localAddress;
        try {
            localAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            localAddress = null;
        }

        if (localAddress != null && isValidAddress(localAddress)) {
            LOCAL_IP_ADDRESS = localAddress.getHostAddress();
        } else {
            LOCAL_IP_ADDRESS = getFirstLocalAddress();
        }
    }

    public static String getLocalAddress() {
        return LOCAL_IP_ADDRESS;
    }

    /**
     * 获取网卡中第一个有效IP
     */
    private static String getFirstLocalAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && !address.getHostAddress().contains(":")) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignored) {}

        return "127.0.0.1";
    }

    private static boolean isValidAddress(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return false;
        }

        String name = address.getHostAddress();
        return (name != null
                && !"0.0.0.0".equals(name)
                && !"127.0.0.1".equals(name)
                && IP_PATTERN.matcher(name).matches());
    }

    private NetUtil() {}
}
