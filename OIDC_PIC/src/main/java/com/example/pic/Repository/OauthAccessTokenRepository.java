package com.example.pic.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.pic.Entity.OauthAccessTokenTb;

@Repository
public interface OauthAccessTokenRepository extends JpaRepository<OauthAccessTokenTb, Long> {
	Optional<OauthAccessTokenTb> findByAccessToken(String accessToken);
	Optional<OauthAccessTokenTb> findByRefreshToken(String refreshToken);
}
