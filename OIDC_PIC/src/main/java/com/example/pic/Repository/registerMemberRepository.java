package com.example.pic.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import Dto.memberInfo;

@Repository
public class registerMemberRepository {

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	/**
	 * 插入會員資料
	 * 
	 * @param userAccount
	 * @param userMail
	 * @param userPassword
	 * @return
	 */
	public int insertMember(String userAccount, String userMail, String userPassword,String userName) {

		String sql = "INSERT INTO member_register(account,password,mail,create_time,name)"
				+ "VALUES(:userAccount,:userPassword,:userMail,:createTime,:userName)";

		Map<String, Object> paramsMap = new HashMap<>();
		paramsMap.put("userAccount", userAccount);
		paramsMap.put("userPassword", userPassword);
		paramsMap.put("userMail", userMail);
		paramsMap.put("userName", userName);
		paramsMap.put("createTime", LocalDateTime.now());

		return namedParameterJdbcTemplate.update(sql, paramsMap);

	}

	/**
	 * 查詢會員資訊
	 * @param userAccount
	 * @return
	 */
	public memberInfo queryMember(String userAccount) {

	    String sql = "SELECT id,account,password,mail,name FROM member_register WHERE account = :account";

	    Map<String, Object> params = new HashMap<>();
	    params.put("account", userAccount);

	    try {
	        return namedParameterJdbcTemplate.queryForObject(
	            sql,
	            params,
	            new BeanPropertyRowMapper<>(memberInfo.class)
	        );
	    } catch (EmptyResultDataAccessException e) {
	        return null; // 找不到帳號
	    }

	}
	
	/**
	 * 查詢會員資訊
	 * @param id
	 * @return
	 */
	public memberInfo queryMemberById(Long id) {

 	    String sql = "SELECT id,account,password,mail,name FROM member_register WHERE id = :id";

	    Map<String, Object> params = new HashMap<>();
	    params.put("id", id);

	    try {
	        return namedParameterJdbcTemplate.queryForObject(
	            sql,
	            params,
	            new BeanPropertyRowMapper<>(memberInfo.class)
	        );
	        //TODO: 若資料庫發生意外怎麼拋錯誤和接錯誤
	    } catch (EmptyResultDataAccessException e) {
	        return null; // 找不到帳號
	    }

	}

}
