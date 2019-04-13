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
 * 飞行中调试, 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子, 注意System.out会被重定向回客户端输出.
 *
 * 实现虽简单, 但使用很灵活, 除了线上调试, 还有很多使用方式:
 *      比如你可以在不重启线上server的前提下临时写一个业务provider直接推送到对应server(需要拿到JServer实例)上以提供服务.
 *
 *      又比如某个provider阻塞时间很长, 严重占用全局的线程池, 你也可以临时写一个线程池的实现并通过
 *      {@link org.jupiter.rpc.JServer.ServiceRegistry#executor(java.util.concurrent.Executor)}
 *      将线程池注册到该provider上供其单独使用(需重新调用register).
 *
 * 使用方式(参照jupiter-example#org.jupiter.example.flight.exec*):
 *      1. 服务端注册 {@link org.jupiter.flight.exec.JavaClassExecProvider} 作为一个provider.
 *
 *      2. 客户端使用 {@link org.jupiter.flight.exec.JavaCompiler} 编译需要执行的类, 将编译返回的字节码byte数组
 *         作为consumer的参数, 最后再以RPC的方式调用 {@link org.jupiter.flight.exec.JavaClassExec#exec(byte[])}.
 *
 * jupiter
 * org.jupiter.flight.exec
 *
 * @author jiachun.fjc
 */
package org.jupiter.flight.exec;
