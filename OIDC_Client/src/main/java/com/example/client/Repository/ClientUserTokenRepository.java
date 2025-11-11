package com.example.client.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.client.Entity.ClientOauthStateTb;
import com.example.client.Entity.ClientUserTokenTb;

public interface ClientUserTokenRepository extends JpaRepository<ClientUserTokenTb, String>{

	
}
