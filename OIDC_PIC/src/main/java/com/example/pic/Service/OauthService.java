package com.example.pic.Service;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import Dto.OauthCode;
import Dto.RegisterTranrsData;
import Dto.commonRes;
import jakarta.servlet.http.HttpServletResponse;

public interface OauthService {

	/**
	 * 獲取auth url 並進行認證
	 * 
	 * @param request
	 * @return
	 */
	public commonRes<Void> getAuthUrl(Map<String, String> request, HttpServletResponse response);

	/**
	 * user同意授權 回傳code url
	 * 
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public commonRes<Void> approveAuthorization(Map<String, String> request, HttpServletResponse response)
			throws IOException;
	
	/**
	 * 換取 token
	 * @param request
	 * @return
	 */
	public commonRes<Map<String, Object>> accessCode(OauthCode request);
	
	/**
	 * 拿 access_token 請求 user information
	 * @param authHeader
	 * @return
	 */
	public ResponseEntity<Map<String, Object>> getUserInfo(String authHeader);


}
