package org.jupiter.rpc;

import java.util.EventListener;

/**
 * RPC 回调, 一个服务对象中的所有方法都共用一个JListener, 以参数request作为区别
 *
 * 注意:
 * {@link JListener#complete(Request, Object)}执行过程中出现异常会触发 {@link JListener#failure(Request, Throwable)}
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public interface JListener extends EventListener {

    /**
     * 调用成功返回结果
     *
     * @param request 请求对象
     * @param result 返回结果
     * @throws Exception
     */
    void complete(Request request, Object result) throws Exception;

    /**
     * 调用失败返回异常信息
     *
     * @param request 请求对象
     * @param cause 异常信息
     */
    void failure(Request request, Throwable cause);
}
