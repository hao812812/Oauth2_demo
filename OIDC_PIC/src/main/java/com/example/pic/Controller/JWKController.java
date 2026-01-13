package com.example.pic.Controller;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 把公鑰打包成json格式
 */
@RestController
@RequestMapping("/.well-known")
public class JWKController {
	@Autowired
	private PublicKey publicKey;

	/**
	 * JWKS 端點 - 提供公鑰給 A平台 A平台會自動從這個端點下載公鑰
	 */
	@GetMapping("/jwks.json")
	public Map<String, Object> getJwks() {
		try {
			RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

			// 建立 JWK (JSON Web Key)
			Map<String, Object> jwk = new HashMap<>();
			jwk.put("kty", "RSA");// key type
			jwk.put("use", "sig");// key用途 :sign
			jwk.put("alg", "RS256");// 加密算法
			jwk.put("kid", "key-1"); // Key ID

			// 將 RSA 公鑰轉為 Base64 URL 編碼
			jwk.put("n", base64UrlEncode(rsaPublicKey.getModulus().toByteArray()));// modulus
			jwk.put("e", base64UrlEncode(rsaPublicKey.getPublicExponent().toByteArray()));// exponent

			return Map.of("keys", List.of(jwk));

		} catch (Exception e) {
			throw new RuntimeException("生成 JWKS 失敗", e);
		}
	};

	/**
	 * configuration json文件
	 */
	@GetMapping("/openid-configuration")
	public Map<String, Object> openidConfiguration() {
		String issuer = "https://oauth2-demo-provider.onrender.com";

		Map<String, Object> config = new HashMap<>();
		config.put("issuer", issuer);
		config.put("authorization_endpoint", issuer + "/api/oauth/authorize");
		config.put("token_endpoint", issuer + "/api/oauth/token");
		config.put("userinfo_endpoint", issuer + "/api/oauth/userInfo");
		config.put("jwks_uri", issuer + "/.well-known/jwks.json");
		config.put("response_types_supported", List.of("code"));
		config.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic", "client_secret_post"));
		config.put("subject_types_supported", List.of("public"));
		config.put("id_token_signing_alg_values_supported", List.of("RS256"));
		config.put("scopes_supported", List.of("openid", "profile"));
		config.put("end_session_endpoint", issuer + "/oauth/revokedSessionId");
		config.put("claims_supported", List.of("sub", "iss", "aud", "exp", "iat", "email", "name"));
		return config;

	}

	private String base64UrlEncode(byte[] data) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
	}

}
