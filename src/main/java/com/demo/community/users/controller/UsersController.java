package com.demo.community.users.controller;

import com.demo.community.common.dto.ApiResponse;
import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.dto.UsersRequestDTO;
import com.demo.community.users.dto.UsersResponseDTO;
import com.demo.community.users.service.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UsersService usersService;

    // 회원가입 요청 (Create)
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> userCreate(
            @RequestBody @Valid UsersRequestDTO.UserCreateRequest req
            ){

        Long postId = usersService.creatUser(req);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(postId)
                .toUri();

        return ResponseEntity.created(location).body(new ApiResponse<>("user created", Map.of("location", location.toString())));
    }

    // 중복이메일 확인
    @PostMapping("/availability/email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> emailCheck(
            @RequestBody @Valid UsersRequestDTO.EmailCheckRequest req
    ){
        Boolean result = usersService.checkEmail(req);
        String message = result ? "you can use this email" : "email already exists";

        return ResponseEntity.ok(new ApiResponse<>(message, Map.of("availability", result)));
    }

    // 중목닉네임 확인
    @PostMapping("/availability/nickname")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> emailCheck(
            @RequestBody @Valid UsersRequestDTO.NicknameCheckRequest req
    ){
        Boolean result = usersService.checkNickname(req);
        String message = result ? "you can use this nickname" : "nickname already exists";

        return ResponseEntity.ok(new ApiResponse<>(message, Map.of("availability", result)));
    }

    // 프로필 사진 업로드 (임시)
    // 추후에 프로필사진 업로드 프리사인드 S3 url을 반환해주는것으로 변경 (FE는 이 url에 업로드 후, 업로드 위치 URL을 다시 보내줘야 함)
    // 현재는 BE 특정 디렉에 이미지 파일을 저장한 후, 해당 디렉을 URL로 public에 공개하는 방식 사용
    @PostMapping("/image")
    public ResponseEntity<ApiResponse<UsersResponseDTO.UserImageResponse>> getUserImageUrl(
            @RequestPart("img") MultipartFile file
            ) throws IOException {

         if (file.isEmpty()) {
             throw new BadRequestException("no file included");
         }

         String contentType = file.getContentType();
         if (contentType == null || !contentType.startsWith("image/")){
             throw new BadRequestException("not an image format file");
         }

         UsersResponseDTO.UserImageResponse result = usersService.getProfileImageUrl(file);

         return ResponseEntity.ok(new ApiResponse<>("uploaded link provided", result));
    }

    // 유저 회원정보 수정
    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<UsersResponseDTO.UserInfoResponse>> modifyUser(
        @PathVariable("userId") Long userId,
        @RequestBody @Valid UsersRequestDTO.UserUpdateRequest request,
        HttpServletRequest req
    ){
        UsersResponseDTO.UserInfoResponse result = usersService.updateProfile(request, req, userId);

        return ResponseEntity.ok(new ApiResponse<>("user successfully modified", result));
    }

    // 유저 비밀번호 수정
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> modifyPassword(
            @PathVariable("userId") Long userId,
            @RequestBody @Valid UsersRequestDTO.PasswordUpdateRequest request,
            HttpServletRequest req
    ) {
        usersService.modifyPassword(request, req, userId);

        return ResponseEntity.ok(new ApiResponse<>("password successfully modified", null));
    }


    // 유저 상세조회
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UsersResponseDTO.UserInfoResponse>> getUser(
            @PathVariable("userId") Long userId
    ){
        UsersResponseDTO.UserInfoResponse result =usersService.getUser(userId);

        return ResponseEntity.ok(new ApiResponse<>("user info succefully got", result));
    }


    // 유저 간단조회 (필수X)


    // 유저 회원탈퇴
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable("userId") Long userId,
            HttpServletRequest req
    ){
        usersService.deleteUser(userId, req);

        return ResponseEntity.noContent().build();
    }


}
