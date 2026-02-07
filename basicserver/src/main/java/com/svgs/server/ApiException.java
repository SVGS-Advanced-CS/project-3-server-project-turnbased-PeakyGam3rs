package com.svgs.server;
public class ApiException extends RuntimeException {
    public final int status;
    public final String code;
    public final String safeMessage;

    public ApiException(int status, String code, String safeMessage) {
        super(safeMessage);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
    }

    public ApiException(int status, String code, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.status = status;
        this.code = code;
        this.safeMessage = safeMessage;
    }
}
