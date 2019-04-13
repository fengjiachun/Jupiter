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
     * Splits the provided text into an array, separator specified.
     *
     * A null input String returns null.
     *
     * Strings.split(null, *)         = null
     * Strings.split("", *)           = []
     * Strings.split("a.b.c", '.')    = ["a", "b", "c"]
     * Strings.split("a..b.c", '.')   = ["a", "b", "c"]
     * Strings.split("a:b:c", '.')    = ["a:b:c"]
     * Strings.split("a b c", ' ')    = ["a", "b", "c"]
     */
    public static String[] split(String str, char separator) {
        return split(str, separator, false);
    }

    /**
     * Splits the provided text into an array, separator specified,
     * if {@code} true, preserving all tokens, including empty tokens created
     * by adjacent separators.
     *
     * A null input String returns null.
     *
     * Strings.split(null, *, true)         = null
     * Strings.split("", *, true)           = []
     * Strings.split("a.b.c", '.', true)    = ["a", "b", "c"]
     * Strings.split("a..b.c", '.', true)   = ["a", "", "b", "c"]
     * Strings.split("a:b:c", '.', true)    = ["a:b:c"]
     * Strings.split("a b c", ' ', true)    = ["a", "b", "c"]
     * Strings.split("a b c ", ' ', true)   = ["a", "b", "c", ""]
     * Strings.split("a b c  ", ' ', true)  = ["a", "b", "c", "", ""]
     * Strings.split(" a b c", ' ', true)   = ["", a", "b", "c"]
     * Strings.split("  a b c", ' ', true)  = ["", "", a", "b", "c"]
     * Strings.split(" a b c ", ' ', true)  = ["", a", "b", "c", ""]
     */
    public static String[] split(String str, char separator, boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<>();
        int i = 0, start = 0;
        boolean match = false;
        while (i < len) {
            if (str.charAt(i) == separator) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                }
                start = ++i;
                continue;
            }
            match = true;
            i++;
        }
        if (match || preserveAllTokens) {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[0]);
    }

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private Strings() {}
}
