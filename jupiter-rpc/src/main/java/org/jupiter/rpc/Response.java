package org.jupiter.rpc;

import org.jupiter.rpc.model.metadata.ResultWrapper;

import static org.jupiter.rpc.Status.OK;
import static org.jupiter.rpc.Status.parse;

/**
 * Provider响应数据
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class Response extends BytesHolder {

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

    @Override
    public String toString() {
        return "Response{" +
                "status=" + parse(status) +
                ", id=" + id +
                ", result=" + result +
                '}';
    }
}
