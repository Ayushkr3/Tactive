package com.job.scheduler.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.job.scheduler.dto.LoginRequest;
import com.job.scheduler.dto.RegisterRequest;
import com.job.scheduler.entity.User;
import com.job.scheduler.repository.ProjectRepository;
import com.job.scheduler.repository.UserRepository;
import com.job.scheduler.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository users;
    @Mock ProjectRepository projects;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtil jwt;
    private UserService service;

    @BeforeEach
    void setUp() { service = new UserService(users, projects, encoder, jwt); }

    @Test
    void registerHashesPasswordAndReturnsAuthDetails() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Ada"); request.setEmail("ada@example.com"); request.setPassword("secret123");
        User saved = new User(); saved.setId(7L); saved.setName("Ada"); saved.setEmail("ada@example.com");
        when(users.existsByEmail("ada@example.com")).thenReturn(false);
        when(encoder.encode("secret123")).thenReturn("hash");
        when(users.save(any(User.class))).thenReturn(saved);
        when(jwt.generateToken(7L, "ada@example.com")).thenReturn("token");

        var response = service.register(request);

        assertEquals("token", response.getToken());
        assertEquals(7L, response.getUserId());
        verify(encoder).encode("secret123");
        verify(users).save(argThat(u -> "hash".equals(u.getPasswordHash())));
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("ada@example.com");
        when(users.existsByEmail("ada@example.com")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.register(request));
        verify(users, never()).save(any());
    }

    @Test
    void loginRejectsWrongPassword() {
        LoginRequest request = new LoginRequest(); request.setEmail("ada@example.com"); request.setPassword("bad");
        User user = new User(); user.setPasswordHash("hash");
        when(users.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.login(request));
        verifyNoInteractions(jwt);
    }
}
