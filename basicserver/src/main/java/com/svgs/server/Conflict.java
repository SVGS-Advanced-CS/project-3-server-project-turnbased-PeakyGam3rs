package com.svgs.server;

public class Conflict extends ApiException {
    public Conflict(String code, String msg) {
        super(409, code, msg);
    }
}
