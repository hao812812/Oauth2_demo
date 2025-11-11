package com.example.pic.Service;

import org.springframework.stereotype.Component;

import Dto.RegisterTranrsData;
import Dto.commonRes;
import Dto.memberInfo;

@Component
public interface RegisterMemberService {
	
	/**
	 * 註冊新會員
	 * @param request
	 * @return
	 */
	commonRes<Void> insertMember(RegisterTranrsData request);
	
	/**
	 * 登入會員
	 * @param request
	 * @return
	 */

	commonRes<memberInfo> queryMember(RegisterTranrsData request);
	

	

}
