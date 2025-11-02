package com.demo.community.likes.controller;

import com.demo.community.common.dto.ApiResponse;
import com.demo.community.likes.dto.LikesPostsResponseDTO;
import com.demo.community.likes.service.LikesPostsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Controller
@RequestMapping("/likes/posts")
@RequiredArgsConstructor
public class LikesPostsController {

    private final LikesPostsService likesPostsService;

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<LikesPostsResponseDTO.LikesPostsResultResponse>> createLike(
            @PathVariable("postId") Long postId,
            HttpServletRequest req
    ){
        LikesPostsResponseDTO.LikesPostsResultResponse result = likesPostsService.likeCreate(postId, req);

        return ResponseEntity.ok(new ApiResponse<>("like added", result));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<LikesPostsResponseDTO.LikesPostsResultResponse>> DeleteLike(
            @PathVariable("postId") Long postId,
            HttpServletRequest req
    ){
        LikesPostsResponseDTO.LikesPostsResultResponse result = likesPostsService.likeDelete(postId, req);

        return ResponseEntity.ok(new ApiResponse<>("like deleted", result));
    }
}
