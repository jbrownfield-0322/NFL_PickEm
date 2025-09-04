package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.ErrorResponse;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserController(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @PutMapping("/{userId}/updateName")
    public ResponseEntity<Object> updateName(@PathVariable Long userId, @RequestBody UpdateNameRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }
            
            user.setName(request.getName().trim());
            User updatedUser = userRepository.save(user);
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now().toEpochMilli());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{userId}/updateUsername")
    public ResponseEntity<Object> updateUsername(@PathVariable Long userId, @RequestBody UpdateUsernameRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            
            // Check if username is already taken by another user
            User existingUser = userRepository.findByUsername(request.getUsername().trim());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw new IllegalArgumentException("Username already taken");
            }
            
            user.setUsername(request.getUsername().trim());
            User updatedUser = userRepository.save(user);
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now().toEpochMilli());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{userId}/updatePassword")
    public ResponseEntity<Object> updatePassword(@PathVariable Long userId, @RequestBody UpdatePasswordRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("Password cannot be empty");
            }
            
            user.setPassword(bCryptPasswordEncoder.encode(request.getNewPassword()));
            User updatedUser = userRepository.save(user);
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now().toEpochMilli());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    // DTOs for the requests
    public static class UpdateNameRequest {
        private String name;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class UpdateUsernameRequest {
        private String username;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    public static class UpdatePasswordRequest {
        private String newPassword;
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
