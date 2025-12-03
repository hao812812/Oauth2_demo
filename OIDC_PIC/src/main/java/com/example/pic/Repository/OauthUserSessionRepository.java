package com.example.pic.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.pic.Entity.OauthUserSession;

@Repository
public interface OauthUserSessionRepository extends JpaRepository<OauthUserSession, Long> {
	Optional<OauthUserSession> findBySessionId(String sessionId);
	
	@Modifying
	@Transactional
	Integer deleteBySessionId(String sessionId);
}
