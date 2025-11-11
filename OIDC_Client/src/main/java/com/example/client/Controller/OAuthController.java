package com.example.client.Controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.client.Dto.ExchangeTokenRequest;
import com.example.client.Service.OAuthService;

import Dto.commonRes;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/oauth")
@RestController
public class OAuthController {
	
	@Autowired
	private OAuthService oauthService;
	
	/**
	 * 產生 url 授權
	 * @param session
	 * @return
	 */
	@PostMapping("/authorization-url")
	public commonRes<Map<String, Object>> generateAuthUrl(){
		
		return oauthService.generateAuthUrl();
	}
	
	/**
	 * 接收 授權平台 傳送的code
	 * 得到 使用者資訊
	 * @param request
	 * @return
	 */
	@PostMapping("/sendCode")
	public commonRes<Map<String, Object>> sendCode(@RequestBody ExchangeTokenRequest request,HttpServletResponse response){
		//取得access_token
		commonRes<Map<String, Object>> tokenRes = oauthService.accessToken(request, response);
		if (!tokenRes.isSuccess()) {
		    return tokenRes;
		}
		// 設置 Cookie 給前端
	    String sessionId = (String) tokenRes.getData().get("session_id");
	    if (sessionId != null) {
	        ResponseCookie cookie = ResponseCookie.from("SESSION_ID", sessionId)
	                .httpOnly(true)
	                .secure(false)
	                .path("/")
	                .sameSite("Strict")
	                .maxAge(Duration.ofDays(7))
	                .build();
	        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	    }
        
		//aceess_token 去取 userinfo
		String accessToken = (String)tokenRes.getData().get("access_token");
		commonRes<Map<String, Object>> userInfoRes = oauthService.getUserInfoByToken(accessToken);
		
	    Map<String, Object> result = new HashMap<>();
	    result.putAll(tokenRes.getData());
	    if (userInfoRes.isSuccess()) {
	        result.putAll(userInfoRes.getData());
	    }
		
	    return commonRes.success("登入成功", result);
	}
	
	@GetMapping("/autoLogin")
	public commonRes<?> autoLogin(@CookieValue(value = "SESSION_ID", required = false) String sessionId) {
		return null;

	}
	
	
	

}
