package com.example.pic.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.pic.Entity.OauthAuthorizeRequestTb;

@Repository
public interface OauthAuthorizeReqRepository extends JpaRepository<OauthAuthorizeRequestTb, Long> {
    Optional<OauthAuthorizeRequestTb> findByState(String state);
}
