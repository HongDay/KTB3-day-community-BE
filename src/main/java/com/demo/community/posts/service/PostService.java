package com.demo.community.posts.service;

import com.demo.community.common.dto.ApiResponse;
import com.demo.community.likes.domain.repository.LikesPostsRepository;
import com.demo.community.posts.domain.entity.*;
import com.demo.community.posts.domain.repository.PostRepository;
import com.demo.community.posts.domain.repository.PostViewCountsRepository;
import com.demo.community.posts.domain.repository.PostsCountsRepository;
import com.demo.community.posts.domain.repository.PostsImageRepository;
import com.demo.community.posts.dto.PostRequestDTO;
import com.demo.community.posts.dto.PostResponseDTO;
import com.demo.community.users.domain.enitty.QUsers;
import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.domain.repository.UserRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostsCountsRepository postsCountsRepository;
    private final PostViewCountsRepository postViewCountsRepository;
    private final PostsImageRepository postsImageRepository;
    private final LikesPostsRepository likesPostsRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Transactional
    public PostResponseDTO.PostCreateResponse createPost(PostRequestDTO.PostCreateRequest request, HttpServletRequest req) {

//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");

        Users user = userRepository.findById(userId)
                // 이 예외는 나중에 커스텀 에외 (실패코드, 메세지를 응답으로 반환하는)로 변경 예정
                .orElseThrow(() -> new EntityNotFoundException("user not found"));

        Boolean includeImage = request.getImageUrl()!=null;

        Posts post = Posts.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .includeImage(includeImage).build();
        if (includeImage) {post.addImages(request.getImageUrl());}

        PostsCounts postsCounts = PostsCounts.builder().posts(post).build();
        PostViewCounts postViewCounts = PostViewCounts.builder().posts(post).build();

        postRepository.save(post);
        postsCountsRepository.save(postsCounts);
        postViewCountsRepository.save(postViewCounts);

        return PostResponseDTO.PostCreateResponse.builder().postId(post.getId()).build();
    }

    @Transactional
    public void deletePost(Long postId, HttpServletRequest req){

        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("post not found"));

        // 인가
//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");
        if (!post.getUser().getId().equals(userId)){
            throw new EntityNotFoundException("delete forbidden user");
        }

        postViewCountsRepository.deleteById(postId);
        postsCountsRepository.deleteById(postId);
        postsImageRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional
    public PostResponseDTO.PostDetailResponse detailPost(Long postId, HttpServletRequest req){

//        HttpSession session = req.getSession(false);
//        Long userId = -1L;
//        if (session != null) {userId = (Long) session.getAttribute("USER_ID");}
        Long userId = (Long) req.getAttribute("userId");

        QPosts p = QPosts.posts;
        QUsers u = QUsers.users;
        QPostsImages pi = QPostsImages.postsImages;
        QPostsCounts pc = QPostsCounts.postsCounts;
        QPostViewCounts pv = QPostViewCounts.postViewCounts;
        //QLike l = QLike.like;

        // likePressed: exists 서브쿼리
//        BooleanExpression likeExists = JPAExpressions
//                .selectOne()
//                .from(l)
//                .where(l.posts.id.eq(p.id),
//                        l.user.id.eq(currentUserId))
//                .exists();

        List<Tuple> rows = jpaQueryFactory
                .select(
                        p.id,
                        p.title,
                        p.content,
                        u.nickname,
                        u.profileImage,
                        u.id,
                        pc.likeCounts,
                        pv.viewCounts,
                        pc.replyCounts,
                        pi.imageUrl
                        //,likeExists
                )
                .from(p)
                .leftJoin(p.user, u)
                .leftJoin(pc).on(pc.posts.id.eq(p.id))
                .leftJoin(pv).on(pv.posts.id.eq(p.id))
                .leftJoin(pi).on(pi.posts.id.eq(p.id))
                .where(p.id.eq(postId))
                .fetch();

        if(rows.isEmpty()){
            // 조회값이 비어있을 때 (존재하지 않는 postId일 때) 오류 반환하는 코드 필요함.
        }

        List<String> imageUrls = rows.stream()
                .map(t -> t.get(pi.imageUrl))
                .filter(Objects::nonNull)              // null 제외 (LEFT JOIN일 경우)
                .toList();

        Tuple t = rows.getFirst();

        boolean likepressed = likesPostsRepository.existsByUsersIdAndPostsId(userId, postId);

        // 이 값이 null 일 때 (존재하지 않는 postId일 때) 오류 반환하는 코드 필요함.
        return PostResponseDTO.PostDetailResponse.builder()
                .postId(t.get(p.id))
                .title(t.get(p.title))
                .content(t.get(p.content))
                .writer(t.get(u.nickname))
                .writerImage(t.get(u.profileImage))
                .count(PostResponseDTO.Count.builder()
                        .like(t.get(pc.likeCounts))
                        .reply(t.get(pc.replyCounts))
                        .visit(t.get(pv.viewCounts)).build())
                .likePressed(likepressed)
                .authorization(Objects.equals(t.get(u.id), userId))
                .images(imageUrls)
                .build();
    }

    @Transactional
    public PostResponseDTO.PostUpdateResponse updatePost(PostRequestDTO.PostUpdateRequest request, Long postId, HttpServletRequest req) {
        Posts post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("post not found"));

        // 인가
//        HttpSession session = req.getSession(false);
//        Long userId = (Long) session.getAttribute("USER_ID");
        Long userId = (Long) req.getAttribute("userId");
        if (!post.getUser().getId().equals(userId)){
            throw new EntityNotFoundException("delete forbidden user");
        }

        post.updatePost(request.getTitle(), request.getContent(), request.getImageUrl());

        postRepository.flush();

        return PostResponseDTO.PostUpdateResponse.builder()
                .postId(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .postImages(post.getImages())
                .createdAt(post.getCreatedAt())
                .modifiedAt(post.getUpdatedAt())
                .userId(post.getUser().getId())
                .nickname(post.getUser().getNickname()) // lazy loading 될듯 여기서
                .build();
    }

    @Transactional(readOnly = true)
    public PostResponseDTO.PostListSliceResponse getListPost(Long cursorId, int size) {

        QPosts p = QPosts.posts;
        QUsers u = QUsers.users;
        QPostsCounts pc = QPostsCounts.postsCounts;
        QPostViewCounts pv = QPostViewCounts.postViewCounts;

        List<PostResponseDTO.PostListResponse> posts = jpaQueryFactory
                    .select(Projections.constructor(PostResponseDTO.PostListResponse.class,
                            p.id, p.title,
                            u.nickname, u.profileImage,
                            pc.likeCounts, pc.replyCounts, pv.viewCounts,
                            p.createdAt
                    ))
                    .from(p)
                    .leftJoin(p.user, u)
                    .leftJoin(pc).on(pc.posts.eq(p))
                    .leftJoin(pv).on(pv.posts.eq(p))
                    .where(cursorId != null ? p.id.lt(cursorId) : null)
                    .orderBy(p.id.desc())
                    .limit(size + 1)
                    .fetch();


        boolean hasNext = posts.size() > size;
        if (hasNext){
            posts = posts.subList(0, size);
        }

        Long nextCursor = posts.isEmpty() ? null : posts.getLast().getPostId();

        PostResponseDTO.PostListSliceResponse body = PostResponseDTO.PostListSliceResponse.builder()
                .items(posts)
                .hasNext(hasNext)
                .nextCursorId(nextCursor).build();

        return body;
    }


}
