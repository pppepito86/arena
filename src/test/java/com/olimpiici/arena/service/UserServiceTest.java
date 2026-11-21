package com.olimpiici.arena.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.AuthorityRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.UserDTO;
import com.olimpiici.arena.web.rest.errors.LoginAlreadyUsedException;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private UserService userService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(cacheManager.getCache(any())).thenReturn(cache);
        userService = new UserService(userRepository, passwordEncoder, authorityRepository, cacheManager);
    }

    @Test
    public void testRegisterUser_DoesNotDeleteAdminDeactivatedUser() {
        User deactivated = new User();
        deactivated.setId(42L);
        deactivated.setLogin("banneduser");
        deactivated.setEmail("banned@example.com");
        deactivated.setActivated(false);
        deactivated.setActivationKey(null);

        UserDTO dto = new UserDTO();
        dto.setLogin("banneduser");
        dto.setEmail("new@example.com");

        when(userRepository.findOneByLogin("banneduser")).thenReturn(Optional.of(deactivated));

        assertThatThrownBy(() -> userService.registerUser(dto, "secret123"))
            .isInstanceOf(LoginAlreadyUsedException.class);

        verify(userRepository, never()).delete(any());
    }

    @Test
    public void testRegisterUser_DoesNotDeleteNonRemovableUnactivatedUser() {
        User withSubmissions = new User();
        withSubmissions.setId(43L);
        withSubmissions.setLogin("emailchanger");
        withSubmissions.setEmail("old@example.com");
        withSubmissions.setActivated(false);
        withSubmissions.setActivationKey("some-key");

        UserDTO dto = new UserDTO();
        dto.setLogin("emailchanger");
        dto.setEmail("other@example.com");

        when(userRepository.findOneByLogin("emailchanger")).thenReturn(Optional.of(withSubmissions));
        when(userRepository.isRemovableNotActivatedUser(43L)).thenReturn(false);

        assertThatThrownBy(() -> userService.registerUser(dto, "secret123"))
            .isInstanceOf(LoginAlreadyUsedException.class);

        verify(userRepository, never()).delete(any());
    }

    @Test
    public void testRemoveNotActivatedUsers_DeletesOnlyRemovableCandidates() {
        User removable = new User();
        removable.setId(99L);
        removable.setLogin("abandoned");
        removable.setEmail("abandoned@example.com");

        when(userRepository.findAllRemovableNotActivatedUsers(any(Instant.class)))
            .thenReturn(Collections.singletonList(removable));

        userService.removeNotActivatedUsers();

        verify(userRepository).delete(removable);
    }
}
