package com.demo.community;

import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.domain.repository.UserRepository;
import com.demo.community.users.dto.UsersRequestDTO;
import com.demo.community.users.dto.UsersResponseDTO;
import com.demo.community.users.service.UsersService;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsersServiceTest {

	@Autowired
	UsersService usersService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	Long createdUserId;

	@Test
	@DisplayName("회원가입 후 실제 DB에 저장 확인")
	void createUserTest() {
		// given
		UsersRequestDTO.UserCreateRequest req =
				new UsersRequestDTO.UserCreateRequest("test@test.com", "1234", "hong", null);

		// when
		createdUserId = usersService.creatUser(req);

		// then
		Optional<Users> saved = userRepository.findFirstByEmail("test@test.com");
		assertTrue(saved.isPresent());
		assertEquals(createdUserId, saved.get().getId());
		assertTrue(passwordEncoder.matches("1234", saved.get().getPassword()));
	}

	@Test
	@DisplayName("이미 있는 이메일로 회원가입 시도")
	void createUserEmailDuplicateTest(){
		// given
		UsersRequestDTO.UserCreateRequest req =
				new UsersRequestDTO.UserCreateRequest("test@test.com", "1234", "kim", null);

		// when & then
		assertThrows(EntityExistsException.class,
				() -> usersService.creatUser(req));
	}

	@Test
	@DisplayName("이미 있는 닉네임으로 회원가입 시도")
	void createUserNicknameDuplicateTest(){
		// given
		UsersRequestDTO.UserCreateRequest req =
				new UsersRequestDTO.UserCreateRequest("random@test.com", "1234", "hong", null);

		// when & then
		assertThrows(EntityExistsException.class,
				() -> usersService.creatUser(req));
	}

	@Test
	@DisplayName("중복 이메일 확인")
	void checkEmailTest() {
		// when
		Boolean exists = usersService.checkEmail(
				new UsersRequestDTO.EmailCheckRequest("test@test.com"));
		Boolean notExists = usersService.checkEmail(
				new UsersRequestDTO.EmailCheckRequest("random@test.com"));

		// then
		assertFalse(exists);
		assertTrue(notExists);
	}

	@Test
	@DisplayName("중복 이메일 확인")
	void checkNicknameTest() {
		// when
		Boolean exists = usersService.checkNickname(
				new UsersRequestDTO.NicknameCheckRequest("hong"));
		Boolean notExists = usersService.checkNickname(
				new UsersRequestDTO.NicknameCheckRequest("kim"));

		// then
		assertFalse(exists);
		assertTrue(notExists);
	}

	@Test
	@DisplayName("회원정보 조회")
	void getUserTest() {
		// when
		UsersResponseDTO.UserInfoResponse res = usersService.getUser(createdUserId);

		// then
		assertEquals(createdUserId, res.getUserId());
		assertEquals("test@test.com", res.getEmail());
		assertEquals("hong", res.getNickname());
	}

	@Test
	@DisplayName("회원정보 수정 (닉네임만)")
	void updateUserTest() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setAttribute("userId", createdUserId);

		UsersRequestDTO.UserUpdateRequest request = new UsersRequestDTO.UserUpdateRequest("kim", null);

		// when
		usersService.updateProfile(request, req, createdUserId);
		Optional<Users> updated = userRepository.findById(createdUserId);

		// then
		assertTrue(updated.isPresent());
		assertEquals("kim", updated.get().getNickname());
	}

	@Test
	@DisplayName("비밀번호 수정")
	void updatePasswordTest() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setAttribute("userId", createdUserId);

		UsersRequestDTO.PasswordUpdateRequest request = new UsersRequestDTO.PasswordUpdateRequest("1234", "4321");

		// when
		usersService.modifyPassword(request, req, createdUserId);
		Optional<Users> updated = userRepository.findById(createdUserId);

		// then
		assertTrue(updated.isPresent());
		assertTrue(passwordEncoder.matches("4321", updated.get().getPassword()));
	}

}
