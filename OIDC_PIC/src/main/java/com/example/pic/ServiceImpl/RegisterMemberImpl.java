package com.example.pic.ServiceImpl;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.pic.Entity.OauthUserSession;
import com.example.pic.Exception.BaseException;
import com.example.pic.Repository.OauthUserSessionRepository;
import com.example.pic.Repository.registerMemberRepository;
import com.example.pic.Service.RegisterMemberService;


import Dto.RegisterTranrsData;
import Dto.commonRes;
import Dto.memberInfo;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RegisterMemberImpl implements RegisterMemberService {

	@Autowired
	private registerMemberRepository memberRepository;
	@Autowired
	private OauthUserSessionRepository oauthUserSessionRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	private static final Logger log = LoggerFactory.getLogger(RegisterMemberImpl.class);
 

	/**
	 * 註冊會員 (ok
	 */
	@Override
	public commonRes<Void> insertMember(RegisterTranrsData request) {

		try {
			String hashedPassword = passwordEncoder.encode(request.getUserPassword());
			int result = memberRepository.insertMember(request.getUserAccount(), request.getUserMail(),
					hashedPassword,request.getUserName());
			if (result > 0) {
				log.info("註冊成功");
				return commonRes.success("註冊成功");
			} else {
				log.error("註冊失敗，帳號:{}",request.getUserAccount());
				return commonRes.error("註冊失敗");
			}
 		} catch (DataAccessException e) {
			// 捕捉資料庫存取錯誤
 			log.error("註冊會員資料庫錯誤",e);
 			throw new BaseException("資料庫錯誤");
		} catch (Exception e) {
			// 捕捉其他所有例外'
			log.error("註冊會員失敗",e);
			throw new BaseException("註冊失敗");
		}
	}

	/**
	 * 登入會員 (ok
	 */
	@Override
	public commonRes<memberInfo> queryMember(RegisterTranrsData request, HttpServletResponse response) {
		memberInfo result = memberRepository.queryMember(request.getUserAccount());
	    //判斷是否存在
	    if (result == null) {
	    	log.warn("登入失敗，查無此帳號{}",request.getUserAccount());
	        return commonRes.error("查無此帳號");
	    }
		String inputPassword = request.getUserPassword();
		String dbPassword = result.getPassword();
		if(!passwordEncoder.matches(inputPassword, dbPassword)) {
			log.warn("登入失敗，密碼錯誤,userId={}",result.getId());
	        return commonRes.error("密碼錯誤");
		}
	    try {
		    // 建立 session
		    String sessionId = UUID.randomUUID().toString();
		    Timestamp expiresAt = Timestamp.from(Instant.now().plus(Duration.ofDays(7)));
		    
		    OauthUserSession session = new OauthUserSession();
		    session.setSessionId(sessionId);
		    session.setUserId(result.getId());
		    session.setExpiresAt(expiresAt);
		    oauthUserSessionRepository.save(session);
		    
		    // 回傳 cookie 給使用者
		    ResponseCookie cookie = ResponseCookie.from("OIDC_SESSION_ID", sessionId)
		            .httpOnly(true)
		            .secure(true)//TODO: research ,只能使用https 傳到 server
		            .sameSite("Strict") //user 在跨網域不會帶cookie
		            .path("/")
		            .maxAge(Duration.ofDays(7))
		            .build();
		    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		    
		    log.info("會員登入成功,userId={}",result.getId());
		    
			return commonRes.success("會員登入成功",result);
		} catch (Exception e) {
			log.error("登入流程建立session失敗,userId={}",result.getId(),e);
			throw new BaseException("登入失敗，系統錯誤");
		
		}
	   
	}
}
