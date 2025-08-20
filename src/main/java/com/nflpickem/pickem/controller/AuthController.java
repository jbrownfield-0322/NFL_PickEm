package com.nflpickem.pickem.controller;

import com.nflpickem.pickem.dto.LoginRequest;
import com.nflpickem.pickem.dto.RegisterRequest;
import com.nflpickem.pickem.dto.UserResponse;
import com.nflpickem.pickem.model.User;
import com.nflpickem.pickem.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nflpickem.pickem.dto.ErrorResponse; // Import ErrorResponse
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody RegisterRequest request) {
        try {
            User registeredUser = authService.registerUser(request.getUsername(), request.getPassword());
            // Return only safe user information
            UserResponse userResponse = new UserResponse(registeredUser);
            return new ResponseEntity<>(userResponse, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value(), Instant.now().toEpochMilli());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.loginUser(request.getUsername(), request.getPassword());
            // Return only safe user information
            UserResponse userResponse = new UserResponse(user);
            return new ResponseEntity<>(userResponse, HttpStatus.OK);
        } catch (RuntimeException e) {
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage(), HttpStatus.UNAUTHORIZED.value(), Instant.now().toEpochMilli());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }
} 