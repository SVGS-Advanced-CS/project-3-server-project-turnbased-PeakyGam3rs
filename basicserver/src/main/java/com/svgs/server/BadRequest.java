package com.svgs.server;

public class BadRequest extends ApiException {
    public BadRequest(String code, String msg) {
        super(400, code, msg);
    }
}


