package com.jerryzhu.retrofitofflinecaching.bean;


public class BaseResponse<T> {

    public static final int SUCCESS = 0;
    public static final int FAIL = 0;

    private int errorCode;

    private String msg;

    private T data;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
