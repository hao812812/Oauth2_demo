package com.example.client.ServiceImpl;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

@Service
public class JwtImpl {

	private static final Logger log = LoggerFactory.getLogger(OAuthServiceImpl.class);
	private static final String JWKS_URL = "https://oauth2-client-backend.onrender.com/.well-known/jwks.json";

	/**
	 * 驗證 id_token (ok
	 */
	public Map<String, Object> validateAndParseIdToken(String idToken) {
		try {
			log.info("開始驗證idToken:{}", idToken);
			RSAPublicKey pk = getCachedPublicKey();

			if (pk == null) {
				log.error("JWK 取得失敗");
				return null;
			}

			// 驗證jwt簽章
			Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(pk) // 用 B 平台的公開金鑰
					.build().parseClaimsJws(idToken); // 用id_token(header+payload)驗證 public key是否遭到竄改

			Claims jwsBody = jws.getBody();

			// 檢查 id_token 過期日
			Date now = new Date();
			Date exp = jwsBody.getExpiration();
			if (exp == null || exp.before(now)) {
				log.warn("IdToken 已過期");
				return null;
			}
			log.info("JWT 驗證成功");

			Map<String, Object> response = new HashMap<String, Object>();
			response.put("sub", jwsBody.get("sub"));

			return response;

		} catch (Exception e) {
			log.error("JWT 驗證失敗", e);
			return null;
		}
	}

	/**
	 * 取得JKS 公鑰 (含catch機制)
	 */
	private RSAPublicKey getCachedPublicKey() {
		try {

			RestTemplate restTemplate = new RestTemplate();
			// 打 b 平台的 jwk 端點 取 public key
			String jwkResponse = restTemplate.getForObject(JWKS_URL, String.class);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode keys = mapper.readTree(jwkResponse).path("keys");

			if (!keys.isArray() || keys.size() == 0) {
				log.error("取得 JKS 無任何 key");
				return null;
			}

			JsonNode jwk = keys.get(0);

			// 解析 modules(n),exponent(e)
			BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("n").asText()));
			BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("e").asText()));

			RSAPublicKeySpec spec = new RSAPublicKeySpec(n, e);
			RSAPublicKey pk = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
			log.info("取得 JKS 成功");

			return pk;

		} catch (Exception e) {
			log.error("JKS 解析失敗", e);
			return null;
		}

	}
}
