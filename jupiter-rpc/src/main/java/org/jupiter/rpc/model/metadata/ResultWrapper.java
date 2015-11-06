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

import org.jupiter.common.util.Recyclable;
import org.jupiter.common.util.internal.Recyclers;

import java.io.Serializable;

/**
 * Response data wrapper.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ResultWrapper implements Recyclable, Serializable {

    private static final long serialVersionUID = -1126932930252953428L;

    // 服务调用结果
    private Object result;
    // 错误信息
    private Throwable error;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    /**
     * 获取一个ResultWrapper对象
     */
    public static ResultWrapper getInstance() {
        return recyclers.get();
    }

    public ResultWrapper() {
        this.handle = null;
    }

    /**
     * 回收ResultWrapper对象, 不是通过getInstance()获得实例的不需要回收
     */
    @Override
    public boolean recycle() {
        if (handle == null) return false;

        // help GC
        result = null;
        error = null;

        return recyclers.recycle(this, handle);
    }

    private static final Recyclers<ResultWrapper> recyclers = new Recyclers<ResultWrapper>() {

        @Override
        protected ResultWrapper newObject(Handle<ResultWrapper> handle) {
            return new ResultWrapper(handle);
        }
    };

    private transient final Recyclers.Handle<ResultWrapper> handle;

    private ResultWrapper(Recyclers.Handle<ResultWrapper> handle) {
        this.handle = handle;
    }

    @Override
    public String toString() {
        return "ResultWrapper{" +
                "result=" + result +
                ", error=" + error +
                '}';
    }
}
