package com.example.security;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.example.entity.User;
import com.example.service.TokenBlacklistService;
import com.example.service.UserService;
import com.example.utils.TokenUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private UserService userService;

    @Resource
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (StrUtil.isNotBlank(token)
                    && !tokenBlacklistService.isBlacklisted(token)
                    && TokenUtils.verify(token)) {
                String audience = JWT.decode(token).getAudience().get(0);
                String[] split = audience.split("-");
                Integer userId = Integer.valueOf(split[0]);
                User user = userService.getById(userId);

                if (user != null && !"DISABLED".equals(user.getStatus())) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    Collections.singletonList(
                                            new SimpleGrantedAuthority("ROLE_" + user.getRole())
                                    )
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    com.example.common.CurrentUserContext.set(user);
                }
            }
        } catch (Exception e) {
            logger.warn("JWT 认证失败: " + request.getRequestURI());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            com.example.common.CurrentUserContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StrUtil.isBlank(token)) {
            token = request.getHeader("Authorization");
            if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }
        return token;
    }
}
