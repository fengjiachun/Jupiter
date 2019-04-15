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

/**
 * 远程调用方式, 支持同步调用和异步调用, 异步方式支持 Future 以及 Listener.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public enum InvokeType {
    SYNC,   // 同步调用
    ASYNC,  // 异步调用
    AUTO;   // 当接口返回值是一个 CompletableFuture 或者它的子类将自动适配为异步调用, 否则为同步调用

    public static InvokeType parse(String name) {
        for (InvokeType s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static InvokeType getDefault() {
        return AUTO;
    }
}
