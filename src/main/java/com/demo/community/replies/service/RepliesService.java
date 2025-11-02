package com.demo.community.replies.service;

import com.demo.community.posts.domain.entity.Posts;
import com.demo.community.posts.domain.repository.PostRepository;
import com.demo.community.replies.domain.entity.Replies;
import com.demo.community.replies.domain.repository.RepliesRepository;
import com.demo.community.replies.dto.RepliesRequestDTO;
import com.demo.community.replies.dto.RepliesResponseDTO;
import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RepliesService {

    private final RepliesRepository repliesRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public RepliesResponseDTO.ReplyListSliceResponse getReplyList(Long postId, Long lastSeenId, int size){

        List<Replies> replies = repliesRepository.findSliceByPostId(postId, lastSeenId, PageRequest.of(0, size + 1));

        boolean hasNext = replies.size() > size;
        if (hasNext){
            replies = replies.subList(0, size);
        }

        Long nextCursor = replies.isEmpty() ? null : replies.getLast().getId();

        List<RepliesResponseDTO.ReplyDetailResponse> result = replies.stream().map(
                reply -> RepliesResponseDTO.ReplyDetailResponse.builder()
                        .id(reply.getId())
                        .userId(reply.getUsers().getId())
                        .content(reply.getContent())
                        .createdAt(reply.getCreatedAt())
                        .nickname(reply.getUsers().getNickname())
                        .profileImg(reply.getUsers().getProfileImage())
                        .build()).toList();

        return new RepliesResponseDTO.ReplyListSliceResponse(result, hasNext, nextCursor);
    }

    @Transactional
    public RepliesResponseDTO.ReplyDetailResponse createReply(HttpServletRequest req, RepliesRequestDTO.ReplyCreateRequest request){

//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");

        Optional<Users> user = userRepository.findById(userId);
        if(user.isEmpty()){throw new EntityNotFoundException("user not found");}

        Optional<Posts> post = postRepository.findById(request.getPostId());
        if(post.isEmpty()){throw new EntityNotFoundException("post not found");}

        Replies reply = Replies.builder()
                .posts(post.get())
                .content(request.getContent())
                .users(user.get()).build();

        repliesRepository.save(reply);
        repliesRepository.flush();

        return RepliesResponseDTO.ReplyDetailResponse.builder()
                .id(reply.getId())
                .userId(reply.getUsers().getId())
                .createdAt(reply.getCreatedAt())
                .nickname(reply.getUsers().getNickname())
                .profileImg(reply.getUsers().getProfileImage())
                .content(reply.getContent()).build();
    }

    @Transactional
    public RepliesResponseDTO.ReplyDetailResponse updateReply(Long replyId, HttpServletRequest req, RepliesRequestDTO.ReplyUpdateRequest request){

        Optional<Replies> reply = repliesRepository.findById(replyId);
        if(reply.isEmpty()){throw new EntityNotFoundException("post not found");}
        Replies gotReply = reply.get();

        // 인가
//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");
        if (!Objects.equals(userId, gotReply.getUsers().getId())){
            throw new AccessDeniedException("forbidden user (not a writer)");
        }

        gotReply.setContent(request.getContent());
        repliesRepository.flush();

        return RepliesResponseDTO.ReplyDetailResponse.builder()
                .id(gotReply.getId())
                .content(gotReply.getContent())
                .createdAt(gotReply.getCreatedAt())
                .updatedAt(gotReply.getUpdatedAt())
                .userId(gotReply.getUsers().getId())
                .nickname(gotReply.getUsers().getNickname())
                .profileImg(gotReply.getUsers().getProfileImage()).build();
    }

    @Transactional
    public void deleteReply(Long replyId, HttpServletRequest req){

        Optional<Replies> reply = repliesRepository.findById(replyId);
        if(reply.isEmpty()){throw new EntityNotFoundException("post not found");}

        // 인가
//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");
        if (!reply.get().getUsers().getId().equals(userId)){
            throw new EntityNotFoundException("delete forbidden user");
        }

        repliesRepository.deleteById(replyId);
    }

}
