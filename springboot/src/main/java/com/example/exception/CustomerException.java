package com.example.exception;

public class CustomerException extends RuntimeException {
    private String code;
    private String meg;

    public CustomerException(String code, String meg) {
        this.code = code;
        this.meg = meg;
    }

    public CustomerException(String meg) {
        this.code = "500";
        this.meg = meg;
    }

    public CustomerException() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMeg() {
        return meg;
    }

    public void setMeg(String meg) {
        this.meg = meg;
    }

    @Override
    public String getMessage() {
        return meg;
    }
}
