package com.budgetmanager.backend.service;

import com.budgetmanager.backend.dto.AuthResponse;

import com.budgetmanager.backend.dto.LoginRequest;
import com.budgetmanager.backend.dto.RegisterRequest;
import com.budgetmanager.backend.jwt.JwtService;
import com.budgetmanager.backend.model.User;
import com.budgetmanager.backend.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Add this import
import org.springframework.security.core.context.SecurityContextHolder; // Add this import
import com.budgetmanager.backend.dto.ChangePasswordRequest; // Add this import

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtService jwtService,
                         AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }
        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail()) // Ensure email is passed
                .village(request.getVillage())
                .phoneNumber(request.getPhoneNumber())
                .budget(0)
                .build();
        userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder().token(jwtToken).build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder().token(jwtToken).build();
    }
    public void changePassword(ChangePasswordRequest request) {
        // 1. Get the currently authenticated user
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new IllegalStateException("User not authenticated");
        }
        
        User user = (User) authentication.getPrincipal();

        // 2. Check if the current password is correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Wrong current password");
        }

        // 3. Check if the new passwords match (basic check, frontend should also do this)
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // 4. Encode the new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // 5. Save the updated user
        userRepository.save(user);

        // Optional: Re-authenticate the user if needed, though typically not required
        // var newAuth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        // SecurityContextHolder.getContext().setAuthentication(newAuth);

        System.out.println("Password changed successfully for user: " + user.getUsername()); // Log success
    }
}