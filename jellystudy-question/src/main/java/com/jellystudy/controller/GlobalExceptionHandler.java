package com.jellystudy.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "服务器错误: " + e.getMessage());
        result.put("exception", e.getClass().getName());
        if (e.getCause() != null) {
            result.put("cause", e.getCause().getMessage());
            result.put("causeClass", e.getCause().getClass().getName());
        }
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleThrowable(Throwable t) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "严重错误: " + t.getMessage());
        result.put("exception", t.getClass().getName());
        if (t.getCause() != null) {
            result.put("cause", t.getCause().getMessage());
            result.put("causeClass", t.getCause().getClass().getName());
        }
        t.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}
