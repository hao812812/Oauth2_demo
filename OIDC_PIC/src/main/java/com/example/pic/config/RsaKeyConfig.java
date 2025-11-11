package com.example.pic.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsaKeyConfig {
	
    @Bean
    public KeyPair keyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);//設定2048位元強度
        return keyPairGenerator.generateKeyPair();//產生金鑰對
    }
    
    @Bean
    public PrivateKey privateKey(KeyPair keyPair) {
        return keyPair.getPrivate(); //取出私鑰
    }
    
    @Bean
    public PublicKey publicKey(KeyPair keyPair) {
        return keyPair.getPublic(); //取出公鑰
    }

}
