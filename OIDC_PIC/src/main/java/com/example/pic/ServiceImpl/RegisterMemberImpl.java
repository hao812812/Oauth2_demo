package com.example.pic.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.example.pic.Repository.registerMemberRepository;
import com.example.pic.Service.RegisterMemberService;

import Dto.RegisterTranrsData;
import Dto.commonRes;
import Dto.memberInfo;

@Service
public class RegisterMemberImpl implements RegisterMemberService {

	@Autowired
	private registerMemberRepository memberRepository;

	/**
	 * 註冊會員
	 */
	@Override
	public commonRes<Void> insertMember(RegisterTranrsData request) {

		try {
			int result = memberRepository.insertMember(request.getUserAccount(), request.getUserMail(),
					request.getUserPassword(),request.getUserName());
			if (result > 0) {
				return commonRes.success("註冊成功");
			} else {
				return commonRes.error("註冊失敗");
			}
		} catch (DataAccessException e) {
			// 捕捉資料庫存取錯誤
		
			e.printStackTrace();
			return commonRes.error("資料庫錯誤: " + e.getMessage());
		} catch (Exception e) {
			// 捕捉其他所有例外
			e.printStackTrace();
			return commonRes.error("註冊失敗: " + e.getMessage());
		}
	}

	/**
	 * 登入會員
	 */
	@Override
	public commonRes<memberInfo> queryMember(RegisterTranrsData request) {
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
		
		return commonRes.success("會員查詢成功",result);
	}
}
