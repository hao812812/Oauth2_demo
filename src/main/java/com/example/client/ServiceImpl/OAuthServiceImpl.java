package com.example.client.ServiceImpl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
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
import com.example.client.Dto.commonRes;
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

	@Autowired
	private JwtImpl jwtImpl;

	private static final Logger log = LoggerFactory.getLogger(OAuthServiceImpl.class);

	/**
	 * 產生 授權 url (OK)
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
			log.info("組裝授權URL成功 url:{}", authorizationUrl);
			return commonRes.success("組裝授權URL成功", result);

		} catch (Exception e) {
			log.error("產生授權URL發生錯誤", e);
			throw new BaseException("產生授權URL發生錯誤");
		}

	}

	/**
	 * (ok) client 接收 b平台 傳送的 code return code (authorization_code) 給授權平台(B平台)
	 * client 接收 b平台 傳送的 access_token
	 */
	@Override
	public commonRes<Map<String, Object>> accessToken(ExchangeTokenRequest request, HttpServletResponse response) {
		String tokenUrl = "http://localhost:9090/api/oauth/token";

		String code = request.getCode();
		String state = request.getState();
		log.info("開始交換授權碼code,code={}", code);

		// db 中 查詢state、code_verifeir, 驗證 state 是否有值(防止CSRF 攻擊)
		Optional<ClientOauthStateTb> stateOpt = clientOauthStateRepository.findById(state);

		// 驗證 state 是否存在
		if (stateOpt.isEmpty()) {
			log.error("無效的 state, state={}", state);
			return commonRes.error("invalid_state");
		}
		;
		ClientOauthStateTb oauthState = stateOpt.get();

		// 建立 code request
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);// 資料是表單格式

		// 設置參數
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", client_id);
		body.add("client_secret", client_secret);
		body.add("code", code);
		body.add("redirect_uri", "http://localhost:4200/callback");
		// 在 request body 中添加 PKCE 的 code_verifier
		body.add("code_verifier", oauthState.getCode_verifier());

		RestTemplate restTemplate = new RestTemplate();

		String tokenRes;

		try {
			tokenRes = restTemplate.postForObject(tokenUrl, new HttpEntity<>(body, headers), String.class);
		} catch (Exception e) {
			log.error("呼叫 Token API 失敗,code={}", code, e);
			return commonRes.error("Token_API_Failed");
		}

		// 解析 Token response
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = null;
		try {
			root = mapper.readTree(tokenRes); // 解析成樹狀結構
		} catch (Exception e) {
			log.error("Token API 無法解析Json,ApiResponse={}", tokenRes, e);
			return commonRes.error("invalid_token_response");
		}

		// 檢查 success
		if (!root.path("success").asBoolean()) {
			String errorMsg = root.path("message").asText();
			log.error("Token API回傳錯誤:{}", errorMsg);
			return commonRes.error("invalid_token_response");
		}

		// 取 data
		JsonNode dataNode = root.path("data");
		String access_token = dataNode.path("access_token").asText(); // 取出 access_token
		String id_token = dataNode.path("id_token").asText(); // 取出 id_token
		String refresh_token = dataNode.path("refresh_token").asText(); // 取出 refresh_token
		long accessExpiresAt = dataNode.path("access_token_expiresAt").asLong();
		long refreshExpiresAt = dataNode.path("refresh_token_expiresAt").asLong();

		// 檢查是否有缺乏值
		Map<String, String> fields = new HashMap<>();
		fields.put("access_token", access_token);
		fields.put("id_token", id_token);
		fields.put("refresh_token", refresh_token);
		for (var entrys : fields.entrySet()) {
			if (entrys.getValue() == null || entrys.getValue().isBlank()) {
				log.error("TokenApi response 缺少:{}", entrys.getKey());
				return commonRes.error("缺少" + entrys.getKey());
			}
		}
		;
		if (accessExpiresAt <= 0 || refreshExpiresAt <= 0) {
			log.error("expiresAt 無效");
			return commonRes.error("expiresAt 無效");
		}
		log.info("取得Token成功");

		// JWT 驗證與解析
		Map<String, Object> idTokenBody = jwtImpl.validateAndParseIdToken(id_token);

		// JWT 驗證失敗
		if (idTokenBody == null) {
			log.error("IdToken_failed,idToken:{}", id_token);
			return commonRes.error("JWT驗證失敗");
		}

		log.info("IdToken 驗證成功");

		Timestamp now = new Timestamp(System.currentTimeMillis());

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
		tokenEntity.setCreatedAt(now);
		tokenEntity.setUpdatedAt(now);

		try {
			clientUserTokenRepository.save(tokenEntity);
		} catch (Exception e) {
			log.error("儲存 client_user_token DB 發生錯誤", e);
			return commonRes.error("client_user_token db failed");
		}

		// 設置sessionId 到前端
		ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId).httpOnly(true).secure(true)
				.sameSite("Strict").path("/").maxAge(Duration.ofDays(7)).build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		log.info("設置 sessionID 到Cookie");

		// 移除 state
		try {
			clientOauthStateRepository.deleteById(state);
		} catch (Exception e) {
			log.warn("clientOauthState DB 刪除資料失敗", e);
		}

		Map<String, Object> tokenResult = Map.of("access_token", access_token, "session_id", sessionId);

		return commonRes.success("取得accessToken 成功", tokenResult);
	}

	/**
	 * (ok) 拿 access_token 去取得 user info
	 */
	@Override
	public commonRes<Map<String, Object>> getUserInfoByToken(String accessToken) {
		String userInfoUrl = "http://localhost:9090/api/oauth/userInfo";

		try {
			log.info("開始呼叫userInfo API, AccessToken={}", accessToken);
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			HttpEntity<Void> request = new HttpEntity<>(headers);

			RestTemplate restTemplate = new RestTemplate();

			ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, String.class);

			ObjectMapper mapper = new ObjectMapper();

			// 將 json 轉成 java map
			Map<String, Object> userInfo = mapper.readValue(response.getBody(),
					new TypeReference<Map<String, Object>>() {
					});

			// 欄位缺少 驗證
			List<String> keyList = List.of("id", "mail", "name");
			for (String key : keyList) {
				if (!userInfo.containsKey(key)) {
					log.error("缺少userInfo:{} ", key);
					return commonRes.error("缺少 userinfo:" + key + "欄位");
				}
			}

			log.info("取得userInfo 成功");
			return commonRes.success("取得使用者資訊成功", userInfo);

		} catch (HttpClientErrorException e) {
			// http status 401
			log.warn("accessToken 失效,accessToken={}", accessToken);
			return commonRes.error("Invalid_accessToken");

		} catch (Exception e) {
			log.error("Call userinfo API 發生錯誤", e);
			return commonRes.error("呼叫 userInfo 發生錯誤");
		}
	}

	/**
	 * 檢查token
	 */
	@Override
	public commonRes<Map<String, Object>> checkToken(HttpServletRequest request) {
		String sessionId = null;

		log.info("開始checkToken API");

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
			log.warn("瀏覽器中找不到SESSION_ID");
			return commonRes.error("找不到sessionId");
		}

		// 查 DB 拿 accessToken
		Optional<ClientUserTokenTb> tokenOpt = clientUserTokenRepository.findById(sessionId);
		if (tokenOpt.isEmpty()) {
			log.warn("clientUserToken table 中找不到sessionId:{}", sessionId);
			return commonRes.error("session 無效");
		}
		log.info("取得Token成功");
		ClientUserTokenTb tokenData = tokenOpt.get();

		// 判斷 accessToken 是否過期
		String accessToken = ensureValidAccessToken(tokenData);

		// 用 access_token 取 userInfo
		commonRes<Map<String, Object>> userInfo = getUserInfoByToken(accessToken);
		if (!userInfo.isSuccess()) {
			return commonRes.error("access_token 無效，請重新登入");
		}

		return commonRes.success("token 有效", userInfo.getData());
	}

	/**
	 * refreshToken 換發 新accessToken
	 */
	private String ensureValidAccessToken(ClientUserTokenTb tokenData) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		String oldAccessToken = tokenData.getAccessToken();

		// 若accessToken未過期, 直接使用accessToken
		if (tokenData.getAccessExpiresAt().after(now)) {
			return oldAccessToken;
		}

		// 用refreshToken 換發新AccessToken
		log.info("accessToken已過期，開始用refreshToken換發新token");
		String tokenUrl = "http://localhost:9090/api/oauth/token";
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
				log.error("換發Token失敗:{}", message);
				throw new BaseException(message);
			}
			// JWT 驗證與解析
			Map<String, Object> idTokenBody = jwtImpl.validateAndParseIdToken(id_token);
			// JWT 驗證失敗
			if (idTokenBody == null) {
				log.error("JWT 驗證失敗");
				throw new BaseException("JWT驗證失敗");
			}
			// 更新 db中的 update_at、accessToken、accessExpires_at
			tokenData.setAccessToken(newAccessToken);
			tokenData.setAccessExpiresAt(new Timestamp(System.currentTimeMillis() + accessExpiresAt * 1000));
			tokenData.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
			clientUserTokenRepository.save(tokenData);
			log.info("refresh_token 換發 access_token 成功");
			return newAccessToken;

		} catch (Exception e) {
			log.error("refresh_token 換新 access_token 發生例外", e);
			throw new BaseException("refresh_token 換新 access_token 失敗");
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
