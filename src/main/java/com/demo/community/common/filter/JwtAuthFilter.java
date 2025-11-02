package com.demo.community.common.filter;

import com.demo.community.auth.jwt.JwtProvider;
import com.demo.community.common.dto.ApiResponse;
import com.demo.community.common.domain.entity.SessionErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    private record Exclusion(HttpMethod method, String path){}

    private static final List<Exclusion> EXCLUSIONS = List.of(
            new Exclusion(HttpMethod.POST, "/auth"),
            new Exclusion(HttpMethod.GET, "/posts"),
            new Exclusion(HttpMethod.GET, "/posts/*"),
            new Exclusion(HttpMethod.GET, "/replies/*"),
            new Exclusion(HttpMethod.POST, "/users"),
            new Exclusion(HttpMethod.POST, "/users/availability/*"),
            new Exclusion(HttpMethod.POST, "/users/image")
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return EXCLUSIONS.stream().anyMatch(e ->
                e.method.name().equalsIgnoreCase(method) && new AntPathMatcher().match(e.path, path)
        );
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws IOException, ServletException {

        Optional<String> accessToken = extractTokenFromHeader(request);

        // 액세스 토큰이 헤더에 없을 경우
        if (accessToken.isEmpty()){
            // 헤더에 액세스 토큰을 포함시키세요
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, SessionErrorCode.AUTH_NO_AT, "no access token in header");
            return;
        }

        // 만료된 액세스 토큰일 때
        if (!validateAndSetAttributes(accessToken.get(), request)){
            // 액세스 토큰이 만료되었습니다. 리프레시 토큰으로 재발급 받으세요.
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, SessionErrorCode.AUTH_AT_EXPIRED, "access token expired, get a new token");
            return;
        }

        chain.doFilter(request, response);
    }

    // 헤더에서 토큰 추출 (Access Token)
    private Optional<String> extractTokenFromHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7));
    }


    private boolean validateAndSetAttributes(String token, HttpServletRequest request) {
        try {
            var jws = jwtProvider.parse(token);
            Claims body = jws.getBody();
            request.setAttribute("userId", Long.valueOf(body.getSubject()));
            //request.setAttribute("role", body.get("role"));
            return true;
        } catch (Exception exception) {
            return false;
        }
    }


    private void writeError(HttpServletResponse response, int status, SessionErrorCode code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        ApiResponse<Void> body = new ApiResponse<>(message, code, null);

        new ObjectMapper().writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }

}
