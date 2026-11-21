package com.olimpiici.arena.web.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zalando.problem.Status;

import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.MailService;
import com.olimpiici.arena.service.UserService;
import com.olimpiici.arena.web.rest.errors.InvalidActivationKeyException;

public class AccountResourceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    private AccountResource accountResource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        accountResource = new AccountResource(userRepository, userService, mailService);
    }

    @Test
    public void testActivateAccount_UnknownKeyThrowsBadRequest() {
        when(userService.activateRegistration("unknown-key")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountResource.activateAccount("unknown-key"))
            .isInstanceOf(InvalidActivationKeyException.class)
            .hasFieldOrPropertyWithValue("status", Status.BAD_REQUEST);
    }
}
