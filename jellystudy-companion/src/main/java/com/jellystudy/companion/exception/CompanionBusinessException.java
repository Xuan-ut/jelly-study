package com.jellystudy.companion.exception;

/**
 * 业务异常基类
 */
public class CompanionBusinessException extends RuntimeException {
    private final int code;

    public CompanionBusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CompanionBusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
