package com.lxw.gamestats.handler;

import com.lxw.common.constant.MessageConstant;
import com.lxw.common.exception.BaseException;
import com.lxw.common.exception.UserNotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理。
 * <p>
 * 前端 axios 拦截器在错误时读取 response.data.message，
 * 故此处统一返回 { "message": "..." } 并设置合适的 HTTP 状态码。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未登录 → 401，前端会清空登录态并跳转登录页 */
    @ExceptionHandler(UserNotLoginException.class)
    public ResponseEntity<Map<String, String>> handleNotLogin(UserNotLoginException ex) {
        log.warn("用户未登录: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage()));
    }

    /** 业务异常 → 400，前端默认分支展示 message */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BaseException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    /** 兜底异常 → 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception ex) {
        log.error("未知异常: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", MessageConstant.UNKNOWN_ERROR));
    }
}
