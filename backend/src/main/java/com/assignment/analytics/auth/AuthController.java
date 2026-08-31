package com.assignment.analytics.auth;

import com.assignment.analytics.domain.Operator;
import com.assignment.analytics.repo.OperatorRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(OperatorRepository operatorRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, String username, String displayName, String role) {
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Operator operator = operatorRepository.findByUsername(request.username())
                .filter(op -> passwordEncoder.matches(request.password(), op.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        String token = jwtService.issueToken(operator.getOperatorId(), operator.getUsername(),
                operator.getDisplayName(), operator.getRole());
        return new LoginResponse(token, operator.getUsername(), operator.getDisplayName(), operator.getRole());
    }

    @GetMapping("/me")
    public AuthenticatedOperator me(@AuthenticationPrincipal AuthenticatedOperator operator) {
        return operator;
    }
}
