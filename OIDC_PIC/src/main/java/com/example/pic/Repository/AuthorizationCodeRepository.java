package com.example.pic.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.pic.Entity.OauthAuthorizationCodeTb;
@Repository
public interface AuthorizationCodeRepository extends JpaRepository<OauthAuthorizationCodeTb, String> {
    Optional<OauthAuthorizationCodeTb> findByCode(String code);
}
