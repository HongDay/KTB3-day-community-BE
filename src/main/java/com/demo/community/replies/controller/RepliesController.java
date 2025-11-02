package com.demo.community.replies.controller;

import com.demo.community.common.dto.ApiResponse;
import com.demo.community.replies.dto.RepliesRequestDTO;
import com.demo.community.replies.dto.RepliesResponseDTO;
import com.demo.community.replies.service.RepliesService;
import com.demo.community.users.dto.UsersResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Validated
@RestController
@RequestMapping("/replies")
@RequiredArgsConstructor
public class RepliesController {

    private final RepliesService repliesService;

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<RepliesResponseDTO.ReplyListSliceResponse>> getRepliesList(
            @PathVariable("postId") Long postId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long lastSeenId
    ){
        RepliesResponseDTO.ReplyListSliceResponse result = repliesService.getReplyList(postId, lastSeenId, size);

        return ResponseEntity.ok(new ApiResponse<>("reply list successfully loaded", result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RepliesResponseDTO.ReplyDetailResponse>> createReply(
            @RequestBody @Valid RepliesRequestDTO.ReplyCreateRequest request,
            HttpServletRequest req
    ){
        RepliesResponseDTO.ReplyDetailResponse result = repliesService.createReply(req, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.getId())
                .toUri();

        return ResponseEntity.created(location).body(new ApiResponse<>("reply created", result));
    }

    @PatchMapping("/{replyId}")
    public ResponseEntity<ApiResponse<RepliesResponseDTO.ReplyDetailResponse>> updateReply(
            @PathVariable("replyId") Long replyId,
            @RequestBody @Valid RepliesRequestDTO.ReplyUpdateRequest request,
            HttpServletRequest req
    ){
        RepliesResponseDTO.ReplyDetailResponse result = repliesService.updateReply(replyId, req, request);

        return ResponseEntity.ok(new ApiResponse<>("reply successfully updated", result));
    }

    @DeleteMapping("/{replyId}")
    public ResponseEntity<ApiResponse<Void>> deleteReply(
            @PathVariable("replyId") Long replyId,
            HttpServletRequest req
    ){
        repliesService.deleteReply(replyId, req);

        return ResponseEntity.noContent().build();
    }

}
