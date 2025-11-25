package com.example.client.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.client.Entity.ClientOauthStateTb;

@Repository
public interface ClientOauthStateRepository extends JpaRepository<ClientOauthStateTb, String> {
  
}
