package com.suyo.suyo.session;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import lombok.Getter;
import lombok.Setter;

/** 요청 하나 동안 유효한 익명 세션 ID 홀더. SessionIdInterceptor가 채운다. */
@Component
@RequestScope
@Getter
@Setter
public class SessionContext {

    private String sessionId;
}
