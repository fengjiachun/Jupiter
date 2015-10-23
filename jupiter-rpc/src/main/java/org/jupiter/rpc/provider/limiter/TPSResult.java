package org.jupiter.rpc.provider.limiter;

/**
 * TPS服务限流检查结果
 *
 * jupiter
 * org.jupiter.rpc.provider.limiter
 *
 * @author jiachun.fjc
 */
public class TPSResult {

    private boolean allowed = true;
    private String message;

    public TPSResult(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "TPSResult{" +
                "allowed=" + allowed +
                ", message='" + message + '\'' +
                '}';
    }
}
