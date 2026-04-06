package com.workly.modules.admin;

import com.workly.common.security.JwtUtils;
import com.workly.core.WorklyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;

    private AdminAuthController adminAuthController;

    @BeforeEach
    void setUp() {
        adminAuthController = new AdminAuthController(adminUserRepository, passwordEncoder, jwtUtils);
        ReflectionTestUtils.setField(adminAuthController, "ttlHours", 24);
    }

    private AdminUser adminUser(String username, String hash) {
        AdminUser u = new AdminUser();
        u.setUsername(username);
        u.setPasswordHash(hash);
        return u;
    }

    @Test
    void login_validCredentials_returnsToken() {
        AdminUser u = adminUser("admin", "hashed");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pass", "hashed")).thenReturn(true);
        when(jwtUtils.generateTokenWithRole("admin", "ADMIN")).thenReturn("jwt-token");

        AdminAuthController.AdminLoginRequest req = new AdminAuthController.AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("pass");

        var result = adminAuthController.login(req);

        assertTrue(result.isSuccess());
        assertEquals("jwt-token", result.getData().getToken());
        assertEquals(86400L, result.getData().getExpiresIn());
    }

    @Test
    void login_userNotFound_throws() {
        when(adminUserRepository.findByUsername("bad")).thenReturn(Optional.empty());

        AdminAuthController.AdminLoginRequest req = new AdminAuthController.AdminLoginRequest();
        req.setUsername("bad");
        req.setPassword("pass");

        assertThrows(WorklyException.class, () -> adminAuthController.login(req));
    }

    @Test
    void login_wrongPassword_throws() {
        AdminUser u = adminUser("admin", "hashed");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AdminAuthController.AdminLoginRequest req = new AdminAuthController.AdminLoginRequest();
        req.setUsername("admin");
        req.setPassword("wrong");

        assertThrows(WorklyException.class, () -> adminAuthController.login(req));
    }

    @Test
    void changePassword_validCurrentPassword_updatesHash() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        AdminUser u = adminUser("admin", "oldHash");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        AdminAuthController.ChangePasswordRequest req = new AdminAuthController.ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass");

        var result = adminAuthController.changePassword(req);

        assertTrue(result.isSuccess());
        assertEquals("newHash", u.getPasswordHash());
        verify(adminUserRepository).save(u);
        SecurityContextHolder.clearContext();
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        AdminUser u = adminUser("admin", "oldHash");
        when(adminUserRepository.findByUsername("admin")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

        AdminAuthController.ChangePasswordRequest req = new AdminAuthController.ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("new");

        assertThrows(WorklyException.class, () -> adminAuthController.changePassword(req));
        SecurityContextHolder.clearContext();
    }
}
