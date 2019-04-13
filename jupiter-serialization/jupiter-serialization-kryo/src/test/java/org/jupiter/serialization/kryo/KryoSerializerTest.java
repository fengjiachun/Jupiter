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
package org.jupiter.serialization.kryo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * jupiter
 * org.jupiter.serialization.kryo
 *
 * @author jiachun.fjc
 */
public class KryoSerializerTest {

    @Test
    public void testSerializer() {
        Serializer serializer = SerializerFactory.getSerializer(SerializerType.KRYO.value());
        ResultWrapper wrapper = new ResultWrapper();
        wrapper.setResult("test");
        wrapper.setError(new RuntimeException("test"));
        // Class<?>[] parameterTypes 需要优化 -------- 后续: 已优化掉了
        wrapper.setClazz(new Class[] { String.class, ArrayList.class, Serializable.class });
        byte[] bytes = serializer.writeObject(wrapper);
        ResultWrapper wrapper1 = serializer.readObject(bytes, ResultWrapper.class);
        wrapper1.getError().printStackTrace();
        System.out.println(bytes.length);
        System.out.println(wrapper1.getResult());
        // noinspection ImplicitArrayToString
        System.out.println(wrapper1.getClazz());
        assertThat(String.valueOf(wrapper1.getResult()), is("test"));

        SerializerInterface obj = new SerializerObj();
        obj.setStr("SerializerObj1");
        wrapper.setResult(obj);
        bytes = serializer.writeObject(wrapper);
        ResultWrapper wrapper2 = serializer.readObject(bytes, ResultWrapper.class);
        System.out.println(wrapper2.getResult());
        assertThat(String.valueOf(wrapper2.getResult()), is(obj.toString()));
    }
}

class ResultWrapper implements Serializable {

    private static final long serialVersionUID = -1126932930252953428L;

    private Object result; // 服务调用结果
    private Exception error; // 错误信息
    private Class<?>[] clazz;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public Class<?>[] getClazz() {
        return clazz;
    }

    public void setClazz(Class<?>[] clazz) {
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return "ResultWrapper{" +
                "result=" + result +
                ", error=" + error +
                '}';
    }
}

interface SerializerInterface {
    String getStr();
    void setStr(String str);
}

class SerializerObj implements SerializerInterface {

    String str;
    List<String> list = new ArrayList<>();

    public SerializerObj() { // 反序列化时不会被调用
        list.add("test");
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return "SerializerObj{" +
                "str='" + str + '\'' +
                ", list=" + list +
                '}';
    }
}
