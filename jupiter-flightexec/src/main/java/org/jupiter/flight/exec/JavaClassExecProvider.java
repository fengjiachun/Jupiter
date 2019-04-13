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

import org.jupiter.common.util.ThrowUtil;
import org.jupiter.rpc.ServiceProviderImpl;

/**
 * 把它作为一个服务提供者, 它会修改常量池中 {@link java.lang.System} 类的指向,
 * 可以将 System.out 重定向到你的客户端, 你可以在客户端代码中直接使用 System.out.println() 输入你想要的信息,
 * 最终 System.out.println() 的输出会完整的返回到客户端.
 *
 * jupiter
 * org.jupiter.flight.exec
 *
 * @author jiachun.fjc
 */
@ServiceProviderImpl
public class JavaClassExecProvider implements JavaClassExec {

    private static String SYSTEM_STRING = System.class.getName().replace('.', '/');
    private static String HACK_SYSTEM_STRING = HackSystem.class.getName().replace('.', '/');

    @Override
    public ExecResult exec(byte[] classBytes) {
        ExecResult result = new ExecResult();
        UserExecInterface executor = null;
        try {
            // modify class
            ClassModifier cm = new ClassModifier(classBytes);
            classBytes = cm.modifyUTF8Constant(SYSTEM_STRING, HACK_SYSTEM_STRING);

            // load class
            FlightExecClassLoader loader = new FlightExecClassLoader();
            Class<?> clazz = loader.loadBytes(classBytes);

            executor = (UserExecInterface) clazz.newInstance();
        } catch (Throwable t) {
            ThrowUtil.throwException(t);
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
