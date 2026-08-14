package com.qiji.cps.module.cps.controller.openapi.didi.vo;

public record DidiCallbackResponse(int code, String msg) {
    public static DidiCallbackResponse ok() { return new DidiCallbackResponse(0, "ok"); }
    public static DidiCallbackResponse error() { return new DidiCallbackResponse(1, "err"); }
}
