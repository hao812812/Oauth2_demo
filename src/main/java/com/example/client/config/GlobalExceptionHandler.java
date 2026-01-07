package com.example.client.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.client.Exception.BaseException;

import com.example.client.Dto.commonRes;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	/**
	 * 抓取自定例外
	 * @param e
	 * @return
	 */
	@ExceptionHandler(BaseException.class)
	public commonRes<Object> handleBaseException(BaseException e){
		
		return new commonRes<>(false,e.getMessage());	
	}
	
	/**
	 * 抓取全部例外
	 * @param e
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public commonRes<Object> handleException(Exception e){
		
		return new commonRes<>(false,e.getMessage());	
	}
	

}
