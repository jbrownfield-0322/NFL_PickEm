package com.nflpickem.pickem.dto;

import com.nflpickem.pickem.model.User;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
    }
}
