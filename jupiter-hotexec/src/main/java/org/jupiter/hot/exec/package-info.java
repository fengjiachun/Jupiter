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

/**
 * 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子.
 *
 * 使用方式:
 *      服务端注册 {@link org.jupiter.hot.exec.JavaClassExecProvider} 作为一个provider.
 *
 *      客户端使用 {@link org.jupiter.hot.exec.JavaCompiler} 编译需要执行的类, 将编译返回的字节码byte数组
 *      作为consumer的参数, 最后再以RPC的方式调用 {@link org.jupiter.hot.exec.JavaClassExec#exec(byte[])}.
 *
 * jupiter
 * org.jupiter.hot.exec
 *
 * @author jiachun.fjc
 */
package org.jupiter.hot.exec;
