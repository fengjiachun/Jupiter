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

import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.StackTraceUtil;

/**
 * jupiter
 * org.jupiter.hot.exec
 *
 * @author jiachun.fjc
 */
public class JavaClassExecProvider implements JavaClassExec {

    @Override
    public String exec(byte[] classBytes) {
        try {
            HotExecClassLoader loader = new HotExecClassLoader();
            @SuppressWarnings("unchecked")
            Class<UserExecInterface> clazz = (Class<UserExecInterface>) loader.loadBytes(classBytes);
            UserExecInterface executor = Reflects.newInstance(clazz); // 不要妄想在构造函数里做任何事, 这里不调用构造函数
            return executor.exec();
        } catch (Exception e) {
            return StackTraceUtil.stackTrace(e);
        }
    }
}
