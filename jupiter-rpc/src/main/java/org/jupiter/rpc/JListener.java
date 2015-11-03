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

package org.jupiter.rpc;

import java.util.EventListener;

/**
 * RPC 回调, 一个服务对象中的所有方法都共用一个JListener, 以参数request作为区别.
 *
 * 注意:
 * {@link JListener#complete(Request, Object)}执行过程中出现异常会触发 {@link JListener#failure(Request, Throwable)}
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JListener extends EventListener {

    /**
     * 调用成功返回结果
     *
     * @param request 请求对象
     * @param result 返回结果
     * @throws Exception
     */
    void complete(Request request, Object result) throws Exception;

    /**
     * 调用失败返回异常信息
     *
     * @param request 请求对象
     * @param cause 异常信息
     */
    void failure(Request request, Throwable cause);
}
