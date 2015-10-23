package org.jupiter.serialization.proto;

import org.junit.Test;
import org.jupiter.common.util.Recyclable;
import org.jupiter.common.util.internal.Recyclers;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerHolder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * jupiter
 * org.jupiter.serialization.proto
 *
 * @author jiachun.fjc
 */
public class ProtoStuffSerializerTest {

    @Test
    public void testSerializer() {
        Serializer serializer = SerializerHolder.getSerializer();
        ResultWrapper wrapper = ResultWrapper.getInstance();
        wrapper.setResult("test");
        byte[] bytes = serializer.writeObject(wrapper);
        ResultWrapper wrapper1 = serializer.readObject(bytes, ResultWrapper.class);
        System.out.println(wrapper1.getResult());
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

class ResultWrapper implements Recyclable, Serializable {

    private static final long serialVersionUID = -1126932930252953428L;

    private Object result; // 服务调用结果
    private String error; // 错误信息

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

    /**
     * 获取一个ResultWrapper对象
     */
    public static ResultWrapper getInstance() {
        return recyclers.get();
    }

    /**
     * 照顾一些需要调用默认构造函数的反序列化框架
     */
    public ResultWrapper() {
        this.handle = null;
    }

    /**
     * 回收ResultWrapper对象, 如果不是通过getInstance()不需要回收
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

interface SerializerInterface {
    String getStr();
    void setStr(String str);
}

class SerializerObj implements SerializerInterface {

    String str;
    List<String> list = new ArrayList<String>();

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
