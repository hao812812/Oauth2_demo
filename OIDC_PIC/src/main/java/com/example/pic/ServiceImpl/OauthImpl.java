package com.example.pic.ServiceImpl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.pic.Entity.OauthAuthorizationCodeTb;
import com.example.pic.Entity.OauthAuthorizeRequestTb;
import com.example.pic.Entity.OauthRegisterTb;
import com.example.pic.Entity.OauthAccessTokenTb;
import com.example.pic.Repository.AuthorizationCodeRepository;
import com.example.pic.Repository.OauthAuthorizeReqRepository;
import com.example.pic.Repository.OauthRegisterRepository;
import com.example.pic.Repository.OauthAccessTokenRepository;
import com.example.pic.Repository.registerMemberRepository;
import com.example.pic.Service.OauthService;

import Dto.OauthCode;
import Dto.commonRes;
import Dto.memberInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class OauthImpl implements OauthService {

	/**
	 * oauth_register DB
	 */
	@Autowired
	private OauthRegisterRepository oauthRepository;

	/**
	 * oauth_authorization_code DB
	 */
	@Autowired
	private AuthorizationCodeRepository authorizationCodeRepository;

	/**
	 * oauth_authorize_request DB
	 */
	@Autowired
	private OauthAuthorizeReqRepository oauthAuthorizeReqRepository;

	/**
	 * oauth_token DB
	 */
	@Autowired
	private OauthAccessTokenRepository oauthAccessTokenRepository;

	/**
	 * member_register DB
	 */
	@Autowired
	private registerMemberRepository registerMemberRepository;

	@Autowired
	private KeyPair keyPair;

	/**
	 * 驗證對方 傳送來的 url 認證
	 */
	@Override
	public commonRes<Void> getAuthUrl(Map<String, String> request, HttpServletResponse response) {

		String clientId = request.get("client_id");
		String redirectUri = request.get("redirect_uri");
		String scope = request.get("scope");
		String state = request.get("state");
		String codeChallenge = request.get("code_challenge");
		String codeChallengeMethod = request.get("code_challenge_method");

		// 查 client 的 oauth_register 註冊表
		Optional<OauthRegisterTb> clientOpt = oauthRepository.findByClientId(clientId);

		// 驗證 client id是否存在
		if (clientOpt.isEmpty()) {
			sendBadRequest(response, "invalid_client id");
			return null;
		}
		OauthRegisterTb client = clientOpt.get();

		// 驗證 redirect_uri 是否匹配
		if (!client.getRedirectUris().equals(redirectUri)) {
			sendBadRequest(response, "invalid_redirect_uri");
			return null;
		}
		// 驗證 response_type
		if (!"code".equals(client.getResponseTypes())) {
			sendBadRequest(response, "response_type");
			return null;
		}

		// 驗證 scope
		Set<String> requestedScope = Arrays.stream(scope.split("\\s+")).map(String::toLowerCase)
				.collect(Collectors.toSet());// ["profile", "openid"]

		Set<String> dbScope = Arrays.stream(client.getScopes().split(",")).map(String::toLowerCase)
				.collect(Collectors.toSet());

		if (!dbScope.containsAll(requestedScope)) {
			sendBadRequest(response, "invalid_scope");
			return null;
		}
		// 驗證 PKCE
		if (!"S256".equals(codeChallengeMethod) || codeChallenge == null) {
			sendBadRequest(response, "invalid_pkce");
			return null;
		}

		// 暫存url 資料 至DB 以利後續產code使用
		OauthAuthorizeRequestTb entity = new OauthAuthorizeRequestTb();
		entity.setClientId(clientId);
		entity.setRedirectUri(redirectUri);
		entity.setState(state);
		entity.setScope(scope);
		entity.setCodeChallenge(codeChallenge);
		entity.setCodeChallengeMethod(codeChallengeMethod);

		oauthAuthorizeReqRepository.save(entity);

		// 導向b平台 登入頁 (Angular)
		try {
			String angularLogin = String.format(
					"http://localhost:4300/login?client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=%s",
					URLEncoder.encode(clientId, StandardCharsets.UTF_8),
					URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
					URLEncoder.encode(scope, StandardCharsets.UTF_8), URLEncoder.encode(state, StandardCharsets.UTF_8),
					URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8),
					URLEncoder.encode(codeChallengeMethod, StandardCharsets.UTF_8));
			response.sendRedirect(angularLogin);
		} catch (Exception e) {
			e.printStackTrace();
			sendBadRequest(response, "redirect_failed");
		}

		return null;
	}

	/**
	 * 同意授權後 傳送 code url 給 client端
	 */
	@Override
	public commonRes<Void> approveAuthorization(Map<String, String> request, HttpServletResponse response)
			throws IOException {

		String state = request.get("state");
		String userId = request.get("user_id");

		// DB 查找 暫存的 request url 資料
		Optional<OauthAuthorizeRequestTb> data = oauthAuthorizeReqRepository.findByState(state);

		// db 是否有資料
		if (data.isEmpty()) {
			sendBadRequest(response, "search oauth_authorizeRequest DB failed");
			return null;
		}

		OauthAuthorizeRequestTb urlData = data.get();

		// 建立授權碼
		String code = UUID.randomUUID().toString();

		// 設定有效期限（5分鐘）
		Timestamp expiresAt = Timestamp.from(Instant.now().plus(Duration.ofMinutes(5)));

		// 寫入資料表
		OauthAuthorizationCodeTb entity = new OauthAuthorizationCodeTb();
		entity.setCode(code);
		entity.setClientId(urlData.getClientId());
		entity.setUserId(userId);
		entity.setRedirectUri(urlData.getRedirectUri());
		entity.setScope(urlData.getScope());
		entity.setCodeChallenge(urlData.getCodeChallenge());
		entity.setCodeChallengeMethod(urlData.getCodeChallengeMethod());
		entity.setExpiresAt(expiresAt);
		authorizationCodeRepository.save(entity);

		// redirect 回 A 平台
		String redirectUrl = String.format("%s?code=%s&state=%s&scope=%s", urlData.getRedirectUri(), code, state,
				urlData.getScope());
		response.sendRedirect(redirectUrl);
		return null;
	}

	/**
	 * get code 資訊 ，產生access_token、refresh_token
	 * return 傳送access_Token 等資料給client端
	 */
	@Override
	public commonRes<Map<String, Object>> accessCode(OauthCode request) {

		// 驗證 grant_type, 若為refreshToken
		if ("refresh_token".equals(request.getGrant_type())) {
			return handleRefreshToken(request);
		} else if ("authorization_code".equals(request.getGrant_type())) {
			return handleAuthorizationCode(request);
		} else {
			return commonRes.error("不支援的 grant_type");
		}
	}

	/**
	 * 處理 refresh_token 流程
	 */
	private commonRes<Map<String, Object>> handleRefreshToken(OauthCode request) {
		String refreshToken = request.getRefresh_token();
		String clientId = request.getClient_id();
		String clientSecret = request.getClient_secret();

		// 查詢 refresh_token
		Optional<OauthAccessTokenTb> tokenOpt = oauthAccessTokenRepository.findByRefreshToken(refreshToken);

		if (tokenOpt.isEmpty()) {
			return commonRes.error("無效的 refresh_token");
		}

		OauthAccessTokenTb tokenEntity = tokenOpt.get();
		
	    // 驗證是否已撤銷
	    if (tokenEntity.getRevoked()) {
	        return commonRes.error("refresh_token 已被撤銷");
	    }

	    // 驗證 refresh_token 是否過期
	    Timestamp now = Timestamp.from(Instant.now());
	    if (tokenEntity.getRefreshToken_expiresAt() != null && tokenEntity.getRefreshToken_expiresAt().before(now)) {
	        return commonRes.error("refresh_token 已過期");
	    }

		// 驗證 client_id
		if (!tokenEntity.getClientId().equals(clientId)) {
			return commonRes.error("client_id 不匹配");
		}

		// 驗證 client_secret
		Optional<OauthRegisterTb> clientOpt = oauthRepository.findByClientId(clientId);
		if (clientOpt.isEmpty() || !clientOpt.get().getClientSecret().equals(clientSecret)) {
			return commonRes.error("client_secret 錯誤");
		}

		// 查詢使用者資訊
		memberInfo userInfo = registerMemberRepository.queryMemberById(tokenEntity.getUserId());
		if (userInfo == null) {
			return commonRes.error("查無使用者資訊");
		}

		long nowMillis = System.currentTimeMillis();

		// 只生成新的 access_token 和 id_token
		String newAccessToken = UUID.randomUUID().toString();

		String idToken = Jwts.builder()
				.setIssuer("http://localhost:9090")
				.setSubject(String.valueOf(tokenEntity.getUserId()))
				.setAudience(clientId)
				.setIssuedAt(new Date(nowMillis))
				.setExpiration(new Date(nowMillis + 3600_000))//3600秒
				.claim("email", userInfo.getMail())
				.claim("name", userInfo.getName())
				.signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256).compact();

		// 更新資料庫中的 access_token
		tokenEntity.setAccessToken(newAccessToken);
		tokenEntity.setAccessToken_expiresAt(new Timestamp(nowMillis + 3600_000));
		oauthAccessTokenRepository.save(tokenEntity);

		long accessExpiresIn = 3600;// 1小時
		// 回傳
		Map<String, Object> response = new HashMap<>();
		response.put("access_token", newAccessToken);
		response.put("id_token", idToken);
		response.put("token_type", "Bearer");
		response.put("access_token_expiresAt", accessExpiresIn);
		return commonRes.success("成功刷新 token", response);

	}

	/**
	 * 處理 authorization_code 流程
	 */
	private commonRes<Map<String, Object>> handleAuthorizationCode(OauthCode request) {
		String code = request.getCode();
		String clientId = request.getClient_id();
		String clientSecret = request.getClient_secret();
		String codeVerifier = request.getCode_verifier();

		// 查詢並驗證 authorization code
		Optional<OauthAuthorizationCodeTb> codeOpt = authorizationCodeRepository.findByCode(code);
		if (codeOpt.isEmpty()) {
			return commonRes.error("無效的 code");
		}

		OauthAuthorizationCodeTb codeEntity = codeOpt.get();

		// 驗證 code 是否過期
		if (codeEntity.getExpiresAt().before(Timestamp.from(Instant.now()))) {
			return commonRes.error("code 已過期");
		}

		// 驗證是否已使用
		if (codeEntity.isUsed()) {
			return commonRes.error("code 已被使用");
		}

		// 驗證 client_id
		if (!codeEntity.getClientId().equals(clientId)) {
			return commonRes.error("client_id 不匹配");
		}

		// 驗證 client_secret
		Optional<OauthRegisterTb> clientOpt = oauthRepository.findByClientId(clientId);
		if (clientOpt.isEmpty() || clientOpt.get().getClientSecret() == null
				|| !clientOpt.get().getClientSecret().equals(clientSecret)) {
			return commonRes.error("client_secret 錯誤或不存在");
		}

		// 驗證 PKCE
		if ("S256".equalsIgnoreCase(codeEntity.getCodeChallengeMethod())) {
			String expectedChallenge = createHash(codeVerifier);
			if (!expectedChallenge.equals(codeEntity.getCodeChallenge())) {
				return commonRes.error("code_verifier 驗證失敗");
			}
		} else {
			return commonRes.error("驗證 PKCE 失敗");
		}

		// 查詢使用者資訊
		memberInfo userInfo = registerMemberRepository.queryMemberById(Long.parseLong(codeEntity.getUserId()));
		if (userInfo == null) {
			return commonRes.error("查無使用者資訊");
		}

		// 標記授權碼為已使用
		codeEntity.setUsed(true);
		authorizationCodeRepository.save(codeEntity);

		long nowMillis = System.currentTimeMillis();
	    
	    // 生成 tokens
	    String accessToken = UUID.randomUUID().toString();
	    String refreshToken = UUID.randomUUID().toString();
	    
	    // 生成 id_token
	    String idToken = Jwts.builder()
	        .setIssuer("http://localhost:9090")
	        .setSubject(codeEntity.getUserId())
	        .setAudience(clientId)
	        .setIssuedAt(new Date(nowMillis))
	        .setExpiration(new Date(nowMillis + 3600_000))
	        .claim("email", userInfo.getMail())
	        .claim("name", userInfo.getName())
	        .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
	        .compact();
	 
	    // 儲存 access_token 到資料庫
	    OauthAccessTokenTb entity = new OauthAccessTokenTb();
	    entity.setUserId(Long.parseLong(codeEntity.getUserId()));
	    entity.setClientId(clientId);
	    entity.setAccessToken(accessToken);
	    entity.setRefreshToken(refreshToken);
	    entity.setAccessToken_expiresAt(new Timestamp(nowMillis + 3600_000));//1hr
	    entity.setRefreshToken_expiresAt(new Timestamp(nowMillis + 7 * 24 * 3600_000L));//7天
	    entity.setScope(codeEntity.getScope());
	    oauthAccessTokenRepository.save(entity);
	    
	    // 組裝回應
	    Map<String, Object> response = new HashMap<>();
	    response.put("access_token", accessToken);
	    response.put("id_token", idToken);
	    response.put("refresh_token", refreshToken);
	    response.put("token_type", "Bearer");
	    response.put("access_token_expiresAt", 3600);
	    response.put("refresh_token_expiresAt", 3600*24*7);//7天

		return commonRes.success("成功,回傳 access_token", response);
	}

	/**
	 * get access_token 
	 * return 使用者資訊
	 */
	@Override
	public ResponseEntity<Map<String, Object>> getUserInfo(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 取出access_token
		String accessToken = authHeader.substring(7);

		// 資料庫驗證access_token
		Optional<OauthAccessTokenTb> tokenData = oauthAccessTokenRepository.findByAccessToken(accessToken);
		if (tokenData.isEmpty() || tokenData.get().getRevoked()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "access_token驗證失敗"));
		}

		OauthAccessTokenTb token = tokenData.get();

		// 驗證是否被撤銷
		if (token.getRevoked() != null && token.getRevoked()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "access_token 已失效"));
		}
		// 驗證 token 有效時間
		Timestamp now = new Timestamp(System.currentTimeMillis());
		if (token.getAccessToken_expiresAt().before(now)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "access_token 已過期"));
		}

		// 查詢使用者資料
		memberInfo userResult = registerMemberRepository.queryMemberById(token.getUserId());
		// 判斷是否存在
		if (userResult == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "查無使用者"));
		}

		// 組裝 回傳 使用者資料
		Map<String, Object> userInfo = new HashMap<>();
		userInfo.put("id", userResult.getId());
		userInfo.put("mail", userResult.getMail());
		userInfo.put("name", userResult.getName());

		return ResponseEntity.ok(userInfo);
	}

	/**
	 * S256雜湊演算法
	 */
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

	
	
	/**
	 * 統一導到錯誤頁面
	 */
	private void sendBadRequest(HttpServletResponse response, String message) {
		try {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
