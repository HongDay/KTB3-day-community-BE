package com.demo.community.common.filter;

import com.demo.community.common.dto.ApiResponse;
import com.demo.community.common.domain.entity.SessionErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 스프링 빈 필터로 구현 (시큐리티필터체인에 추가예정)
@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {

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
            ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Cookie[] cookies = request.getCookies();

        // 쿠키가 없는 경우
        if (cookies == null || cookies.length == 0){
            // 쿠키를 포함시키세요
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, SessionErrorCode.AUTH_NO_COOKIE, "cookie is missing");
            return;
        }

        // 쿠키는 왔는데 인증 정보 (세션ID) 없을 경우
        for (Cookie c : cookies) {
            if ("JSESSIONID".equals(c.getName())){
                // 쿠키에 세션을 포함시키세요
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, SessionErrorCode.AUTH_NO_SID, "no SID in cookie");
                return;
            }
        }

        // 인증정보가 잘못된 경우 (세션ID가 존재하지 않는 세션ID)
        String requestedSid = request.getRequestedSessionId();
        boolean sidValid = request.isRequestedSessionIdValid();
        if (requestedSid != null && !sidValid){
            // 즉시 세션 삭제 (로그아웃)
            // 토큰이라면, 토큰 재사용이 감지되었을 때, 해당 사용자의 AT / RT 를 모두 즉시 무효화
            Cookie c = new Cookie("JSESSIONID", null);
            c.setPath("/");
            c.setMaxAge(0);
            c.setHttpOnly(true);

            response.addCookie(c);

            // 비정상 접근. 유효하지 않은 세션 ID입니다. 리다이렉트하세요
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, SessionErrorCode.AUTH_INVALID, "inavliad access, redirect to login");
            return;
        }

        // 세션이 만료된 세션일 때
        if (requestedSid != null && session == null){
            // 세션이 만료되었습니다. 리다이렉트하세요
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, SessionErrorCode.AUTH_EXPIRED, "session expired, redirect to login");
            return;
            // 혹은 추후에 세션 자동 업데이트 (재발급) 로직 추가 (리프레시 토큰처럼)
        }

        chain.doFilter(request, response);
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
