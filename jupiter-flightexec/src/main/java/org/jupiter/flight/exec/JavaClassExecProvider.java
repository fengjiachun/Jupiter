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

import static org.jupiter.common.util.internal.UnsafeAccess.UNSAFE;

/**
 * jupiter
 * org.jupiter.flight.exec
 *
 * @author jiachun.fjc
 */
public class JavaClassExecProvider implements JavaClassExec {

    @Override
    public ExecResult exec(byte[] classBytes) {
        ExecResult result = new ExecResult();
        UserExecInterface executor = null;
        try {
            // modify class
            ClassModifier cm = new ClassModifier(classBytes);
            classBytes = cm.modifyUTF8Constant("java/lang/System", "org/jupiter/flight/exec/HackSystem");

            // load class
            FlightExecClassLoader loader = new FlightExecClassLoader();
            Class<?> clazz = loader.loadBytes(classBytes);

            executor = (UserExecInterface) clazz.newInstance();
        } catch (Throwable t) {
            UNSAFE.throwException(t);
        }

        synchronized (HackSystem.class) {
            HackSystem.clearBuf();
            Object value = null;
            try {
                // execute
                if (executor != null) {
                    value = executor.exec();
                }
            } catch (Throwable t) {
                t.printStackTrace(HackSystem.out);
            } finally {
                result.setDebugInfo(HackSystem.getBufString());
                result.setValue(value);
            }
        }
        return result;
    }
}
