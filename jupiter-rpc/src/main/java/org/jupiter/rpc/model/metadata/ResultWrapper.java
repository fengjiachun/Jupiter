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

package org.jupiter.rpc.model.metadata;

import org.jupiter.common.util.StackTraceUtil;

import java.io.Serializable;

/**
 * Response data wrapper.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ResultWrapper implements Serializable {

    private static final long serialVersionUID = -1126932930252953428L;

    private Object result;
    private String error;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setError(Throwable t) {
        this.error = StackTraceUtil.stackTrace(t);
    }

    public static ResultWrapper getInstance() {
        return new ResultWrapper();
    }

    @Override
    public String toString() {
        return "ResultWrapper{" +
                "result=" + result +
                ", error=" + error +
                '}';
    }
}
