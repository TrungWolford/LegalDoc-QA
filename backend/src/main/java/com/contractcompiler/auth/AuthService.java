package com.contractcompiler.auth;

import com.contractcompiler.auth.dto.LoginRequest;
import com.contractcompiler.auth.dto.LoginResponse;
import com.contractcompiler.auth.dto.RegisterRequest;
import com.contractcompiler.auth.dto.RegisterResponse;
import com.contractcompiler.common.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Authentication service handling login and JWT token generation.
 * Delegates password verification to Spring Security's {@link PasswordEncoder}.
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @throws BadCredentialsException if email or password is invalid
     */
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = jwtService.generateToken(user);
        long expiresIn = appProperties.security().jwt().expirationMs();

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                expiresIn
        );
    }

    /**
     * Registers a new user account with the REVIEWER role.
     * The caller must subsequently login to obtain a JWT token.
     *
     * @throws ResponseStatusException 409 if the email is already taken
     */
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email is already registered.");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        AppUser newUser = new AppUser(
                request.email(),
                hashedPassword,
                request.fullName(),
                Role.REVIEWER
        );
        userRepository.save(newUser);

        return new RegisterResponse(
                newUser.getId(),
                newUser.getEmail(),
                newUser.getFullName(),
                newUser.getRole().name(),
                newUser.getCreatedAt()
        );
    }
}
