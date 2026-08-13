package com.suyo.suyo.session;

import java.util.UUID;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그인 없는 익명 세션 식별자. 프론트가 X-Session-Id 헤더로 보내면 그대로 쓰고,
 * 없으면 새로 발급해서 응답 헤더로 내려준다.
 */
@RequiredArgsConstructor
public class SessionIdInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "X-Session-Id";

    private final SessionContext sessionContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String sessionId = request.getHeader(HEADER_NAME);
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        sessionContext.setSessionId(sessionId);
        response.setHeader(HEADER_NAME, sessionId);
        return true;
    }
}
