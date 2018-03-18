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

package org.jupiter.serialization;

import java.io.IOException;

/**
 * jupiter
 * org.jupiter.serialization
 *
 * @author jiachun.fjc
 */
public class SerializeException extends RuntimeException {

    static final long serialVersionUID = -1L;

    public SerializeException() {}

    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializeException(Throwable cause) {
        super(cause);
    }

    public SerializeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static SerializeException invalidWriting(IOException e) {
        return new SerializeException("Writing to a byte array threw an IOException (should never happen).", e);
    }

    public static SerializeException invalidReading(IOException e) {
        return new SerializeException("Reading from a byte array threw an IOException (should never happen).", e);
    }
}
