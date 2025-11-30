package com.demo.community;

import com.demo.community.users.domain.enitty.Users;
import com.demo.community.users.domain.repository.UserRepository;
import com.demo.community.users.dto.UsersRequestDTO;
import com.demo.community.users.dto.UsersResponseDTO;
import com.demo.community.users.service.UsersService;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.*;
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

	@Test
	@DisplayName("회원가입 후 실제 DB에 저장 확인")
	void createUserTest() {
		// given
		UsersRequestDTO.UserCreateRequest req =
				new UsersRequestDTO.UserCreateRequest("test@test.com", "1234", "hong", "none");

		// when
		Long id = usersService.creatUser(req);

		// then
		Optional<Users> saved = userRepository.findFirstByEmail("test@test.com");
		assertTrue(saved.isPresent());
		assertEquals(id, saved.get().getId());
		assertTrue(passwordEncoder.matches("1234", saved.get().getPassword()));
	}

	@Test
	@DisplayName("이미 있는 이메일로 회원가입 시도")
	void createUserEmailDuplicateTest(){
		// given
		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		userRepository.save(user);

		UsersRequestDTO.UserCreateRequest req =
				new UsersRequestDTO.UserCreateRequest("test@test.com", "1234", "kim", "none");

		// when & then
		assertThrows(EntityExistsException.class,
				() -> usersService.creatUser(req));
	}

	@Test
	@DisplayName("이미 있는 닉네임으로 회원가입 시도")
	void createUserNicknameDuplicateTest(){
		// given
		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		userRepository.save(user);

		UsersRequestDTO.UserCreateRequest req =
				new UsersRequestDTO.UserCreateRequest("random@test.com", "1234", "hong", "none");

		// when & then
		assertThrows(EntityExistsException.class,
				() -> usersService.creatUser(req));
	}

	@Test
	@DisplayName("중복 이메일 확인")
	void checkEmailTest() {
		// given
		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		userRepository.save(user);

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
	@DisplayName("중복 닉네임 확인")
	void checkNicknameTest() {
		// given
		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		userRepository.save(user);

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
		// given
		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		Users saved = userRepository.save(user);

		// when
		UsersResponseDTO.UserInfoResponse res = usersService.getUser(saved.getId());

		// then
		assertEquals(saved.getId(), res.getUserId());
		assertEquals("test@test.com", res.getEmail());
		assertEquals("hong", res.getNickname());
	}

	@Test
	@DisplayName("회원정보 수정 (닉네임만)")
	void updateUserTest() {
		// given
		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		Users saved = userRepository.save(user);

		// given
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setAttribute("userId", saved.getId());

		UsersRequestDTO.UserUpdateRequest request = new UsersRequestDTO.UserUpdateRequest("kim", null);

		// when
		usersService.updateProfile(request, req, saved.getId());
		Optional<Users> updated = userRepository.findById(saved.getId());

		// then
		assertTrue(updated.isPresent());
		assertEquals("kim", updated.get().getNickname());
	}

	@Test
	@DisplayName("비밀번호 수정")
	void updatePasswordTest() {
		// given
		MockHttpServletRequest req = new MockHttpServletRequest();

		Users user = Users.builder()
				.email("test@test.com")
				.password(passwordEncoder.encode("1234"))
				.nickname("hong")
				.profileImage("none").build();
		Users saved = userRepository.save(user);

		req.setAttribute("userId", saved.getId());

		UsersRequestDTO.PasswordUpdateRequest request = new UsersRequestDTO.PasswordUpdateRequest("1234", "4321");

		// when
		usersService.modifyPassword(request, req, saved.getId());
		Optional<Users> updated = userRepository.findById(saved.getId());

		// then
		assertTrue(updated.isPresent());
		assertTrue(passwordEncoder.matches("4321", updated.get().getPassword()));
	}

}
