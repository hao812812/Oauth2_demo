package com.example.pic.Entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "oauth_access_token")
public class OauthAccessTokenTb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private Timestamp accessToken_expiresAt;
    
    @Column(name = "refresh_token_expires_at", nullable = false)
    private Timestamp refreshToken_expiresAt;

    @Column(name = "scope")
    private String scope;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;



	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}


	public Timestamp getAccessToken_expiresAt() {
		return accessToken_expiresAt;
	}

	public void setAccessToken_expiresAt(Timestamp accessToken_expiresAt) {
		this.accessToken_expiresAt = accessToken_expiresAt;
	}

	public Timestamp getRefreshToken_expiresAt() {
		return refreshToken_expiresAt;
	}

	public void setRefreshToken_expiresAt(Timestamp refreshToken_expiresAt) {
		this.refreshToken_expiresAt = refreshToken_expiresAt;
	}
	
	
    
    

}
