package com.example.superrankinglist.interceptor;

import com.example.superrankinglist.utils.JwtUtil;
import com.example.superrankinglist.utils.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 用户上下文拦截器
 * 用于在请求开始时设置用户ID，并在请求结束时清除
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(UserContextInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取token
        String token = request.getHeader("Authorization");
        logger.debug("Received token: {}", token);
        
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil != null && jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                if (userId != null) {
                    UserContext.setUserId(userId);
                    logger.debug("Set userId: {} to UserContext", userId);
                }
            } else {
                logger.warn("Invalid token or jwtUtil is null");
            }
        } else {
            logger.debug("No token found in request");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束时清除用户ID
        UserContext.clear();
        logger.debug("Cleared UserContext");
    }
} 