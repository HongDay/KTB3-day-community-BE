package com.demo.community.auth.controller;

import com.demo.community.auth.dto.AuthRequestDTO;
import com.demo.community.auth.dto.AuthResponseDTO;
import com.demo.community.auth.service.AuthService;
import com.demo.community.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<?>> login(
            @RequestBody @Valid AuthRequestDTO.LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response
            ){
        // 아이디, 비밀번호 검증 로직
        AuthResponseDTO.LoginResponse user = authService.verifyUser(req.getEmail(), req.getPassword(), request, response);
        if (user == null){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("login failed", null));}

        return ResponseEntity.ok(new ApiResponse<>("login success", user));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        authService.logoutUser(request, response);
        return ResponseEntity.ok(new ApiResponse<>("logout success", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<String>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String accessToken = authService.refreshToken(request, response);

        if (Objects.equals(accessToken, "AUTH_NO_RT")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("include refresh token in cookie", accessToken));
        } else if (Objects.equals(accessToken, "AUTH_RT_EXPIRED")){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("refresh token expired, login again", accessToken));
        } else if (accessToken == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse<>("invalid token, user not found", null));
        } else {
            return ResponseEntity.ok(new ApiResponse<>("refresh success", accessToken));
        }

    }

}
