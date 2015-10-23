package org.jupiter.common.util;

import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */
public class SystemPropertyUtil {

    private static boolean initializedLogger;
    private static final InternalLogger logger;
    private static boolean loggedException;

    static {
        initializedLogger = false;
        logger = InternalLoggerFactory.getInstance(SystemPropertyUtil.class);
        initializedLogger = true;
    }

    /**
     * Returns {@code true} if and only if the system property with the specified {@code key}
     * exists.
     */
    public static boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to {@code null} if the property access fails.
     *
     * @return the property value or {@code null}
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static String get(final String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("key must not be empty.");
        }

        String value = null;
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value = AccessController.doPrivileged(new PrivilegedAction<String>() {

                    @Override
                    public String run() {
                        return System.getProperty(key);
                    }
                });
            }
        } catch (Exception e) {
            if (!loggedException) {
                log("Unable to retrieve a system property '" + key + "'; default values will be used.", e);
                loggedException = true;
            }
        }

        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static boolean getBoolean(String key, boolean def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        log("Unable to parse the boolean system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+");

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static int getInt(String key, int def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (INTEGER_PATTERN.matcher(value).matches()) {
            try {
                return Integer.parseInt(value);
            } catch (Exception ignored) {}
        }

        log("Unable to parse the integer system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified
     * {@code key}, while falling back to the specified default value if
     * the property access fails.
     *
     * @return the property value.
     *         {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static long getLong(String key, long def) {
        String value = get(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (INTEGER_PATTERN.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (Exception ignored) {}
        }

        log("Unable to parse the long integer system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    /**
     * Sets the value of the Java system property with the specified {@code key}
     */
    public static Object setProperty(String key, String value) {
        return System.getProperties().setProperty(key, value);
    }

    private static void log(String msg) {
        if (initializedLogger) {
            logger.warn(msg);
        } else {
            // Use JDK logging if logger was not initialized yet.
            Logger.getLogger(SystemPropertyUtil.class.getName()).log(Level.WARNING, msg);
        }
    }

    private static void log(String msg, Exception e) {
        if (initializedLogger) {
            logger.warn(msg, e);
        } else {
            // Use JDK logging if logger was not initialized yet.
            Logger.getLogger(SystemPropertyUtil.class.getName()).log(Level.WARNING, msg, e);
        }
    }

    private SystemPropertyUtil() {}
}