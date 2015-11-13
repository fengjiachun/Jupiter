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

package org.jupiter.hot.exec;

/**
 * jupiter
 * org.jupiter.hot.exec
 *
 * @author jiachun.fjc
 */
public class JavaClassExecProvider implements JavaClassExec {

    @Override
    public ExecResult exec(byte[] classBytes) {
        ExecResult result = new ExecResult();
        try {
            // modify class
            ClassModifier cm = new ClassModifier(classBytes);
            classBytes = cm.modifyUTF8Constant("java/lang/System", "org/jupiter/hot/exec/HackSystem");

            // load class
            HotExecClassLoader loader = new HotExecClassLoader();
            Class<?> clazz = loader.loadBytes(classBytes);

            synchronized (HackSystem.class) {
                HackSystem.clearBuf();
                UserExecInterface executor = (UserExecInterface) clazz.newInstance();
                // execute
                Object value = executor.exec();

                result.setDebugInfo(HackSystem.getBufString());
                result.setValue(value);
            }
        } catch (Throwable t) {
            synchronized (HackSystem.class) {
                t.printStackTrace(HackSystem.out);
            }
        }

        return result;
    }
}
