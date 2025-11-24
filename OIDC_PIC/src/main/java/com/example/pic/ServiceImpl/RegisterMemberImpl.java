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
	
	private static final Logger log = LoggerFactory.getLogger(OauthImpl.class);
 

	/**
	 * 註冊會員
	 */
	@Override
	public commonRes<Void> insertMember(RegisterTranrsData request) {

		try {
			log.info("開始註冊帳號");
			int result = memberRepository.insertMember(request.getUserAccount(), request.getUserMail(),
					request.getUserPassword(),request.getUserName());
			if (result > 0) {
				log.info("註冊成功");
				return commonRes.success("註冊成功");
			} else {
				log.error("註冊失敗，帳號:{}",request.getUserAccount());
				return commonRes.error("註冊失敗");
			}
 		} catch (DataAccessException e) {
			// 捕捉資料庫存取錯誤
 			throw new BaseException("資料庫庫錯誤",e);
		} catch (Exception e) {
			// 捕捉其他所有例外
			return commonRes.error("註冊失敗: " + e.getMessage());
		}
	}

	/**
	 * 登入會員
	 */
	@Override
	public commonRes<memberInfo> queryMember(RegisterTranrsData request, HttpServletResponse response) {
		memberInfo result = memberRepository.queryMember(request.getUserAccount());
	    //判斷是否存在
	    if (result == null) {
	        return commonRes.error("查無此帳號");
	    }
		String inputPassword = request.getUserPassword();
		String dbPassword = result.getPassword();
	    if (!inputPassword.equals(dbPassword)) {
	        return commonRes.error("密碼錯誤");
	    }
	    
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
	    
		return commonRes.success("會員查詢成功",result);
	}
}
