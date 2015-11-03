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

package org.jupiter.rpc.provider.limiter;

import org.jupiter.rpc.Request;

/**
 * jupiter
 * org.jupiter.rpc.provider.limiter
 *
 * @author jiachun.fjc
 */
public class DefaultTPSLimiter implements TPSLimiter {

    @Override
    public TPSResult process(Request request) {
        // TODO 以APP为最小粒度限流? 还是以方法为最小粒度? 我再仔细想想
        return new TPSResult(true, null);
    }
}
