package com.kunyan.entity;

public class IdentifyException extends Exception {
    private String msg;
    public IdentifyException(String msg) {
        this.msg = msg;
    }
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}