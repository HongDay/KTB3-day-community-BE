package com.demo.community.auth.service;

import com.demo.community.auth.dto.AuthResponseDTO;
import com.demo.community.auth.jwt.JwtProvider;
import com.demo.community.common.dto.ApiResponse;
import com.demo.community.common.filter.JwtAuthFilter;
import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    private static final int ACCESS_TOKEN_EXPIRATION = 5 * 60;
    private static final int REFRESH_TOKEN_EXPIRATION = 14 * 24 * 3600;

    @Transactional
    public AuthResponseDTO.LoginResponse verifyUser(
            String email, String password, HttpServletRequest request, HttpServletResponse response)
    {
        Optional<Users> user = userRepository.findFirstByEmail(email);
        if(user.isEmpty()){
            // exception 으로 바로 오류코드와 함께 응답 반환하게 변경
            return null;
        }

        if (!passwordEncoder.matches(password, user.get().getPassword())){
            // exception 으로 바로 오류코드와 함께 응답 반환하게 변경
            return null;
        }

        // 세션 생성
        // HttpSession session = request.getSession(true);
        // session.setAttribute("USER_ID", user.get().getId());

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(user.get().getId(), user.get().getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.get().getId());

        addTokenCookie(response, "refreshToken", refreshToken, REFRESH_TOKEN_EXPIRATION);

        return AuthResponseDTO.LoginResponse.builder()
                .userId(user.get().getId())
                .email(user.get().getEmail())
                .nickname(user.get().getNickname())
                .token(accessToken)
                .profileImage(user.get().getProfileImage()).build();
    }

    @Transactional
    public void logoutUser(HttpServletRequest request, HttpServletResponse response){
        // HttpSession session = request.getSession(false);
        // if (session != null) session.invalidate();
        addTokenCookie(response, "refreshToken", null, 0);
    }

    @Transactional
    public String refreshToken(HttpServletRequest request, HttpServletResponse response){
        Optional<String> refreshToken = extractTokenFromCookie(request);

        if (refreshToken.isEmpty()) {
            // 쿠키에 리프레시 토큰을 추가하세요
            return "AUTH_NO_RT";
        }

        var parsedRefreshToken = jwtProvider.parse(refreshToken.get());

        if (parsedRefreshToken.getBody().getExpiration().before(new Date())){
            // 만료된 리프레시 토큰입니다. 재로그인 하세요
            return "AUTH_RT_EXPIRED";
        }

        Long userId = Long.valueOf(parsedRefreshToken.getBody().getSubject());
        Users user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return null;
        }

        return jwtProvider.createAccessToken(userId, user.getEmail());
    }

    // 쿠키에서 토큰 추출 (Refresh Token)
    private Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        return Optional.ofNullable(request.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .filter(cookie -> "accessToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

}
