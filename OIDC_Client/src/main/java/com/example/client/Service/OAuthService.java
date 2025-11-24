package com.example.client.Service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.client.Dto.ExchangeTokenRequest;

import Dto.commonRes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public interface OAuthService {
	
	/**
	 * 產生 URL 授權資訊
	 * @return
	 */
	commonRes<Map<String, Object>> generateAuthUrl();
	
	/**
	 * client 接收 授權平台(b平台) 傳送的code return code post api 給授權平台(B平台)
	 * @param session
	 * @return
	 */
	commonRes<Map<String, Object>> accessToken(ExchangeTokenRequest request,HttpServletResponse response);
	
	/**
	 * 用 accessToken，return 使用者資訊
	 * @param accessToken
	 * @return
	 */
	commonRes<Map<String, Object>> getUserInfoByToken(String accessToken );

	
	/**
	 * token 檢查
	 * @return
	 */
	commonRes<Map<String, Object>> checkToken(HttpServletRequest request);
	

	
	
}
