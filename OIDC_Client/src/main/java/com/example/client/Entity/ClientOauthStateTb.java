package com.example.client.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client_oauth_state")
public class ClientOauthStateTb {
	
    @Id
    private String state;
    
    private String code_verifier;

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCode_verifier() {
		return code_verifier;
	}

	public void setCode_verifier(String code_verifier) {
		this.code_verifier = code_verifier;
	}
    
    
}
