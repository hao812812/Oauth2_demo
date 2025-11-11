package com.example.pic.Repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.pic.Entity.OauthRegisterTb;

@Repository
public interface OauthRegisterRepository extends JpaRepository<OauthRegisterTb, Long> {
    Optional<OauthRegisterTb> findByClientId(String clientId);
}
