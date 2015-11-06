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

package org.jupiter.rpc.aop;

import org.jupiter.rpc.Request;

/**
 * Consumer's hook.
 *
 * jupiter
 * org.jupiter.rpc.aop
 *
 * @author jiachun.fjc
 */
public interface ConsumerHook {

    /**
     * Will be triggered when the request data sent to the network.
     */
    void before(Request request);

    /**
     * Will to be triggered when the server returns the results.
     */
    void after(Request request);
}
