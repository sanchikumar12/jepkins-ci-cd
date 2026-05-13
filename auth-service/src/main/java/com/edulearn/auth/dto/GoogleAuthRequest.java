package com.edulearn.auth.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String token;
    private String role;
}
