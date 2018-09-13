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

import static org.jupiter.common.util.Preconditions.checkNotNull;

/**
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class DefaultFilterChain implements JFilterChain {

    private final JFilter filter;
    private final JFilterChain next;

    public DefaultFilterChain(JFilter filter, JFilterChain next) {
        this.filter = checkNotNull(filter, "filter");
        this.next = next;
    }

    @Override
    public JFilter getFilter() {
        return filter;
    }

    @Override
    public JFilterChain getNext() {
        return next;
    }

    @Override
    public <T extends JFilterContext> void doFilter(JRequest request, T filterCtx) throws Throwable {
        filter.doFilter(request, filterCtx, next);
    }
}
