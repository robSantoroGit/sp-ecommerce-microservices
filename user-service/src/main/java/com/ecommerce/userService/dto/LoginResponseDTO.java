package com.ecommerce.userService.dto;

public class LoginResponseDTO {

    private String token;
    private String type = "Bearer";
    private Long expiresIn;
    private UserResponseDTO user;

    // Constructors
    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String token, Long expiresIn, UserResponseDTO user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public void setUser(UserResponseDTO user) {
        this.user = user;
    }
}