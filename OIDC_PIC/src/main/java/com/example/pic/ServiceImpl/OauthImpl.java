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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.pic.Entity.OauthAuthorizationCodeTb;
import com.example.pic.Entity.OauthAuthorizeRequestTb;
import com.example.pic.Entity.OauthRegisterTb;
import com.example.pic.Entity.OauthUserSession;
import com.example.pic.Entity.OauthAccessTokenTb;
import com.example.pic.Repository.AuthorizationCodeRepository;
import com.example.pic.Repository.OauthAuthorizeReqRepository;
import com.example.pic.Repository.OauthRegisterRepository;
import com.example.pic.Repository.OauthUserSessionRepository;
import com.example.pic.Repository.OauthAccessTokenRepository;
import com.example.pic.Repository.registerMemberRepository;
import com.example.pic.Service.OauthService;

import Dto.OauthCode;
import Dto.commonRes;
import Dto.memberInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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

	@Autowired
	private OauthUserSessionRepository oauthUserSessionRepository;

	private static final Logger log = LoggerFactory.getLogger(OauthImpl.class);
	private static final long ACCESS_TOKEN_TTL_SECONDS = 3600; // 1 小時

	/**
	 * 驗證對方 傳送來的 url 認證 (ok
	 */
	@Override
	public void getAuthUrl(Map<String, String> request, HttpServletRequest httpRequest, HttpServletResponse response)
			throws IOException {

		log.info("開始接收授權 url");

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
			log.error("無效的client Id={}", clientId);
			redirectError(response, redirectUri, "invalid_client id");
		}
		OauthRegisterTb client = clientOpt.get();

		// 驗證 redirect_uri 是否匹配
		if (!client.getRedirectUri().equals(redirectUri)) {
			log.error("無效的invalid_redirect_uri={}", redirectUri);
			redirectError(response, redirectUri, "invalid_redirect_uri");
			return;
		}
		// 驗證 response_type
		if (!"code".equals(client.getResponseTypes())) {
			log.error("無效的response_type={}", client.getResponseTypes());
			redirectError(response, redirectUri, "response_type");
			return;
		}

		// 驗證 scope
		Set<String> requestedScope = Arrays.stream(scope.split("\\s+")).map(String::toLowerCase)
				.collect(Collectors.toSet());// ["profile", "openid"]

		Set<String> dbScope = Arrays.stream(client.getScopes().split(",")).map(String::toLowerCase)
				.collect(Collectors.toSet());

		if (!dbScope.containsAll(requestedScope)) {
			log.error("無效的invalid_scope={}", requestedScope);
			redirectError(response, redirectUri, "invalid_scope");
			return;
		}
		// 驗證 PKCE
		if (!"S256".equals(codeChallengeMethod) || codeChallengeMethod == null) {
			log.error("無效的invalid_pkce={}", codeChallengeMethod);
			redirectError(response, redirectUri, "invalid_pkce");
			return;
		}

		// 驗證cookie 是否 免登入
		if (tryAutoLogin(httpRequest, request, response)) {
			log.info("使用者免登入,已由 tryAutoLogin 處理流程");
			return;
		}

		try {
			// 暫存url 資料 至DB 以利後續產code使用
			OauthAuthorizeRequestTb entity = new OauthAuthorizeRequestTb();
			entity.setClientId(clientId);
			entity.setRedirectUri(redirectUri);
			entity.setState(state);
			entity.setScope(scope);
			entity.setCodeChallenge(codeChallenge);
			entity.setCodeChallengeMethod(codeChallengeMethod);
			oauthAuthorizeReqRepository.save(entity);

		} catch (Exception e) {
			log.error("寫入 OauthAuthorizeRequestTb 發生錯誤", e);
			redirectError(response, redirectUri, "db_error");
			return;

		}

		// 導向b平台 登入頁 (Angular)
		try {
			String angularLogin = String.format(
					"http://localhost:4300/login?client_id=%s&redirect_uri=%s&scope=%s&state=%s&code_challenge=%s&code_challenge_method=%s",
					URLEncoder.encode(clientId, StandardCharsets.UTF_8),
					URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
					URLEncoder.encode(scope, StandardCharsets.UTF_8), URLEncoder.encode(state, StandardCharsets.UTF_8),
					URLEncoder.encode(codeChallenge, StandardCharsets.UTF_8),
					URLEncoder.encode(codeChallengeMethod, StandardCharsets.UTF_8));
			log.info("redirect OIDC Server會員登入頁面");
			response.sendRedirect(angularLogin);
			return;

		} catch (Exception e) {
			log.error("導向OIDC Server會員登入頁失敗", e);
			redirectError(response, redirectUri, "redirect_failed");

			return;
		}
	}

	/**
	 * 同意授權後 傳送 code url 給 client端 (ok
	 */
	@Override
	public void approveAuthorization(Map<String, String> request, HttpServletResponse response) throws IOException {

		String state = request.get("state");
		String userId = request.get("user_id");
		String redirectUri = request.get("redirect_uri");

		log.info("收到使用者授權同意, state={}, userId={}", state, userId);

		// DB 查找 暫存的 request url 資料
		Optional<OauthAuthorizeRequestTb> data = oauthAuthorizeReqRepository.findByState(state);

		// db 是否有資料
		if (data.isEmpty()) {
			log.error("找不到暫存的授權請求資料,state:{}", state);
			redirectError(response, redirectUri, "invalid_request");
			return;
		}

		OauthAuthorizeRequestTb urlData = data.get();

		// 建立授權碼
		String code = UUID.randomUUID().toString();

		// 設定有效期限（5分鐘）
		Timestamp expiresAt = Timestamp.from(Instant.now().plus(Duration.ofMinutes(5)));

		try {
			// 寫入資料表
			OauthAuthorizationCodeTb entity = new OauthAuthorizationCodeTb();
			entity.setCode(code);
			entity.setClientId(urlData.getClientId());
			entity.setUserId(Long.parseLong(userId));
			entity.setRedirectUri(urlData.getRedirectUri());
			entity.setScope(urlData.getScope());
			entity.setCodeChallenge(urlData.getCodeChallenge());
			entity.setCodeChallengeMethod(urlData.getCodeChallengeMethod());
			entity.setExpiresAt(expiresAt);
			authorizationCodeRepository.save(entity);
			log.info("成功產生授權碼, code={}, clientId={}, userId={}", code, urlData.getClientId(), userId);

		} catch (Exception e) {
			log.error("寫入授權碼進DB錯誤,state={}", request.get("state"), e);
			redirectError(response, redirectUri, "DB_Fail");
		}

		// redirect 回 A 平台
		String redirectUrl = String.format("%s?code=%s&state=%s", urlData.getRedirectUri(), code, state);
		response.sendRedirect(redirectUrl);
		return;
	}

	/**
	 * Token (ok)
	 */
	@Override
	public commonRes<Map<String, Object>> accessCode(OauthCode request) {
		String grantType = request.getGrant_type();
		// 驗證 grant_type, 若為refreshToken
		if ("refresh_token".equals(grantType)) {
			return handleRefreshToken(request);
		} else if ("authorization_code".equals(grantType)) {
			return handleAuthorizationCode(request);
		} else {
			log.error("不支援的 grant_type:{}", grantType);
			return commonRes.error("不支援的 grant_type");
		}
	}

	/**
	 * 解除 平台 免登入綁定(ok
	 */
	@Override
	public commonRes<Map<String, Object>> revokedSession(@RequestBody Map<String, String> req,
			HttpServletRequest httpRequest, HttpServletResponse response) {

		log.info("開始 revokedSession 解除平台免登入");
		// 取cookie(oidc_session_id)
		String sessionId = null;
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if ("OIDC_SESSION_ID".equals(c.getName())) {
					sessionId = c.getValue();
					break;
				}
			}
		}

		int deleteRow = oauthUserSessionRepository.deleteBySessionId(sessionId);

		// 刪除OIDC Session
		if (deleteRow == 0) {
			log.warn("oauth_User_Session DB 中找不到sessionId={}", sessionId);
		} else {
			log.info("成功刪除oauth_User_Session DB session, sessionId={}", sessionId);
		}

		// 刪除 browser session
		ResponseCookie clearCookie = ResponseCookie.from("OIDC_SESSION_ID", "").path("/").maxAge(0).httpOnly(true)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

		return commonRes.success("解除綁定成功");
	}

	/**
	 * 驗證是否先前有登入過 (ok
	 * 
	 * @param httpRequest
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	private boolean tryAutoLogin(HttpServletRequest httpRequest, Map<String, String> request,
			HttpServletResponse response) throws IOException {

		log.info("開始驗證是否免登入");

		String sessionId = null;

		// 取得sessionId cookie
		Cookie[] cookies = httpRequest.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if ("OIDC_SESSION_ID".equals(c.getName())) {
					sessionId = c.getValue();
					break;
				}
			}
		}
		// 沒有 cookie，代表未登入
		if (sessionId == null) {
			log.warn("找不到sessionID");
			return false;
		}

		// 查DB 是否有效SessionId
		Optional<OauthUserSession> sessionOpt = oauthUserSessionRepository.findBySessionId(sessionId);
		// session 是否有效
		if (sessionOpt.isEmpty()) {
			log.warn("DB找不到sessionId:{}", sessionId);
			return false;
		}

		OauthUserSession session = sessionOpt.get();
		Instant now = Instant.now();
		Instant expiredAt = session.getExpiresAt().toInstant();

		// 若session 過期
		if (expiredAt.isBefore(now)) {
			log.warn("session:{}已過期，expiresAt:{}", session.getSessionId(), session.getExpiresAt());
			return false;
		}

		// 直接發 授權code 導回 client平台
		String code = UUID.randomUUID().toString();
		// 5分鐘有效期限
		Timestamp codeExpires = Timestamp.from(Instant.now().plus(Duration.ofMinutes(5)));
		try {
			OauthAuthorizationCodeTb codeEntity = new OauthAuthorizationCodeTb();
			codeEntity.setCode(code);
			codeEntity.setClientId(request.get("client_id"));
			codeEntity.setUserId(session.getUserId());
			codeEntity.setRedirectUri(request.get("redirect_uri"));
			codeEntity.setScope(request.get("scope"));
			codeEntity.setCodeChallenge(request.get("code_challenge"));
			codeEntity.setCodeChallengeMethod(request.get("code_challenge_method"));
			codeEntity.setExpiresAt(codeExpires);
			codeEntity.setUsed(false);
			authorizationCodeRepository.save(codeEntity);

			log.info("建立授權code:{}", codeEntity.getCode());
		} catch (Exception e) {
			log.error("免登入失敗，儲存授權碼失敗", e);
			return false;
		}
		// redirect 回 A 平台
		String redirectUrl = String.format("%s?code=%s&state=%s&scope=%s", request.get("redirect_uri"), code,
				request.get("state"), request.get("scope"));
		response.sendRedirect(redirectUrl);
		return true;

	}

	/**
	 * 處理 refresh_token 流程(ok)
	 */
	private commonRes<Map<String, Object>> handleRefreshToken(OauthCode request) {

		String refreshToken = request.getRefresh_token();
		String clientId = request.getClient_id();
		String clientSecret = request.getClient_secret();
		log.info("開始 refresh_token 流程,clientId={},refreshToken={}", clientId, refreshToken);

		// 查詢 refresh_token
		Optional<OauthAccessTokenTb> tokenOpt = oauthAccessTokenRepository.findByRefreshToken(refreshToken);

		if (tokenOpt.isEmpty()) {
			log.error("refreshToken:{} ,查詢oauthAccessToken table 失敗", refreshToken);
			return commonRes.error("無效的 refresh_token");
		}

		OauthAccessTokenTb tokenEntity = tokenOpt.get();

		// 驗證 refresh_token 是否過期
		Timestamp now = Timestamp.from(Instant.now());
		Timestamp refreshToken_expired = tokenEntity.getRefreshToken_expiresAt();
		if (refreshToken_expired != null && refreshToken_expired.before(now)) {
			log.error("refreshToken:{} 已失效,失效日:{}", refreshToken, refreshToken_expired);
			return commonRes.error("refresh_token 已失效");
		}

		// 驗證 client_id
		if (!tokenEntity.getClientId().equals(clientId)) {
			log.error("client_id:{} 不匹配", clientId);
			return commonRes.error("client_id 不匹配");
		}

		// 驗證 client_secret
		Optional<OauthRegisterTb> registerInfoOpt = oauthRepository.findByClientId(clientId);
		OauthRegisterTb registerInfo = registerInfoOpt.get();
		if (registerInfoOpt.isEmpty() || !registerInfo.getClientSecret().equals(clientSecret)) {
			log.error("client_secret:{} 驗證失敗", registerInfo.getClientSecret());
			return commonRes.error("client_secret 錯誤");
		}

		// 查詢使用者資訊
		memberInfo userInfo = registerMemberRepository.queryMemberById(tokenEntity.getUserId());
		if (userInfo == null) {
			log.error("refresh_token 流程查無使用者資訊, userId={}", tokenEntity.getUserId());
			return commonRes.error("查無使用者資訊");
		}

		// 只生成新的 access_token 和 id_token
		long nowMillis = System.currentTimeMillis();
		String newAccessToken = UUID.randomUUID().toString();

		String idToken = Jwts.builder().setIssuer("http://localhost:9090")
				.setSubject(String.valueOf(tokenEntity.getUserId())).setAudience(clientId)
				.setIssuedAt(new Date(nowMillis)).setExpiration(new Date(nowMillis + ACCESS_TOKEN_TTL_SECONDS * 1000))// 1hr
																														// 過期
				.claim("email", userInfo.getMail()).claim("name", userInfo.getName())
				.signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256).compact();

		// 更新資料庫中的 access_token
		tokenEntity.setAccessToken(newAccessToken);
		tokenEntity.setAccessToken_expiresAt(new Timestamp(nowMillis + ACCESS_TOKEN_TTL_SECONDS * 1000));
		oauthAccessTokenRepository.save(tokenEntity);

		log.info("refresh_token 流程成功刷新 access_token, userId={}, clientId={}", tokenEntity.getUserId(), clientId);

		// 回傳
		Map<String, Object> response = new HashMap<>();
		response.put("access_token", newAccessToken);
		response.put("id_token", idToken);
		response.put("token_type", "Bearer");
		response.put("access_token_expiresAt", ACCESS_TOKEN_TTL_SECONDS);
		return commonRes.success("成功刷新 token", response);
	}

	/**
	 * 處理 authorization_code 流程(ok)
	 */
	@Transactional
	private commonRes<Map<String, Object>> handleAuthorizationCode(OauthCode request) {
		String code = request.getCode();
		String clientId = request.getClient_id();
		String clientSecret = request.getClient_secret();
		String codeVerifier = request.getCode_verifier();

		long nowMillis = System.currentTimeMillis();
		Timestamp nowTs = new Timestamp(nowMillis);

		log.info("開始 authorization_code 流程,clientId={},code={}", clientId, code);

		// 查詢並驗證 authorization code
		Optional<OauthAuthorizationCodeTb> codeOpt = authorizationCodeRepository.findByCode(code);
		if (codeOpt.isEmpty()) {
			log.error("oauth_authorize_code Table 中找不到 code={}", code);
			return commonRes.error("無效的 code");
		}

		OauthAuthorizationCodeTb codeEntity = codeOpt.get();

		// 驗證 code 是否過期
		Timestamp expireAt = codeEntity.getExpiresAt();
		if (expireAt == null || expireAt.before(nowTs)) {
			log.error("code 已失效,code={},expiresAt={}", code, expireAt);
			return commonRes.error("code 已過期");
		}

		// 驗證是否已使用
		if (codeEntity.isUsed()) {
			log.error("code 已失效,code={},expiresAt={}", code, expireAt);
			return commonRes.error("code 已被使用");
		}

		// 驗證 client_id
		if (!codeEntity.getClientId().equals(clientId)) {
			log.error("client_id 不匹配, clientId={}", clientId);
			return commonRes.error("client_id 不匹配");
		}

		// 驗證 client_secret
		Optional<OauthRegisterTb> clientOpt = oauthRepository.findByClientId(clientId);
		OauthRegisterTb client = clientOpt.get();
		if (client.getClientSecret() == null || !client.getClientSecret().equals(clientSecret)) {
			log.error("client_secret 不匹配,clientId={}", clientId);
			return commonRes.error("client_secret 不匹配");
		}

		// 驗證 PKCE
		if (codeVerifier == null || codeVerifier.isBlank()) {
			log.error("缺乏 codeVerifier值,clientId={}", clientId);
			return commonRes.error("缺乏 codeVerifier值");
		}

		if ("S256".equalsIgnoreCase(codeEntity.getCodeChallengeMethod())) {
			String expectedChallenge = createHash(codeVerifier);
			if (!expectedChallenge.equals(codeEntity.getCodeChallenge())) {
				log.error("code_verifier 驗證失敗");
				return commonRes.error("code_verifier 驗證失敗");
			}
		} else {
			log.error("不符合S256 PKCE方法,method={}", codeEntity.getCodeChallengeMethod());
			return commonRes.error("不符合S256 PKCE方法");
		}

		// 查詢使用者資訊
		memberInfo userInfo = registerMemberRepository.queryMemberById(codeEntity.getUserId());
		if (userInfo == null) {
			log.error("查無使用者資訊,userId={}", codeEntity.getUserId());
			return commonRes.error("查無使用者資訊");
		}

		// 標記授權碼為已使用
		codeEntity.setUsed(true);
		authorizationCodeRepository.save(codeEntity);
		log.info("authorize_code table 標記為已使用,code={}", code);

		// 生成 tokens
		String accessToken = UUID.randomUUID().toString();
		String refreshToken = UUID.randomUUID().toString();

		// 生成 id_token
		String idToken = Jwts.builder().setIssuer("http://localhost:9090")
				.setSubject(String.valueOf(codeEntity.getUserId())).setAudience(clientId)
				.setIssuedAt(new Date(nowMillis)).setExpiration(new Date(nowMillis + 3600_000))
				.claim("email", userInfo.getMail()).claim("name", userInfo.getName())
				.signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256).compact();

		// 儲存 access_token 到資料庫
		OauthAccessTokenTb entity = new OauthAccessTokenTb();
		entity.setUserId(codeEntity.getUserId());
		entity.setClientId(clientId);
		entity.setAccessToken(accessToken);
		entity.setRefreshToken(refreshToken);
		entity.setAccessToken_expiresAt(new Timestamp(nowMillis + 3600_000));// 1hr
		entity.setRefreshToken_expiresAt(new Timestamp(nowMillis + 7 * 24 * 3600_000L));// 7天
		entity.setScope(codeEntity.getScope());
		oauthAccessTokenRepository.save(entity);

		log.info("idToken/refreshToken/accessToken 建立完成");

		// 組裝回應
		Map<String, Object> response = new HashMap<>();
		response.put("access_token", accessToken);
		response.put("id_token", idToken);
		response.put("refresh_token", refreshToken);
		response.put("token_type", "Bearer");
		response.put("access_token_expiresAt", 3600);
		response.put("refresh_token_expiresAt", 3600 * 24 * 7);// 7天

		return commonRes.success("成功,回傳 access_token", response);
	}

	/**
	 * 取得 UserInfo(ok)
	 */
	@Override
	public ResponseEntity<Map<String, Object>> getUserInfo(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer")) {
			log.error("UserInfo 失敗：Authorization Header 缺失或格式錯誤");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		log.info("開始取得 userInfo");

		// 取出access_token
		String accessToken = authHeader.substring(7);

		// 資料庫驗證access_token
		Optional<OauthAccessTokenTb> tokenOpt = oauthAccessTokenRepository.findByAccessToken(accessToken);
		if (tokenOpt.isEmpty()) {
			log.error("accessToken:{} 不存在", accessToken);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "access_token驗證失敗"));
		}

		OauthAccessTokenTb token = tokenOpt.get();

		// 驗證 token 有效時間
		if (token.getAccessToken_expiresAt().toInstant().isBefore(Instant.now())) {
			log.error("access_token 已過期: {}", accessToken);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "access_token 已過期"));
		}

		// 查詢使用者資料
		memberInfo userResult = registerMemberRepository.queryMemberById(token.getUserId());
		// 判斷是否存在
		if (userResult == null) {
			log.error("查無此使用者 userId={}", token.getUserId());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "查無使用者"));
		}

		log.info("userInfo 認證成功，回傳使用者資料 userId={}", userResult.getId());

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
	 * 錯誤訊息導回給user
	 */
	private void redirectError(HttpServletResponse response, String redirectUri, String error) throws IOException {
		String url = String.format("%s?error=%s", redirectUri, URLEncoder.encode(error, StandardCharsets.UTF_8));

		response.sendRedirect(url);
	}

}
