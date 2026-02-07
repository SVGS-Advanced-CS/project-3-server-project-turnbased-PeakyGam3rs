package com.svgs.server;

public class NotFound extends ApiException {
    public NotFound(String code, String msg) {
        super(404, code, msg);
    }
}


