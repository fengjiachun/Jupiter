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

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility methods pertaining to {@code String} or {@code CharSequence}
 * instances.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class Strings {

    /**
     * Returns the given string if it is non-null; the empty string otherwise.
     */
    public static String nullToEmpty(String string) {
        return (string == null) ? "" : string;
    }

    /**
     * Returns the given string if it is nonempty; {@code null} otherwise.
     */
    public static String emptyToNull(String string) {
        return isNullOrEmpty(string) ? null : string;
    }

    /**
     * Returns {@code true} if the given string is null or is the empty string.
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * Checks if a string is whitespace, empty ("") or null.
     *
     * Strings.isBlank(null)      = true
     * Strings.isBlank("")        = true
     * Strings.isBlank(" ")       = true
     * Strings.isBlank("bob")     = false
     * Strings.isBlank("  bob  ") = false
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for (int i = 0; i < strLen; i++) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if a string is not empty (""), not null and not whitespace only.
     *
     * Strings.isNotBlank(null)      = false
     * Strings.isNotBlank("")        = false
     * Strings.isNotBlank(" ")       = false
     * Strings.isNotBlank("bob")     = true
     * Strings.isNotBlank("  bob  ") = true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Splits the specified {@link String} with the specified delimiter.  This operation is a simplified and optimized
     * version of {@link String#split(String)}.
     */
    public static String[] split(String value, char delimiter) {
        if (value == null) {
            return null;
        }

        final int end = value.length();
        final List<String> result = new ArrayList<>();

        int start = 0;
        for (int i = 0; i < end; i++) {
            if (value.charAt(i) == delimiter) {
                if (start == i) {
                    result.add(EMPTY_STRING);
                } else {
                    result.add(value.substring(start, i));
                }
                start = i + 1;
            }
        }

        if (start == 0) { // if no delimiter was found in the value
            result.add(value);
        } else {
            if (start != end) {
                // add the last element if it's not empty.
                result.add(value.substring(start, end));
            } else {
                // truncate trailing empty elements.
                for (int i = result.size() - 1; i >= 0; i--) {
                    if (result.get(i).isEmpty()) {
                        result.remove(i);
                    } else {
                        break;
                    }
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }

    private static final String EMPTY_STRING = "";

    private Strings() {}
}
