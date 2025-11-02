package com.demo.community.likes.service;

import com.demo.community.likes.domain.entity.LikesPosts;
import com.demo.community.likes.domain.repository.LikesPostsRepository;
import com.demo.community.likes.dto.LikesPostsResponseDTO;
import com.demo.community.posts.domain.entity.Posts;
import com.demo.community.posts.domain.repository.PostRepository;
import com.demo.community.posts.domain.repository.PostsCountsRepository;
import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LikesPostsService {

    private final LikesPostsRepository likesPostsRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostsCountsRepository postsCountsRepository;

    @Transactional
    public LikesPostsResponseDTO.LikesPostsResultResponse likeCreate(Long postId, HttpServletRequest req){

//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");

        boolean nowPressed = likesPostsRepository.existsByUsersIdAndPostsId(userId, postId);
        if (nowPressed){
            throw new AccessDeniedException("this user already pressed this post. request likeDelete instead of likeCreate");
        }

        Optional<Users> user = userRepository.findById(userId);
        if(user.isEmpty()){throw new EntityNotFoundException("user not found");}

        Optional<Posts> post = postRepository.findById(postId);
        if(post.isEmpty()){throw new EntityNotFoundException("post not found");}

        LikesPosts likesPosts = LikesPosts.builder().posts(post.get()).users(user.get()).build();
        likesPostsRepository.save(likesPosts);

        // 원자적으로 post의 likeCount를 +1 하는 SQL
        postsCountsRepository.incrementLikeCount(postId);

        int likeCount = postsCountsRepository.getLikeCount(postId);

        return LikesPostsResponseDTO.LikesPostsResultResponse.builder()
                .likeCount(likeCount).userPressed(true).build();
    }

    @Transactional
    public LikesPostsResponseDTO.LikesPostsResultResponse likeDelete(Long postId, HttpServletRequest req){

//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");

        boolean nowPressed = likesPostsRepository.existsByUsersIdAndPostsId(userId, postId);
        if (!nowPressed){
            throw new AccessDeniedException("this user didn't pressed this post. request likeCreate instead of likeDelete");
        }

        Optional<Users> user = userRepository.findById(userId);
        if(user.isEmpty()){throw new EntityNotFoundException("user not found");}

        Optional<Posts> post = postRepository.findById(postId);
        if(post.isEmpty()){throw new EntityNotFoundException("post not found");}

        likesPostsRepository.deleteByUsersIdAndPostsId(userId, postId);

        // 원자적으로 post의 likeCount를 -1 하는 SQL
        postsCountsRepository.decrementLikeCount(postId);

        int likeCount = postsCountsRepository.getLikeCount(postId);

        return LikesPostsResponseDTO.LikesPostsResultResponse.builder()
                .likeCount(likeCount).userPressed(false).build();
    }

}
