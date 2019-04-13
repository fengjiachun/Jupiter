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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.jupiter.common.util.Lists;
import org.jupiter.common.util.ThrowUtil;

/**
 * Java compiler, base on javac.
 *
 * 基于javac的编译工具.
 *
 * jupiter
 * org.jupiter.flight.exec
 *
 * @author jiachun.fjc
 */
public class JavaCompiler {

    public static byte[] compile(String classPath, String className, List<String> args) {
        return compile(classPath, Lists.newArrayList(className), args);
    }

    public static byte[] compile(final String classPath, List<String> classNames, List<String> args) {
        StandardJavaFileManager javaFileManager = null;
        ClassFileManager classFileManager = null;
        try {
            javax.tools.JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
            javaFileManager = javac.getStandardFileManager(null, null, null);
            classFileManager = new ClassFileManager(javaFileManager);

            List<String> classFiles = Lists.transform(classNames, input -> classPath + input.replace(".", "/") + ".java");
            String[] names = classFiles.toArray(new String[classFiles.size()]);
            javax.tools.JavaCompiler.CompilationTask javacTask =
                    javac.getTask(null, classFileManager, null, args, null, javaFileManager.getJavaFileObjects(names));

            if (javacTask.call()) {
                return classFileManager.getJavaClassObject().classBytes();
            }
        } catch (Throwable t) {
            ThrowUtil.throwException(t);
        } finally {
            if (javaFileManager != null) {
                try {
                    javaFileManager.close();
                } catch (IOException ignored) {}
            }
            if (classFileManager != null) {
                try {
                    classFileManager.close();
                } catch (IOException ignored) {}
            }
        }
        return null;
    }

    /**
     * Class文件管理器
     *
     * @author jiachun.fjc
     */
    static class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

        private JavaClassObject javaclassObject;

        protected ClassFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            javaclassObject = new JavaClassObject(className, kind);
            return javaclassObject;
        }

        public JavaClassObject getJavaClassObject() {
            return javaclassObject;
        }
    }

    /**
     * 类文件上进行操作的工具的文件抽象
     *
     * @author jiachun.fjc
     */
    static class JavaClassObject extends SimpleJavaFileObject {

        protected final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        public JavaClassObject(String name, JavaFileObject.Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return bos;
        }

        /**
         * 返回编译好的字节码
         */
        public byte[] classBytes() {
            return bos.toByteArray();
        }
    }
}
