package com.ram.civicdesk.service;

import com.ram.civicdesk.dto.AuthResponse;
import com.ram.civicdesk.dto.LoginRequest;
import com.ram.civicdesk.dto.RegisterRequest;
import com.ram.civicdesk.dto.UserResponse;
import com.ram.civicdesk.entity.Role;
import com.ram.civicdesk.entity.User;
import com.ram.civicdesk.exception.ConflictException;
import com.ram.civicdesk.exception.UnauthorizedException;
import com.ram.civicdesk.repository.UserRepository;
import com.ram.civicdesk.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccessService accessService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AccessService accessService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accessService = accessService;
    }

    /** Public sign-up always creates a CITIZEN. Officers are created by an admin. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (userRepository.existsByPhone(req.phone())) {
            throw new ConflictException("Phone is already registered");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPhone(req.phone());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.CITIZEN);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        JwtService.IssuedToken token = jwtService.issue(saved);
        return new AuthResponse(token.token(), token.expiresAt(), Mappers.user(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("This account has been deactivated");
        }

        User full = accessService.loadActor(user.getId());
        JwtService.IssuedToken token = jwtService.issue(full);
        return new AuthResponse(token.token(), token.expiresAt(), Mappers.user(full));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return Mappers.user(accessService.loadActor(userId));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
