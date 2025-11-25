package com.example.client.Entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_user_token")
public class ClientUserTokenTb {
	@Id
	@Column(name = "session_id")
	private String sessionId;

	/**
	 * 使用者識別碼 (對應 JWT 的 subject)
	 */
	@Column(name = "user_sub", nullable = false, length = 255)
	private String userSub;

	/**
	 * Access Token
	 */
	@Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
	private String accessToken;

	/**
	 * Refresh Token
	 */
	@Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
	private String refreshToken;

	/**
	 * Access Token 過期時間
	 */
	@Column(name = "access_expires_at", nullable = false)
	private Timestamp accessExpiresAt;

	/**
	 * Refresh Token 過期時間
	 */
	@Column(name = "refresh_expires_at")
	private Timestamp refreshExpiresAt;

	/**
	 * 建立時間
	 */
	@Column(name = "created_at", updatable = false)
	private Timestamp createdAt;

	/**
	 * 更新時間
	 */
	@Column(name = "updated_at")
	private Timestamp updatedAt;



	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getUserSub() {
		return userSub;
	}

	public void setUserSub(String userSub) {
		this.userSub = userSub;
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

	public Timestamp getAccessExpiresAt() {
		return accessExpiresAt;
	}

	public void setAccessExpiresAt(Timestamp accessExpiresAt) {
		this.accessExpiresAt = accessExpiresAt;
	}

	public Timestamp getRefreshExpiresAt() {
		return refreshExpiresAt;
	}

	public void setRefreshExpiresAt(Timestamp refreshExpiresAt) {
		this.refreshExpiresAt = refreshExpiresAt;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	

}
