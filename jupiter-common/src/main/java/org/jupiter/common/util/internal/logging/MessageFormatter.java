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
package org.jupiter.common.util.internal.logging;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

// contributors: lizongbo: proposed special treatment of array parameter values
// Joern Huxhorn: pointed out double[] omission, suggested deep array copy

/**
 * Formats messages according to very simple substitution rules. Substitutions
 * can be made 1, 2 or more arguments.
 * <p/>
 * <p/>
 * For example,
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;)
 * </pre>
 * <p/>
 * will return the string "Hi there.".
 * <p/>
 * The {} pair is called the <em>formatting anchor</em>. It serves to designate
 * the location where arguments need to be substituted within the message
 * pattern.
 * <p/>
 * In case your message contains the '{' or the '}' character, you do not have
 * to do anything special unless the '}' character immediately follows '{'. For
 * example,
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Set {1,2,3} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * will return the string "Set {1,2,3} is not equal to 1,2.".
 * <p/>
 * <p/>
 * If for whatever reason you need to place the string "{}" in the message
 * without its <em>formatting anchor</em> meaning, then you need to escape the
 * '{' character with '\', that is the backslash character. Only the '{'
 * character should be escaped. There is no need to escape the '}' character.
 * For example,
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Set \\{} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * will return the string "Set {} is not equal to 1,2.".
 * <p/>
 * <p/>
 * The escaping behavior just described can be overridden by escaping the escape
 * character '\'. Calling
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;File name is C:\\\\{}.&quot;, &quot;file.zip&quot;);
 * </pre>
 * <p/>
 * will return the string "File name is C:\file.zip".
 * <p/>
 * <p/>
 * The formatting conventions are different than those of {@link MessageFormat}
 * which ships with the Java platform. This is justified by the fact that
 * SLF4J's implementation is 10 times faster than that of {@link MessageFormat}.
 * This local performance difference is both measurable and significant in the
 * larger context of the complete logging processing chain.
 * <p/>
 * <p/>
 * See also {@link #format(String, Object)},
 * {@link #format(String, Object, Object)} and
 * {@link #arrayFormat(String, Object[])} methods for more details.
 * <p/>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
final class MessageFormatter {
    private static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';

    /**
     * Performs single argument substitution for the 'messagePattern' passed as
     * parameter.
     *
     * For example,
     *
     * <pre>
     * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
     * </pre>
     *
     * will return the string "Hi there.".
     *
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param arg            The argument to be substituted in place of the formatting anchor
     * @return The formatted message
     */
    static FormattingTuple format(String messagePattern, Object arg) {
        return arrayFormat(messagePattern, new Object[]{arg});
    }

    /**
     * Performs a two argument substitution for the 'messagePattern' passed as
     * parameter.
     *
     * For example,
     *
     * <pre>
     * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
     * </pre>
     *
     * will return the string "Hi Alice. My name is Bob.".
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param argA           The argument to be substituted in place of the first formatting
     *                       anchor
     * @param argB           The argument to be substituted in place of the second formatting
     *                       anchor
     * @return The formatted message
     */
    static FormattingTuple format(final String messagePattern,
                                  Object argA, Object argB) {
        return arrayFormat(messagePattern, new Object[]{argA, argB});
    }

    /**
     * Same principle as the {@link #format(String, Object)} and
     * {@link #format(String, Object, Object)} methods except that any number of
     * arguments can be passed in an array.
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param argArray       An array of arguments to be substituted in place of formatting
     *                       anchors
     * @return The formatted message
     */
    static FormattingTuple arrayFormat(final String messagePattern,
                                       final Object[] argArray) {
        if (argArray == null || argArray.length == 0) {
            return new FormattingTuple(messagePattern, null);
        }

        int lastArrIdx = argArray.length - 1;
        Object lastEntry = argArray[lastArrIdx];
        Throwable throwable = lastEntry instanceof Throwable ? (Throwable) lastEntry : null;

        if (messagePattern == null) {
            return new FormattingTuple(null, throwable);
        }

        int j = messagePattern.indexOf(DELIM_STR);
        if (j == -1) {
            // this is a simple string
            return new FormattingTuple(messagePattern, throwable);
        }

        StringBuilder buf = new StringBuilder(messagePattern.length() + 50);
        int i = 0;
        int L = 0;
        do {
            boolean notEscaped = j == 0 || messagePattern.charAt(j - 1) != ESCAPE_CHAR;
            if (notEscaped) {
                // normal case
                buf.append(messagePattern, i, j);
            } else {
                buf.append(messagePattern, i, j - 1);
                // check that escape char is not is escaped: "abc x:\\{}"
                notEscaped = j >= 2 && messagePattern.charAt(j - 2) == ESCAPE_CHAR;
            }

            i = j + 2;
            if (notEscaped) {
                deeplyAppendParameter(buf, argArray[L], null);
                L++;
                if (L > lastArrIdx) {
                    break;
                }
            } else {
                buf.append(DELIM_STR);
            }
            j = messagePattern.indexOf(DELIM_STR, i);
        } while (j != -1);

        // append the characters following the last {} pair.
        buf.append(messagePattern, i, messagePattern.length());
        return new FormattingTuple(buf.toString(), L <= lastArrIdx ? throwable : null);
    }

    // special treatment of array values was suggested by 'lizongbo'
    private static void deeplyAppendParameter(StringBuilder buf, Object o,
                                              Set<Object[]> seenSet) {
        if (o == null) {
            buf.append("null");
            return;
        }
        Class<?> objClass = o.getClass();
        if (!objClass.isArray()) {
            if (Number.class.isAssignableFrom(objClass)) {
                // Prevent String instantiation for some number types
                if (objClass == Long.class) {
                    buf.append(((Long) o).longValue());
                } else if (objClass == Integer.class || objClass == Short.class || objClass == Byte.class) {
                    buf.append(((Number) o).intValue());
                } else if (objClass == Double.class) {
                    buf.append(((Double) o).doubleValue());
                } else if (objClass == Float.class) {
                    buf.append(((Float) o).floatValue());
                } else {
                    safeObjectAppend(buf, o);
                }
            } else {
                safeObjectAppend(buf, o);
            }
        } else {
            // check for primitive array types because they
            // unfortunately cannot be cast to Object[]
            buf.append('[');
            if (objClass == boolean[].class) {
                booleanArrayAppend(buf, (boolean[]) o);
            } else if (objClass == byte[].class) {
                byteArrayAppend(buf, (byte[]) o);
            } else if (objClass == char[].class) {
                charArrayAppend(buf, (char[]) o);
            } else if (objClass == short[].class) {
                shortArrayAppend(buf, (short[]) o);
            } else if (objClass == int[].class) {
                intArrayAppend(buf, (int[]) o);
            } else if (objClass == long[].class) {
                longArrayAppend(buf, (long[]) o);
            } else if (objClass == float[].class) {
                floatArrayAppend(buf, (float[]) o);
            } else if (objClass == double[].class) {
                doubleArrayAppend(buf, (double[]) o);
            } else {
                objectArrayAppend(buf, (Object[]) o, seenSet);
            }
            buf.append(']');
        }
    }

    private static void safeObjectAppend(StringBuilder buf, Object o) {
        try {
            String oAsString = o.toString();
            buf.append(oAsString);
        } catch (Throwable t) {
            System.err.println("SLF4J: Failed toString() invocation on an object of type ["  + o.getClass().getName() + ']');
            t.printStackTrace();
            buf.append("[FAILED toString()]");
        }
    }

    private static void objectArrayAppend(StringBuilder buf, Object[] a, Set<Object[]> seenSet) {
        if (a.length == 0) {
            return;
        }
        if (seenSet == null) {
            seenSet = new HashSet<>(a.length);
        }
        if (seenSet.add(a)) {
            deeplyAppendParameter(buf, a[0], seenSet);
            for (int i = 1; i < a.length; i++) {
                buf.append(", ");
                deeplyAppendParameter(buf, a[i], seenSet);
            }
            // allow repeats in siblings
            seenSet.remove(a);
        } else {
            buf.append("...");
        }
    }

    private static void booleanArrayAppend(StringBuilder buf, boolean[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void byteArrayAppend(StringBuilder buf, byte[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void charArrayAppend(StringBuilder buf, char[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void shortArrayAppend(StringBuilder buf, short[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void intArrayAppend(StringBuilder buf, int[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void longArrayAppend(StringBuilder buf, long[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void floatArrayAppend(StringBuilder buf, float[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private static void doubleArrayAppend(StringBuilder buf, double[] a) {
        if (a.length == 0) {
            return;
        }
        buf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            buf.append(", ");
            buf.append(a[i]);
        }
    }

    private MessageFormatter() {}
}
