package com.example.board.user;

public class TokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;

    public TokenResponse(String accessToken, long expiresInMs) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresInMs / 1000;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType()   { return tokenType; }
    public long   getExpiresIn()   { return expiresIn; }
}
