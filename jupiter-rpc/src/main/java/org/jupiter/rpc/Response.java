package org.jupiter.rpc;

import org.jupiter.common.util.Recyclable;
import org.jupiter.common.util.RecycleUtil;
import org.jupiter.rpc.model.metadata.ResultWrapper;

import static org.jupiter.rpc.Status.OK;
import static org.jupiter.rpc.Status.parse;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class Response extends BytesHolder implements Recyclable {

    private byte status = OK.value();

    private long id; // invoke id
    private ResultWrapper result; // 服务调用结果

    public Response() {}

    public Response(long id) {
        this.id = id;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }

    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
    }

    public ResultWrapper result() {
        return result;
    }

    public void result(ResultWrapper result) {
        this.result = result;
    }

    /**
     * Response对象应在encode之后回收.
     */
    @Override
    public boolean recycle() {
        Object recyclableObject = result;
        result = null; // 如果result对象已经逃逸出去了就没啥好办法了, 但也不会引起致命错误

        return RecycleUtil.recycle(recyclableObject);
    }

    @Override
    public String toString() {
        return "Response{" +
                "status=" + parse(status) +
                ", id=" + id +
                ", result=" + result +
                '}';
    }
}
