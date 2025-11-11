package com.example.pic.Controller;

import java.io.IOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.pic.Service.OauthService;
import com.example.pic.Service.RegisterMemberService;
import com.example.pic.ServiceImpl.RegisterMemberImpl;
import Dto.OauthCode;
import Dto.RegisterTranrsData;
import Dto.commonRes;
import Dto.memberInfo;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = {"http://localhost:4200","http://localhost:4300"}, allowCredentials = "true")
@RequestMapping("/api")
public class OidcController {

	
	@Autowired
	private  RegisterMemberService registerService;
	@Autowired
	private OauthService oauthService;

	/**
	 * 使用者 註冊
	 * @param request
	 * @return
	 */
	@PostMapping("/register")
	public commonRes<Void> register(@RequestBody RegisterTranrsData request){
		
		return registerService.insertMember(request);
	}
	
	/**
	 * 使用者 登入
	 * @return
	 */
	@PostMapping("/login")
	public commonRes<memberInfo> login(@RequestBody RegisterTranrsData request){
		return registerService.queryMember(request);
	}
	
	/*
	 * 獲取授權URL
	 */
	@GetMapping("/oauth/getAuthorizeUri")
	public commonRes<Void> getAuthCode(@RequestParam Map<String, String> params,HttpServletResponse response) {
		return oauthService.getAuthUrl(params,response);
	}
	

	/*
	 * 使用者 授權同意後
	 * return  code url 給 client
	 */
	@GetMapping("/oauth/approve")
	public commonRes<Void> approve(@RequestParam Map<String, String> params,HttpServletResponse response) throws IOException {
		 
		 return oauthService.approveAuthorization(params,response);
	
	}
	
	/**
	 * 接收client平台傳送的code ，並回傳access_token
	 * @param params
	 * @return
	 */
	@PostMapping("/oauth/token")
	public commonRes<Map<String, Object>>accessCode(@ModelAttribute OauthCode params) {
		 return oauthService.accessCode(params);
	}
	
	/**
	 * 拿 access_token 請求 user information
	 * @param authHeader
	 * @return
	 */
	@GetMapping("/oauth/userInfo")
	public ResponseEntity<Map<String, Object>>getUserInfo(@RequestHeader("Authorization") String authHeader) {
		 return oauthService.getUserInfo(authHeader);
	}
	

	

}
