package com.example.client.ServiceImpl;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.client.Dto.ExchangeTokenRequest;
import com.example.client.Entity.ClientOauthStateTb;
import com.example.client.Entity.ClientUserTokenTb;
import com.example.client.Exception.BaseException;
import com.example.client.Repository.ClientOauthStateRepository;
import com.example.client.Repository.ClientUserTokenRepository;
import com.example.client.Service.OAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import Dto.commonRes;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class OAuthServiceImpl implements OAuthService {

	@Value("${oauth.client.client-id}")
	private String client_id;

	@Value("${oauth.client.client-secret}")
	private String client_secret;

	@Value("${endpoint}")
	private String endpoint;

	@Autowired
	private ClientOauthStateRepository clientOauthStateRepository;

	@Autowired
	private ClientUserTokenRepository clientUserTokenRepository;

	private static final Logger log = LoggerFactory.getLogger(OAuthServiceImpl.class);

	/**
	 * 產生 授權 url
	 */
	@Override
	public commonRes<Map<String, Object>> generateAuthUrl() {

		log.info("開始組裝oauth2 授權 url");
		try {
			String state = generateRandomState();

			UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(endpoint)
					.queryParam("response_type", "code").queryParam("client_id", client_id)
					.queryParam("scope", "profile+openid").queryParam("redirect_uri", "http://localhost:4200/callback")
					.queryParam("state", state);

			// PKCE 的部分
			// codeVerifier 生成一個隨機的值
			String codeVerifier = generateRandomCodeVerifier();

			// 使用 SHA-256 演算法，去 hash codeVerifier，並且將 hash 過後的值，儲存在 codeChallenge 中
			String codeChallengeMethod = "S256";
			String codeChallenge = createHash(codeVerifier);

			// 拼接 PKCE 的參數到 url 中
			urlBuilder.queryParam("code_challenge_method", codeChallengeMethod).queryParam("code_challenge",
					codeChallenge);

			// 儲存到client 端得db中
			ClientOauthStateTb entity = new ClientOauthStateTb();
			entity.setState(state);
			entity.setCode_verifier(codeVerifier);
			entity.setCreated_at(new Timestamp(System.currentTimeMillis()));

			clientOauthStateRepository.save(entity);

			// 建立完整 URL 字串
			String authorizationUrl = urlBuilder.toUriString();
			Map<String, Object> result = Map.of("authorizationUrl", authorizationUrl);
			return commonRes.success("組裝授權URL成功", result);

		} catch (Exception e) {
			log.error("產生授權URL發生錯誤");
			throw new BaseException("產生授權URL發生錯誤", e);
		}

	}

	/**
	 * get client 接收 授權平台(b平台) 傳送的code return code (authorization_code) 給授權平台(B平台)
	 * get 接收 access_token
	 */
	@Override
	public commonRes<Map<String, Object>> accessToken(ExchangeTokenRequest request, HttpServletResponse response) {
		String tokenUrl = "http://localhost:9090/api/oauth/token";

		// client db 中 查詢state、code_verifeir, 驗證 state 是否有值(防止CSRF 攻擊)
		Optional<ClientOauthStateTb> data = clientOauthStateRepository.findById(request.getState());

		// 驗證 data 是否存在
		if (data.isEmpty()) {
			return commonRes.error("查找 client_oauth_state 失敗");
		}
		;

		ClientOauthStateTb oauthState = data.get();

		// 建立 POST 請求 設置表頭
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);// 資料是表單格式

		// 設置參數
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", client_id);
		body.add("client_secret", client_secret);
		body.add("code", request.getCode());
		body.add("redirect_uri", "http://localhost:4200/callback");

		// 在 request body 中添加 PKCE 的code_verifier
		body.add("code_verifier", oauthState.getCode_verifier());

		RestTemplate restTemplate = new RestTemplate();

		// 傳送code， 接收access_token等資訊
		try {
			String result = restTemplate.postForObject(tokenUrl, new HttpEntity<>(body, headers), String.class);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(result); // 解析成樹狀結構
			JsonNode dataNode = root.path("data"); // 取出 data 物件
			String access_token = dataNode.path("access_token").asText(); // 取出 access_token
			String id_token = dataNode.path("id_token").asText(); // 取出 id_token
			String refresh_token = dataNode.path("refresh_token").asText(); // 取出 refresh_token
			long accessExpiresAt = dataNode.path("access_token_expiresAt").asLong();
			long refreshExpiresAt = dataNode.path("refresh_token_expiresAt").asLong();

			// JWT 驗證與解析
			Map<String, Object> idTokenBody = validateAndParseIdToken(id_token);

			// JWT 驗證失敗
			if (idTokenBody == null) {
				return commonRes.error("JWT驗證失敗");
			}

			// 儲存token值到 db
			String sessionId = UUID.randomUUID().toString();

			String userSub = (String) idTokenBody.get("sub");// user_id
			ClientUserTokenTb tokenEntity = new ClientUserTokenTb();
			tokenEntity.setSessionId(sessionId);
			tokenEntity.setUserSub(userSub);
			tokenEntity.setAccessToken(access_token);
			tokenEntity.setRefreshToken(refresh_token);

			tokenEntity.setAccessExpiresAt(new Timestamp(System.currentTimeMillis() + accessExpiresAt * 1000));
			tokenEntity.setRefreshExpiresAt(new Timestamp(System.currentTimeMillis() + refreshExpiresAt * 1000));
			tokenEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			tokenEntity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

			clientUserTokenRepository.save(tokenEntity);

			// 設置sessionId 到前端
			ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId).httpOnly(true).secure(true)
					.sameSite("Strict").path("/").maxAge(Duration.ofDays(7)).build();
			response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

			Map<String, Object> tokenResult = Map.of("access_token", access_token, "session_id", sessionId);

			return commonRes.success("取得accessToken 成功", tokenResult);

		} catch (Exception e) {
			return commonRes.error("傳送code失敗");
		}
	}

	/**
	 * 拿 access_token 去取得 user info
	 */
	@Override
	public commonRes<Map<String, Object>> getUserInfoByToken(String accessToken) {
		String userInfoUrl = "http://localhost:9090/api/oauth/userInfo";

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			HttpEntity<Void> request = new HttpEntity<>(headers);

			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, String.class);

			if (response.getStatusCode() != HttpStatus.OK) {
				return commonRes.error("取得使用者資訊失敗");
			}

			ObjectMapper mapper = new ObjectMapper();
			// 將json 轉乘 java map
			Map<String, Object> userInfo = mapper.readValue(response.getBody(),
					new TypeReference<Map<String, Object>>() {
					});
			return commonRes.success("取得使用者資訊成功", userInfo);

		} catch (Exception e) {
			return commonRes.error("呼叫 userInfo 發生錯誤");
		}
	}

	/**
	 * 檢查token
	 */
	@Override
	public commonRes<Map<String, Object>> checkToken(HttpServletRequest request) {
		String sessionId = null;

		// 從cookie 抓 session_id
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if ("SESSION_ID".equals(c.getName())) {
					sessionId = c.getValue();
					break;
				}
			}
		}
		if (sessionId == null) {
			return commonRes.error("找不到sessionId");
		}

		// 查 DB 拿 accessToken
		Optional<ClientUserTokenTb> tokenOpt = clientUserTokenRepository.findById(sessionId);
		if (tokenOpt == null) {
			return commonRes.error("session 過期或無效");
		}
		ClientUserTokenTb tokenData = tokenOpt.get();

		// 判斷 accessToken 是否過期
		Timestamp now = new Timestamp(System.currentTimeMillis());
		String accessToken = tokenData.getAccessToken();
		// 若過期，用refresh_token換發 新access_token
		if (tokenData.getAccessExpiresAt().before(now)) {
			String tokenUrl = "http://localhost:9090/api/oauth/token";

			// 打 token api 取新 accessToken
			// 建立 POST 請求 設置表頭
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);// 資料是表單格式
			// 設置參數
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("grant_type", "refresh_token");
			body.add("client_id", client_id);
			body.add("client_secret", client_secret);
			body.add("refresh_token", tokenData.getRefreshToken());
			RestTemplate restTemplate = new RestTemplate();

			try {
				String result = restTemplate.postForObject(tokenUrl, new HttpEntity<>(body, headers), String.class);
				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(result); // 解析成樹狀結構
				Boolean success = root.path("success").asBoolean();
				String message = root.path("message").asText();
				JsonNode dataNode = root.path("data"); // 取出 data 物件
				String newAccessToken = dataNode.path("access_token").asText(); // 取出 access_token
				String id_token = dataNode.path("id_token").asText();
				long accessExpiresAt = dataNode.path("access_token_expiresAt").asLong();

				// 若失敗
				if (!success) {
					return commonRes.error(message);
				}

				// JWT 驗證與解析
				Map<String, Object> idTokenBody = validateAndParseIdToken(id_token);

				// JWT 驗證失敗
				if (idTokenBody == null) {
					return commonRes.error("JWT驗證失敗");
				}

				// 更新 db中的 update_at、accessToken、accessExpires_at
				tokenData.setAccessToken(newAccessToken);
				tokenData.setAccessExpiresAt(new Timestamp(System.currentTimeMillis() + accessExpiresAt * 1000));
				tokenData.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
				clientUserTokenRepository.save(tokenData);

				accessToken = newAccessToken;
			} catch (Exception e) {
				return commonRes.error("refresh_token 換新 access_token 失敗");
			}
		}

		// 用 access_token 取 userInfo
		commonRes<Map<String, Object>> userInfo = getUserInfoByToken(accessToken);
		if (!userInfo.isSuccess()) {
			return commonRes.error("access_token 無效，請重新登入");
		}

		return commonRes.success("token 有效", userInfo.getData());
	}

	/**
	 * 驗證 + 解析 id_token jwt 安全性
	 * 
	 * @param id_token
	 * @return
	 */
	private Map<String, Object> validateAndParseIdToken(String idToken) {

		String jwksUrl = "http://localhost:9090/.well-known/jwks.json";
		try {
			RestTemplate restTemplate = new RestTemplate();
			// 打 b 平台的 jwk 端點 取 public key
			String jwkResponse = restTemplate.getForObject(jwksUrl, String.class);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(jwkResponse);
			JsonNode keys = root.path("keys");

			// key中沒有值
			if (!keys.isArray() || keys.size() == 0) {
				throw new RuntimeException("JWKS 無任何 key");
			}

			// 假設只有一把 key
			JsonNode jwk = keys.get(0);
			String n = jwk.path("n").asText();
			String e = jwk.path("e").asText();

			// 將 n, e (Base64URL 編碼的 modulus, exponent) 轉為 RSAPublicKey
			byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
			byte[] exponentBytes = Base64.getUrlDecoder().decode(e);
			// 把位元組陣列轉成 BigInteger
			BigInteger modulus = new BigInteger(1, modulusBytes);
			BigInteger exponent = new BigInteger(1, exponentBytes);

			RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(modulus, exponent);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			// 組出 public key 值
			RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

			// 驗證jwt簽章
			Jws<Claims> jws = Jwts.parserBuilder().setSigningKey(publicKey) // 用 B 平台的公開金鑰
					.build().parseClaimsJws(idToken); // 用id_token(header+payload)驗證 public key是否遭到竄改

			Claims jwsBody = jws.getBody();

			Map<String, Object> response = new HashMap<String, Object>();
			response.put("sub", jwsBody.get("sub"));

			return response;

		} catch (Exception e) {

			return null;

		}

	}

	private String generateRandomState() {
		SecureRandom sr = new SecureRandom();
		byte[] data = new byte[6];
		sr.nextBytes(data);
		return Base64.getUrlEncoder().encodeToString(data);
	}

	private String generateRandomCodeVerifier() {
		SecureRandom sr = new SecureRandom();
		byte[] data = new byte[96];
		sr.nextBytes(data);
		return Base64.getUrlEncoder().encodeToString(data);
	}

	private String createHash(String value) {
		String result;

		try {

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
			result = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("algorithm not found");
		}

		return result;
	}
}
