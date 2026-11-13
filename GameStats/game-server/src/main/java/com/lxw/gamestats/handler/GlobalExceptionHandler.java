package com.lxw.gamestats.handler;

import com.lxw.common.constant.MessageConstant;
import com.lxw.common.exception.BaseException;
import com.lxw.common.exception.UserNotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * 全局异常处理。
 * <p>
 * 前端 axios 拦截器在错误时读取 response.data.message，
 * 故此处统一返回 { "message": "..." } 并设置合适的 HTTP 状态码。
 * <p>
 * 参数类异常（类型转换、缺参、破损 JSON、绑定校验、上传超限）全部归 400，
 * 不得落入 {@link Exception} 兜底分支产生 500。
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
        return badRequest(ex.getMessage());
    }

    /** 请求参数类型无法转换（如 days=abc）→ 400，提示具体参数名 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型错误: {}={}", ex.getName(), ex.getValue());
        return badRequest(MessageConstant.INVALID_PARAM + "：" + ex.getName());
    }

    /** 缺少必填请求参数 → 400，提示具体参数名 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("缺少请求参数: {}", ex.getParameterName());
        return badRequest("缺少参数：" + ex.getParameterName());
    }

    /** 请求体不可读（破损 JSON / 字段类型不符）→ 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体格式错误: {}", ex.getMostSpecificCause().getMessage());
        return badRequest(MessageConstant.INVALID_PARAM + "：请求体格式错误");
    }

    /** 参数绑定失败 → 400，优先展示字段提示 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
        return badRequestWithField(ex.getBindingResult());
    }

    /** 请求体字段校验失败 → 400，与 BindException 同一响应格式 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return badRequestWithField(ex.getBindingResult());
    }

    /** multipart 超限 → 400，与业务层大小校验使用同一文案，禁止裸 413/500 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("上传文件超过大小限制: {}", ex.getMessage());
        return badRequest(MessageConstant.UPLOAD_TOO_LARGE);
    }

    /** 兜底异常 → 500，不返回堆栈与 SQL */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception ex) {
        log.error("未知异常: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", MessageConstant.UNKNOWN_ERROR));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message == null ? MessageConstant.INVALID_PARAM : message));
    }

    /** 绑定类异常统一响应：有字段提示则拼接，否则仅返回通用参数错误 */
    private ResponseEntity<Map<String, String>> badRequestWithField(BindingResult bindingResult) {
        String fieldMessage = firstFieldMessage(bindingResult);
        log.warn("参数绑定失败: {}", fieldMessage);
        return badRequest(fieldMessage == null
                ? MessageConstant.INVALID_PARAM
                : MessageConstant.INVALID_PARAM + "：" + fieldMessage);
    }

    /** 取第一个字段错误的提示文案，无字段错误返回 null */
    private String firstFieldMessage(BindingResult bindingResult) {
        if (bindingResult == null) {
            return null;
        }
        FieldError fieldError = bindingResult.getFieldError();
        return fieldError == null ? null : fieldError.getDefaultMessage();
    }
}