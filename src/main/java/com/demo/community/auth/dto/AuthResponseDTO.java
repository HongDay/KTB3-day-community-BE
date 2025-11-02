package com.demo.community.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

public class AuthResponseDTO {

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class LoginResponse{
        @NotNull
        private Long userId;
        @NotNull
        private String email;
        @NotNull
        private String nickname;
        @NotNull
        private String profileImage;
        private String token;
    }
}
