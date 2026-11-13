package com.example.aspect;

import com.example.common.CurrentUserContext;
import com.example.annotation.OperationLog;
import com.example.entity.User;
import com.example.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final int MAX_PARAMS_LENGTH = 2000;

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(com.example.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        String operationType = annotation.value().isBlank() ? method.getName() : annotation.value();

        Object result = null;
        Throwable throwable = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            try {
                saveLog(point, method, operationType, start, throwable);
            } catch (Exception e) {
                log.warn("操作日志记录失败", e);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint point, Method method, String operationType,
                         long start, Throwable throwable) {
        User current = CurrentUserContext.get();
        HttpServletRequest request = getRequest();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        com.example.entity.OperationLog operationLog = new com.example.entity.OperationLog();
        operationLog.setUserId(current == null ? null : current.getId());
        operationLog.setUsername(current == null ? null : current.getUsername());
        operationLog.setProjectId(extractProjectId(point.getArgs(), annotation.projectIdParam()));
        operationLog.setOperationType(operationType);
        operationLog.setMethodName(method.getDeclaringClass().getName() + "." + method.getName());
        operationLog.setParams(truncate(serializerParams(point.getArgs())));
        operationLog.setIp(getIp(request));
        operationLog.setDurationMs((int) (System.currentTimeMillis() - start));
        operationLog.setStatus(throwable == null ? 1 : 0);
        operationLog.setErrorMsg(throwable == null ? null : truncate(throwable.getMessage()));
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLogService.save(operationLog);
    }

    private Integer extractProjectId(Object[] args, String projectIdParam) {
        if (args == null) {
            return null;
        }
        if (projectIdParam != null && !projectIdParam.isBlank()) {
            for (Object arg : args) {
                if (arg == null) continue;
                Integer value = getNestedValue(arg, projectIdParam);
                if (value != null) return value;
            }
        }
        for (Object arg : args) {
            if (arg == null) continue;
            Integer value = findProjectIdInObject(arg);
            if (value != null) return value;
        }
        return null;
    }

    private Integer findProjectIdInObject(Object obj) {
        if (obj instanceof Integer) {
            return null;
        }
        Class<?> clazz = obj.getClass();
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if ("projectId".equals(field.getName()) && (field.getType() == Integer.class || field.getType() == int.class)) {
                field.setAccessible(true);
                try {
                    return (Integer) field.get(obj);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private Integer getNestedValue(Object obj, String path) {
        String[] parts = path.split("\\.");
        Object current = obj;
        for (String part : parts) {
            if (current == null) return null;
            try {
                java.lang.reflect.Field field = current.getClass().getDeclaredField(part);
                field.setAccessible(true);
                current = field.get(current);
            } catch (Exception e) {
                return null;
            }
        }
        return current instanceof Integer ? (Integer) current : null;
    }

    private String serializerParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        try {
            return MAPPER.writeValueAsString(args);
        } catch (Exception e) {
            return String.valueOf(args);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_PARAMS_LENGTH ? text.substring(0, MAX_PARAMS_LENGTH) + "..." : text;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    private String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
