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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public class JvmToolsTest {

    public static void main(String[] args) {
        FileOutputStream stackOutput = null;
        try {
            stackOutput = new FileOutputStream(new File("jupiter.dump.log"));
            List<String> stacks = JvmTools.jStack();
            for (String s : stacks) {
                stackOutput.write(s.getBytes(JConstants.UTF8));
            }

            JvmTools.jMap("jupiter.dump.bin", false);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (stackOutput != null) {
                try {
                    stackOutput.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
